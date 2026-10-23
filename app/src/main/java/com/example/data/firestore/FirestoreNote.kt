package com.example.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreNote(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "Personal",
    val tags: String = "",
    val isPinned: Boolean = false,
    val userId: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)
