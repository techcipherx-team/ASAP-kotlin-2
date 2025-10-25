package com.xiaomi.getapps.signature.asap

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class SplashActivity : AppCompatActivity() {

    private val splashTimeOut: Long = 3000 // 3 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make the activity fullscreen
        makeFullScreen()
        
        setContentView(R.layout.activity_splash)
        
        // Initialize SupabaseManager
        SupabaseManager.initialize(this)

        // Handler to start the appropriate activity after splash timeout
        Handler(Looper.getMainLooper()).postDelayed({
            // Check if user is already authenticated
            if (SupabaseManager.isUserSignedIn()) {
                // User is signed in, go directly to MainActivity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else {
                // User is not signed in, start OnboardingActivity
                val intent = Intent(this, OnboardingActivity::class.java)
                startActivity(intent)
            }
            
            // Close this activity
            finish()
        }, splashTimeOut)
    }

    private fun makeFullScreen() {
        // Hide the status bar and navigation bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        // Keep screen on during splash
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
