package com.priyanshudeveloper.aod.phoenixengine.core

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.provider.Settings
import com.priyanshudeveloper.aod.phoenixengine.service.AodNotificationListenerService

data class MediaInfo(
    val hasSession: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val durationMs: Long = 0L,
    val currentPositionMs: Long = 0L,
    val canSeek: Boolean = false,
    val canSkipPrevious: Boolean = true,
    val canSkipNext: Boolean = true,
    val canPlayPause: Boolean = true
)

object MediaHelper {

    fun isNotificationListenerGranted(context: Context): Boolean {
        return try {
            val packageName = context.packageName
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            flat != null && flat.contains(packageName)
        } catch (_: Exception) {
            false
        }
    }

    private fun getActiveController(context: Context): MediaController? {
        try {
            val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                ?: return null
            val componentName = ComponentName(context, AodNotificationListenerService::class.java)
            val controllers: List<MediaController>? = try {
                mediaSessionManager.getActiveSessions(componentName)
            } catch (_: SecurityException) {
                mediaSessionManager.getActiveSessions(null)
            } catch (_: Exception) {
                null
            }

            if (!controllers.isNullOrEmpty()) {
                return controllers.firstOrNull { controller ->
                    val state = controller.playbackState?.state
                    state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
                } ?: controllers.firstOrNull()
            }
        } catch (_: Exception) {}
        return null
    }

    fun getCurrentMediaInfo(context: Context): MediaInfo {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val isMusicActive = audioManager?.isMusicActive == true

        if (!isMusicActive) {
            return MediaInfo(hasSession = false, isPlaying = false)
        }

        try {
            val controller = getActiveController(context)
            if (controller != null) {
                val metadata = controller.metadata
                val playbackState = controller.playbackState

                val rawTitle = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                    ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                    ?: ""
                val rawArtist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                    ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
                    ?: ""

                val durationMs = (metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L).coerceAtLeast(0L)

                val state = playbackState?.state ?: PlaybackState.STATE_NONE
                val isPlaying = state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING

                val rawPosition = playbackState?.position ?: 0L
                val lastUpdateTime = playbackState?.lastPositionUpdateTime ?: 0L
                val playbackSpeed = playbackState?.playbackSpeed ?: 1.0f

                val calculatedPositionMs = if (isPlaying && lastUpdateTime > 0L) {
                    val elapsed = SystemClock.elapsedRealtime() - lastUpdateTime
                    val computed = rawPosition + (elapsed * playbackSpeed).toLong()
                    if (durationMs > 0L) computed.coerceIn(0L, durationMs) else computed.coerceAtLeast(0L)
                } else {
                    if (durationMs > 0L) rawPosition.coerceIn(0L, durationMs) else rawPosition.coerceAtLeast(0L)
                }

                val actions = playbackState?.actions ?: 0L
                val canSeek = (actions and PlaybackState.ACTION_SEEK_TO) != 0L && durationMs > 0L
                val canSkipPrevious = actions == 0L || (actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0L
                val canSkipNext = actions == 0L || (actions and PlaybackState.ACTION_SKIP_TO_NEXT) != 0L
                val canPlayPause = actions == 0L || (actions and (PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE)) != 0L

                val displayTitle = if (rawTitle.isNotBlank()) rawTitle else "Music Playing"
                val displayArtist = if (rawArtist.isNotBlank()) rawArtist else "Media Active"

                return MediaInfo(
                    hasSession = true,
                    isPlaying = isPlaying,
                    title = displayTitle,
                    artist = displayArtist,
                    durationMs = durationMs,
                    currentPositionMs = calculatedPositionMs,
                    canSeek = canSeek,
                    canSkipPrevious = canSkipPrevious,
                    canSkipNext = canSkipNext,
                    canPlayPause = canPlayPause
                )
            }
        } catch (_: Exception) {}

        return MediaInfo(
            hasSession = true,
            isPlaying = true,
            title = "Music Playing",
            artist = "Media Active",
            durationMs = 0L,
            currentPositionMs = 0L,
            canSeek = false,
            canSkipPrevious = true,
            canSkipNext = true,
            canPlayPause = true
        )
    }

    fun togglePlayPause(context: Context) {
        try {
            val controller = getActiveController(context) ?: return
            val state = controller.playbackState?.state ?: PlaybackState.STATE_NONE
            if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING) {
                controller.transportControls?.pause()
            } else {
                controller.transportControls?.play()
            }
        } catch (_: Exception) {}
    }

    fun seekTo(context: Context, positionMs: Long) {
        try {
            val controller = getActiveController(context)
            controller?.transportControls?.seekTo(positionMs)
        } catch (_: Exception) {}
    }

    fun skipToPrevious(context: Context) {
        try {
            val controller = getActiveController(context)
            controller?.transportControls?.skipToPrevious()
        } catch (_: Exception) {}
    }

    fun skipToNext(context: Context) {
        try {
            val controller = getActiveController(context)
            controller?.transportControls?.skipToNext()
        } catch (_: Exception) {}
    }
}
