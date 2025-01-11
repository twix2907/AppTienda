package com.example.apptienda

import BarcodeScannerScreen
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.request.crossfade
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

// Enum para los tipos de vista
enum class ViewType {
    DETAILED,
    GRID,
    COMPACT,
    SIMPLE
}

enum class SortType {
    ALPHABETICAL_ASC,
    ALPHABETICAL_DESC,
    ID_ASC,
    ID_DESC,
    PRICE_LOW_TO_HIGH,
    PRICE_HIGH_TO_LOW
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: ProductoViewModel,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToEditProduct: (Producto) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var currentViewType by remember { mutableStateOf(ViewType.DETAILED) }
    var currentSortType by remember { mutableStateOf(SortType.ID_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }
    var isBarcodeScannerVisible by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val uiMessage by viewModel.uiMessage.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode
    val selectedProductIds by viewModel.selectedProductIds
    val context = LocalContext.current
    val productos by viewModel.productos.collectAsState()
    val categorias by viewModel.categorias.collectAsState()

    // Launcher para solicitar permisos
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.generateBarcodes(context)
        }
    }
    val scope = rememberCoroutineScope()

    // Mostrar mensaje si existe
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessage()
        }
    }

    // Función para verificar y solicitar permisos
    fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            viewModel.generateBarcodes(context)
        } else {
            launcher.launch(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedProductIds.size} seleccionados") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, "Cancelar selección")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAllProducts() }) {
                            Icon(Icons.Default.Done, "Seleccionar todo")
                        }
                        IconButton(
                            onClick = { checkAndRequestPermissions() },
                            enabled = selectedProductIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Email, "Generar códigos")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = { Text("Catálogo de Productos") },
                    actions = {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.List, contentDescription = "Ordenar")
                        }
                        // Nuevo menú de 3 puntos
                        var showOverflowMenu by remember { mutableStateOf(false) }
                        var showGenerateDialog by remember { mutableStateOf(false) }

                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Generar códigos de barra") },
                                onClick = {
                                    showOverflowMenu = false
                                    showGenerateDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null)
                                }
                            )
                        }
                        if (showGenerateDialog) {
                            GenerateBarcodesDialog(
                                onDismiss = { showGenerateDialog = false },
                                onGenerate = { startId, count ->
                                    scope.launch {
                                        val barcodeGenerator = BarcodeGenerator(context)
                                        val productos = (startId until startId + count).map { id ->
                                            Producto(
                                                id = "GEN_$id",
                                                idNumerico = "LBM$id",
                                                nombre = "Código $id",
                                                precio = 0.0
                                            )
                                        }
                                        try {
                                            val pdfUri = barcodeGenerator.generateBarcodesAndPDF(productos)
                                            viewModel.showPDFOptions(context, pdfUri)
                                        } catch (e: Exception) {
                                            viewModel.showErrorMessage("Error al generar PDF: ${e.message}")
                                        }
                                    }
                                }
                            )
                        }
                        IconButton(
                            onClick = {
                                currentViewType = when (currentViewType) {
                                    ViewType.DETAILED -> ViewType.GRID
                                    ViewType.GRID -> ViewType.COMPACT
                                    ViewType.COMPACT -> ViewType.SIMPLE
                                    ViewType.SIMPLE -> ViewType.DETAILED
                                }
                            }
                        ) {
                            Icon(
                                when (currentViewType) {
                                    ViewType.DETAILED -> Icons.Default.Menu
                                    ViewType.GRID -> Icons.Default.MoreVert
                                    ViewType.COMPACT -> Icons.Default.List
                                    ViewType.SIMPLE -> Icons.Default.KeyboardArrowDown
                                },
                                contentDescription = "Cambiar vista"
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            // Opciones de ordenamiento
                            DropdownMenuItem(
                                text = { Text("Alfabético (A-Z)") },
                                onClick = {
                                    currentSortType = SortType.ALPHABETICAL_ASC
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = null,
                                        modifier = Modifier.rotate(
                                            if (currentSortType == SortType.ALPHABETICAL_ASC) 0f else 180f
                                        )
                                    )
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("ID (menor a mayor)") },
                                onClick = {
                                    currentSortType = SortType.ID_ASC
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Menu, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("ID (mayor a menor)") },
                                onClick = {
                                    currentSortType = SortType.ID_DESC
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Menu, contentDescription = null)
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Precio (menor a mayor)") },
                                onClick = {
                                    currentSortType = SortType.PRICE_LOW_TO_HIGH
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Menu, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Precio (mayor a menor)") },
                                onClick = {
                                    currentSortType = SortType.PRICE_HIGH_TO_LOW
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Menu, contentDescription = null)
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddProduct,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Producto")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchBar(
                windowInsets = WindowInsets(top = 0.dp),
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { },
                active = false,
                onActiveChange = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 1.dp),
                placeholder = { Text("Buscar productos por nombre o id...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    Row {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
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
            ) { }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategories.isEmpty(),
                        onClick = { selectedCategories = emptySet() },
                        label = { Text("Todos") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                items(categorias) { categoria ->
                    FilterChip(
                        selected = selectedCategories.contains(categoria.id),
                        onClick = {
                            selectedCategories = if (selectedCategories.contains(categoria.id)) {
                                selectedCategories.minus(categoria.id)
                            } else {
                                selectedCategories.plus(categoria.id)
                            }
                        },
                        label = { Text(categoria.nombre) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Filtrar y ordenar productos
            val filteredAndSortedProducts = productos
                .filter { producto ->
                    val productCategoryNames = producto.categorias.mapNotNull { catId ->
                        categorias.find { it.id == catId }?.nombre
                    }
                    val matchesSearch = producto.nombre.contains(searchQuery, ignoreCase = true) ||
                            producto.idNumerico.toString().equals(searchQuery)
                    val matchesCategoryName = productCategoryNames.any { catName ->
                        catName.contains(searchQuery, ignoreCase = true)
                    }
                    val matchesCategory = selectedCategories.isEmpty() ||
                            selectedCategories.all { it in producto.categorias }
                    matchesSearch && matchesCategory || matchesCategory && matchesCategoryName
                }
                .sortedWith(
                    when (currentSortType) {
                        SortType.ALPHABETICAL_ASC -> compareBy { it.nombre.lowercase() }
                        SortType.ALPHABETICAL_DESC -> compareByDescending { it.nombre.lowercase() }
                        SortType.ID_ASC -> compareBy { it.idOrdenNumerico }
                        SortType.ID_DESC -> compareByDescending { it.idOrdenNumerico }
                        SortType.PRICE_LOW_TO_HIGH -> compareBy { it.precio }
                        SortType.PRICE_HIGH_TO_LOW -> compareByDescending { it.precio }
                    }
                )

            when (currentViewType) {
                ViewType.DETAILED -> DetailedProductList(
                    productos = filteredAndSortedProducts,
                    viewModel = viewModel,
                    onNavigateToEdit = onNavigateToEditProduct
                )
                ViewType.GRID -> GridProductList(
                    productos = filteredAndSortedProducts,
                    viewModel = viewModel,
                    onNavigateToEdit = onNavigateToEditProduct
                )
                ViewType.COMPACT -> CompactProductList(
                    productos = filteredAndSortedProducts,
                    viewModel = viewModel,
                    onNavigateToEdit = onNavigateToEditProduct
                )
                ViewType.SIMPLE -> SimpleProductList(
                    productos = filteredAndSortedProducts,
                    viewModel = viewModel,
                    onNavigateToEdit = onNavigateToEditProduct
                )
            }
        }
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
                    searchQuery = code
                    isBarcodeScannerVisible = false
                },
                onClose = { isBarcodeScannerVisible = false }
            )
        }
    }
}
@Composable
fun DetailedProductList(
    productos: List<Producto>,
    viewModel: ProductoViewModel,
    onNavigateToEdit: (Producto) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(productos) { producto ->
            ProductCard(producto, viewModel, onNavigateToEdit)
        }
    }
}

private fun needsExpansion(
    descripcion: String,
    categorias: List<String>,
    camposAdicionales: Map<String, String>,
    isExpanded: Boolean
): Boolean {
    if (isExpanded) return false  // Si ya está expandido, no es necesario "ver más"

    val descripcionNeedsTruncation = descripcion.count { it == '\n' } > 1 ||
            descripcion.length > 100
    val categoriasNeedExpansion = categorias.size > 2
    val tieneCamposAdicionales = camposAdicionales.isNotEmpty()

    return descripcionNeedsTruncation || categoriasNeedExpansion || tieneCamposAdicionales
}
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ProductCard(
    producto: Producto,
    viewModel: ProductoViewModel,
    onNavigateToEdit: (Producto) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showImagePreview by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isSelectionMode by viewModel.isSelectionMode
    // Usar remember y derivedStateOf para manejar el estado de selección
    val isSelected by remember(producto.id, viewModel.selectedProductIds.value) {
        derivedStateOf { viewModel.isProductSelected(producto) }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        viewModel.toggleProductSelection(producto)
                    } else {
                        isExpanded = !isExpanded
                    }
                },
                onLongClick = {
                    if (!isSelectionMode) {
                        viewModel.toggleSelectionMode()
                        viewModel.toggleProductSelection(producto)
                    }
                }
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {


            // Contenido principal del ProductCard
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { viewModel.toggleProductSelection(producto) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = producto.nombre,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(
                            start = if (isSelectionMode) 0.dp else 8.dp
                        )

                    )

                    Text(
                        text = "#${producto.idNumerico}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = producto.descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "S/. ${String.format("%.2f", producto.precio)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    // Categorías
                    if (producto.categorias.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val categoriasToShow = if (!isExpanded && producto.categorias.size > 2) {
                                producto.categorias.take(2)
                            } else {
                                producto.categorias
                            }

                            categoriasToShow.forEach { categoriaId ->
                                viewModel.getCategoriaById(categoriaId)?.let { categoria ->
                                    AssistChip(
                                        onClick = { },
                                        label = {
                                            Text(
                                                text = categoria.nombre,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }

                            if (!isExpanded && producto.categorias.size > 2) {
                                AssistChip(
                                    onClick = { },
                                    label = {
                                        Text(
                                            text = "+${producto.categorias.size - 2}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                    }

                    if (needsExpansion(producto.descripcion, producto.categorias, producto.camposAdicionales, isExpanded)) {
                        Text(
                            text = "ver más...",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .align(Alignment.End)
                        )
                    }
                    // Mostrar campos adicionales solo si la tarjeta está expandida
                    if (isExpanded && producto.camposAdicionales.isNotEmpty()) {
                        producto.camposAdicionales.forEach { (clave, valor) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "$clave:",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = valor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Box {
                    Card(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(MaterialTheme.shapes.medium)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(if (producto.imageUrl.isNullOrEmpty()) {
                                    R.drawable.placeholder
                                } else {
                                    producto.imageUrl.replace("http://", "https://")
                                })
                                .crossfade(true)
                                .build(),
                            contentDescription = producto.nombre,
                            modifier = Modifier
                                .fillMaxSize()
                                .combinedClickable(
                                    onClick = { },
                                    onLongClick = { showImagePreview = true }
                                ),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .align(Alignment.TopEnd)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = CircleShape
                            )
                    ) {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Opciones",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text("Editar")
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onNavigateToEdit(producto)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            "Eliminar",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    // Diálogos para el ProductCard
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar producto") },
            text = { Text("¿Estás seguro de que quieres eliminar este producto?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.eliminarProducto(producto.id)
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showImagePreview) {
        dialogoVistaPrevia(
            ondismiss = { showImagePreview = false },
            imageUrl = producto.imageUrl.replace("http://", "https://")
        )
    }
}

@Composable
private fun ZoomableImage(
    imageUrl: String,
    contentDescription: String?
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 3f)
        if (scale > 1f) {
            offsetX += offsetChange.x
            offsetY += offsetChange.y
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
                .transformable(state = state),
            contentScale = ContentScale.FillWidth
        )
    }
}

@Composable
fun GridProductList(
    productos: List<Producto>,
    viewModel: ProductoViewModel,
    onNavigateToEdit: (Producto) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(productos) { producto ->
            GridProductCard(producto, viewModel, onNavigateToEdit)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun GridProductCard(
    producto: Producto,
    viewModel: ProductoViewModel,
    onNavigateToEdit: (Producto) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showImagePreview by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isSelectionMode by viewModel.isSelectionMode
    val isSelected by remember(producto.id, viewModel.selectedProductIds.value) {
        derivedStateOf { viewModel.isProductSelected(producto) }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        viewModel.toggleProductSelection(producto)
                    } else {
                        isExpanded = !isExpanded
                    }
                },
                onLongClick = {
                    if (!isSelectionMode) {
                        viewModel.toggleSelectionMode()
                        viewModel.toggleProductSelection(producto)
                    }
                }
            )
    ) {
        Box {
            Column {
                Box {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(if (producto.imageUrl.isNullOrEmpty()) {
                                R.drawable.placeholder
                            } else {
                                producto.imageUrl.replace("http://", "https://")
                            })
                            .crossfade(true)
                            .build(),
                        contentDescription = producto.nombre,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .combinedClickable(
                                onClick = { },
                                onLongClick = { showImagePreview = true }
                            ),
                        contentScale = ContentScale.Crop
                    )

                    // Checkbox en modo selección
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { viewModel.toggleProductSelection(producto) },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        )
                    }

                    // Menú de opciones
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .align(Alignment.TopEnd)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = CircleShape
                            )
                    ) {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Opciones",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text("Editar")
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onNavigateToEdit(producto)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            "Eliminar",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }

                // Información del producto
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = producto.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "#${producto.idNumerico}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "S/. ${String.format("%.2f", producto.precio)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )


                    if (isExpanded) {
                        Text(
                            text = producto.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Categorías expandidas
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            producto.categorias.forEach { categoriaId ->
                                viewModel.getCategoriaById(categoriaId)?.let { categoria ->
                                    AssistChip(
                                        onClick = { },
                                        label = {
                                            Text(
                                                text = categoria.nombre,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }
                        }
                    }
                    // Mostrar campos adicionales solo si la tarjeta está expandida
                    if (isExpanded && producto.camposAdicionales.isNotEmpty()) {
                        producto.camposAdicionales.forEach { (clave, valor) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "$clave:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = valor,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Diálogos
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Eliminar producto") },
                text = { Text("¿Estás seguro de que quieres eliminar este producto?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                viewModel.eliminarProducto(producto.id)
                            }
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showImagePreview) {
            dialogoVistaPrevia(
                ondismiss = { showImagePreview = false },
                imageUrl = producto.imageUrl.replace("http://", "https://")
            )
        }
    }
}

@Composable
fun CompactProductList(
    productos: List<Producto>,
    viewModel: ProductoViewModel,
    onNavigateToEdit: (Producto) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(productos) { producto ->
            CompactProductCard(producto, viewModel, onNavigateToEdit)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun CompactProductCard(
    producto: Producto,
    viewModel: ProductoViewModel,
    onNavigateToEdit: (Producto) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showImagePreview by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isSelectionMode by viewModel.isSelectionMode
    val isSelected by remember(producto.id, viewModel.selectedProductIds.value) {
        derivedStateOf { viewModel.isProductSelected(producto) }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        viewModel.toggleProductSelection(producto)
                    } else {
                        isExpanded = !isExpanded
                    }
                },
                onLongClick = {
                    if (!isSelectionMode) {
                        viewModel.toggleSelectionMode()
                        viewModel.toggleProductSelection(producto)
                    }
                }
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                Card(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(MaterialTheme.shapes.small)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(if (producto.imageUrl.isNullOrEmpty()) {
                                R.drawable.placeholder
                            } else {
                                producto.imageUrl.replace("http://", "https://")
                            })
                            .crossfade(true)
                            .build(),
                        contentDescription = producto.nombre,
                        modifier = Modifier
                            .fillMaxSize()
                            .combinedClickable(
                                onClick = { },
                                onLongClick = { showImagePreview = true }
                            ),
                        contentScale = ContentScale.Crop
                    )
                }

                // Checkbox en modo selección
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { viewModel.toggleProductSelection(producto) },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    )
                }
            }

            // Información del producto
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "#${producto.idNumerico}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "S/. ${String.format("%.2f", producto.precio)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                if (isExpanded) {
                    Text(
                        text = producto.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        producto.categorias.forEach { categoriaId ->
                            viewModel.getCategoriaById(categoriaId)?.let { categoria ->
                                AssistChip(
                                    onClick = { },
                                    label = {
                                        Text(
                                            text = categoria.nombre,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                    }
                }
                // Mostrar campos adicionales solo si la tarjeta está expandida
                if (isExpanded && producto.camposAdicionales.isNotEmpty()) {
                    producto.camposAdicionales.forEach { (clave, valor) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$clave:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = valor,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            // Menú de opciones para CompactProductCard
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("Editar")
                            }
                        },
                        onClick = {
                            showMenu = false
                            onNavigateToEdit(producto)
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "Eliminar",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // Diálogos para CompactProductCard
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar producto") },
            text = { Text("¿Estás seguro de que quieres eliminar este producto?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.eliminarProducto(producto.id)
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showImagePreview) {
        dialogoVistaPrevia(
            ondismiss = { showImagePreview = false },
            imageUrl = producto.imageUrl.replace("http://", "https://")
        )
    }
}

@Composable
fun SimpleProductList(
    productos: List<Producto>,
    viewModel: ProductoViewModel,
    onNavigateToEdit: (Producto) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(productos) { producto ->
            SimpleProductCard(producto, viewModel, onNavigateToEdit)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimpleProductCard(
    producto: Producto,
    viewModel: ProductoViewModel,
    onNavigateToEdit: (Producto) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isSelectionMode by viewModel.isSelectionMode
    val isSelected by remember(producto.id, viewModel.selectedProductIds.value) {
        derivedStateOf { viewModel.isProductSelected(producto) }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isSelectionMode) {
                    viewModel.toggleProductSelection(producto)
                } else {
                    isExpanded = !isExpanded
                }
            }
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox en modo selección
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { viewModel.toggleProductSelection(producto) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = producto.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "#${producto.idNumerico}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "S/. ${String.format("%.2f", producto.precio)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text("Editar")
                                }
                            },
                            onClick = {
                                showMenu = false
                                onNavigateToEdit(producto)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        "Eliminar",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }

            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = producto.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (producto.categorias.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            producto.categorias.forEach { categoriaId ->
                                viewModel.getCategoriaById(categoriaId)?.let { categoria ->
                                    AssistChip(
                                        onClick = { },
                                        label = {
                                            Text(
                                                text = categoria.nombre,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // Mostrar campos adicionales solo si la tarjeta está expandida
            if (isExpanded && producto.camposAdicionales.isNotEmpty()) {
                producto.camposAdicionales.forEach { (clave, valor) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$clave:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = valor,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Eliminar producto") },
                text = { Text("¿Estás seguro de que quieres eliminar este producto?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                viewModel.eliminarProducto(producto.id)
                            }
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun dialogoVistaPrevia(
    ondismiss: () -> Unit,
    imageUrl: String,
) {
    Dialog(
        onDismissRequest = ondismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            ZoomableImage(
                imageUrl = imageUrl,
                contentDescription = ""
            )

            IconButton(
                onClick = ondismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
