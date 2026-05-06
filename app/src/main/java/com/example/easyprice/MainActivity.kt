package com.example.easyprice

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.easyprice.model.Product
import com.example.easyprice.ui.theme.EasyPriceTheme
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.io.File
import java.io.FileOutputStream

// --- ViewModels ---

class DashboardViewModel : ViewModel() {
    var totalScans by mutableIntStateOf(0)
    var activeUsers by mutableIntStateOf(0)
    var totalProducts by mutableIntStateOf(0)
    var isLoading by mutableStateOf(false)
    var topProducts by mutableStateOf<List<String>>(emptyList())

    fun loadStats() {
        isLoading = true
        val db = FirebaseFirestore.getInstance()
        db.collection("stats").document("global").get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                totalScans = doc.getLong("total_scans")?.toInt() ?: 0
                activeUsers = doc.getLong("active_users")?.toInt() ?: 0
            }
            db.collection("products").count().get(AggregateSource.SERVER).addOnSuccessListener { snapshot ->
                totalProducts = snapshot.count.toInt()
                db.collection("product_stats").orderBy("scan_count", Query.Direction.DESCENDING).limit(5).get().addOnSuccessListener { topSnapshot ->
                    topProducts = topSnapshot.documents.map { it.getString("name") ?: "Sin nombre" }
                    isLoading = false
                }.addOnFailureListener { isLoading = false }
            }.addOnFailureListener { isLoading = false }
        }.addOnFailureListener { isLoading = false }
    }
}

class ProductosViewModel : ViewModel() {
    var productos by mutableStateOf<List<Product>>(emptyList())
    var isLoading by mutableStateOf(false)
    fun loadProductos() {
        isLoading = true
        FirebaseFirestore.getInstance().collection("products").get().addOnSuccessListener { result ->
            productos = result.mapNotNull { doc ->
                try {
                    Product(
                        name = doc.getString("name") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        code = doc.getString("codigo") ?: "",
                        marca = doc.getString("marca") ?: "",
                        categoria = doc.getString("categoria") ?: "",
                        subcategoria = doc.getString("subcategoria") ?: "",
                        description = doc.getString("descripcion") ?: ""
                    )
                } catch (e: Exception) { null }
            }
            isLoading = false
        }.addOnFailureListener { isLoading = false }
    }
}

class EscaneosViewModel : ViewModel() {
    var topProductos by mutableStateOf<List<Pair<String, Int>>>(emptyList())
    var weeklyData by mutableStateOf<List<Int>>(emptyList())
    var isLoading by mutableStateOf(false)
    fun loadData() {
        isLoading = true
        FirebaseFirestore.getInstance().collection("product_stats").orderBy("scan_count", Query.Direction.DESCENDING).limit(5).get().addOnSuccessListener { result ->
            topProductos = result.map { Pair(it.getString("name") ?: "", it.getLong("scan_count")?.toInt() ?: 0) }
            isLoading = false
        }.addOnFailureListener { isLoading = false }
        weeklyData = listOf(10, 25, 40, 30, 50, 60, 45)
    }
}

class TendenciasViewModel : ViewModel() {
    var weeklyData by mutableStateOf<List<Int>>(emptyList())
    var growthProducts by mutableStateOf<List<Triple<String, Int, Int>>>(emptyList())
    var isLoading by mutableStateOf(false)
    fun loadData() {
        isLoading = true
        FirebaseFirestore.getInstance().collection("scans_by_day").orderBy("__name__", Query.Direction.ASCENDING).limitToLast(7).get().addOnSuccessListener { result ->
            weeklyData = result.map { it.getLong("count")?.toInt() ?: 0 }
            isLoading = false
        }.addOnFailureListener { isLoading = false }
        FirebaseFirestore.getInstance().collection("product_stats").get().addOnSuccessListener { result ->
            growthProducts = result.map {
                val name = it.getString("name") ?: ""
                val current = it.getLong("scan_count")?.toInt() ?: 0
                val last = it.getLong("last_week_count")?.toInt() ?: 0
                Triple(name, current, last)
            }.sortedByDescending { it.second - it.third }.take(5)
        }
    }
}

class ReportesViewModel : ViewModel() {
    var isGenerating by mutableStateOf(false)
    var reportsList = mutableStateListOf<String>()
    data class ReportProduct(val name: String, val count: Int)
    fun generarNuevoReporte(context: Context) {
        isGenerating = true
        val db = FirebaseFirestore.getInstance()
        db.collection("stats").document("global").get().addOnSuccessListener { statsDoc ->
            val totalScans = statsDoc.getLong("total_scans") ?: 0
            val activeUsers = statsDoc.getLong("active_users") ?: 0
            db.collection("products").count().get(AggregateSource.SERVER).addOnSuccessListener { productSnapshot ->
                val totalProductsCount = productSnapshot.count
                db.collection("product_stats").orderBy("scan_count", Query.Direction.DESCENDING).limit(5).get().addOnSuccessListener { topSnap ->
                    val topProducts = topSnap.documents.map { ReportProduct(name = it.getString("name") ?: "Sin nombre", count = it.getLong("scan_count")?.toInt() ?: 0) }
                    db.collection("scans_by_day").orderBy("__name__", Query.Direction.ASCENDING).limitToLast(7).get().addOnSuccessListener { scansSnap ->
                        val weeklyScans = scansSnap.documents.map { it.getLong("count")?.toInt() ?: 0 }
                        val chartBitmap = createChartBitmap(context, weeklyScans)
                        crearPdfReal(context, totalScans, activeUsers, totalProductsCount, topProducts, chartBitmap)
                    }.addOnFailureListener { isGenerating = false }
                }.addOnFailureListener { isGenerating = false }
            }.addOnFailureListener { isGenerating = false }
        }.addOnFailureListener { isGenerating = false }
    }
    private fun createChartBitmap(context: Context, data: List<Int>): Bitmap {
        val chart = LineChart(context)
        chart.layout(0, 0, 600, 300)
        val entries = data.mapIndexed { index, value -> Entry(index.toFloat(), value.toFloat()) }
        val dataSet = LineDataSet(entries, "Escaneos").apply { 
            color = android.graphics.Color.parseColor("#2EF2A3")
            setDrawValues(false) 
        }
        chart.data = LineData(dataSet)
        val bitmap = Bitmap.createBitmap(600, 300, Bitmap.Config.ARGB_8888)
        chart.draw(Canvas(bitmap))
        return bitmap
    }
    private fun crearPdfReal(context: Context, scans: Long, users: Long, products: Long, topProductos: List<ReportProduct>, chartBitmap: Bitmap) {
        val pdfDocument = PdfDocument()
        val page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas: Canvas = page.canvas
        val paint = Paint()
        paint.textSize = 14f
        canvas.drawText("Total escaneos: $scans", 50f, 100f, paint)
        canvas.drawText("Usuarios activos: $users", 50f, 130f, paint)
        canvas.drawText("Total productos: $products", 50f, 160f, paint)
        var y = 200f
        topProductos.forEach { canvas.drawText("${it.name}: ${it.count}", 50f, y, paint); y += 30f }
        canvas.drawBitmap(chartBitmap, 50f, y + 20f, paint)
        pdfDocument.finishPage(page)
        val file = File(context.getExternalFilesDir(null), "reporte_${System.currentTimeMillis()}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            reportsList.add(0, file.name)
            Toast.makeText(context, "Reporte generado", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { Log.e("PDF", e.message ?: "") }
        finally { pdfDocument.close(); isGenerating = false }
    }
    fun openReport(context: Context, fileName: String) {
        val file = File(context.getExternalFilesDir(null), fileName)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        }
    }
    fun shareReport(context: Context, fileName: String) {
        val file = File(context.getExternalFilesDir(null), fileName)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir"))
        }
    }
}

class AsistenteIAViewModel : ViewModel() {
    var messages = mutableStateListOf<Pair<String, Boolean>>()
    var isTyping by mutableStateOf(false)
    init { messages.add("¡Hola! Soy tu asistente EasyPrice. ¿En qué puedo ayudarte?" to false) }
    fun sendMessage(query: String) {
        messages.add(query to true)
        isTyping = true
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            messages.add("Entiendo tu consulta sobre '$query'. ¿Deseas ver estadísticas específicas?" to false)
            isTyping = false
        }, 1500)
    }
}

// --- MainActivity ---

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val targetScreen = intent.getStringExtra("target_screen") ?: "role_selection"
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isWide = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

            EasyPriceTheme {
                var currentScreen by rememberSaveable { mutableStateOf(targetScreen) }
                var scannedBarcode by rememberSaveable { mutableStateOf("") }
                val context = LocalContext.current
                val barcodeLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
                    onResult = { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                            val barcode = result.data?.getStringExtra("barcode_result")?.trim() ?: ""
                            scannedBarcode = barcode
                            FirebaseFirestore.getInstance().collection("products").whereEqualTo("codigo", barcode).get()
                                .addOnSuccessListener { documents ->
                                    currentScreen = if (documents.isEmpty) "admin_add_product" else "admin_product_exists"
                                }
                        }
                    }
                )

                when (currentScreen) {
                    "role_selection" -> RoleSelectionScreen(
                        isWide = isWide,
                        onAdminClick = { currentScreen = "admin_login" },
                        onConsumerClick = { trackUserActivity(); currentScreen = "consumer_home" }
                    )
                    "admin_login" -> AdminLoginScreen(
                        isWide = isWide,
                        onLoginClick = { user, pass ->
                            val u = user.trim()
                            val p = pass.trim()
                            if (u == "admin" && p == "admin") currentScreen = "admin_home"
                            else if (u == "gerencia" && p == "gerencia") currentScreen = "management_home"
                            else Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                        },
                        onBack = { currentScreen = "role_selection" }
                    )
                    "consumer_home" -> MainScreen(
                        isWide = isWide,
                        onFavoritesClick = { context.startActivity(Intent(context, FavoritesActivity::class.java)) },
                        onHistoryClick = { context.startActivity(Intent(context, HistoryActivity::class.java)) },
                        onLogout = { currentScreen = "role_selection" }
                    )
                    "admin_home" -> AdminHomeScreen(
                        isWide = isWide,
                        onAdminScan = { barcodeLauncher.launch(Intent(context, ScannerActivity::class.java)) },
                        onDatabaseClick = { currentScreen = "admin_database" },
                        onLogout = { currentScreen = "role_selection" }
                    )
                    "management_home" -> ManagementHomeScreen(
                        isWide = isWide,
                        onLogout = { currentScreen = "role_selection" },
                        onDashboardClick = { currentScreen = "dashboard" },
                        onProductsClick = { currentScreen = "management_products" },
                        onScansClick = { currentScreen = "management_scans" },
                        onTrendsClick = { currentScreen = "management_trends" },
                        onAiClick = { currentScreen = "ai_assistant" },
                        onReportsClick = { currentScreen = "reports" }
                    )
                    "dashboard" -> DashboardScreen(isWide = isWide, onBack = { currentScreen = "management_home" })
                    "management_products" -> ProductosScreen(isWide = isWide, onBack = { currentScreen = "management_home" }, onProductClick = { bc ->
                        val intent = Intent(context, Result::class.java).apply { putExtra("barcode", bc) }
                        context.startActivity(intent)
                    })
                    "management_scans" -> EscaneosScreen(onBack = { currentScreen = "management_home" })
                    "management_trends" -> TendenciasScreen(onBack = { currentScreen = "management_home" })
                    "ai_assistant" -> AiAssistantScreen(onBack = { currentScreen = "management_home" })
                    "reports" -> ReportesScreen(onBack = { currentScreen = "management_home" })
                    "admin_database" -> DatabaseScreen(onProductClick = { bc ->
                        val intent = Intent(context, Result::class.java).apply { putExtra("barcode", bc); putExtra("edit_mode", true) }
                        context.startActivity(intent)
                    }, onBackToAdminHome = { currentScreen = "admin_home" })
                    "admin_product_exists" -> ProductExistsScreen(
                        onViewProduct = { context.startActivity(Intent(context, Result::class.java).apply { putExtra("barcode", scannedBarcode) }) },
                        onBackToAdminHome = { currentScreen = "admin_home" }
                    )
                    "admin_add_product" -> AddProductScreen(
                        barcode = scannedBarcode,
                        onProductLoaded = { currentScreen = "admin_success" },
                        onError = { currentScreen = "admin_error" },
                        onCancel = { currentScreen = "admin_home" },
                        onBackToAdminHome = { currentScreen = "admin_home" }
                    )
                    "admin_success" -> SuccessScreen(onCargarOtro = { barcodeLauncher.launch(Intent(context, ScannerActivity::class.java)) }, onBackToAdminHome = { currentScreen = "admin_home" })
                    "admin_error" -> ErrorScreen(onRetry = { currentScreen = "admin_add_product" }, onBackToAdminHome = { currentScreen = "admin_home" })
                }
            }
        }
    }

    private fun trackUserActivity() {
        FirebaseFirestore.getInstance().collection("stats").document("global").set(mapOf("active_users" to FieldValue.increment(1)), SetOptions.merge())
    }
}

// --- Composables ---

@Composable
fun MainScreen(isWide: Boolean, onFavoritesClick: () -> Unit, onHistoryClick: () -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val barcodeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val intent = Intent(context, Result::class.java).apply { putExtra("barcode", res.data?.getStringExtra("barcode_result")); putExtra("is_consumer", true) }
            context.startActivity(intent)
        }
    }
    
    val bgColor = Color(0xFF1A0B46)
    val buttonColor = Color(0xFFFFD54F) 

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Logo
        Image(
            painter = painterResource(R.drawable.logo_easy_price),
            contentDescription = null,
            modifier = Modifier.size(if (isWide) 320.dp else 250.dp).padding(top = 16.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(if (isWide) 0.7f else 1f)
        ) {
            // Main Scan Button
            Button(
                onClick = { barcodeLauncher.launch(Intent(context, ScannerActivity::class.java)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.width(20.dp))
                    Text(
                        "Escanear Código",
                        color = Color.Black,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // History and Favorites Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ConsumerSquareButton(
                    text = "Historial",
                    icon = Icons.Default.FormatListBulleted,
                    onClick = onHistoryClick,
                    modifier = Modifier.weight(1f)
                )
                ConsumerSquareButton(
                    text = "Favoritos",
                    icon = Icons.Default.StarBorder,
                    onClick = onFavoritesClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Exit Button at the bottom
        Button(
            onClick = onLogout,
            modifier = Modifier
                .width(180.dp)
                .height(90.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(40.dp)
                )
                Text("Salir", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun ConsumerSquareButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Color.Black, modifier = Modifier.size(45.dp))
            Spacer(Modifier.height(8.dp))
            Text(text, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RoleSelectionScreen(isWide: Boolean, onAdminClick: () -> Unit, onConsumerClick: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFF1A0B46)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painterResource(R.drawable.logo_easy_price), null, Modifier.size(if (isWide) 250.dp else 220.dp).padding(bottom = 32.dp))
        if (isWide) {
            Row(Modifier.fillMaxWidth(0.8f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1f)) { RoleButton("Administrador", Icons.Default.Person, onAdminClick) }
                Box(Modifier.weight(1f)) { RoleButton("Consumidor", Icons.Default.ShoppingCart, onConsumerClick) }
            }
        } else {
            RoleButton("Administrador", Icons.Default.Person, onAdminClick)
            Spacer(Modifier.height(16.dp))
            RoleButton("Consumidor", Icons.Default.ShoppingCart, onConsumerClick)
        }
    }
}

@Composable
fun RoleButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Button(onClick = onClick, Modifier.fillMaxWidth().height(70.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Color.Black); Spacer(Modifier.width(8.dp)); Text(text, color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun AdminLoginScreen(isWide: Boolean, onLoginClick: (String, String) -> Unit, onBack: () -> Unit) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val content = @Composable {
        Column(Modifier.fillMaxWidth(if (isWide) 0.5f else 1f), horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text("Usuario", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Usuario", tint = Color.White) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF2EF2A3),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                ),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Contraseña", color = Color.White) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Contraseña", tint = Color.White) },
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    val description = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = icon, contentDescription = description, tint = Color.White)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF2EF2A3),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )
            Spacer(Modifier.height(32.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Button(onClick = { onLoginClick(user, pass) }, modifier = Modifier.weight(1f).height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EF2A3))) { Text("Login", color = Color.Black, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(12.dp))
                Button(onClick = onBack, modifier = Modifier.weight(1f).height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))) { Text("Salir", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
    }

    if (isWide) {
        Row(Modifier.fillMaxSize().background(Color(0xFF1A0B46)).padding(32.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Image(painterResource(R.drawable.logo_easy_price), null, Modifier.size(200.dp).padding(end = 48.dp))
            content()
        }
    } else {
        Column(Modifier.fillMaxSize().background(Color(0xFF1A0B46)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(painterResource(R.drawable.logo_easy_price), null, Modifier.size(150.dp).padding(bottom = 32.dp))
            content()
        }
    }
}

@Composable
fun AdminHomeScreen(isWide: Boolean, onAdminScan: () -> Unit, onDatabaseClick: () -> Unit, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFF1A0B46)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painterResource(R.drawable.logo_easy_price), null, Modifier.size(180.dp))
        Spacer(Modifier.height(32.dp))
        if (isWide) {
            Row(Modifier.fillMaxWidth(0.8f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onAdminScan, Modifier.weight(1.5f).height(100.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))) { Text("Cargar Producto", color = Color.Black, fontSize = 20.sp) }
                AdminMenuButton("Base Datos", Icons.AutoMirrored.Filled.List, onDatabaseClick, Modifier.weight(1f))
                AdminMenuButton("Salir", Icons.AutoMirrored.Filled.ArrowBack, onLogout, Modifier.weight(1f))
            }
        } else {
            Button(onAdminScan, Modifier.fillMaxWidth().height(80.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))) { Text("Cargar Producto", color = Color.Black, fontSize = 20.sp) }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AdminMenuButton("Base Datos", Icons.AutoMirrored.Filled.List, onDatabaseClick, Modifier.weight(1f))
                AdminMenuButton("Salir", Icons.AutoMirrored.Filled.ArrowBack, onLogout, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun AdminMenuButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    Button(onClick = onClick, modifier = modifier.height(100.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, tint = Color.Black); Text(text, color = Color.Black, fontSize = 12.sp) }
    }
}

@Composable
fun ManagementHomeScreen(isWide: Boolean, onLogout: () -> Unit, onDashboardClick: () -> Unit, onProductsClick: () -> Unit, onScansClick: () -> Unit, onTrendsClick: () -> Unit, onAiClick: () -> Unit, onReportsClick: () -> Unit) {
    val bgColor = Color(0xFF1A0B46)
    val exitButtonColor = Color(0xFFFFD54F)
    
    Column(
        Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo
        Image(
            painter = painterResource(R.drawable.logo_easy_price),
            contentDescription = null,
            modifier = Modifier.size(if (isWide) 160.dp else 140.dp).padding(top = 8.dp)
        )
        
        Spacer(Modifier.height(12.dp))
        
        val buttons = listOf(
            Triple("Dashboard", Icons.Filled.Dashboard, onDashboardClick),
            Triple("Productos", Icons.Filled.Inventory2, onProductsClick),
            Triple("Escaneos", Icons.Filled.QrCodeScanner, onScansClick),
            Triple("Tendencias", Icons.Filled.TrendingUp, onTrendsClick),
            Triple("Reportes", Icons.Filled.BarChart, onReportsClick),
            Triple("Asistente IA", Icons.AutoMirrored.Filled.Chat, onAiClick)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isWide) 3 else 2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(buttons) { (text, icon, onClick) ->
                ManagementButton(text, icon, onClick)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Exit Button
        Button(
            onClick = onLogout,
            modifier = Modifier
                .width(180.dp)
                .height(110.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = exitButtonColor)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(48.dp)
                )
                Text("Salir", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun ManagementButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(140.dp),
        shape = RoundedCornerShape(32.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EF2A3))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Color(0xFF555555), modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(4.dp))
            Text(text, color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DashboardScreen(isWide: Boolean, viewModel: DashboardViewModel = viewModel(), onBack: () -> Unit) {
    LaunchedEffect(Unit) { viewModel.loadStats() }
    Column(Modifier.fillMaxSize().background(Color(0xFF1A0B46)).padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }; Text("📊 Dashboard", color = Color.White, style = MaterialTheme.typography.headlineMedium) }
        
        if (isWide) {
            Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("Escaneos", viewModel.totalScans.toString(), Modifier.weight(1f))
                StatCard("Usuarios", viewModel.activeUsers.toString(), Modifier.weight(1f))
                StatCard("Productos", viewModel.totalProducts.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1f)) { SimpleChart() }
                Card(Modifier.weight(1f).height(150.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f))) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Top Productos", color = Color.White, fontWeight = FontWeight.Bold)
                        viewModel.topProducts.take(3).forEach { Text("• $it", color = Color.White.copy(0.7f)) }
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Escaneos", viewModel.totalScans.toString(), Modifier.weight(1f))
                StatCard("Usuarios", viewModel.activeUsers.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(24.dp)); SimpleChart()
        }
        
        Button(onBack, Modifier.fillMaxWidth().padding(top = 32.dp).height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))) { Text("Volver", color = Color.Black) }
    }
}

@Composable
fun StatCard(t: String, v: String, modifier: Modifier) {
    Card(modifier.height(100.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2EF2A3))) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Text(t, color = Color.Black); Text(v, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
    }
}

@Composable
fun SimpleChart() {
    Card(Modifier.fillMaxWidth().height(150.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f))) {
        Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            listOf(3, 6, 8, 5, 9, 7, 4).forEach { v -> Box(Modifier.width(20.dp).height((v * 10).dp).background(Color(0xFFFFD54F))) }
        }
    }
}

@Composable
fun ProductosScreen(isWide: Boolean, viewModel: ProductosViewModel = viewModel(), onBack: () -> Unit, onProductClick: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.loadProductos() }
    Column(Modifier.fillMaxSize().background(Color(0xFF1A0B46)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }; Text("📦 Productos", color = Color.White, style = MaterialTheme.typography.headlineMedium) }
        TextField(query, { query = it }, label = { Text("Buscar...") }, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
        
        val filteredList = viewModel.productos.filter { it.name.contains(query, true) }
        
        if (isWide) {
            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredList) { p ->
                    ProductCard(p, onProductClick)
                }
            }
        } else {
            LazyColumn {
                items(filteredList) { p ->
                    ProductCard(p, onProductClick)
                }
            }
        }
    }
}

@Composable
fun ProductCard(p: Product, onClick: (String) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick(p.code) }) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(p.name, Modifier.weight(1f))
            Text("$" + p.price.toString(), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EscaneosScreen(v: EscaneosViewModel = viewModel(), onBack: () -> Unit) {
    LaunchedEffect(Unit) { v.loadData() }
    Column(Modifier.fillMaxSize().background(Color(0xFF1A0B46)).padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }; Text("🔍 Escaneos", color = Color.White, style = MaterialTheme.typography.headlineMedium) }
        Spacer(Modifier.height(24.dp)); BarChart(v.weeklyData)
        v.topProductos.forEachIndexed { i, p -> TopItem(i + 1, p.first, p.second) }
        Button(onBack, Modifier.fillMaxWidth().padding(top = 32.dp).height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))) { Text("Volver", color = Color.Black) }
    }
}

@Composable
fun BarChart(d: List<Int>) {
    Card(Modifier.fillMaxWidth().height(150.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f))) {
        Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            d.forEach { valH -> Box(Modifier.width(24.dp).height((valH * 2).dp).background(Color(0xFF2EF2A3))) }
        }
    }
}

@Composable
fun TopItem(r: Int, n: String, c: Int) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2EF2A3))) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("#$r $n", color = Color.Black); Text("$c scans", color = Color(0xFF2EF2A3)) }
    }
}

@Composable
fun TendenciasScreen(v: TendenciasViewModel = viewModel(), onBack: () -> Unit) {
    LaunchedEffect(Unit) { v.loadData() }
    Column(Modifier.fillMaxSize().background(Color(0xFF1A0B46)).padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }; Text("📈 Tendencias", color = Color.White, style = MaterialTheme.typography.headlineMedium) }
        Spacer(Modifier.height(24.dp)); RealLineChart(v.weeklyData)
        v.growthProducts.forEach { gp -> GrowthItem(gp.first, gp.second, gp.third) }
        Button(onBack, Modifier.fillMaxWidth().padding(top = 32.dp).height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))) { Text("Volver", color = Color.Black) }
    }
}

@Composable
fun RealLineChart(chartData: List<Int>) {
    AndroidView(factory = { ctx -> LineChart(ctx).apply {
        description.isEnabled = false; legend.isEnabled = false
        val entries = chartData.mapIndexed { i, v -> Entry(i.toFloat(), v.toFloat()) }
        this.data = LineData(LineDataSet(entries, "S").apply { color = android.graphics.Color.parseColor("#2EF2A3"); lineWidth = 3f })
        invalidate()
    }}, modifier = Modifier.fillMaxWidth().height(200.dp))
}

@Composable
fun GrowthItem(n: String, c: Int, l: Int) {
    val g = c - l
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f))) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(n, color = Color.White); Text(if (g >= 0) "▲ $g" else "▼ $g", color = if (g >= 0) Color.Green else Color.Red) }
    }
}

@Composable
fun AiAssistantScreen(v: AsistenteIAViewModel = viewModel(), onBack: () -> Unit) {
    var queryInput by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Color(0xFF1A0B46)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }; Text("💬 IA Asistente", color = Color.White, style = MaterialTheme.typography.headlineMedium) }
        LazyColumn(Modifier.weight(1f).padding(vertical = 16.dp)) { items(v.messages) { m -> ChatBubble(m.first, m.second) } }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextField(queryInput, { queryInput = it }, Modifier.weight(1f), shape = RoundedCornerShape(24.dp), placeholder = { Text("Consulta...") })
            FloatingActionButton(onClick = { if (queryInput.isNotBlank()) { v.sendMessage(queryInput); queryInput = "" } }, containerColor = Color(0xFF2EF2A3), modifier = Modifier.padding(start = 8.dp)) { Icon(Icons.AutoMirrored.Filled.Send, null) }
        }
    }
}

@Composable
fun ChatBubble(t: String, u: Boolean) {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = if (u) Alignment.CenterEnd else Alignment.CenterStart) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (u) Color(0xFFFFD54F) else Color.White.copy(0.1f))) { Text(t, Modifier.padding(12.dp), color = if (u) Color.Black else Color.White) }
    }
}

@Composable
fun ReportesScreen(v: ReportesViewModel = viewModel(), onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(Color(0xFF1A0B46)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }; Text("📄 Reportes", color = Color.White, style = MaterialTheme.typography.headlineMedium) }
        Button(onClick = { v.generarNuevoReporte(context) }, modifier = Modifier.fillMaxWidth().padding(top = 24.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EF2A3))) {
            if (v.isGenerating) CircularProgressIndicator(Modifier.size(24.dp), color = Color.Black) else Text("Generar Nuevo PDF", color = Color.Black)
        }
        LazyColumn(Modifier.weight(1f).padding(top = 16.dp)) { items(v.reportsList) { r ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { v.openReport(context, r) }, colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f))) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text(r, Modifier.weight(1f), color = Color.White); IconButton(onClick = { v.shareReport(context, r) }) { Icon(Icons.Default.Share, null, tint = Color(0xFF2EF2A3)) } }
            }
        }}
    }
}

@Composable
fun DatabaseScreen(onProductClick: (String) -> Unit, onBackToAdminHome: () -> Unit) {
    var list by remember { mutableStateOf<List<Product>>(emptyList()) }
    LaunchedEffect(Unit) { FirebaseFirestore.getInstance().collection("products").get().addOnSuccessListener { d -> list = d.mapNotNull { it.toObject(Product::class.java) } } }
    Column(Modifier.fillMaxSize().background(Color(0xFF1E2A35)).padding(16.dp)) {
        Text("Base de Datos", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        LazyColumn(Modifier.weight(1f).padding(top = 16.dp)) { items(list) { p ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onProductClick(p.code) }) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(p.name); Icon(Icons.Default.Edit, null) }
            }
        }}
        Button(onClick = onBackToAdminHome, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF59E689))) { Text("Volver") }
    }
}

@Composable
fun ProductExistsScreen(onViewProduct: () -> Unit, onBackToAdminHome: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFF1A0B46)).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Face, null, Modifier.size(100.dp), tint = Color.White); Text("Producto existente", color = Color.White)
        Button(onClick = onViewProduct, modifier = Modifier.padding(top = 24.dp).fillMaxWidth()) { Text("Ver Detalles") }
        TextButton(onClick = onBackToAdminHome) { Text("Volver", color = Color.White) }
    }
}

@Composable
fun SuccessScreen(onCargarOtro: () -> Unit, onBackToAdminHome: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFF1A0B46)).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Check, null, Modifier.size(100.dp), tint = Color.Green); Text("¡Éxito!", color = Color.White, fontSize = 24.sp)
        Button(onClick = onCargarOtro, modifier = Modifier.padding(top = 32.dp).fillMaxWidth()) { Text("Cargar Otro") }
        TextButton(onClick = onBackToAdminHome) { Text("Inicio", color = Color.White) }
    }
}

@Composable
fun ErrorScreen(onRetry: () -> Unit, onBackToAdminHome: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFF1A0B46)).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Close, null, Modifier.size(100.dp), tint = Color.Red); Text("Error al cargar", color = Color.White)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 32.dp).fillMaxWidth()) { Text("Reintentar") }
        TextButton(onClick = onBackToAdminHome) { Text("Inicio", color = Color.White) }
    }
}

@Composable
fun AddProductScreen(barcode: String, onProductLoaded: () -> Unit, onError: () -> Unit, onCancel: () -> Unit, onBackToAdminHome: () -> Unit) {
    var n by remember { mutableStateOf("") }; var p by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Color(0xFF1A0B46)).padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Cargar Producto: $barcode", color = Color.White, fontSize = 20.sp)
        TextField(n, { n = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
        TextField(p, { p = it }, label = { Text("Precio") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Button(onClick = { 
            val productMap = mapOf("codigo" to barcode, "name" to n, "price" to (p.toDoubleOrNull() ?: 0.0))
            FirebaseFirestore.getInstance().collection("products").add(productMap).addOnSuccessListener { onProductLoaded() }.addOnFailureListener { onError() } 
        }, modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(55.dp)) { Text("Guardar") }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancelar", color = Color.Red) }
        TextButton(onClick = onBackToAdminHome, modifier = Modifier.fillMaxWidth()) { Text("Volver", color = Color.White) }
    }
}
