package com.focal.nfcguard

import java.io.IOException

/**
 * The only thing the reader needs from the phone. On Android this is a thin
 * wrapper around android.nfc.tech.NfcA (see android/AndroidNfcTransceiver.kt);
 * in tests it is a simulated card.
 */
interface NfcTransceiver {
    val uid: ByteArray

    /** Send a raw NTAG command; the CRC is added and checked by the phone. */
    fun transceive(command: ByteArray): ByteArray

    /** After a NAK the tag drops back to IDLE; select it again. */
    fun reselect() {}
}

class NtagException(message: String) : IOException(message)

/** Everything the gate needs, read in one tap. Port of gate.Scan. */
class CardScan(
    val uid: ByteArray,
    val model: String,
    val nxpSignature: ByteArray,
    val counter: Int,
    val payload: CardPayload?,
    val payloadError: String?,
)

/** NTAG213/215/216 commands. Port of ntag.py (read side) and gate.scan_card. */
class NtagReader(private val t: NfcTransceiver) {
    companion object {
        const val GET_VERSION = 0x60
        const val READ = 0x30
        const val READ_SIG = 0x3C
        const val READ_CNT = 0x39

        /** GET_VERSION byte 6 (storage size) -> model. */
        val MODELS = mapOf(0x0F to "NTAG213", 0x11 to "NTAG215", 0x13 to "NTAG216")
        val LAST_USER_PAGE = mapOf("NTAG213" to 0x27, "NTAG215" to 0x81, "NTAG216" to 0xE1)
    }

    val uid: ByteArray get() = t.uid

    private fun command(vararg bytes: Int): ByteArray {
        val rsp = t.transceive(ByteArray(bytes.size) { bytes[it].toByte() })
        if (rsp.size <= 1) throw NtagException("NAK to command 0x%02X".format(bytes[0]))
        return rsp
    }

    fun model(): String {
        val rsp = command(GET_VERSION)
        if (rsp.size < 8) throw NtagException("GET_VERSION gave ${rsp.size} bytes; not an NTAG21x?")
        val size = rsp[6].toInt() and 0xFF
        return MODELS[size] ?: throw NtagException("unknown storage size 0x%02X; not NTAG213/215/216".format(size))
    }

    /** READ returns 4 pages (16 bytes). */
    fun read4(page: Int): ByteArray {
        val rsp = command(READ, page)
        if (rsp.size < 16) throw NtagException("READ page $page returned ${rsp.size} bytes")
        return rsp.copyOfRange(0, 16)
    }

    fun readSignature(): ByteArray = command(READ_SIG, 0x00).copyOfRange(0, 32)

    fun readCounter(): Int {
        val r = command(READ_CNT, 0x02)
        return (r[0].toInt() and 0xFF) or ((r[1].toInt() and 0xFF) shl 8) or ((r[2].toInt() and 0xFF) shl 16)
    }

    /** Read from page 4 just far enough to cover the NDEF TLV (fewer reads = faster tap). */
    fun readUserMemory(model: String): ByteArray {
        val last = LAST_USER_PAGE.getValue(model)
        var out = ByteArray(0)
        var page = 4
        while (page <= last) {
            out += read4(page)
            page += 4
            val need = CardFormat.neededBytes(out)
            if (need != null && out.size >= need) break
        }
        return out.copyOfRange(0, minOf(out.size, (last - 3) * 4))
    }

    /**
     * Read memory first (the chip bumps its counter on the first READ of a tap),
     * then the NXP signature and the counter. A card that refuses the signature
     * or counter (blank or clone) is still scanned; the checks then deny it.
     */
    fun scan(): CardScan {
        val uid = t.uid
        val model = model()
        var payload: CardPayload? = null
        var error: String? = null
        try {
            val blob = CardFormat.extractCardBlob(readUserMemory(model))
            if (blob == null) error = "no nfcguard record on this card"
            else payload = CardFormat.parsePayload(blob)
        } catch (e: IOException) {
            throw e  // card left the field: the tap failed, not the card
        } catch (e: Exception) {
            error = "unreadable payload: ${e.message}"
        }
        val sig = try { readSignature() } catch (e: IOException) { t.reselect(); ByteArray(0) }
        val counter = try { readCounter() } catch (e: IOException) { t.reselect(); -1 }
        return CardScan(uid, model, sig, counter, payload, error)
    }
}
