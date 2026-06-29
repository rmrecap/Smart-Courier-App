package com.smartcourier.feature.active_delivery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartcourier.core.domain.model.UiText
import com.smartcourier.core.ui.theme.Dimens
import com.smartcourier.core.ui.theme.components.SmartButton
import com.smartcourier.core.ui.theme.components.StopCard

@Composable
fun ActiveDeliveryScreen(
    routeId: String,
    deliveryId: String,
    onComplete: (String) -> Unit,
    onNavigate: (String, String) -> Unit = { _, _ -> },
    viewModel: ActiveDeliveryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(routeId, deliveryId) {
        viewModel.onAction(ActiveDeliveryAction.LoadRoute(routeId, deliveryId))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ActiveDeliveryEffect.NavigateToProof -> onNavigate(routeId, deliveryId)
                ActiveDeliveryEffect.DeliveryCompleted -> onComplete(routeId)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.grid_16),
        verticalArrangement = Arrangement.spacedBy(Dimens.grid_16)
    ) {
        LinearProgressIndicator(
            progress = { uiState.progress },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(text = "${uiState.currentIndex} of ${uiState.totalDeliveries}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        StopCard(
            index = uiState.currentIndex,
            address = uiState.currentAddress,
            status = "IN TRANSIT"
        )

        Spacer(modifier = Modifier.weight(1f))

        SmartButton(text = "Navigate", onClick = { viewModel.onAction(ActiveDeliveryAction.NavigateClicked) })
        SmartButton(text = "Mark Delivered", onClick = { viewModel.onAction(ActiveDeliveryAction.DeliveredClicked) })
        SmartButton(text = "Mark Failed", onClick = { viewModel.onAction(ActiveDeliveryAction.FailedClicked) })

        (uiState.userMessage as? UiText.DynamicString)?.let {
            Text(text = it.value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
