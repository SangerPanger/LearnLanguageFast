package se.sanger.learnlanguagefast

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
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
                                engine.startRound(allWords)
                                currentScreen = Screen.Game(isRetrain = false)
                            }
                        },
                        onRetrain = {
                            val retrainWords = repository.getRetrainWords()
                            if (retrainWords.isEmpty()) {
                                currentScreen = Screen.Game(isRetrain = true)
                            } else {
                                engine.startRound(retrainWords)
                                currentScreen = Screen.Game(isRetrain = true)
                            }
                        },
                        onAddWords = { currentScreen = Screen.AddWords },
                        onGlossary = {
                            refreshWords()
                            currentScreen = Screen.Glossary
                        }
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
                            onWordComplete = { wordId, perfect ->
                                repository.updateRetrainStatus(wordId, !perfect)
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
                        onRetrainToggle = { id, retrain ->
                            repository.updateRetrainStatus(id, retrain)
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
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
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
