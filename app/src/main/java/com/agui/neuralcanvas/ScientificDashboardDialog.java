package com.agui.neuralcanvas;

import android.app.Dialog;
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

import java.util.Map;

public class ScientificDashboardDialog extends DialogFragment {

    public static ScientificDashboardDialog newInstance() {
        return new ScientificDashboardDialog();
    }

    private int dp(int value) {
        return MonetDialogStyler.dp(requireContext(), value);
    }

    private TextView label(String text, int sizeSp, boolean bold, boolean primary) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(primary ? ThemeManager.getTextPrimary() : ThemeManager.getTextSecondary());
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
        tv.setTextColor(ThemeManager.getTextPrimary());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setPadding(dp(14), dp(10), dp(14), dp(10));
        tv.setBackground(MonetDialogStyler.cardBg());
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
        tv.setTextColor(ThemeManager.getTextPrimary());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        tv.setBackground(MonetDialogStyler.cardBg());
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
        root.addView(label(title, 16, true, true));
        if (nodes == null || nodes.isEmpty()) {
            root.addView(label("当前没有内容", 13, false, false));
            root.addView(spacer(16));
            return;
        }
        int limit = Math.min(nodes.size(), 6);
        for (int i = 0; i < limit; i++) root.addView(buildNodeRow(nodes.get(i), activity));
        if (nodes.size() > limit) root.addView(label("还有 " + (nodes.size() - limit) + " 个", 12, false, false));
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
        sv.setBackgroundColor(ThemeManager.getDialogBg());

        LinearLayout root = MonetDialogStyler.buildRoot(requireContext());
        sv.addView(root);

        TextView title = new TextView(requireContext());
        title.setText("智能工作台");
        root.addView(title);

        TextView sub = new TextView(requireContext());
        sub.setText("减少按钮，把分析、修复、引导合成一条自动链");
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(6);
        sub.setLayoutParams(subLp);
        root.addView(sub);
        MonetDialogStyler.styleHeader(title, sub);

        TextView brainCard = MonetDialogStyler.body(requireContext(), AutonomousBrainEngine.buildReadableSummary(brain));
        brainCard.setTextColor(ThemeManager.getTextPrimary());
        brainCard.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        brainCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        brainCard.setBackground(MonetDialogStyler.cardBg());
        LinearLayout.LayoutParams brainLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        brainLp.topMargin = dp(14);
        root.addView(brainCard, brainLp);

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
            MonetDialogStyler.apply(dialog, requireContext());
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.getWindow().setGravity(Gravity.CENTER);
            }
        });

        return dialog;
    }
}
