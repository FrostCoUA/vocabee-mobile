import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

/**
 * Per-developer overrides live in `local.properties` (gitignored). We read the gateway
 * base URL from there so each contributor can point at their own LAN IP / tunnel
 * without committing the value. Falls back to the emulator loopback for first runs.
 */
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val vocabeeApiBaseUrl: String =
    localProps.getProperty("vocabee.api.baseUrl")
        ?: System.getenv("VOCABEE_API_BASE_URL")
        ?: "http://10.0.2.2:3000"
val vocabeeGoogleWebClientId: String =
    localProps.getProperty("vocabee.google.webClientId")
        ?: System.getenv("VOCABEE_GOOGLE_WEB_CLIENT_ID")
        ?: ""
val vocabeeAdMobAppId: String =
    localProps.getProperty("vocabee.admob.appId")
        ?: System.getenv("VOCABEE_ADMOB_APP_ID")
        ?: "ca-app-pub-3940256099942544~3347511713"
val vocabeeAdMobRewardedAdUnitId: String =
    localProps.getProperty("vocabee.admob.rewardedAdUnitId")
        ?: System.getenv("VOCABEE_ADMOB_REWARDED_AD_UNIT_ID")
        ?: "ca-app-pub-3940256099942544/5224354917"

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Vocabee"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.koin.core)
            implementation(libs.jetbrains.ui.backhandler)
            implementation(libs.qrose)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.viewmodel.ktx)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)
            implementation(libs.play.services.ads)
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.vocabee.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vocabee.android"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "VOCABEE_API_BASE_URL", "\"$vocabeeApiBaseUrl\"")
        buildConfigField("String", "VOCABEE_GOOGLE_WEB_CLIENT_ID", "\"$vocabeeGoogleWebClientId\"")
        buildConfigField("String", "VOCABEE_ADMOB_REWARDED_AD_UNIT_ID", "\"$vocabeeAdMobRewardedAdUnitId\"")
        resValue("string", "vocabee_admob_app_id", vocabeeAdMobAppId)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
