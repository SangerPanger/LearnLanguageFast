package se.sanger.learnlanguagefast.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import se.sanger.learnlanguagefast.model.GameSettings

@Composable
fun OptionsScreen(
    settings: GameSettings,
    onSettingsChange: (GameSettings) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                    onBack()
                    true
                } else {
                    false
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("OPTIONS", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(32.dp))

        OptionToggle("Failsafe", settings.failsafeEnabled) {
            onSettingsChange(settings.copy(failsafeEnabled = it))
        }
        NumberOption(
            label = "Mistakes before reveal",
            value = settings.failsafeMistakes,
            minimum = GameSettings.MIN_FAILSAFE_MISTAKES,
            maximum = GameSettings.MAX_FAILSAFE_MISTAKES,
            enabled = settings.failsafeEnabled
        ) { onSettingsChange(settings.copy(failsafeMistakes = it)) }

        OptionToggle("Hardcore", settings.hardcoreEnabled) {
            onSettingsChange(settings.copy(hardcoreEnabled = it))
        }
        OptionToggle("Repeater", settings.repeaterEnabled) {
            onSettingsChange(settings.copy(repeaterEnabled = it))
        }
        NumberOption(
            label = "Required repetitions",
            value = settings.repeaterCount,
            minimum = GameSettings.MIN_REPEATER_COUNT,
            maximum = GameSettings.MAX_REPEATER_COUNT,
            enabled = settings.repeaterEnabled
        ) { onSettingsChange(settings.copy(repeaterCount = it)) }

        OptionToggle("Flow", settings.flowEnabled) {
            onSettingsChange(settings.copy(flowEnabled = it))
        }
        OptionToggle("Imprint", settings.imprintEnabled) {
            onSettingsChange(settings.copy(imprintEnabled = it))
        }

        OptionToggle("Auto advance to next word", settings.autoAdvanceEnabled) {
            onSettingsChange(settings.copy(autoAdvanceEnabled = it))
        }

        Spacer(Modifier.height(16.dp))
        Text("Pronunciation", style = MaterialTheme.typography.titleLarge)
        OptionToggle("Enabled", settings.pronunciationEnabled) {
            onSettingsChange(settings.copy(pronunciationEnabled = it))
        }
        OptionToggle(
            label = "Auto play pronunciation",
            checked = settings.autoPlayPronunciation && settings.pronunciationEnabled
        ) {
            onSettingsChange(settings.copy(autoPlayPronunciation = it))
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = onBack) {
            Text("← Back")
        }
    }
}

@Composable
private fun OptionToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.width(360.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NumberOption(
    label: String,
    value: Int,
    minimum: Int,
    maximum: Int,
    enabled: Boolean,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.width(360.dp).padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { onValueChange(value - 1) }, enabled = enabled && value > minimum) {
                Text("−")
            }
            Text(value.toString(), modifier = Modifier.padding(horizontal = 16.dp))
            Button(onClick = { onValueChange(value + 1) }, enabled = enabled && value < maximum) {
                Text("+")
            }
        }
    }
}