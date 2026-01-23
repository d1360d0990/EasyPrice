package com.example.easyprice.data

import androidx.compose.runtime.mutableStateListOf
import com.example.easyprice.model.Product

object HistoryManager {
    // Usamos mutableStateListOf para que la UI reaccione a los cambios
    val historyList = mutableStateListOf<Product>()
}
