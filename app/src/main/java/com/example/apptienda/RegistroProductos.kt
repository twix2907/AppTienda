package com.example.apptienda


import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest.permission.CAMERA
import android.content.Context
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun AddProductScreen(
    viewModel: ProductoViewModel,
    onNavigateBack: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var nombresSimilares by remember { mutableStateOf<List<String>>(emptyList()) }
    var descripcion by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val view = LocalView.current




    var selectedCategorias by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showCategoriasMenu by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    val categorias by viewModel.categorias.collectAsState()

    var showSuccessDialog by remember { mutableStateOf(false) }
    var shouldNavigateBack by remember { mutableStateOf(false) }
    // Estado para la animación del botón
    var isButtonPressed by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()


    var categoriaSearchQuery by remember { mutableStateOf("") }
    var filteredCategorias by remember { mutableStateOf(categorias) }

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

    // Efecto para verificar nombres similares mientras se escribe
    LaunchedEffect(nombre) {
        if (nombre.isNotEmpty()) {
            nombresSimilares = viewModel.getNombresSimilares(nombre)
        } else {
            nombresSimilares = emptyList()
        }
    }

    // Diálogo de éxito
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                if (shouldNavigateBack) onNavigateBack()
            },
            title = { Text("Éxito") },
            text = { Text("Producto guardado. La imagen se subirá en segundo plano.") },
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

    // Diálogo de selección de imagen
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Producto") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
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
                    .verticalScroll(rememberScrollState()) // Agregar scroll
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
                    Box(modifier = Modifier.fillMaxWidth()) {
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

                        if (nombresSimilares.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 60.dp) // Ajusta según necesites
                                    .shadow(4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "¡Productos similares encontrados!",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    nombresSimilares.take(3).forEach { nombreSimilar ->
                                        Text(
                                            text = "• $nombreSimilar",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                    if (nombresSimilares.size > 3) {
                                        Text(
                                            text = "... y ${nombresSimilares.size - 3} más",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

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
                    // Selector de categorías con menú desplegable anclado
                    CategoriesSelector(
                        categoriaSearchQuery = categoriaSearchQuery,
                        onQueryChange = { categoriaSearchQuery = it },
                        selectedCategorias = selectedCategorias,
                        onSelectionChange = { selectedCategorias = it },
                        categorias = categorias,
                        onAddNewCategory = { showNewCategoryDialog = true }, // Este ya no se usará pero lo mantenemos por compatibilidad
                        viewModel = viewModel // Añadir el ViewModel
                    )

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
                                                    selectedCategorias =
                                                        selectedCategorias + categoriaId
                                                    showNewCategoryDialog = false
                                                }.onFailure { error ->
                                                    errorMessage = error.message
                                                        ?: "Error al crear la categoría"
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
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(imageUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Imagen seleccionada",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        onLoading = { isLoading = true },
                                        onSuccess = { isLoading = false },
                                        onError = {
                                            isLoading = false
                                            Log.e(
                                                "Image",
                                                "Error loading image: ${it.result.throwable}"
                                            )
                                        }
                                    )
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Done,
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

                        // Activar animación
                        isButtonPressed = true

                        scope.launch(Dispatchers.IO) {
                            viewModel.agregarProducto(
                                nombre = nombre,
                                precio = precioDouble,
                                descripcion = descripcion,
                                categorias = selectedCategorias.toList(),
                                imageUri = imageUri,
                                context = context
                            )
                            // Navegar inmediatamente después de guardar los datos básicos
                            withContext(Dispatchers.Main) {
                                onNavigateBack()
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
                        Text("Guardar Producto")
                    }
                }
            }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesSelector(
    categoriaSearchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedCategorias: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    categorias: List<Categoria>,
    onAddNewCategory: () -> Unit,
    viewModel: ProductoViewModel // Añadir el ViewModel como parámetro
) {
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = categoriaSearchQuery,
                onValueChange = {
                    onQueryChange(it)
                    expanded = true
                },
                label = { Text("Categorías") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                singleLine = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                    onQueryChange("")
                },
                modifier = Modifier.exposedDropdownSize()
            ) {
                // Opción de agregar siempre visible al inicio
                DropdownMenuItem(
                    text = {
                        Text(
                            if (categoriaSearchQuery.isBlank()) "Agregar nueva categoría"
                            else "Agregar \"$categoriaSearchQuery\""
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = {
                        if (categoriaSearchQuery.isNotBlank()) {
                            scope.launch {
                                viewModel.agregarCategoria(
                                    nombre = categoriaSearchQuery,
                                    descripcion = ""
                                ).onSuccess { categoriaId ->
                                    onSelectionChange(selectedCategorias + categoriaId)
                                    expanded = false
                                    onQueryChange("")
                                }
                            }
                        }
                    }
                )

                Divider()

                categorias.filter {
                    it.nombre.contains(categoriaSearchQuery, ignoreCase = true)
                }.forEach { categoria ->
                    DropdownMenuItem(
                        text = { Text(categoria.nombre) },
                        onClick = {
                            onSelectionChange(
                                if (selectedCategorias.contains(categoria.id)) {
                                    selectedCategorias - categoria.id
                                } else {
                                    selectedCategorias + categoria.id
                                }
                            )
                        },
                        trailingIcon = if (selectedCategorias.contains(categoria.id)) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null
                    )
                }
            }
        }

        if (selectedCategorias.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    selectedCategorias.mapNotNull { categoriaId ->
                        categorias.find { it.id == categoriaId }
                    }
                ) { categoria ->
                    FilterChip(
                        selected = true,
                        onClick = {
                            onSelectionChange(selectedCategorias - categoria.id)
                        },
                        label = { Text(categoria.nombre) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "Eliminar")
                        }
                    )
                }
            }
        }
    }
}
// Función auxiliar para crear el archivo de imagen
fun createImageFileUri(context: Context): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val imageFile = File.createTempFile(
        imageFileName,
        ".jpg",
        storageDir
    ).apply {
        Log.d("FileCreation", "Created file at: $absolutePath")
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        imageFile
    ).also {
        Log.d("FileCreation", "Created URI: $it")
    }
}