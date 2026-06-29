package com.smartcourier.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartcourier.core.ui.theme.Dimens
import com.smartcourier.core.ui.theme.components.SmartButton
import com.smartcourier.core.ui.theme.components.SmartTextField

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AuthEffect.NavigateToDashboard -> onAuthSuccess()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.grid_24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Smart Courier", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(Dimens.grid_48))

        SmartTextField(
            value = uiState.phoneNumber,
            onValueChange = { viewModel.onAction(AuthAction.PhoneNumberChanged(it)) },
            label = "Phone Number",
            placeholder = "+971 5X XXX XXXX"
        )
        Spacer(modifier = Modifier.height(Dimens.grid_16))

        if (uiState.isOtpSent) {
            SmartTextField(
                value = uiState.otpCode,
                onValueChange = { viewModel.onAction(AuthAction.OtpCodeChanged(it)) },
                label = "Verification Code",
                placeholder = "123456"
            )
            Spacer(modifier = Modifier.height(Dimens.grid_16))
        }

        SmartButton(
            text = if (uiState.isOtpSent) "Verify" else "Get Code",
            onClick = { viewModel.onAction(if (uiState.isOtpSent) AuthAction.VerifyOtp else AuthAction.RequestOtp) },
            enabled = !uiState.isLoading
        )
        if (uiState.isLoading) Text(text = "Loading...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        uiState.userMessage?.let {
            Text(text = (it as? com.smartcourier.core.domain.model.UiText.DynamicString)?.value ?: "Error", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
