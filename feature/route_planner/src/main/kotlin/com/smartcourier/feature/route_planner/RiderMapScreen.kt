package com.smartcourier.feature.route_planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartcourier.core.domain.model.UiText
import com.smartcourier.core.ui.theme.ColorTokens
import com.smartcourier.core.ui.theme.Dimens
import com.smartcourier.core.ui.theme.components.SmartButton
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun RiderMapScreen(
    onRouteOptimized: (String) -> Unit,
    viewModel: RiderMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is RiderMapEffect.NavigateToPreview -> onRouteOptimized(effect.routeId)
            }
        }
    }

    Configuration.getInstance().apply {
        userAgentValue = context.packageName
        osmdroidBasePath = context.cacheDir
        osmdroidTileCache = context.cacheDir
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapLayer(
            centerLat = uiState.mapCenter.latitude,
            centerLng = uiState.mapCenter.longitude,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.grid_16),
            verticalArrangement = Arrangement.spacedBy(Dimens.grid_12)
        ) {
            CountryPickerChip(
                selectedCountry = uiState.selectedCountry,
                onCountrySelected = { viewModel.onAction(RiderMapAction.CountrySelected(it)) }
            )

            Spacer(modifier = Modifier.weight(1f))

            AddressInputCard(
                rawText = uiState.rawText,
                deliveryCount = uiState.deliveryCount,
                isLoading = uiState.isLoading,
                onTextChange = { viewModel.onAction(RiderMapAction.RawTextChanged(it)) },
                onOptimize = {
                    focusManager.clearFocus()
                    viewModel.onAction(RiderMapAction.OptimizeClicked)
                }
            )

            if (uiState.userMessage != null) {
                val msg = uiState.userMessage
                if (msg is UiText.DynamicString) {
                    Text(
                        text = msg.value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.radius_8))
                            .background(ColorTokens.Error.copy(alpha = 0.1f))
                            .padding(Dimens.grid_12)
                    )
                }
            }
        }
    }
}

@Composable
private fun MapLayer(
    centerLat: Double,
    centerLng: Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(12.0)
                controller.setCenter(GeoPoint(centerLat, centerLng))
                mapView = this
            }
        },
        modifier = modifier
    )

    DisposableEffect(Unit) {
        onDispose {
            mapView?.onDetach()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CountryPickerChip(
    selectedCountry: String,
    onCountrySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = COUNTRIES.firstOrNull { it.first == selectedCountry }?.second ?: selectedCountry

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.grid_8),
        verticalArrangement = Arrangement.spacedBy(Dimens.grid_8)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.radius_12))
                    .background(ColorTokens.SurfaceDark)
                    .border(1.dp, ColorTokens.DeliveryOrange, RoundedCornerShape(Dimens.radius_12))
                    .clickable { expanded = true }
                    .padding(horizontal = Dimens.grid_16, vertical = Dimens.grid_8),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = ColorTokens.DeliveryOrange
                )
                Spacer(modifier = Modifier.width(Dimens.grid_4))
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = "Select country",
                    tint = ColorTokens.DeliveryOrange,
                    modifier = Modifier.size(Dimens.iconSize)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(ColorTokens.SurfaceDark)
            ) {
                COUNTRIES.forEach { (code, name) ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = name, style = MaterialTheme.typography.bodyMedium)
                                if (code == selectedCountry) {
                                    Spacer(modifier = Modifier.width(Dimens.grid_8))
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = ColorTokens.DeliveryOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            onCountrySelected(code)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddressInputCard(
    rawText: String,
    deliveryCount: Int,
    isLoading: Boolean,
    onTextChange: (String) -> Unit,
    onOptimize: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.radius_16))
            .background(ColorTokens.CyberDark.copy(alpha = 0.92f))
            .padding(Dimens.grid_16),
        verticalArrangement = Arrangement.spacedBy(Dimens.grid_12)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Route Planner Terminal",
                style = MaterialTheme.typography.titleMedium,
                color = ColorTokens.DeliveryOrange
            )
            Icon(
                imageVector = Icons.Outlined.ContentPaste,
                contentDescription = "Paste addresses",
                tint = ColorTokens.TextSecondary,
                modifier = Modifier.size(Dimens.iconSize)
            )
        }

        OutlinedTextField(
            value = rawText,
            onValueChange = onTextChange,
            label = { Text("Paste addresses (one per line)") },
            placeholder = { Text("Al Muteena St, Dubai\nAl Rebat St, Sharjah\n...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            singleLine = false,
            minLines = 5,
            shape = RoundedCornerShape(Dimens.radius_12),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ColorTokens.DeliveryOrange,
                unfocusedBorderColor = ColorTokens.Divider,
                focusedContainerColor = ColorTokens.SurfaceDark.copy(alpha = 0.6f),
                unfocusedContainerColor = ColorTokens.SurfaceDark.copy(alpha = 0.4f)
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onOptimize() })
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "$deliveryCount stops detected",
                style = MaterialTheme.typography.bodySmall,
                color = ColorTokens.TextSecondary
            )
            if (isLoading) {
                Text(
                    text = "Optimizing...",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTokens.DeliveryOrange
                )
            }
        }

        SmartButton(
            text = "Optimize Route",
            onClick = onOptimize,
            enabled = deliveryCount >= 2 && !isLoading
        )
    }
}
