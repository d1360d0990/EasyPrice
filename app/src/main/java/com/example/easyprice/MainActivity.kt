package com.example.easyprice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyprice.ui.theme.EasyPriceTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EasyPriceTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {

    val context = LocalContext.current

    val barcodeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val barcode = data?.getStringExtra("barcode_result")
                Toast.makeText(context, "Código escaneado: $barcode", Toast.LENGTH_LONG).show()
                // Aquí puedes usar el valor de 'barcode'
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2B0A57))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(32.dp))

        Image(
            painter = painterResource(id = R.drawable.logo_easy_price),
            contentDescription = "Logo Easy Price",
            modifier = Modifier.size(260.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val intent = Intent(context, ScannerActivity::class.java)
                barcodeLauncher.launch(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF4C430)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_camera),
                contentDescription = "Escanear",
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Escanear Código",
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            BottomButton(
                text = "Inicio",
                icon = R.drawable.ic_home,
                onClick = { }
            )

            BottomButton(
                text = "Historial",
                icon = R.drawable.ic_list,
                onClick = { }
            )
        }
    }
}

@Composable
fun BottomButton(
    text: String,
    icon: Int,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,
        modifier = Modifier
            .width(160.dp)
            .height(80.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFF4C430)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Icon(
                painter = painterResource(id = icon),
                contentDescription = text,
                tint = Color.Black
            )

            Text(
                text = text,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


