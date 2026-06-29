package com.smartcourier.app

import androidx.compose.runtime.Composable
import com.smartcourier.core.ui.theme.SmartCourierTheme

@Composable
fun SmartCourierApp() {
    SmartCourierTheme {
        AppNavGraph()
    }
}
