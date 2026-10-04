package com.offlineplayer.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlineplayer.core.database.dao.LibraryDao
import com.offlineplayer.core.model.Track
import com.offlineplayer.core.playback.PlayerController
import com.offlineplayer.provider.local.LocalProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryDao: LibraryDao,
    private val playerController: PlayerController,
    private val localProvider: LocalProvider
) : ViewModel() {

    val uiState: StateFlow<LibraryUiState> = libraryDao.getAllTracks()
        .map { entities ->
            val tracks = entities.map { entity ->
                Track(
                    id = entity.id,
                    title = entity.title,
                    artist = entity.artist,
                    albumArtist = entity.albumArtist,
                    album = entity.album,
                    albumId = entity.albumId,
                    trackNumber = entity.trackNumber,
                    discNumber = entity.discNumber,
                    durationMs = entity.durationMs,
                    year = entity.year,
                    genre = entity.genre,
                    uri = entity.uri,
                    mimeType = entity.mimeType,
                    codec = entity.codec,
                    sampleRate = entity.sampleRate,
                    bitDepth = entity.bitDepth,
                    channels = entity.channels,
                    bitrate = entity.bitrate,
                    size = entity.size,
                    dateModified = entity.dateModified,
                    dateAdded = entity.dateAdded,
                    artworkUri = entity.artworkUri,
                    isFavorite = entity.isFavorite
                )
            }
            LibraryUiState.Success(tracks)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LibraryUiState.Loading
        )

    fun scanLocalMedia() {
        viewModelScope.launch {
            localProvider.discoverTracks()
        }
    }

    fun playTrack(track: Track, tracks: List<Track>) {
        val index = tracks.indexOf(track).takeIf { it >= 0 } ?: 0
        playerController.playTracks(tracks, index)
    }
}

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Success(val tracks: List<Track>) : LibraryUiState
}
