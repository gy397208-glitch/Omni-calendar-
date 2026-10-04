package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AiScheduleDialog(
    isLoading: Boolean,
    aiMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var prompt by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "✨ AI Smart Scheduler",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Describe your meeting or event in natural language (e.g., 'Dinner with Alex on Friday at 7pm at Downtown Cafe'):",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Event description...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_prompt_input"),
                    minLines = 3
                )
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                if (aiMessage != null) {
                    Text(
                        text = aiMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (aiMessage.startsWith("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (prompt.isNotBlank()) {
                        onSubmit(prompt)
                    }
                },
                enabled = !isLoading && prompt.isNotBlank(),
                modifier = Modifier.testTag("ai_submit_button")
            ) {
                Text("Schedule with AI")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
