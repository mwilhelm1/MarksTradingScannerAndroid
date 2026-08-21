plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun configuredValue(environmentName: String, propertyName: String): String =
    providers.environmentVariable(environmentName).orNull
        ?: localProperties.getProperty(propertyName, "")

val releaseKeystorePath = providers.environmentVariable(
    "ANDROID_KEYSTORE_PATH"
).orNull
val releaseKeystorePassword = providers.environmentVariable(
    "ANDROID_KEYSTORE_PASSWORD"
).orNull
val releaseKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
val hasReleaseSigning = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "com.example.markstradingscanner"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.markstradingscanner"
        minSdk = 26
        targetSdk = 37
        versionCode = configuredValue("ANDROID_VERSION_CODE", "ANDROID_VERSION_CODE")
            .toIntOrNull() ?: 3
        versionName = configuredValue("ANDROID_VERSION_NAME", "ANDROID_VERSION_NAME")
            .ifBlank { "1.2" }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "MOBILE_API_BASE_URL",
            "\"${configuredValue("MOBILE_API_BASE_URL", "MOBILE_API_BASE_URL")}\"",
        )
        buildConfigField(
            "String",
            "MOBILE_API_KEY",
            "\"${configuredValue("MOBILE_API_KEY", "MOBILE_API_KEY")}\"",
        )
        buildConfigField(
            "String",
            "ANDROID_UPDATE_REPOSITORY",
            "\"${configuredValue("ANDROID_UPDATE_REPOSITORY", "ANDROID_UPDATE_REPOSITORY")}\"",
        )
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("releaseSecrets") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("releaseSecrets")
            }
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

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation("org.json:json:20250517")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
