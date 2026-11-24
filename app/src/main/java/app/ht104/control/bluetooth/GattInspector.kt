package app.ht104.control.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import app.ht104.control.core.Uuids
import app.ht104.control.data.ConnectionEventLog
import app.ht104.control.diagnostics.CharacteristicDump
import app.ht104.control.diagnostics.DescriptorDump
import app.ht104.control.diagnostics.GattSnapshot
import app.ht104.control.diagnostics.GattState
import app.ht104.control.diagnostics.GattStatus
import app.ht104.control.diagnostics.ServiceDump
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private class ReadResult(val value: ByteArray?, val status: Int)

/**
 * Connects over BLE GATT, enumerates every service/characteristic/descriptor and READS values.
 *
 * Writes are structurally limited to the standard Alert Level characteristic (0x2A06) inside the
 * Immediate Alert Service (0x1802). There is no generic write API, so unknown characteristics can
 * never be written by this class.
 */
@SuppressLint("MissingPermission")
class GattInspector(private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private val _snapshot = MutableStateFlow(GattSnapshot.IDLE)
    val snapshot: StateFlow<GattSnapshot> = _snapshot.asStateFlow()

    private var gatt: BluetoothGatt? = null
    private var address: String? = null
    private var state = GattState.IDLE
    private var rssi: Int? = null
    private var mtu: Int? = null
    private val errors = ArrayList<String>()
    private val charValues = HashMap<BluetoothGattCharacteristic, ReadResult>()
    private val descValues = HashMap<BluetoothGattDescriptor, ReadResult>()
    private val readQueue = ArrayDeque<Any>()
    private var reading = false
    private var current: Any? = null
    private var lastServices: List<ServiceDump> = emptyList()

    private val timeout = Runnable {
        val item = current ?: return@Runnable
        record(item, null, -1)
        finishRead()
    }

    fun connect(targetAddress: String) {
        disconnect()
        errors.clear()
        charValues.clear()
        descValues.clear()
        lastServices = emptyList()
        rssi = null
        mtu = null
        address = targetAddress
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        val device = try {
            adapter?.getRemoteDevice(targetAddress)
        } catch (e: IllegalArgumentException) {
            null
        }
        if (device == null) {
            addError("Invalid address or Bluetooth adapter unavailable: $targetAddress")
            setState(GattState.FAILED)
            return
        }
        setState(GattState.CONNECTING)
        try {
            gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
            if (gatt == null) {
                addError("connectGatt returned null")
                setState(GattState.FAILED)
            }
        } catch (e: SecurityException) {
            addError("Missing BLUETOOTH_CONNECT permission: ${e.message}")
            setState(GattState.FAILED)
        }
    }

    fun disconnect() {
        main.removeCallbacks(timeout)
        readQueue.clear()
        reading = false
        current = null
        gatt?.let {
            runCatching { it.disconnect() }
            runCatching { it.close() }
        }
        gatt = null
        if (state != GattState.IDLE && state != GattState.FAILED) setState(GattState.DISCONNECTED)
    }

    fun readRssi() {
        runCatching { gatt?.readRemoteRssi() }
    }

    /** Re-reads battery characteristics (0x2A19 and 0x2BED). Read-only. */
    fun refreshBattery() {
        val g = gatt ?: return
        if (state != GattState.READY) return
        for (s in g.services) {
            if (s.uuid != Uuids.BATTERY_SERVICE) continue
            for (c in s.characteristics) {
                if ((c.uuid == Uuids.BATTERY_LEVEL || c.uuid == Uuids.BATTERY_LEVEL_STATUS) &&
                    (c.properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0 && c !in readQueue
                ) {
                    readQueue.addLast(c)
                }
            }
        }
        readNext()
    }

    /**
     * Writes the standard Alert Level (0x2A06) of the Immediate Alert Service using write-without-response.
     * Returns null on success or a human-readable error. The characteristic is write-only, so the result
     * cannot be read back from the device.
     */
    fun writeImmediateAlert(level: Int): String? {
        val g = gatt ?: return "GATT is not connected"
        if (state != GattState.READY) return "GATT is not ready"
        val svc = g.services.firstOrNull { it.uuid == Uuids.IMMEDIATE_ALERT } ?: return "Immediate Alert Service not found"
        val c = svc.characteristics.firstOrNull { it.uuid == Uuids.ALERT_LEVEL } ?: return "Alert Level characteristic not found"
        if ((c.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) {
            return "Alert Level does not support write without response"
        }
        val payload = byteArrayOf(level.toByte())
        val ok = try {
            if (Build.VERSION.SDK_INT >= 33) {
                g.writeCharacteristic(c, payload, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) == 0
            } else {
                legacyWriteNoResponse(g, c, payload)
            }
        } catch (e: SecurityException) {
            return "Missing permission: ${e.message}"
        }
        ConnectionEventLog.add("GATT-WRITE", "Alert Level 0x2A06 <- %02X (write without response), accepted=%s".format(level, ok))
        return if (ok) null else "Android rejected the write request"
    }

    @Suppress("DEPRECATION")
    private fun legacyWriteNoResponse(g: BluetoothGatt, c: BluetoothGattCharacteristic, payload: ByteArray): Boolean {
        c.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        c.value = payload
        return g.writeCharacteristic(c)
    }

    private fun setState(s: GattState) {
        state = s
        publish()
    }

    private fun addError(message: String) {
        errors.add(message)
        ConnectionEventLog.add("GATT", message)
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            main.post {
                if (g !== gatt) {
                    runCatching { g.close() }
                    return@post
                }
                if (status != BluetoothGatt.GATT_SUCCESS) addError("Connection state change: ${GattStatus.describe(status)}")
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        setState(GattState.DISCOVERING)
                        g.readRemoteRssi()
                        if (!g.discoverServices()) addError("discoverServices() refused to start")
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        main.removeCallbacks(timeout)
                        readQueue.clear()
                        reading = false
                        current = null
                        runCatching { g.close() }
                        gatt = null
                        setState(if (status == BluetoothGatt.GATT_SUCCESS) GattState.DISCONNECTED else GattState.FAILED)
                    }
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            main.post {
                if (g !== gatt) return@post
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    addError("Service discovery: ${GattStatus.describe(status)}")
                    setState(GattState.FAILED)
                    return@post
                }
                for (s in g.services) {
                    for (c in s.characteristics) {
                        if ((c.properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0) readQueue.addLast(c)
                        for (d in c.descriptors) {
                            if (d.uuid == Uuids.CHAR_USER_DESCRIPTION || d.uuid == Uuids.CHAR_PRESENTATION_FORMAT) {
                                readQueue.addLast(d)
                            }
                        }
                    }
                }
                setState(GattState.READY)
                readNext()
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            @Suppress("DEPRECATION")
            val v = c.value
            main.post { handleResult(c, v, status) }
        }

        override fun onCharacteristicRead(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            main.post { handleResult(c, value, status) }
        }

        @Deprecated("Deprecated in Java")
        override fun onDescriptorRead(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
            @Suppress("DEPRECATION")
            val v = d.value
            main.post { handleResult(d, v, status) }
        }

        override fun onDescriptorRead(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int, value: ByteArray) {
            main.post { handleResult(d, value, status) }
        }

        override fun onReadRemoteRssi(g: BluetoothGatt, rssiValue: Int, status: Int) {
            main.post {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    rssi = rssiValue
                    publish()
                }
            }
        }

        override fun onMtuChanged(g: BluetoothGatt, mtuValue: Int, status: Int) {
            main.post {
                mtu = mtuValue
                publish()
            }
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            main.post {
                ConnectionEventLog.add("GATT-WRITE", "Write result for ${Uuids.display(c.uuid)}: ${GattStatus.describe(status)}")
            }
        }
    }

    private fun handleResult(item: Any, value: ByteArray?, status: Int) {
        record(item, value, status)
        if (item === current) finishRead()
    }

    private fun record(item: Any, value: ByteArray?, status: Int) {
        val result = ReadResult(value?.copyOf(), status)
        when (item) {
            is BluetoothGattCharacteristic -> charValues[item] = result
            is BluetoothGattDescriptor -> descValues[item] = result
        }
        if (status != BluetoothGatt.GATT_SUCCESS) {
            val uuid = when (item) {
                is BluetoothGattCharacteristic -> item.uuid
                is BluetoothGattDescriptor -> item.uuid
                else -> null
            }
            addError("Read of ${uuid?.let { Uuids.display(it) }}: ${GattStatus.describe(status)}")
        }
    }

    private fun readNext() {
        val g = gatt ?: return
        if (reading) return
        val item = readQueue.removeFirstOrNull()
        if (item == null) {
            publish()
            return
        }
        reading = true
        current = item
        val started = try {
            when (item) {
                is BluetoothGattCharacteristic -> g.readCharacteristic(item)
                is BluetoothGattDescriptor -> g.readDescriptor(item)
                else -> false
            }
        } catch (e: SecurityException) {
            false
        }
        if (started) {
            main.postDelayed(timeout, 5000)
        } else {
            record(item, null, -1)
            finishRead()
        }
    }

    private fun finishRead() {
        main.removeCallbacks(timeout)
        reading = false
        current = null
        publish()
        readNext()
    }

    private fun propertyNames(p: Int): List<String> = buildList {
        if ((p and BluetoothGattCharacteristic.PROPERTY_BROADCAST) != 0) add("BROADCAST")
        if ((p and BluetoothGattCharacteristic.PROPERTY_READ) != 0) add("READ")
        if ((p and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) add("WRITE_NO_RESPONSE")
        if ((p and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) add("WRITE")
        if ((p and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) add("NOTIFY")
        if ((p and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) add("INDICATE")
        if ((p and BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) != 0) add("SIGNED_WRITE")
        if ((p and BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) != 0) add("EXTENDED_PROPS")
    }

    private fun publish() {
        val g = gatt
        if (g != null && g.services.isNotEmpty()) {
            lastServices = g.services.map { s ->
                ServiceDump(
                    uuid = s.uuid,
                    name = Uuids.serviceName(s.uuid),
                    instanceId = s.instanceId,
                    primary = s.type == BluetoothGattService.SERVICE_TYPE_PRIMARY,
                    characteristics = s.characteristics.map { c ->
                        val r = charValues[c]
                        CharacteristicDump(
                            uuid = c.uuid,
                            name = Uuids.characteristicName(c.uuid),
                            instanceId = c.instanceId,
                            properties = propertyNames(c.properties),
                            value = r?.value,
                            readStatus = r?.status,
                            descriptors = c.descriptors.map { d ->
                                DescriptorDump(d.uuid, Uuids.descriptorName(d.uuid), descValues[d]?.value)
                            },
                        )
                    },
                )
            }
        }
        _snapshot.value = GattSnapshot(state, address, lastServices, rssi, mtu, errors.toList())
    }
}
