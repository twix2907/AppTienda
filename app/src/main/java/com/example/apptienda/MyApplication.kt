package com.example.apptienda

import android.app.Application
import android.util.Log
import com.cloudinary.android.MediaManager
import com.google.firebase.Firebase
import com.google.firebase.initialize

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Firebase.initialize(this)
        // Inicializar Cloudinary
        val config = hashMapOf(
            "cloud_name" to "dzr2ljc4h",
            "api_key" to "332839777227448",
            "api_secret" to "RuAGkqSoiPMDt7wNZpJwZ8R-7lU"
        )
        try {
            MediaManager.init(this, config)
        } catch (e: Exception) {
            Log.e("Cloudinary", "Error inicializando Cloudinary: ${e.message}")
        }
    }

}