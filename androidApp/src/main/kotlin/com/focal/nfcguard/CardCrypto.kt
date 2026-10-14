package com.focal.nfcguard

import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Signature checks. Port of keys.py (verification only: the app never signs).
 *
 * - Ours: ECDSA P-256 / SHA-256, raw r||s, checked with public_key.pem (check 2).
 * - NXP:  ECDSA secp128r1 over the raw 7-byte UID, no hash (check 1).
 *   Android's built-in crypto has no secp128r1, so this uses BouncyCastle
 *   (Gradle: implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")).
 */
object CardCrypto {
    /** NXP's published originality-check key for NTAG21x. */
    val NXP_NTAG21X_PUBKEY: ByteArray =
        hex("04494E1A386D3D3CFE3DC10E5DE68A499B1C202DB5B132393E89ED19FE5BE8BC61")

    /** Load our P-256 public key from the PEM text of public_key.pem. */
    fun loadPublicKeyPem(pem: String): PublicKey {
        val b64 = pem.lines().filterNot { it.startsWith("-----") }.joinToString("").trim()
        return KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(b64)))
    }

    /** Check 2: was this body signed by our private key? */
    fun verifyOurSignature(publicKey: PublicKey, body: ByteArray, rawSignature: ByteArray): Boolean = try {
        if (rawSignature.size != 64) false
        else Signature.getInstance("SHA256withECDSA").run {
            initVerify(publicKey)
            update(body)
            verify(rawToDer(rawSignature))
        }
    } catch (e: Exception) {
        false
    }

    /** Check 1: is this a valid NXP originality signature for this UID? */
    fun verifyNxpSignature(uid: ByteArray, signature: ByteArray, publicKey: ByteArray = NXP_NTAG21X_PUBKEY): Boolean = try {
        if (signature.size != 32 || signature.all { it.toInt() == 0 }) false
        else {
            val curve = SECNamedCurves.getByName("secp128r1")
            val domain = ECDomainParameters(curve.curve, curve.g, curve.n, curve.h)
            val signer = ECDSASigner()
            signer.init(false, ECPublicKeyParameters(curve.curve.decodePoint(publicKey), domain))
            signer.verifySignature(
                uid,
                BigInteger(1, signature.copyOfRange(0, 16)),
                BigInteger(1, signature.copyOfRange(16, 32)),
            )
        }
    } catch (e: Exception) {
        false
    }

    /** Raw r||s (as stored on the card) to the DER form java.security expects. */
    internal fun rawToDer(raw: ByteArray): ByteArray {
        fun derInt(x: ByteArray): ByteArray {
            var v = x.dropWhile { it.toInt() == 0 }.toByteArray()
            if (v.isEmpty()) v = byteArrayOf(0)
            if (v[0].toInt() and 0x80 != 0) v = byteArrayOf(0) + v
            return byteArrayOf(0x02, v.size.toByte()) + v
        }
        val half = raw.size / 2
        val seq = derInt(raw.copyOfRange(0, half)) + derInt(raw.copyOfRange(half, raw.size))
        return byteArrayOf(0x30, seq.size.toByte()) + seq
    }

    fun hex(s: String): ByteArray = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    fun toHex(b: ByteArray): String = b.joinToString("") { "%02X".format(it) }
}
