plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

import java.util.Properties
import java.io.FileInputStream

// Cargar propiedades del keystore
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = if (keystorePropertiesFile.exists()) {
    Properties().apply {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
} else {
    null
}

android {
    namespace = "com.example.gestorgastos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.glebursol.registrogastos"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            keystoreProperties?.let { props ->
                val storeFile = file(props.getProperty("storeFile"))
                if (storeFile.exists()) {
                    keyAlias = props.getProperty("keyAlias")
                    keyPassword = props.getProperty("keyPassword")
                    this.storeFile = storeFile
                    storePassword = props.getProperty("storePassword")
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigningConfig = signingConfigs.findByName("release")
            if (releaseSigningConfig != null && releaseSigningConfig.storeFile?.exists() == true) {
                signingConfig = releaseSigningConfig
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation("com.google.android.material:material:1.9.0")
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.activity)
    annotationProcessor(libs.room.compiler)
    
    implementation(libs.workmanager)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation("com.google.firebase:firebase-analytics")
    
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    
    implementation(libs.mpandroidchart)
    implementation(libs.recyclerview)
    
    // SwipeRefreshLayout para pull-to-refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // OkHttp para requests HTTP (ya incluido para Mercado Pago)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}