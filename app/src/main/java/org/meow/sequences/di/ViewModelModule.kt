package org.meow.sequences.di

import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.meow.sequences.ui.screens.SequenceRunViewModel
import org.meow.sequences.ui.screens.SequenceViewModel

val viewModelModule = module {
    viewModel { SequenceViewModel(get()) }
    viewModel { SequenceRunViewModel(get(), androidApplication()) }
}
