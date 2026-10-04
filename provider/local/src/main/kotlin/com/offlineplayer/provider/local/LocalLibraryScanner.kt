package com.offlineplayer.provider.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.offlineplayer.core.database.dao.LibraryDao
import com.offlineplayer.core.database.entity.LocalFileEntity
import com.offlineplayer.core.database.entity.TrackEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalLibraryScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libraryDao: LibraryDao
) {
    suspend fun scanMediaStore() = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val query = context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            sortOrder
        )

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumArtistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown Title"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val albumArtist = cursor.getString(albumArtistColumn) ?: artist
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val trackNum = cursor.getInt(trackColumn)
                val duration = cursor.getLong(durationColumn)
                val year = cursor.getInt(yearColumn)
                val mimeType = cursor.getString(mimeTypeColumn) ?: ""
                val size = cursor.getLong(sizeColumn)
                val dateModified = cursor.getLong(dateModifiedColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val albumIdStore = cursor.getLong(albumIdColumn)
                
                val uri = ContentUris.withAppendedId(collection, id).toString()
                
                val existingFile = libraryDao.getLocalFile(uri)
                if (existingFile != null && existingFile.size == size && existingFile.modifiedAt == dateModified) {
                    continue // Skip unchanged files
                }
                
                val albumArtUri = Uri.parse("content://media/external/audio/albumart")
                val artworkUri = ContentUris.withAppendedId(albumArtUri, albumIdStore).toString()

                val artistId = libraryDao.getOrCreateArtistId(artist)
                val albumId = libraryDao.getOrCreateAlbumId(album, artist, year, artworkUri)

                val track = TrackEntity(
                    title = title,
                    artist = artist,
                    albumArtist = albumArtist,
                    album = album,
                    albumId = albumId,
                    artistId = artistId,
                    trackNumber = trackNum,
                    discNumber = 0,
                    durationMs = duration,
                    year = year,
                    genre = "",
                    uri = uri,
                    mimeType = mimeType,
                    codec = "", // Extracted later if needed
                    sampleRate = 0,
                    bitDepth = 0,
                    channels = 0,
                    bitrate = 0L,
                    size = size,
                    dateModified = dateModified,
                    dateAdded = dateAdded,
                    artworkUri = artworkUri
                )
                
                try {
                    libraryDao.insertTrack(track)
                    libraryDao.insertLocalFile(
                        LocalFileEntity(uri = uri, size = size, modifiedAt = dateModified, scanState = 1)
                    )
                } catch (e: Exception) {
                    Log.e("LocalLibraryScanner", "Failed to insert track: $uri", e)
                    libraryDao.insertLocalFile(
                        LocalFileEntity(uri = uri, size = size, modifiedAt = dateModified, scanState = 2, errorMessage = e.message)
                    )
                }
            }
        }
    }
}
