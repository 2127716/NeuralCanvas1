package com.agui.neuralcanvas;

import android.app.Dialog;
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

    public static MemoryReviewDialog newInstance(Map<String, Node> nodes, Runnable callback) {
        currentNodes = nodes;
        onSaved = callback;
        return new MemoryReviewDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LearningLoopEngine.LearningReport learningReport = LearningLoopEngine.analyze(currentNodes, null);
        MemoryEngine.MemorySnapshot snapshot = MemoryEngine.build(currentNodes);

        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(ThemeManager.getDialogBg());
        LinearLayout root = MonetDialogStyler.buildRoot(requireContext());
        scroll.addView(root);

        TextView title = new TextView(requireContext());
        title.setText("间隔复习队列");
        root.addView(title);

        TextView sub = new TextView(requireContext());
        sub.setText("学习闭环、到期复习、记忆强度统一收口");
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = MonetDialogStyler.dp(requireContext(), 6);
        root.addView(sub, subLp);
        MonetDialogStyler.styleHeader(title, sub);

        TextView body = MonetDialogStyler.body(requireContext(), "");
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        body.setPadding(MonetDialogStyler.dp(requireContext(), 16), MonetDialogStyler.dp(requireContext(), 14),
                MonetDialogStyler.dp(requireContext(), 16), MonetDialogStyler.dp(requireContext(), 14));
        body.setBackground(MonetDialogStyler.cardBg());
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = MonetDialogStyler.dp(requireContext(), 14);
        root.addView(body, bodyLp);

        StringBuilder head = new StringBuilder();
        head.append(learningReport.buildSummary()).append("\n\n");

        if (snapshot.dueNodes.isEmpty()) {
            head.append("当前没有到期复习节点。")
                    .append("\n未来待复习：").append(snapshot.upcomingNodes.size()).append(" 个");
            body.setText(head.toString());
            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(scroll)
                    .setPositiveButton("关闭", null)
                    .create();
            dialog.setOnShowListener(d -> MonetDialogStyler.apply(dialog, requireContext()));
            return dialog;
        }

        final Node node = snapshot.dueNodes.get(0);
        LearningLoopEngine.ensureReviewAnchor(node);
        NodeIntelligenceEngine.markFocus(node);
        String content = node.getContent() == null || node.getContent().trim().isEmpty() ? "(无内容)" : node.getContent();
        head.append("当前复习：").append(safeTitle(node))
                .append("\n\n").append(content)
                .append("\n\n").append(MemoryEngine.getStatsText(node));
        body.setText(head.toString());

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(scroll)
                .setPositiveButton("Again", (d, w) -> apply(node, MemoryEngine.Grade.AGAIN))
                .setNeutralButton("Good", (d, w) -> apply(node, MemoryEngine.Grade.GOOD))
                .setNegativeButton("Easy", (d, w) -> apply(node, MemoryEngine.Grade.EASY))
                .create();
        dialog.setOnShowListener(d -> MonetDialogStyler.apply(dialog, requireContext()));
        return dialog;
    }

    private void apply(Node node, MemoryEngine.Grade grade) {
        MemoryEngine.review(node, grade);
        LearningLoopEngine.ensureReviewAnchor(node);
        OutcomeFeedbackEngine.markReviewed(node);
        if (onSaved != null) onSaved.run();
    }

    private String safeTitle(Node node) {
        String title = node == null ? "" : node.getTitle();
        return title == null || title.trim().isEmpty() ? "(无标题)" : title.trim();
    }
}
