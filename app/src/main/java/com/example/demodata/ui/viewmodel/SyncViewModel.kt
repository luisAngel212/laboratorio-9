package com.example.demodata.ui.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.format.DateTimeFormatter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.demodata.data.remote.NetworkConstants
import com.example.demodata.data.remote.RetrofitClient
import com.example.demodata.data.remote.model.GeoEventRequest
import com.example.demodata.data.remote.model.GeoEventResponse
import com.example.demodata.data.repository.AudioRepository
import com.example.demodata.data.repository.GpsRepository
import com.example.demodata.data.repository.MediaRepository
import com.example.demodata.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

data class SyncCounts(
    val gpsGoogle: Int = 0,
    val gpsSensors: Int = 0,
    val photos: Int    = 0,
    val videos: Int    = 0,
    val audios: Int    = 0
) {
    val total: Int get() = gpsGoogle + gpsSensors + photos + videos + audios
}

class SyncViewModel(
    private val gpsRepository: GpsRepository,
    private val mediaRepository: MediaRepository,
    private val audioRepository: AudioRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage = _syncMessage.asStateFlow()

    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress = _syncProgress.asStateFlow()
    val counts = combine(
        gpsRepository.googleCount,
        gpsRepository.sensorsCount,
        mediaRepository.photoCount,
        mediaRepository.videoCount,
        audioRepository.count
    ) { g, s, p, v, a ->
        SyncCounts(g, s, p, v, a)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SyncCounts()
    )

    private val _cloudRecords = MutableStateFlow<List<GeoEventResponse>>(emptyList())
    val cloudRecords = _cloudRecords.asStateFlow()

    private val _isLoadingCloud = MutableStateFlow(false)
    val isLoadingCloud = _isLoadingCloud.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    fun sync(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncProgress.value = 0f
            _syncMessage.value = "Iniciando sincronización..."
            try {
                val googlePoints = gpsRepository.googlePoints.first()
                val sensorsPoints = gpsRepository.sensorsPoints.first()

                val deviceId = sessionManager.getDeviceId()
                val userId = sessionManager.userId.first()          // ← viene de la Parte 1
                val token = sessionManager.accessToken.first()
                val authHeader = if (token != null) "Bearer $token" else null

                if (userId == null) {
                    _syncMessage.value = "Error: No se encontró el ID de usuario. Por favor, cierra sesión e inicia sesión de nuevo."
                    _isSyncing.value = false
                    onResult(false)
                    return@launch
                }

                val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
                val formatter = DateTimeFormatter.ISO_INSTANT
                var successCount = 0
                val totalToSync = googlePoints.size + sensorsPoints.size

                if (totalToSync == 0) {
                    _syncMessage.value = "No hay datos para sincronizar"
                    _isSyncing.value = false
                    _syncProgress.value = 1f
                    onResult(true)
                    return@launch
                }

                var currentItem = 0
                googlePoints.forEach { point ->
                    if (point.latitude != null && point.longitude != null && point.latitude != 0.0) {
                        val request = GeoEventRequest(
                            userId = userId,
                            latitude = point.latitude,
                            longitude = point.longitude,
                            accuracy = point.accuracy?.toDouble() ?: 0.0,
                            speed = point.speed?.toDouble() ?: 0.0,
                            heading = point.bearing?.toDouble() ?: 0.0,
                            eventType = "gps_google",
                            deviceId = deviceId,
                            appVersion = "1.0.0",
                            deviceModel = deviceModel,
                            recordedAt = formatter.format(Instant.ofEpochMilli(point.timestamp))
                        )
                        val response = RetrofitClient.apiService.createGeoEventORM(NetworkConstants.PROJECT_SLUG, authHeader, request)
                        if (response.isSuccessful) successCount++
                    }
                    currentItem++
                    _syncProgress.value = currentItem.toFloat() / totalToSync
                }

                sensorsPoints.forEach { point ->
                    if (point.latitude != null && point.longitude != null && point.latitude != 0.0) {
                        val request = GeoEventRequest(
                            userId = userId,
                            latitude = point.latitude,
                            longitude = point.longitude,
                            altitude = point.altitude ?: 0.0,
                            eventType = "gps_sensors",
                            deviceId = deviceId,
                            appVersion = "1.0.0",
                            deviceModel = deviceModel,
                            recordedAt = formatter.format(Instant.ofEpochMilli(point.timestamp))
                        )
                        val response = RetrofitClient.apiService.createGeoEventORM(NetworkConstants.PROJECT_SLUG, authHeader, request)
                        if (response.isSuccessful) successCount++
                    }
                    currentItem++
                    _syncProgress.value = currentItem.toFloat() / totalToSync
                }

                if (successCount > 0) {
                    gpsRepository.clearAll()                         // ← borrado local, solo si hubo éxito
                    _syncMessage.value = "Sincronizados $successCount registros con éxito"
                    refreshCloudData()                                // ← dispara la Parte 3
                } else {
                    _syncMessage.value = "Error al sincronizar con el servidor"
                }
                onResult(successCount > 0)
            } catch (e: Exception) {
                _syncMessage.value = "Error: ${e.localizedMessage}"
                onResult(false)
            } finally {
                _isSyncing.value = false
            }
        }
    }
    fun refreshCloudData() {
        viewModelScope.launch {
            _isLoadingCloud.value = true
            try {
                val userId = sessionManager.userId.first()
                val token = sessionManager.accessToken.first()
                val authHeader = if (token != null) "Bearer $token" else null

                val response = RetrofitClient.apiService.listGeoEventsORM(
                    NetworkConstants.PROJECT_SLUG,
                    authHeader,
                    userId = userId,
                    limit = 10
                )

                if (response.isSuccessful) {
                    _cloudRecords.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // Silencioso o log
            } finally {
                _isLoadingCloud.value = false
            }
        }
    }

    class Factory(
        private val gps:   GpsRepository,
        private val media: MediaRepository,
        private val audio: AudioRepository,
        private val session: SessionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SyncViewModel(gps, media, audio, session) as T
    }
}