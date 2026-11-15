package sambhav.os.launcher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import kotlin.reflect.KProperty

/** One persisted setting that is also Compose state, so the UI updates when it changes. */
class Pref<T>(
    private val sp: SharedPreferences,
    private val key: String,
    private val default: T,
    private val read: (SharedPreferences, String, T) -> T,
    private val write: (SharedPreferences.Editor, String, T) -> Unit,
) {
    private var state by mutableStateOf(read(sp, key, default))
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = state
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        state = value
        val e = sp.edit()
        write(e, key, value)
        e.apply()
    }
    fun reload() { state = read(sp, key, default) }
}

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("sambhav_prefs", Context.MODE_PRIVATE)
    private val all = mutableListOf<Pref<*>>()

    private fun <T> reg(p: Pref<T>): Pref<T> { all.add(p); return p }
    private fun intP(k: String, d: Int) =
        reg(Pref(sp, k, d, { s, key, df -> s.getInt(key, df) }, { e, key, v -> e.putInt(key, v) }))
    private fun floatP(k: String, d: Float) =
        reg(Pref(sp, k, d, { s, key, df -> s.getFloat(key, df) }, { e, key, v -> e.putFloat(key, v) }))
    private fun boolP(k: String, d: Boolean) =
        reg(Pref(sp, k, d, { s, key, df -> s.getBoolean(key, df) }, { e, key, v -> e.putBoolean(key, v) }))
    private fun listP(k: String) =
        reg(Pref(sp, k, emptyList<String>(),
            { s, key, _ -> (s.getString(key, "") ?: "").split("\n").filter { it.isNotEmpty() } },
            { e, key, v -> e.putString(key, v.joinToString("\n")) }))

    // Appearance
    var themeMode by intP("theme", 0)            // 0 system, 1 light, 2 dark
    var accent by intP("accent", 0)
    var dim by floatP("dim", 0.15f)
    var clockSerif by boolP("clock_serif", true)
    var clockSize by floatP("clock_size", 112f)
    // Icons
    var iconSize by floatP("icon_size", 58f)
    var iconShape by intP("icon_shape", 0)       // 0 squircle, 1 circle, 2 square
    var iconStyle by intP("icon_style", 0)       // 0 original, 1 themed (monochrome if the app has one)
    var showLabels by boolP("labels", true)
    // Grid & drawer
    var columns by intP("cols", 4)
    var rows by intP("rows", 4)
    var drawerColumns by intP("drawer_cols", 4)
    var showRecents by boolP("show_recents", true)
    var animations by boolP("animations", true)
    var parallax by boolP("parallax", true)
    // Content
    var seeded by boolP("seeded", false)
    var homeKeys by listP("home")
    var dockKeys by listP("dock")
    var favorites by listP("favorites")
    var hidden by listP("hidden")
    var recents by listP("recents")
    var recentSearches by listP("recent_searches")
    // Hosted widgets (ids are device specific, so they are not part of backups)
    var widgetIds by listP("wids")
    var widgetHeights by listP("wh")

    private val skipInBackup = setOf("wids", "wh")

    fun exportJson(): String {
        val o = JSONObject()
        for ((k, v) in sp.all) {
            if (k in skipInBackup) continue
            val t = when (v) {
                is Int -> "i"
                is Float -> "f"
                is Boolean -> "b"
                is String -> "s"
                else -> continue
            }
            o.put(k, JSONObject().put("t", t).put("v", v))
        }
        return o.toString(2)
    }

    fun importJson(text: String) {
        val o = JSONObject(text)
        val e = sp.edit()
        o.keys().forEach { k ->
            if (k in skipInBackup) return@forEach
            val x = o.getJSONObject(k)
            when (x.getString("t")) {
                "i" -> e.putInt(k, x.getInt("v"))
                "f" -> e.putFloat(k, x.getDouble("v").toFloat())
                "b" -> e.putBoolean(k, x.getBoolean("v"))
                "s" -> e.putString(k, x.getString("v"))
            }
        }
        e.apply()
        all.forEach { it.reload() }
    }
}