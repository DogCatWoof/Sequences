package org.meow.sequences

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.meow.sequences.di.authModule
import org.meow.sequences.di.databaseModule
import org.meow.sequences.di.diagnosticsModule
import org.meow.sequences.di.firestoreModule
import org.meow.sequences.di.repositoryModule
import org.meow.sequences.di.viewModelModule

class SequencesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SequencesApp)
            modules(
                databaseModule,
                diagnosticsModule,
                authModule,
                repositoryModule,
                firestoreModule,
                viewModelModule,
            )
        }
    }
}
