package com.example.easyprice

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyprice.data.FavoritesManager
import com.example.easyprice.data.HistoryManager
// Importamos el modelo correcto de forma explícita para evitar ambigüedad
import com.example.easyprice.model.Product
import com.google.firebase.firestore.FirebaseFirestore

class Result : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val barcode = intent.getStringExtra("barcode")

        setContent {
            val context = LocalContext.current
            var product by remember { mutableStateOf<Product?>(null) }
            var productNotFound by remember { mutableStateOf(false) }

            if (barcode != null) {
                LaunchedEffect(barcode) {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("products").whereEqualTo("codigo", barcode).get()
                        .addOnSuccessListener { documents ->
                            if (documents.isEmpty) {
                                productNotFound = true
                            } else {
                                // 1. Usamos el modelo Product antiguo (de com.example.easyprice) para leer desde Firestore
                                val firestoreProduct = documents.documents[0].toObject(com.example.easyprice.Product::class.java)
                                if (firestoreProduct != null) {
                                    // 2. Convertimos el objeto de Firestore al nuevo modelo (de com.example.easyprice.model)
                                    val newProduct = Product(
                                        name = firestoreProduct.name,
                                        price = firestoreProduct.price,
                                        description = firestoreProduct.descripcion,
                                        code = firestoreProduct.codigo
                                    )

                                    // 3. Ahora los tipos coinciden y no habrá error
                                    product = newProduct
                                    HistoryManager.historyList.add(newProduct)
                                } else {
                                    productNotFound = true
                                }
                            }
                        }
                        .addOnFailureListener { productNotFound = true }
                }
            } else {
                productNotFound = true
            }

            if (productNotFound) {
                val intent = Intent(context, NotFound::class.java)
                startActivity(intent)
                finish()
            } else if (product != null) {
                ResultScreen(
                    product = product!!,
                    onBack = {
                        val intent = Intent(context, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun QuantityDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (Product) -> Unit
) {
    var quantity by remember { mutableStateOf(1) }
    val totalPrice = product.price * quantity

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = product.name) },
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { if (quantity > 1) quantity-- }) {
                        Icon(painter = painterResource(id = R.drawable.ic_remove), contentDescription = "Remove")
                    }
                    Text(text = quantity.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { quantity++ }) {
                        Icon(painter = painterResource(id = R.drawable.ic_add), contentDescription = "Add")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Precio total: $ $totalPrice", fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val productWithQuantity = product.copy(quantity = quantity)
                onConfirm(productWithQuantity)
            }) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(text = label, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = value)
    }
}

@Composable
fun ResultScreen(
    product: Product,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showQuantityDialog by remember { mutableStateOf(false) }
    // Estado para saber si el producto está en favoritos
    var isFavorite by remember { 
        mutableStateOf(FavoritesManager.favorites.any { it.name == product.name && it.price == product.price })
    }

    if (showQuantityDialog) {
        QuantityDialog(
            product = product,
            onDismiss = { showQuantityDialog = false },
            onConfirm = {
                FavoritesManager.add(it)
                isFavorite = true
                showQuantityDialog = false
                Toast.makeText(context, "Agregado a favoritos", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(colorResource(id = R.color.activity_background)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(24.dp))

        Image(painter = painterResource(id = R.drawable.logo_easy_price), contentDescription = "Easy Price", modifier = Modifier.size(200.dp))

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.white)),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Text("Resultado de Escaneo", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))

                InfoRow("Nombre:", product.name)
                InfoRow("Precio:", "$ ${product.price}")
                InfoRow("Descripción:", product.description)
                InfoRow("Código:", product.code)
                
                Spacer(modifier = Modifier.height(24.dp))

                // El botón ahora cambia dinámicamente
                Button(
                    onClick = { 
                        if (isFavorite) {
                            FavoritesManager.remove(product)
                            isFavorite = false
                            Toast.makeText(context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
                        } else {
                            showQuantityDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        // Cambiamos el color si es favorito
                        containerColor = if (isFavorite) Color.Red else Color(0xFF2EF2A3)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_star), contentDescription = "Favorito", tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    // Cambiamos el texto si es favorito
                    Text(if (isFavorite) "Quitar de Favoritos" else "Agregar a Favorito", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = {
                        val intent = Intent(context, HistoryActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(painter = painterResource(id = R.drawable.ic_list), contentDescription = "Historial", modifier = Modifier.size(45.dp))
                    }
                    IconButton(onClick = {
                        val intent = Intent(context, FavoritesActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(painter = painterResource(id = R.drawable.ic_star), contentDescription = "Ver Favoritos", modifier = Modifier.size(45.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5EF28B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Volver al Inicio", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
