package com.smartcourier.feature.auth

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val AUTH_ROUTE = "auth"

fun NavGraphBuilder.authNavGraph(navController: NavController) {
    composable(AUTH_ROUTE) {
        AuthScreen(onAuthSuccess = {
            navController.navigate("dashboard") {
                popUpTo(AUTH_ROUTE) { inclusive = true }
            }
        })
    }
}
