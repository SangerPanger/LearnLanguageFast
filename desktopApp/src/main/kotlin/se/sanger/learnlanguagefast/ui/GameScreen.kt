package se.sanger.learnlanguagefast.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import se.sanger.learnlanguagefast.game.*

@Composable
fun GameScreen(
    engine: GameEngine,
    onWordComplete: (wordId: Long, perfect: Boolean) -> Unit,
    onRoundComplete: () -> Unit,
    onBack: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var wordState by remember { mutableStateOf(engine.currentState) }
    var flashRed by remember { mutableStateOf(false) }
    var showWordComplete by remember { mutableStateOf(false) }

    val bgColor by animateColorAsState(
        targetValue = if (flashRed) Color.Red.copy(alpha = 0.3f) else Color.Transparent
    )

    LaunchedEffect(flashRed) {
        if (flashRed) {
            delay(300)
            flashRed = false
        }
    }

    fun handleKey(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        if (showWordComplete) return false

        val char = event.utf16CodePoint.toChar()
        if (!char.isLetter()) return false

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
            }
            is GuessResult.Correct -> {}
            null -> {}
        }
        return true
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
            .onPreviewKeyEvent { handleKey(it) }
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
                        onWordComplete(word.id, perfect)
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
        if (!showWordComplete) {
            focusRequester.requestFocus()
        }
    }
}
