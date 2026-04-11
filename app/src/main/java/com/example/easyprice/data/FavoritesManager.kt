package com.example.easyprice.data

import androidx.compose.runtime.mutableStateListOf
import com.example.easyprice.model.Product

object FavoritesManager {

    val favorites = mutableStateListOf<Product>()

    fun add(product: Product) {
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
        val productToRemove = favorites.find { it.name == product.name && it.price == product.price }
        if (productToRemove != null) {
            favorites.remove(productToRemove)
        }
    }

    fun increaseQuantity(product: Product) {
        val index = favorites.indexOfFirst { it.name == product.name && it.price == product.price }
        if (index != -1) {
            val currentProduct = favorites[index]
            val updatedProduct = currentProduct.copy(quantity = currentProduct.quantity + 1)
            favorites[index] = updatedProduct
        }
    }

    fun decreaseQuantity(product: Product) {
        val index = favorites.indexOfFirst { it.name == product.name && it.price == product.price }
        if (index != -1) {
            val currentProduct = favorites[index]
            if (currentProduct.quantity > 1) {
                val updatedProduct = currentProduct.copy(quantity = currentProduct.quantity - 1)
                favorites[index] = updatedProduct
            }
        }
    }

    // Se actualiza a Double para permitir decimales en el total
    fun total(): Double {
        return favorites.sumOf { it.price * it.quantity }
    }
}
