package com.xiaomi.getapps.signature.asap

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.xiaomi.getapps.signature.asap.databinding.ActivityOnboardingBinding
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var onboardingAdapter: OnboardingAdapter
    private var currentPage = 0

    // Google Sign-In components
    private lateinit var credentialManager: CredentialManager
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val TAG = "OnboardingActivity"
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make the activity fullscreen
        makeFullScreen()

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleSignIn()
        setupOnboarding()
        setupClickListeners()
        updateButtonAppearance() // Set initial button appearance
    }

    private fun makeFullScreen() {
        // Hide the status bar and navigation bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Keep screen on during onboarding
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setupGoogleSignIn() {
        // Initialize SupabaseManager and CredentialManager
        SupabaseManager.initialize(this)
        credentialManager = CredentialManager.create(this)

        // Configure Google Sign-In
        val webClientId = try {
            googleServicesClientId() ?: getString(R.string.google_oauth_client_id)
        } catch (e: Exception) {
            getString(R.string.google_oauth_client_id)
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupOnboarding() {
        // Use centralized onboarding data
        onboardingAdapter = OnboardingAdapter(OnboardingData.screens)
        binding.viewPager.adapter = onboardingAdapter
    }

    private fun setupClickListeners() {
        binding.btnContinue.setOnClickListener {
            if (!OnboardingData.isLastScreen(currentPage)) {
                // Go to next onboarding screen
                binding.viewPager.currentItem = currentPage + 1
            } else {
                // On last screen, trigger Google sign-in
                signInWithGoogle()
            }
        }

        // Update button appearance based on current page
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPage = position
                updateButtonAppearance()
            }
        })
    }

    private fun updateButtonAppearance() {
        if (OnboardingData.isLastScreen(currentPage)) {
            // Google sign-in button: small Google image on left, "Continue with Google" text on right
            binding.btnContinue.text = "Continue with Google"
            binding.btnContinue.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_google, 0, 0, 0)
            binding.btnContinue.compoundDrawablePadding = 75
            binding.btnContinue.textSize = 18f
            binding.btnContinue.setTextColor(android.graphics.Color.parseColor("#000000"))
            binding.btnContinue.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            // Ensure rounded background is visible
            binding.btnContinue.setBackgroundResource(R.drawable.continue_button_background)
            binding.btnContinue.backgroundTintList = null
            binding.btnContinue.foreground = null
        } else {
            // Regular continue buttons: only "Continue" text
            binding.btnContinue.text = "Continue"
            binding.btnContinue.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            binding.btnContinue.compoundDrawablePadding = 0
            binding.btnContinue.textSize = 18f
            binding.btnContinue.setTextColor(android.graphics.Color.parseColor("#000000"))
            binding.btnContinue.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            // Ensure rounded background is visible
            binding.btnContinue.setBackgroundResource(R.drawable.continue_button_background)
            binding.btnContinue.backgroundTintList = null
            binding.btnContinue.foreground = null
        }
    }

    // ------------------ GOOGLE SIGN-IN HANDLING ------------------

    private fun signInWithGoogle() {
        Log.d(TAG, "Starting Google Sign-In process")
        showLoading(true)

        // Clear any existing sign-in state
        googleSignInClient.signOut().addOnCompleteListener {
            Log.d(TAG, "Cleared existing Google Sign-In state")
            lifecycleScope.launch {
                try {
                    Log.d(TAG, "Attempting Credential Manager Sign-In")
                    tryCredentialManagerSignIn()
                } catch (e: Exception) {
                    Log.w(TAG, "Credential Manager failed, trying legacy Google Sign-In: ${e.message}", e)
                    tryLegacyGoogleSignIn()
                }
            }
        }
    }

    private suspend fun tryCredentialManagerSignIn() {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(getString(R.string.google_oauth_client_id))
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            Log.d(TAG, "Starting Credential Manager Google Sign-In")
            val result = credentialManager.getCredential(
                request = request,
                context = this
            )

            Log.d(TAG, "Received credential type: ${result.credential::class.java.simpleName}")

            when (val credential = result.credential) {
                is GoogleIdTokenCredential -> {
                    Log.d(TAG, "Received Google ID token credential")
                    handleGoogleSignInSuccess(credential.idToken)
                }
                else -> {
                    // Try to extract ID token from other credential types
                    try {
                        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        Log.d(TAG, "Successfully parsed Google credential from generic credential")
                        handleGoogleSignInSuccess(googleCredential.idToken)
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Failed to parse Google credential: ${e.message}", e)
                        Log.e(TAG, "Unexpected credential type: ${credential::class.java.simpleName}")
                        Log.d(TAG, "Falling back to legacy Google Sign-In")
                        tryLegacyGoogleSignIn()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing credential: ${e.message}", e)
                        tryLegacyGoogleSignIn()
                    }
                }
            }

        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager failed: ${e.type} - ${e.message}", e)
            when (e.type) {
                "android.credentials.GetCredentialException.TYPE_NO_CREDENTIAL" -> {
                    Log.d(TAG, "No credentials available, trying legacy Google Sign-In")
                    tryLegacyGoogleSignIn()
                }
                "android.credentials.GetCredentialException.TYPE_USER_CANCELED" -> {
                    Log.d(TAG, "User cancelled credential selection")
                    showToast("Google sign-in cancelled")
                    showLoading(false)
                }
                else -> {
                    Log.d(TAG, "Credential Manager error, falling back to legacy Google Sign-In")
                    tryLegacyGoogleSignIn()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in Credential Manager: ${e.message}", e)
            Log.d(TAG, "Falling back to legacy Google Sign-In due to unexpected error")
            tryLegacyGoogleSignIn()
        }
    }

    private fun tryLegacyGoogleSignIn() {
        Log.d(TAG, "Starting legacy Google Sign-In")
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    @Deprecated("Use registerForActivityResult instead for new code.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleLegacySignInResult(task)
        }
    }

    private fun handleLegacySignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val idToken = account.idToken

            if (idToken != null) {
                Log.d(TAG, "Legacy Google Sign-In successful, got ID token")
                lifecycleScope.launch {
                    handleGoogleSignInSuccess(idToken)
                }
            } else {
                Log.e(TAG, "ID token is null")
                showToast("Failed to get authentication token")
                showLoading(false)
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Legacy Google Sign-In failed: ${e.statusCode} - ${e.message}", e)
            when (e.statusCode) {
                12501 -> showToast("Google sign-in cancelled")
                7 -> showToast("Network error. Please check your connection.")
                else -> showToast("Google sign-in failed: ${e.message}")
            }
            showLoading(false)
        }
    }

    private suspend fun handleGoogleSignInSuccess(idToken: String) {
        try {
            Log.d(TAG, "Authenticating with Supabase...")
            val clientId = getString(R.string.google_oauth_client_id)
            val result = SupabaseManager.signInWithGoogle(idToken, clientId)

            if (result.isSuccess) {
                showToast("Google sign-in successful!")
                kotlinx.coroutines.delay(500)
                navigateToMainActivity()
            } else {
                val error = result.exceptionOrNull()
                showToast("Authentication failed: ${error?.message ?: "Unknown error"}")
                showLoading(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase authentication error", e)
            showToast("Authentication error: ${e.message}")
            showLoading(false)
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        // You can add a progress bar to the layout if needed
        binding.btnContinue.isEnabled = !isLoading
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun googleServicesClientId(): String? {
        return try {
            val jsonString = assets.open("google-services.json").bufferedReader().use { it.readText() }
            val jsonObject = org.json.JSONObject(jsonString)
            val clients = jsonObject.getJSONArray("client")

            if (clients.length() > 0) {
                val client = clients.getJSONObject(0)
                val oauthClients = client.getJSONArray("oauth_client")

                // Find the web client (type 3)
                for (i in 0 until oauthClients.length()) {
                    val oauthClient = oauthClients.getJSONObject(i)
                    if (oauthClient.getInt("client_type") == 3) {
                        return oauthClient.getString("client_id")
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error reading google-services.json: ${e.message}")
            null
        }
    }

}
