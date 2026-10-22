package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NoteEntity
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
    fun `verify word and character count calculation`() {
        val content = "The quick brown fox jumps over the lazy dog."
        val wordCount = content.trim().split("\\s+".toRegex()).count { it.isNotBlank() }
        val charCount = content.length

        assertEquals(9, wordCount)
        assertEquals(44, charCount)
    }

    @Test
    fun `verify json backup serialization`() {
        val notes = listOf(
            NoteEntity(id = 1, title = "Meeting", content = "Discuss roadmap", category = "Work", tags = "Sprint, Dev", isPinned = true),
            NoteEntity(id = 2, title = "Groceries", content = "Apples, Milk", category = "Personal", tags = "Shopping", isPinned = false)
        )

        val jsonArray = JSONArray()
        notes.forEach { note ->
            val obj = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("category", note.category)
                put("tags", note.tags)
                put("isPinned", note.isPinned)
            }
            jsonArray.put(obj)
        }
        val root = JSONObject().apply {
            put("version", 1)
            put("totalNotes", notes.size)
            put("notes", jsonArray)
        }

        val jsonString = root.toString()
        assertTrue(jsonString.contains("Meeting"))
        assertTrue(jsonString.contains("Groceries"))
        assertTrue(jsonString.contains("Sprint, Dev"))
        assertEquals(2, root.getInt("totalNotes"))
    }
}
