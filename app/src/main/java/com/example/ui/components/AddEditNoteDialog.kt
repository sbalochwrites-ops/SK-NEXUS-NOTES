package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.NoteEntity
import com.example.ui.theme.DarkOnPrimary
import com.example.ui.theme.DarkOnPrimaryContainer
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkPrimary
import com.example.ui.theme.DarkPrimaryContainer
import kotlinx.coroutines.delay

val EDITABLE_CATEGORIES = listOf("Personal", "Work", "Ideas", "Study", "General")
val SUGGESTED_TAGS = listOf("Important", "Urgent", "Meeting", "Sprint", "Design", "Dev", "Review", "Shopping")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditNoteDialog(
    note: NoteEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, category: String, tags: String, isPinned: Boolean) -> Unit,
    onDelete: (NoteEntity) -> Unit,
    onAutoSave: (id: Long?, title: String, content: String, category: String, tags: String, isPinned: Boolean, onSaved: (Long) -> Unit) -> Unit = { _, _, _, _, _, _, _ -> }
) {
    val context = LocalContext.current

    var currentNoteId by remember(note) { mutableStateOf(note?.id) }
    var title by remember(note) { mutableStateOf(note?.title ?: "") }
    var content by remember(note) { mutableStateOf(note?.content ?: "") }
    var category by remember(note) { mutableStateOf(note?.category ?: "Personal") }
    var isPinned by remember(note) { mutableStateOf(note?.isPinned ?: false) }
    var tagInput by remember { mutableStateOf("") }
    var tagsList by remember(note) {
        mutableStateOf(
            note?.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        )
    }

    var autoSaveStatus by remember { mutableStateOf<String?>(null) } // "saving", "saved", null
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Dynamic Word and Character Count calculation
    val wordCount = remember(content) {
        if (content.isBlank()) 0
        else content.trim().split("\\s+".toRegex()).count { it.isNotBlank() }
    }
    val charCount = remember(content) { content.length }

    // Speech-to-Text Recognition Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenList?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                content = if (content.isBlank()) spokenText else "$content $spokenText"
                Toast.makeText(context, "Transcribed: \"$spokenText\"", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Microphone Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.voice_input_listening))
            }
            try {
                speechLauncher.launch(speechIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Voice recognition service unavailable", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission is required for voice transcription", Toast.LENGTH_SHORT).show()
        }
    }

    val triggerVoiceTranscription = {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.voice_input_listening))
            }
            try {
                speechLauncher.launch(speechIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Voice recognition service unavailable", Toast.LENGTH_SHORT).show()
            }
        } else {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    // Auto-save debounced effect
    LaunchedEffect(title, content, category, tagsList, isPinned) {
        val joinedTags = tagsList.joinToString(", ")
        val isInitialState = note != null &&
                title == note.title &&
                content == note.content &&
                category == note.category &&
                joinedTags == note.tags &&
                isPinned == note.isPinned

        if (!isInitialState && (title.isNotBlank() || content.isNotBlank())) {
            autoSaveStatus = "saving"
            delay(1200) // Debounce period while user is typing
            onAutoSave(
                currentNoteId,
                title,
                content,
                category,
                joinedTags,
                isPinned
            ) { newId ->
                currentNoteId = newId
                autoSaveStatus = "saved"
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            border = BorderStroke(1.dp, DarkOutline)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row with Title, Auto-save Indicator & Actions (Mic, Share, Delete)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = if (note == null && currentNoteId == null) R.string.add_note else R.string.edit_note),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )

                        // Auto-Save status badge
                        if (autoSaveStatus != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (autoSaveStatus == "saved") Icons.Default.Check else Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = if (autoSaveStatus == "saved") DarkPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (autoSaveStatus == "saved") stringResource(id = R.string.auto_saved) else stringResource(id = R.string.saving),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (autoSaveStatus == "saved") DarkPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Voice-to-Text Transcription Button
                        IconButton(
                            onClick = { triggerVoiceTranscription() },
                            modifier = Modifier.testTag("voice_input_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = stringResource(id = R.string.voice_input),
                                tint = DarkPrimary
                            )
                        }

                        // Share Action Button
                        IconButton(
                            onClick = {
                                val joinedTags = tagsList.joinToString(", ")
                                val shareText = buildString {
                                    if (title.isNotBlank()) appendLine(title.trim())
                                    if (content.isNotBlank()) {
                                        if (title.isNotBlank()) appendLine()
                                        appendLine(content.trim())
                                    }
                                    if (category.isNotBlank()) appendLine("\nCategory: $category")
                                    if (joinedTags.isNotBlank()) appendLine("Tags: $joinedTags")
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TITLE, title.ifBlank { "Note" })
                                    putExtra(Intent.EXTRA_SUBJECT, title.ifBlank { "Note" })
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                val chooser = Intent.createChooser(sendIntent, context.getString(R.string.share_via))
                                context.startActivity(chooser)
                            },
                            modifier = Modifier.testTag("share_note_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(id = R.string.share_note),
                                tint = DarkPrimary
                            )
                        }

                        if (note != null || currentNoteId != null) {
                            IconButton(
                                onClick = { showDeleteConfirmation = true },
                                modifier = Modifier.testTag("delete_note_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(id = R.string.delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title Input Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(id = R.string.title)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkPrimary,
                        unfocusedBorderColor = DarkOutline,
                        focusedLabelColor = DarkPrimary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Content Input Field with Voice input shortcut
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(id = R.string.content)) },
                    minLines = 4,
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkPrimary,
                        unfocusedBorderColor = DarkOutline,
                        focusedLabelColor = DarkPrimary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_content_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Dynamic Word and Character Count Indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, DarkOutline)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("word_count_indicator")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = DarkPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(id = R.string.word_count, wordCount, charCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Quick Voice Dictation Assist Chip
                    AssistChip(
                        onClick = { triggerVoiceTranscription() },
                        label = { Text(stringResource(id = R.string.voice_input), fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = DarkPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = DarkPrimaryContainer,
                            labelColor = DarkOnPrimaryContainer
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = DarkOutline
                        ),
                        modifier = Modifier.testTag("dictate_chip_button")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category Selection
                Text(
                    text = stringResource(id = R.string.category),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EDITABLE_CATEGORIES.forEach { cat ->
                        val isSel = cat == category
                        FilterChip(
                            selected = isSel,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DarkPrimaryContainer,
                                selectedLabelColor = DarkOnPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSel,
                                borderColor = DarkOutline,
                                selectedBorderColor = DarkOutline
                            ),
                            modifier = Modifier.testTag("dialog_category_$cat")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tagging System Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = null,
                            tint = DarkPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.tags),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Custom Tag Input Field + Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        placeholder = { Text(stringResource(id = R.string.enter_tag_hint), fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            val trimmed = tagInput.trim().removePrefix("#")
                            if (trimmed.isNotBlank() && !tagsList.contains(trimmed)) {
                                tagsList = tagsList + trimmed
                                tagInput = ""
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkPrimary,
                            unfocusedBorderColor = DarkOutline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tag_input_field")
                    )

                    Button(
                        onClick = {
                            val trimmed = tagInput.trim().removePrefix("#")
                            if (trimmed.isNotBlank() && !tagsList.contains(trimmed)) {
                                tagsList = tagsList + trimmed
                                tagInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkPrimaryContainer,
                            contentColor = DarkOnPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_tag_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(id = R.string.add_tag),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Active Tags Chips
                if (tagsList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tagsList.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkPrimaryContainer,
                                border = BorderStroke(1.dp, DarkOutline),
                                modifier = Modifier.testTag("active_tag_$tag")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DarkOnPrimaryContainer,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                    IconButton(
                                        onClick = { tagsList = tagsList - tag },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove tag $tag",
                                            tint = DarkOnPrimaryContainer,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Suggested Tags Quick-Add Row
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SUGGESTED_TAGS.filter { !tagsList.contains(it) }.forEach { suggested ->
                        AssistChip(
                            onClick = {
                                if (!tagsList.contains(suggested)) {
                                    tagsList = tagsList + suggested
                                }
                            },
                            label = { Text("+$suggested", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                enabled = true,
                                borderColor = DarkOutline
                            ),
                            modifier = Modifier.testTag("suggested_tag_$suggested")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pin Note Toggle (Pin feature to keep notes at top)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = if (isPinned) DarkPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.pinned),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isPinned) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Switch(
                        checked = isPinned,
                        onCheckedChange = { isPinned = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkOnPrimary,
                            checkedTrackColor = DarkPrimary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = DarkOutline
                        ),
                        modifier = Modifier.testTag("note_pin_switch")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Buttons (Cancel & Save)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_note_button")
                    ) {
                        Text(
                            text = stringResource(id = R.string.cancel),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val joinedTags = tagsList.joinToString(", ")
                            onSave(title, content, category, joinedTags, isPinned)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkPrimary,
                            contentColor = DarkOnPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_note_button")
                    ) {
                        Text(
                            text = stringResource(id = R.string.save),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation && (note != null || currentNoteId != null)) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = stringResource(id = R.string.delete_confirmation_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.delete_confirmation_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        val noteToDelete = note ?: NoteEntity(
                            id = currentNoteId ?: 0L,
                            title = title,
                            content = content,
                            category = category,
                            tags = tagsList.joinToString(", "),
                            isPinned = isPinned
                        )
                        onDelete(noteToDelete)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(
                        text = stringResource(id = R.string.delete),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false },
                    modifier = Modifier.testTag("dismiss_delete_button")
                ) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.testTag("delete_confirmation_dialog")
        )
    }
}
