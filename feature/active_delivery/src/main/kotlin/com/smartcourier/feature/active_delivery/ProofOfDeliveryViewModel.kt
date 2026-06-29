package com.smartcourier.feature.active_delivery

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcourier.core.domain.model.Resource
import com.smartcourier.core.domain.model.UiText
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

sealed interface ProofAction {
    data class NotesChanged(val notes: String) : ProofAction
    data class ConfirmClicked(val deliveryId: String) : ProofAction
    data class PhotoCaptured(val uri: String) : ProofAction
    data object RetakeClicked : ProofAction
}

@Immutable
data class ProofUiState(
    val photoUri: String? = null,
    val notes: String = "",
    val isLoading: Boolean = false,
    val userMessage: UiText? = null
)

sealed interface ProofEffect {
    data object Confirmed : ProofEffect
}

@HiltViewModel
class ProofOfDeliveryViewModel @Inject constructor(
    private val completeDeliveryUseCase: CompleteDeliveryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProofUiState())
    val uiState: StateFlow<ProofUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ProofEffect>()
    val effect: SharedFlow<ProofEffect> = _effect.asSharedFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        _uiState.update { it.copy(isLoading = false, userMessage = UiText.DynamicString(exception.localizedMessage ?: "Unexpected error")) }
    }

    fun onAction(action: ProofAction) {
        when (action) {
            is ProofAction.NotesChanged -> onNotesChanged(action.notes)
            is ProofAction.ConfirmClicked -> onConfirmClicked(action.deliveryId)
            is ProofAction.PhotoCaptured -> onPhotoCaptured(action.uri)
            ProofAction.RetakeClicked -> onRetakeClicked()
        }
    }

    private fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    private fun onConfirmClicked(deliveryId: String) {
        val photoUri = _uiState.value.photoUri
        if (photoUri == null) {
            _uiState.update { it.copy(userMessage = UiText.DynamicString("Please capture a photo first.")) }
            return
        }
        _uiState.update { it.copy(isLoading = true, userMessage = null) }
        viewModelScope.launch(exceptionHandler) {
            completeDeliveryUseCase(deliveryId, photoUri, 0.0).collect { resource ->
                when (resource) {
                    is Resource.Loading -> { }
                    is Resource.Success -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _effect.emit(ProofEffect.Confirmed)
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false, userMessage = UiText.DynamicString(resource.exception.message ?: "Failed to complete delivery.")) }
                    }
                }
            }
        }
    }

    private fun onPhotoCaptured(uri: String) {
        _uiState.update { it.copy(photoUri = uri, userMessage = null) }
    }

    private fun onRetakeClicked() {
        _uiState.update { it.copy(photoUri = null, userMessage = null) }
    }
}
