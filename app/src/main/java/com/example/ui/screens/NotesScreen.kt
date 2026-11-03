package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.BackupSerializer
import com.example.data.local.NoteEntity
import com.example.ui.NoteViewModel
import com.example.ui.components.AddEditNoteDialog
import com.example.ui.components.CategoryFilterChips
import com.example.ui.components.NexusBottomNavBar
import com.example.ui.components.NexusHeader
import com.example.ui.components.NoteCard
import com.example.ui.components.NotesDashboardCard
import com.example.ui.components.SearchBarComponent
import com.example.ui.components.SortMenuComponent
import com.example.ui.components.TagFilterChips
import com.example.ui.theme.DarkOnPrimary
import com.example.ui.theme.DarkOnPrimaryContainer
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkPrimary
import com.example.ui.theme.DarkPrimaryContainer

val AUTO_LOCK_TIMEOUT_OPTIONS = listOf(
    0L to "Immediately",
    15_000L to "15 sec",
    60_000L to "1 min",
    300_000L to "5 min",
    600_000L to "10 min"
)

@Composable
fun NotesScreen(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier,
    userEmail: String? = null,
    onSignOut: () -> Unit = {},
    isDarkTheme: Boolean = true,
    onToggleDarkTheme: (Boolean) -> Unit = {},
    themeMode: String = "system",
    onThemeModeChange: (String) -> Unit = {},
    isDynamicColor: Boolean = false,
    onToggleDynamicColor: (Boolean) -> Unit = {},
    isBiometricLockEnabled: Boolean = false,
    onToggleBiometricLock: (Boolean) -> Unit = {},
    lockTimeoutMs: Long = 15_000L,
    onUpdateLockTimeout: (Long) -> Unit = {},
    savedPin: String = "1234",
    onUpdatePin: (String) -> Unit = {},
    onLockAppNow: () -> Unit = {},
    notificationsEnabled: Boolean = true,
    onToggleNotifications: (Boolean) -> Unit = {},
    dailyReminderEnabled: Boolean = true,
    onToggleDailyReminder: (Boolean) -> Unit = {},
    reminderTime: String = "09:00 AM",
    onUpdateReminderTime: (String) -> Unit = {},
    pinnedNotesAlertEnabled: Boolean = true,
    onTogglePinnedNotesAlert: (Boolean) -> Unit = {},
    syncAlertsEnabled: Boolean = true,
    onToggleSyncAlerts: (Boolean) -> Unit = {},
    soundVibrateEnabled: Boolean = true,
    onToggleSoundVibrate: (Boolean) -> Unit = {}
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val allTagsList by viewModel.allTagsList.collectAsState()
    val allNotesList by viewModel.allNotesList.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val isAddEditOpen by viewModel.isAddEditOpen.collectAsState()
    val startVoiceOnOpen by viewModel.startVoiceOnOpen.collectAsState()
    val editingNote by viewModel.editingNote.collectAsState()
    val notesList by viewModel.notesState.collectAsState()
    val aiAssistState by viewModel.aiAssistState.collectAsState()
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsState()
    val backupOperationStatus by viewModel.backupOperationStatus.collectAsState()

    val pinnedNotes = remember(notesList) { notesList.filter { it.isPinned } }
    val unpinnedNotes = remember(notesList) { notesList.filter { !it.isPinned } }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NexusBottomNavBar(
                selectedTab = activeTab,
                onTabSelected = { viewModel.onTabSelected(it) }
            )
        },
        floatingActionButton = {
            if (activeTab != 3) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = { viewModel.onOpenAddNote(startVoiceImmediately = true) },
                        containerColor = DarkPrimaryContainer,
                        contentColor = DarkOnPrimaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("voice_note_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Create Voice Note",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    FloatingActionButton(
                        onClick = { viewModel.onOpenAddNote(startVoiceImmediately = false) },
                        containerColor = DarkPrimary,
                        contentColor = DarkOnPrimary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .testTag("add_note_fab")
                            .size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Note",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NexusHeader(
                totalNotes = notesList.size,
                onMenuClick = { viewModel.onTabSelected(3) }
            )

            if (activeTab == 0) {
                SearchBarComponent(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                    recentSearches = recentSearches,
                    onSearchSubmitted = { viewModel.onSearchSubmitted(it) },
                    onSelectRecentSearch = { viewModel.onSelectRecentSearch(it) },
                    onDeleteRecentSearch = { viewModel.onDeleteRecentSearch(it) },
                    onClearRecentSearches = { viewModel.onClearRecentSearches() }
                )

                CategoryFilterChips(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.onCategorySelected(it) }
                )

                if (allTagsList.isNotEmpty()) {
                    TagFilterChips(
                        availableTags = allTagsList,
                        selectedTag = selectedTag,
                        onTagSelected = { viewModel.onTagSelected(it) }
                    )
                }

                SortMenuComponent(
                    selectedSortOrder = sortOrder,
                    onSortOrderSelected = { viewModel.onSortOrderSelected(it) }
                )
            }

            when (activeTab) {
                1 -> {
                    LabelsTabView(
                        allTags = allTagsList,
                        allNotes = allNotesList,
                        selectedTag = selectedTag,
                        onTagSelected = { tag ->
                            viewModel.onTagSelected(tag)
                            viewModel.onTabSelected(0)
                        }
                    )
                }
                2 -> {
                    RemindersTabView(
                        pinnedNotes = allNotesList.filter { it.isPinned },
                        onOpenNote = { viewModel.onOpenEditNote(it) },
                        onUnpinNote = { viewModel.onPinToggle(it) }
                    )
                }
                3 -> {
                    SettingsScreen(
                        allNotes = allNotesList,
                        userEmail = userEmail,
                        onSignOut = onSignOut,
                        cloudSyncStatus = cloudSyncStatus,
                        onSyncNow = { viewModel.syncNotesWithFirestore() },
                        backupStatus = backupOperationStatus,
                        onUpdateBackupStatus = { viewModel.updateBackupStatus(it) },
                        onImportBackupContent = { rawContent, callback ->
                            viewModel.importNotesFromRawBackup(rawContent, callback)
                        },
                        themeMode = themeMode,
                        onThemeModeChange = onThemeModeChange,
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = onToggleDarkTheme,
                        isDynamicColor = isDynamicColor,
                        onToggleDynamicColor = onToggleDynamicColor,
                        isBiometricLockEnabled = isBiometricLockEnabled,
                        onToggleBiometricLock = onToggleBiometricLock,
                        lockTimeoutMs = lockTimeoutMs,
                        onUpdateLockTimeout = onUpdateLockTimeout,
                        savedPin = savedPin,
                        onUpdatePin = onUpdatePin,
                        onLockAppNow = onLockAppNow,
                        notificationsEnabled = notificationsEnabled,
                        onToggleNotifications = onToggleNotifications,
                        dailyReminderEnabled = dailyReminderEnabled,
                        onToggleDailyReminder = onToggleDailyReminder,
                        reminderTime = reminderTime,
                        onUpdateReminderTime = onUpdateReminderTime,
                        pinnedNotesAlertEnabled = pinnedNotesAlertEnabled,
                        onTogglePinnedNotesAlert = onTogglePinnedNotesAlert,
                        syncAlertsEnabled = syncAlertsEnabled,
                        onToggleSyncAlerts = onToggleSyncAlerts,
                        soundVibrateEnabled = soundVibrateEnabled,
                        onToggleSoundVibrate = onToggleSoundVibrate
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "analytics_dashboard_card") {
                            NotesDashboardCard(allNotes = allNotesList)
                        }

                        if (notesList.isEmpty()) {
                            item(key = "empty_notes_view") {
                                EmptyNotesView(
                                    query = searchQuery,
                                    category = selectedCategory,
                                    tag = selectedTag,
                                    onAddClick = { viewModel.onOpenAddNote() }
                                )
                            }
                        } else {
                            if (pinnedNotes.isNotEmpty() && selectedCategory != "Pinned") {
                                item(key = "pinned_section_header") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 22.dp, vertical = 2.dp)
                                            .testTag("pinned_notes_header"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = null,
                                            tint = DarkPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "PINNED NOTES (${pinnedNotes.size})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DarkPrimary,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        )
                                    }
                                }
                            }

                            items(
                                items = pinnedNotes,
                                key = { "pinned_${it.id}" }
                            ) { note ->
                                SwipeableNoteItem(
                                    note = note,
                                    onEdit = { viewModel.onOpenEditNote(note) },
                                    onPinToggle = { viewModel.onPinToggle(note) },
                                    onDelete = { viewModel.onDeleteNote(note) }
                                )
                            }

                            if (pinnedNotes.isNotEmpty() && unpinnedNotes.isNotEmpty() && selectedCategory != "Pinned") {
                                item(key = "other_notes_section_header") {
                                    Text(
                                        text = "OTHER NOTES (${unpinnedNotes.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 22.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            items(
                                items = unpinnedNotes,
                                key = { "unpinned_${it.id}" }
                            ) { note ->
                                SwipeableNoteItem(
                                    note = note,
                                    onEdit = { viewModel.onOpenEditNote(note) },
                                    onPinToggle = { viewModel.onPinToggle(note) },
                                    onDelete = { viewModel.onDeleteNote(note) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isAddEditOpen) {
            AddEditNoteDialog(
                note = editingNote,
                onDismiss = {
                    viewModel.clearAiAssistState()
                    viewModel.onCloseDialog()
                },
                onSave = { title, content, category, tags, isPinned ->
                    viewModel.onSaveNote(title, content, category, tags, isPinned)
                },
                onDelete = { note ->
                    viewModel.onDeleteNote(note)
                },
                onAutoSave = { id, title, content, category, tags, isPinned, onSaved ->
                    viewModel.onAutoSave(id, title, content, category, tags, isPinned, onSaved)
                },
                aiAssistState = aiAssistState,
                onEnhanceWithGemini = { title, content, mode, onResult ->
                    viewModel.enhanceNoteWithGemini(title, content, mode, onResult)
                },
                startVoiceOnLaunch = startVoiceOnOpen
            )
        }
    }
}

@Composable
private fun SwipeableNoteItem(
    note: NoteEntity,
    onEdit: () -> Unit,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
        SwipeToDismissBox(
            state = dismissState,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("swipe_dismiss_${note.id}"),
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val alignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.CenterEnd
                }
                val containerColor = if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(containerColor)
                        .padding(horizontal = 20.dp),
                    contentAlignment = alignment
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Note",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        ) {
            NoteCard(
                note = note,
                onClick = onEdit,
                onPinClick = onPinToggle
            )
        }
    }
}

@Composable
fun SettingsTabView(
    allNotes: List<NoteEntity>,
    userEmail: String? = null,
    onSignOut: () -> Unit = {},
    cloudSyncStatus: String? = null,
    onSyncNow: () -> Unit = {},
    backupStatus: String? = null,
    onUpdateBackupStatus: (String?) -> Unit = {},
    onImportBackupContent: (String, (Int, String) -> Unit) -> Unit = { _, _ -> },
    themeMode: String = "system",
    onThemeModeChange: (String) -> Unit = {},
    isDarkTheme: Boolean = true,
    onToggleDarkTheme: (Boolean) -> Unit = {},
    isDynamicColor: Boolean = false,
    onToggleDynamicColor: (Boolean) -> Unit = {},
    isBiometricLockEnabled: Boolean = false,
    onToggleBiometricLock: (Boolean) -> Unit = {},
    lockTimeoutMs: Long = 15_000L,
    onUpdateLockTimeout: (Long) -> Unit = {},
    savedPin: String = "1234",
    onUpdatePin: (String) -> Unit = {},
    onLockAppNow: () -> Unit = {},
    notificationsEnabled: Boolean = true,
    onToggleNotifications: (Boolean) -> Unit = {},
    dailyReminderEnabled: Boolean = true,
    onToggleDailyReminder: (Boolean) -> Unit = {},
    reminderTime: String = "09:00 AM",
    onUpdateReminderTime: (String) -> Unit = {},
    pinnedNotesAlertEnabled: Boolean = true,
    onTogglePinnedNotesAlert: (Boolean) -> Unit = {},
    syncAlertsEnabled: Boolean = true,
    onToggleSyncAlerts: (Boolean) -> Unit = {},
    soundVibrateEnabled: Boolean = true,
    onToggleSoundVibrate: (Boolean) -> Unit = {}
) {
    SettingsScreen(
        allNotes = allNotes,
        userEmail = userEmail,
        onSignOut = onSignOut,
        cloudSyncStatus = cloudSyncStatus,
        onSyncNow = onSyncNow,
        backupStatus = backupStatus,
        onUpdateBackupStatus = onUpdateBackupStatus,
        onImportBackupContent = onImportBackupContent,
        themeMode = themeMode,
        onThemeModeChange = onThemeModeChange,
        isDarkTheme = isDarkTheme,
        onToggleDarkTheme = onToggleDarkTheme,
        isDynamicColor = isDynamicColor,
        onToggleDynamicColor = onToggleDynamicColor,
        isBiometricLockEnabled = isBiometricLockEnabled,
        onToggleBiometricLock = onToggleBiometricLock,
        lockTimeoutMs = lockTimeoutMs,
        onUpdateLockTimeout = onUpdateLockTimeout,
        savedPin = savedPin,
        onUpdatePin = onUpdatePin,
        onLockAppNow = onLockAppNow,
        notificationsEnabled = notificationsEnabled,
        onToggleNotifications = onToggleNotifications,
        dailyReminderEnabled = dailyReminderEnabled,
        onToggleDailyReminder = onToggleDailyReminder,
        reminderTime = reminderTime,
        onUpdateReminderTime = onUpdateReminderTime,
        pinnedNotesAlertEnabled = pinnedNotesAlertEnabled,
        onTogglePinnedNotesAlert = onTogglePinnedNotesAlert,
        syncAlertsEnabled = syncAlertsEnabled,
        onToggleSyncAlerts = onToggleSyncAlerts,
        soundVibrateEnabled = soundVibrateEnabled,
        onToggleSoundVibrate = onToggleSoundVibrate
    )
}

@Composable
fun RemindersTabView(
    pinnedNotes: List<NoteEntity> = emptyList(),
    onOpenNote: (NoteEntity) -> Unit = {},
    onUnpinNote: (NoteEntity) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, DarkOutline)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = DarkPrimary,
                        modifier = Modifier.size(40.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Pinned Priority & Reminders",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "High-priority pinned notes are kept prominently at the top of your main list and summarized below for rapid access.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        if (pinnedNotes.isNotEmpty()) {
            item {
                Text(
                    text = "Pinned Priority Notes (${pinnedNotes.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = DarkPrimary
                )
            }

            items(pinnedNotes, key = { "reminder_pinned_${it.id}" }) { note ->
                NoteCard(
                    note = note,
                    onClick = { onOpenNote(note) },
                    onPinClick = { onUnpinNote(note) }
                )
            }
        }
    }
}

@Composable
fun LabelsTabView(
    allTags: List<String>,
    allNotes: List<NoteEntity>,
    selectedTag: String?,
    onTagSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = stringResource(id = R.string.all_labels),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        if (allTags.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, DarkOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = null,
                            tint = DarkPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No custom tags created yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add custom labels when creating or editing notes to organize your thoughts by project, topic, or priority.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(allTags) { tag ->
                val count = allNotes.count { note ->
                    note.tags.split(",").map { it.trim().lowercase() }.contains(tag.trim().lowercase())
                }
                val isCurrent = tag.equals(selectedTag, ignoreCase = true)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTagSelected(tag) }
                        .testTag("label_item_$tag"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) DarkPrimaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(1.dp, DarkOutline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Label,
                                contentDescription = null,
                                tint = if (isCurrent) DarkOnPrimaryContainer else DarkPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCurrent) DarkOnPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.surface else DarkPrimaryContainer
                        ) {
                            Text(
                                text = "$count notes",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCurrent) DarkPrimary else DarkOnPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyNotesView(
    query: String,
    category: String,
    tag: String?,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NoteAdd,
                contentDescription = null,
                tint = DarkPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val titleText = when {
            query.isNotEmpty() -> "No notes matching \"$query\""
            tag != null -> "No notes with tag #$tag"
            else -> "No $category notes found"
        }

        Text(
            text = titleText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap the + button or Microphone button below to create a note using typing or voice dictation.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
    }
}
