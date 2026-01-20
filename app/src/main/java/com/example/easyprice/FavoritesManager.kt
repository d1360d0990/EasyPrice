package com.example.easyprice

import com.example.easyprice.model.Product // 1. IMPORTAR LA NUEVA CLASE

object FavoritesManager {

    private val favoritesList = mutableListOf<Product>()

    fun addFavorite(product: Product) {
        if (!favoritesList.contains(product)) {
            favoritesList.add(product)
        }
    }

    fun removeFavorite(product: Product) {
        favoritesList.remove(product)
    }

    fun getFavorites(): List<Product> {
        return favoritesList.toList()
    }

    // 2. SIMPLIFICAR LA LÓGICA DE SUMA
    fun getFavoritesTotal(): Int {
        // Como price ahora es un Int, la suma es directa. No se necesitan conversiones.
        return favoritesList.sumOf { it.price }
    }
    
    fun isFavorite(product: Product): Boolean {
        return favoritesList.contains(product)
    }
}