package com.offlineplayer.core.database

import android.content.Context
import androidx.room.Room
import com.offlineplayer.core.database.dao.LibraryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OfflinePlayerDatabase {
        return Room.databaseBuilder(
            context,
            OfflinePlayerDatabase::class.java,
            "offlineplayer_db"
        )
        .fallbackToDestructiveMigration() // For development, allow destructive migrations
        .build()
    }

    @Provides
    fun provideLibraryDao(database: OfflinePlayerDatabase): LibraryDao {
        return database.libraryDao()
    }

    @Provides
    fun provideDownloadDao(database: OfflinePlayerDatabase): com.offlineplayer.core.database.dao.DownloadDao {
        return database.downloadDao()
    }
}
