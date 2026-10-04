package com.offlineplayer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.offlineplayer.core.database.entity.DownloadJobEntity
import com.offlineplayer.core.database.entity.OfflineAssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateJob(job: DownloadJobEntity)

    @Query("SELECT * FROM download_jobs WHERE trackId = :trackId")
    suspend fun getJob(trackId: Long): DownloadJobEntity?

    @Query("SELECT * FROM download_jobs")
    fun observeAllJobs(): Flow<List<DownloadJobEntity>>

    @Query("DELETE FROM download_jobs WHERE trackId = :trackId")
    suspend fun removeJob(trackId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: OfflineAssetEntity)

    @Query("SELECT * FROM offline_assets WHERE trackId = :trackId")
    suspend fun getAsset(trackId: Long): OfflineAssetEntity?

    @Query("SELECT SUM(sizeBytes) FROM offline_assets")
    suspend fun getTotalAssetSize(): Long?
    
    @Query("DELETE FROM offline_assets WHERE trackId = :trackId")
    suspend fun removeAsset(trackId: Long)
}
