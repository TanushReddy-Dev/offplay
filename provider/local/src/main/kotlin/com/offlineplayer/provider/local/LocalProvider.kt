package com.offlineplayer.provider.local

import com.offlineplayer.core.database.dao.LibraryDao
import com.offlineplayer.core.model.Track
import com.offlineplayer.core.provider.api.CatalogProvider
import com.offlineplayer.core.provider.api.ProviderCapability
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalProvider @Inject constructor(
    private val scanner: LocalLibraryScanner,
    private val libraryDao: LibraryDao
) : CatalogProvider {
    
    override val id: String = "local_provider"
    
    override val displayName: String = "Local Library"
    
    override val capabilities: Set<ProviderCapability> = setOf(
        ProviderCapability.SEARCH,
        ProviderCapability.LOSSLESS,
        ProviderCapability.HI_RES
    )

    override suspend fun discoverTracks(): List<Track> {
        scanner.scanMediaStore()
        
        val entities = libraryDao.getAllTracks().first()
        return entities.map { entity ->
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
    }
}
