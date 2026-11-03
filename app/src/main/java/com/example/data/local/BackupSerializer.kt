package com.example.data.local

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupSerializer {

    fun exportToJson(notes: List<NoteEntity>): String {
        val jsonArray = JSONArray()
        notes.forEach { note ->
            val obj = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("category", note.category)
                put("tags", note.tags)
                put("isPinned", note.isPinned)
                put("createdAt", note.createdAt)
                put("updatedAt", note.updatedAt)
            }
            jsonArray.put(obj)
        }
        val root = JSONObject().apply {
            put("version", 1)
            put("appName", "SK Nexus Notes")
            put("exportedAt", System.currentTimeMillis())
            put("totalNotes", notes.size)
            put("notes", jsonArray)
        }
        return root.toString(2)
    }

    fun exportToText(notes: List<NoteEntity>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return buildString {
            appendLine("==========================================")
            appendLine("SK NEXUS NOTES - LOCAL TEXT BACKUP")
            appendLine("Exported At: ${dateFormat.format(Date())}")
            appendLine("Total Notes: ${notes.size}")
            appendLine("==========================================")
            appendLine()
            notes.forEachIndexed { index, note ->
                appendLine("--- NOTE_START ---")
                appendLine("Title: ${note.title}")
                appendLine("Category: ${note.category}")
                appendLine("Tags: ${note.tags}")
                appendLine("Pinned: ${note.isPinned}")
                appendLine("CreatedAt: ${note.createdAt}")
                appendLine("UpdatedAt: ${note.updatedAt}")
                appendLine("Content:")
                appendLine(note.content)
                appendLine("--- NOTE_END ---")
                if (index < notes.lastIndex) {
                    appendLine()
                }
            }
        }
    }

    fun parseFromJson(jsonString: String): List<NoteEntity> {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) return emptyList()

        val notesArray = if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            val root = JSONObject(trimmed)
            root.optJSONArray("notes") ?: JSONArray()
        }

        val parsed = mutableListOf<NoteEntity>()
        val now = System.currentTimeMillis()
        for (i in 0 until notesArray.length()) {
            val obj = notesArray.optJSONObject(i) ?: continue
            val title = obj.optString("title", "").trim()
            val content = obj.optString("content", "").trim()
            if (title.isNotEmpty() || content.isNotEmpty()) {
                parsed.add(
                    NoteEntity(
                        id = 0L,
                        title = title.ifEmpty { "Imported Note" },
                        content = content,
                        category = obj.optString("category", "Personal").ifBlank { "Personal" },
                        tags = obj.optString("tags", ""),
                        isPinned = obj.optBoolean("isPinned", false),
                        createdAt = obj.optLong("createdAt", now),
                        updatedAt = obj.optLong("updatedAt", now)
                    )
                )
            }
        }
        return parsed
    }

    fun parseFromText(textString: String): List<NoteEntity> {
        val trimmed = textString.trim()
        if (trimmed.isEmpty()) return emptyList()

        val now = System.currentTimeMillis()
        if (trimmed.contains("--- NOTE_START ---")) {
            val blocks = trimmed.split("--- NOTE_START ---")
            val results = mutableListOf<NoteEntity>()
            for (block in blocks) {
                val endIdx = block.indexOf("--- NOTE_END ---")
                if (endIdx == -1) continue
                val body = block.substring(0, endIdx).trim()
                val lines = body.lines()

                var title = ""
                var category = "Personal"
                var tags = ""
                var isPinned = false
                var createdAt = now
                var updatedAt = now
                val contentBuilder = StringBuilder()
                var inContent = false

                for (line in lines) {
                    if (inContent) {
                        if (contentBuilder.isNotEmpty()) contentBuilder.append("\n")
                        contentBuilder.append(line)
                    } else {
                        when {
                            line.startsWith("Title:") -> title = line.removePrefix("Title:").trim()
                            line.startsWith("Category:") -> category = line.removePrefix("Category:").trim().ifEmpty { "Personal" }
                            line.startsWith("Tags:") -> tags = line.removePrefix("Tags:").trim()
                            line.startsWith("Pinned:") -> isPinned = line.removePrefix("Pinned:").trim().equals("true", ignoreCase = true)
                            line.startsWith("CreatedAt:") -> createdAt = line.removePrefix("CreatedAt:").trim().toLongOrNull() ?: now
                            line.startsWith("UpdatedAt:") -> updatedAt = line.removePrefix("UpdatedAt:").trim().toLongOrNull() ?: now
                            line.startsWith("Content:") -> {
                                inContent = true
                                val inlineContent = line.removePrefix("Content:").trimStart()
                                if (inlineContent.isNotEmpty()) {
                                    contentBuilder.append(inlineContent)
                                }
                            }
                        }
                    }
                }

                val finalContent = contentBuilder.toString().trim()
                if (title.isNotEmpty() || finalContent.isNotEmpty()) {
                    results.add(
                        NoteEntity(
                            id = 0L,
                            title = title.ifEmpty { "Imported Note" },
                            content = finalContent,
                            category = category,
                            tags = tags,
                            isPinned = isPinned,
                            createdAt = createdAt,
                            updatedAt = updatedAt
                        )
                    )
                }
            }
            return results
        } else {
            // Plain unstructured text file -> import as a single note
            val lines = trimmed.lines()
            val firstLine = lines.firstOrNull()?.trim().orEmpty()
            val restContent = if (lines.size > 1) lines.drop(1).joinToString("\n").trim() else trimmed
            return listOf(
                NoteEntity(
                    id = 0L,
                    title = firstLine.take(60).ifEmpty { "Imported Text Note" },
                    content = restContent,
                    category = "Personal",
                    tags = "Imported",
                    isPinned = false,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun parseAuto(rawContent: String): List<NoteEntity> {
        val trimmed = rawContent.trim()
        if (trimmed.isEmpty()) return emptyList()
        return if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                parseFromJson(trimmed)
            } catch (e: Exception) {
                parseFromText(trimmed)
            }
        } else {
            parseFromText(trimmed)
        }
    }
}
