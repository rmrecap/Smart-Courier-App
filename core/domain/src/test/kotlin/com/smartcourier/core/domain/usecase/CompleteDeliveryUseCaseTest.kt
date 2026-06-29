package com.smartcourier.core.domain.usecase

import com.smartcourier.core.domain.model.Resource
import com.smartcourier.core.domain.repository.DeliveryRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.IOException

class CompleteDeliveryUseCaseTest {

    private val deliveryRepository: DeliveryRepository = mock()
    private lateinit var completeDeliveryUseCase: CompleteDeliveryUseCase

    @Before
    fun setup() {
        completeDeliveryUseCase = CompleteDeliveryUseCase(deliveryRepository)
    }

    @Test
    fun `When repository execution runs smoothly, verification workflow outputs success sequence`() = runTest {
        val targetId = "DEL-9921"
        val photoUri = "file://storage/img_test.jpg"
        val explicitPay = 22.50

        whenever(deliveryRepository.markDeliveryComplete(targetId, photoUri, explicitPay)).thenReturn(Unit)

        val emissionResults = completeDeliveryUseCase(targetId, photoUri, explicitPay).toList()

        assertTrue(emissionResults[0] is Resource.Loading)
        assertTrue(emissionResults[1] is Resource.Success)
    }

    @Test
    fun `When data source raises network execution failure, state safely outputs error wrapper`() = runTest {
        val targetId = "DEL-9921"
        val photoUri = "file://storage/img_test.jpg"
        val explicitPay = 22.50

        whenever(deliveryRepository.markDeliveryComplete(targetId, photoUri, explicitPay)).thenThrow(IOException("Disk full."))

        val emissionResults = completeDeliveryUseCase(targetId, photoUri, explicitPay).toList()

        assertTrue(emissionResults[0] is Resource.Loading)
        assertTrue(emissionResults[1] is Resource.Error)
    }
}
