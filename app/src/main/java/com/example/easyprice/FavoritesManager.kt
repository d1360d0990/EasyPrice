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

    // Se actualiza a Double para coincidir con el modelo de Producto
    fun getFavoritesTotal(): Double {
        return favoritesList.sumOf { it.price }
    }
    
    fun isFavorite(product: Product): Boolean {
        return favoritesList.contains(product)
    }
}