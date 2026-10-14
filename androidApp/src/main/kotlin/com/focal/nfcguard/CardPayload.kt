package com.focal.nfcguard

/**
 * Card payload v1 and the NDEF message that carries it. Port of payload.py.
 *
 * Body (18 bytes, big-endian): version(1) | uid(7) | serial(4) | batch(2) | issued_unix(4)
 * Signature (64 bytes):        ECDSA P-256 over SHA-256(body), raw r||s
 * Record:                      NFC Forum external type "nfcguard.io:card"
 */
class CardPayload(
    val version: Int,
    val uid: ByteArray,
    val serial: Long,
    val batch: Int,
    val issued: Long,
    val body: ByteArray,
    val signature: ByteArray,
) {
    /** The 82 bytes as stored on the card: body + signature. */
    val blob: ByteArray get() = body + signature
}

class CardFormatException(message: String) : IllegalArgumentException(message)

object CardFormat {
    const val VERSION = 1
    const val BODY_LEN = 18
    const val SIG_LEN = 64
    const val TNF_EXTERNAL = 0x04
    val CARD_TYPE = "nfcguard.io:card".toByteArray(Charsets.US_ASCII)

    fun parsePayload(blob: ByteArray): CardPayload {
        if (blob.size != BODY_LEN + SIG_LEN) {
            throw CardFormatException("payload is ${blob.size} bytes, expected ${BODY_LEN + SIG_LEN}")
        }
        val body = blob.copyOfRange(0, BODY_LEN)
        val version = u8(body, 0)
        if (version != VERSION) throw CardFormatException("unsupported payload version $version")
        return CardPayload(
            version = version,
            uid = body.copyOfRange(1, 8),
            serial = beLong(body, 8, 4),
            batch = beLong(body, 12, 2).toInt(),
            issued = beLong(body, 14, 4),
            body = body,
            signature = blob.copyOfRange(BODY_LEN, blob.size),
        )
    }

    /** Walk TLVs from page 4; return the NDEF message bytes, or null. */
    fun findNdefMessage(userMemory: ByteArray): ByteArray? {
        var i = 0
        while (i < userMemory.size) {
            val t = u8(userMemory, i)
            if (t == 0x00) { i += 1; continue }       // NULL TLV
            if (t == 0xFE) return null                  // terminator
            if (i + 1 >= userMemory.size) return null
            var length = u8(userMemory, i + 1)
            var hdr = 2
            if (length == 0xFF) {
                if (i + 3 >= userMemory.size) return null
                length = beLong(userMemory, i + 2, 2).toInt()
                hdr = 4
            }
            if (t == 0x03) {
                val start = i + hdr
                if (start + length > userMemory.size) return null
                return userMemory.copyOfRange(start, start + length)
            }
            i += hdr + length
        }
        return null
    }

    class NdefRecord(val tnf: Int, val type: ByteArray, val payload: ByteArray)

    /** Short and normal records; stops at the record flagged ME. */
    fun parseRecords(msg: ByteArray): List<NdefRecord> {
        val out = mutableListOf<NdefRecord>()
        var i = 0
        while (i < msg.size) {
            val h = u8(msg, i)
            val sr = h and 0x10 != 0
            val il = h and 0x08 != 0
            val tnf = h and 0x07
            val tlen = u8(msg, i + 1)
            val plen: Int
            var j: Int
            if (sr) { plen = u8(msg, i + 2); j = i + 3 } else { plen = beLong(msg, i + 2, 4).toInt(); j = i + 6 }
            var ilen = 0
            if (il) { ilen = u8(msg, j); j += 1 }
            val type = msg.copyOfRange(j, j + tlen)
            j += tlen + ilen
            out += NdefRecord(tnf, type, msg.copyOfRange(j, minOf(j + plen, msg.size)))
            i = j + plen
            if (h and 0x40 != 0) break                  // ME: message end
        }
        return out
    }

    fun extractCardBlob(userMemory: ByteArray): ByteArray? {
        val msg = findNdefMessage(userMemory) ?: return null
        if (msg.isEmpty()) return null
        return parseRecords(msg)
            .firstOrNull { it.tnf == TNF_EXTERNAL && it.type.contentEquals(CARD_TYPE) }
            ?.payload
    }

    /** How many bytes from page 4 hold the NDEF TLV, or null if more data is needed. */
    fun neededBytes(data: ByteArray): Int? {
        var i = 0
        while (i < data.size) {
            val t = u8(data, i)
            if (t == 0x00) { i += 1; continue }
            if (t == 0xFE) return i + 1
            if (i + 1 >= data.size) return null
            var length = u8(data, i + 1)
            var hdr = 2
            if (length == 0xFF) {
                if (i + 3 >= data.size) return null
                length = beLong(data, i + 2, 2).toInt()
                hdr = 4
            }
            if (t == 0x03) return i + hdr + length
            i += hdr + length
        }
        return null
    }

    internal fun u8(b: ByteArray, i: Int): Int = b[i].toInt() and 0xFF

    internal fun beLong(b: ByteArray, off: Int, len: Int): Long {
        var v = 0L
        for (k in 0 until len) v = (v shl 8) or u8(b, off + k).toLong()
        return v
    }
}
