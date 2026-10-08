package com.example.myapplication.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.focal.app.data.FocalRepository

class FocalDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        FocalRepository.instance.reportProtectionRestored()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        FocalRepository.instance.reportProtectionDisabled("Child requested to remove Device Admin / Uninstall FOCAL")
        return "FOCAL Device Admin is required by your parent to enforce focus rules. Disabling will notify your parent immediately."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        FocalRepository.instance.reportProtectionDisabled("Device Admin revoked")
    }
}
