package com.offlineplayer.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_assets")
data class OfflineAssetEntity(
    @PrimaryKey
    val trackId: Long,
    val sourceUri: String,
    val localCachePath: String,
    val sizeBytes: Long,
    val dateDownloaded: Long,
    val isAuthorized: Boolean = true
)
