package se.sanger.learnlanguagefast.pronunciation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * Windows-only pronunciation provider that uses PowerShell + System.Speech to synthesize
 * a WAV file for the target text. Returns the WAV bytes, leaving caching to PronunciationManager.
 */
class WindowsTtsPronunciationProvider(
    private val defaultLanguage: String = PronunciationManager.DEFAULT_LANGUAGE,
    private val processTimeoutSeconds: Long = 20
) : PronunciationProvider {

    override suspend fun generate(text: String, languageCode: String): ByteArray? = withContext(Dispatchers.IO) {
        val lang = languageCode.ifBlank { defaultLanguage }
        val os = System.getProperty("os.name").lowercase()
        if (!os.contains("windows")) {
            println("[Pronunciation][WindowsTTS][WARN] Non-Windows OS '$os' - provider unavailable")
            return@withContext null
        }

        val tmpFile = Files.createTempFile("llf_tts_", ".wav").toFile()
        // Ensure cleanup on failure; on success PronunciationManager will save to cache from bytes
        try {
            val textPs = psSingleQuoted(text)
            val pathPs = psSingleQuoted(tmpFile.absolutePath)
            val langPs = psSingleQuoted(lang)

            val psScript = buildString {
                append("\$ErrorActionPreference = 'Stop'; ")
                append("Add-Type -AssemblyName System.Speech; ")
                append("\$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; ")
                // Find voices matching exact culture code (e.g., pl-PL)
                append("\$voices = \$s.GetInstalledVoices() | Where-Object { \$_.Enabled -and \$_.VoiceInfo.Culture.Name -eq $langPs }; ")
                append("if (-not \$voices -or \$voices.Count -eq 0) { Write-Output \"[WindowsTTS][WARN] No voice for $langPs installed\"; exit 3 }; ")
                append("\$v = \$voices[0]; ")
                append("\$s.SelectVoice(\$v.VoiceInfo.Name); ")
                append("Write-Output (\"[WindowsTTS] Selected voice: \" + \$v.VoiceInfo.Name + \" (\" + \$v.VoiceInfo.Culture.Name + \")\"); ")
                append("\$s.SetOutputToWaveFile($pathPs); ")
                append("\$s.Speak($textPs); ")
                append("\$s.Dispose(); ")
                append("Write-Output \"[WindowsTTS] Synthesis complete -> \" + $pathPs; ")
            }

            val cmd = listOf(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-ExecutionPolicy", "Bypass",
                "-Command", psScript
            )

            println("[Pronunciation] TTS provider: WindowsTtsPronunciationProvider")
            println("[Pronunciation] Requested text='${text}' language='$lang'")

            val pb = ProcessBuilder(cmd)
            pb.redirectErrorStream(true)
            val process = pb.start()
            val output = process.inputStream.bufferedReader(Charsets.UTF_8).readText()
            val finished = process.waitFor(processTimeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                println("[Pronunciation][WindowsTTS][ERROR] PowerShell timed out after ${processTimeoutSeconds}s. Output:\n$output")
                return@withContext null
            }
            val exit = process.exitValue()
            if (output.isNotBlank()) println("[Pronunciation][WindowsTTS] PS output:\n$output")
            if (exit != 0) {
                println("[Pronunciation][WindowsTTS][ERROR] PowerShell exited with code $exit")
                return@withContext null
            }

            if (!tmpFile.exists() || tmpFile.length() <= 0L) {
                println("[Pronunciation][WindowsTTS][ERROR] Expected WAV not created: ${tmpFile.absolutePath}")
                return@withContext null
            }

            val bytes = tmpFile.readBytes()
            println("[Pronunciation][WindowsTTS] Generated file: ${tmpFile.absolutePath} size=${bytes.size}")
            return@withContext bytes
        } catch (t: Throwable) {
            println("[Pronunciation][WindowsTTS][ERROR] ${t.message}")
            return@withContext null
        } finally {
            try { tmpFile.delete() } catch (_: Throwable) {}
        }
    }

    private fun psSingleQuoted(s: String): String {
        // Escape single quotes for PowerShell single-quoted literals by doubling them
        val escaped = s.replace("'", "''")
        return "'$escaped'"
    }
}
