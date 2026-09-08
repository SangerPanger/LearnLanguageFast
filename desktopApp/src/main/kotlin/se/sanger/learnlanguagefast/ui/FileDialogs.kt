package se.sanger.learnlanguagefast.ui

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/** Desktop file dialogs for CSV import/export (Swing based, works with Compose Desktop). */
object FileDialogs {

    fun chooseCsvToOpen(): File? {
        val chooser = JFileChooser().apply {
            dialogTitle = "Import CSV"
            fileFilter = FileNameExtensionFilter("CSV files (*.csv, *.txt)", "csv", "txt")
            isAcceptAllFileFilterUsed = true
        }
        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    }

    fun chooseCsvToSave(suggestedName: String): File? {
        val chooser = JFileChooser().apply {
            dialogTitle = "Export CSV"
            fileFilter = FileNameExtensionFilter("CSV files (*.csv)", "csv")
            selectedFile = File(suggestedName)
        }
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return null
        val file = chooser.selectedFile ?: return null
        return if (file.extension.equals("csv", ignoreCase = true)) file else File(file.path + ".csv")
    }

    /** Turns a list title into a safe file name, e.g. "Polska A1" -> "Polska_A1.csv". */
    fun suggestedFileName(title: String): String {
        val sanitized = title.trim()
            .replace(Regex("""[\\/:*?"<>|]"""), "")
            .replace(Regex("""\s+"""), "_")
            .ifEmpty { "words" }
        return "$sanitized.csv"
    }
}
