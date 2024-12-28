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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    viewModel: ProductoViewModel,
    onNavigateBack: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                                            Log.e("Image", "Error loading image: ${it.result.throwable}")
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

            // Botón de guardar
            Button(
                onClick = {
                    if (nombre.isBlank() || precio.isBlank()) {
                        errorMessage = "Por favor completa todos los campos requeridos"
                        showErrorDialog = true
                        return@Button
                    }

                    val precioDouble = precio.toDoubleOrNull()
                    if (precioDouble == null) {
                        errorMessage = "Por favor ingresa un precio válido"
                        showErrorDialog = true
                        return@Button
                    }

                    isLoading = true
                    scope.launch {
                        try {
                            viewModel.agregarProducto(
                                nombre = nombre,
                                precio = precioDouble,
                                descripcion = descripcion,
                                imageUri = imageUri,
                                context = context
                            ).onSuccess {
                                onNavigateBack()
                            }.onFailure { error ->
                                errorMessage = error.message ?: "Error al guardar el producto"
                                showErrorDialog = true
                            }
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Done, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar Producto")
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