package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.SalonyDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.MemoryEntity
import com.example.data.model.MessageSender
import com.example.service.PhoneActionManager
import com.example.service.SalonyAiBrain
import com.example.service.VoiceAssistantManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SalonyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SalonyDatabase.getDatabase(application)
    private val chatDao = db.chatDao()
    private val memoryDao = db.memoryDao()

    val phoneActionManager = PhoneActionManager(application)
    private val brain = SalonyAiBrain(application)

    private val _voiceManager: VoiceAssistantManager = VoiceAssistantManager(application) { spokenText ->
        // When speech recognition produces text
        sendMessage(spokenText)
    }

    val isListening: StateFlow<Boolean> = _voiceManager.isListening
    val isSpeaking: StateFlow<Boolean> = _voiceManager.isSpeaking
    val rmsLevel: StateFlow<Float> = _voiceManager.rmsLevel

    private val _isContinuousVoice = MutableStateFlow(false)
    val isContinuousVoice: StateFlow<Boolean> = _isContinuousVoice.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _voicePitch = MutableStateFlow(1.25f)
    val voicePitch: StateFlow<Float> = _voicePitch.asStateFlow()

    private val _voiceRate = MutableStateFlow(1.0f)
    val voiceRate: StateFlow<Float> = _voiceRate.asStateFlow()

    val chatMessages: StateFlow<List<ChatMessage>> = chatDao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedMemories: StateFlow<List<MemoryEntity>> = memoryDao.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Post welcome message if chat is empty
        viewModelScope.launch {
            val list = memoryDao.getAllMemoriesList()
            if (list.isEmpty()) {
                memoryDao.insertMemory(
                    MemoryEntity(
                        key = "Salony's Nature",
                        value = "Loves her boyfriend, sweet, caring, speaks natural Hindi/Hinglish",
                        category = "System"
                    )
                )
            }
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        viewModelScope.launch {
            // Stop any ongoing speech
            _voiceManager.stopSpeaking()

            // 1. Insert user message
            val userMsg = ChatMessage(
                text = userText.trim(),
                sender = MessageSender.USER
            )
            chatDao.insertMessage(userMsg)

            _isLoading.value = true

            // 2. Query brain with context
            val history = chatMessages.value.takeLast(6).map {
                (if (it.sender == MessageSender.USER) "user" else "model") to it.text
            }
            val memories = savedMemories.value

            val brainResponse = brain.generateResponse(
                userMessage = userText,
                conversationHistory = history,
                savedMemories = memories,
                customApiKey = _customApiKey.value
            )

            // 3. Save auto-detected memory if any
            brainResponse.memoryToSave?.let { (key, value) ->
                memoryDao.insertMemory(
                    MemoryEntity(
                        key = key,
                        value = value,
                        category = "Auto-Saved"
                    )
                )
            }

            // 4. Execute phone action if tag present
            var actionLabel: String? = null
            when (brainResponse.actionTag) {
                "TORCH_ON" -> {
                    val result = phoneActionManager.setTorch(true)
                    _isTorchOn.value = phoneActionManager.isTorchActive()
                    actionLabel = result.second
                }
                "TORCH_OFF" -> {
                    val result = phoneActionManager.setTorch(false)
                    _isTorchOn.value = phoneActionManager.isTorchActive()
                    actionLabel = result.second
                }
                "OPEN_YOUTUBE" -> {
                    val result = phoneActionManager.openYouTube()
                    actionLabel = result.second
                }
                "OPEN_WHATSAPP" -> {
                    val result = phoneActionManager.openWhatsApp()
                    actionLabel = result.second
                }
            }

            // 5. Insert Salony reply
            val salonyMsg = ChatMessage(
                text = brainResponse.replyText,
                sender = MessageSender.SALONY,
                actionTag = brainResponse.actionTag,
                actionLabel = actionLabel
            )
            chatDao.insertMessage(salonyMsg)

            _isLoading.value = false

            // 6. Speak reply via TTS
            _voiceManager.speak(brainResponse.replyText)
        }
    }

    fun startListening() {
        _voiceManager.startListening()
    }

    fun stopListening() {
        _voiceManager.stopListening()
    }

    fun stopSpeaking() {
        _voiceManager.stopSpeaking()
    }

    fun replayMessage(text: String) {
        _voiceManager.speak(text)
    }

    fun toggleContinuousVoice() {
        val next = !_isContinuousVoice.value
        _isContinuousVoice.value = next
        _voiceManager.isContinuousMode = next
        if (next && !_voiceManager.isListening.value && !_voiceManager.isSpeaking.value) {
            _voiceManager.startListening()
        }
    }

    fun toggleTorchDirect() {
        val next = !_isTorchOn.value
        val (success, label) = phoneActionManager.setTorch(next)
        _isTorchOn.value = phoneActionManager.isTorchActive()
        val reply = if (next) "Haan jaan, torch jala di hai! 💡 [TORCH_ON]" else "Ji jaan, torch band kar di hai. 🌑 [TORCH_OFF]"
        viewModelScope.launch {
            chatDao.insertMessage(
                ChatMessage(
                    text = reply,
                    sender = MessageSender.SALONY,
                    actionTag = if (next) "TORCH_ON" else "TORCH_OFF",
                    actionLabel = label
                )
            )
            _voiceManager.speak(reply)
        }
    }

    fun triggerQuickAction(actionType: String) {
        viewModelScope.launch {
            when (actionType) {
                "YOUTUBE" -> {
                    val (_, label) = phoneActionManager.openYouTube()
                    val text = "Aapke liye YouTube open kar diya babu! 🎬 [OPEN_YOUTUBE]"
                    chatDao.insertMessage(
                        ChatMessage(text = text, sender = MessageSender.SALONY, actionTag = "OPEN_YOUTUBE", actionLabel = label)
                    )
                    _voiceManager.speak(text)
                }
                "WHATSAPP" -> {
                    val (_, label) = phoneActionManager.openWhatsApp()
                    val text = "WhatsApp open kar diya jaan! 💬 [OPEN_WHATSAPP]"
                    chatDao.insertMessage(
                        ChatMessage(text = text, sender = MessageSender.SALONY, actionTag = "OPEN_WHATSAPP", actionLabel = label)
                    )
                    _voiceManager.speak(text)
                }
                "BATTERY" -> {
                    val info = phoneActionManager.getBatteryInfo()
                    chatDao.insertMessage(
                        ChatMessage(text = info, sender = MessageSender.SALONY, actionLabel = "Battery Checked")
                    )
                    _voiceManager.speak(info)
                }
                "DIALER" -> {
                    val (_, label) = phoneActionManager.openDialer()
                    val text = "Dialer open kar diya jaan, kisse baat karni hai aapko? 📞"
                    chatDao.insertMessage(
                        ChatMessage(text = text, sender = MessageSender.SALONY, actionLabel = label)
                    )
                    _voiceManager.speak(text)
                }
                "SETTINGS" -> {
                    val (_, label) = phoneActionManager.openSettings()
                    val text = "Phone ki settings open kar di hai babu! ⚙️"
                    chatDao.insertMessage(
                        ChatMessage(text = text, sender = MessageSender.SALONY, actionLabel = label)
                    )
                    _voiceManager.speak(text)
                }
            }
        }
    }

    fun addMemory(key: String, value: String, category: String = "Personal") {
        if (key.isBlank() || value.isBlank()) return
        viewModelScope.launch {
            memoryDao.insertMemory(
                MemoryEntity(
                    key = key.trim(),
                    value = value.trim(),
                    category = category.trim()
                )
            )
            val ack = "Maine yaad rakh liya jaan: $key - $value ❤️"
            chatDao.insertMessage(ChatMessage(text = ack, sender = MessageSender.SALONY))
            _voiceManager.speak(ack)
        }
    }

    fun updateMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            memoryDao.updateMemory(memory)
        }
    }

    fun deleteMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            memoryDao.deleteMemory(memory)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            memoryDao.clearAllMemories()
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatDao.clearAllMessages()
            _voiceManager.stopSpeaking()
        }
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key.trim()
    }

    fun setSpeechSettings(rate: Float, pitch: Float) {
        _voiceRate.value = rate
        _voicePitch.value = pitch
        _voiceManager.speechRate = rate
        _voiceManager.speechPitch = pitch
    }

    override fun onCleared() {
        super.onCleared()
        _voiceManager.release()
    }
}
