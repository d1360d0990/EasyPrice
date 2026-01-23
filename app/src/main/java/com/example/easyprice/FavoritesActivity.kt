package com.example.easyprice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.easyprice.ui.screens.FavoritesScreen // Se importa la nueva pantalla
import com.example.easyprice.ui.theme.EasyPriceTheme

class FavoritesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EasyPriceTheme {
                // La Activity ahora solo llama a la UI y le pasa la acción de "onBack".
                FavoritesScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}