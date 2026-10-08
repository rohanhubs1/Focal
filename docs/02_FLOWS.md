# FOCAL — Application Flows & User Journey (02_FLOWS.md)

## 1. Launch Router Logic (Single Source of Truth)
On every application launch, `FocalRootApp` evaluates persisted state in `FocalLocalStorage` to determine the start screen:
1. `KEY_ONBOARDING_DONE` == false → **OnboardingScreen** (4 slides)
2. `KEY_SELECTED_ROLE` == null → **RoleSelectScreen**
3. **Parent / Controller Flow**:
   - `KEY_PERSONALIZATION_DONE` == false → **ParentPersonalizationFlow** (P1 → P2 → P3 → Plan Summary)
   - `KEY_AUTH_DONE` == false → **AuthScreen** (Sign Up / Sign In)
   - `KEY_PARENT_NOTIFICATIONS_ASKED` == false → **ParentPermissionsScreen** (Notifications)
   - Otherwise → **ParentHomeScreen** (With optional Add Child checklist)
4. **Child / Target Flow**:
   - `KEY_PERSONALIZATION_DONE` == false → **ChildPersonalizationFlow** (C1 → C2 → C3 → Plan Summary)
   - `KEY_SETUP_DONE` == false → **ChildPairingScreen** (Option A or Option B)
   - Device missing required permissions → **ChildPermissionsSetupScreen** (7 permissions on Android)
   - Active setup complete, but permission revoked later → **ProtectionOffScreen**
   - Otherwise → **ChildHomeScreen**

## 2. Onboarding & Personalization Sequence
- **Onboarding (4 Slides)**: Minimalist monochrome visual slides explaining total distraction control, physical NFC card focus, live session protection, and parent overview.
- **Role Selection**: Controller vs Target selection with direct link to Sign In.
- **Parent Personalization**:
  - `P1`: Primary Focus Goal (Study, Sleep, Reducing Distractions)
  - `P2`: Worry Apps Selection (Social, Video, Games, Shopping categories)
  - `P3`: Children Count & Age Group
  - `Plan Summary Sheet`: Estimated screen time savings breakdown.
- **Child Personalization**:
  - `C1`: Distraction Apps Selection
  - `C2`: Daily Focus Goal
  - `C3`: Hardest Focus Time of Day
  - `Plan Summary Sheet`: Personal study focus commitment.

## 3. Child Profile Creation & Pairing Architecture
- **Option A (Parent-First Pairing Code)**:
  - Parent taps "Add Child" on Parent App → Cloud Function `createPairingCode` generates a 6-digit numeric code + QR payload with a 10-minute expiry.
  - Child phone opens scanner or selects "Enter code instead", submitting to `redeemPairingCode`.
- **Option B (Child-First Pairing Code)**:
  - Unpaired Child app opens "Connect to Parent" → displays a 6-digit pairing code and live QR code countdown.
  - Parent opens Parent App → "Pair Child Phone" modal → types child's 6-digit code.
- **Device Key Generation**:
  - Upon successful pairing, child device generates an asymmetric Ed25519 keypair / UUID hardware token stored securely in Android KeyStore / iOS Keychain (`KEY_DEVICE_HARDWARE_KEY`).
  - Device exchanges key for a Firebase Custom Auth Token with claims `{role: "child", childId: "...", deviceId: "..."}`.

## 4. Parent / Controller Module
- **Home**: Overview cards, overall focus time, active child status indicators, onboarding checklist ("Add child", "Pair device", "Order NFC card").
- **Children List & Child Detail**:
  - Live session monitor (Active focus timer, remaining time, remote stop).
  - Quick action: Remote Focus Start.
  - Policy & Schedule management per child.
  - Device telemetry: Battery level, connection state, protection integrity status.
  - NFC Card management: Add, claim, assign, unpair, or revoke.
- **Policies Tab**: Per-app blocks, whole category blocks (Social, Video, Games, Shopping), website domain normalization and subdomain matching.
- **Schedules Tab**: Weekly recurring windows (Start time, End time, active days, linked policy).
- **Notifications Tab**: Feed of emergency exit requests, protection disabled alerts, device offline alerts, session completion summaries.
- **Profile & Subscription**: Manage ₹299/mo plan, trial status (84 days remaining), parent account security.

## 5. Child / Target Module
- **Home**: Read-only focus stats, quick start trigger (NFC / QR / Schedule), current active policy preview.
- **Focus Active Screen**: Fullscreen high-contrast telemetry timer, dual progress ring, active blocked count.
- **Session Termination**:
  - Physical NFC card re-tap.
  - QR Code verification scan.
  - Remote stop by Parent.
  - Emergency Request flow (Child requests exit → 15-minute approval window on parent device).
- **Enforcement & Protection Off**:
  - Accessibility service detects blocked package window state change → launches `BlockedAppDialog`.
  - Detection of missing permissions or revoked device admin launches `ProtectionOffScreen` with direct "Fix" buttons and read-only home access.

## 6. Admin Console
- **Overview**: System metrics (Total parents, active child devices, active sessions, total cards).
- **Users Tab**: Search parent accounts, extend trial periods, suspend accounts.
- **Devices Tab**: View paired child hardware, reset pairing state, inspect protection compliance.
- **Cards Tab**: Inventory list, program new NFC batch cards (`FC0001`...), lock/deactivate compromised cards.
- **Revenue & Audit Log**: Financial metrics (Subscriptions, NFC card sales), security audit trail of all admin operations.

## 7. Platform Specifics & Differences
| Feature | Android Enforcement | iOS Enforcement (v1 Scope) |
|---|---|---|
| App Blocking | Native Accessibility Service + Window Overlay | Apple `FamilyControls` & `ManagedSettings` |
| Web Blocking | Chrome / Browser URL bar parsing via Accessibility | Safari Web Content Filter Extension |
| App List Upload | `PackageManager.getInstalledApplications()` | Restricted by Apple API; native System Picker used |
| App Usage Telemetry | `UsageStatsManager` | Aggregated Screen Time API summaries |
| Uninstall Protection | Device Admin (`FocalDeviceAdminReceiver`) | iOS MDM / Family Controls Restriction |
