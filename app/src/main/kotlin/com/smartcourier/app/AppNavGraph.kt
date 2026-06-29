package com.smartcourier.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.smartcourier.feature.active_delivery.activeDeliveryNavGraph
import com.smartcourier.feature.auth.AUTH_ROUTE
import com.smartcourier.feature.auth.authNavGraph
import com.smartcourier.feature.dashboard.DASHBOARD_ROUTE
import com.smartcourier.feature.dashboard.dashboardNavGraph
import com.smartcourier.feature.route_planner.routePlannerNavGraph

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AUTH_ROUTE) {
        authNavGraph(navController)
        dashboardNavGraph(navController)
        routePlannerNavGraph(navController)
        activeDeliveryNavGraph(navController)
    }
}
