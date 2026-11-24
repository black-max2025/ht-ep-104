package app.ht104.control.data

import android.content.Context

data class LastLocation(val latitude: Double, val longitude: Double, val accuracyM: Float, val timeMs: Long)

/** Local-only preferences. Nothing here ever leaves the device. */
class SettingsStore(context: Context) {
    private val prefs = (context.applicationContext ?: context)
        .getSharedPreferences("ht104_settings", Context.MODE_PRIVATE)

    var selectedAddress: String?
        get() = prefs.getString("selected_address", null)
        set(v) = prefs.edit().putString("selected_address", v).apply()

    var selectedName: String?
        get() = prefs.getString("selected_name", null)
        set(v) = prefs.edit().putString("selected_name", v).apply()

    /** "system", "en" or "ar". */
    var language: String
        get() = prefs.getString("language", "system") ?: "system"
        set(v) = prefs.edit().putString("language", v).apply()

    var refreshSeconds: Int
        get() = prefs.getInt("refresh_seconds", 30)
        set(v) = prefs.edit().putInt("refresh_seconds", v).apply()

    var lowBatteryThreshold: Int
        get() = prefs.getInt("low_battery", 20)
        set(v) = prefs.edit().putInt("low_battery", v).apply()

    var notifyOnDisconnect: Boolean
        get() = prefs.getBoolean("notify_disconnect", false)
        set(v) = prefs.edit().putBoolean("notify_disconnect", v).apply()

    var saveLocationOnDisconnect: Boolean
        get() = prefs.getBoolean("save_location", false)
        set(v) = prefs.edit().putBoolean("save_location", v).apply()

    var lastConnectedMs: Long
        get() = prefs.getLong("last_connected", 0L)
        set(v) = prefs.edit().putLong("last_connected", v).apply()

    var lastDisconnectedMs: Long
        get() = prefs.getLong("last_disconnected", 0L)
        set(v) = prefs.edit().putLong("last_disconnected", v).apply()

    var lastLocation: LastLocation?
        get() {
            if (!prefs.contains("loc_lat")) return null
            return LastLocation(
                Double.fromBits(prefs.getLong("loc_lat", 0L)),
                Double.fromBits(prefs.getLong("loc_lon", 0L)),
                prefs.getFloat("loc_acc", 0f),
                prefs.getLong("loc_time", 0L),
            )
        }
        set(v) {
            val e = prefs.edit()
            if (v == null) {
                e.remove("loc_lat").remove("loc_lon").remove("loc_acc").remove("loc_time")
            } else {
                e.putLong("loc_lat", v.latitude.toRawBits())
                    .putLong("loc_lon", v.longitude.toRawBits())
                    .putFloat("loc_acc", v.accuracyM)
                    .putLong("loc_time", v.timeMs)
            }
            e.apply()
        }
}
