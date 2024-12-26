package com.example.apptienda

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ProductoViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _productos = MutableStateFlow<List<Producto>>(emptyList())
    val productos: StateFlow<List<Producto>> = _productos

    init {
        cargarProductos()
    }

    private fun cargarProductos() {
        viewModelScope.launch {
            try {
                db.collection("productos")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) return@addSnapshotListener

                        val productosList = snapshot?.documents?.map { doc ->
                            doc.toObject(Producto::class.java)?.copy(id = doc.id)
                        }?.filterNotNull() ?: emptyList()

                        _productos.value = productosList
                    }
            } catch (e: Exception) {
                // Manejar error
            }
        }
    }

    suspend fun agregarProducto(nombre: String, precio: Double, descripcion: String, imageUri: Uri?): Result<Unit> {
        return try {
            val imageUrl = imageUri?.let { uri ->
                val imageRef = storage.reference.child("productos/${UUID.randomUUID()}")
                imageRef.putFile(uri).await()
                imageRef.downloadUrl.await().toString()
            } ?: ""

            val producto = Producto(
                nombre = nombre,
                precio = precio,
                descripcion = descripcion,
                imageUrl = imageUrl
            )

            db.collection("productos")
                .add(producto)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarProducto(productoId: String): Result<Unit> {
        return try {
            db.collection("productos")
                .document(productoId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizarProducto(producto: Producto): Result<Unit> {
        return try {
            db.collection("productos")
                .document(producto.id)
                .set(producto)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}