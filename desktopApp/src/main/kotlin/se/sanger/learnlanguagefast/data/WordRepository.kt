package se.sanger.learnlanguagefast.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import se.sanger.learnlanguagefast.db.AppDatabase
import se.sanger.learnlanguagefast.model.Word

class WordRepository {
    private val driver = JdbcSqliteDriver("jdbc:sqlite:learnlanguagefast.db")
    private val database: AppDatabase

    init {
        try {
            AppDatabase.Schema.create(driver)
        } catch (_: Exception) {
            // Schema already exists, try to migrate if needed
            try {
                // Simple migration for perfectCount if it's missing
                driver.execute(null, "ALTER TABLE WordEntry ADD COLUMN perfectCount INTEGER NOT NULL DEFAULT 0", 0)
            } catch (_: Exception) {
                // Column probably already exists or table doesn't exist yet
            }
        }
        database = AppDatabase(driver)
    }

    private val queries get() = database.wordEntryQueries

    fun getAllWords(): List<Word> =
        queries.getAllWords().executeAsList().map {
            Word(it.id, it.sourceWord, it.targetWord, it.retrain != 0L, it.perfectCount.toInt())
        }

    fun getRetrainWords(): List<Word> =
        queries.getRetrainWords().executeAsList().map {
            Word(it.id, it.sourceWord, it.targetWord, it.retrain != 0L, it.perfectCount.toInt())
        }

    fun insertWord(sourceWord: String, targetWord: String) {
        queries.insertWord(sourceWord, targetWord, 0L, 0L)
    }

    fun updateWord(id: Long, sourceWord: String, targetWord: String) {
        queries.updateWord(sourceWord, targetWord, id)
    }

    fun updateRetrainStatus(id: Long, retrain: Boolean, perfectCount: Int) {
        queries.updateRetrainStatus(if (retrain) 1L else 0L, perfectCount.toLong(), id)
    }
}
