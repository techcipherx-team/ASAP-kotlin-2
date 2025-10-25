package com.xiaomi.getapps.signature.asap

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray

// You should have a data class Brand defined somewhere in your project:
// data class Brand(...)

object SupabaseManager {

    private const val SUPABASE_URL = "https://ffdvhjxvkxouwxcqjbzr.supabase.co"
    private const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZmZHZoanh2a3hvdXd4Y3FqYnpyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzI3MTAxOTEsImV4cCI6MjA0ODI4NjE5MX0.9G9vABtzioKGyd5OhX1CjE5uGtcfXvXWzFssXSpHSP0"

    private const val PREFS_NAME = "supabase_auth"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USER_EMAIL = "user_email"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private lateinit var sharedPrefs: SharedPreferences

    fun initialize(context: Context) {
        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 🔹 Sign Up
    suspend fun signUp(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
            }

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$SUPABASE_URL/auth/v1/signup")
                .post(requestBody)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            Log.d("SupabaseManager", "Signup response: ${response.code}, body: ${responseBody?.take(200)}")

            if (response.isSuccessful) {
                if (responseBody != null) {
                    val responseJson = JSONObject(responseBody)
                    if (responseJson.has("access_token")) {
                        saveUserSession(responseJson.getString("access_token"), email)
                        Result.success(Unit)
                    } else {
                        // User created but email verification required
                        Result.success(Unit)
                    }
                } else {
                    Result.success(Unit)
                }
            } else {
                val errorMsg = try {
                    val errorJson = JSONObject(responseBody ?: "{}")
                    errorJson.optString("error_description",
                        errorJson.optString("message", response.message))
                } catch (_: Exception) { response.message }
                Log.e("SupabaseManager", "Signup failed: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Signup exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    // 🔹 Sign In
    suspend fun signIn(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
            }

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$SUPABASE_URL/auth/v1/token?grant_type=password")
                .post(requestBody)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            Log.d("SupabaseManager", "Signin response: ${response.code}, body: ${responseBody?.take(200)}")

            if (response.isSuccessful) {
                if (responseBody != null) {
                    val responseJson = JSONObject(responseBody)
                    if (responseJson.has("access_token")) {
                        saveUserSession(responseJson.getString("access_token"), email)
                        Result.success(Unit)
                    } else {
                        Log.e("SupabaseManager", "No access token in response")
                        Result.failure(Exception("Invalid credentials"))
                    }
                } else {
                    Log.e("SupabaseManager", "Empty response body")
                    Result.failure(Exception("Invalid credentials"))
                }
            } else {
                val errorMsg = try {
                    val errorJson = JSONObject(responseBody ?: "{}")
                    errorJson.optString("error_description",
                        errorJson.optString("message", response.message))
                } catch (_: Exception) { response.message }
                Log.e("SupabaseManager", "Signin failed: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Signin exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    // 🔹 Sign Out
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val accessToken = getAccessToken()
            clearUserSession()

            if (accessToken != null) {
                val request = Request.Builder()
                    .url("$SUPABASE_URL/auth/v1/logout")
                    .post("".toRequestBody())
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .build()

                client.newCall(request).execute()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            clearUserSession()
            Result.success(Unit)
        }
    }

    // 🔹 Google Sign-In (ID token exchange)
    suspend fun signInWithGoogle(idToken: String, clientId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("SupabaseManager", "Attempting Google sign-in with Supabase using id_token")

            val json = JSONObject().apply {
                put("provider", "google")
                put("id_token", idToken)
                put("client_id", clientId)
            }

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$SUPABASE_URL/auth/v1/token?grant_type=id_token")
                .post(requestBody)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d("SupabaseManager", "Response code: ${response.code}, body: ${responseBody?.take(200)}")

            if (response.isSuccessful && responseBody != null) {
                val responseJson = JSONObject(responseBody)
                val accessToken = responseJson.optString("access_token", null)
                if (accessToken != null) {
                    val email = responseJson.optJSONObject("user")?.optString("email", "") ?: ""
                    Log.d("SupabaseManager", "Successfully authenticated with Google")
                    saveUserSession(accessToken, email)
                    Result.success(Unit)
                } else {
                    Log.e("SupabaseManager", "No access token in response: $responseBody")
                    Result.failure(Exception("Google sign-in failed: No access token received"))
                }
            } else {
                val errorBody = responseBody ?: "null"
                Log.e("SupabaseManager", "Google sign-in failed: ${response.code} - $errorBody")
                val message = try {
                    val errorJson = JSONObject(errorBody)
                    errorJson.optString("error_description", errorJson.optString("message", response.message))
                } catch (_: Exception) { response.message }
                Result.failure(Exception("Google sign-in failed: $message"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Google sign-in exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    // 🔹 Session Helpers
    fun isUserSignedIn(): Boolean = ::sharedPrefs.isInitialized && getAccessToken() != null
    fun getCurrentUserEmail(): String? = if (::sharedPrefs.isInitialized) sharedPrefs.getString(KEY_USER_EMAIL, null) else null

    private fun saveUserSession(accessToken: String, email: String) {
        if (::sharedPrefs.isInitialized) {
            sharedPrefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_USER_EMAIL, email)
                .apply()
        }
    }

    private fun clearUserSession() {
        if (::sharedPrefs.isInitialized) {
            sharedPrefs.edit().clear().apply()
        }
    }

    private fun getAccessToken(): String? =
        if (::sharedPrefs.isInitialized) sharedPrefs.getString(KEY_ACCESS_TOKEN, null) else null

    // 🔹 Fetch All Brands
    suspend fun fetchBrands(): Result<List<Brand>> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/Live%20Brands?select=*"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonArray = JSONArray(responseBody)
                val brands = mutableListOf<Brand>()
                for (i in 0 until jsonArray.length()) {
                    val brandJson = jsonArray.getJSONObject(i)
                    brands.add(
                        Brand(
                            id = i + 1,
                            name = brandJson.optString("brand", ""),
                            email = brandJson.optString("email", ""),
                            instagram = brandJson.optString("instagram", ""),
                            website = brandJson.optString("website", ""),
                            category = brandJson.optString("category", "Most Popular"),
                            logoUrl = brandJson.optString("logo_url", ""),
                            isEnabled = brandJson.optBoolean("is_enabled", true),
                            isPopular = brandJson.optBoolean("is_popular", false),
                            placeNumber = brandJson.optInt("place_number", 0),
                            isFavorite = false
                        )
                    )
                }
                Result.success(brands)
            } else {
                Result.failure(Exception("Failed to fetch brands: ${response.message}"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseManager", "Error fetching brands: ${e.message}", e)
            Result.failure(e)
        }
    }

    // 🔹 Fetch Brands by Category
    suspend fun fetchBrandsByCategory(category: String): Result<List<Brand>> = withContext(Dispatchers.IO) {
        try {
            val encodedCategory = java.net.URLEncoder.encode(category, "UTF-8")
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/Live%20Brands?select=*&category=eq.$encodedCategory")
                .get()
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonArray = JSONArray(responseBody)
                val brands = mutableListOf<Brand>()
                for (i in 0 until jsonArray.length()) {
                    val brandJson = jsonArray.getJSONObject(i)
                    brands.add(
                        Brand(
                            id = brandJson.optInt("id", 0),
                            name = brandJson.optString("brand", ""),
                            email = brandJson.optString("email", ""),
                            instagram = brandJson.optString("instagram", ""),
                            website = brandJson.optString("website", ""),
                            category = brandJson.optString("category", "Most Popular"),
                            logoUrl = brandJson.optString("logo_url", ""),
                            isEnabled = brandJson.optBoolean("is_enabled", true),
                            isPopular = brandJson.optBoolean("is_popular", false),
                            placeNumber = brandJson.optInt("place_number", 0),
                            isFavorite = false
                        )
                    )
                }
                Result.success(brands)
            } else {
                Result.failure(Exception("Failed to fetch brands by category: ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 🔹 Fetch Favorite Brands
    suspend fun fetchFavoriteBrands(userId: String): Result<List<Brand>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/Favourites?select=*,Live%20Brands(*)&user_id=eq.$userId")
                .get()
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonArray = JSONArray(responseBody)
                val brands = mutableListOf<Brand>()

                for (i in 0 until jsonArray.length()) {
                    val favoriteJson = jsonArray.getJSONObject(i)
                    if (favoriteJson.has("Live Brands")) {
                        val brandJson = favoriteJson.getJSONObject("Live Brands")
                        brands.add(
                            Brand(
                                id = brandJson.optInt("id", 0),
                                name = brandJson.optString("brand", ""),
                                email = brandJson.optString("email", ""),
                                instagram = brandJson.optString("instagram", ""),
                                website = brandJson.optString("website", ""),
                                category = brandJson.optString("category", "Most Popular"),
                                logoUrl = brandJson.optString("logo_url", ""),
                                isEnabled = brandJson.optBoolean("is_enabled", true),
                                isPopular = brandJson.optBoolean("is_popular", false),
                                placeNumber = brandJson.optInt("place_number", 0),
                                isFavorite = true
                            )
                        )
                    }
                }

                Result.success(brands)
            } else {
                Result.failure(Exception("Failed to fetch favorite brands: ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
