package com.agui.neuralcanvas;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class FocusSessionDialog extends DialogFragment {
    private static Node currentNode;
    private static MainActivity currentActivity;

    public static FocusSessionDialog newInstance(MainActivity activity, Node node) {
        currentActivity = activity;
        currentNode = node;
        return new FocusSessionDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LinearLayout root = MonetDialogStyler.buildRoot(requireContext());

        TextView title = new TextView(requireContext());
        title.setText("深度工作 Session");
        root.addView(title);

        TextView sub = new TextView(requireContext());
        sub.setText("专注次数、触发命中和当前时长统一显示");
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = MonetDialogStyler.dp(requireContext(), 6);
        root.addView(sub, subLp);
        MonetDialogStyler.styleHeader(title, sub);

        TextView info = MonetDialogStyler.body(requireContext(),
                "当前节点：" + safeTitle(currentNode)
                        + "\n\n"
                        + FocusSessionEngine.getNodeStats(currentNode)
                        + "\n"
                        + FocusSessionEngine.getTriggerStats(currentNode)
        );
        info.setPadding(MonetDialogStyler.dp(requireContext(), 16), MonetDialogStyler.dp(requireContext(), 14),
                MonetDialogStyler.dp(requireContext(), 16), MonetDialogStyler.dp(requireContext(), 14));
        info.setBackground(MonetDialogStyler.cardBg());
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        infoLp.topMargin = MonetDialogStyler.dp(requireContext(), 14);
        root.addView(info, infoLp);

        EditText minutesInput = new EditText(requireContext());
        minutesInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        minutesInput.setHint("计划专注分钟数，例如 25");
        minutesInput.setText("25");
        minutesInput.setTextColor(ThemeManager.getTextPrimary());
        minutesInput.setHintTextColor(ThemeManager.getTextSecondary());
        minutesInput.setBackgroundTintList(ColorStateList.valueOf(ThemeManager.getAccent()));
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        inputLp.topMargin = MonetDialogStyler.dp(requireContext(), 16);
        root.addView(minutesInput, inputLp);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(root)
                .setPositiveButton("开始", (d, w) -> {
                    if (currentActivity == null || currentNode == null) return;
                    int minutes = parseInt(minutesInput.getText().toString(), 25);
                    currentActivity.startFocusSession(currentNode, minutes);
                })
                .setNeutralButton("完成当前SESSION", (d, w) -> {
                    if (currentActivity == null) return;
                    currentActivity.finishRunningFocusSession(false);
                })
                .setNegativeButton("中断当前SESSION", (d, w) -> {
                    if (currentActivity == null) return;
                    currentActivity.finishRunningFocusSession(true);
                })
                .create();
        dialog.setOnShowListener(d -> MonetDialogStyler.apply(dialog, requireContext()));
        return dialog;
    }

    private int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text == null || text.trim().isEmpty() ? String.valueOf(fallback) : text.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private String safeTitle(Node node) {
        String title = node == null ? "" : node.getTitle();
        return title == null || title.trim().isEmpty() ? "(未选节点)" : title.trim();
    }
}
