package com.offlineplayer.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.offlineplayer.core.database.dao.LibraryDao
import com.offlineplayer.core.database.entity.AlbumEntity
import com.offlineplayer.core.database.entity.ArtistEntity
import com.offlineplayer.core.database.entity.LocalFileEntity
import com.offlineplayer.core.database.entity.PlaybackHistoryEntity
import com.offlineplayer.core.database.entity.PlaylistEntity
import com.offlineplayer.core.database.entity.PlaylistTrackCrossRef
import com.offlineplayer.core.database.entity.TrackEntity

@Database(
    entities = [
        TrackEntity::class,
        AlbumEntity::class,
        ArtistEntity::class,
        LocalFileEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        PlaybackHistoryEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class OfflinePlayerDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}
