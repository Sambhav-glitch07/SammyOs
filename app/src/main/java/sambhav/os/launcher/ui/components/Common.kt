package sambhav.os.launcher.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sambhav.os.launcher.data.AppInfo
import sambhav.os.launcher.ui.theme.LocalPal

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun shapeFor(i: Int): Shape = when (i) {
    1 -> CircleShape
    2 -> RoundedCornerShape(6.dp)
    else -> RoundedCornerShape(30)
}

/** The launcher's own icon tile: user-chosen shape, brass hairline, optional themed (monochrome) style. */
@Composable
fun AppIconView(app: AppInfo, size: Dp, shape: Int, style: Int, modifier: Modifier = Modifier) {
    val pal = LocalPal.current
    val s = shapeFor(shape)
    val mono = if (style == 1) app.mono else null
    Box(
        modifier.size(size).clip(s)
            .background(Brush.linearGradient(listOf(pal.tileA, pal.tileB)))
            .border(0.8.dp, pal.accent.copy(alpha = 0.7f), s),
        contentAlignment = Alignment.Center,
    ) {
        if (mono != null) {
            Image(mono, null, Modifier.fillMaxSize().padding(size * 0.17f), colorFilter = ColorFilter.tint(pal.accent))
        } else {
            Image(app.icon, null, Modifier.fillMaxSize())
        }
    }
}

@Composable
fun Pill(text: String, filled: Boolean = false, onClick: () -> Unit) {
    val pal = LocalPal.current
    val pill = RoundedCornerShape(50)
    Box(
        Modifier.clip(pill)
            .background(if (filled) pal.accent else pal.surface.copy(alpha = 0.9f))
            .border(1.dp, pal.accent.copy(alpha = 0.6f), pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(text, color = if (filled) pal.bg else pal.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun RemoveBadge(modifier: Modifier, onClick: () -> Unit) {
    val pal = LocalPal.current
    Box(
        modifier.size(24.dp).clip(CircleShape).background(pal.surface)
            .border(1.dp, pal.accent.copy(alpha = 0.7f), CircleShape)
            .clickable(onClickLabel = "Remove", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text("\u2212", color = pal.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
}

/** Tap handler that also reports the view's on-screen bounds, used for the app-open animation. */
@Composable
fun Modifier.onTapWithBounds(onTap: (Rect) -> Unit): Modifier {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    return this
        .onGloballyPositioned { bounds = it.boundsInWindow() }
        .pointerInput(Unit) { detectTapGestures(onTap = { onTap(bounds) }) }
}
