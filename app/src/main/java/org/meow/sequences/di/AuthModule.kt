package org.meow.sequences.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.meow.sequences.BuildConfig
import org.meow.sequences.data.auth.GoogleAuthManager
import org.meow.sequences.data.auth.TokenStore

val authModule = module {
    single { TokenStore.create(androidContext()) }
    single { GoogleAuthManager(androidContext(), get(), BuildConfig.FIREBASE_WEB_CLIENT_ID) }
}
