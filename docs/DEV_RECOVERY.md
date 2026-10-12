# FOCAL — Developer Recovery & Device Admin Removal Guide

If FOCAL is installed on a test device with Device Admin enabled and Accessibility Service active, use the following ADB commands to safely remove Device Admin and uninstall the application during development.

---

## Package & Receiver Information
- **App Package Name**: `com.example.myapplication`
- **Device Admin Receiver Component**: `com.example.myapplication/com.example.myapplication.protection.FocalDeviceAdminReceiver`

---

## 1. Quick Deactivate & Uninstall (Recommended)

Run these ADB commands from your computer terminal (PowerShell or Bash) while the device is connected via USB debugging:

```bash
# 1. Force stop the FOCAL app & background services
adb shell am force-stop com.example.myapplication

# 2. Disable Accessibility Service for FOCAL
adb shell settings put secure enabled_accessibility_services ""

# 3. Deactivate Device Admin for FOCAL Receiver
adb shell dpm remove-active-admin com.example.myapplication/com.example.myapplication.protection.FocalDeviceAdminReceiver

# 4. Uninstall FOCAL cleanly
adb uninstall com.example.myapplication
```

---

## 2. Step-by-Step Manual Recovery

If `adb uninstall` fails with an error like `DELETE_FAILED_DEVICE_POLICY_MANAGER`:

### Step A: Verify Active Device Admins
```bash
adb shell dpm list-owners
```

### Step B: Remove FOCAL Device Admin
```bash
adb shell dpm remove-active-admin com.example.myapplication/com.example.myapplication.protection.FocalDeviceAdminReceiver
```
*Output expected:* `Success: Admin removed ComponentInfo{com.example.myapplication/com.example.myapplication.protection.FocalDeviceAdminReceiver}`

### Step C: Revoke Accessibility & Usage Permissions
```bash
adb shell pm revoke com.example.myapplication android.permission.BIND_ACCESSIBILITY_SERVICE
```

### Step D: Uninstall App
```bash
adb uninstall com.example.myapplication
```
*Output expected:* `Success`

---

## 3. Clear All Local App Data Without Uninstalling

To reset FOCAL to a fresh install state without uninstalling:

```bash
adb shell pm clear com.example.myapplication
```
