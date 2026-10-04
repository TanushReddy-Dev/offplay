package com.offlineplayer.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["uri"], unique = true),
        Index(value = ["albumId"]),
        Index(value = ["artistId"])
    ]
)
data class TrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val albumArtist: String,
    val album: String,
    val albumId: Long,
    val artistId: Long,
    val trackNumber: Int,
    val discNumber: Int,
    val durationMs: Long,
    val year: Int,
    val genre: String,
    val uri: String,
    val mimeType: String,
    val codec: String,
    val sampleRate: Int,
    val bitDepth: Int,
    val channels: Int,
    val bitrate: Long,
    val size: Long,
    val dateModified: Long,
    val dateAdded: Long,
    val artworkUri: String?,
    val isFavorite: Boolean = false
)
