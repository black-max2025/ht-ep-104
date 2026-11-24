package app.ht104.control.ui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ht104.control.MainViewModel
import app.ht104.control.R
import app.ht104.control.core.Uuids
import app.ht104.control.core.formatTime
import app.ht104.control.core.toPrintableText
import app.ht104.control.data.ConnectionEventLog
import app.ht104.control.data.PermissionHelper
import app.ht104.control.diagnostics.BleAdvertisement
import app.ht104.control.diagnostics.GattState
import app.ht104.control.diagnostics.GattStatus
import app.ht104.control.diagnostics.ServiceDump

@Composable
fun DiagnosticsScreen(vm: MainViewModel, actions: UiActions, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val tick by vm.permissionTick.collectAsStateWithLifecycle()
    val adapter by vm.engine.adapter.collectAsStateWithLifecycle()
    val bonded by vm.engine.bonded.collectAsStateWithLifecycle()
    val classicError by vm.engine.classicError.collectAsStateWithLifecycle()
    val profiles by vm.engine.profiles.collectAsStateWithLifecycle()
    val scanning by vm.engine.scanner.scanning.collectAsStateWithLifecycle()
    val scanError by vm.engine.scanner.error.collectAsStateWithLifecycle()
    val adverts by vm.engine.scanner.results.collectAsStateWithLifecycle()
    val gatt by vm.engine.gatt.snapshot.collectAsStateWithLifecycle()
    val events by ConnectionEventLog.entries.collectAsStateWithLifecycle()
    val systemBattery by vm.systemBattery.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val permissions = remember(tick) { PermissionHelper.statuses(ctx) }

    LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard(stringResource(R.string.diag_export)) {
                Text(stringResource(R.string.diag_export_desc))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.export(false).let { (f, m) -> actions.share(f, m) } }) { Text("TXT") }
                    OutlinedButton(onClick = { vm.export(true).let { (f, m) -> actions.share(f, m) } }) { Text("JSON") }
                }
            }
        }
        item {
            SectionCard(stringResource(R.string.diag_environment)) {
                KeyValueRow(stringResource(R.string.diag_device), "${Build.MANUFACTURER} ${Build.MODEL}")
                KeyValueRow("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                adapter?.let {
                    KeyValueRow(stringResource(R.string.diag_bt_enabled), it.enabled.toString())
                    KeyValueRow("BLE", it.leFeature.toString())
                    KeyValueRow("LE 2M PHY", it.le2mPhy.toString())
                    KeyValueRow("LE extended adv.", it.leExtendedAdvertising.toString())
                }
                permissions.forEach { p ->
                    Mono("${if (p.granted) "[ok]    " else "[denied]"} ${p.permission.substringAfterLast('.')}")
                }
                Button(onClick = { actions.requestPermissions(PermissionHelper.bluetoothRuntime()) }) {
                    Text(stringResource(R.string.action_grant))
                }
            }
        }
        item {
            SectionCard(stringResource(R.string.diag_classic)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.refreshAll() }) { Text(stringResource(R.string.action_refresh)) }
                }
                classicError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                profiles.forEach { KeyValueRow(it.profile, it.state) }
                KeyValueRow(stringResource(R.string.diag_system_battery), systemBattery?.let { "$it%" } ?: "—")
                if (bonded.isEmpty()) Text(stringResource(R.string.dash_no_bonded))
            }
        }
        items(bonded) { d ->
            SectionCard(d.name ?: "?") {
                Mono(d.address)
                Text("${d.bondState} • ${d.deviceType} • ${d.deviceClass ?: "-"}", style = MaterialTheme.typography.bodySmall)
                d.uuids.forEach { Mono("${it.uuid}${it.name?.let { n -> "  ($n)" } ?: ""}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                OutlinedButton(onClick = { vm.select(d.address, d.name) }, enabled = selected?.address != d.address) {
                    Text(stringResource(R.string.diag_use_device))
                }
            }
        }
        item {
            SectionCard(stringResource(R.string.diag_ble)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { if (scanning) vm.engine.scanner.stop() else vm.engine.scanner.start() }) {
                        Text(stringResource(if (scanning) R.string.diag_scan_stop else R.string.diag_scan_start))
                    }
                }
                scanError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text(stringResource(R.string.diag_scan_note), style = MaterialTheme.typography.bodySmall)
            }
        }
        items(adverts.values.sortedWith(compareByDescending<BleAdvertisement> { it.looksLikeTarget }.thenByDescending { it.rssi }).take(60)) { a ->
            AdvertCard(a, onConnect = { vm.connectGatt(a.address) })
        }
        item {
            SectionCard(stringResource(R.string.diag_gatt)) {
                KeyValueRow(stringResource(R.string.diag_state), gattStateText(gatt.state))
                KeyValueRow(stringResource(R.string.dash_address), gatt.address ?: "—")
                KeyValueRow("RSSI", gatt.rssi?.let { "$it dBm" } ?: "—")
                KeyValueRow("MTU", gatt.mtu?.toString() ?: "—")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = selected != null, onClick = { vm.connectGatt() }) { Text(stringResource(R.string.dash_connect_gatt)) }
                    OutlinedButton(enabled = gatt.state != GattState.IDLE, onClick = { vm.disconnectGatt() }) {
                        Text(stringResource(R.string.dash_disconnect_gatt))
                    }
                }
                if (gatt.errors.isNotEmpty()) {
                    Text(stringResource(R.string.diag_errors), fontWeight = FontWeight.SemiBold)
                    gatt.errors.takeLast(12).forEach { Mono(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        items(gatt.services) { ServiceCard(it) }
        item {
            SectionCard(stringResource(R.string.diag_events)) {
                if (events.isEmpty()) Text("—")
                events.takeLast(40).reversed().forEach { Mono("${formatTime(it.timeMs)} [${it.source}] ${it.message}") }
            }
        }
    }
}

@Composable
private fun AdvertCard(a: BleAdvertisement, onConnect: () -> Unit) {
    SectionCard((a.name ?: "(no name)") + if (a.looksLikeTarget) "  ★" else "") {
        Mono(a.address)
        Text("RSSI ${a.rssi} dBm • connectable=${a.connectable} • tx=${a.txPower ?: "-"} • seen ${a.sightings}x", style = MaterialTheme.typography.bodySmall)
        a.serviceUuids.forEach { Mono("service ${it.uuid}${it.name?.let { n -> " ($n)" } ?: ""}") }
        a.manufacturerData.forEach { (id, hex) -> Mono("manufacturer 0x%04X: %s".format(id, hex)) }
        a.serviceData.forEach { (u, hex) -> Mono("service-data $u: $hex") }
        if (a.connectable) OutlinedButton(onClick = onConnect) { Text(stringResource(R.string.dash_connect_gatt)) }
    }
}

@Composable
private fun ServiceCard(s: ServiceDump) {
    SectionCard("${Uuids.display(s.uuid)} ${s.name ?: ""}".trim()) {
        if (Uuids.isVendorSpecific(s.uuid)) Mono(s.uuid.toString())
        s.characteristics.forEach { c ->
            Column {
                Mono("${Uuids.display(c.uuid)} ${c.name ?: ""} [${c.properties.joinToString("|")}]")
                if (c.value != null) {
                    Mono("  = ${c.valueHex}" + (c.value.toPrintableText()?.let { "  \"$it\"" } ?: ""), color = MaterialTheme.colorScheme.primary)
                } else if (c.readStatus != null) {
                    Mono("  read failed: ${GattStatus.describe(c.readStatus)}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
