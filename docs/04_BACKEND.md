# FOCAL — Backend Architecture & Cloud Services (04_BACKEND.md)

## 1. Authentication & Token Architecture
- **Parent / Controller Authentication**:
  - Uses standard Firebase Auth Email/Password provider.
  - User Token Custom Claim: `{ role: "parent" }`.
- **Child Device Authentication**:
  - Children accounts do **not** use email or passwords.
  - Upon pairing verification, the child app submits its hardware public key / device UUID to the `redeemPairingCode` Cloud Function.
  - `redeemPairingCode` validates the code and uses `admin.auth().createCustomToken(deviceId, { role: "child", childId, deviceId, parentId })` to mint a custom Firebase auth token.
  - The child app authenticates via `signInWithCustomToken()` and securely stores the token in encrypted storage.
- **Admin Role Authentication**:
  - Custom Claim: `{ role: "admin" }` set via Admin SDK or Cloud Function. Checked on every request.

---

## 2. Firebase Cloud Functions Catalog

### 1. `createPairingCode`
- **Trigger**: HTTPS Callable (Parent Auth)
- **Inputs**: `{ childName: string, age: number }`
- **Outputs**: `{ codeId: string, pairingCode: string, qrPayload: string, expiresAt: string }`

### 2. `redeemPairingCode`
- **Trigger**: HTTPS Callable (Unauthenticated / Public Device Key)
- **Inputs**: `{ pairingCode: string, deviceHardwareKey: string, deviceName: string, platform: string }`
- **Outputs**: `{ firebaseCustomToken: string, childId: string, deviceId: string, parentId: string }`

### 3. `claimCard`
- **Trigger**: HTTPS Callable (Parent Auth)
- **Inputs**: `{ rawNfcToken: string, childId: string? }`
- **Outputs**: `{ success: boolean, cardId: string, message: string }`

### 4. `revokeCard`
- **Trigger**: HTTPS Callable (Parent / Admin Auth)
- **Inputs**: `{ cardId: string }`
- **Outputs**: `{ success: boolean, message: string }`

### 5. `programCards`
- **Trigger**: HTTPS Callable (Admin Auth)
- **Inputs**: `{ count: number }`
- **Outputs**: `{ success: boolean, generatedCards: Array<{ cardId: string, nfcUid: string, token: string }> }`

### 6. `createFocusQr`
- **Trigger**: HTTPS Callable (Parent Auth)
- **Inputs**: `{ childId: string, policyId: string, durationMinutes: number }`
- **Outputs**: `{ qrToken: string, expiresAt: string }`

### 7. `validateFocusQr`
- **Trigger**: HTTPS Callable (Child Auth)
- **Inputs**: `{ qrToken: string }`
- **Outputs**: `{ valid: boolean, policy: object, durationMinutes: number }`

### 8. `startRemoteSession`
- **Trigger**: HTTPS Callable (Parent Auth)
- **Inputs**: `{ childId: string, policyId: string, durationMinutes: number }`
- **Outputs**: `{ sessionId: string, status: string }`

### 9. `endRemoteSession`
- **Trigger**: HTTPS Callable (Parent Auth)
- **Inputs**: `{ sessionId: string, childId: string }`
- **Outputs**: `{ success: boolean }`

### 10. `handleEmergencyRequest`
- **Trigger**: HTTPS Callable (Child / Parent Auth)
- **Inputs**: `{ action: "REQUEST" | "APPROVE" | "DECLINE", childId: string, sessionId: string, reason: string? }`
- **Outputs**: `{ status: string }`

### 11. `heartbeatWatcher`
- **Trigger**: Scheduled Cloud Function (Every 15 minutes)
- **Execution**: Scans `devices` collection for last `lastHeartbeatAt` > 30 minutes. Pushes `DEVICE_OFFLINE` FCM notification to parent if no update in 30 min. Throttled to max 1 alert per hour per device.

### 12. `adminStats`
- **Trigger**: HTTPS Callable (Admin Auth)
- **Inputs**: `{}`
- **Outputs**: `{ totalParents: number, activeChildren: number, activeSessions: number, totalCards: number, revenueInr: number }`

### 13. `subscriptionWebhooks`
- **Trigger**: HTTP Webhook Endpoint (Razorpay / Payment Gateway)
- **Inputs**: Raw Payment Gateway Webhook Payload + Signature
- **Outputs**: `{ status: "ok" }`

---

## 3. Firebase Cloud Messaging (FCM) Specification
- **High-Priority Push Notifications**:
  - `EMERGENCY_REQUEST`: Delivered instantly to parent with action buttons ("Approve", "Decline").
  - `PROTECTION_OFF`: High urgency notification when child disables Accessibility, Overlay, or Device Admin.
- **Silent Synchronization Data Messages**:
  - `SYNC_NOW`: Silent data payload instructing child device to fetch updated policies/schedules immediately.
  - `SESSION_REMOTE_START`: Command payload initiating immediate focus mode on target device.
  - `SESSION_REMOTE_END`: Payload terminating active session on target device.

---

## 4. Firestore Security Rules Draft (`firestore.rules`)
```solidity
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isParent() {
      return isAuthenticated() && request.auth.token.role == 'parent';
    }
    
    function isChild() {
      return isAuthenticated() && request.auth.token.role == 'child';
    }
    
    function isAdmin() {
      return isAuthenticated() && request.auth.token.role == 'admin';
    }
    
    function isOwnerParent(parentId) {
      return isParent() && request.auth.uid == parentId;
    }
    
    function isTargetChild(childId) {
      return isChild() && request.auth.token.childId == childId;
    }

    // Parents
    match /parents/{parentId} {
      allow read, write: if isOwnerParent(parentId) || isAdmin();
    }

    // Children
    match /children/{childId} {
      allow read: if isParent() && resource.data.parentId == request.auth.uid || isTargetChild(childId) || isAdmin();
      allow write: if isParent() && resource.data.parentId == request.auth.uid || isAdmin();
    }

    // Devices
    match /devices/{deviceId} {
      allow read, write: if isChild() && request.auth.token.deviceId == deviceId || isAdmin();
      allow read: if isParent() && resource.data.parentId == request.auth.uid;
    }

    // Policies & Schedules
    match /policies/{policyId} {
      allow read: if isParent() && resource.data.parentId == request.auth.uid || isChild() || isAdmin();
      allow write: if isParent() && request.auth.uid == request.resource.data.parentId || isAdmin();
    }
    
    match /schedules/{scheduleId} {
      allow read: if isParent() && resource.data.parentId == request.auth.uid || isChild() || isAdmin();
      allow write: if isParent() && request.auth.uid == request.resource.data.parentId || isAdmin();
    }

    // Sessions & Events
    match /sessions/{sessionId} {
      allow read, write: if isChild() || isParent() || isAdmin();
    }

    match /events/{eventId} {
      allow create: if isChild();
      allow read: if isParent() || isAdmin();
    }

    // Disallow Direct Writes to System Collections
    match /cards/{cardId} {
      allow read: if isAuthenticated();
      allow write: if false; // Server Cloud Functions only
    }

    match /pairingCodes/{codeId} {
      allow read: if isAuthenticated();
      allow write: if false; // Server Cloud Functions only
    }

    match /subscriptions/{subId} {
      allow read: if isParent();
      allow write: if false; // Webhooks only
    }
  }
}
```

---

## 5. Offline Child Device Enforcement Architecture
To guarantee 100% protection uninterrupted by network loss, airplane mode, or server outages:
- **Local Room / SQLDelight Database Cache**:
  - Active `policies`, `schedules`, `alwaysAllowedApps`, and `blockedWebsites` are cached in sqlite whenever synced.
  - Active session state, remaining focus time, and cumulative block counts persist locally.
- **Offline Event Buffer**:
  - Block events (`BLOCK_ATTEMPT`) and periodic heartbeats are queued in local DB storage when offline.
  - Upon network reconnection, `FocalSyncEngine` flushes buffered telemetry events to Firestore in a single compressed batch.
