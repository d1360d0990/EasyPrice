package com.example.easyprice.ui.components.pages

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyprice.data.FavoritesManager
import com.example.easyprice.model.Product
import com.example.easyprice.ui.components.atoms.LogoImage

@Composable
fun PantallaScanConsumer(
    isWide: Boolean = false,
    product: Product,
    onHistoryClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A2A3A)) // Fondo oscuro
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Logo EASY PRICE
        LogoImage(size = if (isWide) 180.dp else 320.dp)

        Spacer(modifier = Modifier.height(10.dp))

        // Tarjeta Blanca
        Card(
            modifier = Modifier.fillMaxWidth(if (isWide) 0.7f else 0.95f),
            shape = RoundedCornerShape(50.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Título con recuadro
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .border(0.5.dp, Color.LightGray)
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Resultado de Escaneo",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textDecoration = TextDecoration.Underline,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Campos de Datos: Nombre, Código, Precio
                ConsumerDataField("Nombre", product.name)
                ConsumerDataField("Código", product.code)
                ConsumerDataField("Precio", "$ ${product.price.toInt()}")

                Spacer(modifier = Modifier.height(20.dp))

                // Descripción
                Text(
                    text = "Descripción:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    textDecoration = TextDecoration.Underline,
                    color = Color.Black
                )
                Text(
                    text = product.description,
                    fontSize = 18.sp,
                    color = Color.Black,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 20.dp)
                )

                // Botón Agregar a Favorito (Verde Menta)
                Button(
                    onClick = {
                        if (!FavoritesManager.favorites.contains(product)) {
                            FavoritesManager.favorites.add(product)
                            Toast.makeText(context, "Agregado a favoritos", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EF2A3)),
                    shape = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.buttonElevation(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Agregar a Favorito",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            Icons.Default.BookmarkAdded,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botones Nav Amarillos (Ovalados)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onHistoryClick,
                        modifier = Modifier
                            .width(130.dp)
                            .height(55.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Default.List, contentDescription = null, tint = Color.Black, modifier = Modifier.size(35.dp))
                    }

                    Button(
                        onClick = onFavoritesClick,
                        modifier = Modifier
                            .width(130.dp)
                            .height(55.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black, modifier = Modifier.size(35.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón Volver al Inicio (Verde)
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(65.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF59E689)),
            shape = RoundedCornerShape(25.dp)
        ) {
            Text(
                "Volver al Inicio",
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun ConsumerDataField(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.Black
        )
        Box(
            modifier = Modifier
                .border(0.5.dp, Color.LightGray)
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .background(Color.White)
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                color = Color.Black
            )
        }
    }
}
