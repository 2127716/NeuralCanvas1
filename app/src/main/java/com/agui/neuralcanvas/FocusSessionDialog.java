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

import java.util.Map;

public class FocusSessionDialog extends DialogFragment {
    private static Node currentNode;
    private static Map<String, Node> currentNodes;
    private static Runnable onSaved;

    public static FocusSessionDialog newInstance(Node node, Map<String, Node> nodes, Runnable callback) {
        currentNode = node;
        currentNodes = nodes;
        onSaved = callback;
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
        root.addView(info);

        EditText minutesInput = new EditText(requireContext());
        minutesInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        minutesInput.setHint("计划分钟数，例如 25");
        minutesInput.setText("25");
        root.addView(minutesInput);

        if (currentNode != null) {
            TextView stats = new TextView(requireContext());
            stats.setTextColor(Color.parseColor("#475569"));
            stats.setText(FocusSessionEngine.getNodeStats(currentNode) + "
" + FocusSessionEngine.getTriggerStats(currentNode));
            root.addView(stats);
        }

        FocusSessionEngine.SessionInfo current = FocusSessionEngine.getCurrent(requireContext());
        if (current == null) {
            info.setText("当前没有进行中的 Session。
当前节点：" + safeTitle(currentNode));
            return new AlertDialog.Builder(requireContext())
                    .setTitle("深度工作 Session")
                    .setView(root)
                    .setPositiveButton("开始", (d, w) -> {
                        FocusSessionEngine.start(requireContext(), currentNode, parseInt(minutesInput.getText().toString(), 25));
                        FocusSessionEngine.markTrigger(currentNode, true);
                        if (onSaved != null) onSaved.run();
                    })
                    .setNeutralButton("触发落空", (d, w) -> {
                        FocusSessionEngine.markTrigger(currentNode, false);
                        if (onSaved != null) onSaved.run();
                    })
                    .setNegativeButton("取消", null)
                    .create();
        }

        long elapsed = Math.max(0L, (System.currentTimeMillis() - current.startedAt) / 60000L);
        info.setText("进行中：" + current.nodeTitle + "
已进行 " + elapsed + " 分钟｜计划 " + current.plannedMinutes + " 分钟｜中断 " + current.interruptions + " 次");
        return new AlertDialog.Builder(requireContext())
                .setTitle("深度工作 Session")
                .setView(root)
                .setPositiveButton("完成", (d, w) -> {
                    FocusSessionEngine.finish(requireContext(), currentNodes, true);
                    if (onSaved != null) onSaved.run();
                })
                .setNeutralButton("中断一次", (d, w) -> {
                    FocusSessionEngine.interrupt(requireContext());
                    if (onSaved != null) onSaved.run();
                })
                .setNegativeButton("放弃", (d, w) -> {
                    FocusSessionEngine.finish(requireContext(), currentNodes, false);
                    if (onSaved != null) onSaved.run();
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
