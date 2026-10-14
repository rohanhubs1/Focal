/**
 * Signature checks on the server. Port of keys.py (verification only).
 *
 * - Ours: ECDSA P-256 / SHA-256, raw r||s, checked with public_key.pem (check 2).
 * - NXP:  ECDSA secp128r1 over the raw 7-byte UID, no hash (check 1). Node's
 *         crypto cannot verify an unhashed message, so this uses @noble/curves.
 */
import { createPublicKey, verify as nodeVerify, KeyObject } from "crypto";
import { ecdsa, weierstrassN } from "@noble/curves/abstract/weierstrass";
import { sha256 } from "@noble/hashes/sha2";

/** NXP's published originality-check key for NTAG21x. */
export const NXP_NTAG21X_PUBKEY = Buffer.from(
  "04494E1A386D3D3CFE3DC10E5DE68A499B1C202DB5B132393E89ED19FE5BE8BC61", "hex");

/** SEC 2 secp128r1. */
export const SECP128R1 = {
  p: 0xfffffffdffffffffffffffffffffffffn,
  n: 0xfffffffe0000000075a30d1b9038a115n,
  h: 1n,
  a: 0xfffffffdfffffffffffffffffffffffcn,
  b: 0xe87579c11079f43dd824993c2cee5ed3n,
  Gx: 0x161ff7528b899b2d0c28607ca52c5b86n,
  Gy: 0xcf5ac8395bafeb13c02da292dded7a83n,
};

const nxpEcdsa = ecdsa(weierstrassN(SECP128R1), sha256, { lowS: false });

export function loadPublicKeyPem(pem: string): KeyObject {
  return createPublicKey(pem);
}

/** Check 2: was this body signed by our private key? */
export function verifyOurSignature(publicKey: KeyObject, body: Buffer, rawSignature: Buffer): boolean {
  try {
    return rawSignature.length === 64 &&
      nodeVerify("sha256", body, { key: publicKey, dsaEncoding: "ieee-p1363" }, rawSignature);
  } catch {
    return false;
  }
}

/** Check 1: is this a valid NXP originality signature for this UID? */
export function verifyNxpSignature(uid: Buffer, signature: Buffer, publicKey: Buffer = NXP_NTAG21X_PUBKEY): boolean {
  if (signature.length !== 32 || signature.every((b) => b === 0)) return false;
  try {
    return nxpEcdsa.verify(new Uint8Array(signature), new Uint8Array(uid), new Uint8Array(publicKey),
      { prehash: false, lowS: false, format: "compact" });
  } catch {
    return false;
  }
}
