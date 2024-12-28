package com.example.apptienda

import androidx.annotation.Keep

@Keep
data class Categoria(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = ""
)