package com.example.apptienda

import androidx.annotation.Keep

@Keep
data class Producto(
    val id: String = "",
    var idNumerico: String = "",
    val nombre: String = "",
    val precio: Double = 0.0,
    val descripcion: String = "",
    val imageUrl: String = "",
    val categorias: List<String> = emptyList(), // IDs de las categorías
    val idOrdenNumerico: Long = 0,
    val camposAdicionales: Map<String, String> = emptyMap() // Campos dinámicos
)