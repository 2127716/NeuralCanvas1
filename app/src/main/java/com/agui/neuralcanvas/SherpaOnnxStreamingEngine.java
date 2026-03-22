package com.agui.neuralcanvas;

import android.content.Context;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.k2fsa.sherpa.onnx.EndpointConfig;
import com.k2fsa.sherpa.onnx.EndpointRule;
import com.k2fsa.sherpa.onnx.FeatureConfig;
import com.k2fsa.sherpa.onnx.OnlineModelConfig;
import com.k2fsa.sherpa.onnx.OnlineRecognizer;
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig;
import com.k2fsa.sherpa.onnx.OnlineRecognizerResult;
import com.k2fsa.sherpa.onnx.OnlineStream;
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig;

public class SherpaOnnxStreamingEngine {

    public interface Listener {
        void onReady();
        void onPartialText(String text);
        void onFinalText(String text);
        void onError(String message);
    }

    private static final int SAMPLE_RATE = 16000;
    private static final String MODEL_DIR = "sherpa-onnx/streaming-zh-14m";

    private final Context context;
    private final Listener listener;

    private OnlineRecognizer recognizer;
    private OnlineStream stream;
    private AudioRecord audioRecord;
    private Thread worker;
    private volatile boolean running = false;

    public SherpaOnnxStreamingEngine(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public synchronized void start() {
        if (running) return;

        try {
            recognizer = createRecognizer(context.getAssets());
            stream = recognizer.createStream();

            int minBuffer = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
            );
            if (minBuffer <= 0) {
                throw new IllegalStateException("AudioRecord 初始化失败");
            }

            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuffer * 2
            );

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new IllegalStateException("录音设备初始化失败");
            }

            running = true;
            audioRecord.startRecording();

            if (listener != null) listener.onReady();

            worker = new Thread(() -> loopRead(minBuffer), "sherpa-streaming-thread");
            worker.start();
        } catch (Throwable t) {
            stopInternal(false);
            if (listener != null) listener.onError(safe(t.getMessage(), "Sherpa 启动失败"));
        }
    }

    public synchronized void stop() {
        stopInternal(true);
    }

    private void loopRead(int minBuffer) {
        short[] pcm = new short[Math.max(1600, minBuffer / 2)];
        float[] samples = new float[pcm.length];

        while (running && audioRecord != null) {
            int n = audioRecord.read(pcm, 0, pcm.length);
            if (n <= 0) continue;

            for (int i = 0; i < n; i++) {
                samples[i] = pcm[i] / 32768.0f;
            }

            float[] chunk = new float[n];
            System.arraycopy(samples, 0, chunk, 0, n);

            try {
                if (stream != null) {
                    stream.acceptWaveform(chunk, SAMPLE_RATE);

                    while (recognizer != null && recognizer.isReady(stream)) {
                        recognizer.decode(stream);
                    }

                    OnlineRecognizerResult result = recognizer == null ? null : recognizer.getResult(stream);
                    if (result != null && listener != null) {
                        listener.onPartialText(safe(result.getText(), ""));
                    }

                    if (recognizer != null && recognizer.isEndpoint(stream)) {
                        OnlineRecognizerResult endpointResult = recognizer.getResult(stream);
                        if (endpointResult != null && listener != null) {
                            listener.onFinalText(safe(endpointResult.getText(), ""));
                        }
                        recognizer.reset(stream);
                    }
                }
            } catch (Throwable t) {
                if (listener != null) listener.onError(safe(t.getMessage(), "识别过程中发生错误"));
                stopInternal(false);
                return;
            }
        }
    }

    private synchronized void stopInternal(boolean emitFinal) {
        running = false;

        try {
            if (audioRecord != null) {
                try { audioRecord.stop(); } catch (Exception ignored) {}
                audioRecord.release();
            }
        } catch (Exception ignored) {}
        audioRecord = null;

        try {
            if (stream != null) {
                try { stream.inputFinished(); } catch (Exception ignored) {}
                if (recognizer != null) {
                    while (recognizer.isReady(stream)) {
                        recognizer.decode(stream);
                    }
                    OnlineRecognizerResult result = recognizer.getResult(stream);
                    if (emitFinal && result != null && listener != null) {
                        listener.onFinalText(safe(result.getText(), ""));
                    }
                }
                stream.release();
            }
        } catch (Exception ignored) {}
        stream = null;

        try {
            if (recognizer != null) recognizer.release();
        } catch (Exception ignored) {}
        recognizer = null;

        if (worker != null) {
            try { worker.interrupt(); } catch (Exception ignored) {}
        }
        worker = null;
    }

    private OnlineRecognizer createRecognizer(AssetManager assetManager) {
        OnlineTransducerModelConfig transducer = new OnlineTransducerModelConfig(
                MODEL_DIR + "/encoder-epoch-99-avg-1.int8.onnx",
                MODEL_DIR + "/decoder-epoch-99-avg-1.int8.onnx",
                MODEL_DIR + "/joiner-epoch-99-avg-1.int8.onnx"
        );

        OnlineModelConfig modelConfig = new OnlineModelConfig();
        modelConfig.setTransducer(transducer);
        modelConfig.setTokens(MODEL_DIR + "/tokens.txt");
        modelConfig.setModelType("zipformer");
        modelConfig.setNumThreads(2);
        modelConfig.setProvider("cpu");
        modelConfig.setDebug(false);

        FeatureConfig featureConfig = new FeatureConfig(SAMPLE_RATE, 80, 0.0f);

        EndpointConfig endpointConfig = new EndpointConfig(
                new EndpointRule(false, 2.4f, 0.0f),
                new EndpointRule(true, 1.2f, 0.0f),
                new EndpointRule(false, 0.0f, 20.0f)
        );

        OnlineRecognizerConfig config = new OnlineRecognizerConfig();
        config.setFeatConfig(featureConfig);
        config.setModelConfig(modelConfig);
        config.setEndpointConfig(endpointConfig);
        config.setEnableEndpoint(true);
        config.setDecodingMethod("greedy_search");
        config.setMaxActivePaths(4);

        return new OnlineRecognizer(assetManager, config);
    }

    private String safe(String text, String fallback) {
        return text == null || text.trim().isEmpty() ? fallback : text.trim();
    }
}
