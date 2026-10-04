package com.offlineplayer.core.model

/**
 * Represents a music track in the library.
 *
 * This is the domain model used across the app. It is decoupled from
 * Room entities, MediaStore columns, and provider-specific formats.
 */
data class Track(
    val id: Long = 0L,
    val title: String,
    val artist: String = "",
    val albumArtist: String = "",
    val album: String = "",
    val albumId: Long = 0L,
    val trackNumber: Int = 0,
    val discNumber: Int = 0,
    val durationMs: Long = 0L,
    val year: Int = 0,
    val genre: String = "",
    val uri: String = "",
    val mimeType: String = "",
    val codec: String = "",
    val sampleRate: Int = 0,
    val bitDepth: Int = 0,
    val channels: Int = 0,
    val bitrate: Long = 0L,
    val size: Long = 0L,
    val dateModified: Long = 0L,
    val dateAdded: Long = 0L,
    val artworkUri: String? = null,
    val isFavorite: Boolean = false,
)

/**
 * Represents an album.
 */
data class Album(
    val id: Long = 0L,
    val title: String,
    val artist: String = "",
    val year: Int = 0,
    val trackCount: Int = 0,
    val artworkUri: String? = null,
)

/**
 * Represents an artist.
 */
data class Artist(
    val id: Long = 0L,
    val name: String,
    val albumCount: Int = 0,
    val trackCount: Int = 0,
)

/**
 * Playback queue item.
 */
data class QueueItem(
    val queueId: Long = 0L,
    val track: Track,
    val position: Int,
)

/**
 * Represents the current playback state observable by the UI.
 */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentTrack: Track? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queueIndex: Int = -1,
    val queueSize: Int = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
)

enum class RepeatMode {
    OFF,
    ONE,
    ALL,
}

/**
 * Audio diagnostic information captured from the playback pipeline.
 */
data class AudioDiagnostics(
    val sourceCodec: String? = null,
    val sourceContainer: String? = null,
    val sourceSampleRate: Int = 0,
    val sourceBitDepth: Int = 0,
    val sourceChannels: Int = 0,
    val sourceBitrate: Long = 0L,
    val decoderName: String? = null,
    val outputSampleRate: Int = 0,
    val outputEncoding: String? = null,
    val audioRoute: String? = null,
    val isOffloadEnabled: Boolean = false,
    val isProcessingEnabled: Boolean = false,
    val mayBeResampled: Boolean = false,
)
