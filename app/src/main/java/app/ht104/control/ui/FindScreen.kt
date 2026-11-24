package app.ht104.control.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ht104.control.MainViewModel
import app.ht104.control.R
import app.ht104.control.core.formatTime
import app.ht104.control.data.PermissionHelper
import app.ht104.control.diagnostics.GattState
import app.ht104.control.protocol.Feature
import app.ht104.control.protocol.SupportLevel

@Composable
fun FindScreen(vm: MainViewModel, actions: UiActions, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val tick by vm.permissionTick.collectAsStateWithLifecycle()
    val caps by vm.capabilities.collectAsStateWithLifecycle()
    val gatt by vm.engine.gatt.snapshot.collectAsStateWithLifecycle()
    val lastSeen by vm.lastSeen.collectAsStateWithLifecycle()
    val message by vm.alertMessage.collectAsStateWithLifecycle()
    val active by vm.alertActive.collectAsStateWithLifecycle()
    val hasLocation = remember(tick) { PermissionHelper.allGranted(ctx, PermissionHelper.locationRuntime()) }

    val alertCap = caps[Feature.FIND_ALERT]
    val supported = alertCap?.level == SupportLevel.SUPPORTED
    val ready = gatt.state == GattState.READY

    LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard(stringResource(R.string.find_alert_title)) {
                CapabilityRow(Feature.FIND_ALERT, alertCap)
                CapabilityRow(Feature.FIND_LEFT_RIGHT, caps[Feature.FIND_LEFT_RIGHT])
                if (supported) {
                    if (!ready) Text(stringResource(R.string.find_need_gatt))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = ready, onClick = { vm.startAlert() }) { Text(stringResource(R.string.find_start)) }
                        OutlinedButton(enabled = ready, onClick = { vm.stopAlert() }) { Text(stringResource(R.string.find_stop)) }
                    }
                    if (active) Text(stringResource(R.string.find_active))
                    message?.let { Text(it) }
                    Text(stringResource(R.string.find_limit_note))
                } else {
                    Text(stringResource(R.string.find_unsupported))
                }
            }
        }
        item {
            SectionCard(stringResource(R.string.find_last_seen_title)) {
                KeyValueRow(stringResource(R.string.dash_last_connected), formatTime(lastSeen.connectedMs))
                KeyValueRow(stringResource(R.string.find_last_disconnected), formatTime(lastSeen.disconnectedMs))
                val loc = lastSeen.location
                if (loc != null) {
                    KeyValueRow(stringResource(R.string.find_phone_location), "%.5f, %.5f".format(loc.latitude, loc.longitude))
                    KeyValueRow(stringResource(R.string.find_location_time), formatTime(loc.timeMs))
                    OutlinedButton(onClick = { actions.openMap(loc.latitude, loc.longitude) }) {
                        Text(stringResource(R.string.find_open_map))
                    }
                } else {
                    Text(stringResource(R.string.find_no_location))
                }
                if (!hasLocation) {
                    Button(onClick = { actions.requestPermissions(PermissionHelper.locationRuntime()) }) {
                        Text(stringResource(R.string.find_grant_location))
                    }
                }
                Text(stringResource(R.string.find_location_disclaimer))
            }
        }
    }
}
