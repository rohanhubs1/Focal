/**
 * Card payload v1 and NDEF parsing. Port of payload.py.
 *
 * Body (18 bytes, big-endian): version(1) | uid(7) | serial(4) | batch(2) | issued_unix(4)
 * Signature (64 bytes):        ECDSA P-256 over SHA-256(body), raw r||s
 */
export const VERSION = 1;
export const BODY_LEN = 18;
export const SIG_LEN = 64;
const TNF_EXTERNAL = 0x04;
const CARD_TYPE = Buffer.from("nfcguard.io:card", "ascii");

export interface CardPayload {
  version: number;
  uid: Buffer;
  serial: number;
  batch: number;
  issued: number;
  body: Buffer;
  signature: Buffer;
}

export function parsePayload(blob: Buffer): CardPayload {
  if (blob.length !== BODY_LEN + SIG_LEN) {
    throw new Error(`payload is ${blob.length} bytes, expected ${BODY_LEN + SIG_LEN}`);
  }
  const body = blob.subarray(0, BODY_LEN);
  const version = body[0];
  if (version !== VERSION) throw new Error(`unsupported payload version ${version}`);
  return {
    version,
    uid: Buffer.from(body.subarray(1, 8)),
    serial: body.readUInt32BE(8),
    batch: body.readUInt16BE(12),
    issued: body.readUInt32BE(14),
    body: Buffer.from(body),
    signature: Buffer.from(blob.subarray(BODY_LEN)),
  };
}

/** Walk TLVs from page 4; return the NDEF message bytes, or null. */
export function findNdefMessage(mem: Buffer): Buffer | null {
  let i = 0;
  while (i < mem.length) {
    const t = mem[i];
    if (t === 0x00) { i += 1; continue; }
    if (t === 0xfe) return null;
    if (i + 1 >= mem.length) return null;
    let length = mem[i + 1];
    let hdr = 2;
    if (length === 0xff) {
      if (i + 3 >= mem.length) return null;
      length = mem.readUInt16BE(i + 2);
      hdr = 4;
    }
    if (t === 0x03) {
      const msg = mem.subarray(i + hdr, i + hdr + length);
      return msg.length === length ? msg : null;
    }
    i += hdr + length;
  }
  return null;
}

/** The nfcguard record's payload from a page-4 memory dump, or null. */
export function extractCardBlob(mem: Buffer): Buffer | null {
  const msg = findNdefMessage(mem);
  if (!msg || msg.length === 0) return null;
  let i = 0;
  while (i < msg.length) {
    const h = msg[i];
    const tlen = msg[i + 1];
    let plen: number;
    let j: number;
    if (h & 0x10) { plen = msg[i + 2]; j = i + 3; } else { plen = msg.readUInt32BE(i + 2); j = i + 6; }
    let ilen = 0;
    if (h & 0x08) { ilen = msg[j]; j += 1; }
    const type = msg.subarray(j, j + tlen);
    j += tlen + ilen;
    if ((h & 0x07) === TNF_EXTERNAL && type.equals(CARD_TYPE)) return Buffer.from(msg.subarray(j, j + plen));
    i = j + plen;
    if (h & 0x40) break;
  }
  return null;
}
