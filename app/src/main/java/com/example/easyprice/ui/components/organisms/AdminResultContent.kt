package com.example.easyprice.ui.components.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easyprice.model.Product
import com.example.easyprice.ui.components.atoms.LogoImage
import com.example.easyprice.ui.components.atoms.ResultField

@Composable
fun AdminResultContent(
    product: Product,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
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

        LogoImage()

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
                ResultField("Sub-categoría", product.subcategoria ?: "")
                ResultField("Descripción", product.description)

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onEditClick,
                        modifier = Modifier.weight(1f).height(55.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Editar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }

                    Button(
                        onClick = onDeleteClick,
                        modifier = Modifier.weight(1f).height(55.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
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
