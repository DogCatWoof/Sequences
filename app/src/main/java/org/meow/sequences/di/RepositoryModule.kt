package org.meow.sequences.di

import org.koin.dsl.module
import org.meow.sequences.data.sequence.SequenceRepository

val repositoryModule = module {
    single { SequenceRepository(get(), get()) }
}
