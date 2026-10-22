# SK Nexus Notes (Nexus-Notes)

> A modern, offline-first, feature-rich Android note-taking application crafted with **Kotlin**, **Jetpack Compose**, and **Room Database**.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material%203-indigo.svg)](https://m3.material.io)
[![Room](https://img.shields.io/badge/Database-Room%20SQLite-orange.svg)](https://developer.android.com/training/data-storage/room)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## 🚀 Key Features

* **⚡ Offline-First Architecture:** Powered by local Room SQLite storage—fast, completely offline, zero latency, and respects user privacy.
* **🎙️ Voice-to-Text Dictation:** Built-in speech recognition for effortless, hands-free note dictation.
* **🏷️ Smart Categorization & Tags:** Organize thoughts with predefined categories (*Personal, Work, Ideas, Study, General*) and custom hashtag labels.
* **📌 Pinned Notes Priority:** Pin critical thoughts and tasks to keep them anchored at the top of your feed.
* **💾 Automatic Debounced Saving:** Never lose progress while drafting—smart auto-save captures updates seamlessly as you type.
* **📊 Real-time Stats:** Live word and character counting for structured drafting.
* **📦 JSON Backup & Export:** Export your entire notes collection to a JSON backup file on device storage via Android Storage Access Framework.
* **🎨 Material 3 Dark Aesthetic:** Elegant edge-to-edge dark UI with high-contrast accents and custom adaptive launcher icon.
* **🔍 Search & Dynamic Sort:** Fast multi-query search with sorting by Modified Date, Created Date, or Alphabetical title.

---

## 🛠️ Tech Stack & Architecture

* **Language:** [Kotlin 2.0+](https://kotlinlang.org/)
* **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (M3)
* **Architecture:** Model-View-ViewModel (MVVM) with Unidirectional Data Flow (UDF)
* **Local Persistence:** [AndroidX Room Database](https://developer.android.com/training/data-storage/room) (SQLite Engine + KSP)
* **Concurrency & Streams:** Kotlin Coroutines & `StateFlow` / `SharedFlow`
* **Dependency Injection:** Constructor Injection with Android ViewModel Factory
* **Testing:** Local JVM testing via [Robolectric](https://robolectric.org/) and [Roborazzi](https://github.com/takahirom/roborazzi)

---

## 📁 Project Structure

```
Nexus-Notes/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt               # Edge-to-edge Compose host activity
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/                    # Room Database, DAO & Entity
│   │   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   │   ├── NoteDao.kt
│   │   │   │   │   │   └── NoteEntity.kt
│   │   │   │   │   └── repository/               # Data repository abstraction
│   │   │   │   │       └── NoteRepository.kt
│   │   │   │   └── ui/
│   │   │   │       ├── NoteViewModel.kt          # UI state management & business logic
│   │   │   │       ├── components/               # Modular Compose components
│   │   │   │       │   ├── AddEditNoteDialog.kt  # Full editor with voice transcription
│   │   │   │       │   ├── NoteCard.kt           # Note item card with pin/tags
│   │   │   │       │   ├── NexusHeader.kt        # App header & status indicator
│   │   │   │       │   ├── NexusBottomNavBar.kt  # Bottom navigation
│   │   │   │       │   ├── SearchBarComponent.kt # Filter and search bar
│   │   │   │       │   ├── CategoryFilterChips.kt
│   │   │   │       │   └── TagFilterChips.kt
│   │   │   │       ├── screens/                  # Main screens (Notes, Labels, Backup)
│   │   │   │       │   └── NotesScreen.kt
│   │   │   │       └── theme/                    # Material 3 colors, shapes & type
│   │   │   ├── res/                              # Adaptive icons, mipmaps, strings
│   │   │   └── AndroidManifest.xml
│   │   └── test/                                 # Unit & Robolectric test suites
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml                        # Version catalog dependencies
├── settings.gradle.kts
└── metadata.json
```

---

## 📦 Getting Started

### Prerequisites

* **Android Studio:** Ladybug (2024.2+) or newer
* **JDK:** Version 17 or Version 21
* **Android SDK:** API Level 36 (targetSdk 36, minSdk 24)

### Building & Running

1. **Clone the repository:**
   ```bash
   git clone git@github.com:sbalochwrites-ops/Nexus-Notes.git
   cd Nexus-Notes
   ```

2. **Open in Android Studio:**
   - Select **File > Open** and choose the `Nexus-Notes` directory.
   - Wait for Gradle sync to complete automatically.

3. **Run on Device or Emulator:**
   - Select your target physical device or Android Virtual Device (AVD).
   - Click **Run (Shift + F10)**.

4. **Command Line Build (via Gradle):**
   ```bash
   # Assemble Debug APK
   gradle :app:assembleDebug

   # Run all Unit & Robolectric Tests
   gradle :app:testDebugUnitTest
   ```

---

## 🧪 Testing

Run the local JVM test suite without needing an active emulator:

```bash
gradle :app:testDebugUnitTest
```

Includes test suites verifying:
- Room entity data mapping and sorting logic
- Pinned notes priority order
- Word and character count computation
- JSON backup schema serialization and integrity
- Context and string resource consistency

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!
Feel free to open an issue or pull request on the [GitHub Issues](https://github.com/sbalochwrites-ops/Nexus-Notes/issues) page.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/NewFeature`)
3. Commit your Changes (`git commit -m 'Add some NewFeature'`)
4. Push to the Branch (`git push origin feature/NewFeature`)
5. Open a Pull Request

---

## 📝 License

This project is open-source and distributed under the [MIT License](LICENSE).

---

**Developed with ❤️ by [sbalochwrites-ops](https://github.com/sbalochwrites-ops)**
