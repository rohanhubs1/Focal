package com.example.myapplication.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.focal.app.data.FocalRepository
import com.focal.app.engine.FocusEngine
import com.focal.app.protection.*

class FocalAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "FOCAL_ACCESSIBILITY"

        var isServiceRunning = false
            private set

        var instance: FocalAccessibilityService? = null
            private set
    }

    private var focalAppLabel: String = "focal"

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        com.focal.app.ui.components.PlatformPermissionsHelper.isAccessibilityServiceConnected = true
        instance = this
        try {
            focalAppLabel = packageManager.getApplicationLabel(applicationInfo).toString().lowercase()
        } catch (_: Exception) {
            focalAppLabel = "focal"
        }
        Log.d(TAG, "onServiceConnected: FOCAL Accessibility Service active. App label = '$focalAppLabel'")
        FocalRepository.instance.reportProtectionRestored()
        FocusEngine.instance.checkAndUpdatePermissions()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isServiceRunning = false
        com.focal.app.ui.components.PlatformPermissionsHelper.isAccessibilityServiceConnected = false
        if (instance == this) instance = null
        Log.d(TAG, "onUnbind: Accessibility Service unbound")
        FocalRepository.instance.reportProtectionDisabled("Accessibility Service Disabled")
        FocusEngine.instance.checkAndUpdatePermissions()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        com.focal.app.ui.components.PlatformPermissionsHelper.isAccessibilityServiceConnected = false
        if (instance == this) instance = null
        Log.d(TAG, "onDestroy: Accessibility Service destroyed")
        FocalRepository.instance.reportProtectionDisabled("Accessibility Service Stopped")
        FocusEngine.instance.checkAndUpdatePermissions()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        try {
            val nowElapsed = SystemClock.elapsedRealtime()
            val ownPackage = applicationContext.packageName

            // 1. Detect Top Active Application Window & Package
            val windowInfo = extractWindowInfo(event, ownPackage) ?: return

            // 2. Query Protection Engine for Decision (Pure Kotlin Engine)
            val state = ProtectionStateRepository.instance.state.value
            val isFixActive = com.focal.app.ui.components.PlatformPermissionsHelper.isSetupPageGraceActive()
            val decision = ProtectionEngine.decide(
                windowInfo = windowInfo,
                state = state,
                nowElapsed = nowElapsed,
                appLabel = focalAppLabel,
                ownPackage = ownPackage,
                isFixAllowanceActive = isFixActive
            )

            Log.d(TAG, "onAccessibilityEvent: pkg=${windowInfo.packageName}, focusActive=${state.focusActive}, decision=${decision.label}")

            // 3. Delegate Action Execution (Smooth Overlay + HOME)
            ActionExecutor.execute(
                context = applicationContext,
                accessibilityService = this,
                windowInfo = windowInfo,
                decision = decision,
                currentElapsed = nowElapsed
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing accessibility event: ${e.message}", e)
        }
    }

    private fun extractWindowInfo(event: AccessibilityEvent, ownPackage: String): WindowInfo? {
        var topPackage: String? = null
        var topClass: String = event.className?.toString() ?: ""
        var isAppWindow = true

        try {
            val interactiveWindows = windows
            if (!interactiveWindows.isNullOrEmpty()) {
                val appWindow = interactiveWindows.firstOrNull {
                    it.type == AccessibilityWindowInfo.TYPE_APPLICATION && (it.isFocused || it.isActive)
                } ?: interactiveWindows.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }

                if (appWindow != null) {
                    val rootNode = appWindow.root
                    if (rootNode != null) {
                        topPackage = rootNode.packageName?.toString()
                    }
                }
            }
        } catch (_: Exception) {}

        if (topPackage.isNullOrBlank()) {
            topPackage = event.packageName?.toString()
        }

        if (topPackage.isNullOrBlank()) return null

        val lowerPkg = topPackage.lowercase()
        val ownPkgLower = ownPackage.lowercase()

        // If user returned to FOCAL app -> End Fix-button Allowance immediately
        if (lowerPkg == ownPkgLower || lowerPkg.contains("com.focal.app") ||
            lowerPkg.contains("com.rohan.focalapplication") || lowerPkg.contains("myapplication")) {
            if (com.focal.app.ui.components.PlatformPermissionsHelper.isSetupPageGraceActive()) {
                com.focal.app.ui.components.PlatformPermissionsHelper.setupPageAllowedUntilMs = 0L
                Log.d(TAG, "FIX_ALLOWANCE_END: User returned to FOCAL app, grace ended")
                try {
                    ProtectionStateRepository.instance.recordDecision(
                        DecisionLogEntry(
                            timestampMs = System.currentTimeMillis(),
                            packageName = ownPackage,
                            windowClass = "FocalApp",
                            decisionType = DecisionType.ALLOW,
                            ruleLetter = 'a',
                            reason = "FIX_ALLOWANCE_END",
                            actionTaken = "Fix-button allowance ended upon returning to FOCAL"
                        )
                    )
                } catch (_: Exception) {}
            }
            return null
        }

        // Ignore events from FOCAL's own overlay or launcher while overlay is already showing
        if (topClass.contains("BlockOverlay") || (BlockOverlayManager.isOverlayShowing() && ProtectionEngine.LAUNCHER_PACKAGES.contains(lowerPkg))) {
            return null
        }

        // Performance Optimization: Only perform expensive fullText view-tree traversal
        // for packages that actually require text matching (Settings, Installers, Launchers, System UI, Play Store)
        val requiresFullTextScan = ProtectionEngine.INSTALLER_AND_LAUNCHER_PACKAGES.contains(lowerPkg) ||
                ProtectionEngine.SETTINGS_PACKAGES.contains(lowerPkg) ||
                lowerPkg == "com.android.systemui" ||
                lowerPkg == "com.android.vending"

        val fullText = if (requiresFullTextScan) {
            collectWindowTexts()
        } else {
            ""
        }

        val state = ProtectionStateRepository.instance.state.value
        val urlText = if (ProtectionEngine.KNOWN_BROWSERS.contains(lowerPkg) && state.focusActive && state.policy.blockedWebsites.isNotEmpty()) {
            extractBrowserUrlText(rootInActiveWindow)
        } else {
            null
        }

        return WindowInfo(
            packageName = topPackage,
            windowClass = topClass,
            windowTitle = "",
            fullText = fullText,
            urlText = urlText,
            isApplicationWindow = isAppWindow
        )
    }

    private fun collectWindowTexts(): String {
        val texts = mutableListOf<String>()
        try {
            val interactiveWindows = windows
            if (!interactiveWindows.isNullOrEmpty()) {
                for (window in interactiveWindows) {
                    val root = window.root ?: continue
                    traverseNodeTexts(root, texts)
                }
            } else {
                rootInActiveWindow?.let { traverseNodeTexts(it, texts) }
            }
        } catch (_: Exception) {
            rootInActiveWindow?.let { traverseNodeTexts(it, texts) }
        }
        return texts.joinToString(" ")
    }

    private fun traverseNodeTexts(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        try {
            val text = node.text?.toString()
            if (!text.isNullOrBlank()) list.add(text)

            val desc = node.contentDescription?.toString()
            if (!desc.isNullOrBlank()) list.add(desc)

            for (i in 0 until node.childCount) {
                traverseNodeTexts(node.getChild(i), list)
            }
        } catch (_: Exception) {}
    }

    private fun extractBrowserUrlText(node: AccessibilityNodeInfo?): String? {
        if (node == null) return null
        try {
            val viewId = node.viewIdResourceName?.lowercase() ?: ""
            if (viewId.contains("url") || viewId.contains("location") || viewId.contains("search") || viewId.contains("address")) {
                val text = node.text?.toString() ?: node.contentDescription?.toString()
                if (!text.isNullOrBlank()) return text
            }
            val text = node.text?.toString() ?: ""
            if (text.contains(".com") || text.contains(".org") || text.contains(".net") || text.contains(".io") || text.contains("http")) {
                return text
            }

            for (i in 0 until node.childCount) {
                val childResult = extractBrowserUrlText(node.getChild(i))
                if (childResult != null) return childResult
            }
        } catch (_: Exception) {}
        return null
    }

    override fun onInterrupt() {
        isServiceRunning = false
        com.focal.app.ui.components.PlatformPermissionsHelper.isAccessibilityServiceConnected = false
        FocusEngine.instance.checkAndUpdatePermissions()
    }
}
