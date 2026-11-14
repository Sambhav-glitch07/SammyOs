package sambhav.os.launcher.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import sambhav.os.launcher.data.Prefs

/**
 * Hosts real Android widgets through the supported AppWidget framework. The system asks the user
 * for permission to bind a widget the first time, and runs the widget's own configuration screen.
 */
class WidgetController(private val activity: Activity, private val prefs: Prefs) {
    private val mgr = AppWidgetManager.getInstance(activity)
    val host = AppWidgetHost(activity, 1024)
    val ids = mutableStateListOf<Int>().apply { addAll(prefs.widgetIds.mapNotNull { it.toIntOrNull() }) }
    val heights = mutableStateMapOf<Int, Int>().apply {
        prefs.widgetHeights.forEach { s ->
            val p = s.split("=")
            if (p.size == 2) { val a = p[0].toIntOrNull(); val b = p[1].toIntOrNull(); if (a != null && b != null) put(a, b) }
        }
    }
    private var pendingId = -1

    fun startListening() { try { host.startListening() } catch (_: Exception) {} }
    fun stopListening() { try { host.stopListening() } catch (_: Exception) {} }

    fun providers(): List<AppWidgetProviderInfo> =
        mgr.installedProviders.sortedBy { it.loadLabel(activity.packageManager)?.lowercase() ?: "" }

    fun info(id: Int): AppWidgetProviderInfo? = mgr.getAppWidgetInfo(id)

    fun add(p: AppWidgetProviderInfo) {
        val id = host.allocateAppWidgetId()
        pendingId = id
        if (mgr.bindAppWidgetIdIfAllowed(id, p.provider)) {
            afterBind(id)
        } else {
            val i = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, p.provider)
            @Suppress("DEPRECATION")
            activity.startActivityForResult(i, REQ_BIND)
        }
    }

    private fun afterBind(id: Int) {
        val info = mgr.getAppWidgetInfo(id)
        if (info?.configure != null) {
            try {
                host.startAppWidgetConfigureActivityForResult(activity, id, 0, REQ_CONFIG, null)
            } catch (_: Exception) { commit(id) }
        } else commit(id)
    }

    fun onResult(requestCode: Int, resultCode: Int, @Suppress("UNUSED_PARAMETER") data: Intent?) {
        when (requestCode) {
            REQ_BIND -> if (resultCode == Activity.RESULT_OK) afterBind(pendingId) else cancel()
            REQ_CONFIG -> if (resultCode == Activity.RESULT_OK) commit(pendingId) else cancel()
        }
    }

    private fun commit(id: Int) {
        val info = mgr.getAppWidgetInfo(id)
        val density = activity.resources.displayMetrics.density
        val h = if (info != null && info.minHeight > 0) (info.minHeight / density).toInt().coerceIn(80, 400) else 120
        ids.add(id)
        heights[id] = h
        save()
        pendingId = -1
    }

    private fun cancel() {
        if (pendingId >= 0) host.deleteAppWidgetId(pendingId)
        pendingId = -1
    }

    fun remove(id: Int) {
        host.deleteAppWidgetId(id)
        ids.remove(id)
        heights.remove(id)
        save()
    }

    fun resize(id: Int, deltaDp: Int) {
        heights[id] = ((heights[id] ?: 120) + deltaDp).coerceIn(80, 520)
        save()
    }

    private fun save() {
        prefs.widgetIds = ids.map { it.toString() }
        prefs.widgetHeights = heights.map { "${it.key}=${it.value}" }
    }

    companion object {
        const val REQ_BIND = 4101
        const val REQ_CONFIG = 4102
    }
}
