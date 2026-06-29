package com.smartcourier.feature.active_delivery

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

fun NavGraphBuilder.activeDeliveryNavGraph(navController: NavController) {
    composable(
        route = "active_delivery/{routeId}/{deliveryId}",
        arguments = listOf(
            navArgument("routeId") { type = NavType.StringType },
            navArgument("deliveryId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val routeId = backStackEntry.arguments?.getString("routeId") ?: ""
        val deliveryId = backStackEntry.arguments?.getString("deliveryId") ?: ""
        ActiveDeliveryHUD(
            routeId = routeId,
            deliveryId = deliveryId,
            onComplete = { navController.navigate("dashboard") { popUpTo("dashboard") { inclusive = true } } }
        )
    }
    composable(
        route = "proof/{routeId}/{deliveryId}",
        arguments = listOf(
            navArgument("routeId") { type = NavType.StringType },
            navArgument("deliveryId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val deliveryId = backStackEntry.arguments?.getString("deliveryId") ?: ""
        ProofOfDeliveryScreen(
            deliveryId = deliveryId,
            onConfirmed = { navController.popBackStack() }
        )
    }
}
