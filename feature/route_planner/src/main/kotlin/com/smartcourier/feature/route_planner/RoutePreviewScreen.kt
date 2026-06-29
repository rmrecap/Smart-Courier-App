package com.smartcourier.feature.route_planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartcourier.core.ui.theme.Dimens
import com.smartcourier.core.ui.theme.components.SmartButton
import com.smartcourier.core.ui.theme.components.StopCard

@Composable
fun RoutePreviewScreen(
    routeId: String,
    onStartDelivery: (String, String) -> Unit,
    viewModel: RoutePreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is RoutePreviewEffect.NavigateToActiveDelivery -> onStartDelivery(effect.routeId, effect.deliveryId)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(Dimens.grid_16)) {
        Text(text = "Route Preview", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(Dimens.grid_8))
        Text(text = "Distance: ${uiState.totalDistanceKm} km | Duration: ${uiState.totalDurationMin} min", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(Dimens.grid_16))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.grid_12)
        ) {
            itemsIndexed(uiState.stops, key = { index, _ -> index }) { index, stop ->
                StopCard(index = index + 1, address = stop, status = "PENDING")
            }
        }

        Spacer(modifier = Modifier.height(Dimens.grid_16))
        SmartButton(text = "Start Delivery", onClick = { viewModel.onAction(RoutePreviewAction.StartDelivery) })
    }
}
