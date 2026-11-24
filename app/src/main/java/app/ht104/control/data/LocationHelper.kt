package app.ht104.control.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager

/** Last-known PHONE location. This is not live earbud tracking. */
object LocationHelper {
    @SuppressLint("MissingPermission")
    fun lastKnown(context: Context): LastLocation? {
        if (!PermissionHelper.has(context, android.Manifest.permission.ACCESS_FINE_LOCATION)) return null
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        val best = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        ).mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time } ?: return null
        return LastLocation(best.latitude, best.longitude, best.accuracy, best.time)
    }
}
