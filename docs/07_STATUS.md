# FOCAL — Comprehensive Feature Implementation Status (07_STATUS.md)

## Feature Implementation Matrix

| Module / Feature | Android Status | iOS Status (v1 Scope) | Backend Status |
|---|---|---|---|
| **Design System & Theme** | Done (100% Monochrome) | Done (Compose Multiplatform) | N/A |
| **Launch Router & Progress State** | Done (`FocalLocalStorage`) | Done (`FocalLocalStorage`) | N/A |
| **Onboarding (4 Slides)** | Done | Done | N/A |
| **Role Select & Direct Sign In** | Done | Done | N/A |
| **Parent Personalization (P1-P3)**| Done | Done | Needs backend sync |
| **Child Personalization (C1-C3)** | Done | Done | Needs backend sync |
| **Parent Auth (Sign Up / Sign In)**| Done (UI & Demo Repo) | Done (UI & Demo Repo) | Needs backend (Firebase Auth) |
| **Child Pairing Option A (Code/QR)**| Done (Local Generator) | Done (UI) | Needs backend (`createPairingCode`, `redeemPairingCode`) |
| **Child Pairing Option B (Child Code)**| Done (Local Generator) | Done (UI) | Needs backend (`createPairingCode`, `redeemPairingCode`) |
| **Parent Notifications Setup** | Done | Done | Needs backend (FCM) |
| **Child Permissions Setup (7 Rules)**| Done (Native Android dialogs) | UI Only (FamilyControls setup) | N/A |
| **Restricted Settings Inline Helper**| Done (App Details trigger)| N/A | N/A |
| **Protection Off Screen** | Done (Live permission check)| Done | Needs backend (`PROTECTION_DISABLED` event) |
| **Parent Home & Child List** | Done | Done | Needs backend (Firestore sync) |
| **Parent Child Detail & Remote Start**| Done | Done | Needs backend (`startRemoteSession`) |
| **Parent Policy Editor (Category/Apps)**| Done (Domain normalization) | Done (Category picker UI) | Needs backend (Firestore sync) |
| **Parent Schedules Editor** | Done | Done | Needs backend (Firestore sync) |
| **NFC Card Claim & Assign Flow** | Done (Phone & ACR122U Readers)| UI Only (CoreNFC reader UI) | Needs backend (`claimCard`, `revokeCard`) |
| **Child Home & Active Focus Ring** | Done | Done | Needs backend (Firestore sync) |
| **Android App Blocking Service** | Done (`TYPE_WINDOW_STATE_CHANGED`)| UI Only (ManagedSettings mock)| N/A |
| **Android Web Blocking (Browsers)**| Done (Chrome/Firefox URL read)| UI Only (Safari Filter mock) | N/A |
| **Child Emergency Request Flow** | Done (15m timer dialog) | Done (UI) | Needs backend (`handleEmergencyRequest`) |
| **Admin Console (Overview, Users, Devices, Cards, Revenue)** | Done (Local Admin Repo) | Done (Local Admin Repo) | Needs backend (`adminStats`, `programCards`) |
| **Dev Role Switcher Modal** | Done (Demo flavor only) | Done (Demo flavor only) | N/A |
| **Firebase Stubs & Schema Prepared**| Done (Interfaces & Stubs) | Done (Interfaces & Stubs) | Done (`firestore.rules`, `functions/`) |
| **Firebase Production Credentials**| Pending User Provision | Pending User Provision | Pending User Provision |
