package com.escalachurch.app.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.escalachurch.app.EscalaChurchApp

/** Retrieves the app-wide [AppContainer] from the current composition's context. */
@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current.applicationContext as EscalaChurchApp
    return context.container
}

/**
 * Convenience wrapper around [viewModel] that builds the ViewModel from [factory]
 * using the app's [AppContainer], without needing a DI framework.
 */
@Composable
inline fun <reified VM : androidx.lifecycle.ViewModel> appViewModel(
    crossinline factory: (AppContainer) -> VM
): VM {
    val container = rememberAppContainer()
    return viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            factory(container) as T
    })
}
