package app.ht104.control.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import app.ht104.control.diagnostics.PermissionStatus

object PermissionHelper {
    fun bluetoothRuntime(): List<String> =
        if (Build.VERSION.SDK_INT >= 31) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun locationRuntime(): List<String> =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    fun notificationRuntime(): List<String> =
        if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()

    fun callRuntime(): List<String> = listOf(Manifest.permission.ANSWER_PHONE_CALLS)

    fun has(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun allGranted(context: Context, permissions: List<String>): Boolean = permissions.all { has(context, it) }

    fun hasBluetoothConnect(context: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || has(context, Manifest.permission.BLUETOOTH_CONNECT)

    fun hasBluetoothScan(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 31) has(context, Manifest.permission.BLUETOOTH_SCAN)
        else has(context, Manifest.permission.ACCESS_FINE_LOCATION)

    fun statuses(context: Context): List<PermissionStatus> {
        val list = ArrayList<PermissionStatus>()
        fun add(p: String, purpose: String) = list.add(PermissionStatus(p, has(context, p), purpose))
        if (Build.VERSION.SDK_INT >= 31) {
            add(Manifest.permission.BLUETOOTH_SCAN, "BLE scanning")
            add(Manifest.permission.BLUETOOTH_CONNECT, "Paired device info, GATT, profile state")
        }
        add(Manifest.permission.ACCESS_FINE_LOCATION, "Legacy BLE scanning (Android 11 and lower) and optional last-known phone location")
        add(Manifest.permission.ACCESS_COARSE_LOCATION, "Requested together with fine location")
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS, "Low battery and connection-loss notifications")
        add(Manifest.permission.ANSWER_PHONE_CALLS, "Answer / reject / end call from the app")
        return list
    }
}
