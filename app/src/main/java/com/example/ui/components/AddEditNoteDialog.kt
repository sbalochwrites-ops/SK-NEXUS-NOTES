package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.NoteEntity
import com.example.ui.AiAssistState
import com.example.ui.theme.DarkOnPrimary
import com.example.ui.theme.DarkOnPrimaryContainer
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkPrimary
import com.example.ui.theme.DarkPrimaryContainer
import kotlinx.coroutines.delay
import java.util.Locale

val EDITABLE_CATEGORIES = listOf("Personal", "Work", "Ideas", "Study", "General")
val SUGGESTED_TAGS = listOf("Important", "Urgent", "Meeting", "Sprint", "Design", "Dev", "Review", "Shopping")

enum class VoiceDictationTarget {
    TITLE,
    CONTENT
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditNoteDialog(
    note: NoteEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, category: String, tags: String, isPinned: Boolean) -> Unit,
    onDelete: (NoteEntity) -> Unit,
    aiAssistState: AiAssistState = AiAssistState.Idle,
    onEnhanceWithGemini: (
        title: String,
        content: String,
        mode: String,
        onResult: (title: String, content: String, category: String, tags: List<String>) -> Unit
    ) -> Unit = { _, _, _, _ -> },
    onAutoSave: (id: Long?, title: String, content: String, category: String, tags: String, isPinned: Boolean, onSaved: (Long) -> Unit) -> Unit = { _, _, _, _, _, _, _ -> },
    startVoiceOnLaunch: Boolean = false
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
    var hasAttemptedSave by remember(note) { mutableStateOf(false) }

    val validationResult = remember(title, content) {
        NoteEntity.validateInput(title = title, body = content)
    }
    val showTitleError = hasAttemptedSave && validationResult.titleError != null
    val showBodyError = hasAttemptedSave && validationResult.bodyError != null

    BackHandler(onBack = onDismiss)

    // Speech-to-Text state
    var voiceTarget by remember { mutableStateOf(VoiceDictationTarget.CONTENT) }
    var isListeningLive by remember { mutableStateOf(false) }
    var livePartialSpeech by remember { mutableStateOf("") }
    var speechRecognizerRef by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val appendSpokenText: (String) -> Unit = { spokenText ->
        val clean = spokenText.trim()
        if (clean.isNotEmpty()) {
            if (voiceTarget == VoiceDictationTarget.TITLE) {
                title = if (title.isBlank()) clean else "$title $clean"
            } else {
                content = if (content.isBlank()) clean else "$content $clean"
            }
            Toast.makeText(context, "Dictated: \"$clean\"", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizerRef?.destroy()
            } catch (_: Exception) {
            }
            speechRecognizerRef = null
        }
    }

    // Dynamic Word and Character Count calculation
    val wordCount = remember(content) {
        if (content.isBlank()) 0
        else content.trim().split("\\s+".toRegex()).count { it.isNotBlank() }
    }
    val charCount = remember(content) { content.length }

    // Fallback Intent Speech-to-Text Recognition Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListeningLive = false
        livePartialSpeech = ""
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenList?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                appendSpokenText(spokenText)
            }
        }
    }

    val launchSpeechIntentFallback = {
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.voice_input_listening))
        }
        try {
            speechLauncher.launch(speechIntent)
        } catch (e: Exception) {
            isListeningLive = false
            Toast.makeText(context, "Voice recognition service unavailable on this device", Toast.LENGTH_SHORT).show()
        }
    }

    val startAndroidSpeechRecognition = {
        if (isListeningLive) {
            try {
                speechRecognizerRef?.stopListening()
            } catch (_: Exception) {
            }
            isListeningLive = false
            livePartialSpeech = ""
        } else if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                val recognizer = speechRecognizerRef ?: SpeechRecognizer.createSpeechRecognizer(context).also {
                    speechRecognizerRef = it
                }
                val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListeningLive = true
                        livePartialSpeech = "Listening... speak now"
                    }

                    override fun onBeginningOfSpeech() {
                        livePartialSpeech = "Capturing voice..."
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListeningLive = false
                    }

                    override fun onError(error: Int) {
                        isListeningLive = false
                        livePartialSpeech = ""
                        // Fallback to system speech dialog if direct SpeechRecognizer service fails in emulator
                        launchSpeechIntentFallback()
                    }

                    override fun onResults(results: Bundle?) {
                        isListeningLive = false
                        livePartialSpeech = ""
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val bestMatch = matches?.firstOrNull()
                        if (!bestMatch.isNullOrBlank()) {
                            appendSpokenText(bestMatch)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                        if (!partial.isNullOrBlank()) {
                            livePartialSpeech = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                isListeningLive = true
                livePartialSpeech = "Initializing microphone..."
                recognizer.startListening(recognizerIntent)
            } catch (e: Exception) {
                isListeningLive = false
                livePartialSpeech = ""
                launchSpeechIntentFallback()
            }
        } else {
            launchSpeechIntentFallback()
        }
    }

    // Microphone Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startAndroidSpeechRecognition()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice transcription", Toast.LENGTH_SHORT).show()
        }
    }

    val triggerVoiceTranscription = {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startAndroidSpeechRecognition()
        } else {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(startVoiceOnLaunch) {
        if (startVoiceOnLaunch) {
            delay(250L)
            triggerVoiceTranscription()
        }
    }

    // Auto-save debounced effect (only runs when both title and body pass validation)
    LaunchedEffect(title, content, category, tagsList, isPinned) {
        val joinedTags = tagsList.joinToString(", ")
        val isInitialState = note != null &&
                title == note.title &&
                content == note.content &&
                category == note.category &&
                joinedTags == note.tags &&
                isPinned == note.isPinned

        if (!isInitialState && NoteEntity.validateInput(title, content).isValid) {
            autoSaveStatus = "saving"
            delay(1200)
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag("note_composition_screen"),
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
                // Header Row with Back Navigation, Title, Auto-save Indicator & Actions (Mic, Share, Delete)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("navigate_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Notes",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = stringResource(id = if (note == null && currentNoteId == null) R.string.add_note else R.string.edit_note),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )

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
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { triggerVoiceTranscription() },
                            modifier = Modifier.testTag("voice_input_button")
                        ) {
                            Icon(
                                imageVector = if (isListeningLive) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = stringResource(id = R.string.voice_input),
                                tint = if (isListeningLive) MaterialTheme.colorScheme.error else DarkPrimary
                            )
                        }

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

                // Live Speech-to-Text Status Banner
                AnimatedVisibility(visible = isListeningLive || livePartialSpeech.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkPrimaryContainer,
                        border = BorderStroke(1.dp, DarkPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .testTag("live_speech_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = DarkPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Voice Dictation (${if (voiceTarget == VoiceDictationTarget.TITLE) "Title" else "Content"})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkPrimary
                                    )
                                    Text(
                                        text = livePartialSpeech.ifBlank { "Speak now..." },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DarkOnPrimaryContainer
                                    )
                                }
                            }
                            TextButton(
                                onClick = {
                                    try {
                                        speechRecognizerRef?.stopListening()
                                    } catch (_: Exception) {
                                    }
                                    isListeningLive = false
                                    livePartialSpeech = ""
                                }
                            ) {
                                Text("Stop", color = DarkPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title Input Field with non-empty validation check
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(id = R.string.title)) },
                    placeholder = { Text("Enter a descriptive note title...") },
                    singleLine = true,
                    isError = showTitleError,
                    supportingText = if (showTitleError) {
                        {
                            Text(
                                text = validationResult.titleError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.testTag("title_validation_error")
                            )
                        }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkPrimary,
                        unfocusedBorderColor = DarkOutline,
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        focusedLabelColor = DarkPrimary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) voiceTarget = VoiceDictationTarget.TITLE }
                        .testTag("note_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Text Formatting Toolbar
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, DarkOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = {
                                content = if (content.isEmpty()) "**Bold text**" else "$content **bold**"
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("format_bold_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatBold,
                                contentDescription = "Bold",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                content = if (content.isEmpty()) "*Italic text*" else "$content *italic*"
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("format_italic_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatItalic,
                                contentDescription = "Italic",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val prefix = if (content.isEmpty() || content.endsWith("\n")) "• " else "\n• "
                                content += prefix
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("format_bullet_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                contentDescription = "Bullet List",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val prefix = if (content.isEmpty() || content.endsWith("\n")) "- [ ] " else "\n- [ ] "
                                content += prefix
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("format_checklist_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckBox,
                                contentDescription = "Checklist Item",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val prefix = if (content.isEmpty() || content.endsWith("\n")) "## " else "\n## "
                                content += prefix
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("format_heading_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Title,
                                contentDescription = "Heading",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Content / Body Input Field with meaningful content validation check
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(id = R.string.content)) },
                    placeholder = { Text("Write your note body here (at least 3 characters)...") },
                    minLines = 4,
                    maxLines = 8,
                    isError = showBodyError,
                    supportingText = if (showBodyError) {
                        {
                            Text(
                                text = validationResult.bodyError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.testTag("body_validation_error")
                            )
                        }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkPrimary,
                        unfocusedBorderColor = DarkOutline,
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        focusedLabelColor = DarkPrimary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) voiceTarget = VoiceDictationTarget.CONTENT }
                        .testTag("note_content_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Gemini AI Smart Writing Assistant Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            onEnhanceWithGemini(title, content, "enhance") { newTitle, newContent, newCat, newTags ->
                                title = newTitle
                                content = newContent
                                if (EDITABLE_CATEGORIES.contains(newCat)) {
                                    category = newCat
                                }
                                val merged = (tagsList + newTags).distinct()
                                tagsList = merged
                            }
                        },
                        enabled = aiAssistState !is AiAssistState.Loading,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gemini_enhance_button")
                    ) {
                        if (aiAssistState is AiAssistState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = DarkPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = DarkPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Enhance",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            onEnhanceWithGemini(title, content, "summarize") { newTitle, newContent, newCat, newTags ->
                                title = newTitle
                                content = newContent
                                if (EDITABLE_CATEGORIES.contains(newCat)) {
                                    category = newCat
                                }
                                val merged = (tagsList + newTags).distinct()
                                tagsList = merged
                            }
                        },
                        enabled = aiAssistState !is AiAssistState.Loading,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gemini_summarize_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Summarize,
                            contentDescription = null,
                            tint = DarkPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Summarize",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                when (aiAssistState) {
                    is AiAssistState.Success -> {
                        Text(
                            text = aiAssistState.message,
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkPrimary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    is AiAssistState.Error -> {
                        Text(
                            text = aiAssistState.error,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Dynamic Word and Character Count Indicator + Voice Input Controls
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = voiceTarget == VoiceDictationTarget.TITLE,
                            onClick = {
                                voiceTarget = if (voiceTarget == VoiceDictationTarget.TITLE) {
                                    VoiceDictationTarget.CONTENT
                                } else {
                                    VoiceDictationTarget.TITLE
                                }
                            },
                            label = {
                                Text(
                                    text = if (voiceTarget == VoiceDictationTarget.TITLE) "Mic: Title" else "Mic: Body",
                                    fontSize = 10.sp
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("voice_target_chip")
                        )

                        AssistChip(
                            onClick = { triggerVoiceTranscription() },
                            label = {
                                Text(
                                    text = if (isListeningLive) "Stop Mic" else stringResource(id = R.string.voice_input),
                                    fontSize = 11.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isListeningLive) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (isListeningLive) MaterialTheme.colorScheme.error else DarkPrimary,
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
                            imageVector = Icons.AutoMirrored.Filled.Label,
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

                // Pin Note Toggle
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

                // Validation Error Summary Banner (shown if user tries to save with invalid title or body)
                AnimatedVisibility(visible = hasAttemptedSave && !validationResult.isValid) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .testTag("validation_error_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = validationResult.titleError ?: validationResult.bodyError ?: "Please check your note fields.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Buttons (Cancel & Save / Update Note)
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
                            hasAttemptedSave = true
                            val check = NoteEntity.validateInput(title = title, body = content)
                            if (!check.isValid) {
                                val errorMsg = check.titleError ?: check.bodyError ?: "Invalid note input"
                                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                            } else {
                                val joinedTags = tagsList.joinToString(", ")
                                onSave(title.trim(), content.trim(), category, joinedTags, isPinned)
                            }
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
