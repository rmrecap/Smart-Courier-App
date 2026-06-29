package com.smartcourier.feature.route_planner

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcourier.core.domain.model.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RoutePreviewAction {
    data object StartDelivery : RoutePreviewAction
}

@Immutable
data class RoutePreviewUiState(
    val routeId: String = "",
    val totalDistanceKm: String = "0.0",
    val totalDurationMin: String = "0",
    val stops: ImmutableList<String> = persistentListOf("Al Barsha, Dubai", "Dubai Marina", "JLT Cluster Y", "Business Bay"),
    val userMessage: UiText? = null
)

sealed interface RoutePreviewEffect {
    data class NavigateToActiveDelivery(val routeId: String, val deliveryId: String) : RoutePreviewEffect
}

@HiltViewModel
class RoutePreviewViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(RoutePreviewUiState())
    val uiState: StateFlow<RoutePreviewUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<RoutePreviewEffect>()
    val effect: SharedFlow<RoutePreviewEffect> = _effect.asSharedFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        _uiState.update { it.copy(userMessage = UiText.DynamicString(exception.localizedMessage ?: "Unexpected error")) }
    }

    fun onAction(action: RoutePreviewAction) {
        when (action) {
            RoutePreviewAction.StartDelivery -> onStartDelivery()
        }
    }

    private fun onStartDelivery() {
        viewModelScope.launch(exceptionHandler) {
            _effect.emit(RoutePreviewEffect.NavigateToActiveDelivery(_uiState.value.routeId, "del_1"))
        }
    }
}
