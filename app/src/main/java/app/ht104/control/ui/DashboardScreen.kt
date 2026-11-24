package app.ht104.control.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ht104.control.MainViewModel
import app.ht104.control.R
import app.ht104.control.core.formatTime
import app.ht104.control.data.PermissionHelper
import app.ht104.control.diagnostics.GattState

@Composable
fun profileStateText(raw: String): String = when (raw) {
    "CONNECTED" -> stringResource(R.string.conn_connected)
    "DISCONNECTED" -> stringResource(R.string.conn_disconnected)
    "CONNECTING" -> stringResource(R.string.conn_connecting)
    "DISCONNECTING" -> stringResource(R.string.conn_disconnecting)
    else -> raw
}

@Composable
fun gattStateText(s: GattState): String = stringResource(
    when (s) {
        GattState.IDLE -> R.string.gatt_idle
        GattState.CONNECTING -> R.string.gatt_connecting
        GattState.CONNECTED -> R.string.gatt_connected
        GattState.DISCOVERING -> R.string.gatt_discovering
        GattState.READY -> R.string.gatt_ready
        GattState.DISCONNECTED -> R.string.gatt_disconnected
        GattState.FAILED -> R.string.gatt_failed
    },
)

@Composable
fun DashboardScreen(vm: MainViewModel, actions: UiActions, navigate: (Screen) -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val tick by vm.permissionTick.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val profiles by vm.engine.profiles.collectAsStateWithLifecycle()
    val gatt by vm.engine.gatt.snapshot.collectAsStateWithLifecycle()
    val readings by vm.batteryReadings.collectAsStateWithLifecycle()
    val bonded by vm.engine.bonded.collectAsStateWithLifecycle()
    val lastSeen by vm.lastSeen.collectAsStateWithLifecycle()
    var picker by remember { mutableStateOf(false) }

    val hasPermission = remember(tick) { PermissionHelper.allGranted(ctx, PermissionHelper.bluetoothRuntime()) }
    val classicConnected = profiles.any { (it.profile == "A2DP" || it.profile == "HFP") && it.state == "CONNECTED" }
    val statusText = when {
        !hasPermission -> stringResource(R.string.status_permission_needed)
        selected == null -> stringResource(R.string.status_no_headset)
        classicConnected -> stringResource(R.string.conn_connected)
        profiles.isNotEmpty() -> stringResource(R.string.conn_disconnected)
        else -> stringResource(R.string.conn_unknown)
    }
    val percent = readings.firstOrNull { it.percent != null }?.percent

    LazyColumn(
        modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        selected?.name ?: "HT-EP104",
                        Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                    )
                    Text(
                        stringResource(R.string.dash_tagline),
                        Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                    Box(
                        Modifier.fillMaxWidth(),
                    ) {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 26.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                BluetoothGlyph(MaterialTheme.colorScheme.onPrimary, 30.dp)
                                Text(
                                    statusText,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                )
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text(
                            if (percent != null) "$percent%" else "--%",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                        )
                        Text(stringResource(R.string.dash_battery), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (percent == null) {
                        Text(
                            stringResource(R.string.dash_battery_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (!hasPermission) {
            item {
                SectionCard {
                    Text(stringResource(R.string.perm_bt_needed))
                    Button(onClick = { actions.requestPermissions(PermissionHelper.bluetoothRuntime()) }) {
                        Text(stringResource(R.string.action_grant))
                    }
                }
            }
        }

        item {
            SectionCard(stringResource(R.string.dash_headset)) {
                if (selected == null) {
                    Text(stringResource(R.string.dash_no_headset))
                } else {
                    KeyValueRow(stringResource(R.string.dash_address), selected?.address ?: "")
                    profiles.firstOrNull { it.profile == "A2DP" }?.let {
                        KeyValueRow(stringResource(R.string.dash_a2dp), profileStateText(it.state))
                    }
                    profiles.firstOrNull { it.profile == "HFP" }?.let {
                        KeyValueRow(stringResource(R.string.dash_hfp), profileStateText(it.state))
                    }
                    KeyValueRow(stringResource(R.string.dash_gatt), gattStateText(gatt.state))
                    KeyValueRow(stringResource(R.string.dash_signal), gatt.rssi?.let { "$it dBm" } ?: "—")
                    KeyValueRow(stringResource(R.string.dash_last_connected), formatTime(lastSeen.connectedMs))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { picker = true; vm.refreshAll() }, enabled = hasPermission) {
                        Text(stringResource(if (selected == null) R.string.dash_select else R.string.dash_change))
                    }
                    OutlinedButton(onClick = actions.openBluetoothSettings) {
                        Text(stringResource(R.string.dash_bt_settings))
                    }
                }
                if (selected != null) {
                    val live = gatt.state == GattState.READY || gatt.state == GattState.CONNECTING || gatt.state == GattState.DISCOVERING
                    OutlinedButton(onClick = { if (live) vm.disconnectGatt() else vm.connectGatt() }) {
                        Text(stringResource(if (live) R.string.dash_disconnect_gatt else R.string.dash_connect_gatt))
                    }
                }
            }
        }

        val tiles = listOf(Screen.Battery, Screen.Audio, Screen.Calls, Screen.Find, Screen.Settings, Screen.Diagnostics, Screen.About)
        items(tiles.chunked(2)) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { s ->
                    Card(
                        onClick = { navigate(s) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Text(
                            stringResource(s.title),
                            Modifier.padding(18.dp),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                if (row.size == 1) Box(Modifier.weight(1f)) {}
            }
        }
        item { Box(Modifier.padding(8.dp)) {} }
    }

    if (picker) {
        AlertDialog(
            onDismissRequest = { picker = false },
            title = { Text(stringResource(R.string.dash_select)) },
            text = {
                if (bonded.isEmpty()) {
                    Text(stringResource(R.string.dash_no_bonded))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        bonded.forEach { d ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        vm.select(d.address, d.name)
                                        picker = false
                                    }
                                    .padding(vertical = 8.dp),
                            ) {
                                Text(
                                    (d.name ?: "?") + if (d.looksLikeTarget) "  ★" else "",
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Mono(d.address, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { picker = false }) { Text(stringResource(R.string.action_close)) } },
        )
    }
}
