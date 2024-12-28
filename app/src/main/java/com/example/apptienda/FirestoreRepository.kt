package com.example.apptienda

import androidx.annotation.Keep
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.tasks.await
@Keep
class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    private val productosCollection = db.collection("productos")

    suspend fun saveProduct(producto: Producto) {
        try {
            productosCollection.add(producto)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getProducts(): List<Producto> {
        return try {
            productosCollection.get().await().toObjects()
        } catch (e: Exception) {
            emptyList()
        }
    }
}