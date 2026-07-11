package com.vocabee.android

import android.app.Application
import com.vocabee.android.di.vocabeeAndroidModule
import com.vocabee.android.di.vocabeeCommonModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class VocabeeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@VocabeeApplication)
            modules(vocabeeCommonModule, vocabeeAndroidModule)
        }
    }
}
