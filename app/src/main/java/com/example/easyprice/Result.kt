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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyprice.data.HistoryManager
import com.example.easyprice.model.Product
import com.example.easyprice.ui.theme.EasyPriceTheme
import com.google.firebase.firestore.FirebaseFirestore

class Result : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val barcode = intent.getStringExtra("barcode")

        setContent {
            EasyPriceTheme {
                val context = LocalContext.current
                var product by remember { mutableStateOf<Product?>(null) }
                var productNotFound by remember { mutableStateOf(false) }
                var isEditing by remember { mutableStateOf(false) }

                if (barcode != null) {
                    LaunchedEffect(barcode) {
                        val db = FirebaseFirestore.getInstance()
                        db.collection("products").whereEqualTo("codigo", barcode).get()
                            .addOnSuccessListener { documents ->
                                if (documents.isEmpty) {
                                    productNotFound = true
                                } else {
                                    val doc = documents.documents[0]
                                    val newProduct = Product(
                                        name = doc.getString("name") ?: "",
                                        price = doc.getDouble("price") ?: 0.0,
                                        description = doc.getString("descripcion") ?: "",
                                        code = doc.getString("codigo") ?: "",
                                        marca = doc.getString("marca") ?: "",
                                        categoria = doc.getString("categoria") ?: ""
                                    )
                                    product = newProduct
                                    
                                    val existingIndex = HistoryManager.historyList.indexOfFirst { it.code == newProduct.code }
                                    if (existingIndex != -1) {
                                        HistoryManager.historyList[existingIndex] = newProduct
                                    } else {
                                        HistoryManager.historyList.add(newProduct)
                                    }
                                }
                            }
                            .addOnFailureListener { productNotFound = true }
                    }
                } else {
                    productNotFound = true
                }

                if (productNotFound) {
                    val intent = Intent(context, NotFound::class.java)
                    startActivity(intent)
                    finish()
                } else if (product != null) {
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
                            onCancel = { isEditing = false }
                        )
                    } else {
                        ResultScreen(
                            product = product!!,
                            onEditClick = { isEditing = true },
                            onBack = {
                                // Devolver el control a MainActivity asegurando que caiga en admin_home
                                (context as? Activity)?.finish()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResultScreen(
    product: Product,
    onEditClick: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E2A35))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Image(
            painter = painterResource(id = R.drawable.logo_easy_price),
            contentDescription = "Logo",
            modifier = Modifier.size(180.dp).padding(bottom = 16.dp),
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
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    "Resultado de Escaneo",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )

                ResultField("Código", product.code)
                ResultField("Nombre", product.name)
                ResultField("Marca", product.marca ?: "")
                ResultField("Precio", "$${product.price}")
                ResultField("Categoría", product.categoria ?: "")
                ResultField("Descripción", product.description)

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onEditClick,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(160.dp)
                        .height(55.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Editar", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(0.7f).height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF69F0AE)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Volver al Inicio", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EditProductScreen(
    product: Product,
    onSave: (Product) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var price by remember { mutableStateOf(product.price.toString()) }
    var marca by remember { mutableStateOf(product.marca ?: "") }
    var categoria by remember { mutableStateOf(product.categoria ?: "") }
    var description by remember { mutableStateOf(product.description) }

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
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Precio") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = marca, onValueChange = { marca = it }, label = { Text("Marca") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = categoria, onValueChange = { categoria = it }, label = { Text("Categoría") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { onSave(product.copy(name = name, price = price.toDoubleOrNull() ?: 0.0, marca = marca, categoria = categoria, description = description)) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Guardar Cambios", color = Color.Black, fontWeight = FontWeight.Bold)
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

@Composable
fun ResultField(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$label: ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        Text(value, fontSize = 16.sp, color = Color.DarkGray)
    }
}
