package com.offlineplayer.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "artists",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class ArtistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)
