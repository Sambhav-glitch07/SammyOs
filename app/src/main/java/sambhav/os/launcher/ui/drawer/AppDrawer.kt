package sambhav.os.launcher.ui.drawer

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sambhav.os.launcher.LauncherViewModel
import sambhav.os.launcher.data.AppInfo
import sambhav.os.launcher.ui.components.AppIconView
import sambhav.os.launcher.ui.theme.LocalPal

/** Swipe-up app drawer: favorites, recents, A-Z sections, live search, long-press actions. */
@Composable
fun AppDrawer(
    vm: LauncherViewModel,
    all: List<AppInfo>,
    onLaunch: (AppInfo, Rect?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pal = LocalPal.current
    val prefs = vm.prefs
    val cols = prefs.drawerColumns
    var query by remember { mutableStateOf("") }
    val visible = remember(all, prefs.hidden) { all.filter { it.key !in prefs.hidden } }
    val favs = remember(visible, prefs.favorites) { vm.resolve(prefs.favorites, visible) }
    val recents = remember(visible, prefs.recents, cols) { vm.resolve(prefs.recents, visible).take(cols) }
    val results = remember(query, visible) { vm.search(query, visible) }
    val sections = remember(visible) {
        visible.groupBy { a -> a.label.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() }?.toString() ?: "#" }
    }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }
    val closeThreshold = with(LocalDensity.current) { 64.dp.toPx() }

    LaunchedEffect(vm.focusSearch) {
        if (vm.focusSearch) {
            runCatching { focus.requestFocus() }
            vm.focusSearch = false
        }
    }
    LaunchedEffect(vm.drawerOpen) {
        if (!vm.drawerOpen) { focusManager.clearFocus(); keyboard?.hide() }
    }

    val launchAndRemember: (AppInfo, Rect?) -> Unit = { a, r ->
        if (query.isNotBlank()) vm.recordSearch(query)
        onLaunch(a, r)
    }

    Column(
        modifier.fillMaxSize().background(pal.bg.copy(alpha = 0.96f))
            .statusBarsPadding().navigationBarsPadding().imePadding(),
    ) {
        // Drag handle: pull down to close.
        Box(
            Modifier.fillMaxWidth().height(28.dp).pointerInput(Unit) {
                var acc = 0f
                detectVerticalDragGestures(
                    onDragStart = { acc = 0f },
                    onVerticalDrag = { _, dy -> acc += dy; if (acc > closeThreshold) { acc = 0f; vm.closeDrawer() } },
                )
            },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(pal.sub.copy(alpha = 0.5f)))
        }

        val pill = RoundedCornerShape(50)
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = TextStyle(color = pal.text, fontSize = 15.sp),
            cursorBrush = SolidColor(pal.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { results.firstOrNull()?.let { launchAndRemember(it, null) } }),
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(48.dp)
                .clip(pill).border(1.dp, pal.accent, pill)
                .focusRequester(focus)
                .pointerInput(Unit) { detectTapGestures { runCatching { focus.requestFocus() } } },
            decorationBox = { inner ->
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = pal.accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("Search apps", color = pal.sub, fontSize = 15.sp)
                        inner()
                    }
                    if (query.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close, "Clear search", tint = pal.sub,
                            modifier = Modifier.size(22.dp).clickable { query = "" },
                        )
                    }
                }
            },
        )

        if (query.isEmpty() && prefs.recentSearches.isNotEmpty()) {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                prefs.recentSearches.take(4).forEach { s ->
                    Text(
                        s, color = pal.text, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clip(pill).border(1.dp, pal.accent.copy(alpha = 0.5f), pill)
                            .clickable { query = s }.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(cols),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f),
        ) {
            if (query.isBlank()) {
                if (favs.isNotEmpty()) {
                    header("Favorites")
                    items(favs, key = { "f" + it.key }) { DrawerTile(vm, it, launchAndRemember) }
                }
                if (prefs.showRecents && recents.isNotEmpty()) {
                    header("Recent")
                    items(recents, key = { "r" + it.key }) { DrawerTile(vm, it, launchAndRemember) }
                }
                sections.forEach { (letter, list) ->
                    header(letter)
                    items(list, key = { it.key }) { DrawerTile(vm, it, launchAndRemember) }
                }
            } else {
                items(results, key = { it.key }) { DrawerTile(vm, it, launchAndRemember) }
            }
        }
        if (query.isNotBlank() && results.isEmpty()) {
            Text("No apps match \u201C$query\u201D", color = pal.sub, modifier = Modifier.padding(24.dp))
        }
    }
}

private fun LazyGridScope.header(title: String) {
    item(span = { GridItemSpan(maxLineSpan) }, key = "h$title") {
        val pal = LocalPal.current
        Text(
            title.uppercase(), color = pal.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp, modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun DrawerTile(vm: LauncherViewModel, app: AppInfo, onLaunch: (AppInfo, Rect?) -> Unit) {
    val pal = LocalPal.current
    val prefs = vm.prefs
    var menu by remember { mutableStateOf(false) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    Box(
        Modifier.fillMaxWidth().padding(vertical = 8.dp)
            .onGloballyPositioned { bounds = it.boundsInWindow() }
            .semantics { contentDescription = app.label }
            .pointerInput(app.key) { detectTapGestures(onTap = { onLaunch(app, bounds) }, onLongPress = { menu = true }) },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppIconView(app, prefs.iconSize.dp, prefs.iconShape, prefs.iconStyle)
            if (prefs.showLabels) {
                Text(
                    app.label, color = pal.text, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp),
                )
            }
        }
        AppMenu(vm, app, menu) { menu = false }
    }
}

@Composable
private fun AppMenu(vm: LauncherViewModel, app: AppInfo, expanded: Boolean, onDismiss: () -> Unit) {
    val prefs = vm.prefs
    val shortcuts by produceState(emptyList<ShortcutInfo>(), app.key, expanded) {
        value = if (expanded) withContext(Dispatchers.IO) { vm.repo.shortcuts(app) } else emptyList()
    }
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        shortcuts.forEach { s ->
            DropdownMenuItem(
                text = { Text(s.shortLabel?.toString() ?: "Shortcut") },
                onClick = { vm.repo.startShortcut(s); onDismiss() },
            )
        }
        DropdownMenuItem(text = { Text("Add to home") }, onClick = { vm.addHome(app.key); onDismiss() })
        DropdownMenuItem(text = { Text("Add to dock") }, onClick = { vm.addDock(app.key); onDismiss() })
        DropdownMenuItem(
            text = { Text(if (app.key in prefs.favorites) "Remove from favorites" else "Add to favorites") },
            onClick = { vm.toggleFavorite(app.key); onDismiss() },
        )
        DropdownMenuItem(text = { Text("Hide app") }, onClick = { vm.hide(app.key); onDismiss() })
        DropdownMenuItem(text = { Text("App info") }, onClick = { vm.repo.openAppInfo(app); onDismiss() })
        DropdownMenuItem(text = { Text("Uninstall") }, onClick = { vm.repo.uninstall(app); onDismiss() })
    }
}
