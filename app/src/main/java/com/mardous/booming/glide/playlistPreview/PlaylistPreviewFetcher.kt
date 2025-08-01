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

package com.mardous.booming.glide.playlistPreview

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.mardous.booming.extensions.glide.GlideScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PlaylistPreviewFetcher(val context: Context, private val playlistPreview: PlaylistPreview) :
    DataFetcher<Bitmap>, CoroutineScope by GlideScope() {

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in Bitmap>) {
        launch {
            try {
                val customCoverUri = playlistPreview.playlistEntity.customCoverUri
                val bitmap = if (!customCoverUri.isNullOrEmpty()) {
                    // Load custom cover image
                    try {
                        Glide.with(context)
                            .asBitmap()
                            .load(customCoverUri)
                            .submit()
                            .get()
                    } catch (e: Exception) {
                        // If custom cover fails to load, fall back to auto-generated
                        AutoGeneratedPlaylistBitmap.getBitmap(context, playlistPreview.songs)
                    }
                } else {
                    // No custom cover, use auto-generated
                    AutoGeneratedPlaylistBitmap.getBitmap(context, playlistPreview.songs)
                }
                callback.onDataReady(bitmap)
            } catch (e: Exception) {
                callback.onLoadFailed(e)
            }
        }
    }

    override fun cleanup() {}

    override fun cancel() {
        cancel(null)
    }

    override fun getDataClass(): Class<Bitmap> {
        return Bitmap::class.java
    }

    override fun getDataSource(): DataSource {
        return DataSource.LOCAL
    }
}