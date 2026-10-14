package com.focal.nfcguard

import java.security.PublicKey
import java.util.Base64

/** One line of the gate's report. */
data class Check(val number: Int, val name: String, val passed: Boolean, val detail: String)

/** Result of checks 1-3 on the phone. */
data class LocalResult(val passed: Boolean, val checks: List<Check>) {
    val failed: Check? get() = checks.firstOrNull { !it.passed }
}

/**
 * Checks 1-3, run on the child's phone. Port of gate.authorize (local part).
 *
 *   1. Genuine NXP chip  - NXP originality signature over the UID
 *   2. Our signature     - ECDSA P-256 over the payload, with the app's public key
 *   3. UID binding       - UID inside the signed payload == chip's hardware UID
 *
 * Checks 4 (tap counter) and 5 (family, card status, subscription) need the
 * server: send verifyCardRequest(scan) to the verifyCard Cloud Function. The
 * server runs 1-3 again itself, so a modified app cannot skip them.
 */
class NfcGuardGate(
    private val ourPublicKey: PublicKey,
    private val nxpPublicKey: ByteArray = CardCrypto.NXP_NTAG21X_PUBKEY,
    private val requireGenuine: Boolean = true,
) {
    fun checkLocal(scan: CardScan): LocalResult {
        val checks = mutableListOf<Check>()

        if (CardCrypto.verifyNxpSignature(scan.uid, scan.nxpSignature, nxpPublicKey)) {
            checks += Check(1, "genuine NXP chip", true, "originality signature valid")
        } else if (requireGenuine) {
            checks += Check(1, "genuine NXP chip", false, "originality signature invalid (clone chip?)")
            return LocalResult(false, checks)
        } else {
            checks += Check(1, "genuine NXP chip", true, "SKIPPED (test mode); signature invalid")
        }

        val p = scan.payload
        if (p == null) {
            checks += Check(2, "our signature", false, scan.payloadError ?: "no payload")
            return LocalResult(false, checks)
        }
        if (!CardCrypto.verifyOurSignature(ourPublicKey, p.body, p.signature)) {
            checks += Check(2, "our signature", false, "payload not signed by our key (self-written/tampered)")
            return LocalResult(false, checks)
        }
        checks += Check(2, "our signature", true, "serial ${p.serial}, batch ${p.batch}")

        if (!p.uid.contentEquals(scan.uid)) {
            checks += Check(3, "UID binding", false,
                "payload UID ${CardCrypto.toHex(p.uid)} != chip UID ${CardCrypto.toHex(scan.uid)} (copied payload)")
            return LocalResult(false, checks)
        }
        checks += Check(3, "UID binding", true, CardCrypto.toHex(scan.uid))
        return LocalResult(true, checks)
    }

    /** Body of the verifyCard Cloud Function call (Firebase callable data). */
    fun verifyCardRequest(scan: CardScan): Map<String, Any> = mapOf(
        "uid" to CardCrypto.toHex(scan.uid),
        "payload" to Base64.getEncoder().encodeToString(scan.payload?.blob ?: ByteArray(0)),
        "nxpSignature" to Base64.getEncoder().encodeToString(scan.nxpSignature),
        "counter" to scan.counter,
    )
}
