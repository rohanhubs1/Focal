# FOCAL — Deployment & Setup Checklist (06_SETUP_CHECKLIST.md)

## 1. Credentials & Configuration Requirements
To connect FOCAL to a live production Firebase backend, prepare the following resources:

### 1. Firebase Project Setup
- [ ] Create a new project in the [Firebase Console](https://console.firebase.google.com/).
- [ ] Upgrade project to the **Blaze (Pay-as-you-go)** plan (required for Node.js 20 Cloud Functions).
- [ ] Set up a **Budget Alert** (e.g. $25/month) in Google Cloud Console.

### 2. Android Application Registration
- [ ] Register Android app package name: `com.focal.app`.
- [ ] Generate and register **SHA-1** & **SHA-256** fingerprint hashes for Debug and Release keystores:
  ```bash
  keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
  ```
- [ ] Download `google-services.json` file.

### 3. iOS Application Registration
- [ ] Register iOS Bundle Identifier: `com.focal.app`.
- [ ] Download `GoogleService-Info.plist` file.
- [ ] Download APNs Authentication Key (`.p8`) from Apple Developer Portal for push notifications and upload to Firebase Console -> Cloud Messaging.

### 4. Enable Firebase Services
- [ ] **Authentication**: Enable Email/Password provider.
- [ ] **Cloud Firestore**: Initialize database in `Production Mode` in nearest multi-region (e.g. `asia-south1`).
- [ ] **Cloud Functions**: Enable Cloud Functions API in Google Cloud Console.
- [ ] **Cloud Messaging (FCM)**: Enable FCM API V1.

---

## 2. File Location Mapping
Place configuration files in these target repository paths:

| File Name | Target Repository Location | Git Tracking Status |
|---|---|---|
| `google-services.json` | `androidApp/google-services.json` | **Ignored** (Listed in `.gitignore`) |
| `GoogleService-Info.plist` | `iosApp/iosApp/GoogleService-Info.plist` | **Ignored** (Listed in `.gitignore`) |
| Service Account Key (`.json`) | Cloud Deployment Console Only | **STRICTLY PROHIBITED IN REPO** |

> [!CAUTION]
> **SECURITY DIRECTIVE**: Service account private keys (`*.json`), keystore passwords, and `.p8` private keys must **NEVER** be committed to Git or pasted into chat prompts.

---

## 3. Deployment Commands Reference
Once configuration files are placed:

### Deploy Firebase Backend (Rules, Indexes, Functions)
```bash
cd functions
npm install
npm run build
firebase deploy --only firestore:rules,firestore:indexes,functions
```

### Run Local Firebase Emulator Suite
```bash
firebase emulators:start
```
