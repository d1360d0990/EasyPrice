package com.example.easyprice

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyprice.ui.theme.EasyPriceTheme

class NotFound : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isWide = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
            
            EasyPriceTheme {
                NotFoundScreen(
                    isWide = isWide,
                    onRescan = { 
                        val intent = Intent(this, ScannerActivity::class.java)
                        startActivity(intent)
                        finish() 
                    }
                )
            }
        }
    }
}

@Composable
fun NotFoundScreen(
    isWide: Boolean = false,
    onRescan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E2A35))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_easy_price),
            contentDescription = "Easy Price",
            modifier = Modifier.size(if (isWide) 120.dp else 180.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(if (isWide) 0.6f else 1f),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(if (isWide) 32.dp else 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Código no Encontrado",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(20.dp))

                Image(
                    painter = painterResource(id = R.drawable.ic_sad),
                    contentDescription = "No encontrado",
                    modifier = Modifier.size(if (isWide) 80.dp else 120.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "El código escaneado no está registrado en nuestra base de datos",
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRescan,
            modifier = Modifier
                .fillMaxWidth(if (isWide) 0.4f else 0.8f)
                .height(55.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF4C430)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Volver a Escanear",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}