package com.example.demodata.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.demodata.DemoDataApp
import com.example.demodata.data.local.entity.MediaEntity
import com.example.demodata.data.local.entity.MediaType
import com.example.demodata.ui.viewmodel.MediaViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import coil.request.ImageRequest
import coil.decode.VideoFrameDecoder
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MediaScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as DemoDataApp

    val vm: MediaViewModel = viewModel(
        factory = MediaViewModel.Factory(
            app.mediaRepository,
            app.fileStorage
        )
    )

    val mediaList by vm.mediaList.collectAsStateWithLifecycle()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    var pendingFile by remember { mutableStateOf<File?>(null) }
    var pendingType by remember { mutableStateOf<MediaType?>(null) }
    var videoStartTimeMs by remember { mutableStateOf(0L) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingFile

        if (success && file != null && file.exists()) {
            vm.onPhotoCaptured(
                file.absolutePath,
                widthPx = 0,
                heightPx = 0
            )
        } else {
            file?.takeIf { it.exists() }?.delete()
        }

        pendingFile = null
        pendingType = null
    }

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        val file = pendingFile

        if (success && file != null && file.exists()) {
            val durationMs =
                System.currentTimeMillis() - videoStartTimeMs

            vm.onVideoCaptured(
                file.absolutePath,
                durationMs
            )
        } else {
            file?.takeIf { it.exists() }?.delete()
        }

        pendingFile = null
        pendingType = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Multimedia",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Fotos y videos guardados en filesDir",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!cameraPermission.status.isGranted) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Esta pantalla necesita permiso de cámara.",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            cameraPermission.launchPermissionRequest()
                        }
                    ) {
                        Text("Conceder permiso")
                    }
                }
            }

            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Button(
                onClick = {
                    val file = vm.newPhotoFile()

                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )

                    pendingFile = file
                    pendingType = MediaType.PHOTO

                    photoLauncher.launch(uri)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text("Foto")
            }

            Button(
                onClick = {
                    val file = vm.newVideoFile()

                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )

                    pendingFile = file
                    pendingType = MediaType.VIDEO
                    videoStartTimeMs = System.currentTimeMillis()

                    videoLauncher.launch(uri)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text("Video")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "${mediaList.size} elementos capturados",
            style = MaterialTheme.typography.titleSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (mediaList.isEmpty()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aún no has capturado nada. Tap en Foto o Video.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

        } else {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = mediaList,
                    key = { it.id }
                ) { media ->

                    MediaItemRow(
                        media = media,
                        onDelete = {
                            vm.delete(media)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaItemRow(
    media: MediaEntity,
    onDelete: () -> Unit
) {
    val dateFormat = remember {
        SimpleDateFormat(
            "dd/MM HH:mm:ss",
            Locale.getDefault()
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            val context = LocalContext.current

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(media.filePath))
                    .decoderFactory(VideoFrameDecoder.Factory())
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(45.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (media.type == MediaType.PHOTO.name)
                            Icons.Default.PhotoCamera
                        else
                            Icons.Default.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = media.type,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "${media.sizeBytes / 1024} KB" +
                            (media.durationMs?.let { " · ${it / 1000}s" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = dateFormat.format(Date(media.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            IconButton(
                onClick = onDelete
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}