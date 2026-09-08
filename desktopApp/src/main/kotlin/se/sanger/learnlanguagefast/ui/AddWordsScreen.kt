package se.sanger.learnlanguagefast.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp

@Composable
fun AddWordsScreen(
    onSave: (source: String, target: String) -> Unit,
    onBack: () -> Unit
) {
    var sourceWord by remember { mutableStateOf("") }
    var targetWord by remember { mutableStateOf("") }
    val sourceFocus = remember { FocusRequester() }
    val targetFocus = remember { FocusRequester() }

    fun saveWord() {
        val s = sourceWord.trim()
        val t = targetWord.trim()
        if (s.isNotEmpty() && t.isNotEmpty()) {
            onSave(s, t)
            sourceWord = ""
            targetWord = ""
            sourceFocus.requestFocus()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                    onBack()
                    true
                } else false
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onBack) {
            Text("← Back to Menu")
        }
        Spacer(Modifier.height(24.dp))
        Text("Add Words", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = sourceWord,
            onValueChange = { sourceWord = it },
            label = { Text("Your language") },
            singleLine = true,
            modifier = Modifier.width(300.dp)
                .focusRequester(sourceFocus)
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                        targetFocus.requestFocus()
                        true
                    } else false
                }
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = targetWord,
            onValueChange = { targetWord = it },
            label = { Text("Learning Language") },
            singleLine = true,
            modifier = Modifier.width(300.dp)
                .focusRequester(targetFocus)
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                        saveWord()
                        true
                    } else false
                }
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { saveWord() }) {
            Text("Save Word")
        }
    }

    LaunchedEffect(Unit) {
        sourceFocus.requestFocus()
    }
}
