package com.offlineplayer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.offlineplayer.core.database.entity.AlbumEntity
import com.offlineplayer.core.database.entity.ArtistEntity
import com.offlineplayer.core.database.entity.LocalFileEntity
import com.offlineplayer.core.database.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    
    // Tracks
    @Query("SELECT * FROM tracks ORDER BY title ASC")
    fun getAllTracks(): Flow<List<TrackEntity>>
    
    @Query("SELECT * FROM tracks WHERE albumId = :albumId ORDER BY trackNumber ASC, title ASC")
    fun getTracksForAlbum(albumId: Long): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity): Long

    @Query("DELETE FROM tracks WHERE uri = :uri")
    suspend fun deleteTrackByUri(uri: String)

    // Albums
    @Query("SELECT * FROM albums ORDER BY title ASC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE title = :title AND artist = :artist LIMIT 1")
    suspend fun getAlbumByTitleAndArtist(title: String, artist: String): AlbumEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbum(album: AlbumEntity): Long

    // Artists
    @Query("SELECT * FROM artists ORDER BY name ASC")
    fun getAllArtists(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists WHERE name = :name LIMIT 1")
    suspend fun getArtistByName(name: String): ArtistEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArtist(artist: ArtistEntity): Long

    // Local Files (Scan State)
    @Query("SELECT * FROM local_files WHERE uri = :uri LIMIT 1")
    suspend fun getLocalFile(uri: String): LocalFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalFile(localFile: LocalFileEntity)
    
    @Query("SELECT uri FROM local_files")
    suspend fun getAllLocalFileUris(): List<String>
    
    @Query("DELETE FROM local_files WHERE uri = :uri")
    suspend fun deleteLocalFile(uri: String)

    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteTracks(): Flow<List<TrackEntity>>

    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :trackId")
    suspend fun updateTrackFavorite(trackId: Long, isFavorite: Boolean)

    // --- Playlists ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylist(playlist: com.offlineplayer.core.database.entity.PlaylistEntity): Long

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<com.offlineplayer.core.database.entity.PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTrack(crossRef: com.offlineplayer.core.database.entity.PlaylistTrackCrossRef)

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON t.id = pt.trackId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.dateAdded DESC
    """)
    fun getTracksForPlaylist(playlistId: Long): Flow<List<TrackEntity>>

    // --- History ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaybackHistory(history: com.offlineplayer.core.database.entity.PlaybackHistoryEntity)

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playback_history h ON t.id = h.trackId
        ORDER BY h.timestamp DESC
        LIMIT :limit
    """)
    fun getPlaybackHistory(limit: Int = 50): Flow<List<TrackEntity>>

    // Helper transactions
    @Transaction
    suspend fun getOrCreateArtistId(name: String): Long {
        val existing = getArtistByName(name)
        if (existing != null) return existing.id
        val id = insertArtist(ArtistEntity(name = name))
        return if (id == -1L) getArtistByName(name)?.id ?: 0L else id
    }

    @Transaction
    suspend fun getOrCreateAlbumId(title: String, artist: String, year: Int, artworkUri: String?): Long {
        val existing = getAlbumByTitleAndArtist(title, artist)
        if (existing != null) return existing.id
        val id = insertAlbum(AlbumEntity(title = title, artist = artist, year = year, artworkUri = artworkUri))
        return if (id == -1L) getAlbumByTitleAndArtist(title, artist)?.id ?: 0L else id
    }
}
