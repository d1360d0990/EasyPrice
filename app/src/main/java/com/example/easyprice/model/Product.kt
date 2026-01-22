package com.example.easyprice.model

// Se elimina el override de equals() y hashCode() para que Compose detecte los cambios de cantidad.
data class Product(
    val name: String,
    val price: Int,
    val quantity: Int = 1
)
