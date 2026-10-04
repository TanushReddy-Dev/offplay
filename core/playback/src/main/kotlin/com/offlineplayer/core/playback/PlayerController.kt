package com.offlineplayer.core.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.offlineplayer.core.model.PlaybackState
import com.offlineplayer.core.model.RepeatMode
import com.offlineplayer.core.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around [MediaController] that exposes playback state as
 * a Kotlin [StateFlow] for consumption by Compose ViewModels.
 *
 * ## Lifecycle
 * Call [connect] early (e.g. from `MainActivity.onCreate`).
 * Call [disconnect] in `onDestroy` (or rely on `MediaController.releaseFuture`).
 *
 * ## Threading
 * All [MediaController] calls happen on the main thread, which is
 * required by Media3. The [StateFlow] can be collected from any dispatcher.
 */
@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val diagnosticsReporter: AudioDiagnosticsReporter
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    init {
        // Observe diagnostics and update state
        kotlinx.coroutines.GlobalScope.launch {
            diagnosticsReporter.diagnostics.collect { diag ->
                _state.value = _state.value.copy(diagnostics = diag)
            }
        }
    }

    // ── Connection ──────────────────────────────────────────────────

    fun connect() {
        if (controllerFuture != null) return // already connecting / connected

        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken)
            .buildAsync()
            .also { future ->
                future.addListener(
                    {
                        controller = future.get().also { mc ->
                            _isConnected.value = true
                            mc.addListener(playerListener)
                            syncState(mc)
                            syncQueue(mc)
                        }
                    },
                    MoreExecutors.directExecutor()
                )
            }
    }

    fun disconnect() {
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
        _isConnected.value = false
    }

    // ── Playback commands ───────────────────────────────────────────

    fun play() { controller?.play() }
    fun pause() { controller?.pause() }
    fun playPause() {
        controller?.let { mc ->
            if (mc.isPlaying) mc.pause() else mc.play()
        }
    }

    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }
    fun seekTo(mediaItemIndex: Int, positionMs: Long) { controller?.seekTo(mediaItemIndex, positionMs) }
    fun seekToNext() { controller?.seekToNextMediaItem() }
    fun seekToPrevious() { controller?.seekToPreviousMediaItem() }

    fun setShuffleEnabled(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }

    fun cycleRepeatMode() {
        controller?.let { mc ->
            mc.repeatMode = when (mc.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    /**
     * Replace the current queue with the given [tracks], starting from [startIndex].
     */
    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        controller?.let { mc ->
            val items = tracks.map { it.toMediaItem() }
            mc.setMediaItems(items, startIndex, /* startPositionMs= */ 0L)
            mc.prepare()
            mc.play()
        }
    }

    /**
     * Add a single track to the end of the queue.
     */
    fun addToQueue(track: Track) {
        controller?.let { mc ->
            mc.addMediaItem(track.toMediaItem())
            if (mc.mediaItemCount == 1) {
                mc.prepare()
                mc.play()
            }
        }
    }

    fun clearQueue() {
        controller?.clearMediaItems()
    }

    fun removeFromQueue(index: Int) {
        controller?.removeMediaItem(index)
    }

    fun moveQueueItem(from: Int, to: Int) {
        controller?.moveMediaItem(from, to)
    }

    // ── Internal ────────────────────────────────────────────────────

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) { syncFromController() }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { syncFromController() }
        override fun onPlaybackStateChanged(playbackState: Int) { syncFromController() }
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { syncFromController() }
        override fun onRepeatModeChanged(repeatMode: Int) { syncFromController() }
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            syncFromController()
        }
        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            syncFromController()
            syncQueue(controller)
        }
    }

    private fun syncFromController() {
        controller?.let { syncState(it) }
    }
    
    private fun syncQueue(mc: MediaController?) {
        if (mc == null) {
            _queue.value = emptyList()
            return
        }
        val tracks = mutableListOf<Track>()
        for (i in 0 until mc.mediaItemCount) {
            tracks.add(mc.getMediaItemAt(i).toTrack())
        }
        _queue.value = tracks
    }

    private fun syncState(mc: MediaController) {
        val currentItem = mc.currentMediaItem
        val currentDiagnostics = _state.value.diagnostics
        _state.value = PlaybackState(
            isPlaying = mc.isPlaying,
            currentTrack = currentItem?.toTrack(),
            positionMs = mc.currentPosition.coerceAtLeast(0L),
            durationMs = mc.duration.let { if (it == C.TIME_UNSET) 0L else it },
            queueIndex = mc.currentMediaItemIndex,
            queueSize = mc.mediaItemCount,
            shuffleEnabled = mc.shuffleModeEnabled,
            repeatMode = mc.repeatMode.toRepeatMode(),
            diagnostics = currentDiagnostics
        )
    }

    // ── Mapping helpers ─────────────────────────────────────────────

    companion object {
        internal const val EXTRA_TRACK_ID = "track_id"

        private fun Track.toMediaItem(): MediaItem {
            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(artworkUri?.let { Uri.parse(it) })
                .build()

            return MediaItem.Builder()
                .setMediaId(id.toString())
                .setUri(uri)
                .setMediaMetadata(metadata)
                .setRequestMetadata(
                    MediaItem.RequestMetadata.Builder()
                        .setMediaUri(Uri.parse(uri))
                        .build()
                )
                .build()
        }

        private fun MediaItem.toTrack(): Track {
            val meta = mediaMetadata
            return Track(
                id = mediaId.toLongOrNull() ?: 0L,
                title = meta.title?.toString() ?: "",
                artist = meta.artist?.toString() ?: "",
                album = meta.albumTitle?.toString() ?: "",
                artworkUri = meta.artworkUri?.toString(),
                uri = localConfiguration?.uri?.toString()
                    ?: requestMetadata.mediaUri?.toString()
                    ?: "",
            )
        }

        private fun Int.toRepeatMode(): RepeatMode = when (this) {
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            else -> RepeatMode.OFF
        }


    }
}
