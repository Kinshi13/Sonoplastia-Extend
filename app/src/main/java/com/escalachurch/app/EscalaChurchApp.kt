package com.escalachurch.app

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.escalachurch.app.audio.AppSoundPlayer
import com.escalachurch.app.data.remote.AppForegroundSignal
import com.escalachurch.app.di.AppContainer
import com.escalachurch.app.notification.ReminderNotifier
import com.escalachurch.app.notification.ReminderWorker
import kotlinx.coroutines.launch

class EscalaChurchApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        ReminderNotifier.ensureChannel(this)
        ReminderWorker.schedule(this)
        AppSoundPlayer.preloadSwipeEffect(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppForegroundSignal)

        ProcessLifecycleOwner.get().lifecycleScope.launch {
            container.settingsRepository.settingsFlow.collect { settings ->
                AppSoundPlayer.ensureMusicStarted(this@EscalaChurchApp, settings.musicVolume)
            }
        }
    }
}
