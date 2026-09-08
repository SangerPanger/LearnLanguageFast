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
            // Schema already exists
        }
        database = AppDatabase(driver)
    }

    private val queries get() = database.wordEntryQueries

    fun getAllWords(): List<Word> =
        queries.getAllWords().executeAsList().map {
            Word(it.id, it.sourceWord, it.targetWord, it.retrain != 0L)
        }

    fun getRetrainWords(): List<Word> =
        queries.getRetrainWords().executeAsList().map {
            Word(it.id, it.sourceWord, it.targetWord, it.retrain != 0L)
        }

    fun insertWord(sourceWord: String, targetWord: String) {
        queries.insertWord(sourceWord, targetWord, 0L)
    }

    fun updateWord(id: Long, sourceWord: String, targetWord: String) {
        queries.updateWord(sourceWord, targetWord, id)
    }

    fun updateRetrainStatus(id: Long, retrain: Boolean) {
        queries.updateRetrainStatus(if (retrain) 1L else 0L, id)
    }
}
