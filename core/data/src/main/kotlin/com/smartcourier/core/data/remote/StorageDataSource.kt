package com.smartcourier.core.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val JPEG_QUALITY = 60
private const val MAX_IMAGE_DIMENSION = 1920

@Singleton
class StorageDataSource @Inject constructor(
    private val storage: FirebaseStorage,
    private val context: Context
) {
    suspend fun uploadProofPhoto(
        userId: String,
        deliveryId: String,
        localPath: String,
        countryCode: String = "ae"
    ): NetworkResponse<String> = withContext(Dispatchers.IO) {
        try {
            val compressed = compressImage(File(localPath))
            val ref = proofPhotoRef(countryCode, userId, deliveryId)
            ref.putBytes(compressed).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            NetworkResponse.Success(downloadUrl)
        } catch (e: Exception) {
            NetworkResponse.Failure(e)
        }
    }

    private fun proofPhotoRef(countryCode: String, userId: String, deliveryId: String): StorageReference {
        return storage.reference
            .child("$countryCode/users/$userId/proofs/$deliveryId.jpg")
    }

    private suspend fun compressImage(file: File): ByteArray = withContext(Dispatchers.IO) {
        val original = BitmapFactory.decodeFile(file.absolutePath)
        val scaled = if (original.width > MAX_IMAGE_DIMENSION || original.height > MAX_IMAGE_DIMENSION) {
            val ratio = MAX_IMAGE_DIMENSION.toFloat() / maxOf(original.width, original.height)
            Bitmap.createScaledBitmap(original, (original.width * ratio).toInt(), (original.height * ratio).toInt(), true)
        } else {
            original
        }
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        if (scaled !== original) scaled.recycle()
        original.recycle()
        output.toByteArray()
    }

    fun deleteProofPhoto(userId: String, deliveryId: String, countryCode: String = "ae") {
        proofPhotoRef(countryCode, userId, deliveryId).delete()
    }
}
