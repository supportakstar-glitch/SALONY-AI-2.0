package com.akstarcompany.salony.ai;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.akstarcompany.salony.ai.actions.PhoneActionManager;
import com.akstarcompany.salony.ai.ai.AiApiClient;
import com.akstarcompany.salony.ai.db.ChatDatabaseHelper;
import com.akstarcompany.salony.ai.db.MemoryDatabaseHelper;
import com.akstarcompany.salony.ai.model.ChatMessage;
import com.akstarcompany.salony.ai.model.MemoryItem;
import com.akstarcompany.salony.ai.ui.ChatAdapter;
import com.akstarcompany.salony.ai.voice.VoiceAssistantManager;
import com.example.R;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_MIC_PERMISSION = 101;
    private static final String PREF_NAME = "salony_ai_prefs";

    private ChatDatabaseHelper chatDb;
    private MemoryDatabaseHelper memoryDb;
    private AiApiClient aiClient;
    private VoiceAssistantManager voiceManager;
    private SharedPreferences prefs;

    private RecyclerView rvMessages;
    private ChatAdapter adapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    private EditText etInput;
    private ImageButton btnSend;
    private ImageButton btnMic;
    private LinearLayout layoutEmpty;
    private LinearLayout layoutVoiceStatus;
    private TextView tvVoiceStatusText;
    private View viewVoiceDot;
    private View viewHeaderStatusDot;
    private TextView tvMemoryBadge;
    private TextView chipTorch;
    private TextView tvSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatDb = ChatDatabaseHelper.getInstance(this);
        memoryDb = MemoryDatabaseHelper.getInstance(this);
        aiClient = new AiApiClient(this);
        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        initViews();
        setupVoiceAssistant();
        setupQuickChips();
        loadChatMessages();
        updateMemoryBadge();
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rvMessages);
        etInput = findViewById(R.id.etMessageInput);
        btnSend = findViewById(R.id.btnSend);
        btnMic = findViewById(R.id.btnMic);
        layoutEmpty = findViewById(R.id.layoutEmptyGreeting);
        layoutVoiceStatus = findViewById(R.id.layoutVoiceStatus);
        tvVoiceStatusText = findViewById(R.id.tvVoiceStatusText);
        viewVoiceDot = findViewById(R.id.viewVoiceIndicatorDot);
        viewHeaderStatusDot = findViewById(R.id.viewStatusDot);
        tvMemoryBadge = findViewById(R.id.tvMemoryBadge);
        tvSubtitle = findViewById(R.id.tvAssistantStatus);

        ImageButton btnVoiceMode = findViewById(R.id.btnVoiceMode);
        ImageButton btnMemory = findViewById(R.id.btnMemory);
        ImageButton btnSettings = findViewById(R.id.btnSettings);
        ImageButton btnStopLiveVoice = findViewById(R.id.btnStopLiveVoice);

        btnVoiceMode.setOnClickListener(v -> startActivity(new Intent(this, VoiceAssistantActivity.class)));
        btnMemory.setOnClickListener(v -> startActivity(new Intent(this, MemoryActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        btnStopLiveVoice.setOnClickListener(v -> {
            voiceManager.stopAll();
            layoutVoiceStatus.setVisibility(View.GONE);
        });

        // Setup message list
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);

        adapter = new ChatAdapter(messageList, new ChatAdapter.OnMessageActionListener() {
            @Override
            public void onPlayVoice(ChatMessage message) {
                voiceManager.speak(message.getText());
            }

            @Override
            public void onStopVoice(ChatMessage message) {
                voiceManager.stopSpeaking();
            }

            @Override
            public void onCopyText(ChatMessage message) {
                PhoneActionManager.copyToClipboard(MainActivity.this, message.getText());
            }

            @Override
            public void onShareText(ChatMessage message) {
                PhoneActionManager.shareText(MainActivity.this, message.getText());
            }
        });
        rvMessages.setAdapter(adapter);

        btnSend.setOnClickListener(v -> {
            String text = etInput.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
                etInput.setText("");
            }
        });

        btnMic.setOnClickListener(v -> handleMicClick());

        // Sample greeting prompts
        findViewById(R.id.samplePrompt1).setOnClickListener(v -> sendMessage("Torch on karo jaan 💡"));
        findViewById(R.id.samplePrompt2).setOnClickListener(v -> sendMessage("YouTube open karo babu 🎬"));
        findViewById(R.id.samplePrompt3).setOnClickListener(v -> sendMessage("Mera favorite song yaad rakhna 🧠"));
    }

    private void setupVoiceAssistant() {
        voiceManager = new VoiceAssistantManager(this, new VoiceAssistantManager.AssistantListener() {
            @Override
            public void onListeningStarted() {
                runOnUiThread(() -> {
                    layoutVoiceStatus.setVisibility(View.VISIBLE);
                    tvVoiceStatusText.setText("Salony sun rahi hai, boliye jaan... 🎧");
                    viewVoiceDot.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.listening_cyan));
                    viewHeaderStatusDot.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.listening_cyan));
                    btnMic.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.listening_cyan));
                    tvSubtitle.setText("Sun rahi hoon babu... 🎧");
                });
            }

            @Override
            public void onListeningRms(float rmsdB) {}

            @Override
            public void onListeningEnded() {
                runOnUiThread(() -> {
                    btnMic.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.pink_accent));
                    viewHeaderStatusDot.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.online_green));
                    tvSubtitle.setText("Your Personal AI Assistant");
                    if (!voiceManager.isSpeaking()) {
                        layoutVoiceStatus.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onUserSpoke(String text) {
                runOnUiThread(() -> sendMessage(text));
            }

            @Override
            public void onSpeakingStarted() {
                runOnUiThread(() -> {
                    layoutVoiceStatus.setVisibility(View.VISIBLE);
                    tvVoiceStatusText.setText("Salony bol rahi hai... ❤️");
                    viewVoiceDot.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.speaking_pink));
                    viewHeaderStatusDot.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.speaking_pink));
                    tvSubtitle.setText("Bol rahi hoon jaan... 🎙️");
                });
            }

            @Override
            public void onSpeakingEnded() {
                runOnUiThread(() -> {
                    viewHeaderStatusDot.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.online_green));
                    tvSubtitle.setText("Your Personal AI Assistant");
                    layoutVoiceStatus.setVisibility(View.GONE);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    btnMic.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.pink_accent));
                    layoutVoiceStatus.setVisibility(View.GONE);
                });
            }
        });
    }

    private void setupQuickChips() {
        chipTorch = findViewById(R.id.chipTorch);
        TextView chipYouTube = findViewById(R.id.chipYouTube);
        TextView chipWhatsApp = findViewById(R.id.chipWhatsApp);
        TextView chipMemory = findViewById(R.id.chipMemory);
        TextView chipVoice = findViewById(R.id.chipVoice);
        TextView chipActions = findViewById(R.id.chipActions);
        TextView chipSettings = findViewById(R.id.chipSettings);

        chipTorch.setOnClickListener(v -> {
            boolean nextState = !PhoneActionManager.isTorchActive();
            String label = PhoneActionManager.setTorch(this, nextState);
            updateTorchChip();
            String reply = nextState ? "Haan jaan, torch on kar di aapke liye! 💡 [TORCH_ON]" : "Ji sweetheart, torch off kar di hai. 🌑 [TORCH_OFF]";
            addAssistantMessage(reply, nextState ? "TORCH_ON" : "TORCH_OFF", label);
        });

        chipYouTube.setOnClickListener(v -> {
            String label = PhoneActionManager.openYouTube(this, null);
            addAssistantMessage("Aapke liye YouTube open kar diya mera babu! 🎬 [OPEN_YOUTUBE]", "OPEN_YOUTUBE", label);
        });

        chipWhatsApp.setOnClickListener(v -> {
            String label = PhoneActionManager.openWhatsApp(this, null);
            addAssistantMessage("Ji jaan, WhatsApp open kar diya hai! 💬 [OPEN_WHATSAPP]", "OPEN_WHATSAPP", label);
        });

        chipMemory.setOnClickListener(v -> startActivity(new Intent(this, MemoryActivity.class)));
        chipVoice.setOnClickListener(v -> startActivity(new Intent(this, VoiceAssistantActivity.class)));

        chipActions.setOnClickListener(v -> {
            String battery = PhoneActionManager.getBatteryStatus(this);
            addAssistantMessage(battery, null, "Battery Checked");
        });

        chipSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void updateTorchChip() {
        boolean active = PhoneActionManager.isTorchActive();
        chipTorch.setText(active ? "💡 Torch ON" : "💡 Torch OFF");
        chipTorch.setBackgroundResource(active ? R.drawable.bg_chip_active : R.drawable.bg_chip);
    }

    private void handleMicClick() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (voiceManager.isListening()) {
                voiceManager.stopListening();
            } else {
                voiceManager.startListening();
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC_PERMISSION);
        }
    }

    private void sendMessage(String text) {
        voiceManager.stopSpeaking();

        // 1. Insert user message
        ChatMessage userMsg = new ChatMessage(text, true, null, null);
        chatDb.insertMessage(userMsg);
        messageList.add(userMsg);
        adapter.notifyItemInserted(messageList.size() - 1);
        rvMessages.smoothScrollToPosition(messageList.size() - 1);
        updateEmptyState();

        tvSubtitle.setText("Soch rahi hoon... 💭");

        // 2. Query AI Brain
        aiClient.generateReply(text, messageList, new AiApiClient.AiResponseCallback() {
            @Override
            public void onSuccess(String replyText, String actionTag, String actionLabel) {
                runOnUiThread(() -> {
                    addAssistantMessage(replyText, actionTag, actionLabel);
                    updateTorchChip();
                    updateMemoryBadge();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    tvSubtitle.setText("Your Personal AI Assistant");
                    Toast.makeText(MainActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void addAssistantMessage(String replyText, String actionTag, String actionLabel) {
        ChatMessage aiMsg = new ChatMessage(replyText, false, actionTag, actionLabel);
        chatDb.insertMessage(aiMsg);
        messageList.add(aiMsg);
        adapter.notifyItemInserted(messageList.size() - 1);
        rvMessages.smoothScrollToPosition(messageList.size() - 1);
        updateEmptyState();

        // Speak reply via TTS if auto-speak is enabled
        boolean autoSpeak = prefs.getBoolean("auto_speak", true);
        if (autoSpeak) {
            voiceManager.speak(replyText);
        } else {
            tvSubtitle.setText("Your Personal AI Assistant");
        }
    }

    private void loadChatMessages() {
        messageList.clear();
        messageList.addAll(chatDb.getAllMessages());
        adapter.notifyDataSetChanged();
        if (!messageList.isEmpty()) {
            rvMessages.scrollToPosition(messageList.size() - 1);
        }
        updateEmptyState();
    }

    private void updateEmptyState() {
        layoutEmpty.setVisibility(messageList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateMemoryBadge() {
        List<MemoryItem> memories = memoryDb.getAllMemories();
        if (memories.isEmpty()) {
            tvMemoryBadge.setVisibility(View.GONE);
        } else {
            tvMemoryBadge.setVisibility(View.VISIBLE);
            tvMemoryBadge.setText(String.valueOf(memories.size()));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MIC_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            voiceManager.startListening();
        } else {
            Toast.makeText(this, "Microphone permission is needed to talk with Salony", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        voiceManager.setContinuousMode(prefs.getBoolean("continuous_voice", false));
        voiceManager.setAutoSpeak(prefs.getBoolean("auto_speak", true));
        voiceManager.setPitch(prefs.getFloat("voice_pitch", 1.25f));
        voiceManager.setSpeechRate(prefs.getFloat("voice_rate", 1.0f));
        updateTorchChip();
        updateMemoryBadge();
    }

    @Override
    protected void onPause() {
        super.onPause();
        voiceManager.stopAll();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        voiceManager.release();
    }
}
