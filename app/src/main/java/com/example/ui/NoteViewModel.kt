package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.NoteEntity
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NoteSortOrder(val displayName: String) {
    MODIFIED_DATE("Modified Date"),
    CREATED_DATE("Created Date"),
    ALPHABETICAL("Alphabetical")
}

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository

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

    private val _editingNote = MutableStateFlow<NoteEntity?>(null)
    val editingNote: StateFlow<NoteEntity?> = _editingNote.asStateFlow()

    val allNotesList: StateFlow<List<NoteEntity>>

    val allTagsList: StateFlow<List<String>>

    @OptIn(ExperimentalCoroutinesApi::class)
    val notesState: StateFlow<List<NoteEntity>>

    init {
        val noteDao = AppDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(noteDao)

        viewModelScope.launch(Dispatchers.IO) {
            repository.seedInitialDataIfEmpty()
        }

        allNotesList = repository.allNotes.stateIn(
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

    fun onOpenAddNote() {
        _editingNote.value = null
        _isAddEditOpen.value = true
    }

    fun onOpenEditNote(note: NoteEntity) {
        _editingNote.value = note
        _isAddEditOpen.value = true
    }

    fun onCloseDialog() {
        _isAddEditOpen.value = false
        _editingNote.value = null
    }

    fun onSaveNote(title: String, content: String, category: String, tags: String, isPinned: Boolean) {
        if (title.isBlank() && content.isBlank()) return

        viewModelScope.launch {
            val currentEditing = _editingNote.value
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
            } else {
                val newNote = NoteEntity(
                    title = title.trim().ifEmpty { "Untitled Note" },
                    content = content.trim(),
                    category = category,
                    tags = tags.trim(),
                    isPinned = isPinned,
                    updatedAt = System.currentTimeMillis()
                )
                repository.insertNote(newNote)
            }
            onCloseDialog()
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
        }
    }
}
