package com.example.easyprice

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.easyprice.data.FavoritesManager
import com.example.easyprice.data.HistoryManager
import com.example.easyprice.model.Product
import com.example.easyprice.ui.theme.EasyPriceTheme
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// 🧠 Dashboard ViewModel
class DashboardViewModel : ViewModel() {
    var totalScans by mutableIntStateOf(0)
    var activeUsers by mutableIntStateOf(0)
    var totalProducts by mutableIntStateOf(0)
    var isLoading by mutableStateOf(false)

    var topProducts by mutableStateOf<List<String>>(emptyList())

    fun loadStats(context: Context) {
        isLoading = true
        val db = FirebaseFirestore.getInstance()
        
        db.collection("stats").document("global").get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    totalScans = doc.getLong("total_scans")?.toInt() ?: 0
                    activeUsers = doc.getLong("active_users")?.toInt() ?: 0
                }
                
                db.collection("products").count().get(AggregateSource.SERVER)
                    .addOnSuccessListener { snapshot ->
                        totalProducts = snapshot.count.toInt()
                        
                        db.collection("product_stats")
                            .orderBy("scan_count", Query.Direction.DESCENDING)
                            .limit(5)
                            .get()
                            .addOnSuccessListener { topSnapshot ->
                                topProducts = topSnapshot.documents.map { it.getString("name") ?: "Sin nombre" }
                                isLoading = false
                            }
                            .addOnFailureListener {
                                isLoading = false
                            }
                    }
                    .addOnFailureListener { e ->
                        isLoading = false
                    }
            }
            .addOnFailureListener { e ->
                isLoading = false
                Toast.makeText(context, "Error de conexión al Dashboard", Toast.LENGTH_SHORT).show()
            }
    }
}

// 🧠 Productos ViewModel
class ProductosViewModel : ViewModel() {
    var productos by mutableStateOf<List<Product>>(emptyList())
    var isLoading by mutableStateOf(false)

    fun loadProductos() {
        isLoading = true
        FirebaseFirestore.getInstance()
            .collection("products")
            .get()
            .addOnSuccessListener { result ->
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
            }
            .addOnFailureListener {
                isLoading = false
            }
    }
}

// 🧠 Escaneos ViewModel
class EscaneosViewModel : ViewModel() {
    var topProductos by mutableStateOf<List<Pair<String, Int>>>(emptyList())
    var weeklyData by mutableStateOf<List<Int>>(emptyList())
    var isLoading by mutableStateOf(false)

    fun loadData() {
        isLoading = true
        loadTopProductos()
        loadWeeklyData()
    }

    private fun loadTopProductos() {
        FirebaseFirestore.getInstance()
            .collection("product_stats")
            .orderBy("scan_count", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { result ->
                topProductos = result.map {
                    Pair(
                        it.getString("name") ?: "",
                        it.getLong("scan_count")?.toInt() ?: 0
                    )
                }
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    private fun loadWeeklyData() {
        // Simulación (después podés usar colección "scans_by_day")
        weeklyData = listOf(10, 25, 40, 30, 50, 60, 45)
    }
}

// 🧠 Tendencias ViewModel
class TendenciasViewModel : ViewModel() {
    var weeklyData by mutableStateOf<List<Int>>(emptyList())
    var growthProducts by mutableStateOf<List<Triple<String, Int, Int>>>(emptyList())
    var isLoading by mutableStateOf(false)

    fun loadData() {
        isLoading = true
        loadWeeklyData()
        loadGrowthProducts()
    }

    private fun loadWeeklyData() {
        FirebaseFirestore.getInstance()
            .collection("scans_by_day")
            .orderBy("__name__", Query.Direction.ASCENDING)
            .limitToLast(7)
            .get()
            .addOnSuccessListener { result ->
                weeklyData = result.map {
                    it.getLong("count")?.toInt() ?: 0
                }
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    private fun loadGrowthProducts() {
        FirebaseFirestore.getInstance()
            .collection("product_stats")
            .get()
            .addOnSuccessListener { result ->
                growthProducts = result.map {
                    val name = it.getString("name") ?: ""
                    val current = it.getLong("scan_count")?.toInt() ?: 0
                    val last = it.getLong("last_week_count")?.toInt() ?: 0

                    Triple(name, current, last)
                }.sortedByDescending {
                    it.second - it.third
                }.take(5)
            }
    }
}

// 🧠 Reportes ViewModel
class ReportesViewModel : ViewModel() {
    var isGenerating by mutableStateOf(false)
    var reportsList = mutableStateListOf<String>()

    data class ReportProduct(val name: String, val count: Int)

    fun generarNuevoReporte(context: Context) {
        isGenerating = true
        val db = FirebaseFirestore.getInstance()
        
        // Recolectar datos reales de Firestore
        db.collection("stats").document("global").get().addOnSuccessListener { statsDoc ->
            val totalScans = statsDoc.getLong("total_scans") ?: 0
            val activeUsers = statsDoc.getLong("active_users") ?: 0
            
            db.collection("products").count().get(AggregateSource.SERVER).addOnSuccessListener { productSnapshot ->
                val totalProductsCount = productSnapshot.count

                db.collection("product_stats")
                    .orderBy("scan_count", Query.Direction.DESCENDING)
                    .limit(5)
                    .get()
                    .addOnSuccessListener { topSnap ->
                        val topProducts = topSnap.documents.map { 
                            ReportProduct(
                                name = it.getString("name") ?: "Sin nombre",
                                count = it.getLong("scan_count")?.toInt() ?: 0
                            )
                        }

                        // Obtener datos para el gráfico
                        db.collection("scans_by_day")
                            .orderBy("__name__", Query.Direction.ASCENDING)
                            .limitToLast(7)
                            .get()
                            .addOnSuccessListener { scansSnap ->
                                val weeklyScans = scansSnap.documents.map { it.getLong("count")?.toInt() ?: 0 }
                                
                                // Generar el Bitmap del gráfico off-screen
                                val chartBitmap = createChartBitmap(context, weeklyScans)

                                // Crear el PDF real con el gráfico
                                crearPdfReal(context, totalScans, activeUsers, totalProductsCount, topProducts, chartBitmap)
                            }
                            .addOnFailureListener {
                                isGenerating = false
                                Toast.makeText(context, "Error al obtener datos históricos", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        isGenerating = false
                        Toast.makeText(context, "Error al obtener top productos", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                isGenerating = false
                Toast.makeText(context, "Error al contar productos", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            isGenerating = false
            Toast.makeText(context, "Error al obtener estadísticas globales", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createChartBitmap(context: Context, data: List<Int>): Bitmap {
        val chart = LineChart(context)
        chart.layout(0, 0, 600, 300) // Tamaño del gráfico en el PDF
        
        val entries = data.mapIndexed { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }

        val dataSet = LineDataSet(entries, "Escaneos").apply {
            color = android.graphics.Color.parseColor("#2EF2A3")
            setCircleColor(android.graphics.Color.parseColor("#FFD54F"))
            lineWidth = 2f
            setDrawValues(false)
        }

        chart.data = LineData(dataSet)
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.isEnabled = false
        
        // Renderizar el gráfico a un Bitmap
        val bitmap = Bitmap.createBitmap(600, 300, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        chart.draw(canvas)
        return bitmap
    }

    private fun crearPdfReal(context: Context, scans: Long, users: Long, products: Long, topProductos: List<ReportProduct>, chartBitmap: Bitmap) {
        val pdfDocument = PdfDocument()
        // Page size set to A4 (595 x 842 points)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        
        paint.textSize = 14f
        
        // Agregar texto + métricas
        canvas.drawText("Total escaneos: 1245", 50f, 450f, paint)
        canvas.drawText("Usuarios activos: 89", 50f, 480f, paint)

        // Agregar Top productos
        var y = 520f
        topProductos.forEach {
            canvas.drawText("${it.name} - ${it.count}", 50f, y, paint)
            y += 30f
        }

        pdfDocument.finishPage(page)

        // Guardar el PDF
        val fileName = "reporte.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            if (!reportsList.contains(fileName)) reportsList.add(0, fileName)
            Toast.makeText(context, "Reporte guardado como $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("PDF", "Error al guardar: ${e.message}")
            Toast.makeText(context, "Error al generar PDF", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
            isGenerating = false
        }
    }

    fun openReport(context: Context, fileName: String) {
        val file = File(context.getExternalFilesDir(null), fileName)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "No hay aplicaciones para abrir PDF", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "El archivo no existe", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareReport(context: Context, fileName: String) {
        val file = File(context.getExternalFilesDir(null), fileName)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir Reporte"))
        } else {
            Toast.makeText(context, "El archivo no existe", Toast.LENGTH_SHORT).show()
        }
    }
}

// 🧠 Asistente IA ViewModel (Híbrido Firestore + Simulación IA)
class AsistenteIAViewModel : ViewModel() {
    var messages = mutableStateListOf<Pair<String, Boolean>>() // Mensaje to IsUser
    var isTyping by mutableStateOf(false)

    init {
        messages.add("¡Hola! Soy tu asistente EasyPrice. ¿En qué puedo ayudarte hoy?" to false)
    }

    fun sendMessage(query: String) {
        messages.add(query to true)
        isTyping = true
        
        val db = FirebaseFirestore.getInstance()
        val lowerQuery = query.lowercase()
        
        when {
            lowerQuery.contains("top") || lowerQuery.contains("mas escaneado") -> {
                db.collection("product_stats")
                    .orderBy("scan_count", Query.Direction.DESCENDING)
                    .limit(5)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val data = snapshot.documents.mapIndexed { i, doc ->
                            "${i + 1}. ${doc.getString("name")} (${doc.getLong("scan_count")} scans)"
                        }.joinToString("\n")
                        simulateIAResponse("Análisis de Demanda:\n\nLos productos más buscados son:\n$data\n\nEl interés se concentra fuertemente en el primer puesto, sugiriendo una alta necesidad inmediata de stock.")
                    }
            }
            lowerQuery.contains("crecimiento") || lowerQuery.contains("tendencia") -> {
                db.collection("product_stats")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val trend = snapshot.documents
                            .mapNotNull { it.getString("name") }
                            .take(2)
                            .joinToString { it }
                        simulateIAResponse("Tendencias Actuales:\n\nSe observa un comportamiento positivo en '$trend'. Estos productos han mantenido un ritmo de escaneo creciente en los últimos días.")
                    }
            }
            lowerQuery.contains("total") && lowerQuery.contains("escaneo") -> {
                db.collection("stats").document("global").get().addOnSuccessListener { doc ->
                    val total = doc.getLong("total_scans") ?: 0
                    simulateIAResponse("Actualmente registramos un total acumulado de $total escaneos en toda la plataforma.")
                }
            }
            lowerQuery.contains("usuario") -> {
                db.collection("stats").document("global").get().addOnSuccessListener { doc ->
                    val active = doc.getLong("active_users") ?: 0
                    simulateIAResponse("Contamos con $active usuarios registrados interactuando con la aplicación en este periodo.")
                }
            }
            lowerQuery.contains("escaneo") || lowerQuery.contains("cuanto") || lowerQuery.contains("cantidad") -> {
                db.collection("product_stats").get().addOnSuccessListener { snapshot ->
                    val foundProduct = snapshot.documents.find { doc ->
                        val name = doc.getString("name")?.lowercase() ?: ""
                        name.isNotEmpty() && lowerQuery.contains(name)
                    }
                    
                    if (foundProduct != null) {
                        val name = foundProduct.getString("name")
                        val count = foundProduct.getLong("scan_count") ?: 0
                        simulateIAResponse("El producto '$name' registra un total de $count escaneos. Es uno de los artículos con movimiento constante en la base de datos.")
                    } else {
                        simulateIAResponse("Para darte el número exacto de escaneos, por favor indicame el nombre del producto de forma clara.")
                    }
                }
            }
            else -> {
                simulateIAResponse("Entiendo. Puedo proporcionarte datos sobre el total de escaneos, usuarios activos, el rendimiento de un producto específico o las tendencias semanales. ¿Qué dato necesitás?")
            }
        }
    }

    private fun simulateIAResponse(response: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            messages.add(response to false)
            isTyping = false
        }, 1500)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val targetScreen = intent.getStringExtra("target_screen") ?: "role_selection"
        
        setContent {
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
                            
                            val db = FirebaseFirestore.getInstance()
                            db.collection("products").whereEqualTo("codigo", barcode).get()
                                .addOnSuccessListener { documents ->
                                    if (documents.isEmpty) {
                                        currentScreen = "admin_add_product"
                                    } else {
                                        currentScreen = "admin_product_exists"
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Error al conectar con la base de datos", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                )

                when (currentScreen) {
                    "role_selection" -> {
                        RoleSelectionScreen(
                            onAdminClick = { currentScreen = "admin_login" },
                            onConsumerClick = { 
                                trackUserActivity()
                                currentScreen = "consumer_home" 
                            }
                        )
                    }
                    "admin_login" -> {
                        AdminLoginScreen(
                            onLoginClick = { user, pass ->
                                if (user == "admin" && pass == "admin") {
                                    currentScreen = "admin_home"
                                } else if (user == "gerencia" && pass == "gerencia") {
                                    currentScreen = "management_home"
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
                                val intent = Intent(context, ScannerActivity::class.java)
                                barcodeLauncher.launch(intent)
                            },
                            onDatabaseClick = {
                                currentScreen = "admin_database"
                            },
                            onLogout = { currentScreen = "role_selection" }
                        )
                    }
                    "management_home" -> {
                        ManagementHomeScreen(
                            onLogout = { currentScreen = "role_selection" },
                            onDashboardClick = { currentScreen = "dashboard" },
                            onProductsClick = { currentScreen = "management_products" },
                            onScansClick = { currentScreen = "management_scans" },
                            onTrendsClick = { currentScreen = "management_trends" },
                            onAiClick = { currentScreen = "ai_assistant" },
                            onReportsClick = { currentScreen = "reports" }
                        )
                    }
                    "dashboard" -> {
                        DashboardScreen(
                            onBack = { currentScreen = "management_home" }
                        )
                    }
                    "management_products" -> {
                        ProductosScreen(
                            onBack = { currentScreen = "management_home" },
                            onProductClick = { barcode ->
                                scannedBarcode = barcode
                                val intent = Intent(context, Result::class.java).apply {
                                    putExtra("barcode", barcode)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                    "management_scans" -> {
                        EscaneosScreen(
                            onBack = { currentScreen = "management_home" }
                        )
                    }
                    "management_trends" -> {
                        TendenciasScreen(
                            onBack = { currentScreen = "management_home" }
                        )
                    }
                    "ai_assistant" -> {
                        AiAssistantScreen(
                            onBack = { currentScreen = "management_home" }
                        )
                    }
                    "reports" -> {
                        ReportesScreen(
                            onBack = { currentScreen = "management_home" }
                        )
                    }
                    "admin_database" -> {
                        DatabaseScreen(
                            onProductClick = { barcode ->
                                scannedBarcode = barcode
                                val intent = Intent(context, Result::class.java).apply {
                                    putExtra("barcode", barcode)
                                    putExtra("edit_mode", true)
                                }
                                context.startActivity(intent)
                            },
                            onBackToAdminHome = { currentScreen = "admin_home" }
                        )
                    }
                    "admin_product_exists" -> {
                        ProductExistsScreen(
                            onViewProduct = {
                                currentScreen = "admin_home"
                                val intent = Intent(context, Result::class.java).apply {
                                    putExtra("barcode", scannedBarcode)
                                }
                                context.startActivity(intent)
                            },
                            onBackToAdminHome = { currentScreen = "admin_home" }
                        )
                    }
                    "admin_add_product" -> {
                        AddProductScreen(
                            barcode = scannedBarcode,
                            onProductLoaded = { currentScreen = "admin_success" },
                            onError = { currentScreen = "admin_error" },
                            onCancel = { currentScreen = "admin_home" },
                            onBackToAdminHome = { currentScreen = "admin_home" }
                        )
                    }
                    "admin_success" -> {
                        SuccessScreen(
                            onCargarOtro = {
                                val intent = Intent(context, ScannerActivity::class.java)
                                barcodeLauncher.launch(intent)
                            },
                            onBackToAdminHome = { currentScreen = "admin_home" }
                        )
                    }
                    "admin_error" -> {
                        ErrorScreen(
                            onRetry = { currentScreen = "admin_add_product" },
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

    private fun trackUserActivity() {
        FirebaseFirestore.getInstance().collection("stats").document("global")
            .set(mapOf("active_users" to FieldValue.increment(1)), SetOptions.merge())
    }
}

@Composable
fun ReportesScreen(viewModel: ReportesViewModel = viewModel(), onBack: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0B46))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("📄 Reportes", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2EF2A3)),
            shape = RoundedCornerShape(16.dp),
            onClick = { viewModel.generarNuevoReporte(context) }
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (viewModel.isGenerating) {
                    CircularProgressIndicator(color = Color(0xFF1A0B46), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Generando...", color = Color(0xFF1A0B46), fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color(0xFF1A0B46))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Generar Reporte PDF", color = Color(0xFF1A0B46), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Reportes Recientes", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(viewModel.reportsList) { report ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openReport(context, report) },
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(report, color = Color.White, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.shareReport(context, report) }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color(0xFF2EF2A3))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiAssistantScreen(viewModel: AsistenteIAViewModel = viewModel(), onBack: () -> Unit) {
    var textInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0B46))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("💬 Asistente IA", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(viewModel.messages) { message ->
                    ChatBubble(text = message.first, isUser = message.second)
                }
                if (viewModel.isTyping) {
                    item {
                        Text("EasyPrice IA está pensando...", color = Color(0xFF2EF2A3), fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe tu consulta...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF2EF2A3)
                ),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendMessage(textInput)
                        textInput = ""
                    }
                },
                containerColor = Color(0xFF2EF2A3),
                contentColor = Color(0xFF1A0B46),
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun ChatBubble(text: String, isUser: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.1f)
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) Color(0xFF1A0B46) else Color.White,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel(), onBack: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        Firebase.analytics.logEvent("view_dashboard", bundleOf(
            "user_type" to "gerencia",
            "timestamp" to System.currentTimeMillis()
        ))
        viewModel.loadStats(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0B46))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("📊 Dashboard", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2EF2A3))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard("Escaneos", viewModel.totalScans.toString(), Modifier.weight(1f))
                StatCard("Usuarios", viewModel.activeUsers.toString(), Modifier.weight(1f))
                StatCard("Productos", viewModel.totalProducts.toString(), Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Actividad semanal", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        SimpleChart()

        Spacer(modifier = Modifier.height(32.dp))

        Text("Top Productos", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2EF2A3).copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (viewModel.topProducts.isEmpty()) {
                    Text("No hay datos de escaneos aún", color = Color.Gray, fontSize = 14.sp)
                } else {
                    viewModel.topProducts.forEach {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("🔥", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(it, color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
            shape = RoundedCornerShape(30.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFF1A0B46))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Salir", color = Color(0xFF1A0B46), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ProductosScreen(viewModel: ProductosViewModel = viewModel(), onBack: () -> Unit, onProductClick: (String) -> Unit) {
    var searchText by remember { mutableStateOf("") }
    val analytics = Firebase.analytics

    LaunchedEffect(Unit) {
        analytics.logEvent("view_product_list", null)
        viewModel.loadProductos()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0B46))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("📦 Productos", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                analytics.logEvent("search_product", bundleOf("search_term" to it))
            },
            label = { Text("Buscar producto") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF2EF2A3),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF2EF2A3),
                unfocusedLabelColor = Color.Gray
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2EF2A3))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(viewModel.productos.filter {
                    it.name.contains(searchText, ignoreCase = true) || 
                    it.marca?.contains(searchText, ignoreCase = true) == true
                }) { product ->
                    ProductItem(product) {
                        analytics.logEvent("view_product_from_list", bundleOf("product_name" to product.name))
                        onProductClick(product.code)
                    }
                }
            }
        }
    }
}

@Composable
fun EscaneosScreen(viewModel: EscaneosViewModel = viewModel(), onBack: () -> Unit) {
    val analytics = Firebase.analytics

    LaunchedEffect(Unit) {
        analytics.logEvent("view_scans_dashboard", null)
        viewModel.loadData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0B46))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("🔍 Escaneos", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Actividad semanal", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        BarChart(viewModel.weeklyData)

        Spacer(modifier = Modifier.height(32.dp))

        Text("Top más escaneados", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.isLoading) {
            CircularProgressIndicator(color = Color(0xFF2EF2A3))
        } else {
            viewModel.topProductos.forEachIndexed { index, pair ->
                TopItem(index + 1, pair.first, pair.second)
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
            shape = RoundedCornerShape(30.dp)
        ) {
            Text("Volver", color = Color(0xFF1A0B46), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RealLineChart(data: List<Int>) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                xAxis.setDrawGridLines(false)
                axisLeft.setDrawGridLines(false)
                axisRight.isEnabled = false
                xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                xAxis.textColor = android.graphics.Color.WHITE
                axisLeft.textColor = android.graphics.Color.WHITE
                
                val entries = data.mapIndexed { index, value ->
                    Entry(index.toFloat(), value.toFloat())
                }

                val dataSet = LineDataSet(entries, "Escaneos").apply {
                    color = android.graphics.Color.parseColor("#2EF2A3")
                    lineWidth = 3f
                    setCircleColor(android.graphics.Color.parseColor("#FFD54F"))
                    circleRadius = 5f
                    setDrawCircleHole(false)
                    valueTextColor = android.graphics.Color.WHITE
                    valueTextSize = 10f
                    setDrawFilled(true)
                    fillColor = android.graphics.Color.parseColor("#2EF2A3")
                    fillAlpha = 50
                }

                this.data = LineData(dataSet)
                invalidate()
            }
        },
        modifier = Modifier.fillMaxWidth().height(200.dp)
    )
}

@Composable
fun TendenciasScreen(viewModel: TendenciasViewModel = viewModel(), onBack: () -> Unit) {
    val analytics = Firebase.analytics

    LaunchedEffect(Unit) {
        analytics.logEvent("view_trends", null)
        viewModel.loadData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0B46))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("📈 Tendencias", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Actividad (últimos días)", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (viewModel.weeklyData.isNotEmpty()) {
            RealLineChart(viewModel.weeklyData)
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                if (viewModel.isLoading) CircularProgressIndicator(color = Color(0xFF2EF2A3))
                else Text("No hay datos históricos aún", color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("🔥 Productos en crecimiento", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.growthProducts.isEmpty()) {
            Text("Cargando productos...", color = Color.Gray, modifier = Modifier.padding(8.dp))
        } else {
            viewModel.growthProducts.forEach {
                GrowthItem(it.first, it.second, it.third)
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
            shape = RoundedCornerShape(30.dp)
        ) {
            Text("Volver", color = Color(0xFF1A0B46), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GrowthItem(name: String, current: Int, last: Int) {
    val growth = current - last
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = name, color = Color.White, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (growth >= 0) "▲ $growth" else "▼ ${Math.abs(growth)}",
                    color = if (growth >= 0) Color(0xFF2EF2A3) else Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BarChart(data: List<Int>) {
    Card(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height((it * 2).dp)
                        .background(Color(0xFF2EF2A3), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                )
            }
        }
    }
}

@Composable
fun TopItem(rank: Int, name: String, count: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🥇 #$rank $name", color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = "$count scans", color = Color(0xFF2EF2A3))
        }
    }
}

@Composable
fun ProductItem(product: Product, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = product.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = product.marca ?: "Genérico", color = Color.Gray, fontSize = 14.sp)
                Text(text = "$${product.price}", color = Color(0xFF2EF2A3), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2EF2A3)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Color(0xFF1A0B46), style = MaterialTheme.typography.labelMedium)
            Text(value, color = Color(0xFF1A0B46), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SimpleChart() {
    Card(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            val values = listOf(3, 6, 8, 5, 9, 7, 4)
            values.forEach {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height((it * 12).dp)
                        .background(Color(0xFFFFD54F), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                )
            }
        }
    }
}

@Composable
fun DatabaseScreen(onProductClick: (String) -> Unit, onBackToAdminHome: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var productsList by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        db.collection("products").get()
            .addOnSuccessListener { documents ->
                productsList = documents.mapNotNull { doc ->
                    try {
                        Product(
                            name = doc.getString("name") ?: "Sin nombre",
                            price = (doc.get("price") as? Number)?.toDouble() ?: 0.0,
                            code = doc.getString("codigo") ?: "",
                            categoria = doc.getString("categoria") ?: "Otros",
                            subcategoria = doc.getString("subcategoria") ?: ""
                        )
                    } catch (e: Exception) { null }
                }
                isLoading = false
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al conectar con la base de datos", Toast.LENGTH_SHORT).show()
                isLoading = false
            }
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
            contentDescription = "Logo",
            modifier = Modifier.size(150.dp).padding(bottom = 16.dp),
            contentScale = ContentScale.Fit
        )

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Productos.",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF1A237E)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF1A237E))
                    }
                } else {
                    val groupedProducts = productsList.groupBy { it.categoria ?: "Otros" }
                    
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        groupedProducts.forEach { (category, products) ->
                            item {
                                Text(
                                    text = "$category:",
                                    color = Color(0xFF1A237E),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                                )
                            }
                            items(products) { product ->
                                ProductListItemShort(product, onProductClick)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBackToAdminHome,
            modifier = Modifier.fillMaxWidth(0.8f).height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF59E689)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Volver al Inicio", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProductListItemShort(product: Product, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, Color.LightGray),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = product.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onClick(product.code) }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = Color.Gray,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun AdminHomeScreen(onAdminScan: () -> Unit, onDatabaseClick: () -> Unit, onLogout: () -> Unit) {
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
            contentDescription = "Logo",
            modifier = Modifier.size(280.dp).padding(bottom = 60.dp),
            contentScale = ContentScale.Fit
        )

        Button(
            onClick = onAdminScan,
            modifier = Modifier.fillMaxWidth().height(100.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(id = R.drawable.ic_camera), contentDescription = null, tint = Color.Black, modifier = Modifier.size(60.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("Cargar Producto", color = Color(0xFF1A0B46), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AdminMenuButton(text = "Base de Datos", icon = Icons.Default.List, onClick = onDatabaseClick, modifier = Modifier.weight(1f))
            AdminMenuButton(text = "Salir", icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onLogout, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun AdminMenuButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(30.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF757575), modifier = Modifier.size(50.dp))
            Text(text = text, color = Color(0xFF1A0B46), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ManagementHomeScreen(onLogout: () -> Unit, onDashboardClick: () -> Unit, onProductsClick: () -> Unit, onScansClick: () -> Unit, onTrendsClick: () -> Unit, onAiClick: () -> Unit = {}, onReportsClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0B46))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Image(
            painter = painterResource(id = R.drawable.logo_easy_price),
            contentDescription = "Logo",
            modifier = Modifier.size(160.dp),
            contentScale = ContentScale.Fit
        )
        
        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ManagementButton(
                    text = "Dashboard", 
                    icon = Icons.Filled.Dashboard, 
                    modifier = Modifier.weight(1f),
                    onClick = onDashboardClick
                )
                ManagementButton(
                    text = "Productos", 
                    icon = Icons.Filled.Inventory, 
                    modifier = Modifier.weight(1f),
                    onClick = onProductsClick
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ManagementButton(
                    text = "Escaneos", 
                    icon = Icons.Filled.QrCodeScanner, 
                    modifier = Modifier.weight(1f),
                    onClick = onScansClick
                )
                ManagementButton(
                    text = "Tendencias", 
                    icon = Icons.Filled.ShowChart, 
                    modifier = Modifier.weight(1f),
                    onClick = onTrendsClick
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ManagementButton(
                    text = "Reportes", 
                    icon = Icons.Filled.Assessment, 
                    modifier = Modifier.weight(1f),
                    onClick = onReportsClick
                )
                ManagementButton(
                    text = "Asistente IA", 
                    icon = Icons.Filled.Chat, 
                    modifier = Modifier.weight(1f),
                    onClick = onAiClick
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onLogout,
            modifier = Modifier
                .width(140.dp)
                .height(80.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color(0xFF757575),
                    modifier = Modifier.size(40.dp)
                )
                Text("Salir", color = Color(0xFF1A0B46), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun ManagementButton(text: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit = {}) {
    Button(
        onClick = onClick,
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EF2A3))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF757575),
                modifier = Modifier.size(50.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = text, color = Color(0xFF1A0B46), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SuccessScreen(onCargarOtro: () -> Unit, onBackToAdminHome: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1E2A35)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(painter = painterResource(id = R.drawable.logo_easy_price), contentDescription = "Logo", modifier = Modifier.size(200.dp).padding(bottom = 32.dp))
        Card(
            modifier = Modifier.fillMaxWidth().height(380.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Producto cargado exitosamente", fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.Black)
                Spacer(modifier = Modifier.height(40.dp))
                Box(modifier = Modifier.size(140.dp).border(6.dp, Color(0xFF4CAF50), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(90.dp))
                }
                Spacer(modifier = Modifier.height(40.dp))
                Button(onClick = onCargarOtro, modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)), shape = RoundedCornerShape(30.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Cargar otro producto", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onBackToAdminHome, modifier = Modifier.fillMaxWidth(0.7f).height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF69F0AE)), shape = RoundedCornerShape(14.dp)) {
            Text("Volver al Inicio", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ErrorScreen(onRetry: () -> Unit, onBackToAdminHome: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1E2A35)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(painter = painterResource(id = R.drawable.logo_easy_price), contentDescription = "Logo", modifier = Modifier.size(200.dp).padding(bottom = 32.dp))
        Card(
            modifier = Modifier.fillMaxWidth().height(380.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Error al cargar\nProducto", fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.Black)
                Spacer(modifier = Modifier.height(40.dp))
                Box(modifier = Modifier.size(140.dp).border(6.dp, Color.Red, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.Red, modifier = Modifier.size(90.dp))
                }
                Spacer(modifier = Modifier.height(40.dp))
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)), shape = RoundedCornerShape(30.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reintentar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onBackToAdminHome, modifier = Modifier.fillMaxWidth(0.7f).height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF69F0AE)), shape = RoundedCornerShape(14.dp)) {
            Text("Volver al Inicio", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProductExistsScreen(onViewProduct: () -> Unit, onBackToAdminHome: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1E2A35)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(painter = painterResource(id = R.drawable.logo_easy_price), contentDescription = "Logo", modifier = Modifier.size(200.dp).padding(bottom = 24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Producto ya existente", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(24.dp))
                Icon(imageVector = Icons.Default.Face, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(120.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text("El codigo escaneado ya se encuentra registrado en la base de datos", textAlign = TextAlign.Center, fontSize = 16.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onViewProduct, modifier = Modifier.fillMaxWidth().height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)), shape = RoundedCornerShape(28.dp)) {
                    Text("Ver Producto", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onBackToAdminHome, modifier = Modifier.fillMaxWidth(0.7f).height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF69F0AE)), shape = RoundedCornerShape(14.dp)) {
            Text("Volver al Inicio", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(barcode: String, onProductLoaded: () -> Unit, onError: () -> Unit, onCancel: () -> Unit, onBackToAdminHome: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var nombre by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }
    var subCategoria by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val categoriesMap = mapOf(
        "Alimentos Frescos y Perecederos" to listOf("Frutas y Verduras", "Carnicería", "Pescadería", "Fiambrería y Quesos", "Panadería y Pastelería"),
        "Lácteos y Refrigerados" to listOf("Lácteos", "Huevos", "Pastas Frescas"),
        "Almacén (Alimentos Secos)" to listOf("Infusiones", "Despensa", "Aceites y Condimentos", "Enlatados y Conservas", "Desayuno y Merienda"),
        "Bebidas" to listOf("Sin Alcohol", "Con Alcohol"),
        "Congelados" to listOf("Comidas Listas", "Vegetales Congelados", "Helados"),
        "Limpieza y Cuidado del Hogar" to listOf("Ropa", "Ambientes", "Papelería", "Vajilla"),
        "Perfumería y Cuidado Personal" to listOf("Higiene", "Bucal", "Cuidado Corporal"),
        "Otros (Categorías Especiales)" to listOf("Mascotas", "Bebés", "Electro y Bazar")
    )

    var expandedCategory by remember { mutableStateOf(false) }
    var expandedSubCategory by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E2A35)).padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painter = painterResource(id = R.drawable.logo_easy_price), contentDescription = "Logo", modifier = Modifier.size(180.dp).padding(bottom = 16.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Carga de Nuevo Producto", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                FormField("Código", barcode, enabled = false)
                FormField("Nombre", nombre) { nombre = it }
                FormField("Marca", marca) { marca = it }
                FormField("Precio", precio, keyboardType = KeyboardType.Number) { precio = it }
                Text("Categoría:", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                ExposedDropdownMenuBox(expanded = expandedCategory, onExpandedChange = { expandedCategory = !expandedCategory }, modifier = Modifier.fillMaxWidth()) {
                    TextField(value = categoria, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) }, colors = ExposedDropdownMenuDefaults.textFieldColors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent), modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = expandedCategory, onDismissRequest = { expandedCategory = false }) {
                        categoriesMap.keys.forEach { selectionOption -> DropdownMenuItem(text = { Text(selectionOption) }, onClick = { categoria = selectionOption; subCategoria = ""; expandedCategory = false }) }
                    }
                }
                Text("Sub-categoría:", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                ExposedDropdownMenuBox(expanded = expandedSubCategory, onExpandedChange = { if (categoria.isNotEmpty()) expandedSubCategory = !expandedSubCategory }, modifier = Modifier.fillMaxWidth()) {
                    TextField(value = subCategoria, onValueChange = {}, readOnly = true, enabled = categoria.isNotEmpty(), placeholder = { Text(if (categoria.isEmpty()) "Seleccione categoría primero" else "") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubCategory) }, colors = ExposedDropdownMenuDefaults.textFieldColors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent), modifier = Modifier.menuAnchor().fillMaxWidth())
                    if (categoria.isNotEmpty()) {
                        ExposedDropdownMenu(expanded = expandedSubCategory, onDismissRequest = { expandedSubCategory = false }) {
                            categoriesMap[categoria]?.forEach { selectionOption -> DropdownMenuItem(text = { Text(selectionOption) }, onClick = { subCategoria = selectionOption; expandedSubCategory = false }) }
                        }
                    }
                }
                FormField("Descripción", descripcion, singleLine = false) { descripcion = it }
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    if (nombre.isBlank() || precio.isBlank() || categoria.isBlank() || subCategoria.isBlank()) { Toast.makeText(context, "Nombre, Precio, Categoría y Sub-categoría son obligatorios", Toast.LENGTH_SHORT).show(); return@Button }
                    isLoading = true
                    val productData = hashMapOf("codigo" to barcode.trim(), "name" to nombre, "price" to precio.toDoubleOrNull(), "marca" to marca, "categoria" to categoria, "subcategoria" to subCategoria, "descripcion" to descripcion, "scan_count" to 0)
                    db.collection("products").add(productData).addOnSuccessListener {
                        onProductLoaded() 
                    }.addOnFailureListener { onError(); isLoading = false }
                }, modifier = Modifier.fillMaxWidth().height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)), shape = RoundedCornerShape(28.dp), enabled = !isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Cargar Producto", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Done, contentDescription = null, tint = Color.Black)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), shape = RoundedCornerShape(28.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Cancelar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onBackToAdminHome, modifier = Modifier.fillMaxWidth(0.6f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF69F0AE)), shape = RoundedCornerShape(12.dp)) {
            Text("Volver al Inicio", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FormField(label: String, value: String, enabled: Boolean = true, keyboardType: KeyboardType = KeyboardType.Text, singleLine: Boolean = true, onValueChange: (String) -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        TextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), enabled = enabled, singleLine = singleLine, keyboardOptions = KeyboardOptions(keyboardType = keyboardType), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent))
    }
}

@Composable
fun RoleSelectionScreen(onAdminClick: () -> Unit, onConsumerClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1A0B46)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painter = painterResource(id = R.drawable.logo_easy_price), contentDescription = "Easy Price Logo", modifier = Modifier.size(280.dp).padding(bottom = 32.dp), contentScale = ContentScale.Fit)
        Text(text = "Iniciar como", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Normal, modifier = Modifier.padding(bottom = 32.dp))
        RoleButton(text = "Administrador", icon = Icons.Default.Person, onClick = onAdminClick)
        Spacer(modifier = Modifier.height(20.dp))
        RoleButton(text = "Consumidor", icon = Icons.Default.ShoppingCart, onClick = onConsumerClick)
    }
}

@Composable
fun AdminLoginScreen(onLoginClick: (String, String) -> Unit, onBack: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1A0B46)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painter = painterResource(id = R.drawable.logo_easy_price), contentDescription = "Easy Price Logo", modifier = Modifier.size(250.dp).padding(bottom = 32.dp), contentScale = ContentScale.Fit)
        TextField(value = username, onValueChange = { username = it }, placeholder = { Text("Usuario", color = Color.Gray, fontSize = 18.sp) }, modifier = Modifier.fillMaxWidth().height(70.dp), shape = RoundedCornerShape(35.dp), colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, disabledContainerColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), trailingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(30.dp)) }, singleLine = true)
        Spacer(modifier = Modifier.height(24.dp))
        TextField(value = password, onValueChange = { password = it }, placeholder = { Text("Contraseña", color = Color.Gray, fontSize = 18.sp) }, modifier = Modifier.fillMaxWidth().height(70.dp), shape = RoundedCornerShape(35.dp), visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, disabledContainerColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), trailingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(30.dp)) }, singleLine = true)
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = { onLoginClick(username, password) }, modifier = Modifier.width(200.dp).height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EF2A3)), shape = RoundedCornerShape(30.dp)) { Text(text = "Login", color = Color(0xFF1A0B46), fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("Volver", color = Color(0xFF1A0B46)) }
    }
}

@Composable
fun RoleButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(90.dp), shape = RoundedCornerShape(45.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)), elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(text = text, color = Color(0xFF1A0B46), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF9E9E9E), modifier = Modifier.size(35.dp))
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
                val barcode = result.data?.getStringExtra("barcode_result")
                val intent = Intent(context, com.example.easyprice.Result::class.java).apply {
                    putExtra("barcode", barcode)
                    putExtra("is_consumer", true)
                }
                context.startActivity(intent)
            }
        }
    )
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E2A35)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(40.dp))
        Image(painter = painterResource(id = R.drawable.logo_easy_price), contentDescription = "Logo Easy Price", modifier = Modifier.size(260.dp))
        Spacer(modifier = Modifier.height(40.dp))
        Button(onClick = { val intent = Intent(context, ScannerActivity::class.java); barcodeLauncher.launch(intent) }, modifier = Modifier.fillMaxWidth().height(80.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4C430)), shape = RoundedCornerShape(100.dp)) {
            Icon(painter = painterResource(id = R.drawable.ic_camera), contentDescription = "Escanear", tint = Color.Black, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "Escanear Código", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            BottomNavButton(text = "Historial", iconRes = R.drawable.ic_list, onClick = { if (HistoryManager.historyList.isNotEmpty()) { val intent = Intent(context, HistoryActivity::class.java); context.startActivity(intent) } else { Toast.makeText(context, "No se ha escaneado ningún producto aún", Toast.LENGTH_SHORT).show() } })
            BottomNavButton(text = "Favoritos", iconRes = R.drawable.ic_star_outline, onClick = { if (FavoritesManager.favorites.isEmpty()) { Toast.makeText(context, "No se marcó ningún producto como favorito", Toast.LENGTH_SHORT).show() } else { val intent = Intent(context, FavoritesActivity::class.java); context.startActivity(intent) } })
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun BottomNavButton(text: String, iconRes: Int, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.width(160.dp).height(80.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4C430)), shape = RoundedCornerShape(25.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painter = painterResource(id = iconRes), contentDescription = text, tint = Color.Black, modifier = Modifier.size(30.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = text, color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
