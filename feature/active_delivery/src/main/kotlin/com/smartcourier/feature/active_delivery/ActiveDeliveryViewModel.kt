package com.smartcourier.feature.active_delivery

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

sealed interface ActiveDeliveryAction {
    data class LoadRoute(val routeId: String, val deliveryId: String) : ActiveDeliveryAction
    data object NavigateClicked : ActiveDeliveryAction
    data object DeliveredClicked : ActiveDeliveryAction
    data object FailedClicked : ActiveDeliveryAction
}

@Immutable
data class ActiveDeliveryUiState(
    val routeId: String = "",
    val deliveryId: String = "",
    val currentIndex: Int = 1,
    val totalDeliveries: Int = 0,
    val currentAddress: String = "",
    val progress: Float = 0f,
    val isLoading: Boolean = false,
    val userMessage: UiText? = null
)

sealed interface ActiveDeliveryEffect {
    data object NavigateToProof : ActiveDeliveryEffect
    data object DeliveryCompleted : ActiveDeliveryEffect
}

@HiltViewModel
class ActiveDeliveryViewModel @Inject constructor(
    private val deliveryRepository: DeliveryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveDeliveryUiState())
    val uiState: StateFlow<ActiveDeliveryUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ActiveDeliveryEffect>()
    val effect: SharedFlow<ActiveDeliveryEffect> = _effect.asSharedFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        _uiState.update { it.copy(isLoading = false, userMessage = UiText.DynamicString(exception.localizedMessage ?: "Unexpected error")) }
    }

    fun onAction(action: ActiveDeliveryAction) {
        when (action) {
            is ActiveDeliveryAction.LoadRoute -> loadDelivery(action.routeId, action.deliveryId)
            ActiveDeliveryAction.NavigateClicked -> { }
            ActiveDeliveryAction.DeliveredClicked -> onDeliveredClicked()
            ActiveDeliveryAction.FailedClicked -> { }
        }
    }

    private fun loadDelivery(routeId: String, deliveryId: String) {
        _uiState.update { it.copy(routeId = routeId, deliveryId = deliveryId, isLoading = true) }
        viewModelScope.launch(exceptionHandler) {
            val delivery = deliveryRepository.getDelivery(deliveryId)
            if (delivery != null) {
                var total = 0
                deliveryRepository.observeDeliveriesForRoute(routeId).collect { list ->
                    total = list.size
                    _uiState.update {
                        it.copy(
                            currentIndex = delivery.index,
                            totalDeliveries = total,
                            currentAddress = delivery.address,
                            progress = if (total > 0) delivery.index.toFloat() / total else 0f,
                            isLoading = false
                        )
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false, userMessage = UiText.DynamicString("Delivery not found.")) }
            }
        }
    }

    private fun onDeliveredClicked() {
        viewModelScope.launch(exceptionHandler) {
            _effect.emit(ActiveDeliveryEffect.NavigateToProof)
        }
    }
}
