package se.sanger.learnlanguagefast.pronunciation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import se.sanger.learnlanguagefast.model.Word
import java.io.File
import java.security.MessageDigest

class PronunciationManager(
    private val audioPlayer: AudioPlayer,
    private val provider: PronunciationProvider = NoOpPronunciationProvider(),
    private val languageCode: String = DEFAULT_LANGUAGE
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    suspend fun play(word: Word) {
        // Fail-safe wrapper
        try {
            val file = ensureAudioFile(word)
            if (file != null && file.exists()) {
                audioPlayer.play(file)
            }
        } catch (_: Throwable) {
            // swallow; logging could be added here in the future
        }
    }

    fun stop() {
        audioPlayer.stop()
    }

    private suspend fun ensureAudioFile(word: Word): File? = withContext(Dispatchers.IO) {
        val target = word.targetWord
        val file = File(AppDirectories.audioDirectory, cacheFileName(word.id, target))
        if (file.exists()) return@withContext file

        // Attempt generation only once at a time for the same word
        mutex.withLock {
            if (file.exists()) return@withLock file
            val bytes = try { provider.generate(target, languageCode) } catch (_: Throwable) { null }
            if (bytes != null) {
                try {
                    file.outputStream().use { it.write(bytes) }
                    return@withLock file
                } catch (_: Throwable) {
                    return@withLock null
                }
            }
            null
        }
    }

    private fun cacheFileName(id: Long, text: String): String {
        val normalized = text.trim().lowercase()
        val hash = sha1(normalized).take(8)
        return "${'$'}id_${'$'}hash.wav"
    }

    private fun sha1(s: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(s.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        // TODO: Language should come from WordList/Settings in the future
        const val DEFAULT_LANGUAGE = "pl-PL"
    }
}
