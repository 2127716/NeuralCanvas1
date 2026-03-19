
package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import java.util.Map;

public class ScientificDashboardDialog extends DialogFragment {

    public static ScientificDashboardDialog newInstance() {
        return new ScientificDashboardDialog();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, requireContext().getResources().getDisplayMetrics()
        );
    }

    private android.graphics.drawable.GradientDrawable rounded(String color) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Color.parseColor(color));
        gd.setCornerRadius(dp(16));
        gd.setStroke(dp(1), Color.parseColor("#24324A"));
        return gd;
    }

    private TextView label(String text, int sizeSp, boolean bold, String color) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(Color.parseColor(color));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        if (bold) tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        return tv;
    }

    private View spacer(int hDp) {
        View v = new View(requireContext());
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(hDp)));
        return v;
    }

    private TextView actionChip(String text, Runnable onClick) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#E2E8F0"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setPadding(dp(14), dp(10), dp(14), dp(10));
        tv.setBackground(rounded("#111827"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(8);
        tv.setLayoutParams(lp);
        tv.setOnClickListener(v -> onClick.run());
        return tv;
    }

    private TextView buildNodeRow(final Node node, final MainActivity activity) {
        TextView tv = new TextView(requireContext());
        String title = DashboardSectionBuilder.safe(node.getTitle());
        if (title.isEmpty()) title = "未命名节点";
        String type = node.getType() == null ? "" : node.getType().label;

        String line = "• " + title + "  [" + type + "]";
        String extra = DashboardSectionBuilder.buildNodeExtra(node);
        if (!TextUtils.isEmpty(extra)) line += "\n  " + extra;

        tv.setText(line);
        tv.setTextColor(Color.parseColor("#E2E8F0"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        tv.setBackground(rounded("#111827"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        tv.setLayoutParams(lp);

        tv.setOnClickListener(v -> {
            activity.getMindMapView().focusNodeById(node.getId());
            activity.getMindMapView().selectNodeById(node.getId());
            dismiss();
        });

        tv.setOnLongClickListener(v -> {
            activity.getMindMapView().selectOnlyNode(node.getId());
            WorkflowQuickFixEngine.FixResult result = WorkflowQuickFixEngine.quickFixNode(activity, node);
            FocusGuideDialog.show(activity, NodeFocusGuideEngine.buildForFix(activity, node, result));
            dismiss();
            return true;
        });
        return tv;
    }

    private void addSection(LinearLayout root, String title, java.util.List<Node> nodes, MainActivity activity) {
        root.addView(label(title, 16, true, "#F8FAFC"));
        if (nodes == null || nodes.isEmpty()) {
            root.addView(label("当前没有内容", 13, false, "#94A3B8"));
            root.addView(spacer(16));
            return;
        }
        int limit = Math.min(nodes.size(), 6);
        for (int i = 0; i < limit; i++) root.addView(buildNodeRow(nodes.get(i), activity));
        if (nodes.size() > limit) root.addView(label("还有 " + (nodes.size() - limit) + " 个", 12, false, "#94A3B8"));
        root.addView(spacer(16));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null || activity.getMindMapView() == null) return super.onCreateDialog(savedInstanceState);

        Map<String, Node> nodes = activity.getMindMapView().getNodesInternal();
        Map<String, Connection> connections = activity.getMindMapView().getConnectionsInternal();
        DashboardSectionBuilder.DashboardData data = DashboardSectionBuilder.build(nodes);
        ProjectHealthEngine.ProjectHealthReport projectHealth = ProjectHealthEngine.analyze(nodes, connections);

        Node selectedNode = null;
        java.util.List<String> selectedIds = activity.getMindMapView().getSelectedNodeIds();
        if (selectedIds != null && !selectedIds.isEmpty()) {
            selectedNode = nodes.get(selectedIds.get(0));
        }
        AutonomousBrainEngine.BrainReport brain = AutonomousBrainEngine.analyze(nodes, connections, selectedNode);

        ScrollView sv = new ScrollView(requireContext());
        sv.setFillViewport(true);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.parseColor("#0B1020"));
        sv.addView(root);

        root.addView(label("智能工作台", 20, true, "#F8FAFC"));
        TextView sub = label("减少按钮，把分析、修复、引导合成一条自动链", 13, false, "#94A3B8");
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(6);
        sub.setLayoutParams(subLp);
        root.addView(sub);
        root.addView(spacer(14));

        TextView brainCard = new TextView(requireContext());
        brainCard.setText(AutonomousBrainEngine.buildReadableSummary(brain));
        brainCard.setTextColor(Color.parseColor("#E2E8F0"));
        brainCard.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        brainCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        brainCard.setBackground(rounded("#111827"));
        root.addView(brainCard);

        root.addView(spacer(12));
        HorizontalScrollView hsv = new HorizontalScrollView(requireContext());
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(actionChip("自动巡航", () -> {
            if (!brain.agenda.isEmpty()) {
                AutonomousBrainEngine.BrainAgendaItem item = brain.agenda.get(0);
                WorkflowMethodRecommendationEngine.execute(activity, item.node, item.actionId);
                dismiss();
            }
        }));
        row.addView(actionChip("项目巡检", () -> {
            ProjectHealthDialog.newInstance().show(activity.getSupportFragmentManager(), "project_health_dialog");
            dismiss();
        }));
        row.addView(actionChip("AI 助手", () -> {
            String preset = brain.headline + "\n请基于当前图谱给出保守、可执行、少而精的补强建议。";
            activity.showAiAssistantDialogWithPrompt(preset);
            dismiss();
        }));
        hsv.addView(row);
        root.addView(hsv);
        root.addView(spacer(18));

        addSection(root, "最值得先处理", projectHealth.stuckProjects.isEmpty() ? data.todayNodes : projectHealth.stuckProjects, activity);
        addSection(root, "待复盘 / 待复习", data.reviewNodes, activity);
        addSection(root, "高风险 / 受阻", data.riskNodes, activity);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(sv)
                .setPositiveButton("关闭", null)
                .create();

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.getWindow().setGravity(Gravity.CENTER);
            }
        });

        return dialog;
    }
}
