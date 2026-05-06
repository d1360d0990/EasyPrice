package com.example.easyprice

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
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
import com.example.easyprice.ui.theme.EasyPriceTheme

class HistoryActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isWide = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
            EasyPriceTheme {
                HistoryScreen(isWide)
            }
        }
    }
}

@Composable
fun HistoryScreen(isWide: Boolean = false) {
    val context = LocalContext.current

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
            modifier = Modifier.size(if (isWide) 120.dp else 160.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth(if (isWide) 0.7f else 1f)
                .background(Color(0xFF2C3E50), shape = RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { /* TODO: Handle previous */ }) {
                Icon(painter = painterResource(id = R.drawable.ic_chevron_left), contentDescription = "Previous", tint = Color.White)
            }
            Text(text = "Historial de Búsqueda", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { /* TODO: Handle next */ }) {
                Icon(painter = painterResource(id = R.drawable.ic_chevron_right), contentDescription = "Next", tint = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(if (isWide) 0.8f else 1f).weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            if (isWide) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.padding(16.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(HistoryManager.historyList) {
                        HistoryItem(product = it)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(HistoryManager.historyList) {
                        HistoryItem(product = it)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { 
                val intent = Intent(context, MainActivity::class.java)
                context.startActivity(intent)
             },
            modifier = Modifier
                .fillMaxWidth(if (isWide) 0.4f else 1f)
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
    val context = LocalContext.current
    var isFavorite by remember { mutableStateOf(FavoritesManager.favorites.contains(product)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { 
                val intent = Intent(context, Result::class.java).apply {
                    putExtra("barcode", product.code)
                }
                context.startActivity(intent)
            },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.9f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = product.name, fontSize = 16.sp, modifier = Modifier.weight(1f))
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
                    tint = if (isFavorite) Color(0xFFF4C430) else Color.Gray
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 800)
@Composable
fun HistoryScreenWidePreview() {
    HistoryScreen(isWide = true)
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    HistoryScreen(isWide = false)
}
