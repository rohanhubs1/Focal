# FOCAL — Data Model & Firestore Architecture (03_DATA_MODEL.md)

## 1. Firestore Collection Schemas

### `parents/{parentId}`
- `id`: string (Firebase Auth UID)
- `name`: string
- `email`: string
- `phone`: string
- `role`: string ("parent")
- `subscriptionStatus`: string ("trial", "active", "past_due", "cancelled", "suspended")
- `trialEndsAt`: timestamp
- `nextBillingDate`: timestamp
- `monthlyPriceInr`: number (299)
- `fcmTokens`: array of strings
- `createdAt`: timestamp
- `updatedAt`: timestamp

### `children/{childId}`
- `id`: string
- `parentId`: string (Foreign key to `parents`)
- `name`: string
- `age`: number
- `isOnline`: boolean
- `isFocusing`: boolean
- `currentSessionId`: string?
- `pairedCardId`: string?
- `todayFocusMinutes`: number
- `streakDays`: number
- `createdAt`: timestamp

### `devices/{deviceId}`
- `id`: string
- `childId`: string (Foreign key to `children`)
- `parentId`: string
- `deviceName`: string
- `platform`: string ("android", "ios")
- `osVersion`: string
- `appVersion`: string
- `hardwarePublicKey`: string
- `fcmToken`: string
- `batteryLevel`: number
- `isAccessibilityEnabled`: boolean
- `isOverlayEnabled`: boolean
- `isUsageAccessEnabled`: boolean
- `isNotificationsEnabled`: boolean
- `isDeviceAdminEnabled`: boolean
- `isBatteryUnrestricted`: boolean
- `isExactAlarmEnabled`: boolean
- `lastHeartbeatAt`: timestamp
- `updatedAt`: timestamp

### `pairingCodes/{codeId}`
- `id`: string
- `code`: string (6-digit numeric)
- `qrToken`: string
- `parentId`: string
- `childId`: string?
- `createdByRole`: string ("parent", "child")
- `status`: string ("PENDING", "REDEEMED", "EXPIRED")
- `expiresAt`: timestamp (10 minutes TTL)
- `createdAt`: timestamp

### `cards/{cardId}`
- `id`: string (e.g. "FC0001")
- `cardId`: string ("FC0001")
- `nfcUid`: string
- `token`: string (Hashed secret token)
- `ownerParentId`: string?
- `assignedChildId`: string?
- `status`: string ("in_stock", "registered", "assigned", "deactivated")
- `registeredAt`: timestamp?
- `assignedAt`: timestamp?

### `policies/{policyId}`
- `id`: string
- `parentId`: string
- `childId`: string
- `name`: string
- `mode`: string ("STUDY", "SLEEP", "CUSTOM")
- `durationMinutes`: number
- `blockedCategories`: array of strings ("Social", "Video", "Games", "Shopping")
- `blockedApps`: array of strings (Package names)
- `blockedWebsites`: array of strings (Normalized domains)
- `allowedApps`: array of strings (Package names)
- `emergencyExitAllowed`: boolean
- `parentApprovalRequired`: boolean
- `updatedAt`: timestamp

### `schedules/{scheduleId}`
- `id`: string
- `parentId`: string
- `childId`: string
- `policyId`: string
- `name`: string
- `days`: array of strings ("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
- `startTime`: string ("HH:mm")
- `endTime`: string ("HH:mm")
- `isActive`: boolean
- `updatedAt`: timestamp

### `sessions/{sessionId}`
- `id`: string
- `childId`: string
- `deviceId`: string
- `parentId`: string
- `policyId`: string
- `policyTitle`: string
- `startedBy`: string ("NFC", "QR", "SCHEDULE", "REMOTE_PARENT")
- `startTime`: timestamp
- `scheduledEndTime`: timestamp
- `actualEndTime`: timestamp?
- `status`: string ("ACTIVE", "COMPLETED", "ENDED_EARLY_PARENT", "ENDED_EARLY_EMERGENCY")
- `blocksCount`: number
- `timeSavedMinutes`: number

### `events/{eventId}`
- `id`: string
- `childId`: string
- `deviceId`: string
- `parentId`: string
- `eventType`: string ("BLOCK_ATTEMPT", "PROTECTION_DISABLED", "PROTECTION_RESTORED", "EMERGENCY_REQUESTED", "EMERGENCY_APPROVED", "EMERGENCY_DECLINED", "HEARTBEAT")
- `details`: map
- `timestamp`: timestamp

### `usageDaily/{usageId}`
- `id`: string (e.g. "{childId}_{YYYY-MM-DD}")
- `childId`: string
- `parentId`: string
- `date`: string ("YYYY-MM-DD")
- `totalScreenTimeMinutes`: number
- `totalFocusMinutes`: number
- `blocksCount`: number
- `appBreakdown`: map (PackageName -> Minutes)
- `updatedAt`: timestamp

### `commands/{commandId}`
- `id`: string
- `childId`: string
- `deviceId`: string
- `parentId`: string
- `commandType`: string ("START_SESSION", "STOP_SESSION", "SYNC_POLICIES")
- `payload`: map
- `status`: string ("PENDING", "EXECUTED", "EXPIRED")
- `createdAt`: timestamp

### `notifications/{notificationId}`
- `id`: string
- `recipientUserId`: string
- `type`: string ("EMERGENCY_REQUEST", "PROTECTION_OFF", "DEVICE_OFFLINE", "SESSION_STARTED", "SESSION_COMPLETED")
- `title`: string
- `message`: string
- `childId`: string?
- `isRead`: boolean
- `createdAt`: timestamp

### `subscriptions/{subscriptionId}`
- `id`: string
- `parentId`: string
- `razorpaySubscriptionId`: string?
- `planId`: string
- `status`: string ("active", "halted", "cancelled")
- `currentPeriodEnd`: timestamp

### `adminAudit/{logId}`
- `id`: string
- `adminEmail`: string
- `action`: string
- `target`: string
- `timestamp`: timestamp

---

## 2. Role Security Access Matrix
| Collection | Parent / Controller | Child Device | Admin | Client Direct Writes Allowed |
|---|---|---|---|---|
| `parents` | Read / Write Own | Read Parent Info | Read All | Yes (Own doc only) |
| `children` | Read / Write Own Children | Read Own Child Doc | Read All | Yes (Parent of child) |
| `devices` | Read Own Children's Devices | Read / Write Own Device | Read All | Yes (Child device for telemetry) |
| `pairingCodes` | Read Own Codes | Read Own Code | Read All | **No** (Cloud Functions only) |
| `cards` | Read Owned Cards | Read Assigned Card | Read / Write All | **No** (Cloud Functions only) |
| `policies` | Read / Write Own Policies | Read Assigned Policies | Read All | Yes (Parent only) |
| `schedules` | Read / Write Own Schedules | Read Assigned Schedules | Read All | Yes (Parent only) |
| `sessions` | Read Own Children's Sessions | Read / Write Own Session | Read All | Yes (Child device / Parent) |
| `events` | Read Own Children's Events | Write Own Events | Read All | Yes (Child app append only) |
| `usageDaily` | Read Own Children's Usage | Write Own Usage | Read All | Yes (Child device batch update) |
| `commands` | Write Command | Read / Update Status | Read All | Yes (Parent create, Child update) |
| `notifications` | Read / Update Own Notifications | Read Own Notifications | Read All | Yes (Recipient user update `isRead`) |
| `subscriptions` | Read Own Subscription | No Access | Read / Write All | **No** (Webhooks only) |
| `adminAudit` | No Access | No Access | Read / Write All | **No** (Cloud Functions only) |

---

## 3. Required Firestore Composite Indexes
1. `sessions`: `childId` ASC, `status` ASC, `startTime` DESC
2. `events`: `childId` ASC, `timestamp` DESC
3. `events`: `parentId` ASC, `eventType` ASC, `timestamp` DESC
4. `notifications`: `recipientUserId` ASC, `isRead` ASC, `createdAt` DESC
5. `schedules`: `childId` ASC, `isActive` ASC
6. `cards`: `ownerParentId` ASC, `status` ASC
7. `pairingCodes`: `code` ASC, `status` ASC, `expiresAt` DESC
