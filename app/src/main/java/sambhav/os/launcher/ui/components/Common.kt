package sambhav.os.launcher.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.WindowManager
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
            .background(Brush.linearGradient(listOf(pal.tileA.copy(alpha = 0.85f), pal.tileB.copy(alpha = 0.85f))))
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.55f), pal.accent.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f))),
                s,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (mono != null) {
            Image(mono, null, Modifier.fillMaxSize().padding(size * 0.17f), colorFilter = ColorFilter.tint(pal.accent))
        } else {
            Image(app.icon, null, Modifier.fillMaxSize())
        }
        // Glass sheen across the top half of the tile.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.White.copy(alpha = 0.22f), 0.5f to Color.Transparent),
            ),
        )
    }
}

/** Frosted-glass surface: translucent gradient fill, bright top-left edge, soft depth shadow. */
@Composable
fun Modifier.glass(shape: Shape, elevation: Dp = 10.dp): Modifier {
    val pal = LocalPal.current
    val fill = if (pal.dark) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.17f), Color.White.copy(alpha = 0.06f)))
    else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.4f)))
    val edge = Brush.linearGradient(
        listOf(Color.White.copy(alpha = if (pal.dark) 0.55f else 0.95f), pal.accent.copy(alpha = 0.3f), Color.White.copy(alpha = 0.08f)),
    )
    return this
        .shadow(elevation, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.4f))
        .clip(shape).background(fill).border(1.dp, edge, shape)
}

/** True when the system can blur what is behind the launcher window (Android 12+, blur enabled). */
fun Context.blurSupported(): Boolean =
    Build.VERSION.SDK_INT >= 31 && getSystemService(WindowManager::class.java)?.isCrossWindowBlurEnabled == true

@Composable
fun Pill(text: String, filled: Boolean = false, onClick: () -> Unit) {
    val pal = LocalPal.current
    val pill = RoundedCornerShape(50)
    val base = if (filled) Modifier.clip(pill).background(pal.accent) else Modifier.glass(pill, 6.dp)
    Box(base.clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp)) {
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