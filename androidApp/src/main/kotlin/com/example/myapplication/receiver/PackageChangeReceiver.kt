package com.example.myapplication.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myapplication.util.AndroidAppPickerManager

object PackageChangeNotifier {
    var onPackageAdded: ((context: Context, packageName: String) -> Unit)? = null
    var onPackageRemoved: ((context: Context, packageName: String) -> Unit)? = null
}

class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val action = intent.action ?: return
        val packageName = intent.data?.schemeSpecificPart ?: return

        when (action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                if (!isReplacing) {
                    AndroidAppPickerManager.loadInstalledAppsIntoRepository(context)
                    PackageChangeNotifier.onPackageAdded?.invoke(context, packageName)
                }
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                if (!isReplacing) {
                    AndroidAppPickerManager.loadInstalledAppsIntoRepository(context)
                    PackageChangeNotifier.onPackageRemoved?.invoke(context, packageName)
                }
            }
        }
    }
}
