package com.offlineplayer.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val year: Int,
    val artworkUri: String?
)
