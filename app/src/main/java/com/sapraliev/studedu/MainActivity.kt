package com.sapraliev.studedu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sapraliev.studedu.core.AppGraph
import com.sapraliev.studedu.ui.navigation.AppNavigation
import com.sapraliev.studedu.ui.theme.StudeduTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppGraph.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val themeMode by AppGraph.settings.themeMode.collectAsState()
            StudeduTheme(themeMode = themeMode) {
                AppNavigation()
            }
        }
    }
}
