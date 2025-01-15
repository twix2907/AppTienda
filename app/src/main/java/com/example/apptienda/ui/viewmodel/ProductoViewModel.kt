package com.example.apptienda.ui.viewmodel

import android.content.Context
import android.content.Intent

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

import android.net.Uri
import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apptienda.ui.components.barcode.BarcodeGenerator
import com.example.apptienda.data.repository.CloudinaryRepository
import com.example.apptienda.data.repository.FirestoreRepository
import com.example.apptienda.domain.model.Categoria
import com.example.apptienda.domain.model.Producto
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
    private val firestoreRepository: FirestoreRepository,
    private val cloudinaryRepository: CloudinaryRepository
) : ViewModel() {

    private val _productos = MutableStateFlow<List<Producto>>(emptyList())
    val productos: StateFlow<List<Producto>> = _productos

    private val _categorias = MutableStateFlow<List<Categoria>>(emptyList())
    val categorias: StateFlow<List<Categoria>> = _categorias

    private val _pendingUploads = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val pendingUploads: StateFlow<Map<String, Boolean>> = _pendingUploads

    private val _selectedProductIds = mutableStateOf<Set<String>>(emptySet())
    val selectedProductIds: State<Set<String>> = _selectedProductIds

    private val _isSelectionMode = mutableStateOf(false)
    val isSelectionMode: State<Boolean> = _isSelectionMode

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage

    private val _campos = MutableStateFlow<List<String>>(emptyList())
    val campos: StateFlow<List<String>> = _campos

    init {
        cargarCategorias()
        cargarProductos()
        cargarCampos()
    }

    private fun cargarCategorias() {
        viewModelScope.launch {
            firestoreRepository.getCategorias().collect { categoriasList ->
                _categorias.value = categoriasList
            }
        }
    }

    private fun cargarProductos() {
        viewModelScope.launch {
            firestoreRepository.getProductos().collect { productosList ->
                _productos.value = productosList
            }
        }
    }

    private fun cargarCampos() {
        viewModelScope.launch {
            firestoreRepository.getCampos().collect { camposList ->
                _campos.value = camposList
            }
        }
    }

    fun showErrorMessage(message: String) {
        _uiMessage.value = message
    }

    fun clearMessage() {
        _uiMessage.value = null
    }

    suspend fun agregarProducto(
        nombre: String,
        precio: Double,
        descripcion: String,
        categorias: List<String>,
        imageUris: List<Uri>,
        context: Context,
        camposAdicionales: Map<String, String>,
        idNumerico: String
    ): Result<String> {
        return try {
            val idOrden = firestoreRepository.obtenerSiguienteOrden()

            val producto = Producto(
                idNumerico = idNumerico,
                nombre = nombre,
                precio = precio,
                descripcion = descripcion,
                imageUrls = emptyList(),
                categorias = categorias,
                idOrdenNumerico = idOrden,
                camposAdicionales = camposAdicionales
            )

            val productId = firestoreRepository.agregarProducto(producto).getOrThrow()

            // Subir imágenes en segundo plano
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val imageUrls = imageUris.mapNotNull { uri ->
                        try {
                            cloudinaryRepository.uploadImage(uri, context)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (imageUrls.isNotEmpty()) {
                        firestoreRepository.actualizarProducto(
                            producto.copy(
                                id = productId,
                                imageUrls = imageUrls
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Result.success(productId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizarProducto(
        producto: Producto,
        newImageUris: List<Uri>?,
        context: Context
    ): Result<Unit> {
        return try {
            val imageUrls = if (newImageUris != null) {
                newImageUris.mapNotNull { uri ->
                    try {
                        cloudinaryRepository.uploadImage(uri, context)
                    } catch (e: Exception) {
                        null
                    }
                }
            } else {
                producto.imageUrls
            }

            firestoreRepository.actualizarProducto(
                producto.copy(imageUrls = imageUrls)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarProducto(productoId: String): Result<Unit> {
        return firestoreRepository.eliminarProducto(productoId)
    }

    fun getCategoriaById(id: String): Categoria? {
        return _categorias.value.find { it.id == id }
    }

    suspend fun agregarCategoria(nombre: String, descripcion: String = ""): Result<String> {
        return firestoreRepository.agregarCategoria(
            Categoria(nombre = nombre, descripcion = descripcion)
        )
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

    fun generateBarcodes(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val selectedIds = _selectedProductIds.value
                if (selectedIds.isEmpty()) {
                    _uiMessage.value = "No hay productos seleccionados"
                    return@launch
                }

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

    fun showPDFOptions(context: Context, pdfUri: Uri) {
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

    fun getProductById(productId: String): Producto? {
        return productos.value.find { it.id == productId }
    }
}