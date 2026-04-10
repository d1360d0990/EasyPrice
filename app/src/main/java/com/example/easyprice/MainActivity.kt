package com.example.easyprice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyprice.data.FavoritesManager
import com.example.easyprice.data.HistoryManager
import com.example.easyprice.ui.theme.EasyPriceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EasyPriceTheme {
                var currentScreen by remember { mutableStateOf("role_selection") }

                when (currentScreen) {
                    "role_selection" -> {
                        RoleSelectionScreen(
                            onAdminClick = { currentScreen = "admin_login" },
                            onConsumerClick = { currentScreen = "consumer_home" }
                        )
                    }
                    "admin_login" -> {
                        AdminLoginScreen(
                            onLoginClick = { user, pass ->
                                if (user == "admin" && pass == "admin") {
                                    currentScreen = "admin_home"
                                } else {
                                    Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onBack = { currentScreen = "role_selection" }
                        )
                    }
                    "admin_home" -> {
                        AdminHomeScreen(
                            onLogout = { currentScreen = "role_selection" }
                        )
                    }
                    "consumer_home" -> {
                        MainScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun RoleSelectionScreen(onAdminClick: () -> Unit, onConsumerClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0B46))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_easy_price),
            contentDescription = "Easy Price Logo",
            modifier = Modifier
                .size(280.dp)
                .padding(bottom = 32.dp),
            contentScale = ContentScale.Fit
        )

        Text(
            text = "Iniciar como",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        RoleButton(
            text = "Administrador",
            icon = Icons.Default.Person,
            onClick = onAdminClick
        )

        Spacer(modifier = Modifier.height(20.dp))

        RoleButton(
            text = "Consumidor",
            icon = Icons.Default.ShoppingCart,
            onClick = onConsumerClick
        )
    }
}

@Composable
fun AdminLoginScreen(onLoginClick: (String, String) -> Unit, onBack: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0B46))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_easy_price),
            contentDescription = "Easy Price Logo",
            modifier = Modifier
                .size(250.dp)
                .padding(bottom = 32.dp),
            contentScale = ContentScale.Fit
        )

        TextField(
            value = username,
            onValueChange = { username = it },
            placeholder = { Text("Usuario", color = Color.Gray, fontSize = 18.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            shape = RoundedCornerShape(35.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            trailingIcon = {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(30.dp))
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Contraseña", color = Color.Gray, fontSize = 18.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            shape = RoundedCornerShape(35.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            trailingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(30.dp))
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { onLoginClick(username, password) },
            modifier = Modifier
                .width(200.dp)
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2EF2A3)
            ),
            shape = RoundedCornerShape(30.dp)
        ) {
            Text(
                text = "Login",
                color = Color(0xFF1A0B46),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
            Text("Volver", color = Color.White)
        }
    }
}

@Composable
fun AdminHomeScreen(onLogout: () -> Unit) {
    val context = LocalContext.current

    // Launcher para el escáner del administrador
    val barcodeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val barcode = data?.getStringExtra("barcode_result")
                val intent = Intent(context, Result::class.java).apply {
                    putExtra("barcode", barcode)
                }
                context.startActivity(intent)
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0B46))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_easy_price),
            contentDescription = "Easy Price Logo",
            modifier = Modifier
                .size(280.dp)
                .padding(bottom = 60.dp),
            contentScale = ContentScale.Fit
        )

        // Botón Cargar Producto (Ahora lanza el escáner)
        Button(
            onClick = {
                val intent = Intent(context, ScannerActivity::class.java)
                barcodeLauncher.launch(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFD54F)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_camera),
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Cargar Producto",
                    color = Color(0xFF1A0B46),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Botón Salir
        Button(
            onClick = onLogout,
            modifier = Modifier
                .width(160.dp)
                .height(110.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFD54F)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color(0xFF757575),
                    modifier = Modifier.size(50.dp)
                )
                Text(
                    text = "Salir",
                    color = Color(0xFF1A0B46),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RoleButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        shape = RoundedCornerShape(45.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFD54F)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = Color(0xFF1A0B46),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(35.dp)
            )
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
                val intent = Intent(context, Result::class.java).apply {
                    putExtra("barcode", barcode)
                }
                context.startActivity(intent)
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E2A35))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Image(
            painter = painterResource(id = R.drawable.logo_easy_price),
            contentDescription = "Logo Easy Price",
            modifier = Modifier.size(260.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                val intent = Intent(context, ScannerActivity::class.java)
                barcodeLauncher.launch(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF4C430)
            ),
            shape = RoundedCornerShape(100.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_camera),
                contentDescription = "Escanear",
                tint = Color.Black,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
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
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            BottomNavButton(
                text = "Historial",
                iconRes = R.drawable.ic_list,
                onClick = { 
                    if (HistoryManager.historyList.isNotEmpty()) {
                        val intent = Intent(context, HistoryActivity::class.java)
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "No se ha escaneado ningún producto aún", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            BottomNavButton(
                text = "Favoritos",
                iconRes = R.drawable.ic_star_outline,
                onClick = { 
                    if (FavoritesManager.favorites.isEmpty()) {
                        Toast.makeText(context, "No se marcó ningún producto como favorito", Toast.LENGTH_SHORT).show()
                    } else {
                        val intent = Intent(context, FavoritesActivity::class.java)
                        context.startActivity(intent)
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun BottomNavButton(
    text: String,
    iconRes: Int,
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
        shape = RoundedCornerShape(25.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = text,
                tint = Color.Black,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
