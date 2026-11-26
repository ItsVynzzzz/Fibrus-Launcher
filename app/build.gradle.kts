import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

fun baseVersionName(): String =
    rootProject.file("VERSION").takeIf { it.isFile }
        ?.readText()?.trim()?.takeIf { it.isNotEmpty() }
        ?: "0.1.0"

android {
    // PENTING: namespace TETAP com.israadev.nuxlauncher. Kelas Java mesin Starling (dev.horizon.android.*) yang
    // dipatch amber5 meng-import com.israadev.nuxlauncher.R / BuildConfig / starling.* / discord.*. Mengganti
    // namespace berarti menulis ulang patch dulu (lihat README: "Mengganti nama paket").
    namespace = "com.israadev.nuxlauncher"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.itsvynzzzz.fibrus"
        minSdk = 28 // Starling butuh Android 9+
        targetSdk = 34
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()?.let { 100 + it } ?: 1
        versionName = System.getenv("FIBRUS_VERSION_NAME")?.trim()?.takeIf { it.isNotEmpty() }
            ?: "${baseVersionName()}+dev"

        // Dibaca kelas Java mesin (patch amber5). Discord tidak ada di Fibrus.
        buildConfigField("boolean", "HAS_DISCORD_SDK", "false")

        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a") // Starling hanya dibangun untuk arm64
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Kunci debug bawaan Android: cukup untuk uji. Ganti dengan keystore milikmu sebelum merilis.
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Kelas Java, resource, dan aset mesin Starling. scripts/fetch-horizon-engine.sh (AMBER_ENGINE=starling) mengisi
    // src/starling sebelum build; folder itu tidak ada di repo.
    sourceSets {
        getByName("main") {
            java.srcDir("src/starling/java")
            res.srcDir("src/starling/res")
            assets.srcDir("src/starling/assets")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

// ---- Mesin Rust Starling: dipasang otomatis sebelum build ----
// Mengambil paket mesin dari Release repo (release_repo di starling-engine.properties) dan menaruhnya di
// jniLibs/assets. Tanpa Release-nya atau tanpa jaringan, skrip hanya memberi tahu alasannya dan build tetap jalan
// tanpa mesin (tombol Main akan bilang mesin belum terpasang). Butuh bash (tidak jalan di Windows).
val fetchStarlingEngine by tasks.registering(Exec::class) {
    group = "fibrus"
    description = "Memasang mesin Starling dari Release repo ini bila tersedia"
    workingDir = rootDir
    environment("AMBER_ENGINE", "starling")
    commandLine("bash", "scripts/fetch-horizon-engine.sh")
    isIgnoreExitValue = true
    outputs.upToDateWhen { false }
    enabled = !System.getProperty("os.name").lowercase().contains("win") &&
        File(rootDir, "scripts/fetch-horizon-engine.sh").isFile &&
        File(rootDir, "starling-engine.properties").isFile
}
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(fetchStarlingEngine) }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.gson) // tata letak kontrol Amber (core/controls): baca/tulis dan impor layout
    implementation("androidx.compose.material:material-icons-extended") // versi dari Compose BOM

    debugImplementation(libs.androidx.ui.tooling)
}
