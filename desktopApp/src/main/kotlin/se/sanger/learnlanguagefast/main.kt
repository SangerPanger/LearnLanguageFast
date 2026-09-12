package se.sanger.learnlanguagefast

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
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
import se.sanger.learnlanguagefast.pronunciation.DesktopAudioPlayer
import se.sanger.learnlanguagefast.pronunciation.PronunciationManager
import se.sanger.learnlanguagefast.pronunciation.PronunciationSettings
import se.sanger.learnlanguagefast.pronunciation.PronunciationSettingsStore
import se.sanger.learnlanguagefast.data.ImportResult
import se.sanger.learnlanguagefast.data.WordRepository
import se.sanger.learnlanguagefast.game.GameEngine
import se.sanger.learnlanguagefast.model.Word
import se.sanger.learnlanguagefast.ui.*

fun main() = application {
    val repository = remember { WordRepository() }
    val engine = remember { GameEngine() }
    val pronunciationStore = remember { PronunciationSettingsStore() }
    val pronunciationManager = remember { PronunciationManager(DesktopAudioPlayer()) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Language Learning"
    ) {
        MaterialTheme {
            var currentScreen by remember { mutableStateOf<Screen>(Screen.MainMenu) }
            var gameSettings by remember {
                mutableStateOf(
                    repository.getGameSettings().let { base ->
                        val ps = pronunciationStore.load()
                        base.copy(
                            pronunciationEnabled = ps.enabled,
                            autoPlayPronunciation = ps.autoPlay
                        )
                    }
                )
            }
            var lists by remember { mutableStateOf(repository.getAllLists()) }
            var words by remember { mutableStateOf<List<Word>>(emptyList()) }

            fun refreshLists() {
                lists = repository.getAllLists()
            }

            fun refreshWords(listId: Long) {
                words = repository.getWordsForList(listId)
            }

            fun startGame(gameWords: List<Word>, isRetrain: Boolean) {
                engine.startRound(gameWords, isRetrain = isRetrain, gameSettings = gameSettings)
                currentScreen = Screen.Game(isRetrain)
            }

            when (val screen = currentScreen) {
                is Screen.MainMenu -> {
                    MainMenuScreen(
                        onStartNew = {
                            refreshLists()
                            currentScreen = Screen.StartNewListSelection
                        },
                        onRetrain = {
                            refreshLists()
                            currentScreen = Screen.RetrainListSelection
                        },
                        onAddWords = { currentScreen = Screen.AddWordsChoice },
                        onGlossary = {
                            refreshLists()
                            currentScreen = Screen.GlossaryListSelection
                        },
                        onOptions = { currentScreen = Screen.Options },
                        onExit = { exitApplication() }
                    )
                }

                is Screen.Options -> {
                    OptionsScreen(
                        settings = gameSettings,
                        onSettingsChange = { changedSettings ->
                            gameSettings = changedSettings.normalized()
                            repository.saveGameSettings(gameSettings)
                            pronunciationStore.save(
                                PronunciationSettings(
                                    enabled = gameSettings.pronunciationEnabled,
                                    autoPlay = gameSettings.autoPlayPronunciation
                                )
                            )
                        },
                        onBack = { currentScreen = Screen.MainMenu }
                    )
                }

                // ---------------- Add words flow ----------------

                is Screen.AddWordsChoice -> {
                    AddWordsChoiceScreen(
                        onNewList = { currentScreen = Screen.CreateList },
                        onChooseList = {
                            refreshLists()
                            currentScreen = Screen.AddWordsListSelection
                        },
                        onBack = { currentScreen = Screen.MainMenu }
                    )
                }

                is Screen.CreateList -> {
                    CreateListScreen(
                        onCreate = { title ->
                            val trimmed = title.trim()
                            when {
                                trimmed.isEmpty() -> "Title cannot be empty."
                                repository.findListByTitle(trimmed) != null -> "A list with this title already exists."
                                else -> {
                                    val created = repository.createList(trimmed)
                                    if (created == null) {
                                        "Could not create list."
                                    } else {
                                        refreshLists()
                                        currentScreen = Screen.AddWords(created)
                                        null
                                    }
                                }
                            }
                        },
                        onBack = { currentScreen = Screen.AddWordsChoice }
                    )
                }

                is Screen.AddWordsListSelection -> {
                    SingleListSelectionScreen(
                        title = "Add Words",
                        lists = lists,
                        onSelect = { list -> currentScreen = Screen.AddWords(list) },
                        onCreateNew = { currentScreen = Screen.CreateList },
                        onBack = { currentScreen = Screen.AddWordsChoice }
                    )
                }

                is Screen.AddWords -> {
                    AddWordsScreen(
                        listTitle = screen.list.title,
                        onSave = { source, target ->
                            repository.insertWord(screen.list.id, source, target)
                        },
                        onBack = { currentScreen = Screen.MainMenu }
                    )
                }

                // ---------------- Game flow ----------------

                is Screen.StartNewListSelection -> {
                    MultiListSelectionScreen(
                        title = "Start New",
                        lists = lists,
                        startLabel = "Start",
                        onStart = { selectedIds ->
                            val gameWords = repository.getWordsForLists(selectedIds)
                            if (gameWords.isEmpty()) {
                                "No words found in selected lists."
                            } else {
                                startGame(gameWords, isRetrain = false)
                                null
                            }
                        },
                        onBack = { currentScreen = Screen.MainMenu }
                    )
                }

                is Screen.RetrainListSelection -> {
                    MultiListSelectionScreen(
                        title = "Retrain",
                        lists = lists,
                        startLabel = "Start Retrain",
                        onStart = { selectedIds ->
                            val gameWords = repository.getRetrainWordsForLists(selectedIds)
                            if (gameWords.isEmpty()) {
                                "No words need retraining in the selected lists."
                            } else {
                                startGame(gameWords, isRetrain = true)
                                null
                            }
                        },
                        onBack = { currentScreen = Screen.MainMenu }
                    )
                }

                is Screen.Game -> {
                    if (!engine.hasWords) {
                        currentScreen = Screen.RoundComplete
                    } else {
                        GameScreen(
                            engine = engine,
                            onWordComplete = { word, _ ->
                                repository.updateRetrainStatus(word.id, word.retrain, word.perfectCount)
                            },
                            onRoundComplete = {
                                currentScreen = Screen.RoundComplete
                            },
                            onBack = { currentScreen = Screen.MainMenu },
                            gameSettings = gameSettings,
                            pronunciationManager = pronunciationManager
                        )
                    }
                }

                // ---------------- Glossary flow ----------------

                is Screen.GlossaryListSelection -> {
                    SingleListSelectionScreen(
                        title = "Glossary",
                        lists = lists,
                        onSelect = { list ->
                            refreshWords(list.id)
                            currentScreen = Screen.Glossary(list)
                        },
                        onCreateNew = null,
                        onBack = { currentScreen = Screen.MainMenu }
                    )
                }

                is Screen.Glossary -> {
                    val list = screen.list
                    GlossaryScreen(
                        listTitle = list.title,
                        words = words,
                        onEdit = { word -> currentScreen = Screen.EditWord(word, list) },
                        onRetrainToggle = { word, retrain ->
                            // Manually flagging resets progress; manually unflagging marks it as learned.
                            repository.updateRetrainStatus(word.id, retrain, if (retrain) 0 else 3)
                            refreshWords(list.id)
                        },
                        onImportCsv = {
                            val file = FileDialogs.chooseCsvToOpen() ?: return@GlossaryScreen null
                            val result: ImportResult? = try {
                                val text = file.readText(Charsets.UTF_8)
                                repository.importCsv(list.id, text)
                            } catch (e: Exception) {
                                ImportResult(0, 0, 0)
                            }
                            refreshWords(list.id)
                            result
                        },
                        onExportCsv = {
                            val file = FileDialogs.chooseCsvToSave(FileDialogs.suggestedFileName(list.title))
                                ?: return@GlossaryScreen null
                            try {
                                file.writeText(repository.exportCsv(list.id), Charsets.UTF_8)
                                "Exported ${words.size} words to:\n${file.absolutePath}"
                            } catch (e: Exception) {
                                "Export failed: ${e.message}"
                            }
                        },
                        onBack = { currentScreen = Screen.GlossaryListSelection }
                    )
                }

                is Screen.EditWord -> {
                    EditWordScreen(
                        word = screen.word,
                        onSave = { id, source, target ->
                            repository.updateWord(id, source, target)
                            refreshWords(screen.list.id)
                            currentScreen = Screen.Glossary(screen.list)
                        },
                        onCancel = { currentScreen = Screen.Glossary(screen.list) }
                    )
                }

                // ---------------- Round complete ----------------

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
                        Text("Round Complete", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { currentScreen = Screen.MainMenu }) {
                            Text("Back to Menu")
                        }
                    }
                }
            }
        }
    }
}
