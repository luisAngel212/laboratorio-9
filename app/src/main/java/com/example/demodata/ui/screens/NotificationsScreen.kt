package com.example.demodata.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.demodata.workers.DelayedNotificationWorker
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.UUID
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationsScreen() {
    val context = LocalContext.current

    var mensaje by remember { mutableStateOf("") }
    var ultimoEnvio by remember { mutableStateOf<String?>(null) }
    var contadorProgramadas by remember { mutableStateOf(0) }
    var ultimoWorkId by remember { mutableStateOf<UUID?>(null) }

    val notifPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            null
        }

    val tienePermiso = notifPermission?.status?.isGranted ?: true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Notificaciones",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Programa notificaciones locales con WorkManager",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!tienePermiso) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Se requiere permiso para mostrar notificaciones.",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            notifPermission?.launchPermissionRequest()
                        }
                    ) {
                        Text("Conceder permiso")
                    }
                }
            }

            return@Column
        }

        OutlinedTextField(
            value = mensaje,
            onValueChange = { mensaje = it },
            label = { Text("Mensaje de la notificación") },
            placeholder = { Text("Ej: Revisar inventario") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val request =
                    OneTimeWorkRequestBuilder<DelayedNotificationWorker>()
                        .setInitialDelay(10, TimeUnit.SECONDS)
                        .setInputData(
                            workDataOf(
                                DelayedNotificationWorker.INPUT_MESSAGE to mensaje
                            )
                        )
                        .build()

                ultimoWorkId = request.id

                WorkManager.getInstance(context).enqueue(request)

                ultimoEnvio = mensaje
                contadorProgramadas++
                mensaje = ""
            },
            enabled = mensaje.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NotificationAdd,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text("Programar notificación (10 s)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                ultimoWorkId?.let { id ->
                    WorkManager.getInstance(context)
                        .cancelWorkById(id)

                    ultimoWorkId = null
                }
            },
            enabled = ultimoWorkId != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Cancelar notificación programada")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Notificaciones programadas en esta sesión: $contadorProgramadas",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                if (ultimoEnvio != null) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Último mensaje: \"$ultimoEnvio\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}