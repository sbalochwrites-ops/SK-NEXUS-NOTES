package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

data class NoteValidationResult(
    val isValid: Boolean,
    val titleError: String? = null,
    val bodyError: String? = null
)

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["category"]),
        Index(value = ["isPinned", "updatedAt"])
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "category", defaultValue = "Personal")
    val category: String = "Personal",

    @ColumnInfo(name = "tags", defaultValue = "")
    val tags: String = "",

    @ColumnInfo(name = "isPinned", defaultValue = "0")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Convenience property alias for the note's body content.
     */
    val body: String
        get() = content

    companion object {
        private const val MIN_MEANINGFUL_BODY_LENGTH = 3

        /**
         * Validates that the note title is not empty and the body contains meaningful content.
         */
        fun validateInput(title: String, body: String): NoteValidationResult {
            val trimmedTitle = title.trim()
            val trimmedBody = body.trim()

            val titleError = when {
                trimmedTitle.isEmpty() -> "Title cannot be empty. Please enter a note title."
                else -> null
            }

            val bodyError = when {
                trimmedBody.isEmpty() -> "Note body cannot be empty."
                trimmedBody.length < MIN_MEANINGFUL_BODY_LENGTH ->
                    "Note body must contain meaningful content (at least $MIN_MEANINGFUL_BODY_LENGTH characters)."
                else -> null
            }

            return NoteValidationResult(
                isValid = titleError == null && bodyError == null,
                titleError = titleError,
                bodyError = bodyError
            )
        }
    }
}

