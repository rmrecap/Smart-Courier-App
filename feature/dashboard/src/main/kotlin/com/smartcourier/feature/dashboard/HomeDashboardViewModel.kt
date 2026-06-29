package com.smartcourier.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Immutable
import com.smartcourier.core.domain.model.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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

sealed interface DashboardAction {
    data object NewRouteClicked : DashboardAction
}

@Immutable
data class DashboardUiState(
    val userName: String = "Courier",
    val todayEarnings: Double = 0.0,
    val completedDeliveries: Int = 0,
    val recentRoutes: ImmutableList<String> = persistentListOf(),
    val userMessage: UiText? = null
)

sealed interface DashboardEffect {
    data object NavigateToIngestion : DashboardEffect
    data class NavigateToRoute(val routeId: String) : DashboardEffect
}

@HiltViewModel
class HomeDashboardViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(recentRoutes = persistentListOf("Route #1")))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<DashboardEffect>()
    val effect: SharedFlow<DashboardEffect> = _effect.asSharedFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        viewModelScope.launch {
            _uiState.update { it.copy(userMessage = UiText.DynamicString(exception.localizedMessage ?: "Unexpected error")) }
        }
    }

    fun onAction(action: DashboardAction) {
        when (action) {
            DashboardAction.NewRouteClicked -> onNewRouteClicked()
        }
    }

    private fun onNewRouteClicked() {
        viewModelScope.launch(exceptionHandler) {
            _effect.emit(DashboardEffect.NavigateToIngestion)
        }
    }
}
