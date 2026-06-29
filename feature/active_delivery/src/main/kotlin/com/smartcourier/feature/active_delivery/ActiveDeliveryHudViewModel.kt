package com.smartcourier.feature.active_delivery

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcourier.core.domain.model.Resource
import com.smartcourier.core.domain.model.UiText
import com.smartcourier.core.domain.repository.DeliveryRepository
import com.smartcourier.core.domain.usecase.CompleteDeliveryUseCase
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

sealed interface ActiveDeliveryHudAction {
    data class LoadRoute(val routeId: String, val deliveryId: String) : ActiveDeliveryHudAction
    data object CallClicked : ActiveDeliveryHudAction
    data object WhatsAppClicked : ActiveDeliveryHudAction
    data class PhotoCaptured(val uri: String) : ActiveDeliveryHudAction
    data object DeliverClicked : ActiveDeliveryHudAction
    data object FailClicked : ActiveDeliveryHudAction
    data object DismissMessage : ActiveDeliveryHudAction
}

@Immutable
data class ActiveDeliveryHudUiState(
    val routeId: String = "",
    val deliveryId: String = "",
    val currentIndex: Int = 1,
    val totalDeliveries: Int = 1,
    val currentAddress: String = "",
    val recipientName: String = "",
    val recipientPhone: String = "",
    val trackingToken: String = "",
    val earningsAed: Double = 0.0,
    val countryCode: String = "ae",
    val progress: Float = 0f,
    val photoUri: String? = null,
    val isPhotoCaptured: Boolean = false,
    val isLoading: Boolean = false,
    val userMessage: UiText? = null
)

sealed interface ActiveDeliveryHudEffect {
    data class OpenDialer(val phone: String) : ActiveDeliveryHudEffect
    data class OpenWhatsApp(val phone: String, val message: String) : ActiveDeliveryHudEffect
    data object NavigateToDashboard : ActiveDeliveryHudEffect
}

@HiltViewModel
class ActiveDeliveryHudViewModel @Inject constructor(
    private val deliveryRepository: DeliveryRepository,
    private val completeDeliveryUseCase: CompleteDeliveryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveDeliveryHudUiState())
    val uiState: StateFlow<ActiveDeliveryHudUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ActiveDeliveryHudEffect>()
    val effect: SharedFlow<ActiveDeliveryHudEffect> = _effect.asSharedFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        _uiState.update { it.copy(
            isLoading = false,
            userMessage = UiText.DynamicString(exception.localizedMessage ?: "Unexpected error")
        )}
    }

    fun onAction(action: ActiveDeliveryHudAction) {
        when (action) {
            is ActiveDeliveryHudAction.LoadRoute -> loadDelivery(action.routeId, action.deliveryId)
            ActiveDeliveryHudAction.CallClicked -> onCallClicked()
            ActiveDeliveryHudAction.WhatsAppClicked -> onWhatsAppClicked()
            is ActiveDeliveryHudAction.PhotoCaptured -> onPhotoCaptured(action.uri)
            ActiveDeliveryHudAction.DeliverClicked -> onDeliverClicked()
            ActiveDeliveryHudAction.FailClicked -> onFailClicked()
            ActiveDeliveryHudAction.DismissMessage -> dismissMessage()
        }
    }

    private fun loadDelivery(routeId: String, deliveryId: String) {
        _uiState.update { it.copy(routeId = routeId, deliveryId = deliveryId, isLoading = true) }
        viewModelScope.launch(exceptionHandler) {
            val delivery = deliveryRepository.getDelivery(deliveryId)
            if (delivery != null) {
                deliveryRepository.observeDeliveriesForRoute(routeId).collect { list ->
                    _uiState.update {
                        it.copy(
                            currentIndex = delivery.index,
                            totalDeliveries = list.size,
                            currentAddress = delivery.address,
                            recipientName = delivery.recipientName,
                            recipientPhone = delivery.recipientPhone,
                            trackingToken = delivery.trackingToken,
                            earningsAed = delivery.earningsAed,
                            countryCode = delivery.countryCode,
                            progress = if (list.isNotEmpty()) delivery.index.toFloat() / list.size else 0f,
                            isLoading = false
                        )
                    }
                }
            } else {
                _uiState.update { it.copy(
                    isLoading = false,
                    userMessage = UiText.DynamicString("Delivery not found.")
                )}
            }
        }
    }

    private fun onCallClicked() {
        val phone = _uiState.value.recipientPhone
        if (phone.isBlank()) {
            _uiState.update { it.copy(userMessage = UiText.DynamicString("No phone number available.")) }
            return
        }
        viewModelScope.launch {
            _effect.emit(ActiveDeliveryHudEffect.OpenDialer(phone))
        }
    }

    private fun onWhatsAppClicked() {
        val phone = _uiState.value.recipientPhone
        if (phone.isBlank()) {
            _uiState.update { it.copy(userMessage = UiText.DynamicString("No phone number available.")) }
            return
        }
        val token = _uiState.value.trackingToken
        val message = "Your package from Smart Courier is on the way! Track here: https://smart-courier-app-e8624.firebaseapp.com/tracking.html?token=${token}"
        viewModelScope.launch {
            _effect.emit(ActiveDeliveryHudEffect.OpenWhatsApp(phone, message))
        }
    }

    private fun onPhotoCaptured(uri: String) {
        _uiState.update { it.copy(photoUri = uri, isPhotoCaptured = true, userMessage = null) }
    }

    private fun onDeliverClicked() {
        val photoUri = _uiState.value.photoUri
        if (photoUri == null) {
            _uiState.update { it.copy(userMessage = UiText.DynamicString("Capture a photo first.")) }
            return
        }
        val deliveryId = _uiState.value.deliveryId
        val earnings = _uiState.value.earningsAed
        _uiState.update { it.copy(isLoading = true, userMessage = null) }

        viewModelScope.launch(exceptionHandler) {
            completeDeliveryUseCase(deliveryId, photoUri, earnings).collect { resource ->
                when (resource) {
                    is Resource.Loading -> { }
                    is Resource.Success -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _effect.emit(ActiveDeliveryHudEffect.NavigateToDashboard)
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(
                            isLoading = false,
                            userMessage = UiText.DynamicString(resource.exception.message ?: "Failed to complete delivery.")
                        )}
                    }
                }
            }
        }
    }

    private fun onFailClicked() {
        val deliveryId = _uiState.value.deliveryId
        _uiState.update { it.copy(isLoading = true, userMessage = null) }

        viewModelScope.launch(exceptionHandler) {
            deliveryRepository.markDeliveryFailed(deliveryId)
            _uiState.update { it.copy(isLoading = false) }
            _effect.emit(ActiveDeliveryHudEffect.NavigateToDashboard)
        }
    }

    private fun dismissMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
