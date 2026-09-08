package se.sanger.learnlanguagefast.ui

import se.sanger.learnlanguagefast.model.Word

sealed class Screen {
    object MainMenu : Screen()
    object AddWords : Screen()
    data class Game(val isRetrain: Boolean) : Screen()
    object Glossary : Screen()
    data class EditWord(val word: Word) : Screen()
    object RoundComplete : Screen()
}
