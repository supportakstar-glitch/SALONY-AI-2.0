package com.akstarcompany.salony.ai;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.akstarcompany.salony.ai.ai.AiApiClient;
import com.akstarcompany.salony.ai.db.ChatDatabaseHelper;
import com.akstarcompany.salony.ai.voice.TextToSpeechManager;
import com.example.R;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREF_NAME = "salony_ai_prefs";
    private static final String KEY_PITCH = "voice_pitch";
    private static final String KEY_RATE = "voice_rate";
    private static final String KEY_CONTINUOUS = "continuous_voice";
    private static final String KEY_AUTO_SPEAK = "auto_speak";

    private SharedPreferences prefs;
    private AiApiClient aiClient;
    private TextToSpeechManager ttsManager;

    private EditText etApiKey;
    private TextView tvPitchLabel;
    private TextView tvSpeedLabel;
    private TextView tvApiStatus;
    private SeekBar seekBarPitch;
    private SeekBar seekBarSpeed;
    private MaterialSwitch switchContinuousVoice;
    private MaterialSwitch switchAutoSpeak;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        aiClient = new AiApiClient(this);
        ttsManager = new TextToSpeechManager(this, null);

        ImageButton btnBack = findViewById(R.id.btnBackSettings);
        etApiKey = findViewById(R.id.etApiKey);
        Button btnSaveApiKey = findViewById(R.id.btnSaveApiKey);
        Button btnTestApi = findViewById(R.id.btnTestApi);
        tvApiStatus = findViewById(R.id.tvApiStatus);
        switchContinuousVoice = findViewById(R.id.switchContinuousVoice);
        switchAutoSpeak = findViewById(R.id.switchAutoSpeak);
        seekBarPitch = findViewById(R.id.seekBarPitch);
        seekBarSpeed = findViewById(R.id.seekBarSpeed);
        tvPitchLabel = findViewById(R.id.tvPitchLabel);
        tvSpeedLabel = findViewById(R.id.tvSpeedLabel);
        Button btnTestVoice = findViewById(R.id.btnTestVoice);
        Button btnClearChat = findViewById(R.id.btnClearChat);

        btnBack.setOnClickListener(v -> finish());

        // Load existing values
        String currentKey = aiClient.getApiKey();
        if (!currentKey.isEmpty()) {
            etApiKey.setText(currentKey);
            tvApiStatus.setText("Status: Active Gemini API Key");
        } else {
            tvApiStatus.setText("Status: Smart Offline Girlfriend Engine Ready");
        }

        btnSaveApiKey.setOnClickListener(v -> {
            String newKey = etApiKey.getText().toString().trim();
            aiClient.saveApiKey(newKey);
            tvApiStatus.setText(!newKey.isEmpty() ? "Status: Active Gemini API Key" : "Status: Smart Offline Girlfriend Engine Ready");
            Toast.makeText(this, "API Key saved! 🔑", Toast.LENGTH_SHORT).show();
        });

        btnTestApi.setOnClickListener(v -> {
            Toast.makeText(this, "Testing AI connection...", Toast.LENGTH_SHORT).show();
            aiClient.generateReply("Hello jaan", null, new AiApiClient.AiResponseCallback() {
                @Override
                public void onSuccess(String replyText, String actionTag, String actionLabel) {
                    Toast.makeText(SettingsActivity.this, "AI Connected! Reply: " + replyText, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(SettingsActivity.this, "Test Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Continuous mode & Auto speak switches
        boolean isContinuous = prefs.getBoolean(KEY_CONTINUOUS, false);
        boolean isAutoSpeak = prefs.getBoolean(KEY_AUTO_SPEAK, true);

        switchContinuousVoice.setChecked(isContinuous);
        switchAutoSpeak.setChecked(isAutoSpeak);

        switchContinuousVoice.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_CONTINUOUS, isChecked).apply();
        });

        switchAutoSpeak.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_AUTO_SPEAK, isChecked).apply();
        });

        // Sliders
        float pitchVal = prefs.getFloat(KEY_PITCH, 1.25f);
        float speedVal = prefs.getFloat(KEY_RATE, 1.0f);

        int pitchProgress = (int) ((pitchVal - 0.8f) / 1.0f * 100);
        int speedProgress = (int) ((speedVal - 0.7f) / 0.7f * 100);

        seekBarPitch.setProgress(Math.max(0, Math.min(100, pitchProgress)));
        seekBarSpeed.setProgress(Math.max(0, Math.min(100, speedProgress)));
        updatePitchLabel(pitchVal);
        updateSpeedLabel(speedVal);

        seekBarPitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float calculated = 0.8f + (progress / 100f) * 1.0f;
                updatePitchLabel(calculated);
                prefs.edit().putFloat(KEY_PITCH, calculated).apply();
                ttsManager.setPitch(calculated);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float calculated = 0.7f + (progress / 100f) * 0.7f;
                updateSpeedLabel(calculated);
                prefs.edit().putFloat(KEY_RATE, calculated).apply();
                ttsManager.setSpeechRate(calculated);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnTestVoice.setOnClickListener(v -> {
            ttsManager.speak("Haan mere babu! Main Salony hoon, aapki sweet girlfriend. ❤️");
        });

        btnClearChat.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Clear Chat History?")
                    .setMessage("Kya aap chat ki saari puraani baatein delete karna chahte ho?")
                    .setPositiveButton("Clear", (dialog, which) -> {
                        ChatDatabaseHelper.getInstance(this).clearAllMessages();
                        Toast.makeText(this, "Chat history deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void updatePitchLabel(float pitch) {
        tvPitchLabel.setText(String.format("Voice Sweetness (Pitch): %.2fx", pitch));
    }

    private void updateSpeedLabel(float speed) {
        tvSpeedLabel.setText(String.format("Speech Speed: %.2fx", speed));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ttsManager.shutdown();
    }
}
