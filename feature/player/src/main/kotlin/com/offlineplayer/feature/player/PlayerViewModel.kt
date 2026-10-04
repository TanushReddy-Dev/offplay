package com.offlineplayer.feature.player

import androidx.lifecycle.ViewModel
import com.offlineplayer.core.model.PlaybackState
import com.offlineplayer.core.model.RepeatMode
import com.offlineplayer.core.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playerController.state
    val queue: StateFlow<List<com.offlineplayer.core.model.Track>> = playerController.queue

    fun togglePlayPause() {
        playerController.playPause()
    }

    fun skipToNext() {
        playerController.seekToNext()
    }

    fun skipToPrevious() {
        playerController.seekToPrevious()
    }

    fun seekTo(positionMs: Long) {
        playerController.seekTo(positionMs)
    }
    
    fun toggleShuffle() {
        playerController.setShuffleEnabled(!playbackState.value.shuffleEnabled)
    }

    fun toggleRepeatMode() {
        playerController.cycleRepeatMode()
    }

    fun playQueueItem(index: Int) {
        playerController.seekTo(index, 0L)
    }

    fun removeFromQueue(index: Int) {
        playerController.removeFromQueue(index)
    }

    fun moveQueueItem(from: Int, to: Int) {
        playerController.moveQueueItem(from, to)
    }

    fun clearQueue() {
        playerController.clearQueue()
    }
}
