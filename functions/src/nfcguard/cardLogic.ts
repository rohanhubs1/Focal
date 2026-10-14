/**
 * Server-side card logic. Port of backend.py + gate.py, independent of
 * Firestore so it can be tested on any PC. index.ts runs each function inside
 * a Firestore transaction through the CardStore interface.
 *
 * The caller's family (parentId) always comes from the Firebase login token,
 * never from the request: a phone cannot claim to be another family.
 *
 * Every function does all its reads before any write (Firestore transactions
 * require that order).
 */
import { createHash, randomBytes, KeyObject } from "crypto";
import { verifyNxpSignature, verifyOurSignature } from "./cardCrypto";
import { CardPayload, parsePayload } from "./payload";

export type CardStatus = "unassigned" | "assigned" | "active" | "revoked";

export interface CardRecord {
  serial: number;
  uid: string;              // lowercase hex, 14 chars
  batch: number;
  status: CardStatus;
  orderId: string | null;
  parentId: string | null;  // the family that owns the card once registered
  assignedChildId?: string | null;
  lastCounter: number;
}

export interface OrderRecord {
  orderId: string;
  codeHash: string;
  parentId: string | null;  // optionally pinned to one family
}

export interface TapLog {
  serial: number;
  uid: string;
  parentId: string;
  childId: string | null;
  counter: number;
  result: string;
}

export interface CardStore {
  getCard(serial: number): Promise<CardRecord | null>;
  getOrder(orderId: string): Promise<OrderRecord | null>;
  parentExists(parentId: string): Promise<boolean>;
  subscriptionActive(parentId: string): Promise<boolean>;
  putCard(card: CardRecord): Promise<void>;
  putOrder(order: OrderRecord): Promise<void>;
  logTap(entry: TapLog): Promise<void>;
}

export interface Caller {
  uid: string;                       // Firebase auth uid
  role: "parent" | "child" | "admin";
  parentId: string | null;           // parent: own uid; child: token.parentId
  childId: string | null;            // child: token.childId
}

export interface Check { n: number; name: string; passed: boolean; detail: string }

export interface TapInput {
  uid: string;           // hex
  payload: string;       // base64, 82 bytes
  nxpSignature: string;  // base64, 32 bytes
  counter: number;
}

export interface Keys { ourPublicKey: KeyObject; nxpPublicKey?: Buffer }

/** Activation codes are stored only as a hash (same as backend._h). */
export function hashCode(code: string): string {
  return createHash("sha256").update(code.trim().toUpperCase()).digest("hex");
}

// ------------------------------------------------------------- checks 1-3
/** The server repeats checks 1-3: a modified app must not be able to skip them. */
export function localChecks(input: TapInput, keys: Keys, requireGenuine = true):
  { checks: Check[]; passed: boolean; payload: CardPayload | null } {
  const checks: Check[] = [];
  const uid = Buffer.from(input.uid ?? "", "hex");
  const nxpSig = Buffer.from(input.nxpSignature ?? "", "base64");

  if (verifyNxpSignature(uid, nxpSig, keys.nxpPublicKey)) {
    checks.push({ n: 1, name: "genuine NXP chip", passed: true, detail: "originality signature valid" });
  } else if (requireGenuine) {
    checks.push({ n: 1, name: "genuine NXP chip", passed: false, detail: "originality signature invalid (clone chip?)" });
    return { checks, passed: false, payload: null };
  } else {
    checks.push({ n: 1, name: "genuine NXP chip", passed: true, detail: "SKIPPED (test mode); signature invalid" });
  }

  let p: CardPayload;
  try {
    p = parsePayload(Buffer.from(input.payload ?? "", "base64"));
  } catch (e) {
    checks.push({ n: 2, name: "our signature", passed: false, detail: `unreadable payload: ${(e as Error).message}` });
    return { checks, passed: false, payload: null };
  }
  if (!verifyOurSignature(keys.ourPublicKey, p.body, p.signature)) {
    checks.push({ n: 2, name: "our signature", passed: false, detail: "payload not signed by our key (self-written/tampered)" });
    return { checks, passed: false, payload: p };
  }
  checks.push({ n: 2, name: "our signature", passed: true, detail: `serial ${p.serial}, batch ${p.batch}` });

  if (!p.uid.equals(uid)) {
    checks.push({ n: 3, name: "UID binding", passed: false,
      detail: `payload UID ${p.uid.toString("hex")} != chip UID ${uid.toString("hex")} (copied payload)` });
    return { checks, passed: false, payload: p };
  }
  checks.push({ n: 3, name: "UID binding", passed: true, detail: uid.toString("hex") });
  return { checks, passed: true, payload: p };
}

// ------------------------------------------------------------- checks 4-5
/** Port of Backend.verify. Records every fresh counter, even on a denied tap. */
export async function serverChecks(store: CardStore, serial: number, uidHex: string, counter: number,
  parentId: string, childId: string | null = null):
  Promise<{ counter: [boolean, string]; server: [boolean, string] }> {
  const uid = uidHex.toLowerCase();
  const c = await store.getCard(serial);
  if (c === null) {
    await store.logTap({ serial, uid, parentId, childId, counter, result: "deny: unknown card" });
    return { counter: [false, "unknown card"], server: [false, "unknown card"] };
  }
  const subscriptionOk = await store.subscriptionActive(parentId);

  const last = c.lastCounter;
  let counterRes: [boolean, string];
  let fresh = false;
  if (counter > last) {
    counterRes = [true, `${counter} > last seen ${last}`];
    fresh = true;
  } else {
    counterRes = [false, `${counter} <= last seen ${last}: replay or cloned card`];
  }

  let server: [boolean, string];
  if (c.uid !== uid) server = [false, "UID not registered for this serial"];
  else if (c.status !== "active") server = [false, `card is ${c.status}`];
  else if (c.parentId !== parentId) server = [false, "card belongs to another family"];
  else if (childId && c.assignedChildId && c.assignedChildId !== childId) server = [false, "card is assigned to another child"];
  else if (!subscriptionOk) server = [false, "subscription inactive"];
  else server = [true, "active, this family, subscription ok"];

  if (fresh) await store.putCard({ ...c, lastCounter: counter });
  const ok = counterRes[0] && server[0];
  await store.logTap({ serial, uid, parentId, childId, counter, result: ok ? "approve" : "deny" });
  return { counter: counterRes, server };
}

export interface VerifyResult { approved: boolean; failedCheck: number | null; reason: string; checks: Check[] }

/** verifyCard: all five checks. The family comes from the caller's login token. */
export async function verifyCard(store: CardStore, input: TapInput, caller: Caller, keys: Keys,
  requireGenuine = true): Promise<VerifyResult> {
  if (caller.role !== "child" || !caller.parentId) {
    return { approved: false, failedCheck: null, reason: "only a paired child device can verify a tap", checks: [] };
  }
  const local = localChecks(input, keys, requireGenuine);
  const checks = [...local.checks];
  if (!local.passed || !local.payload) {
    const f = checks.find((c) => !c.passed);
    return { approved: false, failedCheck: f?.n ?? null, reason: f?.detail ?? "invalid card", checks };
  }
  const s = await serverChecks(store, local.payload.serial, input.uid, input.counter, caller.parentId, caller.childId);
  checks.push({ n: 4, name: "tap counter", passed: s.counter[0], detail: s.counter[1] });
  checks.push({ n: 5, name: "server decision", passed: s.server[0], detail: s.server[1] });
  const f = checks.find((c) => !c.passed);
  return { approved: !f, failedCheck: f?.n ?? null, reason: f?.detail ?? "approved", checks };
}

// ------------------------------------------------------------- life cycle
/** Factory: add a provisioned card as unassigned (port of Backend.register_card). Admin only. */
export async function addInventoryCard(store: CardStore, serial: number, uidHex: string, batch: number,
  caller: Caller): Promise<[boolean, string]> {
  if (caller.role !== "admin") return [false, "admin only"];
  if (await store.getCard(serial)) return [false, `card ${serial} already exists`];
  await store.putCard({ serial, uid: uidHex.toLowerCase(), batch, status: "unassigned",
    orderId: null, parentId: null, assignedChildId: null, lastCounter: -1 });
  return [true, "added"];
}

/** Sales: assign cards to an order and create its activation code (port of Backend.create_order). Admin only. */
export async function createOrder(store: CardStore, serials: number[], caller: Caller,
  pinnedParentId: string | null = null): Promise<{ orderId: string; code: string }> {
  if (caller.role !== "admin") throw new Error("admin only");
  const cards: CardRecord[] = [];
  for (const s of serials) {
    const c = await store.getCard(s);
    if (c === null) throw new Error(`card ${s} does not exist`);
    if (c.status !== "unassigned") throw new Error(`card ${s} is ${c.status}, not unassigned`);
    cards.push(c);
  }
  const orderId = "ORD-" + randomBytes(3).toString("hex").toUpperCase();
  const code = [0, 1, 2].map(() => randomBytes(2).toString("hex").toUpperCase()).join("-");
  await store.putOrder({ orderId, codeHash: hashCode(code), parentId: pinnedParentId });
  for (const c of cards) await store.putCard({ ...c, status: "assigned", orderId });
  return { orderId, code };
}

/** Parent registers a card: serial + activation code + tap (port of Backend.register). */
export async function registerCard(store: CardStore, serial: number, code: string, parentId: string,
  uidHex: string, counter: number): Promise<[boolean, string]> {
  const uid = uidHex.toLowerCase();
  const c = await store.getCard(serial);
  if (c === null) return [false, "unknown serial"];
  if (c.uid !== uid) return [false, "tapped card does not match this serial"];
  if (c.status !== "assigned") return [false, `card is ${c.status} (must be assigned to an order)`];
  const order = c.orderId ? await store.getOrder(c.orderId) : null;
  if (order === null || order.codeHash !== hashCode(code)) return [false, "activation code does not match this card's order"];
  if (order.parentId && order.parentId !== parentId) return [false, "order belongs to another family"];
  if (!(await store.parentExists(parentId))) return [false, `family ${parentId} does not exist`];
  await store.putCard({ ...c, status: "active", parentId, lastCounter: counter });
  return [true, "registered"];
}

/** Assign an active card to one child of the same family (null = any child of the family). */
export async function assignCard(store: CardStore, serial: number, childId: string | null,
  caller: Caller): Promise<[boolean, string]> {
  const c = await store.getCard(serial);
  if (c === null) return [false, "unknown serial"];
  if (caller.role !== "parent" || c.parentId !== caller.uid) return [false, "not your card"];
  if (c.status !== "active") return [false, `card is ${c.status}`];
  await store.putCard({ ...c, assignedChildId: childId });
  return [true, childId ? `assigned to ${childId}` : "usable by any child of the family"];
}

/** Lost card: only the owning parent or an admin may revoke it. */
export async function revokeCard(store: CardStore, serial: number, caller: Caller): Promise<[boolean, string]> {
  const c = await store.getCard(serial);
  if (c === null) return [false, "unknown serial"];
  const owner = caller.role === "parent" && c.parentId !== null && c.parentId === caller.uid;
  if (!owner && caller.role !== "admin") return [false, "not your card"];
  await store.putCard({ ...c, status: "revoked" });
  return [true, "revoked"];
}

// ------------------------------------------------------------- in-memory store (tests, emulator-free runs)
export class MemoryStore implements CardStore {
  cards = new Map<number, CardRecord>();
  orders = new Map<string, OrderRecord>();
  parents = new Map<string, { subscriptionActive: boolean }>();
  taps: TapLog[] = [];
  async getCard(serial: number) { const c = this.cards.get(serial); return c ? { ...c } : null; }
  async getOrder(id: string) { return this.orders.get(id) ?? null; }
  async parentExists(id: string) { return this.parents.has(id); }
  async subscriptionActive(id: string) { return this.parents.get(id)?.subscriptionActive ?? false; }
  async putCard(card: CardRecord) { this.cards.set(card.serial, { ...card }); }
  async putOrder(order: OrderRecord) { this.orders.set(order.orderId, order); }
  async logTap(entry: TapLog) { this.taps.push(entry); }
}
