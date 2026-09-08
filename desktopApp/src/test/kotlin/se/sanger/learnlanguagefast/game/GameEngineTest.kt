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

    @Test
    fun `retrain flag logic with 3 perfect hits`() {
        val engine = GameEngine()
        
        // 1. Normal mode: Perfect word doesn't unflag if it was flagged, doesn't increment perfectCount
        val flaggedWord = Word(1, "hund", "dog", true, 0)
        engine.startRound(listOf(flaggedWord), isRetrain = false)
        engine.onKeyPress('d')
        engine.onKeyPress('o')
        engine.onKeyPress('g')
        val res1 = engine.getCompletedWordResult()
        assertTrue(res1!!.first.retrain, "Should remain flagged in normal mode even if perfect")
        assertEquals(0, res1.first.perfectCount, "perfectCount should not increment in normal mode")

        // 2. Normal mode: Mistake flags for retrain and resets perfectCount
        val cleanWord = Word(2, "katt", "cat", false, 2)
        engine.startRound(listOf(cleanWord), isRetrain = false)
        engine.onKeyPress('x') // wrong
        engine.onKeyPress('c')
        engine.onKeyPress('a')
        engine.onKeyPress('t')
        val res2 = engine.getCompletedWordResult()
        assertTrue(res2!!.first.retrain, "Mistake should flag for retrain in normal mode")
        assertEquals(0, res2.first.perfectCount, "Mistake should reset perfectCount")

        // 3. Retrain mode: Perfect word increments perfectCount but stays flagged until 3
        var word = Word(3, "bil", "car", true, 0)
        
        // Hit 1
        engine.startRound(listOf(word), isRetrain = true)
        engine.onKeyPress('c'); engine.onKeyPress('a'); engine.onKeyPress('r')
        word = engine.getCompletedWordResult()!!.first
        assertTrue(word.retrain)
        assertEquals(1, word.perfectCount)

        // Hit 2
        engine.startRound(listOf(word), isRetrain = true)
        engine.onKeyPress('c'); engine.onKeyPress('a'); engine.onKeyPress('r')
        word = engine.getCompletedWordResult()!!.first
        assertTrue(word.retrain)
        assertEquals(2, word.perfectCount)

        // Hit 3
        engine.startRound(listOf(word), isRetrain = true)
        engine.onKeyPress('c'); engine.onKeyPress('a'); engine.onKeyPress('r')
        word = engine.getCompletedWordResult()!!.first
        assertFalse(word.retrain, "Should unflag after 3 perfect hits")
        assertEquals(3, word.perfectCount)

        // 4. Retrain mode: Mistake flags/keeps flagged and resets perfectCount
        val wordWithProgress = Word(4, "hus", "house", true, 2)
        engine.startRound(listOf(wordWithProgress), isRetrain = true)
        engine.onKeyPress('x') // wrong
        engine.onKeyPress('h'); engine.onKeyPress('o'); engine.onKeyPress('u'); engine.onKeyPress('s'); engine.onKeyPress('e')
        val res4 = engine.getCompletedWordResult()
        assertTrue(res4!!.first.retrain)
        assertEquals(0, res4.first.perfectCount, "Mistake should reset perfectCount even in Retrain mode")
    }

    @Test
    fun `mistake via auto-reveal in start new mode sets retrain flag`() {
        val engine = GameEngine()
        val word = Word(1, "hund", "dog", false, 0)
        engine.startRound(listOf(word), isRetrain = false)
        
        // 'd', 'o' correct
        engine.onKeyPress('d')
        engine.onKeyPress('o')
        
        // Two mistakes on 'g' to trigger auto-reveal
        engine.onKeyPress('x')
        engine.onKeyPress('x') 
        
        val result = engine.getCompletedWordResult()
        assertNotNull(result)
        assertFalse(result.second, "Should NOT be perfect")
        assertTrue(result.first.retrain, "Should be flagged for retrain after auto-reveal mistake")
        assertEquals(0, result.first.perfectCount)
    }

    @Test
    fun `mistake then perfect in same round maintains retrain flag`() {
        val engine = GameEngine()
        val word = Word(1, "hund", "dog", false, 0)
        engine.startRound(listOf(word), isRetrain = false)
        
        // Attempt 1: Mistake
        engine.onKeyPress('x')
        engine.onKeyPress('d'); engine.onKeyPress('o'); engine.onKeyPress('g')
        
        val res1 = engine.getCompletedWordResult()
        assertTrue(res1!!.first.retrain, "Should be flagged after mistake")
        
        engine.moveToNextWord()
        assertFalse(engine.isRoundComplete, "Should re-queue word")
        
        // Attempt 2: Perfect
        engine.onKeyPress('d'); engine.onKeyPress('o'); engine.onKeyPress('g')
        val res2 = engine.getCompletedWordResult()
        assertTrue(res2!!.first.retrain, "Should STAY flagged even if second attempt is perfect in Start New mode")
    }

    @Test
    fun `polish characters are distinct`() {
        val engine = GameEngine()
        val word = makeWord("\u017caba", "\u017caba") // żaba
        engine.startRound(listOf(word))

        // Guess 'z' -> wrong
        val r1 = engine.onKeyPress('z')
        assertIs<GuessResult.Wrong>(r1)

        // Reset to avoid auto-reveal
        engine.startRound(listOf(word))

        // Guess 'ź' (\u017a) -> wrong
        val r2 = engine.onKeyPress('\u017a')
        assertIs<GuessResult.Wrong>(r2)

        // Reset
        engine.startRound(listOf(word))

        // Guess 'ż' (\u017c) -> correct
        val r3 = engine.onKeyPress('\u017c')
        assertIs<GuessResult.Correct>(r3)
        assertEquals(0, (r3 as GuessResult.Correct).slotIndex)
    }

    @Test
    fun `polish characters matching`() {
        val engine = GameEngine()
        
        engine.startRound(listOf(makeWord("\u0142\u0105ka", "\u0142\u0105ka"))) // łąka
        assertIs<GuessResult.Correct>(engine.onKeyPress('\u0142')) // ł
        
        engine.startRound(listOf(makeWord("\u017ale", "\u017ale"))) // źle
        assertIs<GuessResult.Correct>(engine.onKeyPress('\u017a')) // ź
        
        // Use a 2-letter word to avoid WordComplete on first hit
        engine.startRound(listOf(makeWord("\u0105x", "\u0105x"))) // ąx
        assertIs<GuessResult.Correct>(engine.onKeyPress('\u0105')) // ą
        
        engine.startRound(listOf(makeWord("\u0107x", "\u0107x"))) // ćx
        assertIs<GuessResult.Correct>(engine.onKeyPress('\u0107')) // ć

        engine.startRound(listOf(makeWord("\u0119x", "\u0119x"))) // ęx
        assertIs<GuessResult.Correct>(engine.onKeyPress('\u0119')) // ę

        engine.startRound(listOf(makeWord("\u0144x", "\u0144x"))) // ńx
        assertIs<GuessResult.Correct>(engine.onKeyPress('\u0144')) // ń

        engine.startRound(listOf(makeWord("\u00f3x", "\u00f3x"))) // óx
        assertIs<GuessResult.Correct>(engine.onKeyPress('\u00f3')) // ó

        engine.startRound(listOf(makeWord("\u015bx", "\u015bx"))) // śx
        assertIs<GuessResult.Correct>(engine.onKeyPress('\u015b')) // ś
    }
}
