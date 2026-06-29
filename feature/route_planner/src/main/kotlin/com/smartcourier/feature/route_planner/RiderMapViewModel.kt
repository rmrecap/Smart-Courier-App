package com.smartcourier.feature.route_planner

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcourier.core.domain.model.Coordinate
import com.smartcourier.core.domain.model.DomainResult
import com.smartcourier.core.domain.model.UiText
import com.smartcourier.core.domain.usecase.OptimizeRouteUseCase
import com.smartcourier.core.domain.usecase.ParseAddressesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface RiderMapAction {
    data class RawTextChanged(val text: String) : RiderMapAction
    data class CountrySelected(val code: String) : RiderMapAction
    data object OptimizeClicked : RiderMapAction
}

@Immutable
data class RiderMapUiState(
    val rawText: String = "",
    val deliveryCount: Int = 0,
    val isLoading: Boolean = false,
    val userMessage: UiText? = null,
    val selectedCountry: String = "AE",
    val mapCenter: Coordinate = DEFAULT_HUB
)

sealed interface RiderMapEffect {
    data class NavigateToPreview(val routeId: String) : RiderMapEffect
}

private val DEFAULT_HUB = Coordinate(25.2048, 55.2708)

val COUNTRIES = listOf(
    "AE" to "UAE",
    "SA" to "Saudi Arabia",
    "OM" to "Oman",
    "PK" to "Pakistan",
    "PH" to "Philippines"
)

@HiltViewModel
class RiderMapViewModel @Inject constructor(
    private val parseAddressesUseCase: ParseAddressesUseCase,
    private val optimizeRouteUseCase: OptimizeRouteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RiderMapUiState())
    val uiState: StateFlow<RiderMapUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<RiderMapEffect>()
    val effect: SharedFlow<RiderMapEffect> = _effect.asSharedFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        _uiState.update { it.copy(isLoading = false, userMessage = UiText.DynamicString(exception.localizedMessage ?: "Unexpected error")) }
    }

    fun onAction(action: RiderMapAction) {
        when (action) {
            is RiderMapAction.RawTextChanged -> onRawTextChanged(action.text)
            is RiderMapAction.CountrySelected -> onCountrySelected(action.code)
            RiderMapAction.OptimizeClicked -> onOptimizeClicked()
        }
    }

    private fun onRawTextChanged(text: String) {
        val deliveries = parseAddressesUseCase.invoke(text, "")
        _uiState.update { it.copy(rawText = text, deliveryCount = deliveries.size, userMessage = null) }
    }

    private fun onCountrySelected(code: String) {
        _uiState.update { it.copy(selectedCountry = code) }
    }

    private fun onOptimizeClicked() {
        val count = _uiState.value.deliveryCount
        if (count < 2) {
            _uiState.update { it.copy(userMessage = UiText.DynamicString("At least 2 addresses required")) }
            return
        }
        _uiState.update { it.copy(isLoading = true, userMessage = null) }

        viewModelScope.launch(exceptionHandler) {
            val parsed = parseAddressesUseCase.invoke(_uiState.value.rawText, "")
            val deliveries = parsed.mapNotNull { parsedAddr ->
                val coord = parseAddressesUseCase.estimateCoordinates(parsedAddr.addressRaw)
                coord?.let { parseAddressesUseCase.toDelivery(parsedAddr, parsedAddr.index, it, _uiState.value.selectedCountry.lowercase()) }
            }
            if (deliveries.size < 2) {
                _uiState.update { it.copy(isLoading = false, userMessage = UiText.DynamicString("At least 2 valid addresses with coordinates are required.")) }
                return@launch
            }
            val userId = "${_uiState.value.selectedCountry.lowercase()}_rider_${UUID.randomUUID().toString().take(8)}"
            when (val result = optimizeRouteUseCase(userId, deliveries, DEFAULT_HUB)) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.emit(RiderMapEffect.NavigateToPreview(result.data.routeId))
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, userMessage = UiText.DynamicString(result.exception.message ?: "Route optimization failed.")) }
                }
            }
        }
    }
}
