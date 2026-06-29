package com.smartcourier.feature.route_planner

import com.smartcourier.core.domain.model.Coordinate
import com.smartcourier.core.domain.model.DomainResult
import com.smartcourier.core.domain.model.Route
import com.smartcourier.core.domain.model.RouteStatus
import com.smartcourier.core.domain.model.SYNC_DIRTY
import com.smartcourier.core.domain.usecase.OptimizeRouteUseCase
import com.smartcourier.core.domain.usecase.ParseAddressesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class IngestionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val parseAddressesUseCase: ParseAddressesUseCase = mock()
    private val optimizeRouteUseCase: OptimizeRouteUseCase = mock()

    private lateinit var viewModel: IngestionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = IngestionViewModel(parseAddressesUseCase, optimizeRouteUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `on RawTextChanged updates delivery count`() = runTest(testDispatcher) {
        val rawText = "Sheikh Zayed Rd, Dubai\nJumeirah Beach Rd, Dubai"
        val parsedAddresses = listOf(
            ParseAddressesUseCase.ParsedAddress(0, "Sheikh Zayed Rd, Dubai", "id-1", ""),
            ParseAddressesUseCase.ParsedAddress(1, "Jumeirah Beach Rd, Dubai", "id-2", "")
        )
        whenever(parseAddressesUseCase.invoke(rawText, "")).thenReturn(parsedAddresses)

        viewModel.onAction(IngestionAction.RawTextChanged(rawText))
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.deliveryCount)
        assertEquals(rawText, viewModel.uiState.value.rawText)
    }

    @Test
    fun `on OptimizeClicked with insufficient deliveries shows error message`() = runTest(testDispatcher) {
        val rawText = "Single address"
        whenever(parseAddressesUseCase.invoke(any(), any())).thenReturn(
            listOf(ParseAddressesUseCase.ParsedAddress(0, "Single address", "id-1", ""))
        )

        viewModel.onAction(IngestionAction.RawTextChanged(rawText))
        advanceUntilIdle()
        viewModel.onAction(IngestionAction.OptimizeClicked)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.deliveryCount < 2)
        assertTrue(viewModel.uiState.value.userMessage != null)
    }

    @Test
    fun `on OptimizeClicked with valid addresses emits NavigateToPreview effect`() = runTest(testDispatcher) {
        val rawText = "25.2048,55.2708\n25.1972,55.2744\n25.1900,55.2800"
        val parsedAddresses = listOf(
            ParseAddressesUseCase.ParsedAddress(0, "25.2048,55.2708", "id-1", ""),
            ParseAddressesUseCase.ParsedAddress(1, "25.1972,55.2744", "id-2", ""),
            ParseAddressesUseCase.ParsedAddress(2, "25.1900,55.2800", "id-3", "")
        )
        val mockRoute = Route(
            routeId = "route-123",
            userId = "unknown_user",
            routeStatus = RouteStatus.ACTIVE,
            syncStatus = SYNC_DIRTY
        )

        whenever(parseAddressesUseCase.invoke(any(), any())).thenReturn(parsedAddresses)
        whenever(parseAddressesUseCase.estimateCoordinates("25.2048,55.2708")).thenReturn(Coordinate(25.2048, 55.2708))
        whenever(parseAddressesUseCase.estimateCoordinates("25.1972,55.2744")).thenReturn(Coordinate(25.1972, 55.2744))
        whenever(parseAddressesUseCase.estimateCoordinates("25.1900,55.2800")).thenReturn(Coordinate(25.1900, 55.2800))
        whenever(optimizeRouteUseCase.invoke(any(), any(), any())).thenReturn(DomainResult.Success(mockRoute))

        viewModel.onAction(IngestionAction.RawTextChanged(rawText))
        advanceUntilIdle()
        viewModel.onAction(IngestionAction.OptimizeClicked)
        advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertTrue(effect is IngestionEffect.NavigateToPreview)
        assertEquals("route-123", (effect as IngestionEffect.NavigateToPreview).routeId)
    }
}
