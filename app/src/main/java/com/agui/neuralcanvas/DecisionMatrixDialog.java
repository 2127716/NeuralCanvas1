package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DecisionMatrixDialog extends DialogFragment {
    private static Node currentNode;
    private static Map<String, Node> currentNodes;
    private static Map<String, Connection> currentConnections;
    private static Runnable onSaved;

    public static DecisionMatrixDialog newInstance(Node node, Map<String, Node> nodes, Map<String, Connection> connections, Runnable callback) {
        currentNode = node;
        currentNodes = nodes;
        currentConnections = connections;
        onSaved = callback;
        return new DecisionMatrixDialog();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, requireContext().getResources().getDisplayMetrics());
    }

    private EditText numberInput(String value) {
        EditText et = new EditText(requireContext());
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setText(value == null ? "" : value);
        et.setTextColor(Color.parseColor("#0F172A"));
        return et;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (currentNode == null || currentNodes == null) return simple("当前没有可分析的节点。");
        final DecisionEngine.DecisionReport initial = DecisionEngine.analyze(currentNode, currentNodes, currentConnections);
        if (initial.options.isEmpty() || initial.criteria.isEmpty()) {
            return simple("请先在当前节点附近建立方案节点（OPTION）和准则节点（CRITERION），再打开评分面板。");
        }

        ScrollView scroll = new ScrollView(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(18);
        root.setPadding(p, p, p, p);
        scroll.addView(root);

        TextView section = new TextView(requireContext());
        section.setText("准则权重（建议 1~10）");
        section.setTextColor(Color.parseColor("#0F172A"));
        root.addView(section);

        final List<Node> criteria = new ArrayList<>(initial.criteria);
        final List<EditText> weightInputs = new ArrayList<>();
        for (Node criterion : criteria) {
            TextView label = new TextView(requireContext());
            label.setText("• " + safeTitle(criterion));
            label.setTextColor(Color.parseColor("#334155"));
            root.addView(label);
            EditText input = numberInput(String.valueOf(GraphMetaHelper.getFloat(criterion, "decision_weight", Math.max(1, criterion.getPriority()))));
            root.addView(input);
            weightInputs.add(input);
        }

        final List<Node> options = new ArrayList<>(initial.options);
        final List<List<EditText>> scoreInputs = new ArrayList<>();
        TextView section2 = new TextView(requireContext());
        section2.setText("方案评分（0~10）");
        section2.setTextColor(Color.parseColor("#0F172A"));
        section2.setPadding(0, dp(12), 0, 0);
        root.addView(section2);

        for (Node option : options) {
            TextView optionTitle = new TextView(requireContext());
            optionTitle.setText(safeTitle(option));
            optionTitle.setTextColor(Color.parseColor("#0F172A"));
            optionTitle.setTypeface(optionTitle.getTypeface(), android.graphics.Typeface.BOLD);
            root.addView(optionTitle);
            List<EditText> row = new ArrayList<>();
            for (Node criterion : criteria) {
                TextView label = new TextView(requireContext());
                label.setText("  - " + safeTitle(criterion));
                label.setTextColor(Color.parseColor("#475569"));
                root.addView(label);
                EditText input = numberInput(String.valueOf(GraphMetaHelper.getFloat(option, "score_" + criterion.getId(), 5f)));
                root.addView(input);
                row.add(input);
            }
            scoreInputs.add(row);
        }

        final TextView reportView = new TextView(requireContext());
        reportView.setTextColor(Color.parseColor("#0F172A"));
        reportView.setPadding(0, dp(14), 0, 0);
        reportView.setText(buildReportText(initial));
        root.addView(reportView);

        return new AlertDialog.Builder(requireContext())
                .setTitle("MCDA 决策评分")
                .setView(scroll)
                .setPositiveButton("保存并分析", (dialog, which) -> {
                    for (int i = 0; i < criteria.size(); i++) {
                        DecisionEngine.saveWeight(criteria.get(i), parseFloat(weightInputs.get(i).getText().toString(), Math.max(1f, criteria.get(i).getPriority())));
                    }
                    for (int i = 0; i < options.size(); i++) {
                        for (int j = 0; j < criteria.size(); j++) {
                            DecisionEngine.saveScore(options.get(i), criteria.get(j), parseFloat(scoreInputs.get(i).get(j).getText().toString(), 5f));
                        }
                    }
                    if (onSaved != null) onSaved.run();
                    DecisionEngine.DecisionReport refreshed = DecisionEngine.analyze(currentNode, currentNodes, currentConnections);
                    new AlertDialog.Builder(requireContext())
                            .setTitle("决策分析结果")
                            .setMessage(buildReportText(refreshed))
                            .setPositiveButton("确定", null)
                            .show();
                })
                .setNegativeButton("取消", null)
                .create();
    }

    private Dialog simple(String message) {
        return new AlertDialog.Builder(requireContext()).setTitle("决策评分").setMessage(message).setPositiveButton("关闭", null).create();
    }

    private float parseFloat(String text, float fallback) {
        try { return Float.parseFloat(text == null || text.trim().isEmpty() ? String.valueOf(fallback) : text.trim()); }
        catch (Exception e) { return fallback; }
    }

    private String safeTitle(Node node) {
        String title = node == null ? "" : node.getTitle();
        return title == null || title.trim().isEmpty() ? "(无标题)" : title.trim();
    }

    private String round(float value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private String buildReportText(DecisionEngine.DecisionReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("方案排名\n");
        for (int i = 0; i < report.rankings.size(); i++) {
            DecisionEngine.OptionScore item = report.rankings.get(i);
            sb.append(i + 1)
                    .append(". ")
                    .append(safeTitle(item.optionNode))
                    .append(" ｜总分=").append(round(item.finalScore))
                    .append(" ｜矩阵=").append(round(item.weightedScore))
                    .append(" ｜支持=").append(round(item.supportEvidence))
                    .append(" ｜反对=").append(round(item.opposeEvidence))
                    .append(" ｜风险=").append(round(item.riskPenalty))
                    .append("\n");
        }
        sb.append("\n稳健性：")
                .append(report.robustnessLabel)
                .append("（")
                .append(round(report.robustnessScore))
                .append("）\n");
        if (!report.warnings.isEmpty()) {
            sb.append("\n提醒\n");
            for (String w : report.warnings) sb.append("- ").append(w).append("\n");
        }
        return sb.toString();
    }
}
