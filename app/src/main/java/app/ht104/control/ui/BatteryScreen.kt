package app.ht104.control.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ht104.control.MainViewModel
import app.ht104.control.R
import app.ht104.control.protocol.BatteryReading
import app.ht104.control.protocol.ChargeState
import app.ht104.control.protocol.Feature
import app.ht104.control.protocol.SupportLevel

@Composable
fun BatteryScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val readings by vm.batteryReadings.collectAsStateWithLifecycle()
    val caps by vm.capabilities.collectAsStateWithLifecycle()

    LazyColumn(
        modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(stringResource(R.string.bat_components)) {
                listOf(Feature.BATTERY_LEFT, Feature.BATTERY_RIGHT, Feature.BATTERY_CASE).forEach { f ->
                    val cap = caps[f]
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(featureTitle(f)), fontWeight = FontWeight.Medium)
                        Text(
                            stringResource(if (cap?.level == SupportLevel.UNSUPPORTED) R.string.support_unsupported else R.string.support_unverified),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    cap?.let {
                        Text(it.evidence, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(stringResource(R.string.bat_note), style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            SectionCard(stringResource(R.string.bat_charging_title)) {
                val cap = caps[Feature.BATTERY_CHARGING]
                if (cap != null) CapabilityRow(Feature.BATTERY_CHARGING, cap)
            }
        }
        if (readings.isEmpty()) {
            item {
                SectionCard(stringResource(R.string.bat_sources)) { Text(stringResource(R.string.dash_battery_unavailable)) }
            }
        } else {
            item { Text(stringResource(R.string.bat_sources), style = MaterialTheme.typography.titleMedium) }
            items(readings) { ReadingCard(it) }
        }
        item {
            Button(onClick = { vm.refreshBatteryNow() }) { Text(stringResource(R.string.bat_refresh_now)) }
        }
    }
}

@Composable
private fun ReadingCard(r: BatteryReading) {
    SectionCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(r.percent?.let { "$it%" } ?: "--%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            val chargeText = when (r.chargeState) {
                ChargeState.CHARGING -> "⚡ " + stringResource(R.string.bat_charging)
                ChargeState.DISCHARGING_ACTIVE, ChargeState.DISCHARGING_INACTIVE -> stringResource(R.string.bat_discharging)
                ChargeState.UNKNOWN -> stringResource(R.string.bat_charge_unknown)
            }
            Text(chargeText)
        }
        r.percent?.let { LinearProgressIndicator(progress = { it / 100f }, modifier = Modifier.fillMaxWidth()) }
        if (r.percent == 100) Text(stringResource(R.string.bat_full), fontWeight = FontWeight.SemiBold)
        Text(r.source, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column { Mono(r.evidence, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
