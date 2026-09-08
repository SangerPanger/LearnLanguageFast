package se.sanger.learnlanguagefast.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.*
import se.sanger.learnlanguagefast.data.ImportResult
import se.sanger.learnlanguagefast.model.Word

@Composable
fun GlossaryScreen(
    listTitle: String,
    words: List<Word>,
    onEdit: (Word) -> Unit,
    onRetrainToggle: (Word, Boolean) -> Unit,
    onImportCsv: () -> ImportResult?,
    onExportCsv: () -> String?,
    onBack: () -> Unit
) {
    var importResult by remember { mutableStateOf<ImportResult?>(null) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
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
            Text("← Back")
        }
        Spacer(Modifier.height(16.dp))
        Text("Glossary", style = MaterialTheme.typography.headlineMedium)
        Text(listTitle, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { importResult = onImportCsv() }) {
                Text("Import CSV")
            }
            OutlinedButton(onClick = { exportMessage = onExportCsv() }) {
                Text("Export CSV")
            }
        }
        Spacer(Modifier.height(16.dp))

        if (words.isEmpty()) {
            Text("No words in this list yet.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(words) { word ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(word.sourceWord, modifier = Modifier.weight(1f))
                        Text(word.targetWord, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onEdit(word) }) {
                            Text("✎")
                        }
                        Checkbox(
                            checked = word.retrain,
                            onCheckedChange = { checked ->
                                onRetrainToggle(word, checked)
                            }
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    importResult?.let { result ->
        AlertDialog(
            onDismissRequest = { importResult = null },
            title = { Text("Import complete") },
            text = {
                Column {
                    Text("Imported: ${result.imported}")
                    Text("Skipped duplicates: ${result.skippedDuplicates}")
                    Text("Invalid rows: ${result.invalidRows}")
                }
            },
            confirmButton = {
                Button(onClick = { importResult = null }) { Text("OK") }
            }
        )
    }

    exportMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { exportMessage = null },
            title = { Text("Export") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = { exportMessage = null }) { Text("OK") }
            }
        )
    }
}

@Composable
fun EditWordScreen(
    word: Word,
    onSave: (Long, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var sourceWord by remember { mutableStateOf(word.sourceWord) }
    var targetWord by remember { mutableStateOf(word.targetWord) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                    onCancel()
                    true
                } else false
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Edit Word", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = sourceWord,
            onValueChange = { sourceWord = it },
            label = { Text("Your language") },
            singleLine = true,
            modifier = Modifier.width(300.dp)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = targetWord,
            onValueChange = { targetWord = it },
            label = { Text("Learning Language") },
            singleLine = true,
            modifier = Modifier.width(300.dp)
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val s = sourceWord.trim()
                val t = targetWord.trim()
                if (s.isNotEmpty() && t.isNotEmpty()) {
                    onSave(word.id, s, t)
                }
            }) {
                Text("Save")
            }
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    }
}
