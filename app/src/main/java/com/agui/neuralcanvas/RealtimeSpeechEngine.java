package com.agui.neuralcanvas;

public interface RealtimeSpeechEngine {
    interface Listener {
        void onReady();
        void onPartial(String text);
        void onFinal(String text);
        void onError(String message);
        void onStopped();
    }

    void start(Listener listener);
    void stop();
    boolean isRunning();
    String getName();
}
