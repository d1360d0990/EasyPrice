package com.example.easyprice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyprice.data.HistoryManager
import com.example.easyprice.model.Product
import com.example.easyprice.ui.theme.EasyPriceTheme
import com.example.easyprice.ui.components.organisms.AdminResultContent
import com.example.easyprice.ui.components.pages.PantallaScanConsumer
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

class Result : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val barcode = intent.getStringExtra("barcode")
        val startInEditMode = intent.getBooleanExtra("edit_mode", false)
        val isConsumer = intent.getBooleanExtra("is_consumer", false)

        setContent {
            EasyPriceTheme {
                val context = LocalContext.current
                var product by remember { mutableStateOf<Product?>(null) }
                var productNotFound by remember { mutableStateOf(false) }
                var isEditing by remember { mutableStateOf(startInEditMode) }

                LaunchedEffect(barcode) {
                    if (barcode != null) {
                        val db = FirebaseFirestore.getInstance()
                        db.collection("products").whereEqualTo("codigo", barcode).get()
                            .addOnSuccessListener { documents ->
                                if (documents.isEmpty) {
                                    productNotFound = true
                                } else {
                                    val doc = documents.documents[0]
                                    val productId = doc.id
                                    val productName = doc.getString("name") ?: ""
                                    
                                    val newProduct = Product(
                                        name = productName,
                                        price = doc.getDouble("price") ?: 0.0,
                                        description = doc.getString("descripcion") ?: "",
                                        code = doc.getString("codigo") ?: "",
                                        marca = doc.getString("marca") ?: "",
                                        categoria = doc.getString("categoria") ?: "",
                                        subcategoria = doc.getString("subcategoria") ?: ""
                                    )
                                    product = newProduct
                                    
                                    val existingIndex = HistoryManager.historyList.indexOfFirst { it.code == newProduct.code }
                                    if (existingIndex != -1) {
                                        HistoryManager.historyList[existingIndex] = newProduct
                                    } else {
                                        HistoryManager.historyList.add(newProduct)
                                    }

                                    db.collection("stats").document("global")
                                        .set(mapOf("total_scans" to FieldValue.increment(1)), SetOptions.merge())

                                    db.collection("product_stats").document(productId).get()
                                        .addOnSuccessListener { statDoc ->
                                            val statsData = mutableMapOf<String, Any>(
                                                "name" to productName,
                                                "scan_count" to FieldValue.increment(1)
                                            )
                                            if (!statDoc.exists()) {
                                                statsData["last_week_count"] = 0
                                            }
                                            
                                            db.collection("product_stats")
                                                .document(productId)
                                                .set(statsData, SetOptions.merge())
                                        }

                                    val dateId = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                    db.collection("scans_by_day").document(dateId)
                                        .set(mapOf("count" to FieldValue.increment(1)), SetOptions.merge())
                                }
                            }
                            .addOnFailureListener { 
                                productNotFound = true 
                            }
                    } else {
                        productNotFound = true
                    }
                }

                LaunchedEffect(productNotFound) {
                    if (productNotFound) {
                        val intent = Intent(context, NotFound::class.java)
                        context.startActivity(intent)
                        (context as? Activity)?.finish()
                    }
                }

                if (product != null) {
                    if (isEditing) {
                        EditProductScreen(
                            product = product!!,
                            onSave = { updatedProduct ->
                                val db = FirebaseFirestore.getInstance()
                                db.collection("products").whereEqualTo("codigo", updatedProduct.code).get()
                                    .addOnSuccessListener { docs ->
                                        if (!docs.isEmpty) {
                                            val docId = docs.documents[0].id
                                            val updateData = hashMapOf(
                                                "name" to updatedProduct.name,
                                                "price" to updatedProduct.price,
                                                "marca" to updatedProduct.marca,
                                                "categoria" to updatedProduct.categoria,
                                                "subcategoria" to updatedProduct.subcategoria,
                                                "descripcion" to updatedProduct.description
                                            )
                                            db.collection("products").document(docId).update(updateData as Map<String, Any>)
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, "Cambios guardados", Toast.LENGTH_SHORT).show()
                                                    product = updatedProduct
                                                    isEditing = false
                                                }
                                        }
                                    }
                            },
                            onDelete = {
                                val db = FirebaseFirestore.getInstance()
                                db.collection("products").whereEqualTo("codigo", product!!.code).get()
                                    .addOnSuccessListener { docs ->
                                        if (!docs.isEmpty) {
                                            val docId = docs.documents[0].id
                                            db.collection("products").document(docId).delete()
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, "Producto eliminado", Toast.LENGTH_SHORT).show()
                                                    val intent = Intent(context, MainActivity::class.java).apply {
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                        putExtra("target_screen", "admin_home")
                                                    }
                                                    context.startActivity(intent)
                                                    (context as? Activity)?.finish()
                                                }
                                        }
                                    }
                            },
                            onCancel = { 
                                if (startInEditMode) {
                                    (context as? Activity)?.finish()
                                } else {
                                    isEditing = false 
                                }
                            }
                        )
                    } else if (isConsumer) {
                        PantallaScanConsumer(
                            product = product!!,
                            onHistoryClick = {
                                context.startActivity(Intent(context, HistoryActivity::class.java))
                            },
                            onFavoritesClick = {
                                context.startActivity(Intent(context, FavoritesActivity::class.java))
                            },
                            onBack = { (context as? Activity)?.finish() }
                        )
                    } else {
                        AdminResultContent(
                            product = product!!,
                            onEditClick = { isEditing = true },
                            onDeleteClick = {
                                val db = FirebaseFirestore.getInstance()
                                db.collection("products").whereEqualTo("codigo", product!!.code).get()
                                    .addOnSuccessListener { docs ->
                                        if (!docs.isEmpty) {
                                            val docId = docs.documents[0].id
                                            db.collection("products").document(docId).delete()
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, "Producto eliminado", Toast.LENGTH_SHORT).show()
                                                    val intent = Intent(context, MainActivity::class.java).apply {
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                        putExtra("target_screen", "admin_home")
                                                    }
                                                    context.startActivity(intent)
                                                    (context as? Activity)?.finish()
                                                }
                                        }
                                    }
                            },
                            onBack = { (context as? Activity)?.finish() }
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E2A35)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00E5FF))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    product: Product,
    onSave: (Product) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var price by remember { mutableStateOf(product.price.toString()) }
    var marca by remember { mutableStateOf(product.marca ?: "") }
    var categoria by remember { mutableStateOf(product.categoria ?: "") }
    var subcategoria by remember { mutableStateOf(product.subcategoria ?: "") }
    var description by remember { mutableStateOf(product.description) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E2A35))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Image(painter = painterResource(id = R.drawable.logo_easy_price), contentDescription = "Logo", modifier = Modifier.size(150.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Editar Producto", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Precio") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                OutlinedTextField(value = marca, onValueChange = { marca = it }, label = { Text("Marca") }, modifier = Modifier.fillMaxWidth())
                
                Text("Categoría:", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = categoria,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        categoriesMap.keys.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    categoria = selectionOption
                                    subcategoria = "" 
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }

                Text("Sub-categoría:", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedSubCategory,
                    onExpandedChange = { if (categoria.isNotEmpty()) expandedSubCategory = !expandedSubCategory },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = subcategoria,
                        onValueChange = {},
                        readOnly = true,
                        enabled = categoria.isNotEmpty(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubCategory) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    if (categoria.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expandedSubCategory,
                            onDismissRequest = { expandedSubCategory = false }
                        ) {
                            categoriesMap[categoria]?.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        subcategoria = selectionOption
                                        expandedSubCategory = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { onSave(product.copy(name = name, price = price.toDoubleOrNull() ?: 0.0, marca = marca, categoria = categoria, subcategoria = subcategoria, description = description)) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Guardar Cambios", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Eliminar Producto", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                ) {
                    Text("Cancelar", color = Color.Black)
                }
            }
        }
    }
}
