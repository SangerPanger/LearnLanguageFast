package se.sanger.learnlanguagefast.pronunciation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

/**
 * Simple WAV player using Java Sound. Policy: stop previous playback and play the newest.
 */
class DesktopAudioPlayer : AudioPlayer {
    @Volatile
    private var clip: Clip? = null

    override fun stop() {
        try {
            clip?.let { c ->
                if (c.isRunning) c.stop()
                c.flush()
                c.close()
            }
        } catch (_: Throwable) {
        } finally {
            clip = null
        }
    }

    override suspend fun play(file: File) {
        // Only WAV is supported in this first version
        withContext(Dispatchers.IO) {
            stop()
            var stream: AudioInputStream? = null
            try {
                stream = AudioSystem.getAudioInputStream(file)
                val newClip = AudioSystem.getClip()
                newClip.open(stream)
                clip = newClip
                newClip.start()
            } finally {
                // Do not close stream here if the clip uses it; close when stopping/closing the clip
            }
        }
    }
}
