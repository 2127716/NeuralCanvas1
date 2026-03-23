package com.agui.neuralcanvas;

import android.content.Context;
import android.content.res.AssetManager;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Locale;

public final class SherpaAudioFileTranscriber {

    public interface Callback {
        void onProgress(String text);
        void onSuccess(String text);
        void onError(String message);
    }

    private static final int TARGET_SAMPLE_RATE = 16000;

    private SherpaAudioFileTranscriber() {}

    public static void transcribeUri(Context context, Uri uri, Callback callback) {
        new Thread(() -> {
            try {
                if (callback != null) callback.onProgress("正在读取音频文件…");
                AudioData audio = loadAudio(context, uri, callback);

                if (callback != null) {
                    callback.onProgress("音频已解析：" + audio.sampleRate + "Hz / "
                            + audio.numChannels + "声道，正在预处理…");
                }

                float[] mono16k = normalizeToMono16k(audio.samples, audio.sampleRate, audio.numChannels);

                if (callback != null) callback.onProgress("正在初始化识别器…");
                String text = transcribeSamples(context, mono16k, callback);
                if (callback != null) callback.onSuccess(text == null ? "" : text.trim());
            } catch (Throwable t) {
                if (callback != null) callback.onError(safe(t.getMessage(), "录音文件转文字失败"));
            }
        }, "sherpa-audio-file-transcriber").start();
    }

    private static AudioData loadAudio(Context context, Uri uri, Callback callback) throws Exception {
        String mime = "";
        try {
            mime = safe(context.getContentResolver().getType(uri), "").toLowerCase(Locale.ROOT);
        } catch (Exception ignored) {}

        String uriText = uri == null ? "" : uri.toString().toLowerCase(Locale.ROOT);

        if (mime.contains("wav") || uriText.endsWith(".wav")) {
            return readWav(context, uri);
        }

        // 第二版：支持 AAC / M4A / MP3 / 其他系统可解码格式
        return decodeCompressedAudio(context, uri, callback);
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

        FeatureConfig featureConfig = new FeatureConfig(TARGET_SAMPLE_RATE, 80, 0.0f);

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
                stream.acceptWaveform(chunk, TARGET_SAMPLE_RATE);

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

    private static AudioData decodeCompressedAudio(Context context, Uri uri, Callback callback) throws Exception {
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(context, uri, null);

        int trackIndex = -1;
        MediaFormat format = null;

        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat f = extractor.getTrackFormat(i);
            String mime = safe(f.getString(MediaFormat.KEY_MIME), "");
            if (mime.startsWith("audio/")) {
                trackIndex = i;
                format = f;
                break;
            }
        }

        if (trackIndex < 0 || format == null) {
            extractor.release();
            throw new IllegalStateException("没有找到可解码的音频轨道");
        }

        extractor.selectTrack(trackIndex);

        String mime = safe(format.getString(MediaFormat.KEY_MIME), "");
        MediaCodec codec = MediaCodec.createDecoderByType(mime);
        codec.configure(format, null, null, 0);
        codec.start();

        int inputEOS = 0;
        int outputEOS = 0;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        ByteArrayOutputStream pcmOut = new ByteArrayOutputStream();

        int sampleRate = format.containsKey(MediaFormat.KEY_SAMPLE_RATE)
                ? format.getInteger(MediaFormat.KEY_SAMPLE_RATE) : TARGET_SAMPLE_RATE;
        int channels = format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
                ? format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 1;

        try {
            while (outputEOS == 0) {
                if (inputEOS == 0) {
                    int inputIndex = codec.dequeueInputBuffer(10000);
                    if (inputIndex >= 0) {
                        ByteBuffer inputBuffer = codec.getInputBuffer(inputIndex);
                        if (inputBuffer != null) inputBuffer.clear();

                        int size = extractor.readSampleData(inputBuffer, 0);
                        if (size < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputEOS = 1;
                        } else {
                            long timeUs = extractor.getSampleTime();
                            codec.queueInputBuffer(inputIndex, 0, size, timeUs, 0);
                            extractor.advance();
                        }
                    }
                }

                int outputIndex = codec.dequeueOutputBuffer(info, 10000);
                if (outputIndex >= 0) {
                    ByteBuffer outputBuffer = codec.getOutputBuffer(outputIndex);
                    if (outputBuffer != null && info.size > 0) {
                        outputBuffer.position(info.offset);
                        outputBuffer.limit(info.offset + info.size);
                        byte[] chunk = new byte[info.size];
                        outputBuffer.get(chunk);
                        pcmOut.write(chunk, 0, chunk.length);
                    }
                    codec.releaseOutputBuffer(outputIndex, false);

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputEOS = 1;
                    }
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat outFormat = codec.getOutputFormat();
                    if (outFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    }
                    if (outFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channels = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    }
                }

                if (callback != null) {
                    long dur = format.containsKey(MediaFormat.KEY_DURATION) ? format.getLong(MediaFormat.KEY_DURATION) : -1L;
                    long cur = extractor.getSampleTime();
                    if (dur > 0 && cur > 0) {
                        int percent = (int) Math.min(99, (cur * 100L) / dur);
                        callback.onProgress("正在解码压缩音频… " + percent + "%");
                    } else {
                        callback.onProgress("正在解码压缩音频…");
                    }
                }
            }
        } finally {
            try { codec.stop(); } catch (Exception ignored) {}
            try { codec.release(); } catch (Exception ignored) {}
            try { extractor.release(); } catch (Exception ignored) {}
        }

        byte[] pcmBytes = pcmOut.toByteArray();
        if (pcmBytes.length == 0) {
            throw new IllegalStateException("音频解码后没有得到 PCM 数据");
        }

        ShortBuffer shortBuffer = ByteBuffer.wrap(pcmBytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer();
        short[] shorts = new short[shortBuffer.remaining()];
        shortBuffer.get(shorts);

        float[] floats = new float[shorts.length];
        for (int i = 0; i < shorts.length; i++) {
            floats[i] = shorts[i] / 32768.0f;
        }

        AudioData out = new AudioData();
        out.sampleRate = sampleRate;
        out.numChannels = channels;
        out.samples = floats;
        return out;
    }

    private static AudioData readWav(Context context, Uri uri) throws Exception {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalStateException("无法打开音频文件");
            byte[] data = readAll(in);
            return parseWav(data);
        }
    }

    private static AudioData parseWav(byte[] data) throws Exception {
        if (data.length < 44) throw new IllegalStateException("WAV 文件过小");
        if (!"RIFF".equals(new String(data, 0, 4, "US-ASCII")) ||
                !"WAVE".equals(new String(data, 8, 4, "US-ASCII"))) {
            throw new IllegalStateException("当前文件不是标准 WAV");
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
                    throw new IllegalStateException("WAV 第一版只支持 PCM");
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
        if (bitsPerSample != 16) throw new IllegalStateException("WAV 第一版只支持 16-bit PCM");

        int sampleCount = dataSize / 2;
        float[] samples = new float[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            int s = leShort(data, dataStart + i * 2);
            samples[i] = s / 32768.0f;
        }

        AudioData out = new AudioData();
        out.sampleRate = sampleRate;
        out.numChannels = channels;
        out.samples = samples;
        return out;
    }

    private static float[] normalizeToMono16k(float[] samples, int sampleRate, int channels) {
        if (samples == null) return new float[0];
        if (channels <= 0) channels = 1;
        float[] mono = samples;

        if (channels > 1) {
            int frames = samples.length / channels;
            mono = new float[frames];
            for (int i = 0; i < frames; i++) {
                float sum = 0f;
                for (int c = 0; c < channels; c++) {
                    sum += samples[i * channels + c];
                }
                mono[i] = sum / channels;
            }
        }

        if (sampleRate == TARGET_SAMPLE_RATE) return mono;
        if (mono.length == 0) return mono;

        int outLen = (int) Math.max(1, ((long) mono.length * TARGET_SAMPLE_RATE) / Math.max(1, sampleRate));
        float[] out = new float[outLen];
        double scale = (double) sampleRate / TARGET_SAMPLE_RATE;

        for (int i = 0; i < outLen; i++) {
            double srcIndex = i * scale;
            int i0 = (int) Math.floor(srcIndex);
            int i1 = Math.min(i0 + 1, mono.length - 1);
            double frac = srcIndex - i0;
            float v0 = mono[Math.min(i0, mono.length - 1)];
            float v1 = mono[i1];
            out[i] = (float) (v0 * (1.0 - frac) + v1 * frac);
        }
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

    private static final class AudioData {
        int sampleRate;
        int numChannels;
        float[] samples;
    }
}
