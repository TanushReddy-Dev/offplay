package com.offlineplayer.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_jobs")
data class DownloadJobEntity(
    @PrimaryKey
    val trackId: Long,
    val targetUri: String,
    val state: String, // e.g. "QUEUED", "DOWNLOADING", "COMPLETED", "FAILED", "CANCELLED"
    val progressPercent: Int = 0,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val errorMessage: String? = null
)
