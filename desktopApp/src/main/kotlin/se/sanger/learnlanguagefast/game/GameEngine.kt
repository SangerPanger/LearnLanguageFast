package se.sanger.learnlanguagefast.game

import se.sanger.learnlanguagefast.model.Word

enum class LetterState { HIDDEN, CORRECT, REVEALED_AFTER_ERRORS }

data class LetterSlot(
    val character: Char,
    val state: LetterState
)

data class GameWordState(
    val word: Word,
    val slots: List<LetterSlot>,
    val hadMistake: Boolean
)

sealed class GuessResult {
    data class Correct(val slotIndex: Int) : GuessResult()
    data class Wrong(val consecutiveErrors: Int) : GuessResult()
    data class AutoReveal(val slotIndex: Int) : GuessResult()
    object WordComplete : GuessResult()
}

class GameEngine {
    private val queue = ArrayDeque<Word>()
    private var currentWordState: GameWordState? = null
    private var consecutiveErrors = 0

    val currentState: GameWordState? get() = currentWordState
    val isRoundComplete: Boolean get() = currentWordState == null && queue.isEmpty()
    val hasWords: Boolean get() = currentWordState != null || queue.isNotEmpty()

    fun startRound(words: List<Word>) {
        queue.clear()
        queue.addAll(words.shuffled())
        consecutiveErrors = 0
        advanceToNextWord()
    }

    private fun advanceToNextWord() {
        if (queue.isEmpty()) {
            currentWordState = null
            return
        }
        val word = queue.removeFirst()
        currentWordState = GameWordState(
            word = word,
            slots = word.targetWord.map { LetterSlot(it, LetterState.HIDDEN) },
            hadMistake = false
        )
        consecutiveErrors = 0
    }

    fun onKeyPress(char: Char): GuessResult? {
        val state = currentWordState ?: return null
        if (state.slots.all { it.state != LetterState.HIDDEN }) return null

        val lowerChar = char.lowercaseChar()
        val matchIndex = state.slots.indexOfFirst {
            it.state == LetterState.HIDDEN && it.character.lowercaseChar() == lowerChar
        }

        if (matchIndex >= 0) {
            consecutiveErrors = 0
            val newSlots = state.slots.toMutableList()
            newSlots[matchIndex] = newSlots[matchIndex].copy(state = LetterState.CORRECT)
            currentWordState = state.copy(slots = newSlots)

            return if (newSlots.all { it.state != LetterState.HIDDEN }) {
                handleWordComplete()
                GuessResult.WordComplete
            } else {
                GuessResult.Correct(matchIndex)
            }
        } else {
            consecutiveErrors++
            val hadMistake = true
            currentWordState = state.copy(hadMistake = true)

            if (consecutiveErrors >= 2) {
                val revealIndex = state.slots.indexOfFirst { it.state == LetterState.HIDDEN }
                if (revealIndex >= 0) {
                    val newSlots = currentWordState!!.slots.toMutableList()
                    newSlots[revealIndex] = newSlots[revealIndex].copy(state = LetterState.REVEALED_AFTER_ERRORS)
                    currentWordState = currentWordState!!.copy(slots = newSlots, hadMistake = true)
                    consecutiveErrors = 0

                    return if (newSlots.all { it.state != LetterState.HIDDEN }) {
                        handleWordComplete()
                        GuessResult.WordComplete
                    } else {
                        GuessResult.AutoReveal(revealIndex)
                    }
                }
            }
            return GuessResult.Wrong(consecutiveErrors)
        }
    }

    private fun handleWordComplete() {
        val state = currentWordState ?: return
        if (state.hadMistake) {
            queue.addLast(state.word)
        }
    }

    fun getCompletedWordResult(): Pair<Word, Boolean>? {
        val state = currentWordState ?: return null
        if (state.slots.any { it.state == LetterState.HIDDEN }) return null
        return Pair(state.word, !state.hadMistake)
    }

    fun moveToNextWord() {
        advanceToNextWord()
    }
}
