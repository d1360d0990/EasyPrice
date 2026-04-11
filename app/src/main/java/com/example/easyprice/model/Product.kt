package com.example.easyprice.model

// Modelo de producto actualizado con todos los campos necesarios
data class Product(
    val name: String,
    val price: Double,
    val quantity: Int = 1,
    val description: String = "",
    val code: String = "",
    val marca: String? = "",
    val categoria: String? = ""
)
