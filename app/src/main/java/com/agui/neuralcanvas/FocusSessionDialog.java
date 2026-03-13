package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
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

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, requireContext().getResources().getDisplayMetrics());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(18);
        root.setPadding(p, p, p, p);

        TextView info = new TextView(requireContext());
        info.setTextColor(Color.parseColor("#0F172A"));
        info.setText("当前节点：" + safeTitle(currentNode) + "\n\n" + FocusSessionEngine.buildNodeSessionSummary(currentNode));
        root.addView(info);

        EditText minutesInput = new EditText(requireContext());
        minutesInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        minutesInput.setHint("计划专注分钟数，例如 25");
        minutesInput.setText("25");
        root.addView(minutesInput);

        return new AlertDialog.Builder(requireContext())
                .setTitle("深度工作 Session")
                .setView(root)
                .setPositiveButton("开始", (d, w) -> {
                    if (currentActivity == null || currentNode == null) return;
                    int minutes = parseInt(minutesInput.getText().toString(), 25);
                    currentActivity.startFocusSession(currentNode, minutes);
                })
                .setNeutralButton("完成当前Session", (d, w) -> {
                    if (currentActivity == null) return;
                    currentActivity.finishRunningFocusSession(false);
                })
                .setNegativeButton("中断当前Session", (d, w) -> {
                    if (currentActivity == null) return;
                    currentActivity.finishRunningFocusSession(true);
                })
                .create();
    }

    private int parseInt(String text, int fallback) {
        try { return Integer.parseInt(text == null || text.trim().isEmpty() ? String.valueOf(fallback) : text.trim()); }
        catch (Exception e) { return fallback; }
    }

    private String safeTitle(Node node) {
        String title = node == null ? "" : node.getTitle();
        return title == null || title.trim().isEmpty() ? "(未选节点)" : title.trim();
    }
}
