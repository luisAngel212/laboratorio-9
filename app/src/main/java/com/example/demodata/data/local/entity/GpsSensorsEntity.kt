package com.example.demodata.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gps_sensors")
data class GpsSensorsEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val latitude: Double?,
    val longitude: Double?,
    val provider: String,
    val altitude: Double? = null,
    val satellites: Int? = null,
    val timestamp: Long
)