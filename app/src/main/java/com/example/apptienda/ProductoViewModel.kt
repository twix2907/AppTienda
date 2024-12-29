package com.example.apptienda

import android.content.Context
import android.net.Uri
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
@Keep
class ProductoViewModel(
    private val cloudinaryRepository: CloudinaryRepository // Nuevo
) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _productos = MutableStateFlow<List<Producto>>(emptyList())
    val productos: StateFlow<List<Producto>> = _productos

    private val _categorias = MutableStateFlow<List<Categoria>>(emptyList())
    val categorias: StateFlow<List<Categoria>> = _categorias

    // Nuevo StateFlow para rastrear las cargas de imágenes pendientes
    private val _pendingUploads = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val pendingUploads: StateFlow<Map<String, Boolean>> = _pendingUploads

    init {
        cargarCategorias()
        cargarProductos()
    }

    private fun cargarCategorias() {
        viewModelScope.launch {
            try {
                db.collection("categorias")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) return@addSnapshotListener

                        val categoriasList = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(Categoria::class.java)?.copy(id = doc.id)
                        }?.sortedWith(
                            compareBy { it.nombre.lowercase() }
                        ) ?: emptyList()

                        _categorias.value = categoriasList
                    }
            } catch (e: Exception) {
                // Manejar error
            }
        }
    }
    fun getCategoriaById(id: String): Categoria? {
        return _categorias.value.find { it.id == id }
    }

    suspend fun agregarCategoria(nombre: String, descripcion: String = ""): Result<String> {
        return try {
            val categoria = Categoria(nombre = nombre, descripcion = descripcion)
            val docRef = db.collection("categorias")
                .add(categoria)
                .await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getProductById(productId: String): Producto? {
        return productos.value.find { it.id == productId }
    }

    suspend fun actualizarProducto(
        producto: Producto,
        newImageUri: Uri?,
        context: Context
    ): Result<Unit> {
        return try {
            val imageUrl = if (newImageUri != null) {
                cloudinaryRepository.uploadImage(newImageUri, context)
            } else {
                producto.imageUrl
            }

            val productoActualizado = producto.copy(
                imageUrl = imageUrl,
                idNumerico = producto.idNumerico // Mantener el ID numérico existente
            )

            db.collection("productos")
                .document(producto.id)
                .set(productoActualizado)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
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

    suspend fun agregarProducto(
        nombre: String,
        precio: Double,
        descripcion: String,
        categorias: List<String>,
        imageUri: Uri?,
        context: Context
    ): Result<String> {
        return try {
            // Obtener ID en segundo plano
            val idNumerico = withContext(Dispatchers.IO) {
                obtenerSiguienteId()
            }

            // Crear el producto sin imagen inicialmente
            val producto = Producto(
                idNumerico = idNumerico,
                nombre = nombre,
                precio = precio,
                descripcion = descripcion,
                imageUrl = "",
                categorias = categorias
            )

            // Guardar el producto en segundo plano
            val docRef = withContext(Dispatchers.IO) {
                db.collection("productos")
                    .add(producto)
                    .await()
            }

            // Si hay imagen, iniciar carga en segundo plano
            imageUri?.let { uri ->
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val imageUrl = cloudinaryRepository.uploadImage(uri, context)
                        db.collection("productos")
                            .document(docRef.id)
                            .update("imageUrl", imageUrl)
                            .await()
                    } catch (e: Exception) {
                        // Manejar error de carga de imagen
                        e.printStackTrace()
                    }
                }
            }

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Función para verificar si hay cargas pendientes
    fun hasPendingUploads(): Boolean {
        return _pendingUploads.value.isNotEmpty()
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


    suspend fun eliminarCategoria(categoriaId: String): Result<Unit> {
        return try {
            db.collection("categorias")
                .document(categoriaId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    private suspend fun obtenerSiguienteId(): Long {
        return try {
            // Obtener el documento que almacena el contador
            val contadorDoc = db.collection("contadores").document("productos")

            // Realizar una transacción para incrementar el contador de forma segura
            db.runTransaction { transaction ->
                val snapshot = transaction.get(contadorDoc)
                val currentId = snapshot.getLong("ultimoId") ?: -1
                val nextId = currentId + 1

                // Actualizar el contador
                transaction.set(contadorDoc, hashMapOf("ultimoId" to nextId))

                nextId
            }.await()
        } catch (e: Exception) {
            // Si hay algún error, intentar obtener el máximo ID actual y sumar 1
            val maxId = db.collection("productos")
                .orderBy("idNumerico", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.getLong("idNumerico") ?: -1

            maxId + 1
        }
    }
    fun getNombresSimilares(nombre: String): List<String> {
        if (nombre.length < 3) return emptyList() // Solo buscar si hay al menos 3 caracteres

        return productos.value
            .filter { producto ->
                producto.nombre.contains(nombre, ignoreCase = true) ||
                        nombre.contains(producto.nombre, ignoreCase = true)
            }
            .map { it.nombre }
    }


}