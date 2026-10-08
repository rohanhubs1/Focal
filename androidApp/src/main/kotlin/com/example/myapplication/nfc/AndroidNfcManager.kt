package com.example.myapplication.nfc

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.focal.app.data.FocalRepository
import com.focal.app.engine.FocusEngine
import com.focal.app.model.CardStatus
import com.focal.app.model.FocusCard
import com.focal.app.model.SessionState
import com.focal.app.model.TriggerType
import java.nio.charset.Charset

object AndroidNfcManager {

    var isProgrammingMode: Boolean = false
    var pendingProgramCardId: String? = null
    var pendingProgramToken: String? = null
    var lastErrorMessage: String? = null
    var detectedPreWrittenToken: String? = null

    private var readerModeCallback: NfcAdapter.ReaderCallback? = null

    fun enableReaderMode(activity: Activity, onTagRead: (Tag) -> Unit) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        if (!adapter.isEnabled) return

        readerModeCallback = NfcAdapter.ReaderCallback { tag ->
            activity.runOnUiThread {
                onTagRead(tag)
            }
        }

        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

        adapter.enableReaderMode(activity, readerModeCallback, flags, null)
    }

    fun disableReaderMode(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        adapter.disableReaderMode(activity)
        readerModeCallback = null
    }

    fun playSuccessFeedback(context: Context) {
        try {
            // Haptic vibration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }

            // Audio tone feedback
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 150)
        } catch (_: Exception) {}
    }

    fun writeNdefToTag(tag: Tag, payloadToken: String): Result<Boolean> {
        return try {
            val payloadString = "FOCUSCARD:$payloadToken"
            val textBytes = payloadString.toByteArray(Charset.forName("UTF-8"))
            val langBytes = "en".toByteArray(Charset.forName("US-ASCII"))
            val payload = ByteArray(1 + langBytes.size + textBytes.size)
            payload[0] = 0x02.toByte() // UTF-8, 2-byte lang code
            System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
            System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)

            val textRecord = NdefRecord(
                NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_TEXT,
                ByteArray(0),
                payload
            )
            val uriRecord = NdefRecord.createUri("focal://card?token=$payloadToken")
            val message = NdefMessage(arrayOf(textRecord, uriRecord))

            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    return Result.failure(Exception("NFC Tag is read-only. Hold a writable NTAG215 card."))
                }
                if (ndef.maxSize < message.byteArrayLength) {
                    return Result.failure(Exception("Tag capacity is too small."))
                }
                ndef.writeNdefMessage(message)
                ndef.close()
                Result.success(true)
            } else {
                val formatable = NdefFormatable.get(tag)
                if (formatable != null) {
                    formatable.connect()
                    formatable.format(message)
                    formatable.close()
                    Result.success(true)
                } else {
                    Result.failure(Exception("Tag does not support NDEF formatting."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun readTokenFromTag(tag: Tag): String? {
        val ndef = Ndef.get(tag) ?: return null
        return try {
            ndef.connect()
            val message = ndef.ndefMessage
            ndef.close()
            if (message == null) return null

            for (record in message.records) {
                val payload = record.payload ?: continue
                var text = String(payload, Charset.forName("UTF-8"))
                if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                    val statusByte = payload[0].toInt()
                    val langCodeLen = statusByte and 0x3F
                    text = String(payload, 1 + langCodeLen, payload.size - 1 - langCodeLen, Charset.forName("UTF-8"))
                }

                if (text.startsWith("FOCUSCARD:")) {
                    return text.substring(10).trim()
                } else if (text.contains("FOCUSCARD:")) {
                    val idx = text.indexOf("FOCUSCARD:")
                    return text.substring(idx + 10).trim()
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun handleTagDiscovered(tag: Tag, context: Context): Boolean {
        lastErrorMessage = null
        val uidBytes = tag.id
        val uidString = uidBytes.joinToString(":") { byte -> "%02X".format(byte) }
        val repo = FocalRepository.instance
        val engine = FocusEngine.instance

        // 1. Program Card Flow (Write NDEF FOCUSCARD:<token> to NTAG215)
        if (isProgrammingMode || !pendingProgramCardId.isNullOrBlank()) {
            val cId = pendingProgramCardId ?: "FC0001"
            val token = pendingProgramToken ?: (10000000..99999999).random().toString(36).uppercase().take(8)

            val writeResult = writeNdefToTag(tag, token)
            if (writeResult.isSuccess) {
                repo.programCard(cId, uidString, token)
                playSuccessFeedback(context)
                isProgrammingMode = false
                pendingProgramCardId = null
                pendingProgramToken = null
                return true
            } else {
                lastErrorMessage = writeResult.exceptionOrNull()?.message ?: "Hold the card still and try again."
                return false
            }
        }

        // 2. Pre-written Tag Detection ("Use this card")
        val scannedToken = readTokenFromTag(tag)
        if (scannedToken != null) {
            detectedPreWrittenToken = scannedToken
        }

        // 3. Regular Focus Trigger: Verify token matches child's assigned card
        val tokenToMatch = scannedToken ?: uidString
        val child = repo.children.firstOrNull()
        val pairedCard = repo.cards.find { it.cardId == child?.pairedCardId }

        val isPaired = pairedCard != null && (
                pairedCard.token.equals(tokenToMatch, ignoreCase = true) ||
                pairedCard.nfcUid.equals(uidString, ignoreCase = true)
        )

        if (engine.sessionState == SessionState.ACTIVE) {
            if (isPaired && pairedCard != null) {
                engine.endFocus(TriggerType.NFC, pairedCard.cardId)
                playSuccessFeedback(context)
                return true
            } else {
                lastErrorMessage = "This card isn't paired to you."
                return false
            }
        } else {
            if (isPaired && pairedCard != null) {
                val defaultPolicy = repo.policies.firstOrNull() ?: return false
                val startRes = engine.startFocus(TriggerType.NFC, pairedCard.cardId, defaultPolicy)
                if (startRes.isSuccess) {
                    playSuccessFeedback(context)
                    return true
                } else {
                    lastErrorMessage = startRes.exceptionOrNull()?.message ?: "Unable to start session."
                    return false
                }
            } else {
                lastErrorMessage = "This card isn't paired to you."
                return false
            }
        }
    }
}
