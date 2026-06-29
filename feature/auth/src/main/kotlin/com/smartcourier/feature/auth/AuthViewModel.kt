package com.smartcourier.feature.auth

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcourier.core.domain.model.UiText
import com.smartcourier.core.domain.repository.AuthRepository
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

sealed interface AuthAction {
    data class PhoneNumberChanged(val value: String) : AuthAction
    data class OtpCodeChanged(val value: String) : AuthAction
    data object RequestOtp : AuthAction
    data object VerifyOtp : AuthAction
}

@Immutable
data class AuthUiState(
    val phoneNumber: String = "",
    val otpCode: String = "",
    val verificationId: String = "",
    val isOtpSent: Boolean = false,
    val isLoading: Boolean = false,
    val userMessage: UiText? = null
)

sealed interface AuthEffect {
    data object NavigateToDashboard : AuthEffect
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<AuthEffect>()
    val effect: SharedFlow<AuthEffect> = _effect.asSharedFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        _uiState.update { it.copy(isLoading = false, userMessage = UiText.DynamicString(exception.localizedMessage ?: "Unexpected error")) }
    }

    fun onAction(action: AuthAction) {
        when (action) {
            is AuthAction.PhoneNumberChanged -> onPhoneNumberChanged(action.value)
            is AuthAction.OtpCodeChanged -> onOtpCodeChanged(action.value)
            AuthAction.RequestOtp -> requestOtp()
            AuthAction.VerifyOtp -> verifyOtp()
        }
    }

    private fun onPhoneNumberChanged(value: String) {
        _uiState.update { it.copy(phoneNumber = value, userMessage = null) }
    }

    private fun onOtpCodeChanged(value: String) {
        _uiState.update { it.copy(otpCode = value, userMessage = null) }
    }

    private fun requestOtp() {
        val phone = _uiState.value.phoneNumber
        if (phone.length < 10) {
            _uiState.update { it.copy(userMessage = UiText.DynamicString("Enter a valid phone number")) }
            return
        }
        _uiState.update { it.copy(isLoading = true, userMessage = null) }

        viewModelScope.launch(exceptionHandler) {
            when (val result = authRepository.requestOtp(phone)) {
                is com.smartcourier.core.domain.model.Resource.Loading -> {}
                is com.smartcourier.core.domain.model.Resource.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        isOtpSent = true,
                        verificationId = result.data
                    )}
                }
                is com.smartcourier.core.domain.model.Resource.Error -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        userMessage = UiText.DynamicString(result.exception.message ?: "Failed to send code")
                    )}
                }
            }
        }
    }

    private fun verifyOtp() {
        val code = _uiState.value.otpCode
        val verificationId = _uiState.value.verificationId
        if (code.length != 6 && verificationId != "anonymous") {
            _uiState.update { it.copy(userMessage = UiText.DynamicString("Enter a valid 6-digit code")) }
            return
        }
        _uiState.update { it.copy(isLoading = true, userMessage = null) }

        viewModelScope.launch(exceptionHandler) {
            when (val result = authRepository.verifyOtp(verificationId, code)) {
                is com.smartcourier.core.domain.model.Resource.Loading -> {}
                is com.smartcourier.core.domain.model.Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.emit(AuthEffect.NavigateToDashboard)
                }
                is com.smartcourier.core.domain.model.Resource.Error -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        userMessage = UiText.DynamicString(result.exception.message ?: "Verification failed")
                    )}
                }
            }
        }
    }
}
