package com.example.easyprice.ui.components.atoms

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.easyprice.R

@Composable
fun LogoImage(size: Dp = 180.dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.logo_easy_price),
        contentDescription = "Logo Easy Price",
        modifier = modifier.size(size).padding(bottom = 16.dp),
        contentScale = ContentScale.Fit
    )
}
