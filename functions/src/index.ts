import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { readFileSync } from "fs";
import { join } from "path";
import * as logic from "./nfcguard/cardLogic";
import { Caller, CardRecord, CardStore, Keys, OrderRecord, TapInput, TapLog } from "./nfcguard/cardLogic";
import { loadPublicKeyPem } from "./nfcguard/cardCrypto";

admin.initializeApp();
const db = admin.firestore();

// ---------------------------------------------------------
// NFC Guard Cryptographic Engine (5-Check Security Gate)
// ---------------------------------------------------------

const keys: Keys = {
  ourPublicKey: loadPublicKeyPem(readFileSync(join(__dirname, "..", "keys", "public_key.pem"), "utf8")),
};
const requireGenuine = process.env.NFCGUARD_ALLOW_NON_NXP !== "true";
const ACTIVE_SUBSCRIPTION = ["trial", "active"];

/** CardStore over one Firestore transaction: reads go through it, writes are queued and applied last. */
class TxStore implements CardStore {
  private writes: Array<() => void> = [];
  constructor(private tx: FirebaseFirestore.Transaction) {}

  async getCard(serial: number) {
    const s = await this.tx.get(db.collection("cards").doc(String(serial)));
    return s.exists ? (s.data() as CardRecord) : null;
  }
  async getOrder(orderId: string) {
    const s = await this.tx.get(db.collection("cardOrders").doc(orderId));
    return s.exists ? (s.data() as OrderRecord) : null;
  }
  async parentExists(parentId: string) {
    return (await this.tx.get(db.collection("parents").doc(parentId))).exists;
  }
  async subscriptionActive(parentId: string) {
    const s = await this.tx.get(db.collection("parents").doc(parentId));
    return s.exists && ACTIVE_SUBSCRIPTION.includes(s.get("subscriptionStatus"));
  }
  async putCard(card: CardRecord) {
    this.writes.push(() => this.tx.set(db.collection("cards").doc(String(card.serial)), card));
  }
  async putOrder(order: OrderRecord) {
    this.writes.push(() => this.tx.set(db.collection("cardOrders").doc(order.orderId), order));
  }
  async logTap(entry: TapLog) {
    this.writes.push(() => this.tx.set(db.collection("cardTaps").doc(),
      { ...entry, at: admin.firestore.FieldValue.serverTimestamp() }));
  }
  flush() { this.writes.forEach((w) => w()); }
}

function inTx<T>(fn: (store: TxStore) => Promise<T>): Promise<T> {
  return db.runTransaction(async (tx) => {
    const store = new TxStore(tx);
    const result = await fn(store);
    store.flush();
    return result;
  });
}

/** Verified caller from Firebase Auth claims */
function callerOf(context: functions.https.CallableContext): Caller {
  const t = context.auth?.token;
  if (!context.auth || !t) throw new functions.https.HttpsError("unauthenticated", "Sign in first.");
  const role = t.role;
  if (role !== "parent" && role !== "child" && role !== "admin") {
    throw new functions.https.HttpsError("permission-denied", "Unknown role.");
  }
  return {
    uid: context.auth.uid,
    role,
    parentId: role === "parent" ? context.auth.uid : (t.parentId ?? null),
    childId: role === "child" ? (t.childId ?? null) : null,
  };
}

function need<T>(ok: boolean, value: T, msg: string): T {
  if (!ok) throw new functions.https.HttpsError("invalid-argument", msg);
  return value;
}

function tapInput(data: any): TapInput {
  return {
    uid: need(typeof data?.uid === "string" && /^[0-9a-fA-F]{14}$/.test(data.uid), data?.uid, "uid: 14 hex chars"),
    payload: need(typeof data?.payload === "string", data?.payload, "payload: base64"),
    nxpSignature: need(typeof data?.nxpSignature === "string", data?.nxpSignature, "nxpSignature: base64"),
    counter: need(Number.isInteger(data?.counter), data?.counter, "counter: integer"),
  };
}

/** Child phone, every tap: checks 1-5. Family = the child's parentId from its login token. */
export const verifyCard = functions.https.onCall(async (data, context) => {
  const caller = callerOf(context);
  const input = tapInput(data);
  return inTx((store) => logic.verifyCard(store, input, caller, keys, requireGenuine));
});

/** Parent: serial (printed on the card) + activation code (order e-mail) + tap. */
export const registerCard = functions.https.onCall(async (data, context) => {
  const caller = callerOf(context);
  if (caller.role !== "parent") throw new functions.https.HttpsError("permission-denied", "Parent only.");
  const input = tapInput(data);
  const serial = need(Number.isInteger(data?.serial), data?.serial as number, "serial: integer");
  const code = need(typeof data?.code === "string", data?.code as string, "code: string");
  const local = logic.localChecks(input, keys, requireGenuine);
  if (!local.passed || local.payload?.serial !== serial) {
    return { ok: false, reason: local.checks.find((c) => !c.passed)?.detail ?? "card does not carry this serial" };
  }
  const [ok, reason] = await inTx((store) => logic.registerCard(store, serial, code, caller.uid, input.uid, input.counter));
  return { ok, reason };
});

/** Parent: limit a card to one child (or null for any child of the family). */
export const assignCardToChild = functions.https.onCall(async (data, context) => {
  const caller = callerOf(context);
  const serial = need(Number.isInteger(data?.serial), data?.serial as number, "serial: integer");
  const childId = data?.childId ?? null;
  if (childId !== null) {
    const child = await db.collection("children").doc(String(childId)).get();
    if (!child.exists || child.get("parentId") !== caller.uid) {
      throw new functions.https.HttpsError("permission-denied", "Not your child.");
    }
  }
  const [ok, reason] = await inTx((store) => logic.assignCard(store, serial, childId, caller));
  return { ok, reason };
});

/** Parent (own card) or admin: disable a lost card. */
export const revokeCard = functions.https.onCall(async (data, context) => {
  const caller = callerOf(context);
  const serial = need(Number.isInteger(data?.serial), data?.serial as number, "serial: integer");
  const [ok, reason] = await inTx((store) => logic.revokeCard(store, serial, caller));
  if (!ok) throw new functions.https.HttpsError("permission-denied", reason);
  return { ok, reason };
});

/** Admin: sell cards. Returns the activation code for the order e-mail. */
export const createOrder = functions.https.onCall(async (data, context) => {
  const caller = callerOf(context);
  const serials = need(Array.isArray(data?.serials) && data.serials.every(Number.isInteger),
    data?.serials as number[], "serials: integer array");
  try {
    return await inTx((store) => logic.createOrder(store, serials, caller, data?.parentId ?? null));
  } catch (e) {
    throw new functions.https.HttpsError("failed-precondition", (e as Error).message);
  }
});

/** Admin / factory station: record a provisioned card as unassigned. */
export const addInventoryCard = functions.https.onCall(async (data, context) => {
  const caller = callerOf(context);
  const serial = need(Number.isInteger(data?.serial), data?.serial as number, "serial: integer");
  const uid = need(typeof data?.uid === "string" && /^[0-9a-fA-F]{14}$/.test(data.uid), data?.uid as string, "uid");
  const batch = need(Number.isInteger(data?.batch), data?.batch as number, "batch: integer");
  const [ok, reason] = await inTx((store) => logic.addInventoryCard(store, serial, uid, batch, caller));
  if (!ok) throw new functions.https.HttpsError("failed-precondition", reason);
  return { ok, reason };
});

// Legacy aliases for backward compatibility
export const claimCard = functions.https.onCall(async (data, context) => {
  return registerCard(data, context);
});

export const programCards = functions.https.onCall(async (data, context) => {
  if (!context.auth || context.auth.token.role !== "admin") {
    throw new functions.https.HttpsError("permission-denied", "Admin role required.");
  }
  return { success: true, message: "Cards are factory-provisioned via station.py." };
});

// ---------------------------------------------------------
// Device Pairing & Session Management
// ---------------------------------------------------------

/**
 * 1. createPairingCode — Generates a 6-digit numeric pairing code + QR payload.
 */
export const createPairingCode = functions.https.onCall(async (data, context) => {
  if (!context.auth || context.auth.token.role !== "parent") {
    throw new functions.https.HttpsError("unauthenticated", "Parent role required.");
  }
  const { childName, age } = data;
  const parentId = context.auth.uid;
  const pairingCode = Math.floor(100000 + Math.random() * 900000).toString();
  const expiresAt = new Date(Date.now() + 10 * 60 * 1000).toISOString();

  const codeRef = db.collection("pairingCodes").doc();
  await codeRef.set({
    code: pairingCode,
    parentId,
    childName,
    age,
    status: "PENDING",
    expiresAt,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  return {
    codeId: codeRef.id,
    pairingCode,
    qrPayload: `FOCAL_PAIR_${pairingCode}`,
    expiresAt,
  };
});

/**
 * 2. redeemPairingCode — Mints a custom Firebase auth token for the child device.
 */
export const redeemPairingCode = functions.https.onCall(async (data) => {
  const { pairingCode, deviceHardwareKey, deviceName, platform } = data;
  const snapshot = await db.collection("pairingCodes")
    .where("code", "==", pairingCode)
    .where("status", "==", "PENDING")
    .limit(1)
    .get();

  if (snapshot.empty) {
    throw new functions.https.HttpsError("not-found", "Invalid or expired pairing code.");
  }

  const codeDoc = snapshot.docs[0];
  const codeData = codeDoc.data();

  // Create child document
  const childRef = db.collection("children").doc();
  await childRef.set({
    parentId: codeData.parentId,
    name: codeData.childName,
    age: codeData.age,
    isOnline: true,
    isFocusing: false,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  // Create device document
  const deviceRef = db.collection("devices").doc();
  await deviceRef.set({
    childId: childRef.id,
    parentId: codeData.parentId,
    deviceName: deviceName || "Child Phone",
    platform: platform || "android",
    hardwarePublicKey: deviceHardwareKey,
    lastHeartbeatAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  // Mark code redeemed
  await codeDoc.ref.update({ status: "REDEEMED", childId: childRef.id });

  // Mint Custom Token with Child Claims
  const customToken = await admin.auth().createCustomToken(deviceRef.id, {
    role: "child",
    childId: childRef.id,
    deviceId: deviceRef.id,
    parentId: codeData.parentId,
  });

  return {
    firebaseCustomToken: customToken,
    childId: childRef.id,
    deviceId: deviceRef.id,
    parentId: codeData.parentId,
  };
});

/**
 * 3. createFocusQr — Generates ephemeral focus session QR code.
 */
export const createFocusQr = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Authentication required.");
  const { childId, policyId, durationMinutes } = data;
  const qrToken = `FOCAL_QR_${Math.random().toString(36).substring(2, 10).toUpperCase()}`;
  const expiresAt = new Date(Date.now() + 10 * 60 * 1000).toISOString();

  await db.collection("pairingCodes").add({
    qrToken,
    childId,
    policyId,
    durationMinutes,
    status: "ACTIVE",
    expiresAt,
  });

  return { qrToken, expiresAt };
});

/**
 * 4. validateFocusQr — Validates ephemeral focus session QR code.
 */
export const validateFocusQr = functions.https.onCall(async (data) => {
  const { qrToken } = data;
  const snap = await db.collection("pairingCodes").where("qrToken", "==", qrToken).where("status", "==", "ACTIVE").limit(1).get();
  if (snap.empty) {
    return { valid: false, message: "Invalid or expired QR code." };
  }
  return { valid: true, data: snap.docs[0].data() };
});

/**
 * 5. startRemoteSession — Remote start focus session on child device with family check.
 */
export const startRemoteSession = functions.https.onCall(async (data, context) => {
  if (!context.auth || context.auth.token.role !== "parent") {
    throw new functions.https.HttpsError("unauthenticated", "Parent role required.");
  }
  const { childId, policyId, durationMinutes } = data;
  const childDoc = await db.collection("children").doc(childId).get();
  if (!childDoc.exists || childDoc.data()?.parentId !== context.auth.uid) {
    throw new functions.https.HttpsError("permission-denied", "Child does not belong to your family.");
  }

  const sessionRef = db.collection("sessions").doc();
  await sessionRef.set({
    childId,
    parentId: context.auth.uid,
    policyId,
    startedBy: "REMOTE_PARENT",
    startTime: admin.firestore.FieldValue.serverTimestamp(),
    scheduledEndTime: new Date(Date.now() + (durationMinutes || 60) * 60 * 1000).toISOString(),
    status: "ACTIVE",
  });
  return { sessionId: sessionRef.id, status: "ACTIVE" };
});

/**
 * 6. endRemoteSession — Remote end focus session with family ownership check.
 */
export const endRemoteSession = functions.https.onCall(async (data, context) => {
  if (!context.auth || context.auth.token.role !== "parent") {
    throw new functions.https.HttpsError("unauthenticated", "Parent role required.");
  }
  const { sessionId } = data;
  const sessionDoc = await db.collection("sessions").doc(sessionId).get();
  if (!sessionDoc.exists || sessionDoc.data()?.parentId !== context.auth.uid) {
    throw new functions.https.HttpsError("permission-denied", "Session does not belong to your family.");
  }

  await sessionDoc.ref.update({
    status: "ENDED_EARLY_PARENT",
    actualEndTime: admin.firestore.FieldValue.serverTimestamp(),
  });
  return { success: true };
});

/**
 * 7. handleEmergencyRequest — Emergency exit request processing (child cannot approve).
 */
export const handleEmergencyRequest = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Auth required.");
  const { action, sessionId, childId, reason } = data;
  if (action === "REQUEST") {
    await db.collection("events").add({
      childId,
      eventType: "EMERGENCY_REQUESTED",
      reason: reason || "Emergency exit requested",
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
    });
    return { status: "PENDING_PARENT_APPROVAL" };
  } else if (action === "APPROVE") {
    if (context.auth.token.role !== "parent") {
      throw new functions.https.HttpsError("permission-denied", "Child cannot approve emergency exit.");
    }
    const sessionDoc = await db.collection("sessions").doc(sessionId).get();
    if (!sessionDoc.exists || sessionDoc.data()?.parentId !== context.auth.uid) {
      throw new functions.https.HttpsError("permission-denied", "Session does not belong to your family.");
    }
    await sessionDoc.ref.update({
      status: "ENDED_EARLY_EMERGENCY",
      actualEndTime: admin.firestore.FieldValue.serverTimestamp(),
    });
    return { status: "APPROVED" };
  }
  return { status: "DECLINED" };
});

/**
 * 8. heartbeatWatcher — Scheduled function running every 15 min for offline detection.
 */
export const heartbeatWatcher = functions.pubsub.schedule("every 15 minutes").onRun(async () => {
  const thirtyMinAgo = new Date(Date.now() - 30 * 60 * 1000);
  const snap = await db.collection("devices").where("lastHeartbeatAt", "<=", thirtyMinAgo).get();

  snap.forEach(async (doc) => {
    const device = doc.data();
    await db.collection("notifications").add({
      recipientUserId: device.parentId,
      type: "DEVICE_OFFLINE",
      title: "Device Offline",
      message: `No signal from device for 30 minutes.`,
      isRead: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });
});

/**
 * 9. adminStats — Aggregates admin telemetry numbers.
 */
export const adminStats = functions.https.onCall(async (data, context) => {
  if (!context.auth || context.auth.token.role !== "admin") {
    throw new functions.https.HttpsError("permission-denied", "Admin role required.");
  }
  const parentsCount = (await db.collection("parents").count().get()).data().count;
  const childrenCount = (await db.collection("children").count().get()).data().count;
  const activeSessions = (await db.collection("sessions").where("status", "==", "ACTIVE").count().get()).data().count;
  const cardsCount = (await db.collection("cards").count().get()).data().count;

  return {
    totalParents: parentsCount,
    activeChildren: childrenCount,
    activeSessions,
    totalCards: cardsCount,
    revenueInr: parentsCount * 299 + cardsCount * 999,
  };
});

/**
 * 10. subscriptionWebhooks — Razorpay Payment Gateway Webhook handler.
 */
export const subscriptionWebhooks = functions.https.onRequest(async (req, res) => {
  res.status(200).send({ status: "ok" });
});
