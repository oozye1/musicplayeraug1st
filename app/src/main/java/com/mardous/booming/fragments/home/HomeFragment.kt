/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.fragments.home

import android.media.session.MediaController
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import android.util.Log
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chibde.BaseVisualizer
import com.chibde.visualizer.BarVisualizer
import com.chibde.visualizer.BlazingColorVisualizer
import com.chibde.visualizer.BreathingCircleVisualizer
import com.chibde.visualizer.CircleBarVisualizer
import com.chibde.visualizer.CircleVisualizer
import com.chibde.visualizer.CustomBarVisualizer
import com.chibde.visualizer.DotMatrixVisualizer
import com.chibde.visualizer.FadingBlocksVisualizer
import com.chibde.visualizer.FountainVisualizer
import com.chibde.visualizer.GradientBlocksVisualizer
import com.chibde.visualizer.LineBarVisualizer
import com.chibde.visualizer.LineVisualizer
import com.chibde.visualizer.RadialSunburstVisualizer
import com.chibde.visualizer.SpikeVisualizer
import com.chibde.visualizer.SquareBarVisualizer
import com.chibde.visualizer.WaveVisualizer
import com.mardous.booming.R
import com.mardous.booming.adapters.HomeAdapter
import com.mardous.booming.adapters.album.AlbumAdapter
import com.mardous.booming.adapters.artist.ArtistAdapter
import com.mardous.booming.adapters.extension.isNullOrEmpty
import com.mardous.booming.adapters.song.SongAdapter
import com.mardous.booming.databinding.FragmentHomeBinding
import com.mardous.booming.extensions.dp
import com.mardous.booming.extensions.navigation.*
import com.mardous.booming.extensions.resources.addPaddingRelative
import com.mardous.booming.extensions.resources.destroyOnDetach
import com.mardous.booming.extensions.resources.primaryColor
import com.mardous.booming.extensions.resources.setTrackingTouchListener
import com.mardous.booming.extensions.resources.setupStatusBarForeground
import com.mardous.booming.extensions.setSupportActionBar
import com.mardous.booming.extensions.toHtml
import com.mardous.booming.extensions.topLevelTransition
import com.mardous.booming.fragments.base.AbsMainActivityFragment
import com.mardous.booming.helper.menu.*
import com.mardous.booming.interfaces.*
import com.mardous.booming.model.*
import com.mardous.booming.service.equalizer.EqualizerManager
import com.mardous.booming.service.equalizer.OpenSLESConstants
import com.mardous.booming.viewmodels.equalizer.EqualizerViewModel
import com.mardous.booming.viewmodels.library.ReloadType
import com.mardous.booming.viewmodels.library.model.SuggestedResult
import com.mardous.booming.views.AnimSlider
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.adapters.EQPresetAdapter
import com.mardous.booming.databinding.DialogRecyclerViewBinding
import com.mardous.booming.dialogs.InputDialog
import com.mardous.booming.extensions.launchAndRepeatWithViewLifecycle
import com.mardous.booming.extensions.showToast
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.util.Formatter
import java.util.Locale
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isGone

/**
 * @author Christians M. A. (mardous)
 */
class HomeFragment : AbsMainActivityFragment(R.layout.fragment_home),
    View.OnClickListener,
    ISongCallback,
    IAlbumCallback,
    IArtistCallback,
    IHomeCallback,
    IScrollHelper,
    CompoundButton.OnCheckedChangeListener,
    IEQPresetCallback {

    private var _binding: HomeBinding? = null
    private val binding get() = _binding!!

    private var homeAdapter: HomeAdapter? = null
    private val visualizers = mutableMapOf<Class<out BaseVisualizer>, BaseVisualizer>()
    private var visualizerContainer: FrameLayout? = null
    private var cycleVisualizerButton: ImageButton? = null
    private var mediaControllerCallback: MediaController.Callback? = null

    private val visualizerClasses = listOf(
        BlazingColorVisualizer::class.java,
        CustomBarVisualizer::class.java,
        WaveVisualizer::class.java,
        RadialSunburstVisualizer::class.java,
        FadingBlocksVisualizer::class.java,
        GradientBlocksVisualizer::class.java,
        DotMatrixVisualizer::class.java,
        FountainVisualizer::class.java,
        SpikeVisualizer::class.java,
        BreathingCircleVisualizer::class.java,
        BarVisualizer::class.java,
        CircleBarVisualizer::class.java,
        CircleVisualizer::class.java,
        LineBarVisualizer::class.java,
        LineVisualizer::class.java,
        SquareBarVisualizer::class.java
    )
    private var currentVisualizerIndex = 0
    private var lastAudioSessionId = -1

    private val currentContent: SuggestedResult
        get() = libraryViewModel.getSuggestions().value ?: SuggestedResult.Idle

    private val equalizerViewModel: EqualizerViewModel by viewModel {
        parametersOf(playerViewModel.audioSessionId)
    }

    private var presetAdapter: EQPresetAdapter? = null
    private var mPresetsDialog: android.app.Dialog? = null
    private var mReverbSpinnerAdapter: ArrayAdapter<String>? = null

    private var mEqualizerBands = 0
    private val mEqualizerSeekBar = arrayOfNulls<AnimSlider>(EqualizerManager.EQUALIZER_MAX_BANDS)

    private val formatBuilder = StringBuilder()
    private val formatter = Formatter(formatBuilder, Locale.getDefault())

    private val bandLevelRange: IntArray
        get() = equalizerViewModel.bandLevelRange

    private val centerFrequencies: IntArray
        get() = equalizerViewModel.centerFreqs

    private val RECORD_AUDIO_REQUEST_CODE = 1001

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
    }

    @Deprecated("This method is now deprecated")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showCurrentVisualizer()
        } else {
            Toast.makeText(requireContext(), "RECORD_AUDIO permission is required for the visualizer", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupVisualizer(force: Boolean = false) {
        val audioSessionId = playerViewModel.audioSessionId
        Log.d("VisualizerDebug", "Setup visualizer. audioSessionId: $audioSessionId, isAdded: $isAdded, isResumed: $isResumed")
        if (audioSessionId > 0 && isAdded && isResumed) {
            if (audioSessionId != lastAudioSessionId || force) {
                try {
                    val currentVisualizer = visualizers[visualizerClasses[currentVisualizerIndex]]
                    currentVisualizer?.setPlayer(audioSessionId)
                    lastAudioSessionId = audioSessionId
                    Log.d("VisualizerDebug", "Visualizer set with sessionId: $audioSessionId")
                } catch (e: Exception) {
                    Log.e("VisualizerDebug", "Error setting visualizer: ${e.message}", e)
                }
            }
        } else {
            Log.w("VisualizerDebug", "audioSessionId not valid or visualizer not ready. audioSessionId: $audioSessionId, isAdded: $isAdded, isResumed: $isResumed")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val homeBinding = FragmentHomeBinding.bind(view)
        _binding = HomeBinding(homeBinding)

        MobileAds.initialize(requireContext()) {}
        val adRequest = AdRequest.Builder().build()
        val adView = view.findViewById<com.google.android.gms.ads.AdView>(R.id.adView)
        adView?.loadAd(adRequest)

        visualizerContainer = view.findViewById(R.id.visualizer_container)
        cycleVisualizerButton = view.findViewById(R.id.cycle_visualizer_button)
        cycleVisualizerButton?.setOnClickListener {
            cycleVisualizer()
        }

        if (hasRecordAudioPermission()) {
            showCurrentVisualizer()
        } else {
            requestRecordAudioPermission()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            playerViewModel.progressFlow.collectLatest {
                setupVisualizer()
            }
        }

        // Fallback: Listen for playback state changes and retry setting sessionId if not valid
        if (mediaControllerCallback == null) {
            mediaControllerCallback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) {
                    val sessionId = playerViewModel.audioSessionId
                    Log.d("VisualizerDebug", "Playback state changed. Retrying sessionId: $sessionId")
                    if (sessionId > 0) {
                        setupVisualizer()
                    }
                }
            }
            mainActivity.mediaController?.registerCallback(mediaControllerCallback!!)
        }

        binding.appBarLayout.setupStatusBarForeground()
        setSupportActionBar(binding.toolbar)
        topLevelTransition(view)

        setupTitle()
        setupListeners()
        checkForMargins()
        setupEqualizer()

        homeAdapter = HomeAdapter(arrayListOf(), this).also {
            it.registerAdapterDataObserver(adapterDataObserver)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = homeAdapter
            addPaddingRelative(bottom = 8.dp(resources))
            destroyOnDetach()
        }
        libraryViewModel.getMiniPlayerMargin().observe(viewLifecycleOwner) {
            binding.recyclerView.updatePadding(
                bottom = it.getWithSpace(16.dp(resources), includeInsets = false)
            )
        }
        libraryViewModel.getSuggestions().apply {
            observe(viewLifecycleOwner) { result ->
                if (result.isLoading && homeAdapter.isNullOrEmpty) {
                    binding.progressIndicator.show()
                } else {
                    binding.progressIndicator.hide()
                }
                homeAdapter?.dataSet = result.data
            }
        }.also { liveData ->
            if (liveData.value == SuggestedResult.Idle) {
                libraryViewModel.forceReload(ReloadType.Suggestions)
            }
        }

        applyWindowInsetsFromView(view)
    }

    private fun cycleVisualizer() {
        val oldVisualizer = visualizers[visualizerClasses[currentVisualizerIndex]]
        oldVisualizer?.visibility = View.GONE

        currentVisualizerIndex = (currentVisualizerIndex + 1) % visualizerClasses.size
        showCurrentVisualizer(true)
    }

    private fun showCurrentVisualizer(force: Boolean = false) {
        val visualizerClass = visualizerClasses[currentVisualizerIndex]
        val visualizer = visualizers.getOrPut(visualizerClass) {
            visualizerClass.getConstructor(android.content.Context::class.java).newInstance(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                visualizerContainer?.addView(this)
            }
        }
        visualizer.visibility = View.VISIBLE
        setupVisualizer(force)
    }

    private val adapterDataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            checkIsEmpty()
        }
    }

    private fun setupTitle() {
        binding.appBarLayout.toolbar.setNavigationOnClickListener {
            findNavController().navigate(R.id.nav_search)
        }
        val hexColor = String.format("#%06X", 0xFFFFFF and primaryColor())
        val appName = "Love <font color=$hexColor>Music</font>".toHtml()
        binding.appBarLayout.title = appName
    }

    private fun setupListeners() {
        binding.myTopTracks.setOnClickListener(this)
        binding.lastAdded.setOnClickListener(this)
        binding.history.setOnClickListener(this)
        binding.shuffleButton.setOnClickListener(this)
    }

    private fun checkIsEmpty() {
        binding.empty.isVisible = !currentContent.isLoading && homeAdapter.isNullOrEmpty
    }

    private fun checkForMargins() {
        checkForMargins(binding.recyclerView)
    }

    override fun onClick(view: View) {
        when (view) {
            binding.myTopTracks -> {
                findNavController().navigate(R.id.nav_detail_list, detailArgs(ContentType.TopTracks))
            }

            binding.lastAdded -> {
                findNavController().navigate(R.id.nav_detail_list, detailArgs(ContentType.RecentSongs))
            }

            binding.history -> {
                findNavController().navigate(R.id.nav_detail_list, detailArgs(ContentType.History))
            }

            binding.shuffleButton -> {
                libraryViewModel.allSongs().observe(viewLifecycleOwner) {
                    playerViewModel.openAndShuffleQueue(it)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkForMargins()
        setupMediaController()
        if (hasRecordAudioPermission()) {
            showCurrentVisualizer()
        }
    }


    override fun onPause() {
        super.onPause()
        binding.recyclerView.stopScroll()
        mediaControllerCallback?.let { mainActivity.mediaController?.unregisterCallback(it) }
        visualizers.values.forEach { it.visibility = View.GONE }
    }

    override fun onDestroyView() {
        homeAdapter?.unregisterAdapterDataObserver(adapterDataObserver)
        if (_binding != null) {
            if (mEqualizerSeekBar.isNotEmpty()) {
                mEqualizerSeekBar.forEach { slider ->
                    slider?.clearOnChangeListeners()
                    slider?.clearOnSliderTouchListeners()
                }
            }
            binding.equalizerEffects.bassboostStrength.let { slider ->
                slider.clearOnChangeListeners()
                slider.clearOnSliderTouchListeners()
            }
            binding.equalizerEffects.virtualizerStrength.let { slider ->
                slider.clearOnChangeListeners()
                slider.clearOnSliderTouchListeners()
            }
            binding.equalizerEffects.loudnessGain.let { slider ->
                slider.clearOnChangeListeners()
                slider.clearOnSliderTouchListeners()
            }
            binding.recyclerView.adapter = null
            binding.recyclerView.layoutManager = null
        }

        homeAdapter = null
        visualizers.values.forEach { it.release() }
        visualizers.clear()
        visualizerContainer?.removeAllViews()
        visualizerContainer = null
        cycleVisualizerButton = null

        super.onDestroyView()
        _binding = null
    }

    override fun onMediaContentChanged() {
        libraryViewModel.forceReload(ReloadType.Suggestions)
    }

    override fun onFavoriteContentChanged() {
        libraryViewModel.forceReload(ReloadType.Suggestions)
    }

    @Suppress("UNCHECKED_CAST")
    override fun createSuggestionAdapter(suggestion: Suggestion): RecyclerView.Adapter<*> {
        return when (suggestion.type) {
            ContentType.TopArtists,
            ContentType.RecentArtists -> ArtistAdapter(
                mainActivity,
                (suggestion.items as List<Artist>),
                R.layout.item_artist,
                this
            )

            ContentType.TopAlbums,
            ContentType.RecentAlbums -> AlbumAdapter(
                mainActivity,
                (suggestion.items as List<Album>),
                R.layout.item_album_gradient,
                callback = this
            )

            ContentType.Favorites,
            ContentType.NotRecentlyPlayed -> SongAdapter(
                mainActivity,
                (suggestion.items as List<Song>),
                R.layout.item_image,
                callback = this
            )

            else -> throw IllegalArgumentException("Unexpected suggestion type: ${suggestion.type}")
        }
    }

    override fun suggestionClick(suggestion: Suggestion) {
        when (suggestion.type) {
            ContentType.Favorites -> {
                libraryViewModel.favoritePlaylist().observe(viewLifecycleOwner) {
                    findNavController().navigate(R.id.nav_playlist_detail, playlistDetailArgs(it.playListId))
                }
            }

            else -> {
                findNavController().navigate(R.id.nav_detail_list, detailArgs(suggestion.type))
            }
        }
    }

    override fun songMenuItemClick(
        song: Song,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?
    ): Boolean = song.onSongMenu(this, menuItem)

    override fun songsMenuItemClick(songs: List<Song>, menuItem: MenuItem) {
        songs.onSongsMenu(this, menuItem)
    }

    override fun albumClick(album: Album, sharedElements: Array<Pair<View, String>>?) {
        findNavController().navigate(
            R.id.nav_album_detail,
            albumDetailArgs(album.id),
            null,
            sharedElements.asFragmentExtras()
        )
    }

    override fun albumMenuItemClick(
        album: Album,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?
    ): Boolean = album.onAlbumMenu(this, menuItem)

    override fun albumsMenuItemClick(albums: List<Album>, menuItem: MenuItem) {
        albums.onAlbumsMenu(this, menuItem)
    }

    override fun artistClick(artist: Artist, sharedElements: Array<Pair<View, String>>?) {
        findNavController().navigate(
            R.id.nav_artist_detail,
            artistDetailArgs(artist),
            null,
            sharedElements.asFragmentExtras()
        )
    }

    override fun artistMenuItemClick(
        artist: Artist,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?
    ): Boolean = artist.onArtistMenu(this, menuItem)

    override fun artistsMenuItemClick(artists: List<Artist>, menuItem: MenuItem) {
        artists.onArtistsMenu(this, menuItem)
    }

    private fun setupMediaController() {
        mediaControllerCallback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) {
                super.onPlaybackStateChanged(state)
                setupVisualizer()
            }
        }
        mediaControllerCallback?.let { mainActivity.mediaController?.registerCallback(it) }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_library, menu)
        menu.removeItem(R.id.action_scan)
        menu.removeItem(R.id.action_grid_size)
        menu.removeItem(R.id.action_view_type)
        menu.removeItem(R.id.action_sort_order)
        menu.findItem(R.id.action_settings).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.action_settings) {
            findNavController().navigate(R.id.nav_settings)
            return true
        }
        return false
    }

    override fun scrollToTop() {
        binding.container.scrollTo(0, 0)
        binding.appBarLayout.setExpanded(true)
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        when (buttonView) {
            binding.equalizerBands.presetSwitch -> {
                equalizerViewModel.setEqualizerState(isChecked)
            }

            binding.equalizerEffects.loudnessEnhancerSwitch -> {
                equalizerViewModel.setLoudnessGain(isEnabled = isChecked)
            }

            binding.equalizerEffects.reverbSwitch -> {
                equalizerViewModel.setPresetReverb(isEnabled = isChecked)
            }
        }
    }

    override fun eqPresetSelected(eqPreset: EQPreset) {
        equalizerViewModel.setEqualizerPreset(eqPreset)
        mPresetsDialog?.dismiss()
    }

    override fun editEQPreset(eqPreset: EQPreset) {
        InputDialog.Builder(requireContext())
            .title(R.string.rename_preset)
            .message(R.string.please_enter_a_new_name_for_this_preset)
            .hint(R.string.preset_name)
            .prefill(eqPreset.getName(requireContext()))
            .positiveText(R.string.rename_action)
            .createDialog { dialog, inputLayout, text, _ ->
                if (text.isNullOrBlank()) {
                    inputLayout.error = getString(R.string.preset_name_is_empty)
                } else {
                    equalizerViewModel.renamePreset(eqPreset, text).observe(viewLifecycleOwner) {
                        showToast(it.messageRes)
                        if (it.canDismiss) {
                            dialog.dismiss()
                        }
                    }
                }
                false
            }
            .show(childFragmentManager, "RENAME_PRESET")
    }

    override fun deleteEQPreset(eqPreset: EQPreset) {
        val presetName = eqPreset.getName(requireContext())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_preset)
            .setMessage(getString(R.string.delete_preset_x, presetName).toHtml())
            .setPositiveButton(R.string.yes) { _: DialogInterface, _: Int ->
                equalizerViewModel.deletePreset(eqPreset).observe(viewLifecycleOwner) { result ->
                    if (result.success) {
                        showToast(getString(R.string.preset_x_deleted, presetName))
                    }
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun setupEqualizer() {
        presetAdapter = EQPresetAdapter(emptyList(), this)
        binding.equalizerBands.presetSwitch.setOnCheckedChangeListener(this)
        binding.equalizerEffects.loudnessEnhancerSwitch.setOnCheckedChangeListener(this)
        binding.equalizerEffects.reverbSwitch.setOnCheckedChangeListener(this)

        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            equalizerViewModel.eqStateFlow.collect { state ->
                binding.equalizerBands.presetSwitch.isEnabled = state.isSupported
                if (binding.equalizerBands.presetSwitch.isChecked != state.isUsable) {
                    binding.equalizerBands.presetSwitch.isChecked = state.isUsable
                }

                val isUsable = state.isUsable
                for (seekBar in mEqualizerSeekBar) {
                    seekBar?.isEnabled = state.isUsable
                }

                binding.equalizerBands.selectPreset.isEnabled = isUsable
                binding.equalizerBands.savePreset.isEnabled = isUsable && equalizerViewModel.isCustomPresetSelected()
                binding.equalizerEffects.virtualizerStrength.isEnabled = isUsable && equalizerViewModel.virtualizerState.isSupported
                binding.equalizerEffects.bassboostStrength.isEnabled = isUsable && equalizerViewModel.bassBoostState.isSupported
                if (equalizerViewModel.presetReverbState.isSupported) {
                    binding.equalizerEffects.reverbSwitch.isEnabled = isUsable
                    binding.equalizerEffects.reverb.isEnabled = isUsable && equalizerViewModel.presetReverbState.isUsable
                }
                if (equalizerViewModel.loudnessGainState.isSupported) {
                    binding.equalizerEffects.loudnessEnhancerSwitch.isEnabled = isUsable
                    binding.equalizerEffects.loudnessGain.isEnabled = isUsable && equalizerViewModel.loudnessGainState.isUsable
                }
            }
        }

        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            equalizerViewModel.loudnessGainFlow.collect { state ->
                if (state.isSupported) {
                    binding.equalizerEffects.loudnessEnhancerSwitch.isEnabled = equalizerViewModel.eqState.isEnabled
                    binding.equalizerEffects.loudnessEnhancerSwitch.isChecked = state.isUsable
                    binding.equalizerEffects.loudnessGain.isEnabled = equalizerViewModel.eqState.isEnabled && state.isUsable
                    binding.equalizerEffects.loudnessGain.setValueAnimated(state.value)
                    binding.equalizerEffects.loudnessGainDisplay.text =
                        String.format(Locale.ROOT, "%.0f mDb", state.value)
                }
            }
        }

        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            equalizerViewModel.presetReverbFlow.collect { state ->
                if (state.isSupported) {
                    binding.equalizerEffects.reverbSwitch.isEnabled = equalizerViewModel.eqState.isEnabled
                    binding.equalizerEffects.reverbSwitch.isChecked = state.isUsable
                    binding.equalizerEffects.reverb.isEnabled = equalizerViewModel.eqState.isEnabled && state.isUsable
                    binding.equalizerEffects.reverb.setSelection(state.value)
                }
            }
        }

        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            equalizerViewModel.presetsFlow.collect { presets ->
                presetAdapter?.presets = equalizerViewModel.getEqualizerPresetsWithCustom(presets.list)
            }
        }

        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            equalizerViewModel.currentPresetFlow.collect { newPreset ->
                if (!equalizerViewModel.eqState.isSupported)
                    return@collect

                binding.equalizerBands.preset.text = newPreset.getName(requireContext())
                binding.equalizerBands.savePreset.isEnabled = newPreset.isCustom

                val levels = newPreset.levels
                for (band in levels.indices) {
                    mEqualizerSeekBar[band]?.setValueAnimated(
                        levels[band].toFloat().let { floatLevel ->
                            bandLevelRange[1] / 100.0f + floatLevel / 100.0f
                        })
                }
            }
        }

        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            equalizerViewModel.bassBoostFlow.collect { state ->
                binding.equalizerEffects.bassboostStrength.setValueAnimated(state.value)
                setSoundEffectDisplay(
                    binding.equalizerEffects.bassboostStrengthDisplay,
                    state.value.toInt(),
                    binding.equalizerEffects.bassboostStrength.valueTo.toInt()
                )
            }
        }

        viewLifecycleOwner.launchAndRepeatWithViewLifecycle {
            equalizerViewModel.virtualizerFlow.collect { state ->
                binding.equalizerEffects.virtualizerStrength.setValueAnimated(state.value)
                setSoundEffectDisplay(
                    binding.equalizerEffects.virtualizerStrengthDisplay,
                    state.value.toInt(),
                    binding.equalizerEffects.virtualizerStrength.valueTo.toInt()
                )
            }
        }
        setUpPresets()
        setUpEQViews()
    }

    private fun setUpPresets() {
        if (equalizerViewModel.eqState.isSupported) {
            // setup equalizer presets
            binding.equalizerBands.selectPreset.setOnClickListener {
                if (mPresetsDialog == null) {
                    val binding = DialogRecyclerViewBinding.inflate(layoutInflater).apply {
                        recyclerView.layoutManager = LinearLayoutManager(requireContext())
                        recyclerView.adapter = presetAdapter
                    }
                    mPresetsDialog = MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.select_preset)
                        .setView(binding.root)
                        .setPositiveButton(R.string.action_cancel, null)
                        .create()
                }
                mPresetsDialog?.show()
            }
            binding.equalizerBands.savePreset.setOnClickListener {
                InputDialog.Builder(requireContext())
                    .title(R.string.save_preset)
                    .message(R.string.please_enter_a_name_for_this_preset)
                    .hint(R.string.preset_name)
                    .maxLength(32)
                    .checkablePrompt(R.string.replace_preset_with_same_name)
                    .positiveText(R.string.action_save)
                    .createDialog { dialog, inputLayout, text, checked ->
                        if (text.isNullOrBlank()) {
                            inputLayout.error = getString(R.string.preset_name_is_empty)
                        } else {
                            equalizerViewModel.savePreset(text, checked).observe(viewLifecycleOwner) {
                                showToast(it.messageRes)
                                if (it.canDismiss) {
                                    dialog.dismiss()
                                }
                            }
                        }
                        false
                    }
                    .show(childFragmentManager, "SAVE_PRESET")
            }
        } else {
            binding.equalizerBands.preset.text = getString(R.string.not_supported)
            binding.equalizerBands.selectPreset.isEnabled = false
            binding.equalizerBands.savePreset.isEnabled = false
        }
    }

    private fun setUpEQViews() {
        setUpEqualizerViews()
        setUpBassBoostViews()
        setUpVirtualizerViews()
        setUpLoudnessViews()
        setUpReverbViews()
    }

    private fun setUpEqualizerViews() {
        //Initialize the equalizer elements
        mEqualizerBands = equalizerViewModel.numberOfBands

        val centerFreqs = centerFrequencies
        val bandLevelRange = bandLevelRange
        val maxProgress = bandLevelRange[1] / 100 - bandLevelRange[0] / 100

        for (band in 0 until mEqualizerBands) {
            //Unit conversion from mHz to Hz and use k prefix if necessary to display
            var centerFreqHz = centerFreqs[band] / 1000.toFloat()
            var unit = "Hz"
            if (centerFreqHz >= 1000) {
                centerFreqHz /= 1000
                unit = "KHz"
            }

            val bandContainer = binding.equalizerBands.eqContainer.findViewById<View>(eqViewContainerIds[band])
            bandContainer.visibility = View.VISIBLE
            bandContainer.findViewById<TextView>(eqViewElementIds[band][0]).text =
                String.format("%s %s", freqFormat(centerFreqHz), unit)

            mEqualizerSeekBar[band] =
                bandContainer.findViewById<AnimSlider>(eqViewElementIds[band][1]).apply {
                    valueTo = maxProgress.toFloat()
                    setLabelFormatter {
                        String.format(Locale.ROOT, "%+.1fdb", it - maxProgress / 2)
                    }
                    setTrackingTouchListener(onStop = {
                        equalizerViewModel.setCustomPresetBandLevel(band, bandLevelRange[0] + it.value.toInt() * 100)
                    })
                }
        }
    }

    private fun setUpBassBoostViews() {
        if (equalizerViewModel.bassBoostState.isSupported) {
            binding.equalizerEffects.bassboostStrength.apply {
                valueTo = (OpenSLESConstants.BASSBOOST_MAX_STRENGTH - OpenSLESConstants.BASSBOOST_MIN_STRENGTH).toFloat()
                addOnChangeListener { slider, value, fromUser ->
                    if (fromUser) {
                        equalizerViewModel.setBassBoost(isEnabled = value > 0f, value = value, apply = false)
                    }
                }
                setTrackingTouchListener(onStop = { equalizerViewModel.applyPendingStates() })
            }
        } else {
            binding.equalizerEffects.bassboostStrength.isEnabled = false
        }
    }

    private fun setUpVirtualizerViews() {
        if (equalizerViewModel.virtualizerState.isSupported) {
            binding.equalizerEffects.virtualizerStrength.apply {
                valueTo = (OpenSLESConstants.VIRTUALIZER_MAX_STRENGTH - OpenSLESConstants.VIRTUALIZER_MIN_STRENGTH).toFloat()
                addOnChangeListener { slider, value, fromUser ->
                    if (fromUser) {
                        equalizerViewModel.setVirtualizer(isEnabled = value > 0f, value = value, apply = false)
                    }
                }
                setTrackingTouchListener(onStop = { equalizerViewModel.applyPendingStates() })
            }
        } else {
            binding.equalizerEffects.virtualizerStrength.isEnabled = false
        }
    }

    private fun setUpLoudnessViews() {
        if (equalizerViewModel.loudnessGainState.isSupported) {
            binding.equalizerEffects.loudnessGain.apply {
                valueFrom = OpenSLESConstants.MINIMUM_LOUDNESS_GAIN.toFloat()
                valueTo = OpenSLESConstants.MAXIMUM_LOUDNESS_GAIN.toFloat()
                addOnChangeListener { _, value, fromUser ->
                    if (fromUser) {
                        equalizerViewModel.setLoudnessGain(value = value, apply = false)
                    }
                }
                setTrackingTouchListener(onStop = { equalizerViewModel.applyPendingStates() })
            }
        } else {
            binding.equalizerEffects.loudnessEnhancerSwitch.isGone = true
            binding.equalizerEffects.loudnessGain.isGone = true
            binding.equalizerEffects.loudnessGainDisplay.isGone = true
        }
    }

    private fun setUpReverbViews() {
        if (equalizerViewModel.presetReverbState.isSupported) {
            if (mReverbSpinnerAdapter == null || mReverbSpinnerAdapter!!.count == 0) {
                mReverbSpinnerAdapter = ArrayAdapter(
                    requireContext(), android.R.layout.simple_spinner_item, resources.getStringArray(R.array.reverb_preset_names)
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                binding.equalizerEffects.reverb.adapter = mReverbSpinnerAdapter
                binding.equalizerEffects.reverb.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            equalizerViewModel.setPresetReverb(value = position)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
            }
        } else {
            binding.equalizerEffects.reverbSwitch.isGone = true
            binding.equalizerEffects.reverb.isGone = true
        }
    }

    private fun setSoundEffectDisplay(view: TextView, value: Int, maxValue: Int) {
        view.text = String.format(Locale.ROOT, "%d%%", (value * 100) / maxValue)
    }

    private fun freqFormat(vararg args: Any): String {
        formatBuilder.setLength(0)
        formatter.format("%.0f", *args)
        return formatBuilder.toString()
    }

    companion object {
        private val eqViewElementIds = arrayOf(
            intArrayOf(R.id.EqBand0TopTextView, R.id.EqBand0SeekBar),
            intArrayOf(R.id.EqBand1TopTextView, R.id.EqBand1SeekBar),
            intArrayOf(R.id.EqBand2TopTextView, R.id.EqBand2SeekBar),
            intArrayOf(R.id.EqBand3TopTextView, R.id.EqBand3SeekBar),
            intArrayOf(R.id.EqBand4TopTextView, R.id.EqBand4SeekBar),
            intArrayOf(R.id.EqBand5TopTextView, R.id.EqBand5SeekBar)
        )

        private val eqViewContainerIds = intArrayOf(
            R.id.EqBand0Container,
            R.id.EqBand1Container,
            R.id.EqBand2Container,
            R.id.EqBand3Container,
            R.id.EqBand4Container,
            R.id.EqBand5Container
        )
    }
}
