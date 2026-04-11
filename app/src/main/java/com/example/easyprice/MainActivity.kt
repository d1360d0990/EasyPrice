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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Face
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyprice.data.FavoritesManager
import com.example.easyprice.data.HistoryManager
import com.example.easyprice.ui.theme.EasyPriceTheme
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EasyPriceTheme {
                var currentScreen by remember { mutableStateOf("role_selection") }
                var scannedBarcode by remember { mutableStateOf("") }

                val context = LocalContext.current
                val barcodeLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
                    onResult = { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            val barcode = result.data?.getStringExtra("barcode_result")?.trim() ?: ""
                            scannedBarcode = barcode
                            
                            val db = FirebaseFirestore.getInstance()
                            db.collection("products").whereEqualTo("codigo", barcode).get()
                                .addOnSuccessListener { documents ->
                                    if (documents.isEmpty) {
                                        currentScreen = "admin_add_product"
                                    } else {
                                        currentScreen = "admin_product_exists"
                                    }
                                }
                        }
                    }
                )

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
                            onAdminScan = {
                                val intent = Intent(this, ScannerActivity::class.java)
                                barcodeLauncher.launch(intent)
                            },
                            onLogout = { currentScreen = "role_selection" }
                        )
                    }
                    "admin_product_exists" -> {
                        ProductExistsScreen(
                            onViewProduct = {
                                val intent = Intent(this, Result::class.java).apply {
                                    putExtra("barcode", scannedBarcode)
                                }
                                startActivity(intent)
                            },
                            onBackToAdminHome = { currentScreen = "admin_home" }
                        )
                    }
                    "admin_add_product" -> {
                        AddProductScreen(
                            barcode = scannedBarcode,
                            onProductLoaded = { currentScreen = "admin_success" },
                            onCancel = { currentScreen = "admin_home" },
                            onBackToAdminHome = { currentScreen = "admin_home" }
                        )
                    }
                    "admin_success" -> {
                        SuccessScreen(
                            onCargarOtro = {
                                val intent = Intent(this, ScannerActivity::class.java)
                                barcodeLauncher.launch(intent)
                            },
                            onBackToAdminHome = { currentScreen = "admin_home" }
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
fun SuccessScreen(onCargarOtro: () -> Unit, onBackToAdminHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E2A35))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_easy_price),
            contentDescription = "Logo",
            modifier = Modifier.size(200.dp).padding(bottom = 32.dp),
            contentScale = ContentScale.Fit
        )

        Card(
            modifier = Modifier.fillMaxWidth().height(380.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Producto cargado exitosamente",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(40.dp))

                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .border(6.dp, Color(0xFF4CAF50), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(90.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = onCargarOtro,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Cargar otro producto", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onBackToAdminHome,
            modifier = Modifier.fillMaxWidth(0.7f).height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF69F0AE)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Volver al Inicio", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProductExistsScreen(onViewProduct: () -> Unit, onBackToAdminHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E2A35))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_easy_price),
            contentDescription = "Logo",
            modifier = Modifier.size(200.dp).padding(bottom = 24.dp),
            contentScale = ContentScale.Fit
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Producto ya existente",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(24.dp))

                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(120.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "El codigo escaneado ya se encuentra registrado en la base de datos",
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onViewProduct,
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Ver Producto", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onBackToAdminHome,
            modifier = Modifier.fillMaxWidth(0.7f).height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF69F0AE)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Volver al Inicio", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AddProductScreen(
    barcode: String,
    onProductLoaded: () -> Unit,
    onCancel: () -> Unit,
    onBackToAdminHome: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    
    var nombre by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E2A35))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_easy_price),
            contentDescription = "Logo",
            modifier = Modifier.size(180.dp).padding(bottom = 16.dp),
            contentScale = ContentScale.Fit
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Carga de Nuevo Producto",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                FormField("Código", barcode, enabled = false)
                FormField("Nombre", nombre) { nombre = it }
                FormField("Marca", marca) { marca = it }
                FormField("Precio", precio, keyboardType = KeyboardType.Number) { precio = it }
                FormField("Categoría", categoria) { categoria = it }
                FormField("Descripción", descripcion, singleLine = false) { descripcion = it }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (nombre.isBlank() || precio.isBlank()) {
                            Toast.makeText(context, "Nombre y Precio son obligatorios", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        val productData = hashMapOf(
                            "codigo" to barcode.trim(),
                            "name" to nombre,
                            "price" to precio.toDoubleOrNull(),
                            "marca" to marca,
                            "categoria" to categoria,
                            "descripcion" to descripcion
                        )
                        db.collection("products").add(productData)
                            .addOnSuccessListener {
                                onProductLoaded()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error al cargar: ${it.message}", Toast.LENGTH_SHORT).show()
                                isLoading = false
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(28.dp),
                    enabled = !isLoading
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Cargar Producto", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Done, contentDescription = null, tint = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Cancelar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBackToAdminHome,
            modifier = Modifier.fillMaxWidth(0.6f).height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF69F0AE)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Volver al Inicio", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FormField(
    label: String,
    value: String,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$label: ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            )
        )
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
fun AdminHomeScreen(onAdminScan: () -> Unit, onLogout: () -> Unit) {
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

        Button(
            onClick = onAdminScan,
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
