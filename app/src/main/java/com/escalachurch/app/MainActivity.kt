package com.escalachurch.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.escalachurch.app.domain.model.AppSettings
import com.escalachurch.app.ui.navigation.EscalaChurchNavGraph
import com.escalachurch.app.ui.theme.EscalaChurchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as EscalaChurchApp).container

        setContent {
            val settings by container.settingsRepository.settingsFlow.collectAsState(initial = AppSettings())

            EscalaChurchTheme(settings = settings) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    EscalaChurchNavGraph()
                }
            }
        }
    }
}
