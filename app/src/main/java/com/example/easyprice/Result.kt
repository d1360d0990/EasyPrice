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
                                // Usamos el modelo Product antiguo para leer desde Firestore
                                val firestoreProduct = documents.documents[0].toObject(com.example.easyprice.Product::class.java)
                                if (firestoreProduct != null) {
                                    // Creamos una instancia del nuevo modelo Product para nuestra UI
                                    val newProduct = Product(
                                        name = firestoreProduct.name,
                                        price = firestoreProduct.price.toIntOrNull() ?: 0
                                    )
                                    product = newProduct
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
                    },
                    onAddToFavorites = {
                        FavoritesManager.add(it)
                        Toast.makeText(context, "Agregado a favoritos", Toast.LENGTH_SHORT).show()
                    },
                    onNavigateToFavorites = {
                        val intent = Intent(context, FavoritesActivity::class.java)
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
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
    onBack: () -> Unit,
    onAddToFavorites: (Product) -> Unit,
    onNavigateToFavorites: () -> Unit
) {
    val context = LocalContext.current // Se obtiene el contexto para la navegación

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
                // Como price es Int, lo convertimos a String para mostrarlo
                InfoRow("Precio:", "$ ${product.price}")
                
                Spacer(modifier = Modifier.height(24.dp))

                // BOTÓN AGREGAR A FAVORITO
                Button(
                    onClick = { onAddToFavorites(product) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EF2A3)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_star), contentDescription = "Favorito", tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Agregar a Favorito", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // NUEVOS BOTONES DE NAVEGACIÓN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { Toast.makeText(context, "Historial no disponible", Toast.LENGTH_SHORT).show() }) {
                        Icon(painter = painterResource(id = R.drawable.ic_list), contentDescription = "Historial", modifier = Modifier.size(45.dp))
                    }
                    IconButton(onClick = onNavigateToFavorites) {
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