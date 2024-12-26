package com.example.apptienda

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.KeyboardArrowDown

data class Product(
    val name: String,
    val description: String,
    val category: String,
    val price: Double,
    val imageUrl: String
)
// Enum para los tipos de vista
enum class ViewType {
    DETAILED,    // Vista detallada original
    GRID,        // Vista en cuadrícula
    COMPACT,     // Vista compacta con imagen
    SIMPLE       // Vista simple sin imagen
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(onNavigateToAddProduct: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    var currentViewType by remember { mutableStateOf(ViewType.DETAILED) }

    // Lista de categorías disponibles
    val categories = listOf("Todos", "Electrónicos", "Computadoras", "Accesorios", "Móviles")

    val products = listOf(
        Product(
            "Smartphone XYZ",
            "Un teléfono inteligente de última generación con características increíbles",
            "Electrónicos",
            999.99,
            "url_imagen_1"
        ),
        Product(
            "Laptop Pro",
            "Laptop profesional con gran rendimiento",
            "Computadoras",
            1499.99,
            "url_imagen_2"
        ),
        // Más productos...
    )

    // Filtrar productos basado en la búsqueda y categoría
    val filteredProducts = products.filter { product ->
        val matchesSearch = product.name.contains(searchQuery, ignoreCase = true) ||
                product.description.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == null || selectedCategory == "Todos" ||
                product.category == selectedCategory
        matchesSearch && matchesCategory
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catálogo de Productos") },
                actions = {
                    // Botón para cambiar el tipo de vista
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
                                ViewType.DETAILED -> Icons.Default.Menu         // Vista detallada
                                ViewType.GRID -> Icons.Default.MoreVert        // Vista en cuadrícula
                                ViewType.COMPACT -> Icons.Default.List         // Vista compacta
                                ViewType.SIMPLE -> Icons.Default.KeyboardArrowDown  // Vista simple
                            },
                            contentDescription = "Cambiar vista"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },floatingActionButton = {
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
            // Buscador
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { },
                active = false,
                onActiveChange = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Buscar productos...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                }
            ) { }

            // Filtros de categorías
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = if (selectedCategory == category) null else category
                        },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }


            when (currentViewType) {
                ViewType.DETAILED -> DetailedProductList(filteredProducts)
                ViewType.GRID -> GridProductList(filteredProducts)
                ViewType.COMPACT -> CompactProductList(filteredProducts)
                ViewType.SIMPLE -> SimpleProductList(filteredProducts)
            }
        }
    }
}

@Composable
fun ProductCard(product: Product) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 6.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Información del producto
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                AssistChip(
                    onClick = { },
                    label = { Text(product.category) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
                Text(
                    text = "$ ${String.format("%.2f", product.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Imagen del producto
            Card(
                modifier = Modifier
                    .size(160.dp)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(product.imageUrl),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun GridProductList(products: List<Product>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(products) { product ->
            GridProductCard(product)
        }
    }
}

@Composable
fun GridProductCard(product: Product) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        Column {
            // Imagen
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(product.imageUrl),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Información
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$ ${String.format("%.2f", product.price)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                AssistChip(
                    onClick = { },
                    label = { Text(product.category, maxLines = 1) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun CompactProductList(products: List<Product>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(products) { product ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Imagen pequeña
                    Card(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(MaterialTheme.shapes.small)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(product.imageUrl),
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Información básica
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$ ${String.format("%.2f", product.price)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = product.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleProductList(products: List<Product>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(products) { product ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = product.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "$ ${String.format("%.2f", product.price)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
@Composable
fun DetailedProductList(products: List<Product>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(products) { product ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 6.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Información del producto
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        AssistChip(
                            onClick = { },
                            label = { Text(product.category) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                        Text(
                            text = "$ ${String.format("%.2f", product.price)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Imagen del producto
                    Card(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(MaterialTheme.shapes.medium)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(product.imageUrl),
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}