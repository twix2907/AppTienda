package com.example.apptienda

data class Producto(
    val id: String = "",
    val nombre: String = "",
    val precio: Double = 0.0,
    val descripcion: String = "",
    val imageUrl: String = "",
    val categorias: List<String> = emptyList() // IDs de las categorías
)