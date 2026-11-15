package sambhav.os.launcher.ui.home

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.abs

/**
 * Smoothed device tilt in -1..1 for the depth effect. The accelerometer is only listened to while the
 * launcher is on screen (resumed), and not at all when the setting is off.
 */
@Composable
fun rememberTilt(enabled: Boolean): State<Offset> {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val tilt = remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(enabled, owner) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (!enabled || sm == null || sensor == null) {
            tilt.value = Offset.Zero
            return@DisposableEffect onDispose { }
        }
        var fx = 0f
        var fy = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val x = (-e.values[0] / 9.8f).coerceIn(-1f, 1f)
                val y = (e.values[1] / 9.8f).coerceIn(-1f, 1f)
                fx += (x - fx) * 0.08f
                fy += (y - fy) * 0.08f
                val t = tilt.value
                if (abs(fx - t.x) > 0.01f || abs(fy - t.y) > 0.01f) tilt.value = Offset(fx, fy)
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        val obs = LifecycleEventObserver { _, ev ->
            when (ev) {
                Lifecycle.Event.ON_RESUME -> sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
                Lifecycle.Event.ON_PAUSE -> sm.unregisterListener(listener)
                else -> {}
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs); sm.unregisterListener(listener) }
    }
    return tilt
}