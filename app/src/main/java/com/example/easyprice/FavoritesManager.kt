package com.example.easyprice

// Usamos un 'object' para crear un Singleton: una única instancia de este gestor para toda la app.
object FavoritesManager {

    // La lista de favoritos vive aquí, en memoria.
    private val favoritesList = mutableListOf<Product>()

    // Añade un producto a la lista si no está ya presente.
    fun addFavorite(product: Product) {
        if (!favoritesList.contains(product)) {
            favoritesList.add(product)
        }
    }

    // Elimina un producto de la lista.
    fun removeFavorite(product: Product) {
        favoritesList.remove(product)
    }

    // Devuelve una copia de la lista de favoritos para evitar modificaciones accidentales.
    fun getFavorites(): List<Product> {
        return favoritesList.toList()
    }

    // Calcula y devuelve la suma total de los precios de los productos favoritos.
    fun getFavoritesTotal(): Double {
        // SOLUCIÓN FINAL: Se convierte el String de precio a Double antes de sumar.
        return favoritesList.sumOf { it.price.toDoubleOrNull() ?: 0.0 }
    }
    
    // Comprueba si un producto ya es favorito.
    fun isFavorite(product: Product): Boolean {
        return favoritesList.contains(product)
    }
}