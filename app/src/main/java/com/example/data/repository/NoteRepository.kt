package com.example.data.repository

import com.example.data.local.NoteDao
import com.example.data.local.NoteEntity
import com.example.data.local.RecentSearchDao
import com.example.data.local.RecentSearchEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

class NoteRepository(
    private val noteDao: NoteDao,
    private val recentSearchDao: RecentSearchDao? = null
) {

    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    val recentSearches: Flow<List<RecentSearchEntity>> =
        recentSearchDao?.getRecentSearches() ?: flowOf(emptyList())

    fun searchNotes(query: String): Flow<List<NoteEntity>> {
        return if (query.isBlank()) {
            noteDao.getAllNotes()
        } else {
            noteDao.searchNotes(query)
        }
    }

    fun getNotesByCategory(category: String): Flow<List<NoteEntity>> {
        return if (category == "All") {
            noteDao.getAllNotes()
        } else {
            noteDao.getNotesByCategory(category)
        }
    }

    suspend fun insertNote(note: NoteEntity): Long = withContext(Dispatchers.IO) {
        noteDao.insertNote(note)
    }

    suspend fun importNotes(notes: List<NoteEntity>): Int = withContext(Dispatchers.IO) {
        if (notes.isEmpty()) return@withContext 0
        val existingNotes = noteDao.getAllNotesSnapshot()
        val existingSignatures = existingNotes.map { "${it.title.trim()}|${it.content.trim()}" }.toMutableSet()
        var importedCount = 0
        for (note in notes) {
            val signature = "${note.title.trim()}|${note.content.trim()}"
            if (signature !in existingSignatures) {
                noteDao.insertNote(note.copy(id = 0L))
                existingSignatures.add(signature)
                importedCount++
            }
        }
        importedCount
    }

    suspend fun updateNote(note: NoteEntity) = withContext(Dispatchers.IO) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: NoteEntity) = withContext(Dispatchers.IO) {
        noteDao.deleteNote(note)
    }

    suspend fun deleteNoteById(id: Long) = withContext(Dispatchers.IO) {
        noteDao.deleteNoteById(id)
    }

    suspend fun togglePin(id: Long, isPinned: Boolean) = withContext(Dispatchers.IO) {
        noteDao.setPinned(id, !isPinned)
    }

    suspend fun saveRecentSearch(query: String) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            recentSearchDao?.insertRecentSearch(
                RecentSearchEntity(query = trimmed, searchedAt = System.currentTimeMillis())
            )
        }
    }

    suspend fun deleteRecentSearch(query: String) = withContext(Dispatchers.IO) {
        recentSearchDao?.deleteRecentSearch(query)
    }

    suspend fun clearAllRecentSearches() = withContext(Dispatchers.IO) {
        recentSearchDao?.clearAllRecentSearches()
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        if (noteDao.getNotesCount() == 0) {
            val defaultNotes = listOf(
                NoteEntity(
                    title = "Project Alpha Strategy",
                    content = "Next steps for the Q3 rollout include final validation of the Jetpack Compose components and migration to Room Database for full offline persistence.",
                    category = "Work",
                    tags = "Sprint, Architecture",
                    isPinned = true,
                    updatedAt = System.currentTimeMillis() - 100000
                ),
                NoteEntity(
                    title = "Grocery List",
                    content = "Oat milk, coffee beans (medium roast), sourdough, organic honey, and greek yogurt...",
                    category = "Personal",
                    tags = "Shopping, Weekly",
                    isPinned = false,
                    updatedAt = System.currentTimeMillis() - 200000
                ),
                NoteEntity(
                    title = "Compose & Room Tips",
                    content = "Remember to use rememberSaveable for configuration changes. State hoisting is key for testing. Room DAOs return Flow for reactive updates automatically cached locally.",
                    category = "Ideas",
                    tags = "Dev, Compose",
                    isPinned = false,
                    updatedAt = System.currentTimeMillis() - 300000
                )
            )
            defaultNotes.forEach { noteDao.insertNote(it) }
        }
        // Seed initial recent search examples if empty and notes were just seeded
        if (recentSearchDao != null) {
            val initialSearches = listOf("Compose", "Project Alpha", "Grocery")
            initialSearches.forEachIndexed { idx, term ->
                recentSearchDao.insertRecentSearch(
                    RecentSearchEntity(
                        query = term,
                        searchedAt = System.currentTimeMillis() - (idx * 5000L)
                    )
                )
            }
        }
    }
}
