package com.example.demodata.data.remote

import com.example.demodata.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*


interface ApiService {

    @POST("{projectSlug}/auth/register")
    suspend fun register(
        @Path("projectSlug") projectSlug: String,
        @Body request: RegisterRequest
    ): Response<Unit>

    @POST("{projectSlug}/auth/login")
    suspend fun login(
        @Path("projectSlug") projectSlug: String,
        @Body request: LoginRequest
    ): Response<TokenResponse>

    @POST("{projectSlug}/auth/google")
    suspend fun loginWithGoogle(
        @Path("projectSlug") projectSlug: String,
        @Body request: GoogleLoginRequest
    ): Response<TokenResponse>

    @GET("{projectSlug}/auth/me")
    suspend fun me(
        @Path("projectSlug") projectSlug: String,
        @Header("Authorization") token: String
    ): Response<UserMeResponse>

    @POST("{projectSlug}/auth/refresh-token")
    suspend fun refreshToken(
        @Path("projectSlug") projectSlug: String,
        @Body request: RefreshTokenRequest
    ): Response<TokenResponse>

    @POST("{projectSlug}/geo-events-orm/")
    suspend fun createGeoEventORM(
        @Path("projectSlug") projectSlug: String,
        @Header("Authorization") token: String?,
        @Body request: GeoEventRequest
    ): Response<GeoEventResponse>

    @GET("{projectSlug}/geo-events-orm/")
    suspend fun listGeoEventsORM(
        @Path("projectSlug") projectSlug: String,
        @Header("Authorization") token: String?,
        @Query("user_id") userId: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<List<GeoEventResponse>>
}