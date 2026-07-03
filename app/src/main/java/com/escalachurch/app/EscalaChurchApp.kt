package com.escalachurch.app

import android.app.Application
import com.escalachurch.app.di.AppContainer
import com.escalachurch.app.notification.ReminderNotifier
import com.escalachurch.app.notification.ReminderWorker

class EscalaChurchApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        ReminderNotifier.ensureChannel(this)
        ReminderWorker.schedule(this)
    }
}
