package com.xiaomi.getapps.signature.asap

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.xiaomi.getapps.signature.asap.databinding.MainBinding
import com.xiaomi.getapps.signature.asap.databinding.PopupCategorySelectionBinding
import com.xiaomi.getapps.signature.asap.databinding.PopupHowItWorksBinding
import com.xiaomi.getapps.signature.asap.databinding.PopupBrandDetailBinding
import com.xiaomi.getapps.signature.asap.databinding.PopupFeedbackBinding
import androidx.core.content.ContextCompat
import android.os.Handler
import android.os.Looper
import kotlin.random.Random
import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import android.util.Log
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import androidx.activity.result.contract.ActivityResultContracts
import android.view.ViewGroup
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainBinding
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var brandAdapter: BrandAdapter
    private lateinit var tutorialAdapter: TutorialAdapter
    private val allBrands = mutableListOf<Brand>() // Will be populated from database
    private var selectedCategory: Category? = null
    private var isShowingTutorials = false
    private var feedbackShown = false
    private val handler = Handler(Looper.getMainLooper())
    private val RC_GMAIL = 9101
    private lateinit var googleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient
    private val gmailSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val scope = Scope("https://www.googleapis.com/auth/gmail.send")
            if (!GoogleSignIn.hasPermissions(account, scope)) {
                GoogleSignIn.requestPermissions(this, RC_GMAIL, account, scope)
            } else {
                Toast.makeText(this, "Gmail connected", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
        }
    }
    private var emailComposeBinding: com.xiaomi.getapps.signature.asap.databinding.PopupEmailComposeBinding? = null
    private val emailAttachments = mutableListOf<Uri>()
    private val attachmentPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        try {
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                emailAttachments.add(uri)
                val binding = emailComposeBinding
                if (binding != null) {
                    val name = queryDisplayName(uri) ?: "Attachment"
                    val tv = TextView(this).apply {
                        text = name
                        setTextColor(android.graphics.Color.BLACK)
                        textSize = 14f
                        setPadding(0, 8, 0, 8)
                    }
                    binding.attachmentsContainer.addView(tv, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to add attachment", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Remove status bar and make fullscreen
        makeFullScreen()
        
        binding = MainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_oauth_client_id))
            .requestEmail()
            .requestProfile()
            .requestScopes(Scope("https://www.googleapis.com/auth/gmail.send"))
            .build()
        googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso)

        setupRecyclerViews()
        setupSearchFunctionality()
        setupClickListeners()
        
        // Load brands from database
        loadBrandsFromDatabase()
        
        // Show "How it Works" popup automatically when app opens
        showHowItWorksPopup()
        
        // Schedule random feedback popup
        scheduleRandomFeedbackPopup()
    }

    private fun makeFullScreen() {
        // Make the app truly fullscreen
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        // Hide both status bar and navigation bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        // Ensure the content goes edge to edge
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
    }

    private fun setupRecyclerViews() {
        // Setup categories (horizontal)
        categoryAdapter = CategoryAdapter(MainPageData.categories) { category ->
            handleCategorySelection(category)
        }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        // Setup brands (vertical) - initialize with empty list first
        brandAdapter = BrandAdapter(
            brands = mutableListOf(), // Start with empty list
            onEmailClick = { brand ->
                showBrandDetailPopup(brand)
            },
            onFavoriteClick = { brand ->
                // Update the filtered list if favorites category is selected
                if (selectedCategory?.name == "Favorites") {
                    filterBrands()
                }
            },
            onBrandLogoClick = { brand ->
                showInstagramPopup(brand)
            }
        )
        binding.rvBrands.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = brandAdapter
        }
        
        Log.d("MainActivity", "RecyclerViews setup complete")
        
        // Setup tutorials (vertical)
        tutorialAdapter = TutorialAdapter(
            tutorials = MainPageData.tutorials.toMutableList(),
            onTutorialClick = { tutorial ->
                // Tutorial click functionality - no toast message
            }
        )
        // Initially hidden, will be shown when Tutorials category is selected
    }

    private fun setupSearchFunctionality() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterBrands(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterBrands(query: String = binding.etSearch.text.toString()) {
        // Only filter brands if we're not showing tutorials
        if (isShowingTutorials) return
        
        // Only filter if we have brands loaded from database
        if (allBrands.isEmpty()) {
            Toast.makeText(this, "No brands loaded from database", Toast.LENGTH_SHORT).show()
            return
        }
        
        var filteredBrands = allBrands.toList() // Use database-loaded brands
        
        // First filter by category if "Favorites" is selected
        if (selectedCategory?.name == "Favorites") {
            filteredBrands = filteredBrands.filter { it.isFavorite }
        }
        
        // Then filter by search query if not empty
        if (query.isNotEmpty()) {
            filteredBrands = filteredBrands.filter { brand ->
                brand.name.contains(query, ignoreCase = true) ||
                brand.category.contains(query, ignoreCase = true)
            }
        }
        
        brandAdapter.updateBrands(filteredBrands)
    }

    private fun handleCategorySelection(category: Category) {
        selectedCategory = category
        
        // Update category selection state
        MainPageData.categories.forEach { cat ->
            cat.isSelected = cat.name == category.name
        }
        categoryAdapter.notifyDataSetChanged()
        
        // Handle different category types
        when (category.name) {
            "Tutorials" -> {
                showTutorials()
            }
            else -> {
                showBrands()
                filterBrands()
            }
        }
    }
    
    private fun showTutorials() {
        isShowingTutorials = true
        binding.rvBrands.adapter = tutorialAdapter
        tutorialAdapter.updateTutorials(MainPageData.tutorials)
    }
    
    private fun showBrands() {
        isShowingTutorials = false
        binding.rvBrands.adapter = brandAdapter
    }

    private fun setupClickListeners() {
        binding.ivHamburger.setOnClickListener {
            showCategoryPopup()
        }

        binding.ivLightning.setOnClickListener {
            showHowItWorksPopup()
        }

        binding.ivMailInbox.setOnClickListener {
            val intent = Intent(this, MailInboxActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun loadBrandsFromDatabase() {
        Log.d("MainActivity", "Starting to load brands from database")
        
        // Show loading state
        Toast.makeText(this, "Loading brands from database...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Calling SupabaseManager.fetchBrands()")
                val result = SupabaseManager.fetchBrands()
                
                // Switch to main thread for UI updates
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val brands = result.getOrNull() ?: emptyList()
                        Log.d("MainActivity", "Successfully fetched ${brands.size} brands from database")
                        
                        if (brands.isNotEmpty()) {
                            allBrands.clear()
                            allBrands.addAll(brands)
                            brandAdapter.updateBrands(allBrands)
                            Toast.makeText(this@MainActivity, "Successfully loaded ${brands.size} brands from database", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.w("MainActivity", "No brands found in database")
                            // Don't load fallback data - keep empty list
                            allBrands.clear()
                            brandAdapter.updateBrands(allBrands)
                            Toast.makeText(this@MainActivity, "No brands found in database", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                        Log.e("MainActivity", "Failed to fetch brands: $errorMsg")
                        // Don't load fallback data - show error instead
                        allBrands.clear()
                        brandAdapter.updateBrands(allBrands)
                        Toast.makeText(this@MainActivity, "Failed to load brands: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("MainActivity", "Exception loading brands: ${e.message}", e)
                    // Don't load fallback data - show error instead
                    allBrands.clear()
                    brandAdapter.updateBrands(allBrands)
                    Toast.makeText(this@MainActivity, "Error loading brands: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun showCategoryPopup() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val popupBinding = PopupCategorySelectionBinding.inflate(LayoutInflater.from(this))
        
        // Setup the popup categories adapter
        val popupAdapter = CategoryPopupAdapter(
            categories = MainPageData.popupCategories.toMutableList(),
            onCategoryClick = { category ->
                // Handle category selection
                // You can add filtering logic here if needed
                bottomSheetDialog.dismiss()
            }
        )
        
        popupBinding.rvPopupCategories.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = popupAdapter
        }
        
        // Setup logout button click listener
        popupBinding.btnLogout.setOnClickListener {
            bottomSheetDialog.dismiss()
            handleLogout()
        }
        
        bottomSheetDialog.setContentView(popupBinding.root)
        bottomSheetDialog.show()
    }
    
    private fun showHowItWorksPopup() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val howItWorksBinding = PopupHowItWorksBinding.inflate(LayoutInflater.from(this))
        
        // Images are now set directly in the layout file
        // CardView handles the rounded corners automatically
        
        // Handle close button click
        howItWorksBinding.btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        
        bottomSheetDialog.setContentView(howItWorksBinding.root)
        bottomSheetDialog.show()
    }
    
    private fun showBrandDetailPopup(brand: Brand) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val brandDetailBinding = PopupBrandDetailBinding.inflate(LayoutInflater.from(this))
        
        // Set brand name from database
        brandDetailBinding.tvBrandName.text = brand.name
        
        // Load brand logo from database URL using Glide
        if (brand.logoUrl.isNotEmpty()) {
            com.bumptech.glide.Glide.with(this)
                .load(brand.logoUrl)
                .placeholder(MainPageData.BRAND_DETAIL_IMAGE)
                .error(MainPageData.BRAND_DETAIL_IMAGE)
                .into(brandDetailBinding.ivBrandImage)
        } else {
            brandDetailBinding.ivBrandImage.setImageResource(MainPageData.BRAND_DETAIL_IMAGE)
        }
        
        // Handle Instagram button click - open Instagram profile from database
//        brandDetailBinding.btnInstagram.setOnClickListener {
//            openInstagram(brand.instagram)
//            bottomSheetDialog.dismiss()
//        }
        
            brandDetailBinding.btnEmail.setOnClickListener {
                bottomSheetDialog.dismiss()
                val intent = Intent(this, EmailComposeActivity::class.java)
                val cleanEmail = brand.email.replace("mailto:", "").trim()
                intent.putExtra("brand_email", cleanEmail)
                intent.putExtra("brand_name", brand.name)
                intent.putExtra("brand_logo_url", brand.logoUrl)
                intent.putExtra("brand_logo_res", brand.logoResource)
                startActivity(intent)
            }
        
        bottomSheetDialog.setContentView(brandDetailBinding.root)
        bottomSheetDialog.show()
    }

    private fun showEmailComposePopup(brand: Brand) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val composeBinding = com.xiaomi.getapps.signature.asap.databinding.PopupEmailComposeBinding.inflate(LayoutInflater.from(this))
        emailComposeBinding = composeBinding
        emailAttachments.clear()

        composeBinding.tvTitle.text = "Compose Email"
        val cleanEmail = brand.email.replace("mailto:", "").trim()
        composeBinding.etTo.setText(cleanEmail)
        composeBinding.etSubject.setText("Inquiry about ${brand.name}")
        composeBinding.etBody.setText("Hello ${brand.name} team,\n\nI am interested in learning more about your products.\n\nBest regards")

        composeBinding.btnAddAttachment.setOnClickListener {
            try {
                attachmentPicker.launch(arrayOf("*/*"))
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to open picker", Toast.LENGTH_SHORT).show()
            }
        }

        composeBinding.btnCancel.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        composeBinding.btnSend.setOnClickListener {
            val to = composeBinding.etTo.text?.toString()?.trim() ?: ""
            val subject = composeBinding.etSubject.text?.toString()?.trim() ?: ""
            val body = composeBinding.etBody.text?.toString() ?: ""

            if (to.isEmpty()) {
                Toast.makeText(this, "Recipient is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val account = GoogleSignIn.getLastSignedInAccount(this)
                val scope = Scope("https://www.googleapis.com/auth/gmail.send")
                if (account == null) {
                    gmailSignInLauncher.launch(googleSignInClient.signInIntent)
                    Toast.makeText(this, "Sign in to Google to send email", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (!GoogleSignIn.hasPermissions(account, scope)) {
                    GoogleSignIn.requestPermissions(this, RC_GMAIL, account, scope)
                    Toast.makeText(this, "Grant Gmail permission and tap Send again", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } catch (_: Exception) {
            }

            lifecycleScope.launch {
                val result = EmailService.sendEmail(
                    this@MainActivity,
                    to,
                    subject,
                    body,
                    emailAttachments,
                    brandName = brand.name,
                    logoUrl = brand.logoUrl,
                    logoRes = brand.logoResource
                )
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        Toast.makeText(this@MainActivity, "Email sent", Toast.LENGTH_SHORT).show()
                        bottomSheetDialog.dismiss()
                    } else {
                        val msg = result.exceptionOrNull()?.message ?: "Failed to send email"
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        bottomSheetDialog.setOnDismissListener {
            emailComposeBinding = null
        }

        bottomSheetDialog.setContentView(composeBinding.root)
        bottomSheetDialog.show()
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            val cursor = contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return it.getString(idx)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun showInstagramPopup(brand: Brand) {
        Log.d("MainActivity", "showInstagramPopup called for brand: ${brand.name}")
        Log.d("MainActivity", "Instagram URL: ${brand.instagram}")
        
        try {
            val bottomSheetDialog = BottomSheetDialog(this)
            val instagramBinding = com.xiaomi.getapps.signature.asap.databinding.PopupBrandInstagramBinding.inflate(LayoutInflater.from(this))
            
            // Set brand name
            instagramBinding.tvBrandName.text = "${brand.name} - Instagram"
            
            Log.d("MainActivity", "Instagram popup binding created successfully")
        
        // Configure WebView
        instagramBinding.webViewInstagram.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    instagramBinding.progressBar.visibility = View.VISIBLE
                }
                
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    instagramBinding.progressBar.visibility = View.GONE
                }
                
                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    instagramBinding.progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Failed to load Instagram page", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Load Instagram URL
        if (brand.instagram.isNotEmpty()) {
            val cleanUrl = brand.instagram.replace("mailto:", "").trim()
            instagramBinding.webViewInstagram.loadUrl(cleanUrl)
        } else {
            instagramBinding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Instagram profile not available", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Handle close button
        instagramBinding.btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        
            bottomSheetDialog.setContentView(instagramBinding.root)
            bottomSheetDialog.show()
            
            Log.d("MainActivity", "Instagram popup shown successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error showing Instagram popup: ${e.message}", e)
            Toast.makeText(this, "Error opening Instagram: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun openInstagram(instagramUrl: String) {
        try {
            if (instagramUrl.isNotEmpty()) {
                // Clean the URL and extract username
                val cleanUrl = instagramUrl.replace("mailto:", "").trim()
                
                // Try to open Instagram app first, then fallback to browser
                val intent = Intent(Intent.ACTION_VIEW)
                
                // Extract username from URL (e.g., https://instagram.com/username -> username)
                val username = cleanUrl.substringAfterLast("/")
                
                // Try Instagram app first
                intent.data = Uri.parse("http://instagram.com/_u/$username")
                intent.setPackage("com.instagram.android")
                
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to browser if Instagram app not installed
                    intent.setPackage(null)
                    intent.data = Uri.parse(cleanUrl)
                    startActivity(intent)
                }
            } else {
                Toast.makeText(this, "Instagram profile not available", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open Instagram", Toast.LENGTH_SHORT).show()
            Log.e("MainActivity", "Error opening Instagram: ${e.message}")
        }
    }
    
    private fun openEmailApp(emailAddress: String, brandName: String) {
        try {
            if (emailAddress.isNotEmpty()) {
                // Clean the email address - remove mailto: prefix if present
                val cleanEmail = emailAddress.replace("mailto:", "").trim()
                
                Log.d("MainActivity", "Opening email for: $cleanEmail")
                
                // Create email intent with ACTION_SEND instead of ACTION_SENDTO
                val emailIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "message/rfc822" // This ensures only email apps are shown
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(cleanEmail))
                    putExtra(Intent.EXTRA_SUBJECT, "Inquiry about $brandName")
                    putExtra(Intent.EXTRA_TEXT, "Hello $brandName team,\n\nI am interested in learning more about your products.\n\nBest regards")
                }
                
                // Create chooser to show available email apps
                val chooser = Intent.createChooser(emailIntent, "Send Email")
                
                // Check if email app is available
                if (chooser.resolveActivity(packageManager) != null) {
                    startActivity(chooser)
                } else {
                    // Fallback: try opening with browser/default handler
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:$cleanEmail"))
                    if (fallbackIntent.resolveActivity(packageManager) != null) {
                        startActivity(fallbackIntent)
                    } else {
                        Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Email address not available", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open email app: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("MainActivity", "Error opening email: ${e.message}")
        }
    }
    
    private fun scheduleRandomFeedbackPopup() {
        if (!feedbackShown) {
            // Show feedback popup randomly between 10-30 seconds
            val randomDelay = Random.nextLong(10000, 30000) // 10-30 seconds
            handler.postDelayed({
                showFeedbackPopup()
            }, randomDelay)
        }
    }
    
    private fun showFeedbackPopup() {
        if (feedbackShown) return
        
        feedbackShown = true
        val bottomSheetDialog = BottomSheetDialog(this)
        val feedbackBinding = PopupFeedbackBinding.inflate(LayoutInflater.from(this))
        
        // Set logo (easily changeable)
        feedbackBinding.ivLogo.setImageResource(MainPageData.FEEDBACK_LOGO)
        
        // Handle "Yes, I love it!" button click
        feedbackBinding.btnYesLoveIt.setOnClickListener {
            // Handle positive feedback (e.g., redirect to app store)
            bottomSheetDialog.dismiss()
        }
        
        // Handle "Not so much" button click
        feedbackBinding.btnNotSoMuch.setOnClickListener {
            // Handle negative feedback (e.g., show feedback form)
            bottomSheetDialog.dismiss()
        }
        
        bottomSheetDialog.setContentView(feedbackBinding.root)
        bottomSheetDialog.show()
    }
    
    private fun handleLogout() {
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                // Perform logout
                lifecycleScope.launch {
                    try {
                        val result = SupabaseManager.signOut()
                        if (result.isSuccess) {
                            // Navigate back to onboarding/login
                            val intent = android.content.Intent(this@MainActivity, OnboardingActivity::class.java)
                            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@MainActivity, "Logout failed", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
