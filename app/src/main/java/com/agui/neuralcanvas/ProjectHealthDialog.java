package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.Color;
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
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                requireContext().getResources().getDisplayMetrics()
        );
    }

    private android.graphics.drawable.GradientDrawable bg(String color) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Color.parseColor(color));
        gd.setCornerRadius(dp(16));
        gd.setStroke(dp(1), Color.parseColor("#24324A"));
        return gd;
    }

    private TextView title(String text, int sizeSp, boolean bold, String color) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        tv.setTextColor(Color.parseColor(color));
        if (bold) tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        return tv;
    }

    private View spacer(int hDp) {
        View v = new View(requireContext());
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(hDp)
        ));
        return v;
    }

    private LinearLayout card(String title, String subtitle, String color) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackground(bg(color));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = dp(4);
        lp.rightMargin = dp(4);
        card.setLayoutParams(lp);
        card.addView(this.title(title, 14, true, "#F8FAFC"));
        card.addView(spacer(4));
        card.addView(this.title(subtitle, 12, false, "#D6E3F5"));
        return card;
    }

    private TextView nodeRow(Node node, MainActivity activity) {
        TextView tv = new TextView(requireContext());
        String label = DashboardSectionBuilder.safe(node.getTitle());
        if (label.isEmpty()) label = "未命名节点";
        String extra = DashboardSectionBuilder.buildNodeExtra(node);
        String text = "• " + label + " [" + node.getType().label + "]";
        if (!DashboardSectionBuilder.safe(extra).isEmpty()) {
            text += "\n  " + extra;
        }
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#E2E8F0"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        tv.setBackground(bg("#111827"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(8);
        tv.setLayoutParams(lp);
        tv.setOnClickListener(v -> {
            activity.getMindMapView().focusNodeById(node.getId());
            activity.getMindMapView().selectNodeById(node.getId());
            dismiss();
        });
        return tv;
    }

    private void addSection(LinearLayout root, String sectionTitle, List<Node> nodes, MainActivity activity) {
        root.addView(title(sectionTitle, 16, true, "#F8FAFC"));
        if (nodes == null || nodes.isEmpty()) {
            root.addView(title("当前没有内容", 13, false, "#94A3B8"));
            root.addView(spacer(16));
            return;
        }
        int limit = Math.min(nodes.size(), 8);
        for (int i = 0; i < limit; i++) {
            root.addView(nodeRow(nodes.get(i), activity));
        }
        if (nodes.size() > limit) {
            root.addView(title("还有 " + (nodes.size() - limit) + " 个", 12, false, "#94A3B8"));
        }
        root.addView(spacer(16));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null || activity.getMindMapView() == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        ProjectHealthEngine.ProjectHealthReport report = ProjectHealthEngine.analyze(
                activity.getMindMapView().getNodesInternal(),
                activity.getMindMapView().getConnectionsInternal()
        );

        ScrollView sv = new ScrollView(requireContext());
        sv.setFillViewport(true);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.parseColor("#0B1020"));
        sv.addView(root);

        root.addView(title("项目巡检", 20, true, "#F8FAFC"));
        root.addView(spacer(6));
        root.addView(title(ProjectHealthEngine.buildSummary(report), 13, false, "#94A3B8"));
        root.addView(spacer(14));

        HorizontalScrollView hsv = new HorizontalScrollView(requireContext());
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(card("项目", String.valueOf(report.projects.size()), "#1E293B"));
        row.addView(card("卡住", String.valueOf(report.stuckProjects.size()), "#7F1D1D"));
        row.addView(card("缺 KR", String.valueOf(report.projectsWithoutKr.size()), "#1D4ED8"));
        row.addView(card("缺复盘", String.valueOf(report.projectsWithoutReview.size()), "#92400E"));
        row.addView(card("逾期动作", String.valueOf(report.overdueActions.size()), "#7C2D12"));
        row.addView(card("无触发", String.valueOf(report.actionsWithoutTrigger.size()), "#0F766E"));
        row.addView(card("弱证据决策", String.valueOf(report.weakEvidenceDecisions.size()), "#581C87"));
        row.addView(card("待复习学习", String.valueOf(report.staleLearningNodes.size()), "#14532D"));
        hsv.addView(row);
        root.addView(hsv);
        root.addView(spacer(18));

        addSection(root, "卡住项目", report.stuckProjects, activity);
        addSection(root, "缺 KR 项目", report.projectsWithoutKr, activity);
        addSection(root, "缺复盘项目", report.projectsWithoutReview, activity);
        addSection(root, "逾期动作", report.overdueActions, activity);
        addSection(root, "无触发条件动作", report.actionsWithoutTrigger, activity);
        addSection(root, "高置信低证据决策", report.weakEvidenceDecisions, activity);
        addSection(root, "待安排复习的学习节点", report.staleLearningNodes, activity);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(sv)
                .setPositiveButton("关闭", null)
                .create();

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                dialog.getWindow().setGravity(Gravity.CENTER);
            }
        });
        return dialog;
    }
}
