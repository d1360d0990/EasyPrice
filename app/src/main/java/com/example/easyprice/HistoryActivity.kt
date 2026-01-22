package com.example.easyprice

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyprice.data.FavoritesManager
import com.example.easyprice.data.HistoryManager
import com.example.easyprice.model.Product

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HistoryScreen()
        }
    }
}

@Composable
fun HistoryScreen() {
    val context = LocalContext.current

    // Dummy data for preview
    if (HistoryManager.historyList.isEmpty()) {
        HistoryManager.historyList.add(Product("Auriculares Bluetooth", 2500))
        HistoryManager.historyList.add(Product("Café \"La morenita\" x 50", 850))
        HistoryManager.historyList.add(Product("Tallarines \"Marolio\"", 350))
        HistoryManager.historyList.add(Product("Café \"La morenita\" x 50", 850))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E2A35))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_easy_price),
            contentDescription = "Logo Easy Price",
            modifier = Modifier.size(160.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2C3E50), shape = RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { /* TODO: Handle previous */ }) {
                Icon(painter = painterResource(id = R.drawable.ic_chevron_left), contentDescription = "Previous", tint = Color.White)
            }
            Text(text = "Historial de Busqueda", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { /* TODO: Handle next */ }) {
                Icon(painter = painterResource(id = R.drawable.ic_chevron_right), contentDescription = "Next", tint = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(HistoryManager.historyList) {
                    HistoryItem(product = it)
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { 
                val intent = Intent(context, MainActivity::class.java)
                context.startActivity(intent)
             },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Volver al Inicio", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HistoryItem(product: Product) {
    var isFavorite by remember { mutableStateOf(FavoritesManager.favorites.contains(product)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = product.name, fontSize = 16.sp)
        IconButton(onClick = { 
            if (isFavorite) {
                FavoritesManager.remove(product)
            } else {
                FavoritesManager.add(product)
            }
            isFavorite = !isFavorite
        }) {
            Icon(
                painter = painterResource(id = if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_outline),
                contentDescription = "Favorite",
                tint = if (isFavorite) Color.Yellow else Color.Gray
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    HistoryScreen()
}
