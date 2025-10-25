plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.xiaomi.getapps.signature.asap"
    compileSdk = 34 // ✅ SDK 36 is still preview; use 34 for stability

    defaultConfig {
        applicationId = "com.xiaomi.getapps.signature.asap"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${project.findProperty("GEMINI_API_KEY") ?: System.getenv("GOOGLE_API_KEY") ?: System.getenv("GEMINI_API_KEY") ?: ""}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    // Core Android libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Activity KTX for lifecycle scope
    implementation("androidx.activity:activity-ktx:1.9.3")

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")

    // HTTP client and JSON parsing for API calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // ✅ stable version
    implementation("com.squareup.retrofit2:retrofit:2.11.0") // ✅ fixed version
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Image loading library
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Credentials Manager API
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")

    // Google ID Library
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Google Play Services Base + Tasks
    implementation("com.google.android.gms:play-services-base:18.5.0")
    implementation("com.google.android.gms:play-services-tasks:18.1.0")

    // Google GenAI SDK
    implementation("com.google.genai:google-genai:1.28.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
