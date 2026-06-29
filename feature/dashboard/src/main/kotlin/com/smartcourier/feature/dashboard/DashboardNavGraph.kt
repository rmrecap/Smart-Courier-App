package com.smartcourier.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val DASHBOARD_ROUTE = "dashboard"

fun NavGraphBuilder.dashboardNavGraph(navController: NavController) {
    composable(DASHBOARD_ROUTE) {
        RiderDashboardScreen(
            onNavigateToIngestion = { navController.navigate("ingestion") },
            onNavigateToRoute = { routeId -> navController.navigate("route_preview/$routeId") }
        )
    }
}
