package com.example.apptienda.data.repository

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
                // Configuración de optimización de imágenes
                val options = HashMap<String, Any>().apply {
                    put("quality_analysis", true)
                    put("quality", "auto:eco")
                    put("fetch_format", "auto")
                    put("width", 1024)
                    put("height", 1024)
                    put("crop", "limit")
                    put("compression", "low")
                    put("strip", true)
                }

                MediaManager.get()
                    .upload(imageUri)
                    .options(options)
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {
                            Log.d(TAG, "Iniciando subida optimizada")
                        }

                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                            val progress = (bytes.toDouble() / totalBytes) * 100
                            Log.d(TAG, "Progreso: $progress%")
                        }

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val imageUrl = (resultData["url"] as? String)?.replace("http://", "https://")
                            val optimizedUrl = imageUrl?.let { url ->
                                url.replace("/upload/", "/upload/f_auto,q_auto:eco,c_limit,w_1024/")
                            }
                            Log.d(TAG, "URL optimizada: $optimizedUrl")

                            if (optimizedUrl != null) {
                                continuation.resume(optimizedUrl)
                            } else {
                                continuation.resumeWithException(Exception("URL no encontrada"))
                            }
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            Log.e(TAG, "Error: ${error.description}")
                            continuation.resumeWithException(Exception(error.description))
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {
                            Log.d(TAG, "Reintentando subida")
                        }
                    })
                    .dispatch()

            } catch (e: Exception) {
                Log.e(TAG, "Error en uploadImage", e)
                continuation.resumeWithException(e)
            }
        }
    }

    companion object {
        private const val TAG = "CloudinaryRepository"
    }
}