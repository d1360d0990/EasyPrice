package com.example.easyprice.ui.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
