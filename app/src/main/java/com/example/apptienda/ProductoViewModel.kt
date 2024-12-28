package com.example.apptienda

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ProductoViewModel(
    private val cloudinaryRepository: CloudinaryRepository // Nuevo
) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

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

    // Función modificada para usar Cloudinary
    suspend fun agregarProducto(
        nombre: String,
        precio: Double,
        descripcion: String,
        imageUri: Uri?,
        context: Context // Nuevo parámetro necesario
    ): Result<Unit> {
        return try {
            val imageUrl = imageUri?.let { uri ->
                cloudinaryRepository.uploadImage(uri, context)
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

    // Esta función se mantiene igual
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

    // Esta función se mantiene igual
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