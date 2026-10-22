package com.akstarcompany.salony.ai.voice;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

public class TextToSpeechManager {

    public interface TtsCallback {
        void onTtsStart();
        void onTtsDone();
        void onTtsError(String error);
    }

    private final Context context;
    private final TtsCallback callback;
    private TextToSpeech tts;
    private boolean isInitialized = false;
    private boolean isSpeaking = false;

    private float pitch = 1.25f;
    private float speechRate = 1.0f;

    public TextToSpeechManager(Context context, TtsCallback callback) {
        this.context = context;
        this.callback = callback;
        initTts();
    }

    private void initTts() {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true;
                Locale hindiLocale = new Locale("hi", "IN");
                int result = tts.setLanguage(hindiLocale);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault());
                }
                tts.setPitch(pitch);
                tts.setSpeechRate(speechRate);

                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        isSpeaking = true;
                        if (callback != null) callback.onTtsStart();
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        isSpeaking = false;
                        if (callback != null) callback.onTtsDone();
                    }

                    @Override
                    public void onError(String utteranceId) {
                        isSpeaking = false;
                        if (callback != null) callback.onTtsError("TTS error occurred");
                    }
                });
            } else {
                Log.e("TextToSpeechManager", "Failed to initialize TTS: " + status);
            }
        });
    }

    public boolean isSpeaking() {
        return isSpeaking;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        if (tts != null && isInitialized) {
            tts.setPitch(pitch);
        }
    }

    public void setSpeechRate(float rate) {
        this.speechRate = rate;
        if (tts != null && isInitialized) {
            tts.setSpeechRate(rate);
        }
    }

    public void speak(String text) {
        if (!isInitialized || tts == null) return;

        stop();

        // Strip action tags e.g. [TORCH_ON], markdown formatting, emojis
        String cleanText = text
                .replaceAll("\\[[A-Z_]+\\]", "")
                .replaceAll("\\*\\*", "")
                .trim();

        if (cleanText.isEmpty()) return;

        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "salony_" + System.currentTimeMillis());
        tts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "salony_speech_id");
    }

    public void stop() {
        if (tts != null && isInitialized) {
            tts.stop();
        }
        isSpeaking = false;
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        isSpeaking = false;
    }
}
