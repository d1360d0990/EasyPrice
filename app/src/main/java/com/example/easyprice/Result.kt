package com.example.easyprice

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

class Result : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val barcode = intent.getStringExtra("barcode")

        setContent {
            var product by remember { mutableStateOf<Product?>(null) }
            var productNotFound by remember { mutableStateOf(false) }

            if (barcode != null) {
                LaunchedEffect(barcode) {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("products")
                        .whereEqualTo("codigo", barcode)
                        .get()
                        .addOnSuccessListener { documents ->
                            if (documents.isEmpty) {
                                productNotFound = true
                            } else {
                                val foundProduct = documents.documents[0].toObject(Product::class.java)
                                if (foundProduct != null) {
                                    product = foundProduct
                                }
                            }
                        }
                        .addOnFailureListener {
                            productNotFound = true
                        }
                }
            } else {
                productNotFound = true
            }

            if (productNotFound) {
                val intent = Intent(this, NotFound::class.java)
                startActivity(intent)
                finish()
            } else if (product != null) {
                var isFavorite by remember { mutableStateOf(product!!.isFavorite) }

                ResultScreen(
                    product = product!!,
                    isFavorite = isFavorite,
                    onBack = {
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    },
                    onFavoriteClick = { p ->
                        p.isFavorite = !p.isFavorite
                        isFavorite = p.isFavorite
                        val message = if (isFavorite) "Agregado a favoritos" else "Eliminado de favoritos"
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    },
                    onHistoryClick = {
                        Toast.makeText(this, "Historial no disponible", Toast.LENGTH_SHORT).show()
                    },
                    onFavoritesNavClick = {
                        Toast.makeText(this, "Favoritos no disponible", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = value)
    }
}

@Composable
fun ResultScreen(
    product: Product,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onFavoriteClick: (Product) -> Unit,
    onHistoryClick: () -> Unit,
    onFavoritesNavClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.activity_background))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(24.dp))

        Image(
            painter = painterResource(id = R.drawable.logo_easy_price),
            contentDescription = "Easy Price",
            modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.white)),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Text(
                    text = "Resultado de Escaneo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                InfoRow("Nombre:", product.name)
                InfoRow("Código:", product.code)
                InfoRow("Precio:", "$ ${product.price}")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Descripción:",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = product.description,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))

                // BOTÓN FAVORITO PRINCIPAL
                Button(
                    onClick = { onFavoriteClick(product) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.favorite_button_color)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        painter = painterResource(id = if (isFavorite) R.drawable.ic_star_filled else R.drawable.ic_favorite),
                        contentDescription = "Favorito",
                        tint = colorResource(id = R.color.black)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFavorite) "Quitar de Favoritos" else "Agregar a Favorito",
                        color = colorResource(id = R.color.black),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // NUEVOS BOTONES DE NAVEGACIÓN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Botón de Historial
                    Button(
                        onClick = onHistoryClick,
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4C430)),
                        modifier = Modifier.size(60.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_list),
                            contentDescription = "Historial",
                            tint = Color.Black,
                            modifier = Modifier.size(45.dp) // Aumentamos el tamaño del icono a 3/4 (45dp)
                        )
                    }
                    // Botón de Favoritos
                    Button(
                        onClick = onFavoritesNavClick,
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4C430)),
                        modifier = Modifier.size(60.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_star_outline),
                            contentDescription = "Favoritos",
                            tint = Color.Black,
                            modifier = Modifier.size(45.dp) // Aumentamos el tamaño del icono a 3/4 (45dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // BOTÓN VOLVER
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.home_button_color)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Volver al Inicio",
                color = colorResource(id = R.color.black),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}