package se.sanger.learnlanguagefast

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.input.key.*
import kotlinx.coroutines.delay
import se.sanger.learnlanguagefast.data.WordRepository
import se.sanger.learnlanguagefast.game.GameEngine
import se.sanger.learnlanguagefast.model.Word
import se.sanger.learnlanguagefast.ui.*

fun main() = application {
    val repository = remember { WordRepository() }
    val engine = remember { GameEngine() }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Language Learning"
    ) {
        MaterialTheme {
            var currentScreen by remember { mutableStateOf<Screen>(Screen.MainMenu) }
            var words by remember { mutableStateOf(repository.getAllWords()) }

            fun refreshWords() {
                words = repository.getAllWords()
            }

            when (val screen = currentScreen) {
                is Screen.MainMenu -> {
                    MainMenuScreen(
                        onStartNew = {
                            val allWords = repository.getAllWords()
                            if (allWords.isEmpty()) {
                                currentScreen = Screen.Game(isRetrain = false)
                            } else {
                                engine.startRound(allWords, isRetrain = false)
                                currentScreen = Screen.Game(isRetrain = false)
                            }
                        },
                        onRetrain = {
                            val retrainWords = repository.getRetrainWords()
                            if (retrainWords.isEmpty()) {
                                currentScreen = Screen.Game(isRetrain = true)
                            } else {
                                engine.startRound(retrainWords, isRetrain = true)
                                currentScreen = Screen.Game(isRetrain = true)
                            }
                        },
                        onAddWords = { currentScreen = Screen.AddWords },
                        onGlossary = {
                            refreshWords()
                            currentScreen = Screen.Glossary
                        },
                        onExit = { exitApplication() }
                    )
                }

                is Screen.AddWords -> {
                    AddWordsScreen(
                        onSave = { source, target ->
                            repository.insertWord(source, target)
                        },
                        onBack = { currentScreen = Screen.MainMenu }
                    )
                }

                is Screen.Game -> {
                    val allWords = if (!screen.isRetrain) repository.getAllWords() else repository.getRetrainWords()
                    if (allWords.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                if (screen.isRetrain) "No words need retraining."
                                else "No words added yet."
                            )
                            Spacer(Modifier.height(16.dp))
                            androidx.compose.material3.Button(onClick = {
                                currentScreen = Screen.MainMenu
                            }) {
                                Text("Back to Menu")
                            }
                        }
                    } else if (!engine.hasWords) {
                        currentScreen = Screen.RoundComplete
                    } else {
                        GameScreen(
                            engine = engine,
                            onWordComplete = { word, perfect ->
                                repository.updateRetrainStatus(word.id, word.retrain, word.perfectCount)
                            },
                            onRoundComplete = {
                                currentScreen = Screen.RoundComplete
                            },
                            onBack = { currentScreen = Screen.MainMenu }
                        )
                    }
                }

                is Screen.Glossary -> {
                    GlossaryScreen(
                        words = words,
                        onEdit = { word -> currentScreen = Screen.EditWord(word) },
                        onRetrainToggle = { word, retrain ->
                            // When manually toggling, we reset perfectCount to 0 if marking for retrain,
                            // or leave it as is if unflagging? Usually manual unflag means 
                            // user wants it gone. Let's reset to 0 if flagging, and set to 3 if unflagging?
                            // Simple approach: just update retrain, reset perfectCount to 0 if retrain=true,
                            // or 3 if retrain=false.
                            repository.updateRetrainStatus(word.id, retrain, if (retrain) 0 else 3)
                            refreshWords()
                        },
                        onBack = { currentScreen = Screen.MainMenu }
                    )
                }

                is Screen.EditWord -> {
                    EditWordScreen(
                        word = screen.word,
                        onSave = { id, source, target ->
                            repository.updateWord(id, source, target)
                            refreshWords()
                            currentScreen = Screen.Glossary
                        },
                        onCancel = { currentScreen = Screen.Glossary }
                    )
                }

                is Screen.RoundComplete -> {
                    LaunchedEffect(Unit) {
                        delay(1000)
                        currentScreen = Screen.MainMenu
                    }
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .onPreviewKeyEvent { event ->
                                if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                                    currentScreen = Screen.MainMenu
                                    true
                                } else false
                            }
                            .focusable(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        LaunchedEffect(Unit) {
                            // Ensure focus for key events
                        }
                        Text("Round Complete", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(24.dp))
                        androidx.compose.material3.Button(onClick = {
                            currentScreen = Screen.MainMenu
                        }) {
                            Text("Back to Menu")
                        }
                    }
                }
            }
        }
    }
}
