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
            println("[Pronunciation] play() called for word id=${word.id} text='${word.targetWord}' provider=${provider.javaClass.simpleName}")
            val file = ensureAudioFile(word)
            println("[Pronunciation] audio cache dir: ${AppDirectories.audioDirectory.absolutePath}")
            if (file != null) {
                val exists = file.exists()
                val size = if (exists) file.length() else -1
                println("[Pronunciation] cache candidate: ${file.absolutePath} exists=${exists} size=${size}")
                if (exists && size > 0L) println("[Pronunciation] Cache hit for '${word.targetWord}' -> ${file.name}")
            }
            if (file != null && file.exists() && file.length() > 0L) {
                println("[Pronunciation] invoking AudioPlayer.play()")
                audioPlayer.play(file)
            } else {
                println("[Pronunciation][WARN] No audio file available to play")
            }
        } catch (t: Throwable) {
            println("[Pronunciation][ERROR] play() failed: ${t.message}")
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
            val bytes = try {
                println("[Pronunciation] Cache miss -> generating TTS. text='${target}', lang=${languageCode}")
                provider.generate(target, languageCode)
            } catch (t: Throwable) {
                println("[Pronunciation][ERROR] provider.generate failed: ${t.message}")
                null
            }
            if (bytes != null) {
                try {
                    file.outputStream().use { it.write(bytes) }
                    println("[Pronunciation] Generated file path: ${file.absolutePath}")
                    println("[Pronunciation] Generated file size: ${file.length()}")
                    return@withLock file
                } catch (t: Throwable) {
                    println("[Pronunciation][ERROR] failed writing audio file: ${t.message}")
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
