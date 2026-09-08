package se.sanger.learnlanguagefast.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import se.sanger.learnlanguagefast.model.Word
import se.sanger.learnlanguagefast.game.*

data class SelectionState(
    val baseChar: Char,
    val alternatives: List<Char>,
    val selectedIndex: Int
) {
    val selectedChar: Char get() = alternatives[selectedIndex]
}

private val polishGroups = mapOf(
    'a' to listOf('a', 'ą'),
    'c' to listOf('c', 'ć'),
    'e' to listOf('e', 'ę'),
    'l' to listOf('l', 'ł'),
    'n' to listOf('n', 'ń'),
    'o' to listOf('o', 'ó'),
    's' to listOf('s', 'ś'),
    'z' to listOf('z', 'ź', 'ż')
)

@Composable
fun GameScreen(
    engine: GameEngine,
    onWordComplete: (Word, Boolean) -> Unit,
    onRoundComplete: () -> Unit,
    onBack: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var wordState by remember { mutableStateOf(engine.currentState) }
    var flashRed by remember { mutableStateOf(false) }
    var showWordComplete by remember { mutableStateOf(false) }
    var selectionState by remember { mutableStateOf<SelectionState?>(null) }

    val bgColor by animateColorAsState(
        targetValue = if (flashRed) Color.Red.copy(alpha = 0.3f) else Color.Transparent
    )

    LaunchedEffect(flashRed) {
        if (flashRed) {
            delay(300)
            flashRed = false
        }
    }

    fun submitGuess(char: Char) {
        val result = engine.onKeyPress(char)
        wordState = engine.currentState

        when (result) {
            is GuessResult.Wrong -> {
                flashRed = true
            }
            is GuessResult.AutoReveal -> {
                flashRed = true
            }
            is GuessResult.WordComplete -> {
                showWordComplete = true
                selectionState = null
            }
            is GuessResult.Correct -> {}
            null -> {}
        }
    }

    fun handleKey(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        
        if (showWordComplete) {
            if (event.key == Key.Enter) {
                val result = engine.getCompletedWordResult()
                if (result != null) {
                    val (word, perfect) = result
                    onWordComplete(word, perfect)
                    engine.moveToNextWord()
                    wordState = engine.currentState
                    showWordComplete = false
                    selectionState = null
                    if (engine.isRoundComplete) {
                        onRoundComplete()
                    }
                    return true
                }
            }
            return false
        }

        if (event.key == Key.Enter) {
            val ss = selectionState
            if (ss != null) {
                submitGuess(ss.selectedChar)
                selectionState = null
                return true
            }
            return false
        }

        // Allow Spacebar as a valid guess for ' ' (space) to support phrases
        if (event.key == Key.Spacebar) {
            // Confirm any pending selection first
            val ss = selectionState
            if (ss != null) {
                submitGuess(ss.selectedChar)
                selectionState = null
            }
            submitGuess(' ')
            return true
        }

        val char = event.utf16CodePoint.toChar().lowercaseChar()
        if (!char.isLetter()) return false

        val group = polishGroups[char]
        if (group != null) {
            val ss = selectionState
            if (ss?.baseChar == char) {
                // Cycle
                selectionState = ss.copy(
                    selectedIndex = (ss.selectedIndex + 1) % ss.alternatives.size
                )
            } else {
                // Confirm previous if any
                if (ss != null) {
                    submitGuess(ss.selectedChar)
                }
                // Start new selection
                selectionState = SelectionState(char, group, 0)
            }
            return true
        } else {
            // Not a special key
            // Confirm previous if any
            val ss = selectionState
            if (ss != null) {
                submitGuess(ss.selectedChar)
                selectionState = null
            }
            submitGuess(event.utf16CodePoint.toChar())
            return true
        }
    }

    if (wordState == null) {
        onRoundComplete()
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                    onBack()
                    true
                } else {
                    handleKey(event)
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = onBack) {
                Text("← Back to Menu")
            }
            Spacer(Modifier.height(48.dp))

            val ws = wordState!!
            Text(
                ws.word.sourceWord,
                fontSize = 32.sp,
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.Center) {
                ws.slots.forEach { slot ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .padding(2.dp)
                            .background(
                                when (slot.state) {
                                    LetterState.HIDDEN -> Color.White
                                    LetterState.CORRECT -> Color(0xFF4CAF50)
                                    LetterState.REVEALED_AFTER_ERRORS -> Color(0xFFF44336)
                                }
                            )
                            .border(1.dp, Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (slot.state != LetterState.HIDDEN) {
                            Text(
                                slot.character.toString(),
                                fontSize = 24.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            selectionState?.let { ss ->
                Spacer(Modifier.height(24.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ss.alternatives.forEachIndexed { index, alt ->
                        val isSelected = index == ss.selectedIndex
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .padding(2.dp)
                                .background(if (isSelected) Color.LightGray else Color.Transparent)
                                .border(if (isSelected) 2.dp else 0.dp, Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isSelected) "[$alt]" else alt.toString(),
                                fontSize = 20.sp,
                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else null
                            )
                        }
                    }
                    Text("▼", fontSize = 16.sp)
                }
            }

            if (showWordComplete) {
                Spacer(Modifier.height(32.dp))
                val result = engine.getCompletedWordResult()
                if (result != null) {
                    val (word, perfect) = result
                    Text(
                        if (perfect) "Perfect! ✓" else "Keep practicing...",
                        fontSize = 20.sp,
                        color = if (perfect) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        onWordComplete(word, perfect)
                        engine.moveToNextWord()
                        wordState = engine.currentState
                        showWordComplete = false
                        if (engine.isRoundComplete) {
                            onRoundComplete()
                        }
                    }) {
                        Text("Next")
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(showWordComplete) {
        if (showWordComplete) {
            val result = engine.getCompletedWordResult()
            if (result != null && result.second) {
                delay(400)
                onWordComplete(result.first, true)
                engine.moveToNextWord()
                wordState = engine.currentState
                showWordComplete = false
                if (engine.isRoundComplete) {
                    onRoundComplete()
                }
            }
        } else {
            focusRequester.requestFocus()
        }
    }
}
