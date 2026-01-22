package com.example.easyprice.ui.screens

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
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyprice.R
import com.example.easyprice.data.FavoritesManager
import com.example.easyprice.model.Product

@Composable
fun FavoritesScreen(onBack: () -> Unit) {
    val favorites = FavoritesManager.favorites
    val total = FavoritesManager.total()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E2A35))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Image(
            painter = painterResource(id = R.drawable.logo_easy_price),
            contentDescription = "Logo",
            modifier = Modifier.size(180.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2EF2A3)
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_star),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Favoritos", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (favorites.isEmpty()) {
                    Text(
                        text = "No se marcaron productos como favoritos aún, marca al menos un producto.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    favorites.forEach { product ->
                        FavoriteItem(product)
                        Divider()
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("$${total}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5EF28B)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Volver al Inicio",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FavoriteItem(product: Product) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(product.name, fontWeight = FontWeight.Bold)
            Text("$${product.price}")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { 
                FavoritesManager.decreaseQuantity(product)
            }) {
                Icon(painter = painterResource(id = R.drawable.ic_remove), contentDescription = "Remove")
            }
            // Usamos directamente la cantidad del producto, sin estado local
            Text(product.quantity.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            IconButton(onClick = { 
                FavoritesManager.increaseQuantity(product)
            }) {
                Icon(painter = painterResource(id = R.drawable.ic_add), contentDescription = "Add")
            }
            IconButton(onClick = { FavoritesManager.remove(product) }) {
                Icon(painter = painterResource(id = R.drawable.ic_delete), contentDescription = "Eliminar")
            }
        }
    }
}
