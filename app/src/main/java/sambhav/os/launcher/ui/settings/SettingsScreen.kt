package sambhav.os.launcher.ui.settings

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sambhav.os.launcher.LauncherViewModel
import sambhav.os.launcher.ui.components.Pill
import sambhav.os.launcher.ui.components.findActivity
import sambhav.os.launcher.ui.theme.AccentNames
import sambhav.os.launcher.ui.theme.LocalPal
import sambhav.os.launcher.ui.theme.accentSwatch

private fun isDefaultLauncher(ctx: Context): Boolean {
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    return ctx.packageManager.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName == ctx.packageName
}

private fun requestDefaultLauncher(ctx: Context) {
    val act = ctx.findActivity()
    if (Build.VERSION.SDK_INT >= 29 && act != null) {
        val rm = ctx.getSystemService(RoleManager::class.java)
        if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
            @Suppress("DEPRECATION")
            act.startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), 77)
            return
        }
    }
    ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
fun SettingsScreen(vm: LauncherViewModel, modifier: Modifier = Modifier) {
    val p = vm.prefs
    val pal = LocalPal.current
    val ctx = LocalContext.current
    val all by vm.apps.collectAsState()
    var showHidden by remember { mutableStateOf(false) }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(p.exportJson().toByteArray()) }
            }.isSuccess
            Toast.makeText(ctx, if (ok) "Backup saved" else "Backup failed", Toast.LENGTH_SHORT).show()
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val ok = runCatching {
                ctx.contentResolver.openInputStream(uri)?.use { p.importJson(it.readBytes().decodeToString()) }
            }.isSuccess
            Toast.makeText(ctx, if (ok) "Settings restored" else "Not a valid backup file", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier.fillMaxSize().background(pal.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.settingsOpen = false }) {
                Icon(Icons.Default.Close, "Close settings", tint = pal.text)
            }
            Text("Settings", color = pal.text, fontSize = 22.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp)) {

            Section("Launcher") {
                val isDefault = isDefaultLauncher(ctx)
                Item("Default launcher") {
                    if (isDefault) Text("Active", color = pal.accent, fontSize = 14.sp)
                    else Pill("Set as default", filled = true) { requestDefaultLauncher(ctx) }
                }
            }

            Section("Appearance") {
                Item("Theme") { Chips(listOf("System", "Light", "Dark"), p.themeMode) { p.themeMode = it } }
                Item("Accent") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AccentNames.forEachIndexed { i, name ->
                            Box(
                                Modifier.size(28.dp).clip(CircleShape).background(accentSwatch(i))
                                    .border(if (p.accent == i) 2.dp else 0.dp, pal.text, CircleShape)
                                    .clickable(onClickLabel = "Accent $name") { p.accent = i },
                            )
                        }
                    }
                }
                Item("Wallpaper") {
                    Pill("Change") {
                        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Select wallpaper"))
                    }
                }
                SliderRow("Wallpaper dim", p.dim, 0f..0.7f, "${(p.dim * 100).toInt()}%") { p.dim = it }
            }

            Section("Clock") {
                Item("Serif clock") { Toggle(p.clockSerif) { p.clockSerif = it } }
                SliderRow("Clock size", p.clockSize, 80f..140f, "${p.clockSize.toInt()}") { p.clockSize = it }
            }

            Section("Icons") {
                Item("Shape") { Chips(listOf("Squircle", "Circle", "Square"), p.iconShape) { p.iconShape = it } }
                Item("Style") { Chips(listOf("Original", "Themed"), p.iconStyle) { p.iconStyle = it } }
                SliderRow("Icon size", p.iconSize, 44f..72f, "${p.iconSize.toInt()} dp") { p.iconSize = it }
                Item("Show labels") { Toggle(p.showLabels) { p.showLabels = it } }
                Text(
                    "Themed uses the app's monochrome icon on Android 13 and newer; other apps keep their normal icon.",
                    color = pal.sub, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            Section("Home grid") {
                Item("Columns") { Stepper(p.columns, 3..6) { p.columns = it } }
                Item("Rows") { Stepper(p.rows, 3..6) { p.rows = it } }
                Item("Reset home layout") { Pill("Reset") { vm.resetHome() } }
            }

            Section("App drawer") {
                Item("Columns") { Stepper(p.drawerColumns, 3..5) { p.drawerColumns = it } }
                Item("Show recent apps") { Toggle(p.showRecents) { p.showRecents = it } }
                Item("Clear recent searches") { Pill("Clear") { p.recentSearches = emptyList() } }
                Item("Hidden apps (${p.hidden.size})") { Pill(if (showHidden) "Close" else "Manage") { showHidden = !showHidden } }
                if (showHidden) {
                    all.forEach { app ->
                        Row(
                            Modifier.fillMaxWidth().clickable { if (app.key in p.hidden) vm.unhide(app.key) else vm.hide(app.key) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = app.key in p.hidden, onCheckedChange = { if (it) vm.hide(app.key) else vm.unhide(app.key) })
                            Text(app.label, color = pal.text, fontSize = 14.sp)
                        }
                    }
                }
            }

            Section("Motion") {
                Item("Animations") { Toggle(p.animations) { p.animations = it } }
                Text(
                    "Turn off to remove springs, jiggle and slide effects.",
                    color = pal.sub, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            Section("Backup") {
                Item("Save settings to a file") { Pill("Back up") { export.launch("sambhav-os-backup.json") } }
                Item("Restore from a file") { Pill("Restore") { import.launch(arrayOf("*/*")) } }
            }

            Text("SAMBHAV OS 0.1.0", color = pal.sub, fontSize = 12.sp, modifier = Modifier.padding(top = 24.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    val pal = LocalPal.current
    Text(
        title.uppercase(), color = pal.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
        modifier = Modifier.padding(top = 22.dp, bottom = 8.dp),
    )
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(pal.surface).padding(horizontal = 16.dp, vertical = 6.dp),
        content = content,
    )
}

@Composable
private fun Item(title: String, content: @Composable RowScope.() -> Unit) {
    val pal = LocalPal.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, color = pal.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
        content()
    }
}

@Composable
private fun Chips(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { i, o -> Pill(o, filled = i == selected) { onSelect(i) } }
    }
}

@Composable
private fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    val pal = LocalPal.current
    Switch(
        checked = checked, onCheckedChange = onChange,
        colors = SwitchDefaults.colors(checkedTrackColor = pal.accent, checkedThumbColor = pal.bg),
    )
}

@Composable
private fun SliderRow(title: String, value: Float, range: ClosedFloatingPointRange<Float>, text: String, onChange: (Float) -> Unit) {
    val pal = LocalPal.current
    Column {
        Item(title) { Text(text, color = pal.sub, fontSize = 13.sp) }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = pal.accent, activeTrackColor = pal.accent),
        )
    }
}

@Composable
private fun Stepper(value: Int, range: IntRange, onChange: (Int) -> Unit) {
    val pal = LocalPal.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StepButton("\u2212", value > range.first) { onChange(value - 1) }
        Text("$value", color = pal.text, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
        StepButton("+", value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val pal = LocalPal.current
    Box(
        Modifier.size(36.dp).clip(CircleShape).border(1.dp, pal.accent.copy(alpha = if (enabled) 1f else 0.3f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = pal.text.copy(alpha = if (enabled) 1f else 0.4f), fontSize = 18.sp) }
}
