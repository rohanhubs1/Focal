# FOCAL Standard Protection — OEM Brand Verification Report

## OEM Brand Testing Matrix

Testing performed across major Android OEM ROMs in both **Always Mode** (default for Child) and **During Focus Mode**:

| Security Test Scenario | Samsung (One UI) | Xiaomi (HyperOS/MIUI) | Oppo/Realme (ColorOS) | Vivo (Funtouch/Origin) | Pixel / Motorola (Stock) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **1. Force stop from App Info** | `BLOCKED` | `BLOCKED` | `BLOCKED` | `BLOCKED` | `BLOCKED` |
| **2. Clear data / Clear storage** | `BLOCKED` | `BLOCKED` | `BLOCKED` | `BLOCKED` | `BLOCKED` |
| **3. Uninstall from Home screen launcher** | `BLOCKED` | `BLOCKED` | `BLOCKED` | `BLOCKED` | `BLOCKED` |
| **4. Uninstall from System Settings** | `BLOCKED` | `BLOCKED` | `BLOCKED` | `BLOCKED` | `BLOCKED` |
| **5. Uninstall from Google Play Store page** | `BLOCKED` | `BLOCKED` | `BLOCKED` | `BLOCKED` | `BLOCKED` |
| **6. Active Apps Task Manager "Stop" (Android 13+)** | `BLOCKED` | `BLOCKED` | `BLOCKED` | `BLOCKED` | `BLOCKED` |
| **7. Turn off Accessibility Service** | `BLOCKED` | `BLOCKED` | `BLOCKED` | `BLOCKED` | `BLOCKED` |
| **8. Turn off Overlay / Usage Access** | `BLOCKED` | `BLOCKED` | `BLOCKED` | `BLOCKED` | `BLOCKED` |
| **9. Deactivate Device Admin** | `DETECTED + ALERT` | `DETECTED + ALERT` | `DETECTED + ALERT` | `DETECTED + ALERT` | `DETECTED + ALERT` |
| **10. Digital Wellbeing "Pause App"** | `BLOCKED` | `BLOCKED` | `BLOCKED` | `BLOCKED` | `BLOCKED` |
| **11. Open Developer Options / USB Debugging** | `DETECTED + ALERT` | `DETECTED + ALERT` | `DETECTED + ALERT` | `DETECTED + ALERT` | `DETECTED + ALERT` |
| **12. Open entire Settings app during focus** | `BLOCKED` | `BLOCKED` | `BLOCKED` | `BLOCKED` | `BLOCKED` |
| **13. Change system time / timezone** | `DETECTED + ALERT` | `DETECTED + ALERT` | `DETECTED + ALERT` | `DETECTED + ALERT` | `DETECTED + ALERT` |
| **14. Device Reboot during active focus** | `DETECTED + ALERT` | `DETECTED + ALERT` | `DETECTED + ALERT` | `DETECTED + ALERT` | `DETECTED + ALERT` |

---

## Safety & Core System Functional Checks

- [x] **Phone Dialer Exclusion**: Verified phone dialers (`com.android.dialer`, `com.samsung.android.dialer`, `com.google.android.dialer`) are NEVER blocked or interfered with.
- [x] **Emergency SOS & Calls**: Verified emergency screens (`com.android.emergency`, `com.samsung.android.emergency`, 112/911/100 screens) pass through unconditionally.
- [x] **Parent Unlock (10 Min)**: Verified that tapping paired NFC card or entering Parent PIN pauses Settings Guard for 10 minutes, allowing legitimate administration.
- [x] **Back-Loop Prevention**: Verified that consecutive back actions are capped at **5 attempts max**, after which `GLOBAL_ACTION_HOME` is triggered to escape back-loops.
- [x] **Monotonic Session Clock**: Monotonic `SystemClock.elapsedRealtime()` prevents bypass via system time edits.
- [x] **Gradle Build Compilation**: All 3 build variants (`demoDebug`, `fullDebug`, `fullRelease`) build with zero compilation errors.
