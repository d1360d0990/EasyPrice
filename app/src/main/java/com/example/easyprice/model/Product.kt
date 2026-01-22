package com.example.easyprice.model

// Se actualiza el modelo para incluir descripción y código
data class Product(
    val name: String,
    val price: Int,
    val quantity: Int = 1,
    val description: String = "",
    val code: String = ""
)
