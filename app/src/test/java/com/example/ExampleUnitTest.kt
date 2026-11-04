package com.example

import com.example.data.local.BackupSerializer
import com.example.data.local.NoteDao
import com.example.data.local.NoteEntity
import com.example.data.local.RecentSearchDao
import com.example.data.local.RecentSearchEntity
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun verify_pinned_notes_sorting_priority() {
        val note1 = NoteEntity(id = 1, title = "A Note", content = "Normal", isPinned = false, updatedAt = 1000L)
        val note2 = NoteEntity(id = 2, title = "B Note", content = "Important", isPinned = true, updatedAt = 500L)
        val note3 = NoteEntity(id = 3, title = "C Note", content = "Recent", isPinned = false, updatedAt = 2000L)

        val list = listOf(note1, note2, note3)
        val sorted = list.sortedWith(
            compareByDescending<NoteEntity> { it.isPinned }
                .thenByDescending { it.updatedAt }
        )

        assertEquals(2L, sorted[0].id)
        assertEquals(3L, sorted[1].id)
        assertEquals(1L, sorted[2].id)
    }

    @Test
    fun verify_text_backup_export_and_import_roundtrip() {
        val notes = listOf(
            NoteEntity(id = 1, title = "Meeting", content = "Discuss roadmap", category = "Work", tags = "Sprint, Dev", isPinned = true),
            NoteEntity(id = 2, title = "Groceries", content = "Apples, Milk", category = "Personal", tags = "Shopping", isPinned = false)
        )

        val txtExport = BackupSerializer.exportToText(notes)
        assertTrue(txtExport.contains("--- NOTE_START ---"))
        assertTrue(txtExport.contains("Pinned: true"))

        val restoredFromTxt = BackupSerializer.parseAuto(txtExport)
        assertEquals(2, restoredFromTxt.size)
        assertEquals("Meeting", restoredFromTxt[0].title)
        assertTrue(restoredFromTxt[0].isPinned)
        assertEquals("Groceries", restoredFromTxt[1].title)
        assertEquals("Apples, Milk", restoredFromTxt[1].content)
    }

    @Test
    fun verify_repository_recent_searches_and_import() = runTest {
        val fakeNotes = MutableStateFlow<List<NoteEntity>>(emptyList())
        val fakeSearches = MutableStateFlow<List<RecentSearchEntity>>(emptyList())

        val noteDao = object : NoteDao {
            override fun getAllNotes(): Flow<List<NoteEntity>> = fakeNotes
            override suspend fun getAllNotesSnapshot(): List<NoteEntity> = fakeNotes.value
            override fun searchNotes(query: String): Flow<List<NoteEntity>> =
                fakeNotes.map { list -> list.filter { it.title.contains(query, true) || it.content.contains(query, true) } }
            override fun getNotesByCategory(category: String): Flow<List<NoteEntity>> =
                fakeNotes.map { list -> list.filter { it.category == category } }
            override suspend fun getNotesCount(): Int = fakeNotes.value.size
            override suspend fun insertNote(note: NoteEntity): Long {
                val id = (fakeNotes.value.size + 1).toLong()
                fakeNotes.value = fakeNotes.value + note.copy(id = id)
                return id
            }
            override suspend fun insertNotes(notes: List<NoteEntity>): List<Long> =
                notes.map { insertNote(it) }
            override suspend fun updateNote(note: NoteEntity) {}
            override suspend fun deleteNote(note: NoteEntity) {}
            override suspend fun deleteNoteById(id: Long) {}
            override suspend fun setPinned(id: Long, isPinned: Boolean) {
                fakeNotes.value = fakeNotes.value.map { if (it.id == id) it.copy(isPinned = isPinned) else it }
            }
        }

        val recentSearchDao = object : RecentSearchDao {
            override fun getRecentSearches(): Flow<List<RecentSearchEntity>> = fakeSearches
            override suspend fun insertRecentSearch(search: RecentSearchEntity) {
                fakeSearches.value = (listOf(search) + fakeSearches.value.filter { it.query != search.query })
            }
            override suspend fun deleteRecentSearch(query: String) {
                fakeSearches.value = fakeSearches.value.filter { it.query != query }
            }
            override suspend fun clearAllRecentSearches() {
                fakeSearches.value = emptyList()
            }
        }

        val repository = NoteRepository(noteDao, recentSearchDao)
        repository.saveRecentSearch("Voice Dictation")
        repository.saveRecentSearch("Pinned Notes")

        val recent = repository.recentSearches.first()
        assertEquals(2, recent.size)
        assertEquals("Pinned Notes", recent[0].query)

        val imported = repository.importNotes(
            listOf(
                NoteEntity(title = "Imported Note 1", content = "Restored from backup", isPinned = true)
            )
        )
        assertEquals(1, imported)
        assertEquals(1, repository.allNotes.first().size)
    }

    @Test
    fun verify_note_title_and_meaningful_body_validation() {
        val emptyTitleResult = NoteEntity.validateInput(title = "   ", body = "Valid note body content")
        assertTrue(!emptyTitleResult.isValid)
        assertTrue(emptyTitleResult.titleError != null)

        val shortBodyResult = NoteEntity.validateInput(title = "Meeting Notes", body = "hi")
        assertTrue(!shortBodyResult.isValid)
        assertTrue(shortBodyResult.bodyError != null)

        val validResult = NoteEntity.validateInput(title = "Project Roadmap", body = "Complete Room DAO and Jetpack Compose UI.")
        assertTrue(validResult.isValid)
        assertEquals(null, validResult.titleError)
        assertEquals(null, validResult.bodyError)
    }
}

