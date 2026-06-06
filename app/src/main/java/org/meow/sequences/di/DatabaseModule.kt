package org.meow.sequences.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.meow.sequences.data.database.SequencesDatabase

val databaseModule = module {
    single { SequencesDatabase.getDatabase(androidContext()) }
    single { get<SequencesDatabase>().sequenceDao() }
}
