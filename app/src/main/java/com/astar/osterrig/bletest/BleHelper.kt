package com.astar.osterrig.bletest

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.preference.PreferenceManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object BleHelper {

    fun isBleEnabled() =
        BluetoothAdapter.getDefaultAdapter().isEnabled

    fun isLocationPermissionGranted(context: Context) =
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    fun isLocationPermissionDeniedForever(activity: Activity): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
        return !isLocationPermissionGranted(activity)
                && preferences.getBoolean("permission_requested", false)
                && !ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    fun isLocationEnabled(context: Context): Boolean {
        if (isMarshmallowOrAbove()) {
            var locationMode = Settings.Secure.LOCATION_MODE_OFF
            try {
                locationMode =
                    Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE)
            } catch (e: Settings.SettingNotFoundException) {
                // do nothing
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF
        }
        return true
    }

    fun isLocationRequired(context: Context): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getBoolean("location_not_required", isMarshmallowOrAbove())
    }

    fun markLocationNotRequired(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putBoolean("location_not_required", false).apply()
    }

    fun markLocationPermissionRequested(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putBoolean("permission_requested", true).apply()
    }

    fun isMarshmallowOrAbove() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

}