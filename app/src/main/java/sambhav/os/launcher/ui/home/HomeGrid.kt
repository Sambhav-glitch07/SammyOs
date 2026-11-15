package sambhav.os.launcher.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import sambhav.os.launcher.data.AppInfo
import sambhav.os.launcher.ui.components.AppIconView
import sambhav.os.launcher.ui.components.RemoveBadge
import sambhav.os.launcher.ui.theme.LocalPal
import kotlin.math.roundToInt

/**
 * Home page icon grid. Press and hold an icon to enter edit mode and drag it to a new slot;
 * the other icons spring out of the way. In edit mode icons jiggle and show a remove badge.
 */
@Composable
fun HomeGrid(
    apps: List<AppInfo>,
    cols: Int,
    rows: Int,
    iconSize: Dp,
    labels: Boolean,
    shape: Int,
    style: Int,
    edit: Boolean,
    animate: Boolean,
    onEnterEdit: () -> Unit,
    onMove: (String, Int) -> Unit,
    onRemove: (AppInfo) -> Unit,
    onLaunch: (AppInfo, Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pal = LocalPal.current
    val haptic = LocalHapticFeedback.current
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val cellH = iconSize + 38.dp
        val cellW = maxWidth / cols
        val fitRows = (maxHeight / cellH).toInt().coerceAtLeast(1)
        val rowsShown = minOf(rows, fitRows)
        val shown = apps.take(cols * rowsShown)
        val cellWpx = with(density) { cellW.toPx() }
        val cellHpx = with(density) { cellH.toPx() }
        val editNow by rememberUpdatedState(edit)
        val shownSize by rememberUpdatedState(shown.size)
        var dragKey by remember { mutableStateOf<String?>(null) }
        var dragPos by remember { mutableStateOf(Offset.Zero) }

        shown.forEachIndexed { i, app ->
            key(app.key) {
                val idx by rememberUpdatedState(i)
                val target = IntOffset(((i % cols) * cellWpx).roundToInt(), ((i / cols) * cellHpx).roundToInt())
                val slot by animateIntOffsetAsState(
                    target,
                    spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold),
                    label = "slot",
                )
                val dragging = dragKey == app.key
                val jiggle = if (edit && !dragging && animate)
                    rememberInfiniteTransition(label = "jiggle").animateFloat(
                        -1.8f, 1.8f,
                        infiniteRepeatable(tween(130 + (i % 3) * 25, easing = LinearEasing), RepeatMode.Reverse),
                        label = "rot",
                    )
                else null
                var bounds by remember { mutableStateOf(Rect.Zero) }
                var pressed by remember { mutableStateOf(false) }
                val pressScale by animateFloatAsState(
                    if (pressed) 0.9f else 1f,
                    spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                    label = "press",
                )

                Box(
                    Modifier
                        .offset { if (dragging) IntOffset(dragPos.x.roundToInt(), dragPos.y.roundToInt()) else slot }
                        .size(cellW, cellH)
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer {
                            val sc = if (dragging) 1.12f else pressScale
                            scaleX = sc
                            scaleY = sc
                            rotationZ = jiggle?.value ?: 0f
                        }
                        .onGloballyPositioned { bounds = it.boundsInWindow() }
                        .semantics { contentDescription = app.label }
                        .pointerInput(app.key) {
                            detectTapGestures(
                                onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                                onTap = { if (!editNow) onLaunch(app, bounds) },
                            )
                        }
                        .pointerInput(app.key, cols, cellWpx, cellHpx, rowsShown) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    dragKey = app.key
                                    dragPos = Offset((idx % cols) * cellWpx, (idx / cols) * cellHpx)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onEnterEdit()
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragPos += amount
                                    val col = ((dragPos.x + cellWpx / 2) / cellWpx).toInt().coerceIn(0, cols - 1)
                                    val row = ((dragPos.y + cellHpx / 2) / cellHpx).toInt().coerceIn(0, rowsShown - 1)
                                    val ni = (row * cols + col).coerceAtMost(shownSize - 1)
                                    if (ni != idx) onMove(app.key, ni)
                                },
                                onDragEnd = { dragKey = null },
                                onDragCancel = { dragKey = null },
                            )
                        },
                ) {
                    Column(Modifier.fillMaxSize().padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        AppIconView(app, iconSize, shape, style)
                        if (labels) {
                            Text(
                                app.label, color = pal.text, fontSize = 11.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp),
                            )
                        }
                    }
                    if (edit && !dragging) {
                        RemoveBadge(
                            Modifier.align(Alignment.TopStart).offset(x = (cellW - iconSize) / 2 - 8.dp, y = 0.dp),
                        ) { onRemove(app) }
                    }
                }
            }
        }
    }
}