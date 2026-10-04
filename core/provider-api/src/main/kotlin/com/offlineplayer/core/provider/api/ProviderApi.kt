package com.offlineplayer.core.provider.api

import com.offlineplayer.core.model.Track

/**
 * Capability flags that a provider can advertise.
 */
enum class ProviderCapability {
    SEARCH,
    STREAM,
    DOWNLOAD,
    LOSSLESS,
    HI_RES,
    DRM,
    EXPIRING_URL,
    OFFLINE_WINDOW,
}

/**
 * Describes a content source (local files, cloud provider, etc.).
 */
interface ContentProvider {
    /** Unique identifier for this provider. */
    val id: String

    /** Human-readable name. */
    val displayName: String

    /** Set of capabilities this provider supports. */
    val capabilities: Set<ProviderCapability>
}

/**
 * A provider that can discover and list tracks.
 */
interface CatalogProvider : ContentProvider {
    /**
     * Discover all tracks available from this provider.
     * Returns a Flow or suspend list depending on implementation.
     */
    suspend fun discoverTracks(): List<Track>
}

/**
 * A provider that can resolve playback URIs.
 */
interface PlaybackProvider : ContentProvider {
    /**
     * Resolve a playable URI for the given track.
     * Returns null if the track cannot be played.
     */
    suspend fun resolvePlaybackUri(track: Track): String?
}
