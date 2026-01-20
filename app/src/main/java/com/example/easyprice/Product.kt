package com.example.easyprice

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Product(
    @DocumentId
    var id: String = "",
    @PropertyName("nombre")
    var name: String = "",
    @PropertyName("precio")
    var price: String = "",
    @PropertyName("descripción")
    var description: String = "",
    @PropertyName("codigo")
    var code: String = "",
    var isFavorite: Boolean = false,
    @ServerTimestamp
    var timestamp: Date? = null
)
