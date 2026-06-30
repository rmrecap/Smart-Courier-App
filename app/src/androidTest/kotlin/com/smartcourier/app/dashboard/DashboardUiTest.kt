package com.smartcourier.app.dashboard

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartcourier.core.domain.model.DailyEarnings
import com.smartcourier.core.domain.model.Delivery
import com.smartcourier.core.domain.model.DeliveryStatus
import com.smartcourier.core.domain.model.TodaySummary
import com.smartcourier.core.ui.theme.SmartCourierTheme
import com.smartcourier.feature.dashboard.EarningsChart
import com.smartcourier.feature.dashboard.EarningsChartTestTag
import com.smartcourier.feature.dashboard.RiderDashboardScreen
import com.smartcourier.feature.dashboard.RiderDashboardViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun earningsChart_rendersCanvasWithTestTag() {
        val earnings = listOf(
            DailyEarnings(1, "Mon", 120.0, 15.0),
            DailyEarnings(2, "Tue", 80.0, 0.0),
            DailyEarnings(3, "Wed", 200.0, 25.0),
            DailyEarnings(4, "Thu", 150.0, 10.0),
            DailyEarnings(5, "Fri", 90.0, 5.0),
            DailyEarnings(6, "Sat", 0.0, 0.0),
            DailyEarnings(7, "Sun", 0.0, 0.0),
        )

        composeRule.setContent {
            SmartCourierTheme {
                EarningsChart(dailyEarnings = earnings)
            }
        }

        composeRule.onNodeWithTag(EarningsChartTestTag).assertExists()
    }

    @Test
    fun earningsChart_rendersWithSingleDay() {
        composeRule.setContent {
            SmartCourierTheme {
                EarningsChart(dailyEarnings = listOf(DailyEarnings(3, "Wed", 50.0, 5.0)))
            }
        }

        composeRule.onNodeWithTag(EarningsChartTestTag).assertExists()
    }

    @Test
    fun earningsChart_emptyList_doesNotRenderCanvas() {
        composeRule.setContent {
            SmartCourierTheme {
                EarningsChart(dailyEarnings = emptyList())
            }
        }

        composeRule.onNodeWithTag(EarningsChartTestTag).assertDoesNotExist()
    }

    @Test
    fun summaryMatrix_displaysTotalCompletedFailedMetrics() {
        val summary = TodaySummary(
            totalDeliveries = 15,
            completedCount = 10,
            failedCount = 2,
            totalEarnings = 450.0,
            totalTips = 35.0
        )

        composeRule.setContent {
            SmartCourierTheme {
                RiderDashboardScreen(
                    onNavigateToIngestion = {},
                    onNavigateToRoute = {},
                    viewModel = createViewModel(summary)
                )
            }
        }

        composeRule.onNodeWithText("15").assertExists()
        composeRule.onNodeWithText("Total").assertExists()
        composeRule.onNodeWithText("10").assertExists()
        composeRule.onNodeWithText("Completed").assertExists()
        composeRule.onNodeWithText("2").assertExists()
        composeRule.onNodeWithText("Failed").assertExists()
    }

    @Test
    fun headerSection_displaysEarningsAndTips() {
        val summary = TodaySummary(
            totalDeliveries = 8,
            completedCount = 6,
            failedCount = 1,
            totalEarnings = 320.0,
            totalTips = 25.0
        )

        composeRule.setContent {
            SmartCourierTheme {
                RiderDashboardScreen(
                    onNavigateToIngestion = {},
                    onNavigateToRoute = {},
                    viewModel = createViewModel(summary)
                )
            }
        }

        composeRule.onNodeWithText("AED 320.00 earned today").assertExists()
        composeRule.onNodeWithText("AED 25.00 in tips").assertExists()
    }

    @Test
    fun headerSection_zeroTips_omitsTipsLine() {
        val summary = TodaySummary(
            totalDeliveries = 5,
            completedCount = 3,
            failedCount = 0,
            totalEarnings = 150.0,
            totalTips = 0.0
        )

        composeRule.setContent {
            SmartCourierTheme {
                RiderDashboardScreen(
                    onNavigateToIngestion = {},
                    onNavigateToRoute = {},
                    viewModel = createViewModel(summary)
                )
            }
        }

        composeRule.onNodeWithText("AED 150.00 earned today").assertExists()
        composeRule.onNodeWithText("AED 0.00 in tips").assertDoesNotExist()
    }

    @Test
    fun historyLedger_displaysDeliveryAddressStatusAndEarnings() {
        val now = System.currentTimeMillis()
        val deliveries = listOf(
            Delivery(
                id = "del_001",
                routeId = "route_1",
                index = 0,
                address = "123 Main St",
                latitude = 25.0,
                longitude = 55.0,
                status = DeliveryStatus.DELIVERED.value,
                earningsAed = 50.0,
                lastModifiedTimestamp = now - 120_000
            )
        )

        composeRule.setContent {
            SmartCourierTheme {
                RiderDashboardScreen(
                    onNavigateToIngestion = {},
                    onNavigateToRoute = {},
                    viewModel = createViewModel(deliveries = deliveries)
                )
            }
        }

        composeRule.onNodeWithText("123 Main St").assertExists()
        composeRule.onNodeWithText("AED 50").assertExists()
        composeRule.onNodeWithText("2m ago").assertExists()
    }

    @Test
    fun historyLedger_failedDelivery_showsFailedStatus() {
        val now = System.currentTimeMillis()
        val deliveries = listOf(
            Delivery(
                id = "del_002",
                routeId = "route_1",
                index = 1,
                address = "456 Oak Ave",
                latitude = 25.1,
                longitude = 55.1,
                status = DeliveryStatus.FAILED.value,
                earningsAed = 0.0,
                lastModifiedTimestamp = now - 60_000
            )
        )

        composeRule.setContent {
            SmartCourierTheme {
                RiderDashboardScreen(
                    onNavigateToIngestion = {},
                    onNavigateToRoute = {},
                    viewModel = createViewModel(deliveries = deliveries)
                )
            }
        }

        composeRule.onNodeWithText("456 Oak Ave").assertExists()
        composeRule.onNodeWithText("Failed").assertExists()
    }

    @Test
    fun actionBar_newRouteButton_displays() {
        composeRule.setContent {
            SmartCourierTheme {
                RiderDashboardScreen(
                    onNavigateToIngestion = {},
                    onNavigateToRoute = {},
                    viewModel = createViewModel()
                )
            }
        }

        composeRule.onNodeWithText("New Route").assertExists()
    }

    @Test
    fun actionBar_tipsButton_displays() {
        composeRule.setContent {
            SmartCourierTheme {
                RiderDashboardScreen(
                    onNavigateToIngestion = {},
                    onNavigateToRoute = {},
                    viewModel = createViewModel()
                )
            }
        }

        composeRule.onNodeWithText("Tips").assertExists()
    }

    private fun createViewModel(
        summary: TodaySummary = TodaySummary(),
        deliveries: List<Delivery> = emptyList()
    ): RiderDashboardViewModel {
        val repo = FakeDeliveryRepository(
            initialSummary = summary,
            initialDeliveries = deliveries
        )
        return RiderDashboardViewModel(repo)
    }
}
