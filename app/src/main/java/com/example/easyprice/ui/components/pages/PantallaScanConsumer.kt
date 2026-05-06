package com.example.easyprice.ui.components.pages

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
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
            .background(Color(0xFF1E2A35))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        LogoImage(size = if (isWide) 120.dp else 220.dp)

        Card(
            modifier = Modifier.fillMaxWidth(if (isWide) 0.7f else 1f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Resultado de Escaneo",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )

                if (isWide) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(Modifier.weight(1f)) {
                            ConsumerDataField("Nombre", product.name)
                            ConsumerDataField("Código", product.code)
                            ConsumerDataField("Precio", "$ ${product.price.toInt()}")
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Descripción:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                textDecoration = TextDecoration.Underline,
                                color = Color.Black
                            )
                            Text(
                                text = product.description,
                                fontSize = 16.sp,
                                color = Color.Black,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    ConsumerDataField("Nombre", product.name)
                    ConsumerDataField("Código", product.code)
                    ConsumerDataField("Precio", "$ ${product.price.toInt()}")

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Descripción:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textDecoration = TextDecoration.Underline,
                        color = Color.Black
                    )
                    Text(
                        text = product.description,
                        fontSize = 16.sp,
                        color = Color.Black,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (!FavoritesManager.favorites.contains(product)) {
                            FavoritesManager.favorites.add(product)
                            Toast.makeText(context, "Agregado a favoritos", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Ya está en favoritos", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EF2A3)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Agregar a Favorito", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onHistoryClick,
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Icon(Icons.Default.List, contentDescription = null, tint = Color.Black, modifier = Modifier.size(30.dp))
                    }

                    Button(
                        onClick = onFavoritesClick,
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black, modifier = Modifier.size(30.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(if (isWide) 0.4f else 0.8f).height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF59E689)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Volver al Inicio", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ConsumerDataField(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$label: ", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
        Box(
            modifier = Modifier
                .border(1.dp, Color.LightGray)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(value, fontSize = 18.sp, color = Color.Black)
        }
    }
}
