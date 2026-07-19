package com.example.demodata.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserMeResponse(
    val user: UserData
)

@Serializable
data class UserData(
    @SerialName("user_id") val userId: String,
    val email: String
)