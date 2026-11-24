import java.util.Properties

// Datos de la llave de firma: se leen de keystore.properties (archivo local que NO se sube a GitHub).
// Ver keystore.properties.example.
val firma = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    // Mide la cobertura de las pruebas unitarias (reporte HTML + regla del 85%)
    alias(libs.plugins.kover)
}

android {
    namespace = "cl.duoc.serviciotecnico"
    compileSdk = 35

    defaultConfig {
        applicationId = "cl.duoc.serviciotecnico"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // URL del backend: se toma de API_URL en gradle.properties; si no existe, usa el backend local del emulador (10.0.2.2)
        val apiUrl = (project.findProperty("API_URL") as String?) ?: "http://10.0.2.2:8080/"
        buildConfigField("String", "API_URL", "\"$apiUrl\"")
    }

    // Firma del APK de release con la llave del equipo (release/tactech-release.jks)
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("release/tactech-release.jks")
            storePassword = firma.getProperty("storePassword")
            keyAlias = firma.getProperty("keyAlias")
            keyPassword = firma.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
        compose = true
        buildConfig = true
    }

    // Robolectric necesita los recursos de Android (temas, manifest, FileProvider) en las pruebas unitarias
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { it.maxHeapSize = "3g" }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.play.services.location)
    implementation(libs.coil.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.androidx.datastore.preferences)

    // ---------- Pruebas unitarias (app/src/test) ----------
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    // Room, pantallas Compose, Activity y utilidades de Android probadas en la JVM con Robolectric
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}

/*
 * Cobertura con Kover sobre TODO el codigo de la app (sin exclusiones): dominio, datos, Room,
 * ViewModel, pantallas y componentes Compose, navegacion, tema, Activity y Application.
 * Las pantallas y Room se prueban en la JVM con Robolectric.
 *
 *   Reporte:  ./gradlew koverHtmlReportDebug  -> app/build/reports/kover/htmlDebug/index.html
 *   Total:    ./gradlew koverLogDebug
 *   Regla:    ./gradlew koverVerifyDebug      -> falla si la cobertura baja del 80%
 */
kover {
    reports {
        verify {
            rule {
                minBound(80)
            }
        }
    }
}
