# Google Play Submission & Compliance Preparation

## 1. Accessibility Service Declaration Text

### Prominent Disclosure Text (In-App)
> **Title**: How FOCAL protects your child's phone
>
> **Declaration**:
> FOCAL uses the Android Accessibility Service (`BIND_ACCESSIBILITY_SERVICE`) strictly for parental control and focus enforcement authorized by the child's parent/guardian.
>
> **What the Accessibility Service does**:
> 1. **App & Website Barrier**: Detects when restricted apps or websites are launched during active focus routines and displays the FOCAL focus barrier shield.
> 2. **Protection Guard**: Prevents children from bypassing focus routines, force-stopping FOCAL, clearing app storage/cache, or disabling protection permissions without parent authorization.
>
> **Privacy & Data Handling**:
> FOCAL does **NOT** read personal messages, typed text, screen content, banking credentials, or private app data beyond detecting window titles and app package names to identify blocked apps and settings screens. All telemetry data (installed app names, focus session duration, and block attempt counts) is strictly transmitted to the child's linked parent account over encrypted connections and is never shared with third parties.

---

## 2. Play Console Video Shot List (Required for Accessibility API Approval)

To obtain Google Play approval for `BIND_ACCESSIBILITY_SERVICE`, record a clean video demonstrating the following exact sequence:

| Scene # | Time | Description |
| :--- | :--- | :--- |
| **Scene 1** | `0:00 - 0:08` | **Prominent Disclosure Screen**: Show the full-screen *"How FOCAL protects your child's phone"* screen detailing Accessibility usage, Device Admin, and privacy terms during child onboarding. |
| **Scene 2** | `0:08 - 0:16` | **Parent Explicit Consent**: Tap the *"I agree, I'm the parent"* button to proceed to permission requests. |
| **Scene 3** | `0:16 - 0:25` | **Enabling Accessibility Service**: System settings opens to FOCAL Focus Barrier Service -> toggle ON -> confirm system prompt. |
| **Scene 4** | `0:25 - 0:35` | **Focus App Barrier**: Start a 60-minute "Study Mode" session. Open a blocked app (e.g., Instagram or YouTube) and show FOCAL intercepting it cleanly with the focus barrier shield. |
| **Scene 5** | `0:35 - 0:48` | **Settings Guard Protection**: Open Android System Settings -> FOCAL App Info -> tap *"Force Stop"* or *"Uninstall"*. Show FOCAL's Settings Guard intercepting the screen, executing BACK/HOME, and displaying the *"Locked by FOCAL — Ask your parent"* notice. |

---

## 3. Data Safety Form Answers

### Data Collection & Usage
- **App Information & Performance**:
  - *Installed Apps*: Collected & shared with linked parent account to allow policy configuration.
  - *App Usage Stats*: Collected & shared with linked parent account to show daily screen time & focus score.
- **Device or Other Identifiers**:
  - *Device Name & Hardware ID*: Collected to pair child device with parent account.
  - *NFC Card Token*: Collected to pair physical hardware FOCAL card to child profile.
- **Data Security**:
  - All data is encrypted in transit using TLS 1.3 / HTTPS.
  - Data can be deleted at any time upon parent account deletion.

---

## 4. Children's Data & Privacy Policy Points (COPPA & Play Families Policy)

1. **Parental Consent**: Account creation and device pairing require parent authentication.
2. **No Data Selling or Advertising**: FOCAL contains zero ad networks, third-party analytics, or commercial data selling.
3. **Data Minimization**: Only collects app names, screen time minutes, and protection health metrics required to enforce focus rules set by the parent.
4. **Data Retention & Deletion**: Deleting the parent account immediately purges all child profile data, session histories, and paired card tokens from system storage.
