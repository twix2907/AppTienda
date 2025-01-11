package com.example.apptienda

import BarcodeScannerScreen
import android.Manifest.permission.CAMERA
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showImageDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val view = LocalView.current

    // Nuevos estados para el manejo del ID
    var conId by remember { mutableStateOf(!producto?.idNumerico.isNullOrEmpty()) }
    var idNumerico by remember { mutableStateOf(producto?.idNumerico ?: "") }
    var isBarcodeScannerVisible by remember { mutableStateOf(false) }

    // Estados para campos adicionales
    var camposAdicionales by remember {
        mutableStateOf(producto?.camposAdicionales?.map { it.key to it.value }?.toMutableList() ?: mutableListOf())
    }
    var mensajeError by remember { mutableStateOf("") }
    val listaCampos by viewModel.campos.collectAsState()

    // Variables para el nuevo campo
    var nuevoCampoClave by remember { mutableStateOf("") }
    var nuevoCampoValor by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    // Estados para categorías
    var selectedCategorias by remember {
        mutableStateOf<Set<String>>(producto?.categorias?.toSet() ?: emptySet())
    }
    var showCategoriasMenu by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    val categorias by viewModel.categorias.collectAsState()

    var categoriaSearchQuery by remember { mutableStateOf("") }
    var filteredCategorias by remember { mutableStateOf(categorias) }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var shouldNavigateBack by remember { mutableStateOf(false) }
    var isButtonPressed by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // [Los launchers se mantienen igual]
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUris = imageUris + it
            showPreview = true
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempImageUri?.let { uri ->
                imageUris = imageUris + uri
                showPreview = true
            }
        } else {
            tempImageUri = null
            showPreview = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val uri = createImageFileUri(context)
            tempImageUri = uri
            cameraLauncher.launch(uri)
        }
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        // Switch para alternar entre con/sin ID
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (conId) "Con ID" else "Sin ID",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Switch(
                                checked = conId,
                                onCheckedChange = {
                                    conId = it
                                    if (!it) idNumerico = ""
                                }
                            )
                        }

                        // Campo de ID que solo se muestra si conId es true
                        AnimatedVisibility(
                            visible = conId,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            OutlinedTextField(
                                value = idNumerico,
                                onValueChange = { idNumerico = it },
                                label = { Text("ID del producto") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Info, contentDescription = null)
                                },
                                trailingIcon = {
                                    Row {
                                        if (idNumerico.isNotEmpty()) {
                                            IconButton(onClick = { idNumerico = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                            }
                                        }
                                        IconButton(onClick = { isBarcodeScannerVisible = true }) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Escanear código de barras"
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        // [Resto de campos igual que en AddProductScreen]
                        OutlinedTextField(
                            value = nombre,
                            onValueChange = { nombre = it },
                            label = { Text("Nombre del producto") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Create, contentDescription = null)
                            },
                            trailingIcon = {
                                if (nombre.isNotEmpty()) {
                                    IconButton(onClick = { nombre = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                    }
                                }
                            }
                        )

                        OutlinedTextField(
                            value = descripcion,
                            onValueChange = { descripcion = it },
                            label = { Text("Descripción") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            leadingIcon = {
                                Icon(Icons.Default.Info, contentDescription = null)
                            },
                            trailingIcon = {
                                if (descripcion.isNotEmpty()) {
                                    IconButton(onClick = { descripcion = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                    }
                                }
                            }
                        )

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
                            trailingIcon = {
                                if (precio.isNotEmpty()) {
                                    IconButton(onClick = { precio = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                    }
                                }
                            },
                            prefix = { Text("$") }
                        )

                        Text("Campos adicionales", style = MaterialTheme.typography.titleMedium)

                        // Campos adicionales
                        camposAdicionales.forEachIndexed { index, campo ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                var expanded by remember { mutableStateOf(false) }

                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = campo.first,
                                        onValueChange = { newValue ->
                                            camposAdicionales = camposAdicionales.toMutableList().apply {
                                                this[index] = newValue to campo.second
                                            }
                                        },
                                        label = { Text("Nombre del campo") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .menuAnchor(),
                                        singleLine = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                        }
                                    )

                                    ExposedDropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        listaCampos.forEach { nombreCampo ->
                                            DropdownMenuItem(
                                                text = { Text(nombreCampo) },
                                                onClick = {
                                                    camposAdicionales = camposAdicionales.toMutableList().apply {
                                                        this[index] = nombreCampo to campo.second
                                                    }
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = campo.second,
                                    onValueChange = { newValue ->
                                        camposAdicionales = camposAdicionales.toMutableList().apply {
                                            this[index] = campo.first to newValue
                                        }
                                    },
                                    label = { Text("Valor del campo") },
                                    modifier = Modifier.weight(1.5f),
                                    singleLine = true
                                )

                                IconButton(
                                    onClick = {
                                        camposAdicionales = camposAdicionales.toMutableList().apply {
                                            removeAt(index)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar campo")
                                }
                            }
                        }

                        Button(
                            onClick = {
                                camposAdicionales = camposAdicionales.toMutableList().apply {
                                    add("" to "")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Agregar campo")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Agregar campo")
                        }

                        // Selector de categorías
                        CategoriesSelector(
                            categoriaSearchQuery = categoriaSearchQuery,
                            onQueryChange = { categoriaSearchQuery = it },
                            selectedCategorias = selectedCategorias,
                            onSelectionChange = { selectedCategorias = it },
                            categorias = categorias,
                            onAddNewCategory = { showNewCategoryDialog = true },
                            viewModel = viewModel
                        )

                        // Campo de imagen
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Botón para agregar imagen
                            item {
                                OutlinedCard(
                                    modifier = Modifier
                                        .size(200.dp)
                                        .clickable { showImageDialog = true }
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Info,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Agregar imagen",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            // Imágenes existentes del producto
                            items(producto?.imageUrls ?: emptyList()) { imageUrl ->
                                Box(
                                    modifier = Modifier.size(200.dp)
                                ) {
                                    OutlinedCard(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(imageUrl.replace("http://", "https://"))
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            // Botón para eliminar imagen
                                            IconButton(
                                                onClick = {
                                                    // Aquí agregar lógica para eliminar imagen
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(32.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                                        shape = CircleShape
                                                    )
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Eliminar imagen",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Nuevas imágenes seleccionadas
                            items(imageUris) { uri ->
                                Box(
                                    modifier = Modifier.size(200.dp)
                                ) {
                                    OutlinedCard(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box {
                                            AsyncImage(
                                                model = uri,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            IconButton(
                                                onClick = { imageUris = imageUris.filter { it != uri } },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(32.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                                        shape = CircleShape
                                                    )
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Eliminar imagen",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            // Botón de guardar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        if (nombre.isBlank() || precio.isBlank()) return@Button

                        val precioDouble = precio.toDoubleOrNull() ?: return@Button
                        val camposMap = camposAdicionales.associate { it.first to it.second }

                        isButtonPressed = true

                        scope.launch(Dispatchers.IO) {
                            val productoActualizado = producto?.copy(
                                id = productId,
                                idNumerico = if (conId) idNumerico else "",
                                nombre = nombre,
                                descripcion = descripcion,
                                precio = precioDouble,
                                categorias = selectedCategorias.toList(),
                                camposAdicionales = camposMap
                            )

                            if (productoActualizado != null) {
                                viewModel.actualizarProducto(
                                    productoActualizado,
                                    if (imageUris.isNotEmpty()) imageUris else null,
                                    context
                                ).onSuccess {
                                    withContext(Dispatchers.Main) {
                                        showSuccessDialog = true
                                        shouldNavigateBack = true
                                    }
                                }.onFailure { error ->
                                    withContext(Dispatchers.Main) {
                                        errorMessage = error.message ?: "Error al actualizar el producto"
                                        showErrorDialog = true
                                        isButtonPressed = false
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(if (isButtonPressed) 0.95f else 1f)
                        .animateContentSize(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isButtonPressed) {
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

    // Diálogos
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                if (shouldNavigateBack) onNavigateBack()
            },
            title = { Text("Éxito") },
            text = { Text("Producto actualizado correctamente.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        if (shouldNavigateBack) onNavigateBack()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

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

    if (isBarcodeScannerVisible) {
        Dialog(
            onDismissRequest = { isBarcodeScannerVisible = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            BarcodeScannerScreen(
                onBarcodeDetected = { code ->
                    idNumerico = code
                    isBarcodeScannerVisible = false
                },
                onClose = { isBarcodeScannerVisible = false }
            )
        }
    }
}