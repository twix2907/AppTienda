package com.example.apptienda

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.request.crossfade
import kotlinx.coroutines.launch

// Enum para los tipos de vista
enum class ViewType {
    DETAILED,    // Vista detallada original
    GRID,        // Vista en cuadrícula
    COMPACT,     // Vista compacta con imagen
    SIMPLE       // Vista simple sin imagen
}
enum class SortType {
    ALPHABETICAL_ASC,      // A-Z
    ALPHABETICAL_DESC,     // Z-A
    ID_ASC,               // ID menor a mayor
    ID_DESC,              // ID mayor a menor
    PRICE_LOW_TO_HIGH,    // Precio bajo a alto
    PRICE_HIGH_TO_LOW     // Precio alto a bajo
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: ProductoViewModel,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToEditProduct: (Producto) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var currentViewType by remember { mutableStateOf(ViewType.DETAILED) }


    var currentSortType by remember { mutableStateOf(SortType.ID_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Obtener productos del ViewModel
    val productos by viewModel.productos.collectAsState()

    // Obtener categorías del ViewModel
    val categorias by viewModel.categorias.collectAsState()


    // Filtrar y ordenar productos
    val filteredAndSortedProducts = productos
        .filter { producto ->
            val matchesSearch = producto.nombre.contains(searchQuery, ignoreCase = true) ||
                    producto.idNumerico.toString().equals(searchQuery) // Búsqueda por ID
            val matchesCategory = selectedCategory == null ||
                    producto.categorias.contains(selectedCategory)
            matchesSearch && matchesCategory
        }
        .sortedWith(
            when (currentSortType) {
                SortType.ALPHABETICAL_ASC -> compareBy { it.nombre.lowercase() }
                SortType.ALPHABETICAL_DESC -> compareByDescending { it.nombre.lowercase() }
                SortType.ID_ASC -> compareBy { it.idNumerico }
                SortType.ID_DESC -> compareByDescending { it.idNumerico }
                SortType.PRICE_LOW_TO_HIGH -> compareBy { it.precio }
                SortType.PRICE_HIGH_TO_LOW -> compareByDescending { it.precio }
            }
        )



    Scaffold(

        topBar = {
            TopAppBar(
                title = { Text("Catálogo de Productos") },
                actions = {
                    // Botón de ordenamiento
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.List, contentDescription = "Ordenar")
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
                    // Menú desplegable de ordenamiento
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
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
                        DropdownMenuItem(
                            text = { Text("Alfabético (Z-A)") },
                            onClick = {
                                currentSortType = SortType.ALPHABETICAL_DESC
                                showSortMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = null
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
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
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
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Todos") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                items(categorias) { categoria ->
                    FilterChip(
                        selected = selectedCategory == categoria.id,
                        onClick = {
                            selectedCategory = if (selectedCategory == categoria.id) null else categoria.id
                        },
                        label = { Text(categoria.nombre) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
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


// Función auxiliar para determinar si se necesita expansión
private fun needsExpansion(
    descripcion: String,
    categorias: List<String>,
    isExpanded: Boolean
): Boolean {
    if (isExpanded) return false

    // La descripción necesita expansión si tiene más de 2 líneas
    val descripcionNeedsTruncation = descripcion.count { it == '\n' } > 1 ||
            descripcion.length > 100

    // Las categorías necesitan expansión si hay más de 2
    val categoriasNeedExpansion = categorias.size > 2

    return descripcionNeedsTruncation || categoriasNeedExpansion
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

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
                .clickable { isExpanded = !isExpanded },
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 6.dp
            )
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = producto.nombre,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis
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

                        // Mostramos las categorías de manera condicional
                        if (producto.categorias.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Si no está expandido, mostrar solo las primeras 2 categorías
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

                                // Si hay más categorías y no está expandido, mostrar indicador
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

                        // Solo mostrar "ver más" si realmente hay contenido adicional para mostrar
                        if (needsExpansion(producto.descripcion, producto.categorias, isExpanded)) {
                            Text(
                                text = "ver más...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontStyle = FontStyle.Italic
                                ),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .align(Alignment.End)
                            )
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
                                        R.drawable.placeholder // Tu imagen placeholder
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
        // Diálogo de vista previa de imagen con zoom
        if (showImagePreview) {
            Dialog(
                onDismissRequest = { showImagePreview = false },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    ZoomableImage(
                        imageUrl = if (producto.imageUrl.isNullOrEmpty()) {
                            R.drawable.placeholder.toString()
                        } else {
                            producto.imageUrl.replace("http://", "https://")
                        },
                        contentDescription = producto.nombre
                    )

                    IconButton(
                        onClick = { showImagePreview = false },
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
            .fillMaxWidth()
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

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clickable { isExpanded = !isExpanded }
    ) {
        Column {
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(if (producto.imageUrl.isNullOrEmpty()) {
                            R.drawable.placeholder // Tu imagen placeholder
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

            // Información apilada verticalmente
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Nombre
                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )

                // ID
                Text(
                    text = "#${producto.idNumerico}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // Precio
                Text(
                    text = "$ ${String.format("%.2f", producto.precio)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // Descripción
                Text(
                    text = producto.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
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

                // Ver más
                if (needsExpansion(producto.descripcion, producto.categorias, isExpanded)) {
                    Text(
                        text = "ver más...",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = FontStyle.Italic
                        ),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp)
                    )
                }
            }
        }
    }

    // Continuación de los diálogos
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
        Dialog(
            onDismissRequest = { showImagePreview = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                ZoomableImage(
                    imageUrl = producto.imageUrl.replace("http://", "https://"),
                    contentDescription = producto.nombre
                )

                IconButton(
                    onClick = { showImagePreview = false },
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

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clickable { isExpanded = !isExpanded }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier
                    .size(84.dp)
                    .clip(MaterialTheme.shapes.small)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(if (producto.imageUrl.isNullOrEmpty()) {
                            R.drawable.placeholder // Tu imagen placeholder
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

            // Información apilada verticalmente (lado derecho)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Nombre
                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )

                // ID
                Text(
                    text = "#${producto.idNumerico}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // Precio
                Text(
                    text = "$ ${String.format("%.2f", producto.precio)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // Descripción
                Text(
                    text = producto.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
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

                // Ver más
                if (needsExpansion(producto.descripcion, producto.categorias, isExpanded)) {
                    Text(
                        text = "ver más...",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = FontStyle.Italic
                        ),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp)
                    )
                }
            }

            // Menú de opciones
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
                    Dialog(
                        onDismissRequest = { showImagePreview = false },
                        properties = DialogProperties(
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            ZoomableImage(
                                imageUrl = producto.imageUrl.replace("http://", "https://"),
                                contentDescription = producto.nombre
                            )

                            IconButton(
                                onClick = { showImagePreview = false },
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
            }
            }
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

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Nombre
                    Text(
                        text = producto.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // ID debajo del nombre
                    Text(
                        text = "#${producto.idNumerico}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Precio
                Text(
                    text = "$ ${String.format("%.2f", producto.precio)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Menú de opciones
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

            // Descripción expandible
            if (isExpanded) {
                Text(
                    text = producto.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Categorías en estado expandido
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

            // "ver más" si hay contenido adicional
            if (!isExpanded && (producto.descripcion.isNotEmpty() || producto.categorias.isNotEmpty())) {
                Text(
                    text = "ver más...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontStyle = FontStyle.Italic
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }
        }
    }

    // Diálogo de confirmación para eliminar
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