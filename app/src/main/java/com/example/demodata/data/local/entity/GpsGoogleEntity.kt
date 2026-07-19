package com.example.demodata.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gps_google")
data class GpsGoogleEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val latitude: Double?,
    val longitude: Double?,
    val accuracy: Float?,
    val speed: Float? = null,
    val bearing: Float? = null,
    val timestamp: Long
)