package com.focal.app.pairing

/**
 * Pure Kotlin Base64 and Base64URL encoder and decoder.
 * URL-safe: uses '-' and '_' instead of '+' and '/', without padding '='.
 */
object Base64Url {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    fun encode(bytes: ByteArray): String {
        val sb = StringBuilder()
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i++].toInt() and 0xFF
            val b1 = if (i < bytes.size) bytes[i++].toInt() and 0xFF else -1
            val b2 = if (i < bytes.size) bytes[i++].toInt() and 0xFF else -1

            val d0 = b0 ushr 2
            val d1 = ((b0 and 0x03) shl 4) or (if (b1 >= 0) (b1 ushr 4) else 0)
            sb.append(ALPHABET[d0])
            sb.append(ALPHABET[d1])

            if (b1 >= 0) {
                val d2 = ((b1 and 0x0F) shl 2) or (if (b2 >= 0) (b2 ushr 6) else 0)
                sb.append(ALPHABET[d2])
                if (b2 >= 0) {
                    val d3 = b2 and 0x3F
                    sb.append(ALPHABET[d3])
                }
            }
        }
        return sb.toString()
    }

    fun encode(text: String): String = encode(text.encodeToByteArray())

    fun decode(encoded: String): ByteArray {
        val clean = encoded.trim().replace("=", "")
        val out = mutableListOf<Byte>()
        var bits = 0
        var bitCount = 0
        for (c in clean) {
            val index = ALPHABET.indexOf(c)
            val charVal = if (index >= 0) {
                index
            } else if (c == '+') {
                62
            } else if (c == '/') {
                63
            } else {
                continue
            }
            bits = (bits shl 6) or charVal
            bitCount += 6
            if (bitCount >= 8) {
                bitCount -= 8
                out.add(((bits ushr bitCount) and 0xFF).toByte())
            }
        }
        return out.toByteArray()
    }

    fun decodeToString(encoded: String): String = decode(encoded).decodeToString()
}
