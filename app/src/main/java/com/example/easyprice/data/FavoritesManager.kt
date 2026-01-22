package com.example.easyprice.data

import androidx.compose.runtime.mutableStateListOf
import com.example.easyprice.model.Product

object FavoritesManager {

    val favorites = mutableStateListOf<Product>()

    fun add(product: Product) {
        // Buscamos el producto por sus propiedades, no por el objeto en sí
        val existingProduct = favorites.find { it.name == product.name && it.price == product.price }
        if (existingProduct != null) {
            val index = favorites.indexOf(existingProduct)
            if (index != -1) {
                val newProduct = existingProduct.copy(quantity = existingProduct.quantity + product.quantity)
                favorites[index] = newProduct
            }
        } else {
            favorites.add(product)
        }
    }

    fun remove(product: Product) {
        // Buscamos el producto a eliminar por sus propiedades para asegurar que se encuentre
        val productToRemove = favorites.find { it.name == product.name && it.price == product.price }
        if (productToRemove != null) {
            favorites.remove(productToRemove)
        }
    }

    fun increaseQuantity(product: Product) {
        // Buscamos el producto por sus propiedades para encontrar el índice
        val index = favorites.indexOfFirst { it.name == product.name && it.price == product.price }
        if (index != -1) {
            val currentProduct = favorites[index]
            val updatedProduct = currentProduct.copy(quantity = currentProduct.quantity + 1)
            favorites[index] = updatedProduct
        }
    }

    fun decreaseQuantity(product: Product) {
        // Buscamos el producto por sus propiedades para encontrar el índice
        val index = favorites.indexOfFirst { it.name == product.name && it.price == product.price }
        if (index != -1) {
            val currentProduct = favorites[index]
            // Solo disminuimos si la cantidad es mayor que 1
            if (currentProduct.quantity > 1) {
                val updatedProduct = currentProduct.copy(quantity = currentProduct.quantity - 1)
                favorites[index] = updatedProduct
            }
        }
    }

    fun total(): Int {
        return favorites.sumOf { it.price * it.quantity }
    }
}
