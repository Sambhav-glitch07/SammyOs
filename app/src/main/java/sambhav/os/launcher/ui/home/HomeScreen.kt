package sambhav.os.launcher.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.flow.distinctUntilChanged
import sambhav.os.launcher.ui.components.blurSupported
import sambhav.os.launcher.ui.components.findActivity
import sambhav.os.launcher.ui.components.glass
import sambhav.os.launcher.ui.widgets.WidgetPicker
import kotlin.math.abs
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import sambhav.os.launcher.LauncherViewModel
import sambhav.os.launcher.data.AppInfo
import sambhav.os.launcher.data.Prefs
import sambhav.os.launcher.ui.components.AppIconView
import sambhav.os.launcher.ui.components.Pill
import sambhav.os.launcher.ui.components.RemoveBadge
import sambhav.os.launcher.ui.components.onTapWithBounds
import sambhav.os.launcher.ui.drawer.AppDrawer
import sambhav.os.launcher.ui.settings.SettingsScreen
import sambhav.os.launcher.ui.theme.LocalPal
import sambhav.os.launcher.ui.widgets.WidgetsPage
import sambhav.os.launcher.widgets.WidgetController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BottomReserve = 190.dp

@Composable
fun HomeScreen(vm: LauncherViewModel, wc: WidgetController) {
    val prefs = vm.prefs
    val pal = LocalPal.current
    val ctx = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val all by vm.apps.collectAsState()
    val pager = rememberPagerState(pageCount = { 2 })

    val homeApps = remember(all, prefs.homeKeys) { vm.resolve(prefs.homeKeys, all) }
    val dockApps = remember(all, prefs.dockKeys) { vm.resolve(prefs.dockKeys, all) }

    val launch: (AppInfo, Rect?) -> Unit = { app, r ->
        vm.recordLaunch(app.key)
        val ar = r?.let { android.graphics.Rect(it.left.toInt(), it.top.toInt(), it.right.toInt(), it.bottom.toInt()) }
        vm.repo.launch(app, view, ar)
    }

    val spec: AnimationSpec<Float> =
        if (prefs.animations) spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMediumLow) else snap()
    val p by animateFloatAsState(if (vm.drawerOpen) 1f else 0f, spec, label = "drawer")

    val tilt = rememberTilt(prefs.parallax)
    // Real backdrop blur of the wallpaper behind the drawer and settings (Android 12+).
    val win = ctx.findActivity()?.window
    LaunchedEffect(Unit) {
        if (ctx.blurSupported()) {
            snapshotFlow { (maxOf(p, if (vm.settingsOpen) 1f else 0f) * 70).toInt() }
                .distinctUntilChanged()
                .collect { r -> runCatching { win?.setBackgroundBlurRadius(r) } }
        }
    }
    // After adding a widget, jump to the widgets page so it is easy to find.
    val prevWidgets = remember { mutableStateOf(wc.ids.size) }
    LaunchedEffect(wc.ids.size) {
        if (wc.ids.size > prevWidgets.value) pager.animateScrollToPage(1)
        prevWidgets.value = wc.ids.size
    }

    LaunchedEffect(vm.homeTick) { if (vm.homeTick > 0) pager.animateScrollToPage(0) }
    BackHandler(enabled = vm.settingsOpen || vm.drawerOpen || vm.editMode || pager.currentPage != 0) {
        when {
            vm.settingsOpen -> vm.settingsOpen = false
            vm.drawerOpen -> vm.closeDrawer()
            vm.editMode -> vm.editMode = false
            else -> scope.launch { pager.animateScrollToPage(0) }
        }
    }

    val scrimAlpha = if (pal.dark) prefs.dim else maxOf(prefs.dim, 0.55f)
    Box(Modifier.fillMaxSize().background(pal.scrim.copy(alpha = scrimAlpha))) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                val s = 1f - 0.05f * p
                scaleX = s
                scaleY = s
                alpha = 1f - p
            },
        ) {
            DepthRings(tilt, pager)
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                Box(
                    Modifier.fillMaxSize().graphicsLayer {
                        val o = abs((pager.currentPage - page) + pager.currentPageOffsetFraction).coerceIn(0f, 1f)
                        val sc = 1f - 0.07f * o
                        scaleX = sc
                        scaleY = sc
                        alpha = 1f - 0.6f * o
                    },
                ) {
                    if (page == 0) HomePage(vm, homeApps, tilt, launch)
                    else WidgetsPage(vm, wc, Modifier.fillMaxSize().navigationBarsPadding())
                }
            }

            AnimatedVisibility(
                vm.editMode,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp),
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f),
            ) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Done", filled = true) { vm.editMode = false }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill("Wallpaper") {
                            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Select wallpaper"))
                        }
                        Pill("Widgets") { vm.widgetPicker = true }
                        Pill("Settings") { vm.settingsOpen = true }
                    }
                }
            }

            Column(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PageDots(pager.currentPage, 2)
                Spacer(Modifier.height(10.dp))
                SearchPill { vm.openDrawer(true) }
                Spacer(Modifier.height(12.dp))
                Dock(vm, dockApps, launch)
            }
        }

        if (p > 0.001f) {
            AppDrawer(
                vm, all, { a, r -> launch(a, r); vm.closeDrawer() },
                Modifier.graphicsLayer {
                    translationY = (1f - p) * size.height
                    alpha = (p * 1.4f).coerceIn(0f, 1f)
                },
            )
        }

        AnimatedVisibility(
            vm.settingsOpen,
            enter = slideInVertically { it / 6 } + fadeIn(),
            exit = slideOutVertically { it / 6 } + fadeOut(),
        ) { SettingsScreen(vm) }

        if (vm.widgetPicker) WidgetPicker(wc) { vm.widgetPicker = false }
    }
}

@Composable
private fun HomePage(vm: LauncherViewModel, homeApps: List<AppInfo>, tilt: State<Offset>, launch: (AppInfo, Rect?) -> Unit) {
    val prefs = vm.prefs
    val threshold = with(LocalDensity.current) { 56.dp.toPx() }
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { vm.editMode = true }, onTap = { if (vm.editMode) vm.editMode = false })
            }
            .pointerInput(Unit) {
                var acc = 0f
                detectVerticalDragGestures(
                    onDragStart = { acc = 0f },
                    onDragEnd = { acc = 0f },
                    onDragCancel = { acc = 0f },
                    onVerticalDrag = { _, dy ->
                        acc += dy
                        if (acc < -threshold) { acc = 0f; vm.openDrawer(false) }
                    },
                )
            },
    ) {
        val compact = maxHeight < 560.dp
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(bottom = BottomReserve)) {
            ClockBlock(prefs, compact, tilt)
            BatteryStrip(compact)
            Spacer(Modifier.height(if (compact) 8.dp else 18.dp))
            HomeGrid(
                apps = homeApps,
                cols = prefs.columns,
                rows = prefs.rows,
                iconSize = prefs.iconSize.dp,
                labels = prefs.showLabels,
                shape = prefs.iconShape,
                style = prefs.iconStyle,
                edit = vm.editMode,
                animate = prefs.animations,
                onEnterEdit = { vm.editMode = true },
                onMove = { k, i -> vm.moveHome(k, i) },
                onRemove = { vm.removeHome(it.key) },
                onLaunch = { a, r -> launch(a, r) },
                modifier = Modifier.weight(1f).fillMaxWidth().graphicsLayer {
                    translationX = tilt.value.x * 5.dp.toPx()
                    translationY = tilt.value.y * 4.dp.toPx()
                },
            )
        }
    }
}

/** Clock that updates on the system minute tick; no polling, no background work. */
@Composable
private fun rememberNow(): State<LocalDateTime> {
    val ctx = LocalContext.current
    val now = remember { mutableStateOf(LocalDateTime.now()) }
    DisposableEffect(ctx) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { now.value = LocalDateTime.now() }
        }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(ctx, r, f, ContextCompat.RECEIVER_NOT_EXPORTED)
        now.value = LocalDateTime.now()
        onDispose { ctx.unregisterReceiver(r) }
    }
    return now
}

@Composable
private fun rememberBattery(): State<Pair<Int, Boolean>> {
    val ctx = LocalContext.current
    fun read(i: Intent?): Pair<Int, Boolean> {
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else 0
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return pct to charging
    }
    val state = remember { mutableStateOf(read(ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))) }
    DisposableEffect(ctx) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { state.value = read(i) }
        }
        ContextCompat.registerReceiver(ctx, r, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { ctx.unregisterReceiver(r) }
    }
    return state
}

@Composable
private fun ClockBlock(prefs: Prefs, compact: Boolean, tilt: State<Offset>) {
    val pal = LocalPal.current
    val ctx = LocalContext.current
    val now by rememberNow()
    val is24 = DateFormat.is24HourFormat(ctx)
    val hour = now.format(DateTimeFormatter.ofPattern(if (is24) "HH" else "hh"))
    val minute = now.format(DateTimeFormatter.ofPattern("mm"))
    val date = now.format(DateTimeFormatter.ofPattern("EEEE \u00B7 d MMMM", Locale.getDefault())).uppercase(Locale.getDefault())
    val size = if (compact) prefs.clockSize * 0.55f else prefs.clockSize
    val family = if (prefs.clockSerif) FontFamily.Serif else FontFamily.SansSerif
    val weight = if (prefs.clockSerif) FontWeight.Light else FontWeight.ExtraLight
    val style = TextStyle(
        color = pal.text, fontFamily = family, fontWeight = weight, fontSize = size.sp,
        lineHeight = (size * 0.95f).sp, letterSpacing = (-0.02f * size).sp,
        shadow = Shadow(pal.accent.copy(alpha = 0.4f), Offset.Zero, 36f),
    )
    Column(
        Modifier.padding(horizontal = 32.dp).padding(top = 20.dp).graphicsLayer {
            translationX = tilt.value.x * 12.dp.toPx()
            translationY = tilt.value.y * 8.dp.toPx()
        },
    ) {
        Text(date, color = pal.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        if (compact) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Digits(hour, style, prefs.animations)
                Text(":", style = style)
                Digits(minute, style, prefs.animations)
            }
        } else {
            Digits(hour, style, prefs.animations)
            Digits(minute, style, prefs.animations)
        }
    }
}

/** Digits roll in from above when the time changes. */
@Composable
private fun Digits(value: String, style: TextStyle, animate: Boolean) {
    if (!animate) {
        Text(value, style = style)
        return
    }
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            (slideInVertically(tween(520)) { -it / 2 } + fadeIn(tween(520))) togetherWith
                (slideOutVertically(tween(420)) { it / 2 } + fadeOut(tween(260)))
        },
        label = "digits",
    ) { Text(it, style = style) }
}

@Composable
private fun BatteryStrip(compact: Boolean) {
    val pal = LocalPal.current
    val battery by rememberBattery()
    val (pct, charging) = battery
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = if (compact) 8.dp else 20.dp)
            .glass(RoundedCornerShape(24.dp), 10.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Battery", color = pal.text, fontSize = 22.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic)
            Text(if (charging) "Charging" else "On battery", color = pal.sub, fontSize = 12.sp)
        }
        Text("$pct%", color = pal.text, fontSize = 40.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Light)
    }
}

@Composable
private fun PageDots(current: Int, count: Int) {
    val pal = LocalPal.current
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(count) { i ->
            val w by animateDpAsState(if (i == current) 14.dp else 5.dp, label = "dot")
            Box(
                Modifier.size(width = w, height = 5.dp).clip(CircleShape)
                    .background(if (i == current) pal.accent else pal.text.copy(alpha = 0.3f)),
            )
        }
    }
}

@Composable
private fun SearchPill(onClick: () -> Unit) {
    val pal = LocalPal.current
    val pill = RoundedCornerShape(50)
    Row(
        Modifier.padding(horizontal = 28.dp).fillMaxWidth().height(48.dp)
            .glass(pill, 8.dp)
            .clickable(onClickLabel = "Open app search", onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = pal.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(10.dp))
        Text("Search apps", color = pal.sub, fontSize = 14.sp)
    }
}

@Composable
private fun Dock(vm: LauncherViewModel, dockApps: List<AppInfo>, launch: (AppInfo, Rect?) -> Unit) {
    val pal = LocalPal.current
    val prefs = vm.prefs
    val shape = RoundedCornerShape(30.dp)
    val iconSize = minOf(prefs.iconSize, 60f).dp
    Row(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(84.dp).glass(shape, 16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dockApps.isEmpty()) {
            Text("Long-press an app in the drawer to add it here", color = pal.sub, fontSize = 12.sp)
        }
        dockApps.forEach { app ->
            key(app.key) {
                Box(
                    Modifier.onTapWithBounds { if (!vm.editMode) launch(app, it) }
                        .semantics { contentDescription = app.label },
                ) {
                    AppIconView(app, iconSize, prefs.iconShape, prefs.iconStyle)
                    if (vm.editMode) {
                        RemoveBadge(Modifier.align(Alignment.TopStart).offset(x = (-8).dp, y = (-8).dp)) { vm.removeDock(app.key) }
                    }
                }
            }
        }
    }
}

/** Background depth layer: brass rings that drift against the tilt and the page swipe. */
@Composable
private fun DepthRings(tilt: State<Offset>, pager: PagerState) {
    val pal = LocalPal.current
    Canvas(Modifier.fillMaxSize()) {
        val shift = (pager.currentPage + pager.currentPageOffsetFraction) * 90.dp.toPx()
        val ox = -tilt.value.x * 28.dp.toPx() - shift
        val oy = -tilt.value.y * 20.dp.toPx()
        val stroke = Stroke(1.dp.toPx())
        drawCircle(pal.accent.copy(alpha = 0.22f), size.width * 0.55f, Offset(size.width * 0.1f + ox, size.height * 0.42f + oy), style = stroke)
        drawCircle(pal.accent.copy(alpha = 0.12f), size.width * 0.38f, Offset(size.width * 0.18f + ox * 1.4f, size.height * 0.46f + oy * 1.4f), style = stroke)
    }
}