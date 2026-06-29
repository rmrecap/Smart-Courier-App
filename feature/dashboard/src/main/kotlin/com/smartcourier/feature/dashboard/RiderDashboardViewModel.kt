package com.smartcourier.feature.dashboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcourier.core.domain.model.DailyEarnings
import com.smartcourier.core.domain.model.Delivery
import com.smartcourier.core.domain.model.TodaySummary
import com.smartcourier.core.domain.model.UiText
import com.smartcourier.core.domain.repository.DeliveryRepository
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
import javax.inject.Inject

sealed interface RiderDashboardAction {
    data object NewRouteClicked : RiderDashboardAction
    data object RefreshSummary : RiderDashboardAction
    data class ShowTipModal(val deliveryId: String) : RiderDashboardAction
    data class DismissTipModal(val deliveryId: String, val amount: Double) : RiderDashboardAction
    data object DismissMessage : RiderDashboardAction
}

@Immutable
data class RiderDashboardUiState(
    val userName: String = "Courier",
    val todaySummary: TodaySummary = TodaySummary(),
    val weeklyEarnings: List<DailyEarnings> = emptyList(),
    val historyLedger: List<Delivery> = emptyList(),
    val showTipModal: Boolean = false,
    val tipTargetDeliveryId: String = "",
    val tipInput: String = "",
    val isLoading: Boolean = false,
    val userMessage: UiText? = null
)

sealed interface RiderDashboardEffect {
    data object NavigateToIngestion : RiderDashboardEffect
    data class NavigateToRoute(val routeId: String) : RiderDashboardEffect
}

@HiltViewModel
class RiderDashboardViewModel @Inject constructor(
    private val deliveryRepository: DeliveryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RiderDashboardUiState())
    val uiState: StateFlow<RiderDashboardUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<RiderDashboardEffect>()
    val effect: SharedFlow<RiderDashboardEffect> = _effect.asSharedFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        _uiState.update { it.copy(
            isLoading = false,
            userMessage = UiText.DynamicString(exception.localizedMessage ?: "Unexpected error")
        )}
    }

    init {
        loadSummary()
        observeWeeklyEarnings()
        observeHistoryLedger()
    }

    fun onAction(action: RiderDashboardAction) {
        when (action) {
            RiderDashboardAction.NewRouteClicked -> onNewRouteClicked()
            RiderDashboardAction.RefreshSummary -> loadSummary()
            is RiderDashboardAction.ShowTipModal -> onShowTipModal(action.deliveryId)
            is RiderDashboardAction.DismissTipModal -> onDismissTipModal(action.deliveryId, action.amount)
            RiderDashboardAction.DismissMessage -> dismissMessage()
        }
    }

    private fun loadSummary(countryCode: String = "ae") {
        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isLoading = true) }
            val summary = deliveryRepository.getTodaySummary(countryCode)
            _uiState.update { it.copy(todaySummary = summary, isLoading = false) }
        }
    }

    private fun observeWeeklyEarnings(countryCode: String = "ae") {
        viewModelScope.launch(exceptionHandler) {
            deliveryRepository.observeWeeklyEarnings(countryCode).collect { earnings ->
                _uiState.update { it.copy(weeklyEarnings = earnings) }
            }
        }
    }

    private fun observeHistoryLedger(countryCode: String = "ae") {
        viewModelScope.launch(exceptionHandler) {
            deliveryRepository.observeHistoryLedger(countryCode).collect { ledger ->
                _uiState.update { it.copy(historyLedger = ledger) }
            }
        }
    }

    private fun onNewRouteClicked() {
        viewModelScope.launch(exceptionHandler) {
            _effect.emit(RiderDashboardEffect.NavigateToIngestion)
        }
    }

    private fun onShowTipModal(deliveryId: String) {
        _uiState.update { it.copy(showTipModal = true, tipTargetDeliveryId = deliveryId, tipInput = "") }
    }

    private fun onDismissTipModal(deliveryId: String, amount: Double) {
        _uiState.update { it.copy(showTipModal = false) }
        if (amount > 0 && deliveryId.isNotBlank()) {
            viewModelScope.launch(exceptionHandler) {
                deliveryRepository.logTip(deliveryId, amount)
                loadSummary()
            }
        }
    }

    private fun dismissMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
