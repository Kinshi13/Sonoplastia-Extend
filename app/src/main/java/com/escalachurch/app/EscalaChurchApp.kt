package com.escalachurch.app

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.escalachurch.app.data.SeedData
import com.escalachurch.app.di.AppContainer
import kotlinx.coroutines.launch

class EscalaChurchApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        ProcessLifecycleOwner.get().lifecycleScope.launch {
            SeedData.populateIfEmpty(container)
        }
    }
}
