package com.example.demodata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.demodata.ui.navigation.AppNavigation
import com.example.demodata.ui.screens.LoginScreen
import com.example.demodata.ui.theme.AppTheme
import com.example.demodata.ui.viewmodel.GpsViewModel
import com.example.demodata.ui.viewmodel.SessionViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as DemoDataApp

        val gpsViewModel = GpsViewModel(app.gpsRepository)
        val sessionViewModel = SessionViewModel(app.sessionManager)

        setContent {
            val isDarkModePref by sessionViewModel.isDarkMode.collectAsStateWithLifecycle()
            val isLoggedIn by sessionViewModel.isLoggedIn.collectAsStateWithLifecycle()

            val usarModoOscuro = isDarkModePref ?: isSystemInDarkTheme()

            AppTheme(
                darkTheme = usarModoOscuro,
                dynamicColor = false
            ){
                AppNavigation()
            }
        }
    }
}