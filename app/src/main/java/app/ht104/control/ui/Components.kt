package app.ht104.control.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ht104.control.R
import app.ht104.control.protocol.Capability
import app.ht104.control.protocol.Feature
import app.ht104.control.protocol.SupportLevel

@Composable
fun SectionCard(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
fun KeyValueRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun Mono(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(text, modifier = modifier, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = color)
}

@Composable
fun SupportBadge(level: SupportLevel) {
    val (res, bg) = when (level) {
        SupportLevel.SUPPORTED -> R.string.support_supported to MaterialTheme.colorScheme.primary
        SupportLevel.UNSUPPORTED -> R.string.support_unsupported to MaterialTheme.colorScheme.outline
        SupportLevel.UNVERIFIED -> R.string.support_unverified to MaterialTheme.colorScheme.secondary
    }
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            stringResource(res),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.surface,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** A feature line: localized title, support badge, and the technical evidence behind it. */
@Composable
fun CapabilityRow(feature: Feature, capability: Capability?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(featureTitle(feature)), fontWeight = FontWeight.Medium)
            if (capability != null) SupportBadge(capability.level) else SupportBadge(SupportLevel.UNVERIFIED)
        }
        Text(
            capability?.evidence ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@StringRes
fun featureTitle(f: Feature): Int = when (f) {
    Feature.BATTERY_LEVEL -> R.string.feat_battery_level
    Feature.BATTERY_CHARGING -> R.string.feat_battery_charging
    Feature.BATTERY_LEFT -> R.string.bat_left
    Feature.BATTERY_RIGHT -> R.string.bat_right
    Feature.BATTERY_CASE -> R.string.bat_case
    Feature.FIND_ALERT -> R.string.feat_find_alert
    Feature.FIND_LEFT_RIGHT -> R.string.feat_find_lr
    Feature.LINK_LOSS -> R.string.feat_link_loss
    Feature.DISCONNECT_GATT -> R.string.feat_disconnect_gatt
    Feature.DISCONNECT_CLASSIC -> R.string.feat_disconnect_classic
    Feature.REMOTE_POWER_OFF -> R.string.feat_power_off
    Feature.REMOTE_POWER_ON -> R.string.feat_power_on
    Feature.BUTTON_CONFIG -> R.string.feat_button_config
    Feature.ANC -> R.string.feat_anc
    Feature.TRANSPARENCY -> R.string.feat_transparency
    Feature.EQUALIZER -> R.string.feat_equalizer
    Feature.MIC_CONTROL -> R.string.feat_mic
    Feature.RENAME -> R.string.feat_rename
    Feature.FIRMWARE_INFO -> R.string.feat_firmware_info
    Feature.FIRMWARE_UPDATE -> R.string.feat_firmware_update
    Feature.SIGNAL_STRENGTH -> R.string.feat_signal
    Feature.MEDIA_CONTROL -> R.string.feat_media
    Feature.CALL_CONTROL -> R.string.feat_calls
}

/** Bluetooth rune drawn as a path so it needs no icon dependency or font glyph. */
@Composable
fun BluetoothGlyph(color: Color, size: Dp = 28.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        fun p(x: Float, y: Float) = Offset(x * w, y * h)
        val path = Path().apply {
            moveTo(p(0.22f, 0.30f).x, p(0.22f, 0.30f).y)
            lineTo(p(0.78f, 0.72f).x, p(0.78f, 0.72f).y)
            lineTo(p(0.50f, 0.95f).x, p(0.50f, 0.95f).y)
            lineTo(p(0.50f, 0.05f).x, p(0.50f, 0.05f).y)
            lineTo(p(0.78f, 0.28f).x, p(0.78f, 0.28f).y)
            lineTo(p(0.22f, 0.70f).x, p(0.22f, 0.70f).y)
        }
        drawPath(path, color, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
