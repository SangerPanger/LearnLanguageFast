package se.sanger.learnlanguagefast.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import se.sanger.learnlanguagefast.db.AppDatabase
import se.sanger.learnlanguagefast.db.WordEntry
import se.sanger.learnlanguagefast.model.GameSettings
import se.sanger.learnlanguagefast.model.Word
import se.sanger.learnlanguagefast.model.WordList

data class ImportResult(
    val imported: Int,
    val skippedDuplicates: Int,
    val invalidRows: Int
)

class WordRepository(
    private val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:learnlanguagefast.db")
) {
    private val database: AppDatabase

    init {
        migrateSchema()
        database = AppDatabase(driver)
        migrateOrphanWordsToDefaultList()
    }

    private val words get() = database.wordEntryQueries
    private val lists get() = database.wordListQueries
    private val settings get() = database.gameSettingsQueries

    // ---------------------------------------------------------------------
    // Migration
    // ---------------------------------------------------------------------

    /**
     * Brings an existing database up to date without dropping any data.
     * - Fresh database: create the full schema.
     * - Existing database: add the missing WordList table and any missing
     *   columns on WordEntry (perfectCount, listId).
     */
    private fun migrateSchema() {
        if (!tableExists("WordEntry")) {
            AppDatabase.Schema.create(driver)
            return
        }
        if (!tableExists("WordList")) {
            driver.execute(
                null,
                "CREATE TABLE WordList (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL)",
                0
            )
        }
        if (!tableExists("GameSettings")) {
            driver.execute(
                null,
                """CREATE TABLE GameSettings (
                    id INTEGER NOT NULL PRIMARY KEY CHECK (id = 1),
                    failsafeEnabled INTEGER NOT NULL DEFAULT 1,
                    failsafeMistakes INTEGER NOT NULL DEFAULT 2,
                    hardcoreEnabled INTEGER NOT NULL DEFAULT 0,
                    repeaterEnabled INTEGER NOT NULL DEFAULT 0,
                    repeaterCount INTEGER NOT NULL DEFAULT 1,
                    flowEnabled INTEGER NOT NULL DEFAULT 0,
                    imprintEnabled INTEGER NOT NULL DEFAULT 0,
                    autoAdvanceEnabled INTEGER NOT NULL DEFAULT 1
                )""".trimIndent(),
                0
            )
        }
        if (!columnExists("GameSettings", "autoAdvanceEnabled")) {
            driver.execute(null, "ALTER TABLE GameSettings ADD COLUMN autoAdvanceEnabled INTEGER NOT NULL DEFAULT 1", 0)
        }
        if (!columnExists("WordEntry", "perfectCount")) {
            driver.execute(null, "ALTER TABLE WordEntry ADD COLUMN perfectCount INTEGER NOT NULL DEFAULT 0", 0)
        }
        if (!columnExists("WordEntry", "listId")) {
            driver.execute(null, "ALTER TABLE WordEntry ADD COLUMN listId INTEGER NOT NULL DEFAULT 0", 0)
        }
    }

    /** Words from older versions have no list: attach them to a "Default" list. */
    private fun migrateOrphanWordsToDefaultList() {
        val orphanCount = words.countOrphanWords().executeAsOne()
        if (orphanCount == 0L) return
        val defaultList = findListByTitle(DEFAULT_LIST_TITLE) ?: createListInternal(DEFAULT_LIST_TITLE)
        words.assignOrphanWordsToList(defaultList.id)
    }

    private fun tableExists(table: String): Boolean {
        val count = driver.executeQuery(
            null,
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?",
            { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getLong(0) ?: 0L)
            },
            1
        ) { bindString(0, table) }.value
        return count > 0
    }

    private fun columnExists(table: String, column: String): Boolean {
        val columns = driver.executeQuery(
            null,
            "PRAGMA table_info($table)",
            { cursor ->
                val names = mutableListOf<String>()
                while (cursor.next().value) {
                    cursor.getString(1)?.let { names.add(it) }
                }
                QueryResult.Value(names)
            },
            0
        ).value
        return columns.any { it.equals(column, ignoreCase = true) }
    }

    // ---------------------------------------------------------------------
    // Lists
    // ---------------------------------------------------------------------

    fun getAllLists(): List<WordList> =
        lists.getAllLists().executeAsList().map { WordList(it.id, it.title) }

    fun getListById(id: Long): WordList? =
        lists.getListById(id).executeAsOneOrNull()?.let { WordList(it.id, it.title) }

    fun findListByTitle(title: String): WordList? {
        val normalized = normalize(title)
        return getAllLists().firstOrNull { normalize(it.title) == normalized }
    }

    /**
     * Creates a new list. Returns the created list, or null when the title is
     * blank or a list with the same (case-insensitive, trimmed) title exists.
     */
    fun createList(title: String): WordList? {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return null
        if (findListByTitle(trimmed) != null) return null
        return createListInternal(trimmed)
    }

    private fun createListInternal(title: String): WordList {
        return database.transactionWithResult {
            lists.insertList(title)
            val id = lists.lastInsertRowId().executeAsOne()
            WordList(id, title)
        }
    }

    fun updateListTitle(id: Long, title: String): Boolean {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return false
        val existing = findListByTitle(trimmed)
        if (existing != null && existing.id != id) return false
        lists.updateListTitle(trimmed, id)
        return true
    }

    fun deleteList(id: Long) {
        database.transaction {
            words.deleteWordsForList(id)
            lists.deleteList(id)
        }
    }

    // ---------------------------------------------------------------------
    // Game settings
    // ---------------------------------------------------------------------

    fun getGameSettings(): GameSettings =
        settings.getGameSettings().executeAsOneOrNull()?.let {
            GameSettings(
                failsafeEnabled = it.failsafeEnabled != 0L,
                failsafeMistakes = it.failsafeMistakes.toInt(),
                hardcoreEnabled = it.hardcoreEnabled != 0L,
                repeaterEnabled = it.repeaterEnabled != 0L,
                repeaterCount = it.repeaterCount.toInt(),
                flowEnabled = it.flowEnabled != 0L,
                imprintEnabled = it.imprintEnabled != 0L,
                autoAdvanceEnabled = (it.autoAdvanceEnabled != 0L)
            ).normalized()
        } ?: GameSettings()

    fun saveGameSettings(gameSettings: GameSettings) {
        val normalized = gameSettings.normalized()
        settings.saveGameSettings(
            if (normalized.failsafeEnabled) 1L else 0L,
            normalized.failsafeMistakes.toLong(),
            if (normalized.hardcoreEnabled) 1L else 0L,
            if (normalized.repeaterEnabled) 1L else 0L,
            normalized.repeaterCount.toLong(),
            if (normalized.flowEnabled) 1L else 0L,
            if (normalized.imprintEnabled) 1L else 0L,
            if (normalized.autoAdvanceEnabled) 1L else 0L
        )
    }

    // ---------------------------------------------------------------------
    // Words
    // ---------------------------------------------------------------------

    fun getAllWords(): List<Word> =
        words.getAllWords().executeAsList().map { it.toWord() }

    fun getWordsForList(listId: Long): List<Word> =
        words.getWordsForList(listId).executeAsList().map { it.toWord() }

    fun getWordsForLists(listIds: Collection<Long>): List<Word> {
        if (listIds.isEmpty()) return emptyList()
        return words.getWordsForLists(listIds).executeAsList().map { it.toWord() }
    }

    fun getRetrainWords(): List<Word> =
        words.getRetrainWords().executeAsList().map { it.toWord() }

    fun getRetrainWordsForLists(listIds: Collection<Long>): List<Word> {
        if (listIds.isEmpty()) return emptyList()
        return words.getRetrainWordsForLists(listIds).executeAsList().map { it.toWord() }
    }

    fun insertWord(listId: Long, sourceWord: String, targetWord: String, retrain: Boolean = false, perfectCount: Int = 0) {
        words.insertWord(listId, sourceWord.trim(), targetWord.trim(), if (retrain) 1L else 0L, perfectCount.toLong())
    }

    fun updateWord(id: Long, sourceWord: String, targetWord: String) {
        words.updateWord(sourceWord.trim(), targetWord.trim(), id)
    }

    fun updateRetrainStatus(id: Long, retrain: Boolean, perfectCount: Int) {
        words.updateRetrainStatus(if (retrain) 1L else 0L, perfectCount.toLong(), id)
    }

    /** Case-insensitive, whitespace-trimmed duplicate check within one list. */
    fun isDuplicateInList(listId: Long, sourceWord: String, targetWord: String): Boolean {
        val key = duplicateKey(sourceWord, targetWord)
        return getWordsForList(listId).any { duplicateKey(it.sourceWord, it.targetWord) == key }
    }

    // ---------------------------------------------------------------------
    // CSV import / export
    // ---------------------------------------------------------------------

    /**
     * Imports CSV text into [listId].
     * Accepted formats (header optional):
     *   sourceWord,targetWord
     *   sourceWord,targetWord,retrain
     *   sourceWord,targetWord,retrain,perfectCount
     * Duplicates (same source+target, case-insensitive & trimmed) against both
     * the existing list and earlier rows in the same file are skipped.
     */
    fun importCsv(listId: Long, csvText: String): ImportResult {
        val rows = Csv.parse(csvText)
        var imported = 0
        var skipped = 0
        var invalid = 0

        val seen = getWordsForList(listId)
            .map { duplicateKey(it.sourceWord, it.targetWord) }
            .toMutableSet()

        database.transaction {
            rows.forEachIndexed { index, row ->
                if (index == 0 && isHeaderRow(row)) return@forEachIndexed

                val source = row.getOrNull(0)?.trim().orEmpty()
                val target = row.getOrNull(1)?.trim().orEmpty()
                if (source.isEmpty() || target.isEmpty()) {
                    invalid++
                    return@forEachIndexed
                }

                val key = duplicateKey(source, target)
                if (!seen.add(key)) {
                    skipped++
                    return@forEachIndexed
                }

                val retrain = parseBoolean(row.getOrNull(2))
                val perfectCount = row.getOrNull(3)?.trim()?.toIntOrNull() ?: 0
                words.insertWord(listId, source, target, if (retrain) 1L else 0L, perfectCount.toLong())
                imported++
            }
        }
        return ImportResult(imported, skipped, invalid)
    }

    /** Exports all words in [listId] as CSV: sourceWord,targetWord,retrain,perfectCount */
    fun exportCsv(listId: Long): String {
        val sb = StringBuilder()
        sb.append(Csv.formatRow(listOf("sourceWord", "targetWord", "retrain", "perfectCount"))).append("\n")
        getWordsForList(listId).forEach { w ->
            sb.append(
                Csv.formatRow(listOf(w.sourceWord, w.targetWord, w.retrain.toString(), w.perfectCount.toString()))
            ).append("\n")
        }
        return sb.toString()
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private fun WordEntry.toWord() =
        Word(id, sourceWord, targetWord, retrain != 0L, perfectCount.toInt(), listId)

    private fun normalize(value: String) = value.trim().lowercase()

    private fun duplicateKey(source: String, target: String) =
        normalize(source) + "\u0000" + normalize(target)

    private fun isHeaderRow(row: List<String>): Boolean {
        val first = row.getOrNull(0)?.trim()?.lowercase() ?: return false
        val second = row.getOrNull(1)?.trim()?.lowercase() ?: return false
        return first == "sourceword" && second == "targetword"
    }

    private fun parseBoolean(value: String?): Boolean {
        val v = value?.trim()?.lowercase() ?: return false
        return v == "true" || v == "1" || v == "yes" || v == "ja"
    }

    companion object {
        const val DEFAULT_LIST_TITLE = "Default"
    }
}
