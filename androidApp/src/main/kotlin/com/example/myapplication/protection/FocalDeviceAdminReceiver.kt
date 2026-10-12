package com.example.myapplication.protection

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.focal.app.data.FocalRepository

class FocalDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "FOCAL_ADMIN"
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device Admin protection enabled successfully.")
        FocalRepository.instance.reportProtectionRestored()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Log.d(TAG, "Device Admin disable attempt requested.")
        FocalRepository.instance.reportDeviceAdminDisableAttempt()
        return "FOCAL Device Admin protection is required by your parent to prevent unauthorized app removal. Disabling will immediately send a high-priority alert to your parent."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device Admin protection disabled.")
        FocalRepository.instance.reportProtectionDisabled("Device Admin protection revoked on child device.")
    }
}
