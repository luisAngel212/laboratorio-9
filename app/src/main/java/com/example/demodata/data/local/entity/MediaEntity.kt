package com.example.demodata.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media")
data class MediaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val filePath: String,
    val type: String,
    val sizeBytes: Long,
    val durationMs: Long? = null,
    val widthPx: Int? = null,
    val heightPx: Int? = null,
    val timestamp: Long
)

enum class MediaType {
    PHOTO,
    VIDEO
}