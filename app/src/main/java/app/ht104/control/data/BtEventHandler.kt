package app.ht104.control.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.IntentCompat
import app.ht104.control.R

/** Manifest-declared receiver (best effort when the app is not running). */
class BtEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        BtEventHandler.handle(context, intent, action)
    }
}

object BtEventHandler {
    private var lastKey: String? = null
    private var lastAt = 0L

    @SuppressLint("MissingPermission")
    fun handle(context: Context, intent: Intent, action: String) {
        val device = IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        val address = device?.address
        val key = "$action|$address|${intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)}|" +
            "${intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)}|${intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)}"
        val now = System.currentTimeMillis()
        synchronized(this) {
            // The same event can arrive via both the manifest and the runtime receiver.
            if (key == lastKey && now - lastAt < 1500) return
            lastKey = key
            lastAt = now
        }
        val settings = SettingsStore(context)
        val isSelected = address != null && address == settings.selectedAddress
        val name = if (PermissionHelper.hasBluetoothConnect(context)) runCatching { device?.name }.getOrNull() else null
        val who = "${name ?: "?"} [${address ?: "-"}]"

        when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                ConnectionEventLog.add("ACL", "Connected: $who")
                if (isSelected) settings.lastConnectedMs = now
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                ConnectionEventLog.add("ACL", "Disconnected: $who")
                if (isSelected) {
                    settings.lastDisconnectedMs = now
                    if (settings.saveLocationOnDisconnect) LocationHelper.lastKnown(context)?.let { settings.lastLocation = it }
                    if (settings.notifyOnDisconnect) {
                        Notifier.notify(
                            context, 1001,
                            context.getString(R.string.notif_disconnect_title),
                            context.getString(R.string.notif_disconnect_text),
                        )
                    }
                }
            }
            BluetoothDevice.ACTION_BOND_STATE_CHANGED ->
                ConnectionEventLog.add("BOND", "$who bond state ${intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)}")
            BluetoothAdapter.ACTION_STATE_CHANGED ->
                ConnectionEventLog.add("ADAPTER", "Adapter state ${intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)}")
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED ->
                ConnectionEventLog.add("A2DP", "$who state ${intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)}")
            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED ->
                ConnectionEventLog.add("HFP", "$who state ${intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)}")
            BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT -> {
                val cmd = intent.getStringExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD)
                val args = intent.getSerializableExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS)
                val argText = (args as? Array<*>)?.joinToString(",") ?: args?.toString()
                ConnectionEventLog.add("HFP-VENDOR", "$who cmd=$cmd args=$argText")
            }
        }
    }
}
