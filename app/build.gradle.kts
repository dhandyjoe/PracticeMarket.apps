import java.time.LocalDate

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/** Commit pendek untuk About (DEV-04); "dev" bila bukan repo git. */
val gitSha = runCatching {
    ProcessBuilder("git", "rev-parse", "--short", "HEAD").redirectErrorStream(true).start()
        .inputStream.bufferedReader().readText().trim().ifEmpty { "dev" }
}.getOrDefault("dev")

android {
    namespace = "com.automation.practice"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.automation.practice"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "COMMIT_SHA", "\"$gitSha\"")
        buildConfigField("String", "BUILD_DATE", "\"${LocalDate.now()}\"")
    }

    // English baru mencakup layar Pesanan, PracticePay, Akun & Pengaturan; sisanya jatuh ke teks Indonesia.
    lint { disable += "MissingTranslation" }

    buildFeatures { viewBinding = true
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

// Skema Room per versi di-commit; wajib untuk menulis migrasi (ARCHITECTURE.md §6.6).
ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.kotlinx.coroutines.android)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
