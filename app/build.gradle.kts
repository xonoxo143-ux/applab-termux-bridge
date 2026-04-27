plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val buildNumber = providers.environmentVariable("GITHUB_RUN_NUMBER")
    .map { it.toIntOrNull() ?: 1 }
    .orElse(1)
    .get()
val buildName = providers.environmentVariable("GITHUB_RUN_NUMBER")
    .orElse("local")
    .get()

val signingKeystore = providers.environmentVariable("APPLAB_SIGNING_KEYSTORE").orNull
val signingStorePassword = providers.environmentVariable("APPLAB_SIGNING_STORE_PASSWORD").orNull
val signingKeyAlias = providers.environmentVariable("APPLAB_SIGNING_KEY_ALIAS").orNull
val signingKeyPassword = providers.environmentVariable("APPLAB_SIGNING_KEY_PASSWORD").orNull
val hasApplabSigning = listOf(
    signingKeystore,
    signingStorePassword,
    signingKeyAlias,
    signingKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.applab.termuxbridge"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.applab.termuxbridge"
        minSdk = 26
        targetSdk = 35
        versionCode = 1000 + buildNumber
        versionName = "0.1.$buildName"
    }

    signingConfigs {
        if (hasApplabSigning) {
            create("applabDev") {
                storeFile = file(signingKeystore!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            if (hasApplabSigning) {
                signingConfig = signingConfigs.getByName("applabDev")
            }
        }
        release {
            if (hasApplabSigning) {
                signingConfig = signingConfigs.getByName("applabDev")
            }
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
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.documentfile:documentfile:1.0.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
