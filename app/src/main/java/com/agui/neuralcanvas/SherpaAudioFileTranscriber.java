package com.agui.neuralcanvas;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;

import com.k2fsa.sherpa.onnx.EndpointConfig;
import com.k2fsa.sherpa.onnx.EndpointRule;
import com.k2fsa.sherpa.onnx.FeatureConfig;
import com.k2fsa.sherpa.onnx.OnlineModelConfig;
import com.k2fsa.sherpa.onnx.OnlineRecognizer;
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig;
import com.k2fsa.sherpa.onnx.OnlineRecognizerResult;
import com.k2fsa.sherpa.onnx.OnlineStream;
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class SherpaAudioFileTranscriber {

    public interface Callback {
        void onProgress(String text);
        void onSuccess(String text);
        void onError(String message);
    }

    private static final int SAMPLE_RATE = 16000;

    private SherpaAudioFileTranscriber() {}

    public static void transcribeUri(Context context, Uri uri, Callback callback) {
        new Thread(() -> {
            try {
                if (callback != null) callback.onProgress("正在读取音频文件…");
                WavData wav = readWav(context, uri);
                if (wav.sampleRate != SAMPLE_RATE) {
                    throw new IllegalStateException("当前第一版仅支持 16kHz WAV，当前文件是 " + wav.sampleRate + "Hz");
                }
                if (wav.numChannels != 1) {
                    throw new IllegalStateException("当前第一版仅支持单声道 WAV，当前文件是 " + wav.numChannels + " 声道");
                }

                if (callback != null) callback.onProgress("正在初始化识别器…");
                String text = transcribeSamples(context, wav.samples, callback);
                if (callback != null) callback.onSuccess(text == null ? "" : text.trim());
            } catch (Throwable t) {
                if (callback != null) callback.onError(safe(t.getMessage(), "录音文件转文字失败"));
            }
        }, "sherpa-audio-file-transcriber").start();
    }

    private static String transcribeSamples(Context context, float[] samples, Callback callback) throws Exception {
        AssetManager assetManager = context.getAssets();

        String encoder = SherpaModelManager.resolveModelPath(context, "encoder-epoch-99-avg-1.int8.onnx");
        String decoder = SherpaModelManager.resolveModelPath(context, "decoder-epoch-99-avg-1.int8.onnx");
        String joiner = SherpaModelManager.resolveModelPath(context, "joiner-epoch-99-avg-1.int8.onnx");
        String tokens = SherpaModelManager.resolveModelPath(context, "tokens.txt");

        OnlineTransducerModelConfig transducer = new OnlineTransducerModelConfig(
                encoder, decoder, joiner
        );

        OnlineModelConfig modelConfig = new OnlineModelConfig();
        modelConfig.setTransducer(transducer);
        modelConfig.setTokens(tokens);
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

        OnlineRecognizer recognizer = new OnlineRecognizer(assetManager, config);
        OnlineStream stream = recognizer.createStream("");

        try {
            int chunkSize = 1600;
            int total = samples.length;
            for (int i = 0; i < total; i += chunkSize) {
                int n = Math.min(chunkSize, total - i);
                float[] chunk = new float[n];
                System.arraycopy(samples, i, chunk, 0, n);
                stream.acceptWaveform(chunk, SAMPLE_RATE);

                while (recognizer.isReady(stream)) {
                    recognizer.decode(stream);
                }

                if (callback != null && i % (chunkSize * 20) == 0) {
                    int percent = total <= 0 ? 0 : (int) ((i * 100L) / total);
                    callback.onProgress("正在转写录音… " + percent + "%");
                }
            }

            stream.inputFinished();
            while (recognizer.isReady(stream)) {
                recognizer.decode(stream);
            }

            OnlineRecognizerResult result = recognizer.getResult(stream);
            return result == null ? "" : safe(result.getText(), "");
        } finally {
            try { stream.release(); } catch (Exception ignored) {}
            try { recognizer.release(); } catch (Exception ignored) {}
        }
    }

    private static WavData readWav(Context context, Uri uri) throws Exception {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalStateException("无法打开音频文件");
            byte[] data = readAll(in);
            return parseWav(data);
        }
    }

    private static WavData parseWav(byte[] data) throws Exception {
        if (data.length < 44) throw new IllegalStateException("WAV 文件过小");
        if (!"RIFF".equals(new String(data, 0, 4, "US-ASCII")) ||
                !"WAVE".equals(new String(data, 8, 4, "US-ASCII"))) {
            throw new IllegalStateException("当前第一版只支持标准 WAV 文件");
        }

        int offset = 12;
        int sampleRate = 0;
        int channels = 0;
        int bitsPerSample = 0;
        int dataStart = -1;
        int dataSize = -1;

        while (offset + 8 <= data.length) {
            String chunkId = new String(data, offset, 4, "US-ASCII");
            int chunkSize = leInt(data, offset + 4);
            int chunkDataStart = offset + 8;
            if ("fmt ".equals(chunkId)) {
                int audioFormat = leShort(data, chunkDataStart);
                channels = leShort(data, chunkDataStart + 2);
                sampleRate = leInt(data, chunkDataStart + 4);
                bitsPerSample = leShort(data, chunkDataStart + 14);
                if (audioFormat != 1) {
                    throw new IllegalStateException("当前第一版只支持 PCM WAV");
                }
            } else if ("data".equals(chunkId)) {
                dataStart = chunkDataStart;
                dataSize = chunkSize;
                break;
            }

            offset = chunkDataStart + chunkSize;
            if ((chunkSize & 1) == 1) offset += 1;
        }

        if (dataStart < 0 || dataSize <= 0) throw new IllegalStateException("WAV 文件缺少 data 区块");
        if (bitsPerSample != 16) throw new IllegalStateException("当前第一版只支持 16-bit PCM WAV");

        int sampleCount = dataSize / 2;
        float[] samples = new float[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            int s = leShort(data, dataStart + i * 2);
            samples[i] = s / 32768.0f;
        }

        WavData out = new WavData();
        out.sampleRate = sampleRate;
        out.numChannels = channels;
        out.samples = samples;
        return out;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) != -1) bos.write(buf, 0, len);
        return bos.toByteArray();
    }

    private static int leShort(byte[] b, int off) {
        return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8);
    }

    private static int leInt(byte[] b, int off) {
        return (b[off] & 0xff)
                | ((b[off + 1] & 0xff) << 8)
                | ((b[off + 2] & 0xff) << 16)
                | ((b[off + 3] & 0xff) << 24);
    }

    private static String safe(String s, String fallback) {
        return s == null || s.trim().isEmpty() ? fallback : s.trim();
    }

    private static final class WavData {
        int sampleRate;
        int numChannels;
        float[] samples;
    }
}
