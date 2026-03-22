package com.agui.neuralcanvas;

import android.Manifest;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

public class SherpaVoiceInputDialog extends DialogFragment {

    public interface Callback {
        void onAppendTranscript(String text);
        void onReplaceTranscript(String text);
    }

    private static Callback callback;
    private static final int REQ_AUDIO = 4101;

    private TextView statusView;
    private TextView partialView;
    private EditText finalEdit;
    private TextView hintView;

    private SherpaOnnxStreamingEngine engine;
    private String lastPartialText = "";
    private String lastFinalText = "";
    private boolean started = false;

    public static SherpaVoiceInputDialog newInstance(Callback cb) {
        callback = cb;
        return new SherpaVoiceInputDialog();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                requireContext().getResources().getDisplayMetrics()
        );
    }

    private Button actionButton(String text, Runnable onClick) {
        Button button = new Button(requireContext());
        button.setAllCaps(false);
        button.setText(text);
        button.setTextColor(ThemeManager.getTextPrimary());
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ThemeManager.getSpinnerBg()));
        button.setOnClickListener(v -> onClick.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(8);
        button.setLayoutParams(lp);
        return button;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(ThemeManager.getDialogBg());

        LinearLayout root = MonetDialogStyler.buildRoot(requireContext());
        scroll.addView(root);

        TextView title = new TextView(requireContext());
        title.setText("实时语音转文字");
        root.addView(title);

        TextView sub = new TextView(requireContext());
        sub.setText("Sherpa-ONNX 本地离线识别｜实时草稿 + 可编辑最终文本");
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(6);
        root.addView(sub, subLp);
        MonetDialogStyler.styleHeader(title, sub);

        statusView = MonetDialogStyler.body(requireContext(), "点击“开始录音”后进行实时识别");
        statusView.setPadding(dp(14), dp(12), dp(14), dp(12));
        statusView.setBackground(MonetDialogStyler.cardBg());
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusLp.topMargin = dp(14);
        root.addView(statusView, statusLp);

        TextView partialTitle = new TextView(requireContext());
        partialTitle.setText("实时草稿");
        partialTitle.setTextColor(ThemeManager.getTextPrimary());
        partialTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        partialTitle.setTypeface(partialTitle.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams partialTitleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        partialTitleLp.topMargin = dp(14);
        root.addView(partialTitle, partialTitleLp);

        partialView = MonetDialogStyler.body(requireContext(), "（实时识别结果会显示在这里）");
        partialView.setTextColor(ThemeManager.getTextPrimary());
        partialView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        partialView.setPadding(dp(14), dp(14), dp(14), dp(14));
        partialView.setBackground(MonetDialogStyler.cardBg());
        LinearLayout.LayoutParams partialLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        partialLp.topMargin = dp(8);
        root.addView(partialView, partialLp);

        TextView finalTitle = new TextView(requireContext());
        finalTitle.setText("最终文本（可直接修改 / 复制）");
        finalTitle.setTextColor(ThemeManager.getTextPrimary());
        finalTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        finalTitle.setTypeface(finalTitle.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams finalTitleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        finalTitleLp.topMargin = dp(14);
        root.addView(finalTitle, finalTitleLp);

        finalEdit = new EditText(requireContext());
        finalEdit.setHint("停止录音后，最终文本会出现在这里。你可以继续手动修改。");
        finalEdit.setMinLines(6);
        finalEdit.setMaxLines(12);
        finalEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        finalEdit.setTextColor(ThemeManager.getTextPrimary());
        finalEdit.setHintTextColor(ThemeManager.getTextSecondary());
        finalEdit.setTextIsSelectable(true);
        finalEdit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ThemeManager.getAccent()));
        LinearLayout.LayoutParams finalLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        finalLp.topMargin = dp(8);
        root.addView(finalEdit, finalLp);

        hintView = MonetDialogStyler.body(requireContext(),
                "建议：录音时看“实时草稿”，停下后以“最终文本”为准。可以复制、清空、追加插入或替换插入。");
        hintView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        hintView.setTextColor(ThemeManager.getTextSecondary());
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = dp(10);
        root.addView(hintView, hintLp);

        HorizontalScrollView actionScroll = new HorizontalScrollView(requireContext());
        actionScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout actionRow = new LinearLayout(requireContext());
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.addView(actionButton("复制", this::copyFinalText));
        actionRow.addView(actionButton("清空", () -> {
            finalEdit.setText("");
            partialView.setText("（实时识别结果会显示在这里）");
            lastPartialText = "";
            lastFinalText = "";
        }));
        actionRow.addView(actionButton("追加到导入框", () -> {
            String text = getFinalEditableText();
            if (text.isEmpty()) {
                Toast.makeText(requireContext(), "没有可插入的文字", Toast.LENGTH_SHORT).show();
                return;
            }
            if (callback != null) callback.onAppendTranscript(text);
            Toast.makeText(requireContext(), "已追加到知识导入文本框", Toast.LENGTH_SHORT).show();
        }));
        actionRow.addView(actionButton("替换导入框", () -> {
            String text = getFinalEditableText();
            if (text.isEmpty()) {
                Toast.makeText(requireContext(), "没有可插入的文字", Toast.LENGTH_SHORT).show();
                return;
            }
            if (callback != null) callback.onReplaceTranscript(text);
            Toast.makeText(requireContext(), "已替换知识导入文本框", Toast.LENGTH_SHORT).show();
        }));
        actionScroll.addView(actionRow);
        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionLp.topMargin = dp(14);
        root.addView(actionScroll, actionLp);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(scroll)
                .setNegativeButton("关闭", (d, w) -> stopEngine())
                .setNeutralButton("停止录音", null)
                .setPositiveButton("开始录音", null)
                .create();

        dialog.setOnShowListener(d -> {
            MonetDialogStyler.apply(dialog, requireContext());

            Button startBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button stopBtn = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

            startBtn.setOnClickListener(v -> ensurePermissionAndStart());
            stopBtn.setOnClickListener(v -> {
                stopEngine();
                String text = getFinalEditableText();
                if (!text.isEmpty()) {
                    statusView.setText("录音已停止。你可以继续修改最终文本，然后选择“追加到导入框”或“替换导入框”。");
                }
            });
        });

        return dialog;
    }

    private void copyFinalText() {
        String text = getFinalEditableText();
        if (text.isEmpty()) {
            Toast.makeText(requireContext(), "没有可复制的文字", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("SherpaVoiceText", text));
            Toast.makeText(requireContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFinalEditableText() {
        return finalEdit == null || finalEdit.getText() == null ? "" : finalEdit.getText().toString().trim();
    }

    private void ensurePermissionAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startEngine();
            return;
        }
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
    }

    private void startEngine() {
        if (started) return;
        started = true;
        statusView.setText("正在初始化 Sherpa-ONNX…");
        partialView.setText("（正在等待语音输入）");
        if (lastFinalText.isEmpty()) {
            finalEdit.setText("");
        }

        engine = new SherpaOnnxStreamingEngine(requireContext(), new SherpaOnnxStreamingEngine.Listener() {
            @Override
            public void onReady() {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() ->
                        statusView.setText("正在录音并实时识别…上方是草稿，下方是最终文本。"));
            }

            @Override
            public void onPartialText(String text) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (text != null && !text.trim().isEmpty()) {
                        lastPartialText = text.trim();
                        partialView.setText(lastPartialText);
                    }
                });
            }

            @Override
            public void onFinalText(String text) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    lastFinalText = text == null ? "" : text.trim();
                    if (!lastFinalText.isEmpty()) {
                        finalEdit.setText(lastFinalText);
                        finalEdit.setSelection(lastFinalText.length());
                    }
                    statusView.setText("已拿到最终文本。你可以手动改字、复制、追加插入或替换插入。");
                });
            }

            @Override
            public void onError(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    statusView.setText("识别失败");
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    started = false;
                });
            }
        });
        engine.start();
    }

    private void stopEngine() {
        started = false;
        if (engine != null) {
            engine.stop();
            engine = null;
        }
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        stopEngine();
        super.onDismiss(dialog);
    }

    @Override
    public void onDestroyView() {
        stopEngine();
        super.onDestroyView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startEngine();
            } else {
                Toast.makeText(requireContext(), "未授予录音权限，无法进行实时语音转文字", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
