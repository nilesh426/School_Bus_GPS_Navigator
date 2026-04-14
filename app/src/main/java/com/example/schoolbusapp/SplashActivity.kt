package com.example.schoolbusapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val splashDelay: Long = 2200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val ivSplashLogo = findViewById<ImageView>(R.id.ivSplashLogo)
        val tvSplashTitle = findViewById<TextView>(R.id.tvSplashTitle)
        val tvSplashSubtitle = findViewById<TextView>(R.id.tvSplashSubtitle)

        ivSplashLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(900)
            .start()

        tvSplashTitle.animate()
            .alpha(1f)
            .setStartDelay(350)
            .setDuration(700)
            .start()

        tvSplashSubtitle.animate()
            .alpha(1f)
            .setStartDelay(650)
            .setDuration(700)
            .start()

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }, splashDelay)
    }
}