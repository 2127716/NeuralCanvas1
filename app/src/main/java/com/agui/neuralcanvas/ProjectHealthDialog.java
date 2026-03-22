package com.agui.neuralcanvas;

import android.app.Dialog;
import android.os.Bundle;
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

import java.util.List;

public class ProjectHealthDialog extends DialogFragment {

    public static ProjectHealthDialog newInstance() {
        return new ProjectHealthDialog();
    }

    private int dp(int value) {
        return MonetDialogStyler.dp(requireContext(), value);
    }

    private TextView title(String text, int sizeSp, boolean bold, boolean primary) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        tv.setTextColor(primary ? ThemeManager.getTextPrimary() : ThemeManager.getTextSecondary());
        if (bold) tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        return tv;
    }

    private View spacer(int hDp) {
        View v = new View(requireContext());
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(hDp)));
        return v;
    }

    private LinearLayout card(String title, String subtitle, int backgroundColor) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(backgroundColor);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), ThemeManager.getChipStroke());
        card.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = dp(4);
        lp.rightMargin = dp(4);
        card.setLayoutParams(lp);
        card.addView(this.title(title, 14, true, true));
        card.addView(spacer(4));
        card.addView(this.title(subtitle, 12, false, false));
        return card;
    }

    private TextView nodeRow(Node node, MainActivity activity) {
        TextView tv = new TextView(requireContext());
        String label = DashboardSectionBuilder.safe(node.getTitle());
        if (label.isEmpty()) label = "未命名节点";
        String extra = DashboardSectionBuilder.buildNodeExtra(node);
        String text = "• " + label + " [" + node.getType().label + "]";
        if (!DashboardSectionBuilder.safe(extra).isEmpty()) text += "\n  " + extra;
        tv.setText(text);
        tv.setTextColor(ThemeManager.getTextPrimary());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        tv.setBackground(MonetDialogStyler.cardBg());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        tv.setLayoutParams(lp);
        tv.setOnClickListener(v -> {
            activity.getMindMapView().focusNodeById(node.getId());
            activity.getMindMapView().selectOnlyNode(node.getId());
            dismiss();
        });
        return tv;
    }

    private void addSection(LinearLayout root, String sectionTitle, List<Node> nodes, MainActivity activity) {
        root.addView(title(sectionTitle, 16, true, true));
        if (nodes == null || nodes.isEmpty()) {
            root.addView(title("当前没有内容", 13, false, false));
            root.addView(spacer(16));
            return;
        }
        int limit = Math.min(nodes.size(), 6);
        for (int i = 0; i < limit; i++) root.addView(nodeRow(nodes.get(i), activity));
        if (nodes.size() > limit) root.addView(title("还有 " + (nodes.size() - limit) + " 个", 12, false, false));
        root.addView(spacer(16));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null || activity.getMindMapView() == null) return super.onCreateDialog(savedInstanceState);

        ProjectHealthEngine.ProjectHealthReport report = ProjectHealthEngine.analyze(
                activity.getMindMapView().getNodesInternal(),
                activity.getMindMapView().getConnectionsInternal()
        );

        ScrollView sv = new ScrollView(requireContext());
        sv.setFillViewport(true);
        sv.setBackgroundColor(ThemeManager.getDialogBg());

        LinearLayout root = MonetDialogStyler.buildRoot(requireContext());
        sv.addView(root);

        TextView pageTitle = new TextView(requireContext());
        pageTitle.setText("项目巡检");
        root.addView(pageTitle);

        TextView sub = new TextView(requireContext());
        sub.setText(ProjectHealthEngine.buildSummary(report));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(6);
        root.addView(sub, subLp);
        MonetDialogStyler.styleHeader(pageTitle, sub);

        HorizontalScrollView hsv = new HorizontalScrollView(requireContext());
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(card("卡住", String.valueOf(report.stuckProjects.size()), 0xFF7F1D1D));
        row.addView(card("缺 KR", String.valueOf(report.projectsWithoutKr.size()), 0xFF1D4ED8));
        row.addView(card("缺复盘", String.valueOf(report.projectsWithoutReview.size()), 0xFF92400E));
        row.addView(card("无触发", String.valueOf(report.actionsWithoutTrigger.size()), 0xFF0F766E));
        row.addView(card("弱证据", String.valueOf(report.weakEvidenceDecisions.size()), 0xFF581C87));
        hsv.addView(row);
        LinearLayout.LayoutParams hsvLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hsvLp.topMargin = dp(14);
        root.addView(hsv, hsvLp);
        root.addView(spacer(18));

        addSection(root, "卡住项目", report.stuckProjects, activity);
        addSection(root, "缺 KR 项目", report.projectsWithoutKr, activity);
        addSection(root, "缺复盘项目", report.projectsWithoutReview, activity);
        addSection(root, "无触发条件动作", report.actionsWithoutTrigger, activity);
        addSection(root, "高置信低证据决策", report.weakEvidenceDecisions, activity);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(sv)
                .setPositiveButton("自动补强", (d, w) -> {
                    WorkflowQuickFixEngine.FixResult fixResult = WorkflowQuickFixEngine.quickFixProjectHealth(activity, report);
                    android.widget.Toast.makeText(activity, fixResult.buildSummary(), android.widget.Toast.LENGTH_LONG).show();
                    FocusGuideDialog.show(activity, NodeFocusGuideEngine.buildForFix(
                            activity,
                            !report.stuckProjects.isEmpty() ? report.stuckProjects.get(0)
                                    : (!report.projectsWithoutKr.isEmpty() ? report.projectsWithoutKr.get(0)
                                    : (!report.actionsWithoutTrigger.isEmpty() ? report.actionsWithoutTrigger.get(0)
                                    : null)),
                            fixResult
                    ));
                })
                .setNegativeButton("关闭", null)
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
