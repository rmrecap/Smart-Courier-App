package com.smartcourier.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartcourier.core.domain.model.Delivery
import com.smartcourier.core.domain.model.DeliveryStatus
import com.smartcourier.core.domain.model.TodaySummary
import com.smartcourier.core.domain.model.UiText
import com.smartcourier.core.ui.theme.ColorTokens
import com.smartcourier.core.ui.theme.Dimens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RiderDashboardScreen(
    onNavigateToIngestion: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    viewModel: RiderDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                RiderDashboardEffect.NavigateToIngestion -> onNavigateToIngestion()
                is RiderDashboardEffect.NavigateToRoute -> onNavigateToRoute(effect.routeId)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorTokens.CyberDark)
            .padding(horizontal = Dimens.grid_16),
        verticalArrangement = Arrangement.spacedBy(Dimens.grid_16)
    ) {
        item { HeaderSection(uiState.todaySummary) }
        item { SummaryMatrix(uiState.todaySummary) }
        item { EarningsChartSection(uiState.weeklyEarnings) }
        item { ActionBar(onNavigateToIngestion, uiState, viewModel) }
        item { LedgerHeader() }
        items(uiState.historyLedger, key = { it.id }) { delivery ->
            HistoryRow(
                delivery = delivery,
                onTipClick = { viewModel.onAction(RiderDashboardAction.ShowTipModal(delivery.id)) }
            )
        }
        item { Spacer(Modifier.height(Dimens.grid_32)) }
    }

    if (uiState.showTipModal) {
        TipModal(
            deliveryId = uiState.tipTargetDeliveryId,
            tipInput = uiState.tipInput,
            onTipInputChange = { /* inline update in future */ },
            onSave = { amount ->
                viewModel.onAction(RiderDashboardAction.DismissTipModal(
                    uiState.tipTargetDeliveryId, amount
                ))
            },
            onDismiss = {
                viewModel.onAction(RiderDashboardAction.DismissTipModal("", 0.0))
            }
        )
    }
}

@Composable
private fun HeaderSection(summary: TodaySummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.grid_24)
    ) {
        Text(
            text = "Rider Dashboard",
            style = MaterialTheme.typography.headlineLarge,
            color = ColorTokens.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(Dimens.grid_4))
        Text(
            text = "AED ${String.format("%.2f", summary.totalEarnings)} earned today",
            style = MaterialTheme.typography.titleMedium,
            color = ColorTokens.DeliveryOrange
        )
        if (summary.totalTips > 0) {
            Text(
                text = "AED ${String.format("%.2f", summary.totalTips)} in tips",
                style = MaterialTheme.typography.bodySmall,
                color = ColorTokens.Success
            )
        }
    }
}

@Composable
private fun SummaryMatrix(summary: TodaySummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.grid_12)
    ) {
        MatrixCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Route,
            label = "Total",
            value = "${summary.totalDeliveries}",
            iconTint = ColorTokens.TextSecondary
        )
        MatrixCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.CheckCircle,
            label = "Completed",
            value = "${summary.completedCount}",
            iconTint = ColorTokens.Success
        )
        MatrixCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Cancel,
            label = "Failed",
            value = "${summary.failedCount}",
            iconTint = ColorTokens.Error
        )
    }
}

@Composable
private fun MatrixCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.radius_12),
        colors = CardDefaults.cardColors(containerColor = ColorTokens.CardDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.grid_12),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.height(24.dp))
            Spacer(Modifier.height(Dimens.grid_4))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = ColorTokens.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = ColorTokens.TextSecondary
            )
        }
    }
}

@Composable
private fun EarningsChartSection(weeklyEarnings: List<com.smartcourier.core.domain.model.DailyEarnings>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radius_12),
        colors = CardDefaults.cardColors(containerColor = ColorTokens.SurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(Dimens.grid_12)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Weekly Earnings",
                    style = MaterialTheme.typography.titleSmall,
                    color = ColorTokens.TextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(8.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ColorTokens.Success.copy(alpha = 0.7f))
                    )
                    Spacer(Modifier.width(Dimens.grid_4))
                    Text("Tips", color = ColorTokens.TextSecondary, fontSize = 10.sp)
                    Spacer(Modifier.width(Dimens.grid_12))
                    Box(
                        Modifier
                            .width(8.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ColorTokens.DeliveryOrange)
                    )
                    Spacer(Modifier.width(Dimens.grid_4))
                    Text("Earnings", color = ColorTokens.TextSecondary, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(Dimens.grid_4))
            EarningsChart(
                dailyEarnings = weeklyEarnings,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ActionBar(
    onNavigateToIngestion: () -> Unit,
    uiState: com.smartcourier.feature.dashboard.RiderDashboardUiState,
    viewModel: RiderDashboardViewModel
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.grid_12)
    ) {
        Button(
            onClick = onNavigateToIngestion,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(Dimens.radius_8),
            colors = ButtonDefaults.buttonColors(containerColor = ColorTokens.DeliveryOrange)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.height(20.dp))
            Spacer(Modifier.width(Dimens.grid_8))
            Text("New Route", fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = { viewModel.onAction(RiderDashboardAction.RefreshSummary) },
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(Dimens.radius_8),
            colors = ButtonDefaults.buttonColors(containerColor = ColorTokens.CardDark)
        ) {
            Icon(Icons.Default.MonetizationOn, null, tint = ColorTokens.Warning, modifier = Modifier.height(20.dp))
            Spacer(Modifier.width(Dimens.grid_8))
            Text("Tips", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LedgerHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.grid_8),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "History Ledger",
            style = MaterialTheme.typography.titleMedium,
            color = ColorTokens.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Today",
            style = MaterialTheme.typography.labelSmall,
            color = ColorTokens.TextSecondary
        )
    }
}

@Composable
private fun HistoryRow(
    delivery: Delivery,
    onTipClick: () -> Unit
) {
    val isDelivered = delivery.status == DeliveryStatus.DELIVERED.value
    val isFailed = delivery.status == DeliveryStatus.FAILED.value
    val timeAgo = formatTimeAgo(delivery.lastModifiedTimestamp)
    val statusColor = when {
        isDelivered -> ColorTokens.Success
        isFailed -> ColorTokens.Error
        else -> ColorTokens.Warning
    }
    val statusLabel = when {
        isDelivered -> "Delivered"
        isFailed -> "Failed"
        else -> delivery.status
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* navigate to delivery detail in future */ },
        shape = RoundedCornerShape(Dimens.radius_8),
        colors = CardDefaults.cardColors(containerColor = ColorTokens.CardDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.grid_12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = delivery.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorTokens.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(Dimens.grid_4))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("  ·  ", color = ColorTokens.Divider)
                    Text(
                        text = timeAgo,
                        color = ColorTokens.TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "AED ${delivery.earningsAed.toInt()}",
                    style = MaterialTheme.typography.titleSmall,
                    color = ColorTokens.DeliveryOrange,
                    fontWeight = FontWeight.Bold
                )
                if (delivery.tipAmountAed > 0) {
                    Text(
                        text = "+${delivery.tipAmountAed.toInt()} tip",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorTokens.Success
                    )
                }
                if (isDelivered) {
                    Spacer(Modifier.height(Dimens.grid_4))
                    TextButton(
                        onClick = onTipClick,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = Dimens.grid_8, vertical = 0.dp
                        )
                    ) {
                        Icon(
                            Icons.Default.Star, null,
                            tint = ColorTokens.Warning,
                            modifier = Modifier.height(14.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text("Tip", color = ColorTokens.Warning, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TipModal(
    deliveryId: String,
    tipInput: String,
    onTipInputChange: (String) -> Unit,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var input by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorTokens.SurfaceElevated,
        title = {
            Text("Log Tip", color = ColorTokens.TextPrimary)
        },
        text = {
            Column {
                Text(
                    "Delivery: ${deliveryId.take(12)}...",
                    color = ColorTokens.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(Dimens.grid_12))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Tip Amount (AED)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.radius_8)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(input.toDoubleOrNull() ?: 0.0) },
                enabled = input.toDoubleOrNull() != null && (input.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = ColorTokens.DeliveryOrange)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ColorTokens.TextSecondary)
            }
        }
    )
}

private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> {
            val instant = Instant.ofEpochMilli(timestamp)
            val local = instant.atZone(ZoneId.systemDefault()).toLocalDate()
            local.format(DateTimeFormatter.ofPattern("MMM d"))
        }
    }
}
