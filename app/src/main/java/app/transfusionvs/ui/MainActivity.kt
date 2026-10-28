package app.transfusionvs.ui

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.transfusionvs.app
import app.transfusionvs.ui.screens.SetupScreen
import app.transfusionvs.ui.theme.TvsTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT))
        // Phase 1: the app opens on Setup check. Home / Start / Detail arrive in Phase 2.
        setContent { TvsTheme { SetupScreen() } }
    }

    override fun onResume() {
        super.onResume()
        // Opening the app always re-arms every alarm from the database (cheap and idempotent).
        app.scope.launch { app.engine.rearm(canStartService = true) }
    }
}
