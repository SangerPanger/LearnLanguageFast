package se.sanger.learnlanguagefast.pronunciation

import java.io.File

object AppDirectories {
    private const val appFolderName = "LearnLanguageFast"

    val baseDirectory: File by lazy {
        val os = System.getProperty("os.name").lowercase()
        val dir = when {
            os.contains("win") -> {
                val appData = System.getenv("APPDATA")
                if (!appData.isNullOrEmpty()) File(appData, appFolderName) else File(System.getProperty("user.home"), "AppData/Roaming/$appFolderName")
            }
            os.contains("mac") -> File(System.getProperty("user.home"), "Library/Application Support/$appFolderName")
            else -> File(System.getProperty("user.home"), ".local/share/$appFolderName")
        }
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    val audioDirectory: File by lazy {
        val dir = File(baseDirectory, "audio")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    val databaseDirectory: File get() = baseDirectory
}
