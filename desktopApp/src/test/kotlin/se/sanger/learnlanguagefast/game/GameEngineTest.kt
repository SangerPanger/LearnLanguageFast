package se.sanger.learnlanguagefast.game

import se.sanger.learnlanguagefast.model.Word
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class GameEngineTest {

    private fun makeWord(source: String, target: String, id: Long = 1) =
        Word(id, source, target, false)

    @Test
    fun `apple example from spec`() {
        val engine = GameEngine()
        val word = makeWord("äpple", "apple")
        engine.startRound(listOf(word))

        val state = engine.currentState
        assertNotNull(state)
        assertEquals(5, state.slots.size)
        assertTrue(state.slots.all { it.state == LetterState.HIDDEN })

        // Press 'p' -> reveals index 1 (first hidden 'p')
        val r1 = engine.onKeyPress('p')
        assertIs<GuessResult.Correct>(r1)
        assertEquals(1, r1.slotIndex)
        assertEquals(LetterState.CORRECT, engine.currentState!!.slots[1].state)
        assertEquals(LetterState.HIDDEN, engine.currentState!!.slots[2].state)

        // Press 'p' -> reveals index 2 (second 'p')
        val r2 = engine.onKeyPress('p')
        assertIs<GuessResult.Correct>(r2)
        assertEquals(2, r2.slotIndex)
        assertEquals(LetterState.CORRECT, engine.currentState!!.slots[2].state)

        // Press 'a' -> reveals index 0
        val r3 = engine.onKeyPress('a')
        assertIs<GuessResult.Correct>(r3)
        assertEquals(0, r3.slotIndex)

        // Press 'x' -> wrong, consecutive errors = 1
        val r4 = engine.onKeyPress('x')
        assertIs<GuessResult.Wrong>(r4)
        assertEquals(1, r4.consecutiveErrors)

        // Press 'z' -> second wrong in a row, auto-reveal index 3 ('l')
        val r5 = engine.onKeyPress('z')
        assertIs<GuessResult.AutoReveal>(r5)
        assertEquals(3, r5.slotIndex)
        assertEquals(LetterState.REVEALED_AFTER_ERRORS, engine.currentState!!.slots[3].state)

        // Press 'e' -> reveals index 4, word complete
        val r6 = engine.onKeyPress('e')
        assertIs<GuessResult.WordComplete>(r6)

        // Word had mistakes
        val result = engine.getCompletedWordResult()
        assertNotNull(result)
        assertFalse(result.second) // not perfect

        // Word should be re-queued
        engine.moveToNextWord()
        assertNotNull(engine.currentState) // word came back
        assertTrue(engine.currentState!!.slots.all { it.state == LetterState.HIDDEN })
    }

    @Test
    fun `perfect word removes from queue`() {
        val engine = GameEngine()
        val word = makeWord("äpple", "apple")
        engine.startRound(listOf(word))

        // Type a, p, p, l, e perfectly
        engine.onKeyPress('a')
        engine.onKeyPress('p')
        engine.onKeyPress('p')
        engine.onKeyPress('l')
        val r = engine.onKeyPress('e')
        assertIs<GuessResult.WordComplete>(r)

        val result = engine.getCompletedWordResult()
        assertNotNull(result)
        assertTrue(result.second) // perfect

        engine.moveToNextWord()
        assertTrue(engine.isRoundComplete)
    }

    @Test
    fun `case insensitive matching`() {
        val engine = GameEngine()
        val word = makeWord("test", "Dog")
        engine.startRound(listOf(word))

        val r = engine.onKeyPress('d') // lowercase matches 'D'
        assertIs<GuessResult.Correct>(r)
        assertEquals(0, r.slotIndex)
        // Original case preserved
        assertEquals('D', engine.currentState!!.slots[0].character)
    }
}
