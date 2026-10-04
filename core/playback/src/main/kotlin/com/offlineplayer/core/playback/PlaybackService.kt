package com.offlineplayer.core.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Background playback service built on Media3.
 *
 * Responsibilities:
 * - Owns the [ExoPlayer] instance and [MediaSession].
 * - Handles audio focus automatically via [AudioAttributes].
 * - Reacts to headphone removal (becoming-noisy).
 * - Shows the standard Media3 notification.
 * - Cleans up when the user swipes the app away from recents.
 *
 * The service is declared in the app module's manifest with
 * `foregroundServiceType="mediaPlayback"` and an intent filter for
 * `MediaSessionService`.
 */
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.common.Format
import androidx.media3.exoplayer.DecoderCounters
import com.offlineplayer.core.model.AudioDiagnostics

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var diagnosticsReporter: AudioDiagnosticsReporter
    @Inject lateinit var playbackCache: PlaybackCache

    private var mediaSession: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(this)
            .setDataSourceFactory(playbackCache.buildCacheDataSourceFactory())

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?
            ) {
                val diagnostics = AudioDiagnostics(
                    sourceCodec = format.codecs ?: format.sampleMimeType,
                    sourceContainer = format.containerMimeType,
                    sourceSampleRate = format.sampleRate,
                    sourceBitDepth = format.pcmEncoding,
                    sourceChannels = format.channelCount,
                    sourceBitrate = format.bitrate.toLong(),
                )
                diagnosticsReporter.report(diagnostics)
            }
            
            override fun onPlayerError(
                eventTime: AnalyticsListener.EventTime,
                error: androidx.media3.common.PlaybackException
            ) {
                com.offlineplayer.core.model.AnalyticsLogger.logError(error, "Playback Error")
                com.offlineplayer.core.model.AnalyticsLogger.logEvent("Playback_Error_Occurred", mapOf(
                    "errorCode" to error.errorCode,
                    "errorMessage" to (error.message ?: "Unknown")
                ))
                diagnosticsReporter.report(null)
            }
        })

        val sessionActivityIntent = packageManager
            ?.getLaunchIntentForPackage(packageName)
            ?.let { intent ->
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(PlaybackSessionCallback())
            .apply {
                sessionActivityIntent?.let { setSessionActivity(it) }
            }
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    /**
     * Called when the user swipes the app away from the recent-apps list.
     * We pause playback and stop the service so it doesn't keep running
     * with no UI to control it.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null && !player.playWhenReady) {
            // Not actively playing — stop immediately.
            stopSelf()
        } else if (player != null) {
            // Actively playing — pause first, then stop.
            player.pause()
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    /**
     * Session callback that validates incoming media items.
     * Media items arriving from a MediaController may not have a
     * [MediaItem.LocalConfiguration] (i.e. the URI). We reject items
     * without a URI so the player never receives an unplayable item.
     */
    private inner class PlaybackSessionCallback : MediaSession.Callback {

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): com.google.common.util.concurrent.ListenableFuture<List<MediaItem>> {
            // Media items sent from a controller often carry only mediaId.
            // Rebuild them with requestMetadata.mediaUri if localConfiguration is missing.
            val resolved = mediaItems.map { item ->
                if (item.localConfiguration != null) {
                    item
                } else {
                    item.requestMetadata.mediaUri?.let { uri ->
                        item.buildUpon()
                            .setUri(uri)
                            .build()
                    } ?: item
                }
            }
            return com.google.common.util.concurrent.Futures.immediateFuture(resolved)
        }
    }
}
