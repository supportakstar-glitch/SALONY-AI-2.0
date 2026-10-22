# SALONY AI — Native Android Voice Assistant (Java + XML)

**SALONY AI** is a complete, native Android AI assistant application written in pure **Java** with native **Android XML layouts**. It features a modern dark burgundy and pink visual design, real continuous voice interactions, long-term persistent SQLite memory, native phone actions, and an interactive animated Canvas AI Orb.

---

## 📱 App Highlights

- **Aesthetic Dark Theme**: Black background (`#000000`) with rich dark burgundy gradients (`#14050E`, `#220A19`) and vibrant pink highlights (`#FF4081`, `#E91E63`).
- **Interactive AI Brain**: Sweet, caring, non-robotic girlfriend assistant persona (Hinglish/Hindi/English) with Google Gemini API integration and an intelligent offline fallback engine.
- **Animated AI Orb**: Custom hardware-accelerated Canvas view (`AiOrbView.java`) supporting 6 dynamic states:
  - `IDLE`: Gentle breathing pulse
  - `LISTENING`: Active glowing waves responding to real-time speech RMS volume
  - `PROCESSING`: Orbiting energy particles and spinning halo
  - `SPEAKING`: Soundwave ripple rings
  - `INTERRUPTED`: Amber stop-transition
  - `ERROR`: Red indicator pulse
- **Continuous Voice Conversations**:
  - Speech-to-Text using native Android `SpeechRecognizer`
  - Text-to-Speech using Android `TextToSpeech` with custom feminine pitch (1.25x) and speed sliders
  - Instant interruption handling when user starts speaking
  - Automatic listening loop in continuous mode
- **Persistent Long-Term Memory**:
  - Backed by genuine SQLite database (`salony_memory.db`)
  - Auto-extracts facts, preferences, and birthdays from conversation
  - Complete Memory Manager screen (`MemoryActivity.java`): Add, Search, Edit, Delete, and Clear All
  - Injects stored memories into Gemini system context
- **Conversation History**:
  - Saved persistently in SQLite (`salony_chat.db`)
  - Messages display timestamps, user pink bubbles, and AI dark burgundy bubbles
  - Per-message controls: Play Voice, Stop Voice, Copy Text, Share Text
- **Native Phone Actions**:
  - 💡 **Torch ON / OFF** via Camera2 `CameraManager.setTorchMode` (`[TORCH_ON]`, `[TORCH_OFF]`)
  - 🎬 **YouTube** launch via intent/browser (`[OPEN_YOUTUBE]`)
  - 💬 **WhatsApp** chat launch (`[OPEN_WHATSAPP]`)
  - 📞 **Dialer** launch with contact preparation
  - 🔋 **Live Battery Status** query via `BatteryManager`
  - ⚙️ **Android Settings** launcher

---

## 🛠️ Project Structure (Java + XML)

```
app/
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/akstarcompany/salony/ai/
│   │   ├── MainActivity.java                # Main chat & quick chips activity
│   │   ├── VoiceAssistantActivity.java      # Fullscreen voice mode with Animated Orb
│   │   ├── MemoryActivity.java              # Long-term memory management screen
│   │   ├── SettingsActivity.java            # AI & Voice configuration screen
│   │   ├── actions/
│   │   │   └── PhoneActionManager.java      # Hardware & intent actions (Torch, WA, YT, etc.)
│   │   ├── ai/
│   │   │   └── AiApiClient.java             # Gemini API client & offline girlfriend engine
│   │   ├── db/
│   │   │   ├── ChatDatabaseHelper.java      # SQLite helper for chat history
│   │   │   └── MemoryDatabaseHelper.java    # SQLite helper for long-term memory
│   │   ├── model/
│   │   │   ├── ChatMessage.java             # Chat entity model
│   │   │   └── MemoryItem.java              # Memory entity model
│   │   ├── ui/
│   │   │   ├── AiOrbView.java               # Custom Canvas animated AI Orb (6 states)
│   │   │   ├── ChatAdapter.java             # RecyclerView adapter for conversation
│   │   │   └── MemoryAdapter.java           # RecyclerView adapter for memories
│   │   └── voice/
│   │       ├── SpeechRecognitionManager.java# Android STT speech recognizer
│   │       ├── TextToSpeechManager.java     # Android TTS speech engine
│   │       └── VoiceAssistantManager.java   # Continuous voice orchestrator
│   └── res/
│       ├── drawable/
│       │   ├── bg_header_gradient.xml       # Dark burgundy header
│       │   ├── bg_user_bubble.xml           # Pink user bubble
│       │   ├── bg_ai_bubble.xml             # Burgundy AI bubble
│       │   ├── bg_chip.xml / bg_chip_active.xml
│       │   ├── bg_card_burgundy.xml
│       │   ├── bg_input_bar.xml
│       │   ├── img_salony_avatar.jpg        # Salony profile portrait
│       │   └── img_salony_logo.jpg          # Salony AI logo
│       ├── layout/
│       │   ├── activity_main.xml            # Native XML main chat layout
│       │   ├── activity_voice_assistant.xml # Native XML voice orb layout
│       │   ├── activity_memory.xml          # Native XML memory list layout
│       │   ├── activity_settings.xml        # Native XML settings layout
│       │   ├── item_user_message.xml        # User message layout
│       │   ├── item_ai_message.xml          # AI message layout with controls
│       │   ├── item_memory.xml              # Memory list item layout
│       │   └── dialog_add_memory.xml        # Add/Edit memory dialog layout
│       └── values/
│           ├── colors.xml                   # Dark burgundy & pink color palette
│           ├── strings.xml                  # App strings
│           └── themes.xml                   # Material3 Dark Theme
```

---

## 🚀 How to Build & Open in AndroidIDE / Android Studio

### In AndroidIDE (on Android device):
1. Copy or extract the project folder to `/sdcard/AndroidIDEProjects/SALONY_AI`
2. Open **AndroidIDE** app on your phone.
3. Select **Open Project** and navigate to the project directory.
4. Let Gradle sync dependencies.
5. Tap the **Play / Build** icon to compile and install `app-debug.apk` directly to your Android device!

### In Android Studio (on PC/Mac/Linux):
1. Open Android Studio -> **File -> Open** -> select the root directory.
2. Allow Gradle to sync.
3. Connect an Android phone with USB Debugging enabled (or start an emulator).
4. Click **Run 'app'** (`Shift + F10`).

---

## 📦 APK Location

After compilation, the debug APK is generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔒 Permissions & Security

- `RECORD_AUDIO`: Required for microphone speech recognition. Prompted at runtime.
- `CAMERA` & `FLASHLIGHT`: Required for torch hardware control (`setTorchMode`).
- `INTERNET`: Required for Gemini API inference and external links.
- **Privacy Assurance**: All chat conversations and long-term memory records are stored 100% locally on your phone in private SQLite database files (`salony_chat.db` and `salony_memory.db`). No personal logs are transmitted to external servers.
