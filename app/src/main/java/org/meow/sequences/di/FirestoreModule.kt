package org.meow.sequences.di

import org.koin.dsl.module
import org.meow.sequences.data.firestore.FirestoreSource
import org.meow.sequences.data.firestore.FirestoreSyncService
import org.meow.sequences.data.sequence.SequenceDao

val firestoreModule = module {
    single { FirestoreSource() }
    single {
        FirestoreSyncService(
            source = get(),
            sequenceDao = get<SequenceDao>(),
        )
    }
}
