package com.agui.neuralcanvas;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;

public class SystemSpeechRecognizerEngine implements RealtimeSpeechEngine {
    private final Context appContext;
    private SpeechRecognizer recognizer;
    private boolean running = false;
    private Listener listener;

    public SystemSpeechRecognizerEngine(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @Override
    public void start(Listener listener) {
        this.listener = listener;
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            if (listener != null) listener.onError("系统语音识别不可用");
            return;
        }
        stop();
        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { running = true; if (SystemSpeechRecognizerEngine.this.listener != null) SystemSpeechRecognizerEngine.this.listener.onReady(); }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) { running = false; if (SystemSpeechRecognizerEngine.this.listener != null) SystemSpeechRecognizerEngine.this.listener.onError("系统识别错误码: " + error); }
            @Override public void onResults(Bundle results) { dispatchFinal(results); }
            @Override public void onPartialResults(Bundle partialResults) { dispatchPartial(partialResults); }
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINA.toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false);
        recognizer.startListening(intent);
    }

    private void dispatchPartial(Bundle bundle) {
        if (listener == null || bundle == null) return;
        ArrayList<String> list = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (list != null && !list.isEmpty()) listener.onPartial(list.get(0));
    }

    private void dispatchFinal(Bundle bundle) {
        running = false;
        if (listener == null || bundle == null) return;
        ArrayList<String> list = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (list != null && !list.isEmpty()) listener.onFinal(list.get(0));
        listener.onStopped();
    }

    @Override
    public void stop() {
        try {
            if (recognizer != null) {
                recognizer.stopListening();
                recognizer.cancel();
                recognizer.destroy();
            }
        } catch (Exception ignored) {}
        recognizer = null;
        running = false;
        if (listener != null) listener.onStopped();
    }

    @Override public boolean isRunning() { return running; }
    @Override public String getName() { return "SystemSpeechRecognizer"; }
}
