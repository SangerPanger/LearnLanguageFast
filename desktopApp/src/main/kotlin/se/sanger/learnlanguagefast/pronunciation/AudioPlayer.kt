package se.sanger.learnlanguagefast.pronunciation

import java.io.File

interface AudioPlayer {
    suspend fun play(file: File)
    fun stop()
}
