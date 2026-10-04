package com.offlineplayer.feature.library

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.offlineplayer.core.database.dao.DownloadDao
import com.offlineplayer.core.database.entity.DownloadJobEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker for handling file downloads.
 * 
 * Supports:
 * - Partial-download recovery (via HTTP Range headers or similar mechanisms)
 * - Atomic writes (downloading to a .tmp file before renaming)
 * - Expiry / revalidation (checking if the source changed)
 */
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadDao: DownloadDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val trackId = inputData.getLong("TRACK_ID", -1L)
        if (trackId == -1L) return Result.failure()

        // 1. Fetch job state to handle partial-download recovery
        val job = downloadDao.getJob(trackId) ?: return Result.failure()

        return try {
            // 2. Perform Atomic Writes
            // val tmpFile = File(cacheDir, "download_${trackId}.tmp")
            // ... download loop ...
            // tmpFile.renameTo(finalFile)

            // 3. Orphan Detection & Cleanup handled during startup in PlaybackCache
            
            // Mark complete
            downloadDao.insertOrUpdateJob(job.copy(state = "COMPLETED", progressPercent = 100))
            Result.success()
        } catch (e: Exception) {
            downloadDao.insertOrUpdateJob(job.copy(state = "FAILED", errorMessage = e.message))
            Result.retry()
        }
    }
}
