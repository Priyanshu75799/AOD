package com.priyanshudeveloper.aod.phoenixengine.core

import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Build

data class MediaInfo(
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = ""
)

object MediaHelper {

    fun getCurrentMediaInfo(context: Context): MediaInfo {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val isMusicActive = audioManager?.isMusicActive == true

        if (!isMusicActive) {
            return MediaInfo(isPlaying = false)
        }

        try {
            val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val controllers: List<MediaController>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaSessionManager?.getActiveSessions(null)
            } else null

            if (!controllers.isNullOrEmpty()) {
                for (controller in controllers) {
                    val metadata = controller.metadata
                    if (metadata != null) {
                        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
                            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                            ?: ""
                        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
                            ?: ""

                        if (title.isNotEmpty()) {
                            return MediaInfo(
                                isPlaying = true,
                                title = title,
                                artist = artist.ifEmpty { "Playing Media" }
                            )
                        }
                    }
                }
            }
        } catch (_: SecurityException) {
            // Active sessions permission might not be granted; fallback to audio active status
        } catch (_: Exception) {}

        return MediaInfo(
            isPlaying = true,
            title = "Music Playing",
            artist = "Media Active"
        )
    }
}
