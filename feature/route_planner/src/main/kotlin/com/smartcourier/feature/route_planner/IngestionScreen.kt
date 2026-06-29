package com.smartcourier.feature.route_planner

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartcourier.core.domain.model.UiText
import com.smartcourier.core.ui.theme.Dimens
import com.smartcourier.core.ui.theme.components.SmartButton
import com.smartcourier.core.ui.theme.components.SmartTextField

@Composable
fun IngestionScreen(
    onRouteOptimized: (String) -> Unit,
    viewModel: IngestionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is IngestionEffect.NavigateToPreview -> onRouteOptimized(effect.routeId)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.grid_16),
        verticalArrangement = Arrangement.spacedBy(Dimens.grid_16)
    ) {
        Text(text = "Paste Addresses", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(text = "One per line. Duplicates will be removed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        SmartTextField(
            value = uiState.rawText,
            onValueChange = { viewModel.onAction(IngestionAction.RawTextChanged(it)) },
            label = "Addresses",
            placeholder = "Enter addresses here...",
            singleLine = false,
            minLines = 8,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "${uiState.deliveryCount} addresses detected",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SmartButton(
            text = "Optimize Route",
            onClick = { viewModel.onAction(IngestionAction.OptimizeClicked) },
            enabled = uiState.deliveryCount >= 2 && !uiState.isLoading
        )

        if (uiState.isLoading) {
            Text(text = "Optimizing...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        (uiState.userMessage as? UiText.DynamicString)?.let {
            Text(text = it.value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
