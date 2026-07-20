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
val vocabeeDevApiBaseUrl: String =
    localProps.getProperty("vocabee.api.devBaseUrl")
        ?: localProps.getProperty("vocabee.api.baseUrl")
        ?: System.getenv("VOCABEE_DEV_API_BASE_URL")
        ?: System.getenv("VOCABEE_API_BASE_URL")
        ?: "https://dev-api.vocabee.online"
val vocabeeProdApiBaseUrl: String =
    localProps.getProperty("vocabee.api.prodBaseUrl")
        ?: System.getenv("VOCABEE_PROD_API_BASE_URL")
        ?: "https://api.vocabee.online"
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
val vocabeeSentryDsn: String =
    localProps.getProperty("vocabee.sentry.dsn")
        ?: System.getenv("VOCABEE_SENTRY_DSN")
        ?: "https://0173dca9d02a75f689499911111b995c@o4511762662948864.ingest.de.sentry.io/4511762686279760"
// PostHog project API key — публічний клієнтський ключ (як Sentry DSN), не секрет.
val vocabeePosthogApiKey: String =
    localProps.getProperty("vocabee.posthog.apiKey")
        ?: System.getenv("VOCABEE_POSTHOG_API_KEY")
        ?: "phc_ofZDMwg8xddytpa9rBHNbRcvFLkgk3UyZ44mUrpufpeW"
val vocabeePosthogHost: String =
    localProps.getProperty("vocabee.posthog.host")
        ?: System.getenv("VOCABEE_POSTHOG_HOST")
        ?: "https://us.i.posthog.com"

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
            implementation(compose.components.resources)
        }

        androidMain.dependencies {
            implementation(libs.posthog.android)
            implementation(libs.sentry.android)
            implementation(libs.androidx.core.ktx)
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
            implementation(libs.ktor.client.mock)
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

        buildConfigField("String", "VOCABEE_GOOGLE_WEB_CLIENT_ID", "\"$vocabeeGoogleWebClientId\"")
        buildConfigField("String", "VOCABEE_ADMOB_REWARDED_AD_UNIT_ID", "\"$vocabeeAdMobRewardedAdUnitId\"")
        buildConfigField("String", "VOCABEE_SENTRY_DSN", "\"$vocabeeSentryDsn\"")
        buildConfigField("String", "VOCABEE_POSTHOG_API_KEY", "\"$vocabeePosthogApiKey\"")
        buildConfigField("String", "VOCABEE_POSTHOG_HOST", "\"$vocabeePosthogHost\"")
        resValue("string", "vocabee_admob_app_id", vocabeeAdMobAppId)
    }

    buildTypes {
        debug {
            buildConfigField("String", "VOCABEE_API_BASE_URL", "\"$vocabeeDevApiBaseUrl\"")
            buildConfigField("String", "VOCABEE_SENTRY_ENVIRONMENT", "\"development\"")
        }
        release {
            buildConfigField("String", "VOCABEE_API_BASE_URL", "\"$vocabeeProdApiBaseUrl\"")
            buildConfigField("String", "VOCABEE_SENTRY_ENVIRONMENT", "\"production\"")
        }
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

compose.resources {
    packageOfResClass = "com.vocabee.android.resources"
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
