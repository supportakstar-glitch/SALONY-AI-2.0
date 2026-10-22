package com.akstarcompany.salony.ai.voice;

import android.content.Context;

public class VoiceAssistantManager {

    public interface AssistantListener {
        void onListeningStarted();
        void onListeningRms(float rmsdB);
        void onListeningEnded();
        void onUserSpoke(String text);
        void onSpeakingStarted();
        void onSpeakingEnded();
        void onError(String message);
    }

    private final SpeechRecognitionManager speechManager;
    private final TextToSpeechManager ttsManager;
    private final AssistantListener listener;

    private boolean isContinuousMode = false;
    private boolean autoSpeak = true;

    public VoiceAssistantManager(Context context, AssistantListener listener) {
        this.listener = listener;

        this.speechManager = new SpeechRecognitionManager(context, new SpeechRecognitionManager.SpeechCallback() {
            @Override
            public void onSpeechStart() {
                if (listener != null) listener.onListeningStarted();
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                if (listener != null) listener.onListeningRms(rmsdB);
            }

            @Override
            public void onSpeechEnd() {
                if (listener != null) listener.onListeningEnded();
            }

            @Override
            public void onSpeechResult(String text) {
                if (listener != null) listener.onUserSpoke(text);
            }

            @Override
            public void onSpeechError(int errorCode, String errorMessage) {
                if (listener != null) listener.onError(errorMessage);
            }
        });

        this.ttsManager = new TextToSpeechManager(context, new TextToSpeechManager.TtsCallback() {
            @Override
            public void onTtsStart() {
                if (listener != null) listener.onSpeakingStarted();
            }

            @Override
            public void onTtsDone() {
                if (listener != null) listener.onSpeakingEnded();
                if (isContinuousMode) {
                    startListening();
                }
            }

            @Override
            public void onTtsError(String error) {
                if (listener != null) listener.onSpeakingEnded();
            }
        });
    }

    public boolean isListening() {
        return speechManager.isListening();
    }

    public boolean isSpeaking() {
        return ttsManager.isSpeaking();
    }

    public boolean isContinuousMode() {
        return isContinuousMode;
    }

    public void setContinuousMode(boolean continuousMode) {
        this.isContinuousMode = continuousMode;
    }

    public void setAutoSpeak(boolean autoSpeak) {
        this.autoSpeak = autoSpeak;
    }

    public void setPitch(float pitch) {
        ttsManager.setPitch(pitch);
    }

    public void setSpeechRate(float rate) {
        ttsManager.setSpeechRate(rate);
    }

    public void startListening() {
        // Interruption: stop speech before listening
        ttsManager.stop();
        speechManager.startListening();
    }

    public void stopListening() {
        speechManager.stopListening();
    }

    public void speak(String text) {
        if (!autoSpeak) return;
        speechManager.stopListening();
        ttsManager.speak(text);
    }

    public void stopSpeaking() {
        ttsManager.stop();
    }

    public void stopAll() {
        speechManager.stopListening();
        ttsManager.stop();
    }

    public void release() {
        speechManager.stopListening();
        ttsManager.shutdown();
    }
}
