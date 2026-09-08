package se.sanger.learnlanguagefast.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.input.key.*

@Composable
fun MainMenuScreen(
    onStartNew: () -> Unit,
    onRetrain: () -> Unit,
    onAddWords: () -> Unit,
    onGlossary: () -> Unit,
    onExit: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                    onExit()
                    true
                } else false
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("LANGUAGE", fontSize = 28.sp)
        Spacer(Modifier.height(40.dp))
        Button(onClick = onStartNew, modifier = Modifier.width(200.dp)) {
            Text("Start New")
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetrain, modifier = Modifier.width(200.dp)) {
            Text("Retrain")
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onAddWords, modifier = Modifier.width(200.dp)) {
            Text("Add Words")
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onGlossary, modifier = Modifier.width(200.dp)) {
            Text("Glossary")
        }
    }
}
