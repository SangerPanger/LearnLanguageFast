package se.sanger.learnlanguagefast.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import se.sanger.learnlanguagefast.model.WordList

private fun Modifier.escapeToBack(onBack: () -> Unit): Modifier =
    this.onPreviewKeyEvent { event ->
        if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
            onBack()
            true
        } else false
    }

/** Add Words entry point: choose between creating a new list or picking an existing one. */
@Composable
fun AddWordsChoiceScreen(
    onNewList: () -> Unit,
    onChooseList: () -> Unit,
    onBack: () -> Unit
) {
    val focus = remember { FocusRequester() }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
            .escapeToBack(onBack)
            .focusRequester(focus)
            .focusable(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onBack) { Text("← Back to Menu") }
        Spacer(Modifier.height(24.dp))
        Text("Add Words", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onNewList, modifier = Modifier.width(220.dp)) { Text("New List") }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onChooseList, modifier = Modifier.width(220.dp)) { Text("Choose List") }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

/** Create a new list. [onCreate] returns an error message or null on success. */
@Composable
fun CreateListScreen(
    onCreate: (String) -> String?,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }

    fun create() {
        error = onCreate(title)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).escapeToBack(onBack),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onBack) { Text("← Back") }
        Spacer(Modifier.height(24.dp))
        Text("New List", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it; error = null },
            label = { Text("List title") },
            singleLine = true,
            isError = error != null,
            modifier = Modifier.width(300.dp)
                .focusRequester(focus)
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                        create()
                        true
                    } else false
                }
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { create() }) { Text("Create") }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

/** Pick exactly one list (used by Add Words → Choose List and Glossary). */
@Composable
fun SingleListSelectionScreen(
    title: String,
    lists: List<WordList>,
    onSelect: (WordList) -> Unit,
    onCreateNew: (() -> Unit)?,
    onBack: () -> Unit
) {
    val focus = remember { FocusRequester() }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
            .escapeToBack(onBack)
            .focusRequester(focus)
            .focusable(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onBack) { Text("← Back") }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Choose a list:")
        Spacer(Modifier.height(16.dp))

        if (lists.isEmpty()) {
            Text("No lists yet.")
            if (onCreateNew != null) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = onCreateNew) { Text("Create New List") }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).width(320.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(lists) { list ->
                    Button(
                        onClick = { onSelect(list) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(list.title)
                    }
                }
            }
            if (onCreateNew != null) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onCreateNew) { Text("Create New List") }
            }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

/** Pick one or more lists with checkboxes (used by Start New and Retrain). */
@Composable
fun MultiListSelectionScreen(
    title: String,
    lists: List<WordList>,
    startLabel: String,
    onStart: (Set<Long>) -> String?,
    onBack: () -> Unit
) {
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var message by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }

    fun start() {
        if (selected.isEmpty()) {
            message = "Please select at least one list."
            return
        }
        message = onStart(selected)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
            .escapeToBack(onBack)
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                    start()
                    true
                } else false
            }
            .focusRequester(focus)
            .focusable(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onBack) { Text("← Back to Menu") }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Select lists:")
        Spacer(Modifier.height(16.dp))

        if (lists.isEmpty()) {
            Text("No lists yet. Add words first.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f).width(320.dp)) {
                items(lists) { list ->
                    val checked = list.id in selected
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable {
                                selected = if (checked) selected - list.id else selected + list.id
                                message = null
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                selected = if (isChecked) selected + list.id else selected - list.id
                                message = null
                            }
                        )
                        Text(list.title)
                    }
                }
            }
            message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { start() }, enabled = selected.isNotEmpty()) { Text(startLabel) }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}
