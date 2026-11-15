package sambhav.os.launcher

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import sambhav.os.launcher.data.AppInfo
import sambhav.os.launcher.data.AppRepository
import sambhav.os.launcher.data.Prefs

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)
    val repo = AppRepository(app)
    val apps: StateFlow<List<AppInfo>> = repo.apps

    var editMode by mutableStateOf(false)
    var drawerOpen by mutableStateOf(false)
    var settingsOpen by mutableStateOf(false)
    var focusSearch by mutableStateOf(false)
    var widgetPicker by mutableStateOf(false)
    var homeTick by mutableIntStateOf(0)

    init {
        repo.start()
        viewModelScope.launch {
            val first = repo.apps.first { it.isNotEmpty() }
            seed(first)
        }
    }

    private fun seed(all: List<AppInfo>) {
        if (prefs.seeded) return
        val probes = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_VIEW, Uri.parse("sms:")),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")),
        )
        val dock = probes.mapNotNull { repo.resolvePackage(it) }
            .mapNotNull { p -> all.firstOrNull { it.pkg == p }?.key }
            .distinct()
        prefs.dockKeys = dock
        prefs.homeKeys = all.map { it.key }.filter { it !in dock }.take(8)
        prefs.seeded = true
    }

    fun resetHome() {
        prefs.seeded = false
        seed(apps.value)
    }

    fun resolve(keys: List<String>, all: List<AppInfo>): List<AppInfo> {
        val m = all.associateBy { it.key }
        return keys.mapNotNull { m[it] }
    }

    fun search(query: String, list: List<AppInfo>): List<AppInfo> {
        val n = query.trim().lowercase()
        if (n.isEmpty()) return emptyList()
        val (starts, contains) = list.filter { it.label.lowercase().contains(n) }
            .partition { it.label.lowercase().startsWith(n) }
        return starts + contains
    }

    // Home grid
    fun moveHome(key: String, to: Int) {
        val keys = resolve(prefs.homeKeys, apps.value).map { it.key }.toMutableList()
        val from = keys.indexOf(key)
        if (from < 0 || to !in keys.indices || from == to) return
        keys.add(to, keys.removeAt(from))
        prefs.homeKeys = keys
    }
    fun addHome(key: String) { if (key !in prefs.homeKeys) prefs.homeKeys = prefs.homeKeys + key }
    fun removeHome(key: String) { prefs.homeKeys = prefs.homeKeys.filter { it != key } }

    // Dock (max 5)
    fun addDock(key: String) {
        val d = prefs.dockKeys.filter { it != key }
        prefs.dockKeys = if (d.size >= 5) d.dropLast(1) + key else d + key
    }
    fun removeDock(key: String) { prefs.dockKeys = prefs.dockKeys.filter { it != key } }

    // Drawer data
    fun toggleFavorite(key: String) {
        prefs.favorites = if (key in prefs.favorites) prefs.favorites.filter { it != key } else prefs.favorites + key
    }
    fun hide(key: String) { prefs.hidden = prefs.hidden + key; removeHome(key); removeDock(key) }
    fun unhide(key: String) { prefs.hidden = prefs.hidden.filter { it != key } }
    fun recordLaunch(key: String) {
        prefs.recents = (listOf(key) + prefs.recents.filter { it != key }).take(10)
    }
    fun recordSearch(q: String) {
        val t = q.trim()
        if (t.isNotEmpty()) prefs.recentSearches = (listOf(t) + prefs.recentSearches.filter { !it.equals(t, true) }).take(6)
    }

    // Navigation
    fun openDrawer(focus: Boolean) { focusSearch = focus; drawerOpen = true; editMode = false }
    fun closeDrawer() { drawerOpen = false }
    fun goHome() {
        drawerOpen = false; settingsOpen = false; editMode = false; homeTick++
    }

    override fun onCleared() {
        repo.stop()
    }
}