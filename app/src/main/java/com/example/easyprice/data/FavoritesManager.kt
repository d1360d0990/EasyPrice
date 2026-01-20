package com.example.easyprice.data

import androidx.compose.runtime.mutableStateListOf
import com.example.easyprice.model.Product

object FavoritesManager {

    val favorites = mutableStateListOf<Product>()

    fun add(product: Product) {
        if (!favorites.contains(product)) {
            favorites.add(product)
        }
    }

    fun remove(product: Product) {
        favorites.remove(product)
    }

    fun total(): Int {
        return favorites.sumOf { it.price }
    }
}