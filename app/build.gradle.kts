plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "br.com.gastoscasal"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.com.gastoscasal"
        minSdk = 26
        targetSdk = 35
        // Cada build no GitHub ganha um número maior, assim a atualização instala por cima.
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1." + (System.getenv("GITHUB_RUN_NUMBER") ?: "0")
    }

    signingConfigs {
        create("fixa") {
            storeFile = file("assinatura.jks")
            storePassword = "gastos123"
            keyAlias = "gastos"
            keyPassword = "gastos123"
        }
    }

    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.getByName("fixa") }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fixa")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
