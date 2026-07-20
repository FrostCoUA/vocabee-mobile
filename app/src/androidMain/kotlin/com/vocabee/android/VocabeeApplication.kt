package com.vocabee.android

import android.app.Application
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.vocabee.android.di.vocabeeAndroidModule
import com.vocabee.android.di.vocabeeCommonModule
import io.sentry.android.core.SentryAndroid
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class VocabeeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initSentry()
        initPostHog()
        startKoin {
            androidLogger()
            androidContext(this@VocabeeApplication)
            modules(vocabeeCommonModule, vocabeeAndroidModule)
        }
    }

    // Порожній ключ (локальна збірка без телеметрії) повністю вимикає PostHog.
    private fun initPostHog() {
        val apiKey = BuildConfig.VOCABEE_POSTHOG_API_KEY.trim()
        if (apiKey.isEmpty()) return
        val config = PostHogAndroidConfig(
            apiKey = apiKey,
            host = BuildConfig.VOCABEE_POSTHOG_HOST,
        ).apply {
            // Єдина Activity з Compose-навігацією — авто-екрани нічого не дадуть.
            captureScreenViews = false
            captureApplicationLifecycleEvents = true
            debug = BuildConfig.DEBUG
        }
        PostHogAndroid.setup(this, config)
        // Спільна з бекендом властивість, щоб фільтрувати dev/prod в одному проєкті.
        PostHog.register("app_environment", BuildConfig.VOCABEE_SENTRY_ENVIRONMENT)
    }

    // Порожній DSN (локальна збірка без телеметрії) повністю вимикає Sentry.
    private fun initSentry() {
        val dsn = BuildConfig.VOCABEE_SENTRY_DSN.trim()
        if (dsn.isEmpty()) return
        SentryAndroid.init(this) { options ->
            options.dsn = dsn
            options.environment = BuildConfig.VOCABEE_SENTRY_ENVIRONMENT
            options.isDebug = BuildConfig.DEBUG
        }
    }
}
