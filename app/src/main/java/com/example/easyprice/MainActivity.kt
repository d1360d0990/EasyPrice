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
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

// --- ViewModels ---

class DashboardViewModel : ViewModel() {
    var totalScans by mutableIntStateOf(0)
    var activeUsers by mutableIntStateOf(0)
    var totalProducts by mutableIntStateOf(0)
    var isLoading by mutableStateOf(false)
    var topProducts by mutableStateOf<List<String>>(emptyList())
    var selectedFilter by mutableStateOf("Historico")
    
    // Indicadores de Tendencia
    var scansTrend by mutableStateOf("")
    var usersTrend by mutableStateOf("")
    
    // Alertas Críticas
    var criticalAlerts by mutableStateOf<List<String>>(emptyList())

    fun loadStats(filter: String = "Historico") {
        selectedFilter = filter
        isLoading = true
        val db = FirebaseFirestore.getInstance()
        db.collection("stats").document("global").get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val multiplier = when(filter) {
                    "Hoy" -> 0.05
                    "7D" -> 0.25
                    "30D" -> 0.6
                    "Año" -> 0.9
                    else -> 1.0
                }
                totalScans = ((doc.getLong("total_scans") ?: 0) * multiplier).toInt()
                activeUsers = ((doc.getLong("active_users") ?: 0) * multiplier).toInt()
                
                // Simulación de tendencias
                scansTrend = if (filter == "7D") "+18%" else if (filter == "Hoy") "+5%" else "+12%"
                usersTrend = if (filter == "7D") "-2%" else "+8%"
            }
            db.collection("products").count().get(AggregateSource.SERVER).addOnSuccessListener { snapshot ->
                totalProducts = snapshot.count.toInt()
                db.collection("product_stats").orderBy("scan_count", Query.Direction.DESCENDING).limit(5).get().addOnSuccessListener { topSnapshot ->
                    topProducts = topSnapshot.documents.map { it.getString("name") ?: "Sin nombre" }
                    
                    // Simulación de Alertas Críticas
                    criticalAlerts = listOf(
                        "12 productos sin precio detectados",
                        "Descalce de precio en 'Leche Entera 1L' (Subió 25%)",
                        "Baja frecuencia de escaneo en zona Norte"
                    )
                    
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
                        description = doc.getString("descripcion") ?: "",
                        quantity = doc.getLong("quantity")?.toInt() ?: 100
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
    var branchActivity by mutableStateOf<List<Pair<String, Int>>>(emptyList())
    var peakHoursData by mutableStateOf<List<Int>>(emptyList())
    var failedScans by mutableStateOf<List<String>>(emptyList())
    var isLoading by mutableStateOf(false)

    fun loadData() {
        isLoading = true
        val db = FirebaseFirestore.getInstance()
        
        // Cargar Top Productos
        db.collection("product_stats").orderBy("scan_count", Query.Direction.DESCENDING).limit(5).get().addOnSuccessListener { result ->
            topProductos = result.map { Pair(it.getString("name") ?: "", it.getLong("scan_count")?.toInt() ?: 0) }
            isLoading = false
        }.addOnFailureListener { isLoading = false }
        
        // Simulación de datos dinámicos solicitados
        weeklyData = listOf(10, 25, 40, 30, 50, 60, 45)
        
        // Mapa de Calor Local (Actividad por Sucursal)
        branchActivity = listOf(
            "Sucursal Centro" to 1250,
            "Sucursal Norte" to 850,
            "Sucursal Sur" to 600,
            "Sucursal Este" to 450
        )
        
        // Horas Pico (Escaneos por hora del día 0-23)
        peakHoursData = listOf(5, 2, 1, 0, 0, 2, 10, 35, 80, 120, 150, 180, 210, 190, 160, 140, 170, 220, 250, 200, 120, 80, 40, 15)
        
        // Escaneos Fallidos (Productos no encontrados)
        failedScans = listOf("7791234567890", "7799876543210", "041234567892", "779456123789")
    }
}

class TendenciasViewModel : ViewModel() {
    var weeklyData by mutableStateOf<List<Int>>(emptyList())
    var hotProducts by mutableStateOf<List<Pair<String, Double>>>(emptyList())
    var depletionPredictions by mutableStateOf<List<Pair<String, Int>>>(emptyList())
    var selectedProductDailyData by mutableStateOf<List<Int>>(emptyList())
    var isLoading by mutableStateOf(false)

    fun loadData() {
        isLoading = true
        val db = FirebaseFirestore.getInstance()
        
        // Datos generales de escaneos semanales para la línea de tiempo
        db.collection("scans_by_day").orderBy("__name__", Query.Direction.ASCENDING).limitToLast(7).get().addOnSuccessListener { result ->
            weeklyData = result.map { it.getLong("count")?.toInt() ?: 0 }
            isLoading = false
        }.addOnFailureListener { isLoading = false }
        
        // Productos con crecimiento (Hot Products > 20%)
        db.collection("product_stats").get().addOnSuccessListener { result ->
            val list = result.map {
                val name = it.getString("name") ?: ""
                val current = it.getLong("scan_count")?.toInt() ?: 0
                val last = it.getLong("last_week_count")?.toInt() ?: 1
                val growth = ((current - last).toDouble() / last) * 100
                name to growth
            }
            
            hotProducts = list.filter { it.second > 20 }.sortedByDescending { it.second }.take(5)

            // Predicción de Agotamiento (Basado en demanda diaria vs stock simulado)
            depletionPredictions = listOf(
                "Leche Entera 1L" to 3,
                "Yerba Mate 500g" to 2,
                "Pan Lactal Familiar" to 5,
                "Aceite Girasol 1.5L" to 8
            )

            // Comparativa por días de la semana (Producto Específico)
            selectedProductDailyData = listOf(12, 18, 25, 30, 45, 60, 55) // Lunes a Domingo
        }
    }
}

class ReportesViewModel : ViewModel() {
    var isGenerating by mutableStateOf(false)
    var reportsList = mutableStateListOf<String>()
    
    // Filtros de columnas
    var includePrice by mutableStateOf(true)
    var includeStock by mutableStateOf(true)
    var includeCategory by mutableStateOf(true)
    var includeCode by mutableStateOf(false)
    
    // Programación
    var scheduleEnabled by mutableStateOf(false)
    var targetEmail by mutableStateOf("gerente@easyprice.com")

    data class ReportProduct(val name: String, val price: Double, val stock: Int, val category: String, val code: String)

    fun generarReportePdf(context: Context) {
        isGenerating = true
        val db = FirebaseFirestore.getInstance()
        db.collection("products").get().addOnSuccessListener { result ->
            val products = result.map { doc ->
                ReportProduct(
                    name = doc.getString("name") ?: "N/A",
                    price = doc.getDouble("price") ?: 0.0,
                    stock = doc.getLong("quantity")?.toInt() ?: 0,
                    category = doc.getString("categoria") ?: "Sin cat.",
                    code = doc.getString("codigo") ?: ""
                )
            }
            crearPdfReal(context, products)
        }.addOnFailureListener { isGenerating = false }
    }

    fun generarReporteExcel(context: Context) {
        isGenerating = true
        val db = FirebaseFirestore.getInstance()
        db.collection("products").get().addOnSuccessListener { result ->
            val products = result.map { doc ->
                ReportProduct(
                    name = doc.getString("name") ?: "N/A",
                    price = doc.getDouble("price") ?: 0.0,
                    stock = doc.getLong("quantity")?.toInt() ?: 0,
                    category = doc.getString("categoria") ?: "Sin cat.",
                    code = doc.getString("codigo") ?: ""
                )
            }
            crearCsvReal(context, products)
        }.addOnFailureListener { isGenerating = false }
    }

    private fun crearCsvReal(context: Context, products: List<ReportProduct>) {
        val fileName = "reporte_${System.currentTimeMillis()}.csv"
        val file = File(context.getExternalFilesDir(null), fileName)
        try {
            val writer = PrintWriter(FileOutputStream(file))
            val header = StringBuilder("Producto")
            if (includePrice) header.append(",Precio")
            if (includeStock) header.append(",Stock")
            if (includeCategory) header.append(",Categoria")
            if (includeCode) header.append(",Codigo")
            writer.println(header.toString())

            products.forEach { p ->
                val row = StringBuilder(p.name)
                if (includePrice) row.append(",${p.price}")
                if (includeStock) row.append(",${p.stock}")
                if (includeCategory) row.append(",${p.category}")
                if (includeCode) row.append(",${p.code}")
                writer.println(row.toString())
            }
            writer.close()
            reportsList.add(0, fileName)
            Toast.makeText(context, "Excel/CSV generado", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { Log.e("CSV", e.message ?: "") }
        finally { isGenerating = false }
    }

    private fun crearPdfReal(context: Context, products: List<ReportProduct>) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("Reporte de Inventario EasyPrice", 50f, 50f, paint)
        
        paint.textSize = 12f
        paint.isFakeBoldText = false
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Fecha: $date", 50f, 80f, paint)

        var y = 120f
        paint.isFakeBoldText = true
        canvas.drawText("Producto", 50f, y, paint)
        var xOffset = 250f
        if (includePrice) { canvas.drawText("Precio", xOffset, y, paint); xOffset += 80f }
        if (includeStock) { canvas.drawText("Stock", xOffset, y, paint); xOffset += 80f }
        if (includeCategory) { canvas.drawText("Cat.", xOffset, y, paint) }
        
        y += 20f
        canvas.drawLine(50f, y - 10f, 550f, y - 10f, paint)
        
        paint.isFakeBoldText = false
        products.take(20).forEach { p ->
            canvas.drawText(p.name.take(25), 50f, y, paint)
            var xVal = 250f
            if (includePrice) { canvas.drawText("$${p.price}", xVal, y, paint); xVal += 80f }
            if (includeStock) { canvas.drawText("${p.stock}", xVal, y, paint); xVal += 80f }
            if (includeCategory) { canvas.drawText(p.category.take(15), xVal, y, paint) }
            y += 20f
            if (y > 800) return@forEach // Limitar a una página por simplicidad
        }

        pdfDocument.finishPage(page)
        val fileName = "reporte_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            reportsList.add(0, fileName)
            Toast.makeText(context, "PDF generado", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { Log.e("PDF", e.message ?: "") }
        finally { pdfDocument.close(); isGenerating = false }
    }

    fun toggleSchedule(enabled: Boolean, context: Context) {
        scheduleEnabled = enabled
        if (enabled) {
            Toast.makeText(context, "Reporte programado: Lunes 8:00 AM para $targetEmail", Toast.LENGTH_LONG).show()
        }
    }

    fun openReport(context: Context, fileName: String) {
        val file = File(context.getExternalFilesDir(null), fileName)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val type = if (fileName.endsWith(".pdf")) "application/pdf" else "text/csv"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, type)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "No hay aplicación para abrir este archivo", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    fun shareReport(context: Context, fileName: String) {
        val file = File(context.getExternalFilesDir(null), fileName)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val type = if (fileName.endsWith(".pdf")) "application/pdf" else "text/csv"
            val intent = Intent(Intent.ACTION_SEND).apply {
                this.type = type
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir"))
        }
    }

    fun generarNuevoReporte(context: Context) {
        // Mantenemos compatibilidad con el dashboard llamando a PDF por defecto
        generarReportePdf(context)
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
            val response = processQuery(query)
            messages.add(response to false)
            isTyping = false
        }, 1500)
    }

    private fun processQuery(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("producto más buscado") || q.contains("más buscado") -> {
                "El producto más buscado el lunes pasado en la tarde fue 'Leche Entera 1L' con 145 escaneos en la sucursal Centro."
            }
            q.contains("sugerencia") || q.contains("precio") -> {
                "Basado en la competencia local, sugiero bajar el precio de los 'Auriculares Bluetooth' un 5% para aumentar un 10% las ventas estimadas esta semana."
            }
            q.contains("resumen") -> {
                generateSummary()
            }
            else -> "Entiendo tu consulta sobre '$query'. ¿Deseas ver estadísticas específicas o sugerencias de precios?"
        }
    }

    fun generateSummary(): String {
        return "Resumen del día: Hoy se registraron 1,250 escaneos, un 12% más que ayer. El producto estrella sigue siendo la 'Yerba Mate 500g'. Se detectaron 3 productos con stock crítico en la sucursal Norte."
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
fun DashboardScreen(isWide: Boolean, viewModel: DashboardViewModel = viewModel(), reportesViewModel: ReportesViewModel = viewModel(), onBack: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.loadStats() }
    Column(Modifier.fillMaxSize().background(Color(0xFF1A0B46)).padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                Text("📊 Dashboard", color = Color.White, style = MaterialTheme.typography.headlineMedium) 
            }
            IconButton(onClick = { reportesViewModel.generarNuevoReporte(context) }) {
                if (reportesViewModel.isGenerating) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = Color(0xFF2EF2A3))
                } else {
                    Icon(Icons.Default.Download, contentDescription = "Exportar Reporte", tint = Color(0xFF2EF2A3))
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Filtros Temporales (Chips)
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("Hoy", "7D", "30D", "Año", "Historico")
            filters.forEach { filter ->
                val isSelected = viewModel.selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.loadStats(filter) },
                    label = { Text(filter, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = Color.White,
                        selectedContainerColor = Color(0xFF2EF2A3),
                        selectedLabelColor = Color.Black
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color.White.copy(alpha = 0.5f),
                        selectedBorderColor = Color(0xFF2EF2A3),
                        borderWidth = 1.dp
                    )
                )
            }
        }
        
        if (isWide) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard("Escaneos", viewModel.totalScans.toString(), viewModel.scansTrend, Modifier.weight(1f))
                StatCard("Usuarios", viewModel.activeUsers.toString(), viewModel.usersTrend, Modifier.weight(1f))
                StatCard("Productos", viewModel.totalProducts.toString(), "", Modifier.weight(1f))
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Escaneos", viewModel.totalScans.toString(), viewModel.scansTrend, Modifier.weight(1f))
                StatCard("Usuarios", viewModel.activeUsers.toString(), viewModel.usersTrend, Modifier.weight(1f))
            }
            Spacer(Modifier.height(24.dp)); SimpleChart()
        }

        Spacer(Modifier.height(24.dp))
        
        // Notificaciones Críticas
        Text("🔔 Alertas Críticas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        viewModel.criticalAlerts.forEach { alert ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB71C1C))
                    Spacer(Modifier.width(12.dp))
                    Text(alert, color = Color(0xFFB71C1C), fontSize = 14.sp)
                }
            }
        }
        
        Button(onBack, Modifier.fillMaxWidth().padding(top = 32.dp).height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))) { Text("Volver", color = Color.Black) }
    }
}

@Composable
fun StatCard(t: String, v: String, trend: String, modifier: Modifier) {
    Card(modifier.height(110.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2EF2A3))) {
        Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(t, color = Color.Black, fontSize = 14.sp)
            Text(v, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            if (trend.isNotEmpty()) {
                val isPositive = trend.startsWith("+")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (isPositive) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(trend, color = if (isPositive) Color(0xFF1B5E20) else Color(0xFFB71C1C), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
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
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }; Text("🔍 Auditoría en Tiempo Real", color = Color.White, style = MaterialTheme.typography.headlineMedium) }
        
        Spacer(Modifier.height(24.dp))
        
        // Mapa de Calor Local
        Text("📍 Actividad por Sucursal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        BranchActivityChart(v.branchActivity)
        
        Spacer(Modifier.height(24.dp))
        
        // Horas Pico
        Text("⏰ Horas Pico de Escaneo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        PeakHoursChart(v.peakHoursData)
        
        Spacer(Modifier.height(24.dp))
        
        // Escaneos Fallidos
        Text("❌ Escaneos Fallidos (Oro para Inventario)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        FailedScansList(v.failedScans)

        Spacer(Modifier.height(24.dp))
        
        // Top Productos (Anteriormente principal)
        Text("🏆 Top Productos Escaneados", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        v.topProductos.forEachIndexed { i, p -> TopItem(i + 1, p.first, p.second) }
        
        Button(onBack, Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 16.dp).height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))) { Text("Volver", color = Color.Black) }
    }
}

@Composable
fun BranchActivityChart(data: List<Pair<String, Int>>) {
    Card(Modifier.fillMaxWidth().height(250.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f))) {
        AndroidView(factory = { ctx ->
            PieChart(ctx).apply {
                description.isEnabled = false
                legend.textColor = android.graphics.Color.WHITE
                setHoleColor(android.graphics.Color.TRANSPARENT)
                setCenterTextColor(android.graphics.Color.WHITE)
                val entries = data.map { PieEntry(it.second.toFloat(), it.first) }
                val dataSet = PieDataSet(entries, "").apply {
                    colors = listOf(
                        android.graphics.Color.parseColor("#2EF2A3"),
                        android.graphics.Color.parseColor("#FFD54F"),
                        android.graphics.Color.parseColor("#4FC3F7"),
                        android.graphics.Color.parseColor("#BA68C8")
                    )
                    valueTextColor = android.graphics.Color.WHITE
                    valueTextSize = 12f
                }
                this.data = PieData(dataSet)
                invalidate()
            }
        }, modifier = Modifier.fillMaxSize().padding(16.dp))
    }
}

@Composable
fun PeakHoursChart(data: List<Int>) {
    Card(Modifier.fillMaxWidth().height(200.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f))) {
        AndroidView(factory = { ctx ->
            BarChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                xAxis.textColor = android.graphics.Color.WHITE
                xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                axisLeft.textColor = android.graphics.Color.WHITE
                axisRight.isEnabled = false
                
                val entries = data.mapIndexed { i, v -> BarEntry(i.toFloat(), v.toFloat()) }
                val dataSet = BarDataSet(entries, "Escaneos").apply {
                    color = android.graphics.Color.parseColor("#FFD54F")
                    setDrawValues(false)
                }
                this.data = BarData(dataSet)
                invalidate()
            }
        }, modifier = Modifier.fillMaxSize().padding(16.dp))
    }
}

@Composable
fun FailedScansList(scans: List<String>) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f))) {
        Column(Modifier.padding(16.dp)) {
            if (scans.isEmpty()) {
                Text("No hay escaneos fallidos registrados.", color = Color.White.copy(0.6f))
            } else {
                scans.forEach { barcode ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(barcode, color = Color.White, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        Spacer(Modifier.weight(1f))
                        Text("Pendiente", color = Color(0xFFFFD54F), fontSize = 12.sp)
                    }
                }
            }
        }
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
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { 
            Text("#$r $n", color = Color.Black, fontWeight = FontWeight.Bold)
            Text("$c scans", color = Color.Black.copy(0.7f)) 
        }
    }
}

@Composable
fun TendenciasScreen(v: TendenciasViewModel = viewModel(), onBack: () -> Unit) {
    LaunchedEffect(Unit) { v.loadData() }
    Column(Modifier.fillMaxSize().background(Color(0xFF1A0B46)).padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }; Text("📈 Análisis Predictivo", color = Color.White, style = MaterialTheme.typography.headlineMedium) }
        
        Spacer(Modifier.height(24.dp))
        
        // 1. Productos "Hot" (>20% crecimiento)
        Text("🔥 Productos 'Hot' (>20% crecimiento semanal)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        if (v.hotProducts.isEmpty()) {
            Text("No hay productos con crecimiento explosivo esta semana.", color = Color.White.copy(0.6f), fontSize = 14.sp)
        } else {
            v.hotProducts.forEach { (name, growth) ->
                HotProductItem(name, growth)
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // 2. Predicción de Agotamiento (Vista al Futuro)
        Text("📉 Predicción de Agotamiento (Stock Crítico)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        v.depletionPredictions.forEach { (name, days) ->
            PredictionItem(name, days)
        }
        
        Spacer(Modifier.height(24.dp))
        
        // 3. Días de la semana (Comparativa Visual)
        Text("📅 Actividad Semanal: 'Leche Entera 1L'", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Comparativa de búsquedas por día de la semana", color = Color.White.copy(0.7f), fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        WeeklyComparisonChart(v.selectedProductDailyData)

        Spacer(Modifier.height(32.dp))
        
        // Tendencia General Histórica
        Text("📈 Tendencia General de Escaneos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        RealLineChart(v.weeklyData)
        
        Button(onBack, Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 16.dp).height(55.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))) { Text("Volver", color = Color.Black) }
    }
}

@Composable
fun HotProductItem(name: String, growth: Double) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2EF2A3))) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Tendencia alcista detectada", color = Color.Black.copy(0.7f), fontSize = 12.sp)
            }
            Text("▲ ${growth.toInt()}%", color = Color(0xFF1B5E20), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        }
    }
}

@Composable
fun PredictionItem(name: String, days: Int) {
    val statusColor = when {
        days <= 2 -> Color(0xFFFF5252) // Crítico
        days <= 4 -> Color(0xFFFFD54F) // Alerta
        else -> Color(0xFF2EF2A3)      // Estable
    }
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f))) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, color = Color.White, fontWeight = FontWeight.Medium)
                Text("Basado en scans de hoy...", color = Color.White.copy(0.5f), fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Se agotará en", color = Color.White.copy(0.7f), fontSize = 10.sp)
                Text("$days días", color = statusColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun WeeklyComparisonChart(data: List<Int>) {
    val daysLabels = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    Card(Modifier.fillMaxWidth().height(220.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f))) {
        AndroidView(factory = { ctx ->
            BarChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                
                xAxis.apply {
                    textColor = android.graphics.Color.WHITE
                    position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                    valueFormatter = IndexAxisValueFormatter(daysLabels)
                    granularity = 1f
                    setDrawGridLines(false)
                }
                
                axisLeft.apply {
                    textColor = android.graphics.Color.WHITE
                    setDrawGridLines(true)
                    gridColor = android.graphics.Color.parseColor("#33FFFFFF")
                }
                
                axisRight.isEnabled = false
                
                val entries = data.mapIndexed { i, v -> BarEntry(i.toFloat(), v.toFloat()) }
                val dataSet = BarDataSet(entries, "Escaneos").apply {
                    colors = listOf(android.graphics.Color.parseColor("#2EF2A3"))
                    valueTextColor = android.graphics.Color.WHITE
                    valueTextSize = 10f
                }
                this.data = BarData(dataSet)
                animateY(1000)
                invalidate()
            }
        }, modifier = Modifier.fillMaxSize().padding(12.dp))
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                Text("💬 IA Asistente", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            }
            
            // Botón de Resumen Ejecutivo
            Button(
                onClick = { 
                    val summary = v.generateSummary()
                    v.messages.add(summary to false)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EF2A3)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Resumen Diario", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        LazyColumn(Modifier.weight(1f).padding(vertical = 16.dp)) {
            items(v.messages) { m -> ChatBubble(m.first, m.second) }
            if (v.isTyping) {
                item {
                    Text("IA escribiendo...", color = Color.White.copy(0.6f), fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                queryInput,
                { queryInput = it },
                Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                placeholder = { Text("Pregunta algo como '¿Cuál es el más buscado?'") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(0.9f),
                    unfocusedContainerColor = Color.White.copy(0.9f)
                )
            )
            FloatingActionButton(
                onClick = { 
                    if (queryInput.isNotBlank()) { 
                        v.sendMessage(queryInput)
                        queryInput = "" 
                    } 
                },
                containerColor = Color(0xFF2EF2A3),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null)
            }
        }
    }
}

@Composable
fun ChatBubble(t: String, u: Boolean) {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = if (u) Alignment.CenterEnd else Alignment.CenterStart) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (u) 16.dp else 0.dp,
                bottomEnd = if (u) 0.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = if (u) Color(0xFFFFD54F) else Color.White.copy(0.1f))
        ) {
            Text(t, Modifier.padding(12.dp), color = if (u) Color.Black else Color.White)
        }
    }
}

@Composable
fun ReportesScreen(v: ReportesViewModel = viewModel(), onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(Color(0xFF1A0B46)).padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) { 
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Text("📄 Reportes y Legal", color = Color.White, style = MaterialTheme.typography.headlineMedium) 
        }

        Spacer(Modifier.height(24.dp))

        // Filtro de Columnas
        Text("⚙️ Configuración de Columnas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Card(Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f))) {
            Column(Modifier.padding(16.dp)) {
                ColumnRowSelection("Precio", v.includePrice) { v.includePrice = it }
                ColumnRowSelection("Stock", v.includeStock) { v.includeStock = it }
                ColumnRowSelection("Categoría", v.includeCategory) { v.includeCategory = it }
                ColumnRowSelection("Código Barras", v.includeCode) { v.includeCode = it }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Formatos de Exportación
        Text("📥 Exportar Reporte Actual", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { v.generarReportePdf(context) },
                modifier = Modifier.weight(1f).height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EF2A3))
            ) {
                if (v.isGenerating) CircularProgressIndicator(Modifier.size(24.dp), color = Color.Black)
                else Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PictureAsPdf, null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("PDF", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = { v.generarReporteExcel(context) },
                modifier = Modifier.weight(1f).height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))
            ) {
                if (v.isGenerating) CircularProgressIndicator(Modifier.size(24.dp), color = Color.Black)
                else Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TableChart, null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("Excel/CSV", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Programación Automática
        Text("🕒 Programación Automática", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Card(Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f))) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enviar reporte todos los lunes a las 8 AM", color = Color.White, modifier = Modifier.weight(1f))
                    Switch(checked = v.scheduleEnabled, onCheckedChange = { v.toggleSchedule(it, context) })
                }
                if (v.scheduleEnabled) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = v.targetEmail,
                        onValueChange = { v.targetEmail = it },
                        label = { Text("Email de Gerencia") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Lista de Reportes Generados
        Text("📚 Historial de Reportes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Column(Modifier.padding(top = 8.dp)) {
            v.reportsList.forEach { r ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { v.openReport(context, r) },
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.1f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (r.endsWith(".pdf")) Icons.Default.PictureAsPdf else Icons.Default.TableChart,
                            contentDescription = null,
                            tint = if (r.endsWith(".pdf")) Color(0xFF2EF2A3) else Color(0xFFFFD54F)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(r, Modifier.weight(1f), color = Color.White, fontSize = 12.sp)
                        IconButton(onClick = { v.shareReport(context, r) }) { 
                            Icon(Icons.Default.Share, null, tint = Color.White.copy(0.7f)) 
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun ColumnRowSelection(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, colors = CheckboxDefaults.colors(checkmarkColor = Color.Black, checkedColor = Color(0xFF2EF2A3)))
        Text(text, color = Color.White)
    }
}

@Composable
fun DatabaseScreen(onProductClick: (String) -> Unit, onBackToAdminHome: () -> Unit) {
    var list by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        FirebaseFirestore.getInstance().collection("products").get()
            .addOnSuccessListener { result ->
                list = result.mapNotNull { doc ->
                    try {
                        Product(
                            name = doc.getString("name") ?: "",
                            price = doc.getDouble("price") ?: 0.0,
                            code = doc.getString("codigo") ?: "",
                            marca = doc.getString("marca") ?: "",
                            categoria = doc.getString("categoria") ?: "",
                            subcategoria = doc.getString("subcategoria") ?: "",
                            description = doc.getString("descripcion") ?: "",
                            quantity = doc.getLong("quantity")?.toInt() ?: 100
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF1E2A35)).padding(16.dp)) {
        Text("Base de Datos", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        
        if (isLoading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            LazyColumn(Modifier.weight(1f).padding(top = 16.dp)) {
                items(list) { p ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onProductClick(p.code) }) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(p.name, Modifier.weight(1f))
                            Icon(Icons.Default.Edit, null)
                        }
                    }
                }
            }
        }

        Button(onClick = onBackToAdminHome, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF59E689))) {
            Text("Volver")
        }
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
