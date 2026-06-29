package com.smartcourier.feature.active_delivery

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartcourier.core.ui.theme.ColorTokens
import com.smartcourier.core.ui.theme.Dimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executor

private const val JPEG_QUALITY = 60
private const val MAX_IMAGE_DIMENSION = 1920

@Composable
fun ActiveDeliveryHUD(
    routeId: String,
    deliveryId: String,
    onComplete: () -> Unit,
    viewModel: ActiveDeliveryHudViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val cameraExecutor: Executor = remember { ContextCompat.getMainExecutor(context) }

    LaunchedEffect(routeId, deliveryId) {
        viewModel.onAction(ActiveDeliveryHudAction.LoadRoute(routeId, deliveryId))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ActiveDeliveryHudEffect.OpenDialer -> {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${effect.phone}")
                    }
                    context.startActivity(intent)
                }
                is ActiveDeliveryHudEffect.OpenWhatsApp -> {
                    val uri = Uri.parse("https://wa.me/${effect.phone}?text=${Uri.encode(effect.message)}")
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
                is ActiveDeliveryHudEffect.NavigateToDashboard -> onComplete()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) },
                            imageCapture
                        )
                    }, cameraExecutor)
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.grid_4, vertical = Dimens.grid_8)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onComplete) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Back",
                        tint = Color.White
                    )
                }
                Text(
                    "${uiState.currentIndex} of ${uiState.totalDeliveries}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = Dimens.grid_16, vertical = Dimens.grid_12)
            ) {
                Text(
                    uiState.currentAddress,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                if (uiState.recipientName.isNotBlank()) {
                    Text(
                        uiState.recipientName,
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (uiState.trackingToken.isNotBlank()) {
                    Text(
                        "Token: ${uiState.trackingToken}",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (uiState.isPhotoCaptured) {
                Icon(
                    Icons.Default.CheckCircle,
                    "Photo captured",
                    tint = ColorTokens.DeliveryOrange,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Dimens.grid_12)
                        .size(28.dp)
                )
            }
        }

        HudBottomControls(
            uiState = uiState,
            onCallClick = { viewModel.onAction(ActiveDeliveryHudAction.CallClicked) },
            onWhatsAppClick = { viewModel.onAction(ActiveDeliveryHudAction.WhatsAppClicked) },
            onCaptureClick = {
                scope.launch {
                    val photoFile = File(
                        context.cacheDir,
                        "proof_${deliveryId}_${System.currentTimeMillis()}.jpg"
                    )
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    imageCapture.takePicture(
                        outputOptions, cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                scope.launch {
                                    val compressed = compressJpeg(photoFile.absolutePath)
                                    viewModel.onAction(ActiveDeliveryHudAction.PhotoCaptured(compressed))
                                }
                            }
                            override fun onError(exception: ImageCaptureException) { }
                        }
                    )
                }
            },
            onDeliverClick = { viewModel.onAction(ActiveDeliveryHudAction.DeliverClicked) },
            onFailClick = { viewModel.onAction(ActiveDeliveryHudAction.FailClicked) }
        )
    }
}

@Composable
private fun HudBottomControls(
    uiState: ActiveDeliveryHudUiState,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onDeliverClick: () -> Unit,
    onFailClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0A))
            .padding(horizontal = Dimens.grid_16, vertical = Dimens.grid_12),
        verticalArrangement = Arrangement.spacedBy(Dimens.grid_8)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.grid_12)
        ) {
            OutlinedButton(
                onClick = onCallClick,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(Dimens.grid_8),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
            ) {
                Icon(Icons.Default.Phone, null, Modifier.size(18.dp))
                Spacer(Modifier.width(Dimens.grid_4))
                Text("Call", style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
                onClick = onWhatsAppClick,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(Dimens.grid_8),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
            ) {
                Icon(Icons.Default.Chat, null, Modifier.size(18.dp))
                Spacer(Modifier.width(Dimens.grid_4))
                Text("WhatsApp", style = MaterialTheme.typography.labelLarge)
            }
        }

        Button(
            onClick = onCaptureClick,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(Dimens.grid_8),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.isPhotoCaptured) Color(0xFF2E7D32)
                else ColorTokens.DeliveryOrange
            )
        ) {
            Icon(
                if (uiState.isPhotoCaptured) Icons.Default.Check else Icons.Default.CameraAlt,
                null, Modifier.size(20.dp)
            )
            Spacer(Modifier.width(Dimens.grid_8))
            Text(
                if (uiState.isPhotoCaptured) "Photo Captured" else "Capture Photo",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.grid_12)
        ) {
            Button(
                onClick = onDeliverClick,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(Dimens.grid_8),
                enabled = uiState.isPhotoCaptured && !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp))
                Spacer(Modifier.width(Dimens.grid_4))
                Text("Deliver", style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
                onClick = onFailClick,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(Dimens.grid_8),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                border = BorderStroke(1.dp, Color(0xFFEF5350))
            ) {
                Icon(Icons.Default.Cancel, null, Modifier.size(20.dp))
                Spacer(Modifier.width(Dimens.grid_4))
                Text("Fail", style = MaterialTheme.typography.labelLarge)
            }
        }

        if (uiState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.grid_4),
                color = ColorTokens.DeliveryOrange,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }

        (uiState.userMessage as? com.smartcourier.core.domain.model.UiText.DynamicString)?.let { msg ->
            Text(
                text = msg.value,
                color = Color(0xFFEF5350),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = Dimens.grid_4)
            )
        }
    }
}

private suspend fun compressJpeg(inputPath: String): String = withContext(Dispatchers.IO) {
    val input = File(inputPath)
    val output = File(input.parent, "hud_${input.name}")
    val bitmap = BitmapFactory.decodeFile(inputPath)

    val (newW, newH) = if (bitmap.width > MAX_IMAGE_DIMENSION || bitmap.height > MAX_IMAGE_DIMENSION) {
        val ratio = MAX_IMAGE_DIMENSION.toFloat() / maxOf(bitmap.width, bitmap.height)
        (bitmap.width * ratio).toInt() to (bitmap.height * ratio).toInt()
    } else bitmap.width to bitmap.height

    val scaled = if (newW != bitmap.width || newH != bitmap.height)
        Bitmap.createScaledBitmap(bitmap, newW, newH, true) else bitmap

    FileOutputStream(output).use { out ->
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
    }

    if (scaled !== bitmap) scaled.recycle()
    bitmap.recycle()
    input.delete()

    output.toURI().toString()
}
