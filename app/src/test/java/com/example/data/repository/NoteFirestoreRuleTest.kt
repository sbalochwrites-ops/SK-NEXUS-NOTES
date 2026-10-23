package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.firestore.FirestoreNoteRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class NoteFirestoreRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createNote_authenticatedUser_success(): Unit {
        runBlocking {
            signInTestUser(ALICE_EMAIL)
            val repository = FirestoreNoteRepository(firestore)

            val noteId = withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.createNote(
                    title = "Alice Note",
                    content = "Alice test content",
                    category = "Work",
                    tags = "Sprint, Review",
                    isPinned = true
                )
            }

            assertTrue(noteId.isNotEmpty())
        }
    }

    @Test
    fun getNoteById_authenticatedOwner_returnsNote(): Unit {
        runBlocking {
            signInTestUser(ALICE_EMAIL)
            val repository = FirestoreNoteRepository(firestore)

            val customId = "note_${UUID.randomUUID().toString().take(8)}"
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.createNote(
                    title = "Fetchable Note",
                    content = "Important details",
                    category = "Personal",
                    tags = "Test",
                    isPinned = false,
                    customId = customId
                )
            }

            val note = withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.getNoteById(customId)
            }

            assertNotNull(note)
            assertEquals("Fetchable Note", note?.title)
        }
    }

    @Test
    fun observeNotes_authenticatedOwner_emitsNotes(): Unit {
        runBlocking {
            signInTestUser(ALICE_EMAIL)
            val repository = FirestoreNoteRepository(firestore)

            val customId = "note_${UUID.randomUUID().toString().take(8)}"
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.createNote(
                    title = "Observable Note",
                    content = "Stream test",
                    category = "Ideas",
                    isPinned = false,
                    customId = customId
                )
            }

            val list = withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeNotes().first { notes ->
                    notes.any { it.id == customId }
                }
            }

            assertTrue(list.any { it.id == customId })
        }
    }

    @Test
    fun crossUserAccess_bobCannotReadAliceNote(): Unit {
        runBlocking {
            val aliceUid = signInTestUser(ALICE_EMAIL)
            val aliceRepo = FirestoreNoteRepository(firestore)

            val customId = "note_${UUID.randomUUID().toString().take(8)}"
            withTimeout(DEFAULT_TIMEOUT_MS) {
                aliceRepo.createNote(
                    title = "Alice Secret",
                    content = "Top secret",
                    category = "Work",
                    customId = customId
                )
            }

            signInTestUser(BOB_EMAIL)
            try {
                withTimeout(DEFAULT_TIMEOUT_MS) {
                    firestore.collection("users").document(aliceUid).collection("notes").document(customId).get().await()
                }
            } catch (e: Exception) {
                val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                    "Expected FirebaseFirestoreException but got $e"
                }
                assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
            }
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@example.com"
        const val BOB_EMAIL = "bob@example.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 6000L
    }
}
