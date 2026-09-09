package se.sanger.learnlanguagefast.game

import se.sanger.learnlanguagefast.model.Word
import se.sanger.learnlanguagefast.model.GameSettings

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

class GameEngine(initialSettings: GameSettings = GameSettings()) {
    private val queue = ArrayDeque<Word>()
    private val repetitions = mutableMapOf<Long, Int>()
    private var currentWordState: GameWordState? = null
    private var completedWordResult: Pair<Word, Boolean>? = null
    private var consecutiveErrors = 0
    private var isRetrainMode = false
    private var settings = initialSettings.normalized()

    val currentState: GameWordState? get() = currentWordState
    val isRoundComplete: Boolean get() = currentWordState == null && queue.isEmpty()
    val hasWords: Boolean get() = currentWordState != null || queue.isNotEmpty()
    val currentRepetition: Int
        get() = currentWordState?.word?.id?.let { repetitions[it] } ?: 0
    val requiredRepetitions: Int
        get() = if (settings.repeaterEnabled) settings.repeaterCount else 1

    fun startRound(
        words: List<Word>,
        isRetrain: Boolean = false,
        gameSettings: GameSettings = settings
    ) {
        settings = gameSettings.normalized()
        queue.clear()
        queue.addAll(words.shuffled())
        repetitions.clear()
        consecutiveErrors = 0
        isRetrainMode = isRetrain
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
        completedWordResult = null
        consecutiveErrors = 0
    }

    fun onKeyPress(char: Char): GuessResult? {
        val state = currentWordState ?: return null
        if (state.slots.all { it.state != LetterState.HIDDEN }) return null

        val matchIndex = findMatchIndex(state, char)

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
            currentWordState = state.copy(hadMistake = true)

            if (settings.failsafeEnabled && consecutiveErrors >= settings.failsafeMistakes) {
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

    private fun findMatchIndex(state: GameWordState, input: Char): Int {
        if (settings.hardcoreEnabled) {
            val firstHidden = state.slots.indexOfFirst { it.state == LetterState.HIDDEN }
            return if (firstHidden >= 0 && lettersMatch(input, state.slots[firstHidden].character)) {
                firstHidden
            } else {
                -1
            }
        }
        return state.slots.indexOfFirst {
            it.state == LetterState.HIDDEN && lettersMatch(input, it.character)
        }
    }

    private fun lettersMatch(input: Char, target: Char): Boolean {
        val lowerInput = input.lowercaseChar()
        val lowerTarget = target.lowercaseChar()
        if (lowerInput == lowerTarget) return true
        if (!settings.flowEnabled) return false
        return FLOW_GROUPS[lowerInput]?.contains(lowerTarget) == true
    }

    private fun handleWordComplete() {
        val state = currentWordState ?: return
        val perfect = !state.hadMistake
        if (perfect) {
            repetitions[state.word.id] = currentRepetition + 1
        }

        val updatedWord = completedWord(state, perfect)
        completedWordResult = updatedWord to perfect
        val needsAnotherAttempt = !perfect || currentRepetition < requiredRepetitions
        if (needsAnotherAttempt) {
            if (settings.imprintEnabled) {
                queue.addFirst(updatedWord)
            } else {
                queue.addLast(updatedWord)
            }
        }
    }

    fun getCompletedWordResult(): Pair<Word, Boolean>? {
        val state = currentWordState ?: return null
        if (state.slots.any { it.state == LetterState.HIDDEN }) return null
        return completedWordResult
    }

    private fun completedWord(state: GameWordState, perfect: Boolean): Word {
        if (!perfect) return state.word.copy(retrain = true, perfectCount = 0)
        if (!isRetrainMode) return state.word

        val newPerfectCount = state.word.perfectCount + 1
        val retrainComplete = if (settings.repeaterEnabled) {
            currentRepetition >= requiredRepetitions
        } else {
            newPerfectCount >= RETRAIN_PERFECT_COUNT
        }
        return state.word.copy(
            retrain = if (retrainComplete) false else state.word.retrain,
            perfectCount = newPerfectCount
        )
    }

    fun moveToNextWord() {
        advanceToNextWord()
    }

    companion object {
        private const val RETRAIN_PERFECT_COUNT = 3

        private val FLOW_GROUPS = mapOf(
            'a' to setOf('a', 'ą'),
            'c' to setOf('c', 'ć'),
            'e' to setOf('e', 'ę'),
            'l' to setOf('l', 'ł'),
            'n' to setOf('n', 'ń'),
            'o' to setOf('o', 'ó'),
            's' to setOf('s', 'ś'),
            'z' to setOf('z', 'ź', 'ż')
        )
    }
}
