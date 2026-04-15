package com.example.easyprice.model

// Modelo de producto actualizado
data class Product(
    val name: String,
    val price: Double,
    val quantity: Int = 1,
    val description: String = "",
    val code: String = "",
    val marca: String? = "",
    val categoria: String? = "",
    val subcategoria: String? = "",
    val scanCount: Int = 0 // Nuevo campo para analítica
)
