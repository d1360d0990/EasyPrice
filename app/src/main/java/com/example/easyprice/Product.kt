package com.example.easyprice

data class Product(
    val name: String = "",
    val price: Int = 0,
    val descripcion: String = "", // Nombre del campo en Firestore
    val codigo: String = "" // Nombre del campo en Firestore
)
