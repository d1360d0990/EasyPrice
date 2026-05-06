package com.example.easyprice

import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val imgLogo = findViewById<ImageView>(R.id.imgLogoSplash)

        // Animación de Zoom y Fade In
        imgLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(1500) // Duración de 1.5 segundos
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                // Pequeña pausa al final antes de navegar
                imgLogo.postDelayed({
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                }, 500)
            }
            .start()
    }
}
