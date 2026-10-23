package com.example.data.firestore

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreNoteRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    private fun userNotesCollection(userId: String) =
        db.collection("users").document(userId).collection("notes")

    fun observeNotes(): Flow<List<FirestoreNote>> {
        val uid = requireUserId()
        val path = "users/$uid/notes"
        return userNotesCollection(uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FirestoreNote::class.java)
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun getNoteById(noteId: String): FirestoreNote? {
        val uid = requireUserId()
        val path = "users/$uid/notes/$noteId"
        return try {
            val doc = userNotesCollection(uid).document(noteId).get().await()
            doc.toObject(FirestoreNote::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    suspend fun createNote(
        title: String,
        content: String,
        category: String = "Personal",
        tags: String = "",
        isPinned: Boolean = false,
        customId: String? = null
    ): String {
        val uid = requireUserId()
        val col = userNotesCollection(uid)
        val docRef = if (customId != null) col.document(customId) else col.document()
        val path = docRef.path

        val payload = hashMapOf<String, Any>(
            "title" to title,
            "content" to content,
            "category" to category,
            "tags" to tags,
            "isPinned" to isPinned,
            "userId" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    suspend fun updateNote(
        noteId: String,
        title: String,
        content: String,
        category: String,
        tags: String,
        isPinned: Boolean
    ) {
        val uid = requireUserId()
        val docRef = userNotesCollection(uid).document(noteId)
        val path = docRef.path

        val updates = hashMapOf<String, Any>(
            "title" to title,
            "content" to content,
            "category" to category,
            "tags" to tags,
            "isPinned" to isPinned,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.update(updates).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    suspend fun togglePin(noteId: String, currentPinned: Boolean) {
        val uid = requireUserId()
        val docRef = userNotesCollection(uid).document(noteId)
        val path = docRef.path

        val updates = hashMapOf<String, Any>(
            "isPinned" to !currentPinned,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.update(updates).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    suspend fun deleteNote(noteId: String) {
        val uid = requireUserId()
        val docRef = userNotesCollection(uid).document(noteId)
        val path = docRef.path

        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }
}
