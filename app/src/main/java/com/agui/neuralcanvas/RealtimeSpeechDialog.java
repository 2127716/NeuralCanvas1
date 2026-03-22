package com.agui.neuralcanvas;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

public class RealtimeSpeechDialog extends DialogFragment {
    private RealtimeSpeechEngine engine;
    private TextView partialView;
    private TextView finalView;
    private ActivityResultLauncher<String> permissionLauncher;

    public static RealtimeSpeechDialog newInstance() {
        return new RealtimeSpeechDialog();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) startListening();
            else if (finalView != null) finalView.setText("麦克风权限被拒绝");
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LinearLayout root = MonetDialogStyler.buildRoot(requireContext());

        TextView title = new TextView(requireContext());
        title.setText("实时语音转文字");
        root.addView(title);

        TextView sub = new TextView(requireContext());
        sub.setText("先用系统实时识别兜底；Sherpa-ONNX 作为离线高阶方案接口已预留");
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = MonetDialogStyler.dp(requireContext(), 6);
        root.addView(sub, subLp);
        MonetDialogStyler.styleHeader(title, sub);

        partialView = MonetDialogStyler.body(requireContext(), "实时片段会显示在这里");
        partialView.setPadding(MonetDialogStyler.dp(requireContext(),16),MonetDialogStyler.dp(requireContext(),14),MonetDialogStyler.dp(requireContext(),16),MonetDialogStyler.dp(requireContext(),14));
        partialView.setBackground(MonetDialogStyler.cardBg());
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        pLp.topMargin = MonetDialogStyler.dp(requireContext(), 14);
        root.addView(partialView, pLp);

        finalView = MonetDialogStyler.body(requireContext(), "最终结果会显示在这里");
        finalView.setMovementMethod(new ScrollingMovementMethod());
        finalView.setPadding(MonetDialogStyler.dp(requireContext(),16),MonetDialogStyler.dp(requireContext(),14),MonetDialogStyler.dp(requireContext(),16),MonetDialogStyler.dp(requireContext(),14));
        finalView.setBackground(MonetDialogStyler.cardBg());
        LinearLayout.LayoutParams fLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fLp.topMargin = MonetDialogStyler.dp(requireContext(), 12);
        root.addView(finalView, fLp);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(root)
                .setNegativeButton("关闭", (d,w) -> stopListening())
                .setNeutralButton("停止", (d,w) -> stopListening())
                .setPositiveButton("开始", null)
                .create();

        dialog.setOnShowListener(d -> {
            MonetDialogStyler.apply(dialog, requireContext());
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> ensurePermissionThenStart());
        });
        return dialog;
    }

    private void ensurePermissionThenStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListening();
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startListening() {
        stopListening();
        engine = new SystemSpeechRecognizerEngine(requireContext());
        engine.start(new RealtimeSpeechEngine.Listener() {
            @Override public void onReady() { partialView.setText("已开始监听..."); }
            @Override public void onPartial(String text) { partialView.setText(text == null ? "" : text); }
            @Override public void onFinal(String text) { finalView.setText(text == null ? "" : text); }
            @Override public void onError(String message) { finalView.setText(message == null ? "识别失败" : message); }
            @Override public void onStopped() {}
        });
    }

    private void stopListening() {
        if (engine != null) engine.stop();
        engine = null;
    }

    @Override
    public void onDestroyView() {
        stopListening();
        super.onDestroyView();
    }
}
