package com.akstarcompany.salony.ai.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

public class SpeechRecognitionManager {

    public interface SpeechCallback {
        void onSpeechStart();
        void onRmsChanged(float rmsdB);
        void onSpeechEnd();
        void onSpeechResult(String text);
        void onSpeechError(int errorCode, String errorMessage);
    }

    private final Context context;
    private final SpeechCallback callback;
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;

    public SpeechRecognitionManager(Context context, SpeechCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    public boolean isListening() {
        return isListening;
    }

    public void startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            if (callback != null) {
                callback.onSpeechError(-1, "Speech recognition is not available on this device");
            }
            return;
        }

        stopListening();

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                isListening = true;
                if (callback != null) callback.onSpeechStart();
            }

            @Override
            public void onBeginningOfSpeech() {
                isListening = true;
                if (callback != null) callback.onSpeechStart();
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                if (callback != null) callback.onRmsChanged(rmsdB);
            }

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                isListening = false;
                if (callback != null) callback.onSpeechEnd();
            }

            @Override
            public void onError(int error) {
                isListening = false;
                String message = getErrorText(error);
                Log.w("SpeechRecognizer", "Error code " + error + ": " + message);
                if (callback != null) callback.onSpeechError(error, message);
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognized = matches.get(0).trim();
                    if (callback != null) callback.onSpeechResult(recognized);
                } else {
                    if (callback != null) callback.onSpeechEnd();
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN");
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Salony sun rahi hai, boliye...");

        try {
            speechRecognizer.startListening(intent);
            isListening = true;
        } catch (Exception e) {
            isListening = false;
            if (callback != null) callback.onSpeechError(-1, e.getMessage());
        }
    }

    public void stopListening() {
        isListening = false;
        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
                speechRecognizer.destroy();
            } catch (Exception ignored) {}
            speechRecognizer = null;
        }
    }

    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO: return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT: return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Microphone permission required";
            case SpeechRecognizer.ERROR_NETWORK: return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH: return "No speech recognized";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Speech service is busy";
            case SpeechRecognizer.ERROR_SERVER: return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No speech detected";
            default: return "Speech error (" + errorCode + ")";
        }
    }
}
