package com.xiaomi.getapps.signature.asap

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.genai.Client
import com.google.genai.types.GenerateContentResponse
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.xiaomi.getapps.signature.asap.databinding.ActivityEmailComposeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmailComposeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmailComposeBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val rcGmail = 9201

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailComposeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val brandName = intent.getStringExtra("brand_name") ?: ""
        val brandEmail = intent.getStringExtra("brand_email") ?: ""
        val brandLogoUrl = intent.getStringExtra("brand_logo_url") ?: ""
        val brandLogoRes = intent.getIntExtra("brand_logo_res", 0)
        binding.toolbar.title = "Compose"
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.tvTitle.text = brandName
        binding.etTo.setText(brandEmail)
        binding.etSubject.setText("Inquiry about $brandName")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_oauth_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val templates = listOf(
            "Hello $brandName team, I love your products.",
            "Hi, seeking collaboration opportunities with $brandName.",
            "Hello, requesting more information about latest releases.")
        templates.forEach { t ->
            val chip = com.google.android.material.chip.Chip(this)
            chip.text = t
            chip.isCheckable = true
            chip.setOnClickListener {
                binding.etBody.setText(t)
            }
            binding.chipTemplates.addView(chip)
        }

        binding.btnAi.setOnClickListener {
            val subject = binding.etSubject.text?.toString()?.trim() ?: ""
            val body = binding.etBody.text?.toString() ?: ""
            val brand = brandName
            if (body.isEmpty()) {
                Toast.makeText(this, "Write a message first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val apiKey = resolveApiKey()
            if (apiKey.isNullOrEmpty()) {
                promptForApiKey { entered ->
                    if (!entered.isNullOrBlank()) {
                        saveApiKey(entered)
                        runAiEnhance(subject, body, brand, entered)
                    }
                }
                return@setOnClickListener
            }
            runAiEnhance(subject, body, brand, apiKey)
        }

        binding.btnSend.setOnClickListener {
            val to = binding.etTo.text?.toString()?.trim() ?: ""
            val subject = binding.etSubject.text?.toString()?.trim() ?: ""
            val body = binding.etBody.text?.toString() ?: ""
            if (to.isEmpty()) {
                Toast.makeText(this, "Recipient is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                val account = GoogleSignIn.getLastSignedInAccount(this)
                val scope = Scope("https://www.googleapis.com/auth/gmail.send")
                if (account == null) {
                    startActivityForResult(googleSignInClient.signInIntent, rcGmail)
                    Toast.makeText(this, "Sign in to Google and try again", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (!GoogleSignIn.hasPermissions(account, scope)) {
                    GoogleSignIn.requestPermissions(this, rcGmail, account, scope)
                    Toast.makeText(this, "Grant Gmail permission and try again", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } catch (_: Exception) {}

            lifecycleScope.launch {
                val logoPair = if (brandLogoUrl.isNotBlank() || brandLogoRes != 0) (brandLogoUrl to brandLogoRes) else resolveBrandLogo(brandName)
                val result = EmailService.sendEmail(
                    this@EmailComposeActivity,
                    to,
                    subject,
                    body,
                    emptyList(),
                    brandName = brandName,
                    logoUrl = logoPair.first,
                    logoRes = logoPair.second
                )
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        Toast.makeText(this@EmailComposeActivity, "Email sent", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        val msg = result.exceptionOrNull()?.message ?: "Failed to send email"
                        Toast.makeText(this@EmailComposeActivity, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun runAiEnhance(subject: String, body: String, brand: String, apiKey: String) {
        binding.btnAi.isEnabled = false
        binding.progressAi.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
                var improved: String? = null
                var error: String? = null
                try {
                    val client = Client.builder().apiKey(apiKey).build()
                    val prompt = "Rewrite the following email to be clear, professional, friendly and concise. Keep it ready to send to $brand. Do not add greetings if already present. Return plain text only.\nSubject: $subject\nBody:\n$body"
                    val response: GenerateContentResponse = client.models.generateContent("gemini-2.5-flash", prompt, null)
                    improved = response.text()
                } catch (e: Exception) {
                    error = e.message
                }
                withContext(Dispatchers.Main) {
                    binding.btnAi.isEnabled = true
                    binding.progressAi.visibility = View.GONE
                    if (!improved.isNullOrBlank()) {
                        binding.etBody.setText(improved)
                        Toast.makeText(this@EmailComposeActivity, "Text enhanced", Toast.LENGTH_SHORT).show()
                    } else {
                        val msg = error ?: "AI enhancement failed"
                        Toast.makeText(this@EmailComposeActivity, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun resolveApiKey(): String? {
        val fromBuild = BuildConfig.GEMINI_API_KEY
        if (!fromBuild.isNullOrEmpty()) return fromBuild
        val prefs = getSharedPreferences("asap_prefs", MODE_PRIVATE)
        return prefs.getString("gemini_api_key", null)
    }

    private fun saveApiKey(value: String) {
        val prefs = getSharedPreferences("asap_prefs", MODE_PRIVATE)
        prefs.edit().putString("gemini_api_key", value).apply()
    }

    private fun promptForApiKey(onEntered: (String?) -> Unit) {
        val input = com.google.android.material.textfield.TextInputEditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Enter Google API key"
        val layout = com.google.android.material.textfield.TextInputLayout(this)
        layout.addView(input)
        AlertDialog.Builder(this)
            .setTitle("API Key Required")
            .setView(layout)
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .setPositiveButton("Save") { d, _ ->
                val text = input.text?.toString()?.trim()
                d.dismiss()
                onEntered(text)
            }
            .show()
    }

    private fun resolveBrandLogo(name: String): Pair<String, Int> {
        return try {
            val b = MainPageData.brands.firstOrNull { it.name.equals(name, ignoreCase = true) }
            if (b != null) {
                (b.logoUrl to b.logoResource)
            } else {
                ("" to 0)
            }
        } catch (_: Exception) { "" to 0 }
    }

    @Deprecated("Use registerForActivityResult for new code")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == rcGmail) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                task.getResult(ApiException::class.java)
            } catch (_: Exception) {}
        }
    }
}
