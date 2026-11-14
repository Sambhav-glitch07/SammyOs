package sambhav.os.launcher.data

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.view.View
import android.widget.Toast
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Discovers installed apps through the supported LauncherApps API (no QUERY_ALL_PACKAGES,
 * no root) and keeps the list current when apps are installed, updated or removed.
 */
class AppRepository(private val ctx: Context) {
    private val la = ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private val size = 160

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
    }

    fun start() {
        la.registerCallback(callback)
        refresh()
    }

    fun stop() {
        la.unregisterCallback(callback)
        scope.cancel()
    }

    fun refresh() {
        job?.cancel()
        job = scope.launch { _apps.value = load() }
    }

    private fun load(): List<AppInfo> {
        val me = Process.myUserHandle()
        val out = ArrayList<AppInfo>()
        for (user in la.profiles) {
            for (info in la.getActivityList(null, user)) {
                if (info.componentName.packageName == ctx.packageName) continue
                runCatching {
                    val d = info.getBadgedIcon(0)
                    val key = info.componentName.flattenToString() + if (user == me) "" else "#" + user.hashCode()
                    out.add(AppInfo(key, info.componentName, user, info.label.toString(),
                        renderFull(d).asImageBitmap(), renderMono(d)?.asImageBitmap()))
                }
            }
        }
        return out.sortedBy { it.label.lowercase() }
    }

    /** Draws adaptive icons full-bleed (no system mask) so the user's chosen shape can clip them. */
    private fun renderFull(d: Drawable): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        if (d is AdaptiveIconDrawable) {
            val o = size / 4 // layers are 108dp, only the centre 72dp is visible: scale by 1.5
            d.background?.let { it.setBounds(-o, -o, size + o, size + o); it.draw(c) }
            d.foreground?.let { it.setBounds(-o, -o, size + o, size + o); it.draw(c) }
        } else {
            val i = (size * 0.14f).toInt()
            d.setBounds(i, i, size - i, size - i)
            d.draw(c)
        }
        return bmp
    }

    private fun renderMono(d: Drawable): Bitmap? {
        if (Build.VERSION.SDK_INT < 33 || d !is AdaptiveIconDrawable) return null
        val m = d.monochrome ?: return null
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val o = size / 4
        m.setBounds(-o, -o, size + o, size + o)
        m.draw(Canvas(bmp))
        return bmp
    }

    fun launch(app: AppInfo, view: View?, bounds: Rect?) {
        try {
            val opts = if (view != null && bounds != null)
                ActivityOptions.makeScaleUpAnimation(view, bounds.left, bounds.top, bounds.width(), bounds.height()).toBundle()
            else null
            la.startMainActivity(app.component, app.user, bounds, opts)
        } catch (e: Exception) {
            Toast.makeText(ctx, "Could not open ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openAppInfo(app: AppInfo) {
        try { la.startAppDetailsActivity(app.component, app.user, null, null) } catch (_: Exception) {}
    }

    fun uninstall(app: AppInfo) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.pkg}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            Toast.makeText(ctx, "Cannot uninstall ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    /** Shortcuts are only available while this app is the default launcher; otherwise empty. */
    fun shortcuts(app: AppInfo): List<ShortcutInfo> = try {
        val q = LauncherApps.ShortcutQuery()
            .setPackage(app.pkg)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            )
        la.getShortcuts(q, app.user)?.filter { it.isEnabled }?.take(4) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    fun startShortcut(s: ShortcutInfo) {
        try { la.startShortcut(s, null, null) } catch (_: Exception) {}
    }

    fun resolvePackage(intent: Intent): String? {
        val pkg = ctx.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
        return if (pkg == null || pkg == "android") null else pkg
    }

    @Suppress("unused")
    fun component(flat: String): ComponentName? = ComponentName.unflattenFromString(flat.substringBefore('#'))
}
