package app.ht104.control.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ht104.control.MainViewModel
import app.ht104.control.R
import app.ht104.control.data.PermissionHelper
import app.ht104.control.diagnostics.GattState
import app.ht104.control.protocol.Feature

@Composable
fun SettingsScreen(vm: MainViewModel, actions: UiActions, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val caps by vm.capabilities.collectAsStateWithLifecycle()
    val gatt by vm.engine.gatt.snapshot.collectAsStateWithLifecycle()
    val refresh by vm.refreshSeconds.collectAsStateWithLifecycle()
    val threshold by vm.lowThreshold.collectAsStateWithLifecycle()
    val notifyDisc by vm.notifyDisconnect.collectAsStateWithLifecycle()
    val saveLoc by vm.saveLocation.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val currentLang = vm.settings.language
    val gattLive = gatt.state == GattState.READY || gatt.state == GattState.CONNECTING || gatt.state == GattState.DISCOVERING

    LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard(stringResource(R.string.set_language)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = currentLang == "system", onClick = { actions.setLanguage("system") },
                        label = { Text(stringResource(R.string.lang_system)) })
                    FilterChip(selected = currentLang == "en", onClick = { actions.setLanguage("en") },
                        label = { Text("English") })
                    FilterChip(selected = currentLang == "ar", onClick = { actions.setLanguage("ar") },
                        label = { Text("العربية") })
                }
            }
        }
        item {
            SectionCard(stringResource(R.string.set_power)) {
                Text(stringResource(R.string.set_power_note))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = gattLive, onClick = { vm.disconnectGatt() }) {
                        Text(stringResource(R.string.set_disconnect_gatt))
                    }
                    Button(enabled = !gattLive && selected != null, onClick = { vm.connectGatt() }) {
                        Text(stringResource(R.string.set_reconnect_gatt))
                    }
                }
                OutlinedButton(onClick = actions.openBluetoothSettings) { Text(stringResource(R.string.set_classic_via_system)) }
                CapabilityRow(Feature.DISCONNECT_CLASSIC, caps[Feature.DISCONNECT_CLASSIC])
                CapabilityRow(Feature.REMOTE_POWER_OFF, caps[Feature.REMOTE_POWER_OFF])
                CapabilityRow(Feature.REMOTE_POWER_ON, caps[Feature.REMOTE_POWER_ON])
            }
        }
        item {
            SectionCard(stringResource(R.string.set_monitoring)) {
                Text(stringResource(R.string.set_refresh_interval, refresh))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 60, 120).forEach { s ->
                        FilterChip(selected = refresh == s, onClick = { vm.setRefreshSeconds(s) }, label = { Text("${s}s") })
                    }
                }
                Text(stringResource(R.string.set_low_threshold, threshold))
                Slider(
                    value = threshold.toFloat(),
                    onValueChange = { vm.setLowThreshold(it.toInt()) },
                    valueRange = 5f..50f,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.set_notify_disconnect))
                    Switch(checked = notifyDisc, onCheckedChange = {
                        if (it) actions.requestPermissions(PermissionHelper.notificationRuntime())
                        vm.setNotifyDisconnect(it)
                    })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.set_save_location))
                    Switch(checked = saveLoc, onCheckedChange = {
                        if (it && !PermissionHelper.allGranted(ctx, PermissionHelper.locationRuntime())) {
                            actions.requestPermissions(PermissionHelper.locationRuntime())
                        }
                        vm.setSaveLocation(it)
                    })
                }
                Text(stringResource(R.string.set_background_note))
            }
        }
        item {
            SectionCard(stringResource(R.string.set_advanced)) {
                listOf(
                    Feature.FIRMWARE_INFO, Feature.BUTTON_CONFIG, Feature.ANC, Feature.TRANSPARENCY,
                    Feature.EQUALIZER, Feature.MIC_CONTROL, Feature.RENAME, Feature.FIRMWARE_UPDATE,
                    Feature.LINK_LOSS, Feature.SIGNAL_STRENGTH,
                ).forEach { CapabilityRow(it, caps[it]) }
            }
        }
    }
}
