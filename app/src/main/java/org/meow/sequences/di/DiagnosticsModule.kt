package org.meow.sequences.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.meow.sequences.data.debug.DebugSettings
import org.meow.sequences.data.debug.ExceptionReporter
import org.meow.sequences.data.diagnostics.QueryLogger

val diagnosticsModule = module {
    single { QueryLogger() }
    single { DebugSettings(androidContext()) }
    single { ExceptionReporter(androidContext(), get()) }
}
