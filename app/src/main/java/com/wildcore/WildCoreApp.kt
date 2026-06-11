package com.wildcore

import android.app.Application
import com.wildcore.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import kotlin.collections.emptyList

class WildCoreApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            // Logi Koina przydatne przy debugowaniu
            androidLogger()
            // Kontekst Androida wstrzykiwany do zależności
            androidContext(this@WildCoreApp)
            // Tutaj później dodamy nasze moduły z warstwy di/
            modules(appModule)
        }
    }
}