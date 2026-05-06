package com.example.easyprice.data

import com.example.easyprice.model.Product
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

class ProductRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getProductByBarcode(
        barcode: String,
        onSuccess: (Product?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("products").whereEqualTo("codigo", barcode).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    onSuccess(null)
                } else {
                    val doc = documents.documents[0]
                    val product = Product(
                        name = doc.getString("name") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        description = doc.getString("descripcion") ?: "",
                        code = doc.getString("codigo") ?: "",
                        marca = doc.getString("marca") ?: "",
                        categoria = doc.getString("categoria") ?: "",
                        subcategoria = doc.getString("subcategoria") ?: ""
                    )
                    
                    // Actualizar estadísticas al obtener el producto (simulando el comportamiento anterior)
                    updateProductStats(doc.id, product.name)
                    incrementGlobalScans()
                    recordDailyScan()
                    
                    onSuccess(product)
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateProduct(
        product: Product,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("products").whereEqualTo("codigo", product.code).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val docId = documents.documents[0].id
                    val updateData = hashMapOf(
                        "name" to product.name,
                        "price" to product.price,
                        "marca" to product.marca,
                        "categoria" to product.categoria,
                        "subcategoria" to product.subcategoria,
                        "descripcion" to product.description
                    )
                    db.collection("products").document(docId).update(updateData as Map<String, Any>)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onFailure(it) }
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteProduct(
        barcode: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("products").whereEqualTo("codigo", barcode).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val docId = documents.documents[0].id
                    db.collection("products").document(docId).delete()
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onFailure(it) }
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    private fun incrementGlobalScans() {
        db.collection("stats").document("global")
            .set(mapOf("total_scans" to FieldValue.increment(1)), SetOptions.merge())
    }

    private fun updateProductStats(productId: String, productName: String) {
        db.collection("product_stats").document(productId).get()
            .addOnSuccessListener { statDoc ->
                val statsData = mutableMapOf<String, Any>(
                    "name" to productName,
                    "scan_count" to FieldValue.increment(1)
                )
                if (!statDoc.exists()) {
                    statsData["last_week_count"] = 0
                }
                db.collection("product_stats").document(productId).set(statsData, SetOptions.merge())
            }
    }

    private fun recordDailyScan() {
        val dateId = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        db.collection("scans_by_day").document(dateId)
            .set(mapOf("count" to FieldValue.increment(1)), SetOptions.merge())
    }
}
