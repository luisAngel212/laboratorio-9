package com.example.demodata.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio")
data class AudioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val filePath: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val format: String,
    val timestamp: Long
)