package com.example.demodata.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.demodata.DemoDataApp
import com.example.demodata.data.remote.model.GeoEventResponse
import com.example.demodata.ui.viewmodel.SyncViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SyncScreen() {
    val context = LocalContext.current
    val app     = context.applicationContext as DemoDataApp
    val vm: SyncViewModel = viewModel(
        factory = SyncViewModel.Factory(
            app.gpsRepository,
            app.mediaRepository,
            app.audioRepository,
            app.sessionManager
        )
    )

    val counts by vm.counts.collectAsStateWithLifecycle()
    val isSyncing by vm.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by vm.syncMessage.collectAsStateWithLifecycle()
    val syncProgress by vm.syncProgress.collectAsStateWithLifecycle()
    val cloudRecords by vm.cloudRecords.collectAsStateWithLifecycle()
    val isLoadingCloud by vm.isLoadingCloud.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.refreshCloudData()
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("Sync Center", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Inventario de registros locales pendientes",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        // ── Botón Sync ──
        Button(
            onClick  = {
                vm.sync { success ->
                    if (success) {
                        Toast.makeText(context, "Sincronización finalizada", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = !isSyncing,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isSyncing) "Sincronizando..." else "Sincronizar ahora")
        }

        // ── Sección de Datos en la Nube ──
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Datos en la nube (Servidor)", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = { vm.refreshCloudData() }) { Text("Actualizar") }
        }

        if (isLoadingCloud) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (cloudRecords.isEmpty() && !isLoadingCloud) {
            Text("No hay datos registrados en el servidor para este usuario.", )
        } else {
            cloudRecords.forEach { record ->
                CloudRecordCard(record)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (isSyncing) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { syncProgress },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (syncMessage != null) {
            Text(
                text = syncMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = if (syncMessage!!.contains("Error")) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total de registros locales", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Suma de todas las categorías", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text("${counts.total}", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Desglose por tipo", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        CategoryRow(Icons.Default.LocationOn, "GNSS Google FLP",  counts.gpsGoogle)
        Spacer(modifier = Modifier.height(8.dp))
        CategoryRow(Icons.Default.Sensors,    "GNSS Sensores HW", counts.gpsSensors)
        Spacer(modifier = Modifier.height(8.dp))
        CategoryRow(Icons.Default.PhotoCamera,"Fotos",            counts.photos)
        Spacer(modifier = Modifier.height(8.dp))
        CategoryRow(Icons.Default.Videocam,   "Videos",           counts.videos)
        Spacer(modifier = Modifier.height(8.dp))
        CategoryRow(Icons.Default.AudioFile,  "Audios",           counts.audios)
    }
}

@Composable
private fun CategoryRow(icon: ImageVector, label: String, count: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                "$count",
                style = MaterialTheme.typography.titleLarge,
                color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun CloudRecordCard(record: GeoEventResponse) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("ID: ${record.id} • ${record.eventType ?: "GPS"}", )
                Text("${record.latitude}, ${record.longitude}", )
                Text("Registrado: ${record.recordedAt}", )
            }
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        }
    }
}