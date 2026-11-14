package sambhav.os.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import sambhav.os.launcher.ui.home.HomeScreen
import sambhav.os.launcher.ui.theme.SambhavTheme
import sambhav.os.launcher.widgets.WidgetController

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()
    private lateinit var widgets: WidgetController

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        widgets = WidgetController(this, vm.prefs)
        setContent {
            SambhavTheme(vm.prefs.themeMode, vm.prefs.accent) {
                HomeScreen(vm, widgets)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        widgets.startListening()
    }

    override fun onStop() {
        widgets.stopListening()
        super.onStop()
    }

    /** Pressing Home while the launcher is open returns to the first page and closes overlays. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) vm.goHome()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        widgets.onResult(requestCode, resultCode, data)
    }
}
