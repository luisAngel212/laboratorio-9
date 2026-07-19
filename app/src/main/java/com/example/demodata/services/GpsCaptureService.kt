package com.example.demodata.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.demodata.DemoDataApp
import com.example.demodata.data.local.entity.GpsGoogleEntity
import com.example.demodata.data.local.entity.GpsSensorsEntity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.cancel

class GpsCaptureService : Service() {

    companion object {
        private const val INTERVAL_MS = 10_000L
        private const val SENSOR_TIMEOUT_MS = 5_000L

        private const val CHANNEL_ID = "gps_capture_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val scope = CoroutineScope(
        Dispatchers.IO + SupervisorJob()
    )

    private var captureJob: Job? = null

    private val gpsRepo by lazy {
        (application as DemoDataApp).gpsRepository
    }

    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val locationManager by lazy {
        getSystemService(LOCATION_SERVICE) as LocationManager
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Captura GNSS activa")
                .setContentText("Registrando coordenadas")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .build()
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (!hasLocationPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (captureJob == null) {

            captureJob = scope.launch {

                while (isActive) {

                    performCaptures()

                    delay(INTERVAL_MS)
                }
            }
        }

        return START_STICKY
    }

    @Suppress("MissingPermission")
    private suspend fun performCaptures() {

        val now = System.currentTimeMillis()

        try {

            val loc = fusedClient
                .getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                )
                .await()

            loc?.let {

                gpsRepo.saveGooglePoint(
                    GpsGoogleEntity(
                        latitude = it.latitude,
                        longitude = it.longitude,
                        accuracy = it.accuracy,
                        speed = if (it.hasSpeed()) it.speed else null,
                        bearing = if (it.hasBearing()) it.bearing else null,
                        timestamp = now
                    )
                )
            }

        } catch (_: Exception) {
        }

        try {
            val sensorLoc = withTimeoutOrNull(SENSOR_TIMEOUT_MS) {
                getRawGpsLocation()
            }

            gpsRepo.saveSensorsPoint(
                GpsSensorsEntity(
                    latitude = sensorLoc?.latitude,
                    longitude = sensorLoc?.longitude,
                    provider = LocationManager.GPS_PROVIDER,
                    altitude = if (sensorLoc?.hasAltitude() == true) sensorLoc.altitude else null,
                    satellites = null,
                    timestamp = now
                )
            )

        } catch (_: Exception) {
            gpsRepo.saveSensorsPoint(
                GpsSensorsEntity(
                    latitude = null,
                    longitude = null,
                    provider = LocationManager.GPS_PROVIDER,
                    altitude = null,
                    satellites = null,
                    timestamp = now
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {

        val fine =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarse =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    @Suppress("MissingPermission")
    private suspend fun getRawGpsLocation(): Location =
        suspendCancellableCoroutine { cont ->

            if (!hasLocationPermission()) {

                cont.resumeWithException(
                    SecurityException(
                        "Location permission missing"
                    )
                )

                return@suspendCancellableCoroutine
            }

            val listener = LocationListener { location ->

                if (cont.isActive) {
                    cont.resume(location)
                }
            }

            try {

                locationManager.requestSingleUpdate(
                    LocationManager.GPS_PROVIDER,
                    listener,
                    null
                )

            } catch (e: SecurityException) {

                if (cont.isActive) {
                    cont.resumeWithException(e)
                }
            }

            cont.invokeOnCancellation {
                locationManager.removeUpdates(listener)
            }
        }

    override fun onDestroy() {
        super.onDestroy()

        captureJob?.cancel()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Captura GNSS",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(channel)
        }
    }
}