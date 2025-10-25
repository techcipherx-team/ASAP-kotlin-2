package com.xiaomi.getapps.signature.asap

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class TestDatabaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val textView = TextView(this).apply {
            text = "Testing database connection..."
            textSize = 16f
            setPadding(20, 20, 20, 20)
        }
        setContentView(textView)
        
        lifecycleScope.launch {
            try {
                val result = SupabaseManager.fetchBrands()
                if (result.isSuccess) {
                    val brands = result.getOrNull() ?: emptyList()
                    runOnUiThread {
                        val brandsList = brands.take(10).joinToString("\n") { 
                            "${it.name} - ${it.category}"
                        }
                        textView.text = "Found ${brands.size} brands:\n\n$brandsList\n\n...and more"
                    }
                } else {
                    runOnUiThread {
                        textView.text = "Failed: ${result.exceptionOrNull()?.message}"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    textView.text = "Error: ${e.message}\n\n${e.stackTraceToString()}"
                }
            }
        }
    }
}
