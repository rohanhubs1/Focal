# FOCAL — Digital Wellbeing & Focus-Control Platform

FOCAL is a production-ready mobile platform built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**. It allows parents and controllers to create focus boundaries for children and users, restricting distracting apps and websites during focus sessions using either a **Physical NFC Focus Card** or a **Dynamic Optical QR-Based Focus System**, driven by a central **Focus Engine**.

---

## 💎 Product Business Model

### Option A — Physical NFC Focus Card
* **Upfront Price:** ₹999 one-time (exclusively for physical hardware card & shipping).
* **Trial Period:** **3 Months Free**.
* **Post-Trial Renewal:** **₹299/month** subscription.
* **Customer Journey:** Choose NFC → Pay ₹999 → Card Order & Assignment → Account Activated → 3 Months Free → Full App Access → Renewal at ₹299/month.

### Option B — Digital QR Focus
* **Upfront Price:** **₹0** (no hardware purchase).
* **Trial Period:** **3 Months Free**.
* **Post-Trial Renewal:** **₹299/month** subscription.
* **Customer Journey:** Choose QR → Create Account → Account Activated (₹0) → 3 Months Free → Full App Access → Renewal at ₹299/month.

> **Crucial Rule:** The ₹999 fee applies *only* to the physical NFC card. QR users are never charged ₹999. Both methods transition to the same ₹299/month plan following their 3-month free trial.

---

## 🎨 Design System: Premium Dark Theme

* **Background:** `#080A0F` (Deep Charcoal)
* **Secondary Background:** `#10131A`
* **Card Surface:** `#151923`
* **Borders:** `#252B38` with glowing accent highlights
* **Primary Accent:** Electric Violet / Purple (`#8B5CF6`)
* **Secondary Accent:** Electric Blue (`#3B82F6`)
* **Status Colors:** Success (`#10B981`), Warning (`#F59E0B`), Error (`#EF4444`)
* **Cards:** 16–20px rounded corners, sleek borders, subtle gradients, and crisp typography.

---

## 👥 Three Core Roles & User Journeys

### 1. Child / User Role (e.g. Rahul & Ananya)
* **Live Dashboard:** Streak counter (🔥 12 Days), today's focus time (`02h 35m`), screen time (`03h 12m`), focus score (`82`), and time saved (`01h 40m`).
* **Start Focus via NFC:** Animated electromagnetic wave pulse, card identifier authentication, and instant restriction activation.
* **Start Focus via QR:** Real-time laser sweep viewfinder scanning secure backend tokens.
* **Active Focus Experience:** Fullscreen circular countdown timer, mode indicator, blocked app counter, website counter, and emergency exit request modal.
* **Calm Blocked App Screen:** Motivational, peaceful dark screen when restricted apps (e.g. Instagram, YouTube) are attempted during focus.
* **Focus Summary:** Complete post-session breakdown with duration, prevented distractions, and score.
* **Schedules, Insights & Community:** Recurring routines, daily focus vs. screen time charts, friend circles, and global focus pulse.

### 2. Controller / Parent Role (e.g. Priya Sharma)
* **Family Dashboard:** Live child status (Online, Focus Active, remaining session time).
* **Add Child & Device Pairing:** Generates a secure Pairing QR for the child device to scan.
* **Focus Policy Builder:** Mode selection (Study, Deep Work, Work, Sleep, Digital Detox, Custom), duration picker, app picker, website picker, and emergency exit approval toggles.
* **Focus QR Generator:** Generates verifiable QR tokens for children to scan.
* **Emergency Exit Approvals:** Real-time push alert handling to approve or reject child exit requests.

### 3. Super Admin Role (e.g. Aman Verma)
* **Platform Metrics:** Total users, NFC customers, QR customers, active trials, active subscriptions, monthly recurring revenue (MRR), and NFC card sales.
* **User Management:** Search and filter users by role, plan, and status.
* **NFC Card Programmer Utility:** Generate Card IDs (e.g. `FC0001`), generate secure tokens (`8XK92LM7`), enter NFC UIDs (`04:A3:91:7B:22:6C`), write NDEF payloads, program, activate, and assign cards.
* **QR Token Management:** Issue, rotate, and revoke backend QR tokens.
* **Subscription & Revenue Tracking:** Real-time visibility into customer renewal lifecycles.

---

## ⚡ Central Focus Engine Architecture

Both NFC and QR triggers converge into the unified `FocusEngine`:

```
   Physical NFC Card                   Digital Focus QR
          │                                   │
          ▼                                   ▼
   NFC Hardware UID / Token            Secure Optical Token
          │                                   │
          └─────────────────┬─────────────────┘
                            ▼
               Server-Side Verification
         (Subscription check, Device validation)
                            │
                            ▼
                   Central Focus Engine
   ┌─────────────────────────────────────────────────┐
   │ • State Machine: IDLE → ACTIVE → COMPLETED      │
   │ • High-Precision Real-time Countdown Timer      │
   │ • App & Website Restriction Barrier             │
   │ • Blocked Distraction Attempt Interceptor       │
   │ • Emergency Exit Workflow with Parent Approval  │
   │ • Analytics & Score Generator                   │
   └─────────────────────────────────────────────────┘
```

---

## 🛠️ Codebase Structure

```
d:/Focal/
├── androidApp/                         # Android Host Application
│   └── src/main/kotlin/com/example/myapplication/
│       └── MainActivity.kt            # Edge-to-edge Compose host with NFC Intent & Vibration haptics
├── shared/                             # Compose Multiplatform Shared Code
│   └── src/commonMain/kotlin/
│       ├── com/example/myapplication/
│       │   └── App.kt                 # Shared entry point launching FocalRootApp
│       └── com/focal/app/
│           ├── theme/
│           │   └── FocalTheme.kt      # Electric dark color palette, typography & shapes
│           ├── model/
│           │   └── FocalModels.kt     # Users, Policies, Sessions, Cards, Subscriptions
│           ├── data/
│           │   └── FocalRepository.kt # Reactive repository with live mock data & server-side validation
│           ├── engine/
│           │   └── FocusEngine.kt     # Unified state engine for NFC & QR sessions
│           └── ui/
│               ├── FocalRootApp.kt    # Root coordinator, bottom navigation & global modals
│               ├── components/
│               │   ├── FocalIcons.kt  # Custom high-performance Vector icons
│               │   ├── FocalComponents.kt # DarkCard, GradientButton, FocusTimer, Skeleton
│               │   ├── NfcDialogs.kt  # Animated NFC reader modal & tap simulator
│               │   ├── QrDialogs.kt   # Laser-scanning QR modal & QR code presenter
│               │   ├── PaymentDialog.kt # Realistic ₹999 NFC vs ₹0 QR trial checkout
│               │   ├── BlockedAppDialog.kt # Motivational blocked screen & emergency exit
│               │   └── RoleSwitcherModal.kt # Seamless live role-switching switcher
│               ├── auth/
│               │   └── AuthScreens.kt # Splash, 4-step Onboarding, Login, SignUp, Plan Picker
│               ├── child/
│               │   └── ChildScreens.kt# Dashboard, Active Focus, Summary, Insights, Friends
│               ├── controller/
│               │   └── ControllerScreens.kt # Parent console, Child detail, Policy creator
│               └── admin/
│                   └── AdminScreens.kt# Admin overview, Card programmer, QR tokens, Revenue
```

---

## 🚀 Building & Running

### Android Application
```bash
./gradlew :androidApp:assembleDebug
```
The compiled APK will be created at:
`androidApp/build/outputs/apk/debug/androidApp-debug.apk`

### Testing & Verification
```bash
./gradlew :shared:compileAndroidMain
```