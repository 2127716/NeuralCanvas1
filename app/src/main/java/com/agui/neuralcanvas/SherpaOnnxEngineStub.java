package com.agui.neuralcanvas;

import android.content.Context;

public class SherpaOnnxEngineStub implements RealtimeSpeechEngine {
    private final Context appContext;
    private boolean running = false;

    public SherpaOnnxEngineStub(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @Override
    public void start(Listener listener) {
        running = false;
        if (listener != null) {
            listener.onError("Sherpa-ONNX 引擎骨架已接入，但你还需要把官方 Android aar/so 和模型文件放进项目后才能真正实时转写。当前先保留系统实时识别兜底。");
        }
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override public boolean isRunning() { return running; }
    @Override public String getName() { return "SherpaOnnxStub"; }
}
