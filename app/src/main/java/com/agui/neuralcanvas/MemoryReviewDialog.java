package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.Map;

public class MemoryReviewDialog extends DialogFragment {
    private static Map<String, Node> currentNodes;
    private static Runnable onSaved;
    public static MemoryReviewDialog newInstance(Map<String, Node> nodes, Runnable callback) { currentNodes = nodes; onSaved = callback; return new MemoryReviewDialog(); }
    private int dp(int value) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, requireContext().getResources().getDisplayMetrics()); }

    @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        MemoryEngine.MemorySnapshot snapshot = MemoryEngine.build(currentNodes);
        ScrollView scroll = new ScrollView(requireContext());
        LinearLayout root = new LinearLayout(requireContext()); root.setOrientation(LinearLayout.VERTICAL); int p = dp(18); root.setPadding(p,p,p,p); scroll.addView(root);
        TextView tv = new TextView(requireContext()); tv.setTextColor(Color.parseColor("#0F172A")); root.addView(tv);
        if (snapshot.dueNodes.isEmpty()) {
            tv.setText("当前没有到期复习节点。
未来待复习：" + snapshot.upcomingNodes.size() + " 个");
            return new AlertDialog.Builder(requireContext()).setTitle("间隔复习队列").setView(scroll).setPositiveButton("关闭", null).create();
        }
        final Node node = snapshot.dueNodes.get(0);
        tv.setText("当前复习：" + safeTitle(node) + "

" + (node.getContent() == null || node.getContent().trim().isEmpty() ? "(无内容)" : node.getContent()) + "

" + MemoryEngine.getStatsText(node));
        return new AlertDialog.Builder(requireContext())
                .setTitle("间隔复习队列")
                .setView(scroll)
                .setPositiveButton("Again", (d, w) -> apply(node, MemoryEngine.Grade.AGAIN))
                .setNeutralButton("Good", (d, w) -> apply(node, MemoryEngine.Grade.GOOD))
                .setNegativeButton("Easy", (d, w) -> apply(node, MemoryEngine.Grade.EASY))
                .create();
    }
    private void apply(Node node, MemoryEngine.Grade grade) { MemoryEngine.review(node, grade); if (onSaved != null) onSaved.run(); }
    private String safeTitle(Node node) { String title = node == null ? "" : node.getTitle(); return title == null || title.trim().isEmpty() ? "(无标题)" : title.trim(); }
}
