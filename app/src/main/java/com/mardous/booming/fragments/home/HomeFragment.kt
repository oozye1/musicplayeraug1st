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
import com.chibde.visualizer.CircleBarVisualizer
import com.chibde.visualizer.CircleVisualizer
import com.chibde.visualizer.LineBarVisualizer
import com.chibde.visualizer.LineVisualizer
import com.chibde.visualizer.SquareBarVisualizer
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
import com.mardous.booming.extensions.resources.setupStatusBarForeground
import com.mardous.booming.extensions.setSupportActionBar
import com.mardous.booming.extensions.toHtml
import com.mardous.booming.extensions.topLevelTransition
import com.mardous.booming.fragments.base.AbsMainActivityFragment
import com.mardous.booming.helper.menu.*
import com.mardous.booming.interfaces.*
import com.mardous.booming.model.*
import com.mardous.booming.viewmodels.library.ReloadType
import com.mardous.booming.viewmodels.library.model.SuggestedResult
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import android.support.v4.media.session.PlaybackStateCompat
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.content.pm.PackageManager
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author Christians M. A. (mardous)
 */
class HomeFragment : AbsMainActivityFragment(R.layout.fragment_home),
    View.OnClickListener,
    ISongCallback,
    IAlbumCallback,
    IArtistCallback,
    IHomeCallback,
    IScrollHelper {

    private var _binding: HomeBinding? = null
    private val binding get() = _binding!!

    private var homeAdapter: HomeAdapter? = null
    private val visualizers = mutableMapOf<Class<out BaseVisualizer>, BaseVisualizer>()
    private var visualizerContainer: FrameLayout? = null
    private var cycleVisualizerButton: ImageButton? = null
    private var mediaControllerCallback: MediaController.Callback? = null

    private val visualizerClasses = listOf(
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

    private val RECORD_AUDIO_REQUEST_CODE = 1001

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
    }

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
        val appName = "Booming <font color=$hexColor>Music</font>".toHtml()
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
        super.onDestroyView()
        homeAdapter?.unregisterAdapterDataObserver(adapterDataObserver)
        binding.recyclerView.adapter = null
        binding.recyclerView.layoutManager = null
        homeAdapter = null
        visualizers.values.forEach { it.release() }
        visualizers.clear()
        visualizerContainer?.removeAllViews()
        visualizerContainer = null
        cycleVisualizerButton = null
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
}
