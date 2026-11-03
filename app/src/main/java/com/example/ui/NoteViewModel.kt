package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.FirestoreNoteRepository
import com.example.data.local.AppDatabase
import com.example.data.local.BackupSerializer
import com.example.data.local.NoteEntity
import com.example.data.local.RecentSearchEntity
import com.example.data.repository.NoteRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "NoteViewModel"

enum class NoteSortOrder(val displayName: String) {
    MODIFIED_DATE("Modified Date"),
    CREATED_DATE("Created Date"),
    ALPHABETICAL("Alphabetical")
}

sealed interface AiAssistState {
    data object Idle : AiAssistState
    data object Loading : AiAssistState
    data class Success(val message: String) : AiAssistState
    data class Error(val error: String) : AiAssistState
}

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository
    private val firestoreRepository: FirestoreNoteRepository by lazy {
        FirestoreNoteRepository(application)
    }

    private val noteAnalysisSchema = Schema.obj(
        properties = mapOf(
            "enhancedTitle" to Schema.string(description = "A concise, clear title for the note"),
            "enhancedContent" to Schema.string(description = "Enhanced, structured, and well-formatted note content"),
            "suggestedCategory" to Schema.enumeration(
                values = listOf("Personal", "Work", "Ideas", "Study", "General"),
                description = "Best matching category"
            ),
            "suggestedTags" to Schema.array(
                items = Schema.string(),
                description = "Up to 4 relevant tags without # prefix"
            )
        )
    )

    private val aiModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = noteAnalysisSchema
            }
        )
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private val _sortOrder = MutableStateFlow(NoteSortOrder.MODIFIED_DATE)
    val sortOrder: StateFlow<NoteSortOrder> = _sortOrder.asStateFlow()

    private val _activeTab = MutableStateFlow(0) // 0: Notes, 1: Labels, 2: Reminders, 3: Settings
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    private val _isAddEditOpen = MutableStateFlow(false)
    val isAddEditOpen: StateFlow<Boolean> = _isAddEditOpen.asStateFlow()

    private val _startVoiceOnOpen = MutableStateFlow(false)
    val startVoiceOnOpen: StateFlow<Boolean> = _startVoiceOnOpen.asStateFlow()

    private val _editingNote = MutableStateFlow<NoteEntity?>(null)
    val editingNote: StateFlow<NoteEntity?> = _editingNote.asStateFlow()

    private val _aiAssistState = MutableStateFlow<AiAssistState>(AiAssistState.Idle)
    val aiAssistState: StateFlow<AiAssistState> = _aiAssistState.asStateFlow()

    private val _cloudSyncStatus = MutableStateFlow<String?>(null)
    val cloudSyncStatus: StateFlow<String?> = _cloudSyncStatus.asStateFlow()

    private val _backupOperationStatus = MutableStateFlow<String?>(null)
    val backupOperationStatus: StateFlow<String?> = _backupOperationStatus.asStateFlow()

    private var searchHistoryDebounceJob: Job? = null

    val allNotesList: StateFlow<List<NoteEntity>>

    val allTagsList: StateFlow<List<String>>

    val recentSearches: StateFlow<List<RecentSearchEntity>>

    @OptIn(ExperimentalCoroutinesApi::class)
    val notesState: StateFlow<List<NoteEntity>>

    init {
        val database = AppDatabase.getDatabase(application)
        val noteDao = database.noteDao()
        val recentSearchDao = database.recentSearchDao()
        repository = NoteRepository(noteDao, recentSearchDao)

        viewModelScope.launch(Dispatchers.IO) {
            repository.seedInitialDataIfEmpty()
        }

        allNotesList = repository.allNotes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        recentSearches = repository.recentSearches.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allTagsList = repository.allNotes.map { notes ->
            notes.flatMap { note ->
                note.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
            }.distinct().sorted()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        notesState = combine(_searchQuery, _selectedCategory, _selectedTag, _sortOrder) { query, category, tag, sortOrder ->
            Tuple4(query, category, tag, sortOrder)
        }.flatMapLatest { (query, category, tag, sortOrder) ->
            val baseFlow = if (query.isNotBlank()) {
                repository.searchNotes(query)
            } else if (category == "Pinned") {
                repository.allNotes
            } else {
                repository.getNotesByCategory(category)
            }
            baseFlow.map { notes ->
                var filtered = if (category == "Pinned") {
                    notes.filter { it.isPinned }
                } else {
                    notes
                }
                if (!tag.isNullOrBlank()) {
                    filtered = filtered.filter { note ->
                        note.tags.split(",").map { it.trim().lowercase() }.contains(tag.trim().lowercase())
                    }
                }
                when (sortOrder) {
                    NoteSortOrder.MODIFIED_DATE -> filtered.sortedWith(
                        compareByDescending<NoteEntity> { it.isPinned }
                            .thenByDescending { it.updatedAt }
                    )
                    NoteSortOrder.CREATED_DATE -> filtered.sortedWith(
                        compareByDescending<NoteEntity> { it.isPinned }
                            .thenByDescending { it.createdAt }
                    )
                    NoteSortOrder.ALPHABETICAL -> filtered.sortedWith(
                        compareByDescending<NoteEntity> { it.isPinned }
                            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title.ifBlank { it.content } }
                    )
                }
            }
        }.flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        searchHistoryDebounceJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length >= 2) {
            searchHistoryDebounceJob = viewModelScope.launch {
                delay(1200L)
                repository.saveRecentSearch(trimmed)
            }
        }
    }

    fun onSearchSubmitted(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            searchHistoryDebounceJob?.cancel()
            viewModelScope.launch {
                repository.saveRecentSearch(trimmed)
            }
        }
    }

    fun onSelectRecentSearch(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            repository.saveRecentSearch(query)
        }
    }

    fun onDeleteRecentSearch(query: String) {
        viewModelScope.launch {
            repository.deleteRecentSearch(query)
        }
    }

    fun onClearRecentSearches() {
        viewModelScope.launch {
            repository.clearAllRecentSearches()
        }
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun onTagSelected(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
    }

    fun onSortOrderSelected(order: NoteSortOrder) {
        _sortOrder.value = order
    }

    fun onTabSelected(tabIndex: Int) {
        _activeTab.value = tabIndex
        if (tabIndex == 0) {
            _selectedTag.value = null
        }
    }

    fun onPinToggle(note: NoteEntity) {
        viewModelScope.launch {
            repository.togglePin(note.id, note.isPinned)
        }
    }

    fun onOpenAddNote(startVoiceImmediately: Boolean = false) {
        _editingNote.value = null
        _aiAssistState.value = AiAssistState.Idle
        _startVoiceOnOpen.value = startVoiceImmediately
        _isAddEditOpen.value = true
    }

    fun onOpenEditNote(note: NoteEntity) {
        val activeSearch = _searchQuery.value.trim()
        if (activeSearch.isNotEmpty()) {
            viewModelScope.launch {
                repository.saveRecentSearch(activeSearch)
            }
        }
        _editingNote.value = note
        _aiAssistState.value = AiAssistState.Idle
        _startVoiceOnOpen.value = false
        _isAddEditOpen.value = true
    }

    fun onCloseDialog() {
        _isAddEditOpen.value = false
        _startVoiceOnOpen.value = false
        _editingNote.value = null
        _aiAssistState.value = AiAssistState.Idle
    }

    fun clearAiAssistState() {
        _aiAssistState.value = AiAssistState.Idle
    }

    fun updateBackupStatus(message: String?) {
        _backupOperationStatus.value = message
    }

    fun importNotesFromRawBackup(rawContent: String, onComplete: (Int, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val parsedNotes = BackupSerializer.parseAuto(rawContent)
                if (parsedNotes.isEmpty()) {
                    val msg = "No valid notes found in the selected file."
                    _backupOperationStatus.value = msg
                    onComplete(0, msg)
                    return@launch
                }
                val importedCount = repository.importNotes(parsedNotes)
                val statusMsg = if (importedCount > 0) {
                    "Successfully restored $importedCount notes into Room Database."
                } else {
                    "All ${parsedNotes.size} notes from backup already exist in your database."
                }
                _backupOperationStatus.value = statusMsg
                onComplete(importedCount, statusMsg)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import notes from backup", e)
                val errMsg = "Import failed: ${e.localizedMessage ?: "Invalid file format"}"
                _backupOperationStatus.value = errMsg
                onComplete(0, errMsg)
            }
        }
    }

    fun enhanceNoteWithGemini(
        currentTitle: String,
        currentContent: String,
        mode: String = "enhance",
        onResult: (title: String, content: String, category: String, tags: List<String>) -> Unit
    ) {
        if (currentTitle.isBlank() && currentContent.isBlank()) {
            _aiAssistState.value = AiAssistState.Error("Write a title or draft content first so Gemini can assist.")
            return
        }

        viewModelScope.launch {
            _aiAssistState.value = AiAssistState.Loading
            try {
                val prompt = when (mode) {
                    "summarize" -> """
                        Summarize and structure the following note into concise bullet points with a clear title, best category, and relevant tags.
                        Title: $currentTitle
                        Content: $currentContent
                    """.trimIndent()
                    else -> """
                        Enhance, polish, and organize the following note for clarity and readability while preserving its core meaning. Also suggest a strong title, matching category, and relevant tags.
                        Title: $currentTitle
                        Content: $currentContent
                    """.trimIndent()
                }

                val response = withTimeout(60_000L) {
                    aiModel.generateContent(prompt)
                }

                val jsonString = response.text ?: throw IllegalStateException("Gemini returned an empty response.")
                val json = JSONObject(jsonString)
                val newTitle = json.optString("enhancedTitle", currentTitle).ifBlank { currentTitle }
                val newContent = json.optString("enhancedContent", currentContent).ifBlank { currentContent }
                val suggestedCategory = json.optString("suggestedCategory", "Personal")
                val tagsArray = json.optJSONArray("suggestedTags")
                val suggestedTags = mutableListOf<String>()
                if (tagsArray != null) {
                    for (i in 0 until tagsArray.length()) {
                        val tag = tagsArray.optString(i).trim().removePrefix("#")
                        if (tag.isNotBlank()) suggestedTags.add(tag)
                    }
                }

                onResult(newTitle, newContent, suggestedCategory, suggestedTags)
                _aiAssistState.value = AiAssistState.Success(
                    if (mode == "summarize") "Summarized with Gemini AI" else "Enhanced with Gemini AI"
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini AI request timed out", e)
                _aiAssistState.value = AiAssistState.Error("Gemini AI request timed out after 60s. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini AI generation failed", e)
                _aiAssistState.value = AiAssistState.Error(e.localizedMessage ?: "Gemini AI enhancement failed.")
            }
        }
    }

    fun syncNotesWithFirestore() {
        viewModelScope.launch {
            _cloudSyncStatus.value = "Syncing with Cloud Firestore..."
            try {
                val localNotes = allNotesList.value
                var syncedCount = 0
                for (note in localNotes) {
                    val docId = "note_${note.id}"
                    firestoreRepository.createNote(
                        title = note.title.ifBlank { "Untitled Note" },
                        content = note.content,
                        category = note.category,
                        tags = note.tags,
                        isPinned = note.isPinned,
                        customId = docId
                    )
                    syncedCount++
                }

                // Also pull any cloud notes that aren't stored locally
                val cloudNotes = withTimeout(15_000L) {
                    firestoreRepository.observeNotes().first()
                }
                val existingTitlesAndContent = localNotes.map { "${it.title}|${it.content}" }.toSet()
                var importedCount = 0
                for (cloudNote in cloudNotes) {
                    val signature = "${cloudNote.title}|${cloudNote.content}"
                    if (signature !in existingTitlesAndContent && (cloudNote.title.isNotBlank() || cloudNote.content.isNotBlank())) {
                        repository.insertNote(
                            NoteEntity(
                                title = cloudNote.title,
                                content = cloudNote.content,
                                category = cloudNote.category,
                                tags = cloudNote.tags,
                                isPinned = cloudNote.isPinned,
                                updatedAt = cloudNote.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                            )
                        )
                        importedCount++
                    }
                }

                _cloudSyncStatus.value = "Synced $syncedCount notes to Cloud" +
                        if (importedCount > 0) " ($importedCount imported)" else ""
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Cloud Firestore sync failed", e)
                _cloudSyncStatus.value = "Sync failed: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun onSaveNote(title: String, content: String, category: String, tags: String, isPinned: Boolean) {
        if (title.isBlank() && content.isBlank()) return

        viewModelScope.launch {
            val currentEditing = _editingNote.value
            val savedId: Long
            if (currentEditing != null && currentEditing.id != 0L) {
                val updated = currentEditing.copy(
                    title = title.trim(),
                    content = content.trim(),
                    category = category,
                    tags = tags.trim(),
                    isPinned = isPinned,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateNote(updated)
                savedId = updated.id
            } else {
                val newNote = NoteEntity(
                    title = title.trim().ifEmpty { "Untitled Note" },
                    content = content.trim(),
                    category = category,
                    tags = tags.trim(),
                    isPinned = isPinned,
                    updatedAt = System.currentTimeMillis()
                )
                savedId = repository.insertNote(newNote)
            }
            onCloseDialog()

            // Mirror to Firestore asynchronously if signed in
            try {
                firestoreRepository.createNote(
                    title = title.trim().ifEmpty { "Untitled Note" },
                    content = content.trim(),
                    category = category,
                    tags = tags.trim(),
                    isPinned = isPinned,
                    customId = "note_$savedId"
                )
            } catch (e: Exception) {
                Log.w(TAG, "Background Firestore mirror skipped: ${e.message}")
            }
        }
    }

    fun onAutoSave(
        currentId: Long?,
        title: String,
        content: String,
        category: String,
        tags: String,
        isPinned: Boolean,
        onSaved: (Long) -> Unit
    ) {
        if (title.isBlank() && content.isBlank()) return

        viewModelScope.launch {
            if (currentId != null && currentId > 0L) {
                val existing = _editingNote.value?.copy(
                    id = currentId,
                    title = title.trim(),
                    content = content.trim(),
                    category = category,
                    tags = tags.trim(),
                    isPinned = isPinned,
                    updatedAt = System.currentTimeMillis()
                ) ?: NoteEntity(
                    id = currentId,
                    title = title.trim().ifEmpty { "Untitled Note" },
                    content = content.trim(),
                    category = category,
                    tags = tags.trim(),
                    isPinned = isPinned,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateNote(existing)
                _editingNote.value = existing
                onSaved(currentId)
            } else {
                val newNote = NoteEntity(
                    title = title.trim().ifEmpty { "Untitled Note" },
                    content = content.trim(),
                    category = category,
                    tags = tags.trim(),
                    isPinned = isPinned,
                    updatedAt = System.currentTimeMillis()
                )
                val newId = repository.insertNote(newNote)
                val savedNote = newNote.copy(id = newId)
                _editingNote.value = savedNote
                onSaved(newId)
            }
        }
    }

    fun onDeleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
            onCloseDialog()
            try {
                firestoreRepository.deleteNote("note_${note.id}")
            } catch (e: Exception) {
                Log.w(TAG, "Background Firestore delete skipped: ${e.message}")
            }
        }
    }
}
