package com.smartcourier.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.smartcourier.core.ui.theme.components.StatCard
import com.smartcourier.core.ui.theme.components.StopCard

@Composable
fun HomeDashboardScreen(
    onNavigateToIngestion: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    viewModel: HomeDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                DashboardEffect.NavigateToIngestion -> onNavigateToIngestion()
                is DashboardEffect.NavigateToRoute -> onNavigateToRoute(effect.routeId)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.grid_16),
        verticalArrangement = Arrangement.spacedBy(Dimens.grid_16)
    ) {
        item {
            Text(
                text = "Good ${if (java.util.Calendar.getInstance().get(java.util.Calendar.AM_PM) == 0) "Morning" else "Evening"}, ${uiState.userName}",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.grid_12)) {
                StatCard(modifier = Modifier.weight(1f), title = "Today's Earnings", value = "AED ${uiState.todayEarnings}")
                StatCard(modifier = Modifier.weight(1f), title = "Deliveries", value = "${uiState.completedDeliveries}")
            }
        }
        item {
            SmartButton(text = "New Route", onClick = { viewModel.onAction(DashboardAction.NewRouteClicked) })
        }
        item {
            Text(text = "Active Routes", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        items(uiState.recentRoutes, key = { it }) { route ->
            StopCard(
                index = 0,
                address = route,
                status = "ACTIVE",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
