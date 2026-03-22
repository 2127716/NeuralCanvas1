package com.agui.neuralcanvas;

import android.content.Context;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class SherpaModelManager {

    public interface DownloadListener {
        void onProgress(String text);
        void onSuccess();
        void onError(String message);
    }

    public static final String MODEL_URL =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/" +
            "sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23.tar.bz2";

    public static final String MODEL_DIR_NAME = "streaming-zh-14m";
    private static final String ARCHIVE_NAME = "streaming-zh-14m.tar.bz2";

    private static final String[] REQUIRED = new String[]{
            "tokens.txt",
            "encoder-epoch-99-avg-1.int8.onnx",
            "decoder-epoch-99-avg-1.int8.onnx",
            "joiner-epoch-99-avg-1.int8.onnx"
    };

    private SherpaModelManager() {}

    public static File getModelRoot(Context context) {
        return new File(new File(context.getFilesDir(), "sherpa-models"), MODEL_DIR_NAME);
    }

    public static File getArchiveFile(Context context) {
        return new File(new File(context.getFilesDir(), "sherpa-model-downloads"), ARCHIVE_NAME);
    }

    public static boolean isDownloadedModelInstalled(Context context) {
        File root = getModelRoot(context);
        for (String name : REQUIRED) {
            if (!new File(root, name).exists()) return false;
        }
        return true;
    }

    public static String resolveModelPath(Context context, String fileName) {
        File downloaded = new File(getModelRoot(context), fileName);
        if (downloaded.exists()) return downloaded.getAbsolutePath();
        return "sherpa-onnx/streaming-zh-14m/" + fileName;
    }

    public static void deleteDownloadedModel(Context context) {
        deleteRecursively(getModelRoot(context));
        File archive = getArchiveFile(context);
        if (archive.exists()) archive.delete();
    }

    public static void downloadAndInstall(Context context, DownloadListener listener) {
        new Thread(() -> {
            try {
                File archive = downloadArchive(context, listener);
                installFromArchive(context, archive, listener);
                if (listener != null) listener.onSuccess();
            } catch (Throwable t) {
                if (listener != null) listener.onError(safe(t.getMessage(), "模型下载或安装失败"));
            }
        }, "sherpa-model-download").start();
    }

    private static File downloadArchive(Context context, DownloadListener listener) throws Exception {
        File out = getArchiveFile(context);
        if (out.getParentFile() != null && !out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(MODEL_URL).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setRequestMethod("GET");
        conn.connect();

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("下载失败，HTTP " + code);
        }

        int total = conn.getContentLength();
        InputStream in = conn.getInputStream();
        FileOutputStream fos = new FileOutputStream(out);

        try {
            byte[] buffer = new byte[8192];
            long sum = 0;
            int len;
            long lastUi = 0;
            while ((len = in.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
                sum += len;
                long now = System.currentTimeMillis();
                if (listener != null && now - lastUi > 300) {
                    if (total > 0) {
                        int percent = (int) ((sum * 100L) / total);
                        listener.onProgress("正在下载模型… " + percent + "%");
                    } else {
                        listener.onProgress("正在下载模型… " + (sum / 1024 / 1024) + " MB");
                    }
                    lastUi = now;
                }
            }
            fos.flush();
        } finally {
            try { fos.close(); } catch (Exception ignored) {}
            try { in.close(); } catch (Exception ignored) {}
            conn.disconnect();
        }

        return out;
    }

    private static void installFromArchive(Context context, File archive, DownloadListener listener) throws Exception {
        File tmp = new File(context.getCacheDir(), "sherpa-model-extract");
        deleteRecursively(tmp);
        tmp.mkdirs();

        if (listener != null) listener.onProgress("正在解压模型…");

        FileInputStream fis = new FileInputStream(archive);
        BZip2CompressorInputStream bzis = new BZip2CompressorInputStream(fis);
        TarArchiveInputStream tis = new TarArchiveInputStream(bzis);

        try {
            TarArchiveEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = tis.getNextTarEntry()) != null) {
                File out = new File(tmp, entry.getName());
                if (entry.isDirectory()) {
                    out.mkdirs();
                    continue;
                }
                File parent = out.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();

                FileOutputStream fos = new FileOutputStream(out);
                try {
                    int len;
                    while ((len = tis.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.flush();
                } finally {
                    try { fos.close(); } catch (Exception ignored) {}
                }
            }
        } finally {
            try { tis.close(); } catch (Exception ignored) {}
            try { bzis.close(); } catch (Exception ignored) {}
            try { fis.close(); } catch (Exception ignored) {}
        }

        File target = getModelRoot(context);
        deleteRecursively(target);
        target.mkdirs();

        for (String name : REQUIRED) {
            File found = findFileRecursively(tmp, name);
            if (found == null || !found.exists()) {
                throw new IllegalStateException("解压后缺少模型文件：" + name);
            }
            copyFile(found, new File(target, name));
        }

        deleteRecursively(tmp);
        if (listener != null) listener.onProgress("模型安装完成");
    }

    private static File findFileRecursively(File dir, String name) {
        if (dir == null || !dir.exists()) return null;
        if (dir.isFile()) return name.equals(dir.getName()) ? dir : null;
        File[] children = dir.listFiles();
        if (children == null) return null;
        for (File child : children) {
            File found = findFileRecursively(child, name);
            if (found != null) return found;
        }
        return null;
    }

    private static void copyFile(File src, File dst) throws Exception {
        if (dst.getParentFile() != null && !dst.getParentFile().exists()) {
            dst.getParentFile().mkdirs();
        }
        FileInputStream fis = new FileInputStream(src);
        FileOutputStream fos = new FileOutputStream(dst);
        try {
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
            fos.flush();
        } finally {
            try { fis.close(); } catch (Exception ignored) {}
            try { fos.close(); } catch (Exception ignored) {}
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursively(child);
            }
        }
        file.delete();
    }

    private static String safe(String s, String fallback) {
        return s == null || s.trim().isEmpty() ? fallback : s.trim();
    }
}
