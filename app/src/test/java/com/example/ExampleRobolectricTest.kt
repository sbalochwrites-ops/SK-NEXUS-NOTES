package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.BackupSerializer
import com.example.data.local.NoteEntity
import com.example.data.local.RecentSearchEntity
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SK Nexus Notes", appName)
    }

    @Test
    fun `verify pinned notes sorting priority`() {
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
    fun `verify json and text backup export and import roundtrip`() {
        val notes = listOf(
            NoteEntity(id = 1, title = "Meeting", content = "Discuss roadmap", category = "Work", tags = "Sprint, Dev", isPinned = true),
            NoteEntity(id = 2, title = "Groceries", content = "Apples, Milk", category = "Personal", tags = "Shopping", isPinned = false)
        )

        val jsonExport = BackupSerializer.exportToJson(notes)
        assertTrue(jsonExport.contains("Meeting"))
        val restoredFromJson = BackupSerializer.parseAuto(jsonExport)
        assertEquals(2, restoredFromJson.size)
        assertEquals("Meeting", restoredFromJson[0].title)
        assertTrue(restoredFromJson[0].isPinned)

        val txtExport = BackupSerializer.exportToText(notes)
        assertTrue(txtExport.contains("--- NOTE_START ---"))
        val restoredFromTxt = BackupSerializer.parseAuto(txtExport)
        assertEquals(2, restoredFromTxt.size)
        assertEquals("Groceries", restoredFromTxt[1].title)
        assertEquals("Apples, Milk", restoredFromTxt[1].content)
    }

    @Test
    fun `verify recent searches and note import in Room database`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val repository = NoteRepository(db.noteDao(), db.recentSearchDao())

        repository.saveRecentSearch("Kotlin Coroutines")
        repository.saveRecentSearch("Jetpack Compose")

        val searches = repository.recentSearches.first()
        assertEquals(2, searches.size)
        assertEquals("Jetpack Compose", searches[0].query)

        val backupNotes = listOf(
            NoteEntity(title = "Restored 1", content = "Body 1", isPinned = true),
            NoteEntity(title = "Restored 2", content = "Body 2", isPinned = false)
        )
        val importedCount = repository.importNotes(backupNotes)
        assertEquals(2, importedCount)

        val allNotes = repository.allNotes.first()
        assertEquals(2, allNotes.size)
        assertTrue(allNotes.first().isPinned)

        db.close()
    }
}
