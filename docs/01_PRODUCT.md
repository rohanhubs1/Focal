# FOCAL — Product Blueprint (01_PRODUCT.md)

## 1. Executive Product Summary
**FOCAL** is a distraction-free focus enforcement platform designed to give parents absolute control over their children's screen time while empowering students to build deep focus habits. Unlike traditional monitoring apps that rely on passive notifications or easily bypassed timers, FOCAL combines local device enforcement (Android Accessibility Services & Device Admin) with physical NFC card tap mechanics, QR verification, and automated schedules. When focus mode is active, blocked applications and websites are instantly suppressed, presenting a minimal monochrome shield screen that cannot be dismissed without authorized exit credentials.

## 2. Target Users & Personas
- **Parents / Controllers**: Demands reliable, tamper-proof control over their child’s phone usage during study hours, dinner, and sleep routines. Desires real-time status alerts, activity reports, and effortless schedule configuration without complex setup.
- **Children / Target Device Users**: Students needing structured boundaries to avoid social media and gaming notifications during study sessions. Can initiate focus sessions independently using a physical FOCAL NFC card or QR code, or request emergency session exits when valid reasons arise.
- **System Administrators**: Operational managers monitoring system-wide telemetry, device health, card inventory, user subscriptions, and audit security.

## 3. Platform Strategy & Scope
- **Android First**: Primary platform for full local enforcement. Utilizes Accessibility Service (`TYPE_WINDOW_STATE_CHANGED`), Usage Stats Manager, Overlay Permissions (`SYSTEM_ALERT_WINDOW`), Device Admin (uninstall protection), and background WorkManager heartbeats.
- **iOS (v1 Scope)**: Controller/Parent app fully functional. Child device enforcement relies on Apple Screen Time API (`FamilyControls` & `ManagedSettings` frameworks) with native category/app selection pickers.
- **v1 Explicit Exclusions**:
  - *No Solo Mode*: FOCAL is built exclusively for parent-child focus management and device protection.
  - *No Social or Leaderboards*: Zero gamification, public badges, or social feeds. Total focus on privacy and productivity.
  - *No Location Tracking*: FOCAL enforces screen boundaries, not geographical tracking.

## 4. Business Model & Pricing Structure
- **Parent Account Subscription**:
  - Every parent account begins with a **3-month free trial**.
  - Following the trial, subscription is **₹299 / month** per family (covers unlimited child profiles and devices).
- **Physical FOCAL NFC Card**:
  - Optional one-time hardware purchase at **₹999 / card**.
  - Provides instant tactile tap-to-focus and tap-to-end functionality.
- **Children Profiles**:
  - Child profiles are zero-cost managed sub-accounts under a parent. Children do not have email accounts, passwords, or direct billing plans.
