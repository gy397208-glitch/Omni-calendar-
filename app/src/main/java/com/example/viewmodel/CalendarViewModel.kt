package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.EventEntity
import com.example.data.EventRepository
import com.example.network.Content
import com.example.network.GeminiClient
import com.example.network.GenerationConfig
import com.example.network.GenerateContentRequest
import com.example.network.Part
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountStatus(
    val name: String,
    val email: String,
    val isConnected: Boolean,
    val lastSynced: String
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: EventRepository

    init {
        val eventDao = AppDatabase.getDatabase(application).eventDao()
        repository = EventRepository(eventDao)
        seedInitialEventsIfNeeded()
    }

    val allEvents: StateFlow<List<EventEntity>> = repository.allEvents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedSourceFilter = MutableStateFlow("All")
    val selectedSourceFilter: StateFlow<String> = _selectedSourceFilter.asStateFlow()

    private val _googleAccount = MutableStateFlow(
        AccountStatus("Google Calendar", "user@gmail.com", true, "Today, 10:30 AM")
    )
    val googleAccount: StateFlow<AccountStatus> = _googleAccount.asStateFlow()

    private val _outlookAccount = MutableStateFlow(
        AccountStatus("Outlook Calendar", "user@outlook.com", true, "Today, 09:15 AM")
    )
    val outlookAccount: StateFlow<AccountStatus> = _outlookAccount.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiMessage = MutableStateFlow<String?>(null)
    val aiMessage: StateFlow<String?> = _aiMessage.asStateFlow()

    fun setSourceFilter(source: String) {
        _selectedSourceFilter.value = source
    }

    fun toggleGoogleSync() {
        val current = _googleAccount.value
        _googleAccount.value = current.copy(
            isConnected = !current.isConnected,
            lastSynced = if (!current.isConnected) "Just now" else current.lastSynced
        )
    }

    fun toggleOutlookSync() {
        val current = _outlookAccount.value
        _outlookAccount.value = current.copy(
            isConnected = !current.isConnected,
            lastSynced = if (!current.isConnected) "Just now" else current.lastSynced
        )
    }

    fun syncAllCalendars() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (_googleAccount.value.isConnected) {
                repository.insert(
                    EventEntity(
                        title = "Google Sync: Team Sync",
                        description = "Synced from Google Calendar",
                        date = now,
                        startTime = "11:00 AM",
                        endTime = "11:30 AM",
                        location = "Google Meet",
                        category = "Work",
                        calendarSource = "Google"
                    )
                )
            }
            if (_outlookAccount.value.isConnected) {
                repository.insert(
                    EventEntity(
                        title = "Outlook Sync: Project Review",
                        description = "Synced from Outlook Calendar",
                        date = now + 86400000L,
                        startTime = "02:00 PM",
                        endTime = "03:00 PM",
                        location = "Microsoft Teams",
                        category = "Meeting",
                        calendarSource = "Outlook"
                    )
                )
            }
        }
    }

    fun addEvent(event: EventEntity) {
        viewModelScope.launch {
            repository.insert(event)
        }
    }

    fun updateEvent(event: EventEntity) {
        viewModelScope.launch {
            repository.update(event)
        }
    }

    fun deleteEvent(event: EventEntity) {
        viewModelScope.launch {
            repository.delete(event)
        }
    }

    fun parseNaturalLanguageEvent(prompt: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _aiLoading.value = true
            _aiMessage.value = null
            try {
                val apiKey = try {
                    val field = com.example.BuildConfig::class.java.getField("GEMINI_API_KEY")
                    field.get(null) as? String ?: ""
                } catch (e: Exception) {
                    ""
                }

                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    _aiMessage.value = "Gemini API Key missing. Please add it in Secrets panel."
                    _aiLoading.value = false
                    onComplete(false)
                    return@launch
                }

                val systemPrompt = "You are a smart calendar assistant. Parse the user's event description into a JSON object with keys: title (string), description (string), startTime (string, e.g. '03:00 PM'), endTime (string), location (string), category (string: Work, Personal, Meeting, Holiday), calendarSource (string: Local, Google, Outlook). Return ONLY valid JSON."
                val userPrompt = "Parse this event: $prompt"

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = userPrompt)))),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.2f
                    ),
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                )

                val response = GeminiClient.service.generateContent(apiKey, request)
                val textResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (textResponse != null) {
                    val parsed = parseEventFromJson(textResponse, prompt)
                    repository.insert(parsed)
                    _aiMessage.value = "Successfully created event: ${parsed.title}!"
                    _aiLoading.value = false
                    onComplete(true)
                } else {
                    _aiMessage.value = "No response from AI."
                    _aiLoading.value = false
                    onComplete(false)
                }
            } catch (e: Exception) {
                _aiMessage.value = "Error: ${e.localizedMessage}"
                _aiLoading.value = false
                onComplete(false)
            }
        }
    }

    private fun parseEventFromJson(json: String, fallbackPrompt: String): EventEntity {
        try {
            val titleMatch = Regex("\"title\"\\s*:\\s*\"([^\"]+)\"").find(json)
            val descMatch = Regex("\"description\"\\s*:\\s*\"([^\"]+)\"").find(json)
            val startTimeMatch = Regex("\"startTime\"\\s*:\\s*\"([^\"]+)\"").find(json)
            val endTimeMatch = Regex("\"endTime\"\\s*:\\s*\"([^\"]+)\"").find(json)
            val locMatch = Regex("\"location\"\\s*:\\s*\"([^\"]+)\"").find(json)
            val catMatch = Regex("\"category\"\\s*:\\s*\"([^\"]+)\"").find(json)
            val sourceMatch = Regex("\"calendarSource\"\\s*:\\s*\"([^\"]+)\"").find(json)

            val title = titleMatch?.groupValues?.get(1) ?: fallbackPrompt
            val desc = descMatch?.groupValues?.get(1) ?: "Created via AI Assistant"
            val startTime = startTimeMatch?.groupValues?.get(1) ?: "09:00 AM"
            val endTime = endTimeMatch?.groupValues?.get(1) ?: "10:00 AM"
            val location = locMatch?.groupValues?.get(1) ?: "Online"
            val category = catMatch?.groupValues?.get(1) ?: "Meeting"
            val source = sourceMatch?.groupValues?.get(1) ?: "Local"

            return EventEntity(
                title = title,
                description = desc,
                date = System.currentTimeMillis(),
                startTime = startTime,
                endTime = endTime,
                location = location,
                category = category,
                calendarSource = source
            )
        } catch (e: Exception) {
            return EventEntity(
                title = fallbackPrompt,
                description = "AI parsed event",
                date = System.currentTimeMillis(),
                startTime = "10:00 AM",
                endTime = "11:00 AM",
                location = "Main Office",
                category = "Work",
                calendarSource = "Local"
            )
        }
    }

    private fun seedInitialEventsIfNeeded() {
        viewModelScope.launch {
            // Seed initial sample events
        }
    }
}
