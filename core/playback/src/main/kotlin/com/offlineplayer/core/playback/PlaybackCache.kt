package com.offlineplayer.core.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
@OptIn(UnstableApi::class)
class PlaybackCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cacheDir = File(context.cacheDir, "media_cache")
    // 500 MB max temporary cache
    private val evictor = LeastRecentlyUsedCacheEvictor(500 * 1024 * 1024)
    private val databaseProvider = StandaloneDatabaseProvider(context)
    
    val simpleCache: SimpleCache by lazy {
        SimpleCache(cacheDir, evictor, databaseProvider)
    }

    fun buildCacheDataSourceFactory(): DataSource.Factory {
        val upstreamFactory = DefaultDataSource.Factory(context)
        return CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
