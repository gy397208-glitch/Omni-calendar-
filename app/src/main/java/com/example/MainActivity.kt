package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.components.AddEditEventDialog
import com.example.ui.components.AiScheduleDialog
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.CalendarMainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CalendarViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: CalendarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf("calendar") }
                    var showAddDialog by remember { mutableStateOf(false) }
                    var showAiDialog by remember { mutableStateOf(false) }

                    val events by viewModel.allEvents.collectAsState()
                    val selectedSource by viewModel.selectedSourceFilter.collectAsState()
                    val googleAccount by viewModel.googleAccount.collectAsState()
                    val outlookAccount by viewModel.outlookAccount.collectAsState()
                    val aiLoading by viewModel.aiLoading.collectAsState()
                    val aiMessage by viewModel.aiMessage.collectAsState()

                    when (currentScreen) {
                        "calendar" -> {
                            CalendarMainScreen(
                                events = events,
                                selectedSource = selectedSource,
                                onSelectSource = { viewModel.setSourceFilter(it) },
                                onAddEventClick = { showAddDialog = true },
                                onAiAssistantClick = { showAiDialog = true },
                                onAccountsClick = { currentScreen = "accounts" },
                                onDeleteEvent = { viewModel.deleteEvent(it) }
                            )
                        }
                        "accounts" -> {
                            AccountsScreen(
                                googleAccount = googleAccount,
                                outlookAccount = outlookAccount,
                                onToggleGoogle = { viewModel.toggleGoogleSync() },
                                onToggleOutlook = { viewModel.toggleOutlookSync() },
                                onSyncAll = { viewModel.syncAllCalendars() },
                                onBack = { currentScreen = "calendar" }
                            )
                        }
                    }

                    if (showAddDialog) {
                        AddEditEventDialog(
                            onDismiss = { showAddDialog = false },
                            onSave = { event ->
                                viewModel.addEvent(event)
                                showAddDialog = false
                            }
                        )
                    }

                    if (showAiDialog) {
                        AiScheduleDialog(
                            isLoading = aiLoading,
                            aiMessage = aiMessage,
                            onDismiss = { showAiDialog = false },
                            onSubmit = { prompt ->
                                viewModel.parseNaturalLanguageEvent(prompt) { success ->
                                    if (success) {
                                        showAiDialog = false
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
