package com.agui.neuralcanvas;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.Text;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class DocumentImportPipeline {

    public static final class ImportResult {
        public String sourceName = "";
        public String type = "";
        public String extractedText = "";
        public String note = "";

        public boolean hasText() {
            return extractedText != null && !extractedText.trim().isEmpty();
        }
    }

    private DocumentImportPipeline() {}

    public static ImportResult importUri(Context context, Uri uri) throws Exception {
        ImportResult out = new ImportResult();
        if (context == null || uri == null) {
            out.note = "无效导入对象";
            return out;
        }

        out.sourceName = safeLast(uri);
        String mime = safeMime(context, uri);
        String ext = safeExt(uri);

        if (mime.startsWith("image/") || isImageExt(ext)) {
            out.type = "image_ocr";
            out.extractedText = ocrImage(context, uri);
            out.note = "图片 OCR";
            return out;
        }

        if ("application/pdf".equals(mime) || "pdf".equals(ext)) {
            out.type = "pdf";
            out.extractedText = extractPdf(context, uri);
            out.note = "PDF 文本提取";
            return out;
        }

        if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(mime) || "docx".equals(ext)) {
            out.type = "docx";
            out.extractedText = extractDocx(context, uri);
            out.note = "DOCX 文本提取";
            return out;
        }

        if ("text/plain".equals(mime) || mime.startsWith("text/") || isTextExt(ext)) {
            out.type = "text";
            out.extractedText = readPlainText(context, uri);
            out.note = "纯文本导入";
            return out;
        }

        if ("application/msword".equals(mime) || "doc".equals(ext)) {
            out.type = "doc";
            out.note = "旧版 .doc 暂未实现解析，建议先转为 docx 或 pdf";
            return out;
        }

        out.type = "unknown";
        out.note = "暂不支持的类型：" + mime;
        return out;
    }

    public static String mergeForAi(String rawText, ImportResult[] docs) {
        StringBuilder sb = new StringBuilder();

        if (rawText != null && !rawText.trim().isEmpty()) {
            sb.append("【手动输入文本】\n").append(rawText.trim()).append("\n\n");
        }

        if (docs != null) {
            for (ImportResult r : docs) {
                if (r == null) continue;
                if (r.hasText()) {
                    sb.append("【导入来源】").append(blank(r.sourceName) ? "(未命名文件)" : r.sourceName).append("\n");
                    sb.append("【导入类型】").append(r.type == null ? "" : r.type).append("\n");
                    sb.append(r.extractedText.trim()).append("\n\n");
                } else if (!blank(r.note)) {
                    sb.append("【导入来源】").append(blank(r.sourceName) ? "(未命名文件)" : r.sourceName).append("\n");
                    sb.append("【说明】").append(r.note).append("\n\n");
                }
            }
        }

        return sb.toString().trim();
    }

    private static String readPlainText(Context context, Uri uri) throws Exception {
        InputStream in = context.getContentResolver().openInputStream(uri);
        if (in == null) return "";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        } finally {
            in.close();
        }
    }

    private static String extractDocx(Context context, Uri uri) throws Exception {
        InputStream in = context.getContentResolver().openInputStream(uri);
        if (in == null) return "";
        ZipInputStream zis = new ZipInputStream(in);
        try {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("word/document.xml".equalsIgnoreCase(entry.getName())) {
                    String xml = readZipEntryFully(zis);
                    return cleanDocxXml(xml);
                }
            }
            return "";
        } finally {
            zis.close();
            in.close();
        }
    }

    private static String extractPdf(Context context, Uri uri) throws Exception {
        PDFBoxResourceLoader.init(context.getApplicationContext());
        InputStream in = context.getContentResolver().openInputStream(uri);
        if (in == null) return "";
        PDDocument document = null;
        try {
            document = PDDocument.load(in);
            PDFTextStripper stripper = new PDFTextStripper();
            return safe(stripper.getText(document)).trim();
        } finally {
            try { if (document != null) document.close(); } catch (Exception ignored) {}
            try { in.close(); } catch (Exception ignored) {}
        }
    }

    private static String ocrImage(Context context, Uri uri) throws Exception {
        InputStream in = context.getContentResolver().openInputStream(uri);
        if (in == null) return "";
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(in);
        } finally {
            in.close();
        }
        if (bitmap == null) return "";

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        CountDownLatch latch = new CountDownLatch(1);
        final String[] out = new String[] { "" };
        final String[] err = new String[] { "" };

        TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build())
                .process(image)
                .addOnSuccessListener(text -> {
                    out[0] = extractStructuredText(text);
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    err[0] = e == null ? "OCR失败" : safe(e.getMessage());
                    latch.countDown();
                });

        latch.await(60, TimeUnit.SECONDS);
        if (!blank(err[0])) throw new RuntimeException(err[0]);
        return safe(out[0]).trim();
    }

    private static String extractStructuredText(Text text) {
        if (text == null) return "";
        String all = safe(text.getText()).trim();
        if (!all.isEmpty()) return all;
        return "";
    }

    private static String readZipEntryFully(ZipInputStream zis) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = zis.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        return bos.toString("UTF-8");
    }

    private static String cleanDocxXml(String xml) {
        if (xml == null) return "";
        String text = xml
                .replaceAll("</w:p>", "\n")
                .replaceAll("</w:tr>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("\\s+", " ")
                .replace(" \n ", "\n")
                .replace("\n ", "\n")
                .replace(" \n", "\n")
                .trim();
        return text;
    }

    private static String safeMime(Context context, Uri uri) {
        try {
            ContentResolver resolver = context.getContentResolver();
            String mime = resolver.getType(uri);
            return mime == null ? "" : mime.trim().toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            return "";
        }
    }

    private static String safeExt(Uri uri) {
        try {
            String s = uri == null ? "" : uri.toString();
            String ext = MimeTypeMap.getFileExtensionFromUrl(s);
            return ext == null ? "" : ext.trim().toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            return "";
        }
    }

    private static String safeLast(Uri uri) {
        if (uri == null) return "";
        String last = uri.getLastPathSegment();
        return last == null ? uri.toString() : last;
    }

    private static boolean isImageExt(String ext) {
        return "png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext) || "webp".equals(ext) || "bmp".equals(ext);
    }

    private static boolean isTextExt(String ext) {
        return "txt".equals(ext) || "md".equals(ext) || "markdown".equals(ext) || "csv".equals(ext);
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
