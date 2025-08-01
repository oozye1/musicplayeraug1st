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
package com.mardous.booming.appshortcuts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import com.mardous.booming.appshortcuts.shortcuttype.LastAddedShortcutType
import com.mardous.booming.appshortcuts.shortcuttype.ShuffleAllShortcutType
import com.mardous.booming.appshortcuts.shortcuttype.TopTracksShortcutType
import com.mardous.booming.extensions.extraNotNull
import com.mardous.booming.model.Playlist
import com.mardous.booming.model.smartplaylist.LastAddedPlaylist
import com.mardous.booming.model.smartplaylist.ShuffleAllPlaylist
import com.mardous.booming.model.smartplaylist.TopTracksPlaylist
import com.mardous.booming.service.MusicService
import com.mardous.booming.service.constants.ServiceAction
import com.mardous.booming.service.playback.Playback

/**
 * @author Adrian Campos
 */
class AppShortcutLauncherActivity : Activity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (extraNotNull(KEY_SHORTCUT_TYPE, SHORTCUT_TYPE_NONE).value) {
            SHORTCUT_TYPE_SHUFFLE_ALL -> {
                startServiceWithPlaylist(Playback.ShuffleMode.On, ShuffleAllPlaylist())
                DynamicShortcutManager.reportShortcutUsed(this, ShuffleAllShortcutType.ID)
            }

            SHORTCUT_TYPE_TOP_TRACKS -> {
                startServiceWithPlaylist(Playback.ShuffleMode.Off, TopTracksPlaylist())
                DynamicShortcutManager.reportShortcutUsed(this, TopTracksShortcutType.ID)
            }

            SHORTCUT_TYPE_LAST_ADDED -> {
                startServiceWithPlaylist(Playback.ShuffleMode.Off, LastAddedPlaylist())
                DynamicShortcutManager.reportShortcutUsed(this, LastAddedShortcutType.ID)
            }
        }
        finish()
    }

    private fun startServiceWithPlaylist(shuffleMode: Playback.ShuffleMode, playlist: Playlist) {
        val intent = Intent(this, MusicService::class.java)
        intent.action = ServiceAction.ACTION_PLAY_PLAYLIST

        val bundle = bundleOf(
            ServiceAction.Extras.EXTRA_PLAYLIST to playlist,
            ServiceAction.Extras.EXTRA_SHUFFLE_MODE to shuffleMode
        )
        intent.setPackage(this.packageName)

        intent.putExtras(bundle)

        startForegroundService(intent)
    }

    companion object {
        const val KEY_SHORTCUT_TYPE = "com.mardous.booming.appshortcuts.ShortcutType"

        const val SHORTCUT_TYPE_SHUFFLE_ALL: Int = 0
        const val SHORTCUT_TYPE_TOP_TRACKS: Int = 1
        const val SHORTCUT_TYPE_LAST_ADDED: Int = 2
        const val SHORTCUT_TYPE_NONE: Int = 3
    }
}
