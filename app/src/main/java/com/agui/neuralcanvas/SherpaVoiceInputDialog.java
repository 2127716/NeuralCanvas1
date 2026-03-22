package com.agui.neuralcanvas;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;
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
        void onTranscriptReady(String text);
    }

    private static Callback callback;
    private static final int REQ_AUDIO = 4101;

    private TextView statusView;
    private TextView transcriptView;
    private SherpaOnnxStreamingEngine engine;
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
        sub.setText("Sherpa-ONNX 本地离线识别｜中文 streaming 模型");
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(6);
        root.addView(sub, subLp);
        MonetDialogStyler.styleHeader(title, sub);

        statusView = MonetDialogStyler.body(requireContext(), "点击“开始录音”后实时识别");
        statusView.setPadding(dp(14), dp(12), dp(14), dp(12));
        statusView.setBackground(MonetDialogStyler.cardBg());
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusLp.topMargin = dp(14);
        root.addView(statusView, statusLp);

        transcriptView = MonetDialogStyler.body(requireContext(), "");
        transcriptView.setText("（识别结果会显示在这里）");
        transcriptView.setTextColor(ThemeManager.getTextPrimary());
        transcriptView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        transcriptView.setPadding(dp(14), dp(14), dp(14), dp(14));
        transcriptView.setBackground(MonetDialogStyler.cardBg());
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textLp.topMargin = dp(12);
        root.addView(transcriptView, textLp);

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
                if (!lastFinalText.trim().isEmpty() && callback != null) {
                    callback.onTranscriptReady(lastFinalText.trim());
                    Toast.makeText(requireContext(), "已插入到知识导入文本框", Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            });
        });

        return dialog;
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
        transcriptView.setText("");

        engine = new SherpaOnnxStreamingEngine(requireContext(), new SherpaOnnxStreamingEngine.Listener() {
            @Override
            public void onReady() {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> statusView.setText("正在录音并实时识别…"));
            }

            @Override
            public void onPartialText(String text) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (text != null && !text.trim().isEmpty()) {
                        transcriptView.setText(text.trim());
                    }
                });
            }

            @Override
            public void onFinalText(String text) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    lastFinalText = text == null ? "" : text.trim();
                    if (!lastFinalText.isEmpty()) {
                        transcriptView.setText(lastFinalText);
                    }
                    statusView.setText("已停止录音，可点击“停止录音”回填文本");
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
