package com.smartcourier.feature.active_delivery

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartcourier.core.domain.model.UiText
import com.smartcourier.core.ui.theme.Dimens
import com.smartcourier.core.ui.theme.components.SmartButton
import com.smartcourier.core.ui.theme.components.SmartTextField
import java.io.File

@Composable
fun ProofOfDeliveryScreen(
    deliveryId: String,
    onConfirmed: () -> Unit,
    viewModel: ProofOfDeliveryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ProofEffect.Confirmed -> onConfirmed()
            }
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val cameraExecutor = remember { ContextCompat.getMainExecutor(context) }

    Column(
        modifier = Modifier.fillMaxSize().padding(Dimens.grid_16),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Proof of Delivery", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(Dimens.grid_16))

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                    }, cameraExecutor)
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(Dimens.grid_16))

        SmartButton(
            text = "Capture Photo",
            onClick = {
                val photoFile = File(context.cacheDir, "proof_${deliveryId}_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                imageCapture.takePicture(outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        viewModel.onAction(ProofAction.PhotoCaptured(photoFile.toURI().toString()))
                    }
                    override fun onError(exception: ImageCaptureException) { }
                })
            }
        )

        if (uiState.photoUri != null) {
            Text(text = "Photo captured", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(Dimens.grid_16))
        SmartTextField(
            value = uiState.notes,
            onValueChange = { viewModel.onAction(ProofAction.NotesChanged(it)) },
            label = "Notes (optional)"
        )
        Spacer(modifier = Modifier.height(Dimens.grid_16))
        SmartButton(
            text = "Confirm Delivery",
            onClick = { viewModel.onAction(ProofAction.ConfirmClicked(deliveryId)) },
            enabled = !uiState.isLoading
        )
        SmartButton(text = "Retake Photo", onClick = { viewModel.onAction(ProofAction.RetakeClicked) })

        (uiState.userMessage as? UiText.DynamicString)?.let {
            Text(text = it.value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
