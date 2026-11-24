package app.ht104.control.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ht104.control.MainViewModel
import app.ht104.control.R
import app.ht104.control.control.CallState
import app.ht104.control.data.PermissionHelper
import kotlinx.coroutines.delay

@Composable
fun CallsScreen(vm: MainViewModel, actions: UiActions, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val tick by vm.permissionTick.collectAsStateWithLifecycle()
    val probeActive by vm.buttonProbe.active.collectAsStateWithLifecycle()
    val probeLog by vm.buttonProbe.log.collectAsStateWithLifecycle()
    var state by remember { mutableStateOf(CallState.IDLE) }
    var muted by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val hasCallPermission = remember(tick) { PermissionHelper.allGranted(ctx, PermissionHelper.callRuntime()) }

    LaunchedEffect(Unit) {
        while (true) {
            state = vm.calls.state()
            muted = vm.calls.isMicMuted()
            delay(1000)
        }
    }

    val failed = stringResource(R.string.calls_action_failed)

    LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard(stringResource(R.string.calls_title)) {
                KeyValueRow(
                    stringResource(R.string.calls_state),
                    stringResource(
                        when (state) {
                            CallState.IDLE -> R.string.calls_idle
                            CallState.RINGING -> R.string.calls_ringing
                            CallState.IN_CALL -> R.string.calls_in_call
                        },
                    ),
                )
                if (!hasCallPermission) {
                    Text(stringResource(R.string.calls_permission))
                    Button(onClick = { actions.requestPermissions(PermissionHelper.callRuntime()) }) {
                        Text(stringResource(R.string.action_grant))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = hasCallPermission && state == CallState.RINGING,
                        onClick = { message = if (vm.calls.answer()) null else failed },
                    ) { Text(stringResource(R.string.calls_answer)) }
                    OutlinedButton(
                        enabled = hasCallPermission && vm.calls.endCallSupported() && state == CallState.RINGING,
                        onClick = { message = if (vm.calls.endOrReject()) null else failed },
                    ) { Text(stringResource(R.string.calls_reject)) }
                    OutlinedButton(
                        enabled = hasCallPermission && vm.calls.endCallSupported() && state == CallState.IN_CALL,
                        onClick = { message = if (vm.calls.endOrReject()) null else failed },
                    ) { Text(stringResource(R.string.calls_end)) }
                }
                if (!vm.calls.endCallSupported()) Text(stringResource(R.string.calls_end_unsupported))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.calls_mute))
                    Switch(
                        checked = muted,
                        enabled = state == CallState.IN_CALL,
                        onCheckedChange = {
                            vm.calls.setMicMuted(it)
                            muted = vm.calls.isMicMuted()
                        },
                    )
                }
                Text(stringResource(R.string.calls_mute_note))
                message?.let { Text(it) }
            }
        }
        item {
            SectionCard(stringResource(R.string.probe_title)) {
                Text(stringResource(R.string.probe_desc))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { if (probeActive) vm.buttonProbe.stop() else vm.buttonProbe.start() }) {
                        Text(stringResource(if (probeActive) R.string.probe_stop else R.string.probe_start))
                    }
                    OutlinedButton(onClick = { vm.buttonProbe.clear() }) { Text(stringResource(R.string.probe_clear)) }
                }
                Text(stringResource(R.string.probe_call_note))
            }
        }
        items(probeLog.takeLast(40).reversed()) { line -> Mono(line, Modifier.fillMaxWidth()) }
    }
}
