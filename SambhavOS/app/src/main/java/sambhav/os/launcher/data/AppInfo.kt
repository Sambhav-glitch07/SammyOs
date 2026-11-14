package sambhav.os.launcher.data

import android.content.ComponentName
import android.os.UserHandle
import androidx.compose.ui.graphics.ImageBitmap

/** One launchable activity as reported by the system's LauncherApps service. */
class AppInfo(
    val key: String,
    val component: ComponentName,
    val user: UserHandle,
    val label: String,
    val icon: ImageBitmap,
    val mono: ImageBitmap?,
) {
    val pkg: String get() = component.packageName
}
