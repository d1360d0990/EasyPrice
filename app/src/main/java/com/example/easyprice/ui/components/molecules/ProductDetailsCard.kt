package com.example.easyprice.ui.components.molecules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.easyprice.ui.components.atoms.ResultField

@Composable
fun ProductDetailsCard(product: Product) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = product.name,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = product.marca ?: "",
                fontSize = 18.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            HorizontalDivider(color = Color.LightGray, thickness = 1.dp)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "$${product.price}",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFF4C430)
            )

            Spacer(modifier = Modifier.height(24.dp))

            ResultField("Categoría", product.categoria ?: "General")
            ResultField("Descripción", product.description)
        }
    }
}
