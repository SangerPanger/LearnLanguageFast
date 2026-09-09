package se.sanger.learnlanguagefast.ui

import se.sanger.learnlanguagefast.model.Word
import se.sanger.learnlanguagefast.model.WordList

sealed class Screen {
    object MainMenu : Screen()
    object Options : Screen()

    // Add words flow
    object AddWordsChoice : Screen()
    object CreateList : Screen()
    object AddWordsListSelection : Screen()
    data class AddWords(val list: WordList) : Screen()

    // Glossary flow
    object GlossaryListSelection : Screen()
    data class Glossary(val list: WordList) : Screen()
    data class EditWord(val word: Word, val list: WordList) : Screen()

    // Game flow
    object StartNewListSelection : Screen()
    object RetrainListSelection : Screen()
    data class Game(val isRetrain: Boolean) : Screen()
    object RoundComplete : Screen()
}
