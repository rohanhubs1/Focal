# FOCAL — Cost Estimation & Operations Analysis (05_COSTS.md)

## 1. Fixed Setup & Operational Costs
- **Google Play Console Developer Account**: $25 (One-time registration fee).
- **Apple Developer Program**: $99 / year (Required for iOS distribution & FamilyControls entitlement).
- **Firebase Blaze Plan**: $0 base cost (Pay-as-you-go above free tier; required for Cloud Functions Node.js runtime).
- **Domain & Transactional Email (Optional)**: ~$12 / year for domain name; free tier for Resend / SendGrid.

---

## 2. Per-Device Daily Telemetry Budgeting
By batching telemetry (combining heartbeat status, app usage updates, and block counts into a single write every 15 minutes):

- **Writes per Child Device per Day**:
  - 1 batch update every 15 minutes = 4 writes / hour * 24 hours = **96 writes / day**.
  - Average 4 session start/stop events = **4 writes / day**.
  - Total per device = **~100 writes / day**.

- **Reads per Child Device per Day**:
  - Policy & schedule real-time listener sync on launch/change = **~10 reads / day**.
  - Parent monitoring app reading status (assuming parent opens app 5 times/day) = **~50 reads / day**.
  - Total per device = **~60 reads / day**.

---

## 3. Monthly Firebase Infrastructure Cost Estimates
*Note: Firebase Free Quotas per day / month:*
- *Firestore Reads Free Tier*: 50,000 / day
- *Firestore Writes Free Tier*: 20,000 / day
- *Cloud Functions Free Tier*: 2,000,000 invocations / month
- *Firebase Auth (Email/Custom Tokens)*: Free up to 50,000 MAU
- *Cloud Messaging (FCM)*: Free unlimited

*Rates assumed above free tier (Subject to verification on [firebase.google.com/pricing](https://firebase.google.com/pricing)):*
- *Firestore Reads*: $0.06 per 100,000
- *Firestore Writes*: $0.18 per 100,000

### Cost Table per Active Scale
| Scale (Children) | Daily Writes | Daily Reads | Monthly Writes | Monthly Reads | Functions Call / mo | Estimated Monthly Firebase Cost | Total Monthly Running Cost |
|---|---|---|---|---|---|---|---|
| **100 Children** | 10,000 | 6,000 | 300,000 | 180,000 | ~150,000 | **$0.00** (Within Free Tier) | **$8.25** (Apple Dev split) |
| **1,000 Children** | 100,000 | 60,000 | 3,000,000 | 1,800,000 | ~1,500,000 | **~$12.96** | **~$21.21** |
| **10,000 Children**| 1,000,000 | 600,000 | 30,000,000 | 18,000,000 | ~15,000,000| **~$148.80** | **~$157.05** |

---

## 4. Cost-Optimization Architectural Rules
1. **Batching Telemetry**: Heartbeats, usage statistics, and block attempt counters are aggregated in local sqlite memory and written to Firestore once every 15 minutes.
2. **Zero Per-Second Firestore Writes**: Local timers run in-memory on device. Firestore is **never** written to per second during active focus countdowns.
3. **Scoped Screen Listeners**: Real-time snapshots (`addSnapshotListener`) attach only when screens are in the foreground and detach immediately upon `onStop` / screen pop.
4. **TTL Cleanup Policy**: Old events and ephemeral pairing codes are automatically deleted after 30 days via Firestore TTL policies or Cloud Firestore trigger cleanups.
