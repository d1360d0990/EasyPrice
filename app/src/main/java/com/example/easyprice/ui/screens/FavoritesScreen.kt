package com.example.easyprice.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyprice.R
import com.example.easyprice.data.FavoritesManager
import com.example.easyprice.model.Product

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun FavoritesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val windowSizeClass = (context as? androidx.activity.ComponentActivity)?.let { calculateWindowSizeClass(it) }
    val isWide = windowSizeClass?.widthSizeClass != WindowWidthSizeClass.Compact
    
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
            modifier = Modifier.size(if (isWide) 120.dp else 180.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(if (isWide) 0.8f else 1f).weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2EF2A3)
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_star),
                        contentDescription = null,
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Favoritos", fontWeight = FontWeight.Bold, color = Color.Black)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (favorites.isEmpty()) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No se marcaron productos como favoritos aún, marca al menos un producto.",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    if (isWide) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(favorites) { product ->
                                FavoriteItemCard(product)
                            }
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            favorites.forEach { product ->
                                FavoriteItem(product)
                                HorizontalDivider()
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("$${total}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF2E7D32))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth(if (isWide) 0.4f else 1f)
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5EF28B)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Volver atrás",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FavoriteItemCard(product: Product) {
    Card(
        modifier = Modifier.padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.9f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        FavoriteItem(product)
    }
}

@Composable
fun FavoriteItem(product: Product) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(product.name, fontWeight = FontWeight.Bold)
            Text("$${product.price}", color = Color(0xFF2E7D32))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { 
                FavoritesManager.decreaseQuantity(product)
            }) {
                Icon(painter = painterResource(id = R.drawable.ic_remove), contentDescription = "Remove")
            }
            Text(product.quantity.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            IconButton(onClick = { 
                FavoritesManager.increaseQuantity(product)
            }) {
                Icon(painter = painterResource(id = R.drawable.ic_add), contentDescription = "Add")
            }
            IconButton(onClick = { FavoritesManager.remove(product) }) {
                Icon(painter = painterResource(id = R.drawable.ic_delete), contentDescription = "Eliminar", tint = Color.Red)
            }
        }
    }
}