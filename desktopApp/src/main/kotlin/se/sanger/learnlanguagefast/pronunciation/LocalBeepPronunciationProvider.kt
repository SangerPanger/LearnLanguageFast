package se.sanger.learnlanguagefast.pronunciation

import kotlin.math.PI
import kotlin.math.sin

/**
 * Minimal local provider that generates a short 440Hz sine-wave WAV (mono, 44.1kHz, 16-bit PCM).
 * Used only as a placeholder so the pronunciation pipeline can play something.
 */
class LocalBeepPronunciationProvider(
    private val durationMs: Int = 500,
    private val frequencyHz: Int = 440,
    private val sampleRate: Int = 44_100
) : PronunciationProvider {
    override suspend fun generate(text: String, languageCode: String): ByteArray {
        val numSamples = (durationMs / 1000.0 * sampleRate).toInt()
        val data = ByteArray(numSamples * 2) // 16-bit mono
        var idx = 0
        for (i in 0 until numSamples) {
            val t = i / sampleRate.toDouble()
            val value = (sin(2.0 * PI * frequencyHz * t) * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            data[idx++] = (value and 0xFF).toByte()
            data[idx++] = ((value ushr 8) and 0xFF).toByte()
        }

        // WAV header for PCM 16-bit mono
        val byteRate = sampleRate * 2
        val blockAlign = 2
        val subchunk2Size = data.size
        val chunkSize = 36 + subchunk2Size

        val header = ByteArray(44)
        fun putStr(pos: Int, s: String) { s.toByteArray(Charsets.US_ASCII).copyInto(header, pos) }
        fun putLE32(pos: Int, v: Int) {
            header[pos] = (v and 0xFF).toByte()
            header[pos + 1] = ((v ushr 8) and 0xFF).toByte()
            header[pos + 2] = ((v ushr 16) and 0xFF).toByte()
            header[pos + 3] = ((v ushr 24) and 0xFF).toByte()
        }
        fun putLE16(pos: Int, v: Int) {
            header[pos] = (v and 0xFF).toByte()
            header[pos + 1] = ((v ushr 8) and 0xFF).toByte()
        }

        putStr(0, "RIFF")
        putLE32(4, chunkSize)
        putStr(8, "WAVE")
        putStr(12, "fmt ")
        putLE32(16, 16) // PCM subchunk size
        putLE16(20, 1)  // PCM format
        putLE16(22, 1)  // mono
        putLE32(24, sampleRate)
        putLE32(28, byteRate)
        putLE16(32, blockAlign)
        putLE16(34, 16) // bits per sample
        putStr(36, "data")
        putLE32(40, subchunk2Size)

        val wav = ByteArray(header.size + data.size)
        header.copyInto(wav, 0)
        data.copyInto(wav, header.size)
        return wav
    }
}
