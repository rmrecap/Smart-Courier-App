package com.smartcourier.feature.route_planner

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

const val INGESTION_ROUTE = "ingestion"
const val RIDER_MAP_ROUTE = "rider_map"

fun NavGraphBuilder.routePlannerNavGraph(navController: NavController) {
    composable(INGESTION_ROUTE) {
        IngestionScreen(onRouteOptimized = { routeId ->
            navController.navigate("route_preview/$routeId")
        })
    }
    composable(RIDER_MAP_ROUTE) {
        RiderMapScreen(onRouteOptimized = { routeId ->
            navController.navigate("route_preview/$routeId")
        })
    }
    composable(
        route = "route_preview/{routeId}",
        arguments = listOf(navArgument("routeId") { type = NavType.StringType })
    ) { backStackEntry ->
        val routeId = backStackEntry.arguments?.getString("routeId") ?: ""
        RoutePreviewScreen(
            routeId = routeId,
            onStartDelivery = { rId, dId ->
                navController.navigate("active_delivery/$rId/$dId")
            }
        )
    }
}
