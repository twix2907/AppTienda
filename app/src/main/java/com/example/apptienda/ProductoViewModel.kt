package com.example.apptienda

import android.content.Context
import android.content.Intent

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

import android.net.Uri
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Keep
class ProductoViewModel(
    private val cloudinaryRepository: CloudinaryRepository
) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _productos = MutableStateFlow<List<Producto>>(emptyList())
    val productos: StateFlow<List<Producto>> = _productos

    private val _categorias = MutableStateFlow<List<Categoria>>(emptyList())
    val categorias: StateFlow<List<Categoria>> = _categorias

    private val _pendingUploads = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val pendingUploads: StateFlow<Map<String, Boolean>> = _pendingUploads

    // Modificado: Ahora almacena IDs en lugar de objetos Producto
    private val _selectedProductIds = mutableStateOf<Set<String>>(emptySet())
    val selectedProductIds: State<Set<String>> = _selectedProductIds

    private val _isSelectionMode = mutableStateOf(false)
    val isSelectionMode: State<Boolean> = _isSelectionMode


    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage

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
                id = producto.id,
                idNumerico = producto.idNumerico,
                nombre = producto.nombre,
                precio = producto.precio,
                descripcion = producto.descripcion,
                categorias = producto.categorias,
                idOrdenNumerico = producto.idOrdenNumerico  // Mantener el mismo idOrdenNumerico
            )

            db.collection("productos")
                .document(producto.id)
                .set(productoActualizado, SetOptions.merge())
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
                    .orderBy("idOrdenNumerico", Query.Direction.DESCENDING)
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
            val idNumerico = withContext(Dispatchers.IO) {
                obtenerSiguienteId()
            }
            val idOrden = withContext(Dispatchers.IO) {
                obtenerSiguienteOrden()
            }

            val producto = Producto(
                idNumerico = idNumerico, // Ahora idNumerico es un String
                nombre = nombre,
                precio = precio,
                descripcion = descripcion,
                imageUrl = "",
                categorias = categorias,
                idOrdenNumerico = idOrden
            )

            val docRef = withContext(Dispatchers.IO) {
                db.collection("productos")
                    .add(producto)
                    .await()
            }

            imageUri?.let { uri ->
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val imageUrl = cloudinaryRepository.uploadImage(uri, context)
                        db.collection("productos")
                            .document(docRef.id)
                            .update("imageUrl", imageUrl)
                            .await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun obtenerSiguienteOrden(): Long {
        return try {
            val contadorDoc = db.collection("contadores").document("ordenProductos")
            db.runTransaction { transaction ->
                val snapshot = transaction.get(contadorDoc)
                val currentId = snapshot.getLong("ultimoOrden") ?: 0L
                val nextId = currentId + 1L
                transaction.set(contadorDoc, hashMapOf("ultimoOrden" to nextId))
                nextId
            }.await()
        } catch (e: Exception) {
            // En caso de error, obtener el último orden del contador
            val contadorDoc = db.collection("contadores").document("ordenProductos")
            val snapshot = contadorDoc.get().await()
            val lastId = snapshot.getLong("ultimoOrden") ?: 0L
            val nextId = lastId + 1L
            contadorDoc.set(hashMapOf("ultimoOrden" to nextId)).await()
            nextId
        }
    }
    fun hasPendingUploads(): Boolean {
        return _pendingUploads.value.isNotEmpty()
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

    private suspend fun obtenerSiguienteId(): String {
        return try {
            val contadorDoc = db.collection("contadores").document("productos")
            val nextId = db.runTransaction { transaction ->
                val snapshot = transaction.get(contadorDoc)
                val currentId = snapshot.getLong("ultimoId") ?: 0
                val nextId = currentId + 1
                transaction.set(contadorDoc, hashMapOf("ultimoId" to nextId))
                nextId
            }.await()

            // Formatear el ID con "LBM" y el número
            "LBM$nextId" // Esto generará IDs como LBM0001, LBM0002, etc.
        } catch (e: Exception) {
            // En caso de error, obtener el último ID y generar el siguiente
            val lastProduct = db.collection("productos")
                .orderBy("idNumerico", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.getString("idNumerico")

            val lastNumber = lastProduct?.replace("LBM", "")?.toIntOrNull() ?: 0
            "LBM${lastNumber + 1}"
        }
    }

    fun getNombresSimilares(nombre: String): List<String> {
        if (nombre.length < 3) return emptyList()

        return productos.value
            .filter { producto ->
                producto.nombre.contains(nombre, ignoreCase = true) ||
                        nombre.contains(producto.nombre, ignoreCase = true)
            }
            .map { it.nombre }
    }

    // Funciones modificadas para el manejo de selección usando IDs
    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            clearSelection()
        }
    }

    fun toggleProductSelection(producto: Producto) {
        val currentSelection = _selectedProductIds.value.toMutableSet()
        if (currentSelection.contains(producto.id)) {
            currentSelection.remove(producto.id)
        } else {
            currentSelection.add(producto.id)
        }
        _selectedProductIds.value = currentSelection

        // Actualizar el modo de selección
        if (currentSelection.isEmpty()) {
            _isSelectionMode.value = false
        } else if (!_isSelectionMode.value) {
            _isSelectionMode.value = true
        }
    }

    fun isProductSelected(producto: Producto): Boolean {
        return _selectedProductIds.value.contains(producto.id)
    }

    fun selectAllProducts() {
        _selectedProductIds.value = _productos.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedProductIds.value = emptySet()
        _isSelectionMode.value = false
    }

    // Función modificada para usar IDs en la generación de códigos de barras
    fun generateBarcodes(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val selectedIds = _selectedProductIds.value
                if (selectedIds.isEmpty()) {
                    _uiMessage.value = "No hay productos seleccionados"
                    return@launch
                }

                // Obtener los productos completos a partir de los IDs seleccionados
                val selectedProducts = _productos.value.filter { it.id in selectedIds }

                val generator = BarcodeGenerator(context)
                val pdfUri = generator.generateBarcodesAndPDF(selectedProducts)

                withContext(Dispatchers.Main) {
                    showPDFOptions(context, pdfUri)
                    clearSelection()
                    _uiMessage.value = "PDF generado correctamente"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiMessage.value = "Error al generar PDF: ${e.message}"
                }
            }
        }
    }

    fun clearMessage() {
        _uiMessage.value = null
    }


    private fun sharePDF(context: Context, pdfUri: Uri) {
        try {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val shareIntent = Intent.createChooser(intent, "Compartir PDF de códigos de barras")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            _uiMessage.value = "Error al compartir PDF: ${e.message}"
        }
    }

    private fun showPDFOptions(context: Context, pdfUri: Uri) {
        viewModelScope.launch {
            try {
                val intentPreview = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(pdfUri, "application/pdf")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                val intentShare = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                val chooserIntent = Intent.createChooser(intentShare, "Compartir PDF").apply {
                    putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(intentPreview))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(chooserIntent)
            } catch (e: Exception) {
                _uiMessage.value = "Error al abrir opciones de PDF: ${e.message}"
            }
        }
    }

    // Función auxiliar para obtener los productos seleccionados
    fun getSelectedProducts(): List<Producto> {
        val selectedIds = _selectedProductIds.value
        return _productos.value.filter { it.id in selectedIds }
    }
}