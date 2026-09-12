package se.sanger.learnlanguagefast.pronunciation

import java.io.File
import java.util.Properties

data class PronunciationSettings(
    val enabled: Boolean = true,
    val autoPlay: Boolean = true
)

class PronunciationSettingsStore {
    private val file: File = File(AppDirectories.baseDirectory, "pronunciation.properties")

    fun load(): PronunciationSettings {
        if (!file.exists()) return PronunciationSettings()
        return try {
            val props = Properties()
            file.inputStream().use { props.load(it) }
            PronunciationSettings(
                enabled = props.getProperty("enabled", "true").toBooleanStrictOrNull() ?: true,
                autoPlay = props.getProperty("autoPlay", "true").toBooleanStrictOrNull() ?: true
            )
        } catch (_: Throwable) {
            PronunciationSettings()
        }
    }

    fun save(settings: PronunciationSettings) {
        try {
            val props = Properties()
            props.setProperty("enabled", settings.enabled.toString())
            props.setProperty("autoPlay", settings.autoPlay.toString())
            file.outputStream().use { props.store(it, "Pronunciation settings") }
        } catch (_: Throwable) {
            // ignore
        }
    }
}
