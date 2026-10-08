import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

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
 * 3. claimCard — Registers an NFC card to a parent account.
 */
export const claimCard = functions.https.onCall(async (data, context) => {
  if (!context.auth || context.auth.token.role !== "parent") {
    throw new functions.https.HttpsError("unauthenticated", "Parent role required.");
  }
  const { rawNfcToken, childId } = data;
  const parentId = context.auth.uid;

  const cardQuery = await db.collection("cards").where("token", "==", rawNfcToken).limit(1).get();
  if (cardQuery.empty) {
    throw new functions.https.HttpsError("not-found", "Card token not found in system inventory.");
  }

  const cardDoc = cardQuery.docs[0];
  const cardData = cardDoc.data();

  if (cardData.status === "deactivated") {
    throw new functions.https.HttpsError("failed-precondition", "This card has been deactivated.");
  }

  await cardDoc.ref.update({
    ownerParentId: parentId,
    assignedChildId: childId || null,
    status: childId ? "assigned" : "registered",
    registeredAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  return { success: true, cardId: cardDoc.id, message: "Card successfully claimed." };
});

/**
 * 4. revokeCard — Revokes an NFC card.
 */
export const revokeCard = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required.");
  }
  const { cardId } = data;
  const cardRef = db.collection("cards").doc(cardId);
  await cardRef.update({
    status: "deactivated",
    ownerParentId: null,
    assignedChildId: null,
  });
  return { success: true, message: "Card deactivated." };
});

/**
 * 5. programCards — Admin batch card generation.
 */
export const programCards = functions.https.onCall(async (data, context) => {
  if (!context.auth || context.auth.token.role !== "admin") {
    throw new functions.https.HttpsError("permission-denied", "Admin role required.");
  }
  const { count } = data;
  const generated: any[] = [];
  const batch = db.batch();

  for (let i = 0; i < (count || 10); i++) {
    const cardId = `FC${Math.floor(1000 + Math.random() * 9000)}`;
    const cardRef = db.collection("cards").doc(cardId);
    const token = `TK_${Math.floor(100000 + Math.random() * 900000)}`;
    const cardData = {
      cardId,
      token,
      status: "in_stock",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    };
    batch.set(cardRef, cardData);
    generated.push(cardData);
  }

  await batch.commit();
  return { success: true, generatedCards: generated };
});

/**
 * 6. createFocusQr — Generates ephemeral focus session QR code.
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
 * 7. validateFocusQr — Validates ephemeral focus session QR code.
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
 * 8. startRemoteSession — Remote start focus session on child device.
 */
export const startRemoteSession = functions.https.onCall(async (data, context) => {
  if (!context.auth || context.auth.token.role !== "parent") {
    throw new functions.https.HttpsError("unauthenticated", "Parent role required.");
  }
  const { childId, policyId, durationMinutes } = data;
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
 * 9. endRemoteSession — Remote end focus session.
 */
export const endRemoteSession = functions.https.onCall(async (data, context) => {
  if (!context.auth || context.auth.token.role !== "parent") {
    throw new functions.https.HttpsError("unauthenticated", "Parent role required.");
  }
  const { sessionId } = data;
  await db.collection("sessions").doc(sessionId).update({
    status: "ENDED_EARLY_PARENT",
    actualEndTime: admin.firestore.FieldValue.serverTimestamp(),
  });
  return { success: true };
});

/**
 * 10. handleEmergencyRequest — Emergency exit request processing.
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
    await db.collection("sessions").doc(sessionId).update({
      status: "ENDED_EARLY_EMERGENCY",
      actualEndTime: admin.firestore.FieldValue.serverTimestamp(),
    });
    return { status: "APPROVED" };
  }
  return { status: "DECLINED" };
});

/**
 * 11. heartbeatWatcher — Scheduled function running every 15 min for offline detection.
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
 * 12. adminStats — Aggregates admin telemetry numbers.
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
 * 13. subscriptionWebhooks — Razorpay Payment Gateway Webhook handler.
 */
export const subscriptionWebhooks = functions.https.onRequest(async (req, res) => {
  // Webhook payload validation & processing placeholder
  res.status(200).send({ status: "ok" });
});
