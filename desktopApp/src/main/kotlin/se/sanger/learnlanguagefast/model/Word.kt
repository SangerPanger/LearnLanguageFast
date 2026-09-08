package se.sanger.learnlanguagefast.model

data class Word(
    val id: Long,
    val sourceWord: String,
    val targetWord: String,
    val retrain: Boolean,
    val perfectCount: Int = 0,
    val listId: Long = 0
)
