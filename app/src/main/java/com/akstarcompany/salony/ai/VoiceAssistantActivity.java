package com.akstarcompany.salony.ai;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.akstarcompany.salony.ai.ai.AiApiClient;
import com.akstarcompany.salony.ai.db.ChatDatabaseHelper;
import com.akstarcompany.salony.ai.model.ChatMessage;
import com.akstarcompany.salony.ai.ui.AiOrbView;
import com.akstarcompany.salony.ai.voice.VoiceAssistantManager;
import com.example.R;

import java.util.List;

public class VoiceAssistantActivity extends AppCompatActivity {

    private static final int REQ_AUDIO_PERM = 202;
    private static final String PREF_NAME = "salony_ai_prefs";

    private AiOrbView aiOrbView;
    private TextView tvOrbStatus;
    private TextView tvUserTranscript;
    private TextView tvSalonyReply;
    private ImageButton btnOrbMic;
    private ImageButton btnOrbStop;

    private VoiceAssistantManager voiceManager;
    private AiApiClient aiClient;
    private ChatDatabaseHelper chatDb;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_assistant);

        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        chatDb = ChatDatabaseHelper.getInstance(this);
        aiClient = new AiApiClient(this);

        aiOrbView = findViewById(R.id.aiOrbView);
        tvOrbStatus = findViewById(R.id.tvOrbStatus);
        tvUserTranscript = findViewById(R.id.tvUserVoiceTranscript);
        tvSalonyReply = findViewById(R.id.tvSalonyVoiceReply);
        btnOrbMic = findViewById(R.id.btnOrbMic);
        btnOrbStop = findViewById(R.id.btnOrbStop);
        ImageButton btnBack = findViewById(R.id.btnBackToChat);
        ImageButton btnSettings = findViewById(R.id.btnVoiceSettings);
        ImageButton btnChat = findViewById(R.id.btnOrbChat);

        btnBack.setOnClickListener(v -> finish());
        btnChat.setOnClickListener(v -> finish());
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        voiceManager = new VoiceAssistantManager(this, new VoiceAssistantManager.AssistantListener() {
            @Override
            public void onListeningStarted() {
                aiOrbView.setState(AiOrbView.State.LISTENING);
                tvOrbStatus.setText("SUN RAHI HOON JAAN... 🎧");
                tvOrbStatus.setTextColor(ContextCompat.getColor(VoiceAssistantActivity.this, R.color.listening_cyan));
            }

            @Override
            public void onListeningRms(float rmsdB) {
                aiOrbView.setRmsLevel(rmsdB);
            }

            @Override
            public void onListeningEnded() {
                if (aiOrbView.getState() == AiOrbView.State.LISTENING) {
                    aiOrbView.setState(AiOrbView.State.IDLE);
                    tvOrbStatus.setText("READY");
                }
            }

            @Override
            public void onUserSpoke(String text) {
                tvUserTranscript.setVisibility(View.VISIBLE);
                tvUserTranscript.setText("“" + text + "”");

                // Save user message in DB
                chatDb.insertMessage(new ChatMessage(text, true, null, null));

                // Switch Orb to PROCESSING
                aiOrbView.setState(AiOrbView.State.PROCESSING);
                tvOrbStatus.setText("SOCH RAHI HOON BABU... 💭");
                tvOrbStatus.setTextColor(ContextCompat.getColor(VoiceAssistantActivity.this, R.color.pink_accent));

                List<ChatMessage> history = chatDb.getAllMessages();
                aiClient.generateReply(text, history, new AiApiClient.AiResponseCallback() {
                    @Override
                    public void onSuccess(String replyText, String actionTag, String actionLabel) {
                        tvSalonyReply.setVisibility(View.VISIBLE);
                        tvSalonyReply.setText(replyText);

                        chatDb.insertMessage(new ChatMessage(replyText, false, actionTag, actionLabel));

                        // Speak AI reply
                        voiceManager.speak(replyText);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        aiOrbView.setState(AiOrbView.State.ERROR);
                        tvOrbStatus.setText("ERROR");
                    }
                });
            }

            @Override
            public void onSpeakingStarted() {
                aiOrbView.setState(AiOrbView.State.SPEAKING);
                tvOrbStatus.setText("BOL RAHI HOON... ❤️");
                tvOrbStatus.setTextColor(ContextCompat.getColor(VoiceAssistantActivity.this, R.color.speaking_pink));
            }

            @Override
            public void onSpeakingEnded() {
                aiOrbView.setState(AiOrbView.State.IDLE);
                tvOrbStatus.setText("READY");
                tvOrbStatus.setTextColor(ContextCompat.getColor(VoiceAssistantActivity.this, R.color.pink_accent));
            }

            @Override
            public void onError(String message) {
                aiOrbView.setState(AiOrbView.State.ERROR);
                tvOrbStatus.setText("ERROR: " + message);
            }
        });

        // Apply settings
        voiceManager.setContinuousMode(prefs.getBoolean("continuous_voice", true));
        voiceManager.setAutoSpeak(true);
        voiceManager.setPitch(prefs.getFloat("voice_pitch", 1.25f));
        voiceManager.setSpeechRate(prefs.getFloat("voice_rate", 1.0f));

        btnOrbMic.setOnClickListener(v -> checkPermissionAndListen());

        btnOrbStop.setOnClickListener(v -> {
            voiceManager.stopAll();
            aiOrbView.setState(AiOrbView.State.INTERRUPTED);
            tvOrbStatus.setText("STOPPED");
        });

        // Start listening automatically on entering voice mode
        checkPermissionAndListen();
    }

    private void checkPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (voiceManager.isListening()) {
                voiceManager.stopListening();
                aiOrbView.setState(AiOrbView.State.IDLE);
                tvOrbStatus.setText("TAP MIC TO TALK");
            } else {
                voiceManager.startListening();
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO_PERM);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO_PERM && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            voiceManager.startListening();
        } else {
            Toast.makeText(this, "Microphone permission required for voice mode", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        voiceManager.setContinuousMode(prefs.getBoolean("continuous_voice", true));
        voiceManager.setPitch(prefs.getFloat("voice_pitch", 1.25f));
        voiceManager.setSpeechRate(prefs.getFloat("voice_rate", 1.0f));
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
