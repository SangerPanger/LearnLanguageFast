package se.sanger.learnlanguagefast.data

/**
 * Minimal RFC 4180-style CSV parser/writer.
 * Supports quoted fields, escaped quotes (""), commas/newlines inside quotes,
 * blank lines, a UTF-8 BOM and automatic detection of ';' as delimiter
 * (Excel in Swedish/Polish locales exports semicolon separated files).
 */
object Csv {

    fun parse(text: String): List<List<String>> {
        val content = text.removePrefix("\uFEFF")
        val delimiter = detectDelimiter(content)
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0

        fun endField() {
            row.add(field.toString())
            field.setLength(0)
        }

        fun endRow() {
            endField()
            if (row.any { it.isNotBlank() }) {
                rows.add(row.toList())
            }
            row.clear()
        }

        while (i < content.length) {
            val c = content[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < content.length && content[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    delimiter -> endField()
                    '\r' -> {
                        if (i + 1 < content.length && content[i + 1] == '\n') i++
                        endRow()
                    }
                    '\n' -> endRow()
                    else -> field.append(c)
                }
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            endRow()
        }
        return rows
    }

    fun formatRow(fields: List<String>): String =
        fields.joinToString(",") { escape(it) }

    private fun escape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' || it == ';' }
                || value.startsWith(" ") || value.endsWith(" ")
        if (!needsQuotes) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    private fun detectDelimiter(content: String): Char {
        val firstLine = content.lineSequence().firstOrNull { it.isNotBlank() } ?: return ','
        val commas = firstLine.count { it == ',' }
        val semicolons = firstLine.count { it == ';' }
        return if (semicolons > commas) ';' else ','
    }
}
