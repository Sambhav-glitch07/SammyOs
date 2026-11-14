package sambhav.os.launcher.ui.widgets

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import sambhav.os.launcher.LauncherViewModel
import sambhav.os.launcher.ui.components.Pill
import sambhav.os.launcher.ui.theme.LocalPal
import sambhav.os.launcher.widgets.WidgetController

/** Page 2: real Android widgets (clock, weather, calendar, music, battery... whatever is installed). */
@Composable
fun WidgetsPage(vm: LauncherViewModel, wc: WidgetController, modifier: Modifier = Modifier) {
    val pal = LocalPal.current
    var picker by remember { mutableStateOf(false) }
    val widthDp = LocalConfiguration.current.screenWidthDp - 40
    Column(
        modifier.verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = 20.dp).padding(top = 28.dp, bottom = 200.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("WIDGETS", color = pal.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp)
        wc.ids.toList().forEach { id -> key(id) { WidgetCard(vm, wc, id, widthDp) } }
        if (wc.ids.isEmpty()) {
            Text(
                "No widgets yet. Add a clock, weather, calendar or music widget from the apps installed on your phone.",
                color = pal.sub, fontSize = 14.sp,
            )
        }
        Pill("Add widget", filled = true) { picker = true }
    }
    if (picker) WidgetPicker(wc) { picker = false }
}

@Composable
private fun WidgetCard(vm: LauncherViewModel, wc: WidgetController, id: Int, widthDp: Int) {
    val info = remember(id) { wc.info(id) }
    if (info == null) {
        LaunchedEffect(id) { wc.remove(id) }
        return
    }
    val h = wc.heights[id] ?: 120
    Box(Modifier.fillMaxWidth()) {
        AndroidView(
            factory = { ctx -> wc.host.createView(ctx, id, info) },
            update = { it.updateAppWidgetSize(null, widthDp, h, widthDp, h) },
            modifier = Modifier.fillMaxWidth().height(h.dp).clip(RoundedCornerShape(24.dp)),
        )
        if (vm.editMode) {
            Row(Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Control("\u2212") { wc.resize(id, -40) }
                Control("+") { wc.resize(id, 40) }
                val pal = LocalPal.current
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(pal.surface)
                        .border(1.dp, pal.accent, CircleShape).clickable(onClickLabel = "Remove widget") { wc.remove(id) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Close, "Remove widget", tint = pal.text, modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

@Composable
private fun Control(label: String, onClick: () -> Unit) {
    val pal = LocalPal.current
    Box(
        Modifier.size(32.dp).clip(CircleShape).background(pal.surface)
            .border(1.dp, pal.accent, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = pal.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun WidgetPicker(wc: WidgetController, onDismiss: () -> Unit) {
    val pal = LocalPal.current
    val ctx = LocalContext.current
    val providers = remember { wc.providers() }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(28.dp)).background(pal.surface).padding(16.dp)) {
            Text("Choose a widget", color = pal.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(8.dp))
            LazyColumn(Modifier.height(420.dp)) {
                items(providers, key = { it.provider.flattenToString() }) { p ->
                    ProviderRow(p) { wc.add(p); onDismiss() }
                }
            }
        }
    }
}

@Composable
private fun ProviderRow(p: AppWidgetProviderInfo, onClick: () -> Unit) {
    val pal = LocalPal.current
    val ctx = LocalContext.current
    val label = remember(p) { p.loadLabel(ctx.packageManager) ?: p.provider.packageName }
    val preview = remember(p) {
        runCatching { p.loadPreviewImage(ctx, 0)?.toBitmap(240, 140)?.asImageBitmap() }.getOrNull()
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (preview != null) {
            Image(preview, null, Modifier.size(width = 96.dp, height = 56.dp).clip(RoundedCornerShape(10.dp)))
        } else {
            Box(Modifier.size(width = 96.dp, height = 56.dp).clip(RoundedCornerShape(10.dp)).background(pal.tileA))
        }
        Column {
            Text(label, color = pal.text, fontSize = 14.sp)
            Text(p.provider.packageName, color = pal.sub, fontSize = 11.sp, maxLines = 1)
        }
    }
}
