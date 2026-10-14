package com.focal.nfcguard

import android.nfc.Tag
import android.nfc.tech.NfcA
import java.io.Closeable
import java.io.IOException

/**
 * NfcTransceiver on a real Android phone. The only Android-specific file:
 * everything else in com.focal.nfcguard is plain Kotlin and is tested on a PC.
 *
 * Use from AndroidNfcManager.handleTagDiscovered (runs off the UI thread; do
 * the verifyCard network call in a coroutine):
 *
 *   AndroidNfcTransceiver(tag).use { nfc ->
 *       val scan = NtagReader(nfc).scan()
 *       val local = gate.checkLocal(scan)
 *       if (!local.passed) { showDenied(local.failed); return }
 *       val result = functions.getHttpsCallable("verifyCard").call(gate.verifyCardRequest(scan)).await()
 *       if (result.data["approved"] == true) engine.toggleFocus(...) else showDenied(result)
 *   }
 */
class AndroidNfcTransceiver(private val tag: Tag) : NfcTransceiver, Closeable {
    private val nfcA: NfcA = NfcA.get(tag) ?: throw IOException("not an NFC-A tag")

    init {
        nfcA.connect()
        nfcA.timeout = 500
    }

    override val uid: ByteArray get() = tag.id

    override fun transceive(command: ByteArray): ByteArray = nfcA.transceive(command)

    override fun reselect() {
        try {
            nfcA.close()
            nfcA.connect()
        } catch (_: IOException) {
            // card gone; the next command will say so
        }
    }

    override fun close() = nfcA.close()
}
