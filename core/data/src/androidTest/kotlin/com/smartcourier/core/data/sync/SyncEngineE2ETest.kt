package com.smartcourier.core.data.sync

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.smartcourier.core.data.local.Database
import com.smartcourier.core.data.local.dao.DeliveryDao
import com.smartcourier.core.data.local.dao.OutboxDao
import com.smartcourier.core.data.local.dao.RouteDao
import com.smartcourier.core.data.local.dao.UserDao
import com.smartcourier.core.data.local.entity.DeliveryEntity
import com.smartcourier.core.data.local.entity.RouteEntity
import com.smartcourier.core.data.local.entity.UserEntity
import com.smartcourier.core.data.mapper.toDto
import com.smartcourier.core.data.remote.FirestoreDataSource
import com.smartcourier.core.data.remote.NetworkResponse
import com.smartcourier.core.data.remote.StorageDataSource
import com.smartcourier.core.domain.model.Route
import com.smartcourier.core.domain.model.RouteStatus
import com.smartcourier.core.domain.model.SYNC_CLEAN
import com.smartcourier.core.domain.model.SYNC_DIRTY
import com.smartcourier.core.domain.repository.DeliveryRepository
import com.smartcourier.core.domain.repository.RouteRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class SyncEngineE2ETest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    @ApplicationContext
    lateinit var context: Context

    @Inject
    lateinit var database: Database

    @Inject
    lateinit var userDao: UserDao

    @Inject
    lateinit var routeDao: RouteDao

    @Inject
    lateinit var deliveryDao: DeliveryDao

    @Inject
    lateinit var outboxDao: OutboxDao

    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var storage: FirebaseStorage

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var firestoreDataSource: FirestoreDataSource

    @Inject
    lateinit var storageDataSource: StorageDataSource

    @Inject
    lateinit var deliveryRepository: DeliveryRepository

    @Inject
    lateinit var routeRepository: RouteRepository

    private val projectId: String = "smart-courier-app-e8624"
    private var testUserId: String = ""

    @Before
    fun setUp() {
        hiltRule.inject()
        ensureFirebaseInitialized()
        signInAnonymously()
        resetFirestoreEmulator()
        database.clearAllTables()
    }

    @After
    fun tearDown() {
        auth.signOut()
    }

    private fun ensureFirebaseInitialized() {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
    }

    private fun signInAnonymously() {
        auth.signOut()
        testUserId = runBlocking {
            auth.signInAnonymously().await().user!!.uid
        }
    }

    private fun resetFirestoreEmulator() {
        runBlocking {
            try {
                val url = URL(
                    "http://10.0.2.2:8080/emulator/v1/projects/$projectId/databases/(default)/documents"
                )
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.connect()
                conn.responseCode
                conn.disconnect()
            } catch (_: Exception) { }
        }
    }

    private fun createTestJpeg(): File {
        val file = File(context.cacheDir, "e2e_test_photo_${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { stream ->
            val width = 100
            val height = 100
            val pixels = IntArray(width * height) { 0xFF888888.toInt() }
            val bitmap = android.graphics.Bitmap.createBitmap(pixels, width, height, android.graphics.Bitmap.Config.ARGB_8888)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, stream)
            bitmap.recycle()
        }
        return file
    }

    @Test
    fun offlineDeliveryComplete_outboxEnqueued_syncToFirestore() = runBlocking {
        val routeId = "e2e-route-${UUID.randomUUID()}"
        val deliveryId = "e2e-del-${UUID.randomUUID()}"

        userDao.upsertUser(
            UserEntity(
                uid = testUserId,
                name = "E2E Courier",
                email = "e2e@test.com",
                subscriptionTier = "PRO",
                syncStatus = SYNC_CLEAN
            )
        )
        routeDao.upsertRoute(
            RouteEntity(
                routeId = routeId,
                userId = testUserId,
                routeStatus = "ACTIVE",
                syncStatus = SYNC_CLEAN
            )
        )
        deliveryDao.upsertDelivery(
            DeliveryEntity(
                id = deliveryId,
                routeId = routeId,
                index = 1,
                recipientName = "Test Recipient",
                address = "123 Test St, Dubai",
                latitude = 25.2048,
                longitude = 55.2708,
                status = "IN_TRANSIT",
                trackingToken = "TOKEN-${UUID.randomUUID()}",
                earningsAed = 0.0,
                syncStatus = SYNC_CLEAN
            )
        )

        deliveryRepository.markDeliveryComplete(deliveryId, null, 45.0)

        with(deliveryDao.getDelivery(deliveryId)!!) {
            assertEquals("DELIVERED", status)
            assertEquals(SYNC_DIRTY, syncStatus)
            assertEquals(45.0, earningsAed, 0.001)
        }

        val mutations = outboxDao.getPendingMutations()
        assertEquals(1, mutations.size)
        assertEquals("DELIVERY_COMPLETE", mutations[0].payloadType)
        assertEquals(deliveryId, mutations[0].targetId)

        val mutation = mutations[0]
        val entity = deliveryDao.getDelivery(mutation.targetId)!!
        val dto = entity.toDomain().toDto()
        val syncResult = firestoreDataSource.syncDelivery(dto)
        assertTrue("Firestore sync should succeed", syncResult is NetworkResponse.Success)

        outboxDao.removeById(mutation.id)
        deliveryDao.updateSyncStatus(mutation.targetId, SYNC_CLEAN)

        val doc = firestore.collection("deliveries").document(deliveryId).get().await()
        assertTrue("Document should exist in Firestore", doc.exists())
        assertEquals("DELIVERED", doc.getString("status"))
        assertEquals(45.0, doc.getDouble("earningsAed"), 0.001)
        assertEquals(routeId, doc.getString("routeId"))

        assertEquals(SYNC_CLEAN, deliveryDao.getDelivery(deliveryId)!!.syncStatus)
        assertEquals(0, outboxDao.getPendingMutations().size)
    }

    @Test
    fun photoUpload_thenDeliverySync_updatesRemoteUrlInFirestore() = runBlocking {
        val routeId = "e2e-route-photo-${UUID.randomUUID()}"
        val deliveryId = "e2e-del-photo-${UUID.randomUUID()}"
        val photoFile = createTestJpeg()

        userDao.upsertUser(
            UserEntity(uid = testUserId, name = "Photo Courier", syncStatus = SYNC_CLEAN)
        )
        routeDao.upsertRoute(
            RouteEntity(routeId = routeId, userId = testUserId, routeStatus = "ACTIVE", syncStatus = SYNC_CLEAN)
        )
        deliveryDao.upsertDelivery(
            DeliveryEntity(
                id = deliveryId, routeId = routeId, index = 1,
                recipientName = "Photo Recipient", address = "456 Photo St", latitude = 25.0, longitude = 55.0,
                status = "IN_TRANSIT", syncStatus = SYNC_CLEAN
            )
        )

        deliveryRepository.markDeliveryComplete(deliveryId, photoFile.absolutePath, 32.5)
        assertEquals(SYNC_DIRTY, deliveryDao.getDelivery(deliveryId)!!.syncStatus)

        val uploadResult = storageDataSource.uploadProofPhoto(testUserId, deliveryId, photoFile.absolutePath)
        assertTrue("Photo upload should succeed", uploadResult is NetworkResponse.Success)
        val downloadUrl = (uploadResult as NetworkResponse.Success).data
        assertNotNull("Download URL should not be null", downloadUrl)

        deliveryDao.updatePhotoRemoteUrl(deliveryId, downloadUrl)

        val entity = deliveryDao.getDelivery(deliveryId)!!
        val dto = entity.toDomain().toDto().copy(photoRemoteUrl = downloadUrl)
        val syncResult = firestoreDataSource.syncDelivery(dto)
        assertTrue("Firestore sync should succeed", syncResult is NetworkResponse.Success)

        val doc = firestore.collection("deliveries").document(deliveryId).get().await()
        assertTrue("Document should exist in Firestore", doc.exists())
        assertEquals("DELIVERED", doc.getString("status"))
        assertEquals(downloadUrl, doc.getString("photoRemoteUrl"))
        assertEquals(32.5, doc.getDouble("earningsAed"), 0.001)

        deliveryDao.updateSyncStatus(deliveryId, SYNC_CLEAN)
        val mutations = outboxDao.getPendingMutations()
        for (m in mutations) outboxDao.removeById(m.id)

        assertTrue(photoFile.delete())
    }

    @Test
    fun routeUpsert_outboxEnqueued_syncsToFirestore() = runBlocking {
        val routeId = "e2e-route-upsert-${UUID.randomUUID()}"

        userDao.upsertUser(
            UserEntity(uid = testUserId, name = "Route Tester", syncStatus = SYNC_CLEAN)
        )

        routeRepository.upsertRoute(
            Route(
                routeId = routeId,
                userId = testUserId,
                routeStatus = RouteStatus.CREATED,
                totalDistanceMeters = 15000.0,
                estimatedDurationSeconds = 5400L
            )
        )

        val route = routeDao.getRoute(routeId)!!
        assertEquals("CREATED", route.routeStatus)

        val outboxEntries = outboxDao.getPendingMutations()
        assertEquals(1, outboxEntries.size)
        assertEquals("ROUTE_UPSERT", outboxEntries[0].payloadType)
        assertEquals(routeId, outboxEntries[0].targetId)

        val dto = route.toDomain().toDto()
        val syncResult = firestoreDataSource.syncRoute(dto)
        assertTrue("Route sync should succeed", syncResult is NetworkResponse.Success)

        val doc = firestore.collection("routes").document(routeId).get().await()
        assertTrue("Route document should exist in Firestore", doc.exists())
        assertEquals("CREATED", doc.getString("routeStatus"))
        assertEquals(testUserId, doc.getString("userId"))

        routeDao.updateSyncStatus(routeId, SYNC_CLEAN)
        val mutations = outboxDao.getPendingMutations()
        for (m in mutations) outboxDao.removeById(m.id)
    }

    @Test
    fun multipleDeliveriesOnRoute_allSyncIndependently() = runBlocking {
        val routeId = "e2e-route-multi-${UUID.randomUUID()}"
        val deliveryIds = (1..3).map { "e2e-del-multi-$it-${UUID.randomUUID()}" }

        userDao.upsertUser(UserEntity(uid = testUserId, name = "Multi Courier", syncStatus = SYNC_CLEAN))
        routeDao.upsertRoute(RouteEntity(routeId = routeId, userId = testUserId, routeStatus = "ACTIVE", syncStatus = SYNC_CLEAN))
        deliveryIds.forEachIndexed { idx, id ->
            deliveryDao.upsertDelivery(
                DeliveryEntity(
                    id = id, routeId = routeId, index = idx,
                    recipientName = "Stop ${idx + 1}", address = "Address $idx",
                    latitude = 25.0 + idx * 0.01, longitude = 55.0 + idx * 0.01,
                    status = "IN_TRANSIT", syncStatus = SYNC_CLEAN, earningsAed = 0.0
                )
            )
        }

        deliveryIds.forEachIndexed { idx, id ->
            deliveryRepository.markDeliveryComplete(id, null, (idx + 1) * 10.0)
        }

        assertEquals(deliveryIds.size, outboxDao.getPendingMutations().size)

        for (mutation in outboxDao.getPendingMutations()) {
            val entity = deliveryDao.getDelivery(mutation.targetId)!!
            val result = firestoreDataSource.syncDelivery(entity.toDomain().toDto())
            assertTrue("Sync should succeed for ${mutation.targetId}", result is NetworkResponse.Success)
            outboxDao.removeById(mutation.id)
            deliveryDao.updateSyncStatus(mutation.targetId, SYNC_CLEAN)
        }

        for ((idx, id) in deliveryIds.withIndex()) {
            val doc = firestore.collection("deliveries").document(id).get().await()
            assertTrue("Delivery $id should exist in Firestore", doc.exists())
            assertEquals("DELIVERED", doc.getString("status"))
            assertEquals((idx + 1) * 10.0, doc.getDouble("earningsAed"), 0.001)
            assertEquals(SYNC_CLEAN, deliveryDao.getDelivery(id)!!.syncStatus)
        }

        assertEquals(0, outboxDao.getPendingMutations().size)
    }
}
