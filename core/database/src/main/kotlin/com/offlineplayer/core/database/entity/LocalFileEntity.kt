package com.offlineplayer.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "local_files",
    indices = [
        Index(value = ["uri"], unique = true)
    ]
)
data class LocalFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uri: String,
    val size: Long,
    val modifiedAt: Long,
    val scanState: Int, // e.g. 0 = PENDING, 1 = SCANNED, 2 = ERROR
    val errorMessage: String? = null
)
