package com.example.apptienda

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CloudinaryRepository {
    suspend fun uploadImage(imageUri: Uri, context: Context): String {
        return suspendCancellableCoroutine { continuation ->
            try {
                MediaManager.get()
                    .upload(imageUri)
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {
                            Log.d("Cloudinary", "Iniciando subida")
                        }

                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                            val progress = (bytes.toDouble() / totalBytes) * 100
                            Log.d("Cloudinary", "Progreso: $progress%")
                        }

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val imageUrl = (resultData["url"] as? String)?.replace("http://", "https://")
                            Log.d("Cloudinary", "URL obtenida: $imageUrl")
                            if (imageUrl != null) {
                                continuation.resume(imageUrl)
                            } else {
                                continuation.resumeWithException(Exception("URL no encontrada"))
                            }
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            Log.e("Cloudinary", "Error: ${error.description}")
                            continuation.resumeWithException(Exception(error.description))
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {
                            Log.d("Cloudinary", "Reintentando subida")
                        }
                    })
                    .dispatch()
            } catch (e: Exception) {
                Log.e("Cloudinary", "Error en uploadImage", e)
                continuation.resumeWithException(e)
            }
        }
    }
}