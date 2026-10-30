import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    // Requiere app/google-services.json (descargado desde la consola de Firebase)
    alias(libs.plugins.google.services)
}

// Base URL of the Supabase Edge Functions. It lives in local.properties (never versioned):
//   supabase.functions.url=https://<project-ref>.supabase.co/functions/v1/
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val supabaseFunctionsUrl: String = localProperties
    .getProperty("supabase.functions.url", "https://example.supabase.co/functions/v1/")
    .let { if (it.endsWith("/")) it else "$it/" }

// Google Maps API key. It lives in local.properties (never versioned):
//   maps.api.key=AIza...
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY", "")

android {
    namespace = "com.jyhs.educheck"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.jyhs.educheck"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_FUNCTIONS_URL", "\"$supabaseFunctionsUrl\"")
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        debug {
            // Cambia a true para usar el Firebase Emulator Suite (Auth 9099, Firestore 8080)
            buildConfigField("boolean", "USE_FIREBASE_EMULATOR", "false")
        }
        release {
            buildConfigField("boolean", "USE_FIREBASE_EMULATOR", "false")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Room exporta el esquema para poder probar migraciones
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Compose y Material 3
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Hilt (KSP, no kapt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Persistencia local y trabajo en segundo plano
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)

    // Coroutines (Task.await() de Firebase)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.androidx.compose.material.icons.core)

    // Mapa y ubicación
    implementation(libs.maps.compose)
    implementation(libs.play.services.location)

    // Red: Retrofit + OkHttp + kotlinx.serialization (solo para las Edge Functions y las URLs firmadas)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // Firebase: Authentication, Firestore y Cloud Messaging
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)

    // Pruebas
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
