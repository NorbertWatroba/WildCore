package com.wildcore.di

import androidx.room.Room
import com.wildcore.data.WildCoreDatabase
import com.wildcore.ui.SurvivalViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Singleton bazy danych
    single {
        Room.databaseBuilder(
            androidContext(),
            WildCoreDatabase::class.java,
            "wildcore_database"
        ).build()
    }

    // Dostarczenie DAO
    single { get<WildCoreDatabase>().survivalDao }

    // ViewModel (Koin automatycznie wstrzyknie do niego wymagane DAO)
    viewModel { SurvivalViewModel(get()) }
}