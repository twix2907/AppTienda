package com.example.apptienda

import androidx.annotation.Keep

@Keep
data class Producto(
    val id: String = "",
    var idNumerico: Long = 0,
    val nombre: String = "",
    val precio: Double = 0.0,
    val descripcion: String = "",
    val imageUrl: String = "",
    val categorias: List<String> = emptyList() // IDs de las categorías
)