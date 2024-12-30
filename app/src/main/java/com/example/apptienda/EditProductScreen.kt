package com.example.apptienda

import android.Manifest.permission.CAMERA
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditProductScreen(
    productId: String,
    viewModel: ProductoViewModel,
    onNavigateBack: () -> Unit
) {
    val producto = remember { viewModel.getProductById(productId) }
    var nombre by remember { mutableStateOf(producto?.nombre ?: "") }
    var descripcion by remember { mutableStateOf(producto?.descripcion ?: "") }
    var precio by remember { mutableStateOf(producto?.precio?.toString() ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Estados para categorías
    var selectedCategorias by remember {
        mutableStateOf<Set<String>>(producto?.categorias?.toSet() ?: emptySet())
    }
    var showCategoriasMenu by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    val categorias by viewModel.categorias.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Para seleccionar de galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        showPreview = true
    }

    // Para tomar foto
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri = tempImageUri
            showPreview = true
            Log.d("Camera", "Photo taken successfully: $imageUri")
        } else {
            Log.e("Camera", "Failed to take photo")
            tempImageUri = null
            showPreview = false
        }
    }

    // Para permisos de cámara
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val uri = createImageFileUri(context)
            tempImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    // Diálogo para seleccionar imagen
    if (showImageDialog) {
        AlertDialog(
            onDismissRequest = { showImageDialog = false },
            title = { Text("Seleccionar imagen") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showImageDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Face, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Seleccionar de galería")
                    }
                    TextButton(
                        onClick = {
                            showImageDialog = false
                            when (PackageManager.PERMISSION_GRANTED) {
                                context.checkSelfPermission(CAMERA) -> {
                                    showPreview = false
                                    tempImageUri = createImageFileUri(context)
                                    cameraLauncher.launch(tempImageUri!!)
                                }
                                else -> {
                                    permissionLauncher.launch(CAMERA)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tomar foto")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImageDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo para nueva categoría
    if (showNewCategoryDialog) {
        var nuevaCategoria by remember { mutableStateOf("") }
        var descripcionCategoria by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text("Nueva categoría") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = nuevaCategoria,
                        onValueChange = { nuevaCategoria = it },
                        label = { Text("Nombre de la categoría") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = descripcionCategoria,
                        onValueChange = { descripcionCategoria = it },
                        label = { Text("Descripción (opcional)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nuevaCategoria.isNotBlank()) {
                            scope.launch {
                                viewModel.agregarCategoria(
                                    nombre = nuevaCategoria,
                                    descripcion = descripcionCategoria
                                ).onSuccess { categoriaId ->
                                    selectedCategorias = selectedCategorias + categoriaId
                                    showNewCategoryDialog = false
                                }.onFailure { error ->
                                    errorMessage = error.message ?: "Error al crear la categoría"
                                    showErrorDialog = true
                                }
                            }
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCategoryDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Producto") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Campo de nombre
                        OutlinedTextField(
                            value = nombre,
                            onValueChange = { nombre = it },
                            label = { Text("Nombre del producto") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Create, contentDescription = null)
                            }
                        )

                        // Campo de descripción
                        OutlinedTextField(
                            value = descripcion,
                            onValueChange = { descripcion = it },
                            label = { Text("Descripción") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            leadingIcon = {
                                Icon(Icons.Default.Info, contentDescription = null)
                            }
                        )

                        // Campo de precio
                        OutlinedTextField(
                            value = precio,
                            onValueChange = { precio = it },
                            label = { Text("Precio") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null)
                            },
                            prefix = { Text("$") }
                        )

                        // Selector de categorías
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCategoriasMenu = true }
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Categorías",
                                        style = MaterialTheme.typography.labelMedium
                                    )

                                    if (selectedCategorias.isEmpty()) {
                                        Text(
                                            text = "Selecciona categorías",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            selectedCategorias.forEach { categoriaId ->
                                                categorias.find { it.id == categoriaId }?.let { categoria ->
                                                    FilterChip(
                                                        selected = true,
                                                        onClick = {
                                                            selectedCategorias = selectedCategorias - categoriaId
                                                        },
                                                        label = { Text(categoria.nombre) },
                                                        trailingIcon = {
                                                            Icon(
                                                                Icons.Default.Close,
                                                                contentDescription = "Eliminar"
                                                            )
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = showCategoriasMenu,
                                onDismissRequest = { showCategoriasMenu = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                categorias.forEach { categoria ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = categoria.nombre,
                                                color = if (selectedCategorias.contains(categoria.id)) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        },
                                        onClick = {
                                            selectedCategorias = if (selectedCategorias.contains(categoria.id)) {
                                                selectedCategorias - categoria.id
                                            } else {
                                                selectedCategorias + categoria.id
                                            }
                                        },
                                        leadingIcon = {
                                            if (selectedCategorias.contains(categoria.id)) {
                                                Icon(
                                                    imageVector = Icons.Default.Done,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        modifier = Modifier.background(
                                            if (selectedCategorias.contains(categoria.id)) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            }
                                        )
                                    )
                                }

                                Divider(modifier = Modifier.padding(vertical = 4.dp))

                                DropdownMenuItem(
                                    text = { Text("Agregar nueva categoría") },
                                    onClick = {
                                        showCategoriasMenu = false
                                        showNewCategoryDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                )
                            }
                        }
                        // Campo de imagen
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clickable { showImageDialog = true }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (showPreview && imageUri != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(imageUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Imagen seleccionada",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (producto?.imageUrl != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(producto.imageUrl.replace("http://", "https://"))
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Imagen actual",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.ShoppingCart,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Toca para agregar imagen",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            // Botón fijo en la parte inferior
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                if (nombre.isBlank() || precio.isBlank()) {
                                    errorMessage = "Por favor completa todos los campos requeridos"
                                    showErrorDialog = true
                                    return@launch
                                }

                                val precioDouble = precio.toDoubleOrNull()
                                if (precioDouble == null) {
                                    errorMessage = "Por favor ingresa un precio válido"
                                    showErrorDialog = true
                                    return@launch
                                }

                                // Crear el producto actualizado manteniendo el idNumerico original
                                val productoActualizado = producto?.copy(
                                    id = productId,
                                    idNumerico = producto.idNumerico, // Mantener el ID numérico original
                                    nombre = nombre,
                                    descripcion = descripcion,
                                    precio = precioDouble,
                                    imageUrl = producto.imageUrl,
                                    categorias = selectedCategorias.toList()
                                )

                                if (productoActualizado != null) {
                                    viewModel.actualizarProducto(
                                        productoActualizado,
                                        imageUri,
                                        context
                                    ).onSuccess {
                                        onNavigateBack()
                                    }.onFailure { error ->
                                        errorMessage = error.message ?: "Error al actualizar el producto"
                                        showErrorDialog = true
                                    }
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Guardar cambios")
                    }
                }
            }
        }
    }

    // Diálogo de error
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}