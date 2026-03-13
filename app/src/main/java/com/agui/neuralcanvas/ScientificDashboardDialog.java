package com.agui.neuralcanvas;

import android.app.Dialog;
import android.content.Context;
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
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                requireContext().getResources().getDisplayMetrics()
        );
    }

    private int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }

    private android.graphics.drawable.GradientDrawable createRoundedDrawable(String color) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Color.parseColor(color));
        gd.setCornerRadius(dp(16));
        gd.setStroke(dp(1), Color.parseColor("#24324A"));
        return gd;
    }

    private TextView buildTitle(String text, int sizeSp, boolean bold, String color) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(Color.parseColor(color));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        if (bold) {
            tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        }
        return tv;
    }

    private View buildSpacer(int heightDp) {
        View view = new View(requireContext());
        view.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(heightDp)
        ));
        return view;
    }

    private LinearLayout buildCard(String title, String subtitle, String bgColor) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        lp.leftMargin = dp(4);
        lp.rightMargin = dp(4);
        card.setLayoutParams(lp);
        card.setBackground(createRoundedDrawable(bgColor));

        TextView t1 = buildTitle(title, 15, true, "#F8FAFC");
        TextView t2 = buildTitle(subtitle, 12, false, "#D6E3F5");

        card.addView(t1);
        card.addView(buildSpacer(6));
        card.addView(t2);

        return card;
    }

    private void refreshSelf() {
        dismiss();
        new ScientificDashboardDialog().show(requireActivity().getSupportFragmentManager(), "scientific_dashboard");
    }

    private TextView buildNodeRow(final Node node, final MainActivity activity) {
        TextView tv = new TextView(requireContext());
        String title = DashboardSectionBuilder.safe(node.getTitle());
        if (title.isEmpty()) title = "未命名节点";
        String type = node.getType() == null ? "" : node.getType().label;

        String line = "• " + title + "  [" + type + "]";
        String extra = DashboardSectionBuilder.buildNodeExtra(node);
        if (!TextUtils.isEmpty(extra)) {
            line += "\n  " + extra;
        }

        tv.setText(line);
        tv.setTextColor(Color.parseColor("#E2E8F0"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        tv.setBackground(createRoundedDrawable("#111827"));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(8);
        tv.setLayoutParams(lp);

        tv.setOnClickListener(v -> {
            activity.getMindMapView().focusNodeById(node.getId());
            activity.getMindMapView().selectNodeById(node.getId());
            dismiss();
        });

        tv.setOnLongClickListener(v -> {
            FragmentActivity fa = getActivity();
            if (fa == null) return true;

            Runnable refresh = this::refreshSelf;

            if (DashboardSectionBuilder.isInboxNode(node)) {
                DashboardActionDialogHelper.showInboxClarifyDialog(fa, node, refresh);
                return true;
            }

            if (DashboardSectionBuilder.isDecisionNode(node)) {
                DashboardActionDialogHelper.askGenerateDecisionStarter(fa, node, refresh);
                return true;
            }

            if (DashboardSectionBuilder.isLearningNode(node)) {
                DashboardActionDialogHelper.askGenerateLearningStarter(fa, node, refresh);
                return true;
            }

            if (DashboardSectionBuilder.isKrNode(node)) {
                DashboardActionDialogHelper.showKrQuickUpdateDialog(fa, node, refresh);
                return true;
            }

            if (DashboardSectionBuilder.isExecutionNode(node) || DashboardSectionBuilder.isReviewNode(node)) {
                DashboardActionDialogHelper.showMarkDoneDialog(fa, node, refresh);
                return true;
            }

            return true;
        });

        return tv;
    }

    private void addSection(LinearLayout root, String title, java.util.List<Node> nodes, MainActivity activity) {
        TextView sectionTitle = buildTitle(title, 16, true, "#F8FAFC");
        root.addView(sectionTitle);

        if (nodes.isEmpty()) {
            TextView empty = buildTitle("当前没有内容", 13, false, "#94A3B8");
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.topMargin = dp(8);
            empty.setLayoutParams(lp);
            root.addView(empty);
            root.addView(buildSpacer(18));
            return;
        }

        int limit = Math.min(nodes.size(), 8);
        for (int i = 0; i < limit; i++) {
            root.addView(buildNodeRow(nodes.get(i), activity));
        }

        if (nodes.size() > limit) {
            TextView more = buildTitle("还有 " + (nodes.size() - limit) + " 个", 12, false, "#94A3B8");
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.topMargin = dp(8);
            more.setLayoutParams(lp);
            root.addView(more);
        }

        root.addView(buildSpacer(18));
    }

    private View buildWorkflowSummarySection(Context context,
                                             Map<String, Node> nodes,
                                             Map<String, Connection> connections) {
        WorkflowSnapshot snapshot = WorkflowSnapshot.from(nodes, connections);

        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(context, 12);
        section.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(context);
        title.setText("工作流概览");
        title.setTextSize(16f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        section.addView(title);

        TextView content = new TextView(context);
        content.setText(
                "Inbox： " + snapshot.inboxCount + "\n" +
                "项目： " + snapshot.projectCount + "\n" +
                "下一步动作： " + snapshot.nextActionCount + "\n" +
                "待复盘： " + snapshot.reviewDueCount + "\n" +
                "卡住项目： " + snapshot.stuckProjectCount
        );
        content.setTextSize(14f);
        content.setPadding(0, dp(context, 8), 0, 0);
        section.addView(content);

        TextView nextTitle = new TextView(context);
        nextTitle.setText("下一步建议");
        nextTitle.setTextSize(15f);
        nextTitle.setTextColor(Color.parseColor("#F8FAFC"));
        nextTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        nextTitle.setPadding(0, dp(context, 14), 0, dp(context, 6));
        section.addView(nextTitle);

        TextView nextContent = new TextView(context);
        nextContent.setText(WorkflowRecommendationEngine.buildSummary(nodes, connections));
        nextContent.setTextSize(13f);
        nextContent.setTextColor(Color.parseColor("#D6E3F5"));
        section.addView(nextContent);

        return section;
    }



    private View buildTriageSection(Context context, Map<String, Node> nodes, Map<String, Connection> connections) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(context, 12);
        section.setPadding(padding, padding, padding, padding);
        section.setBackground(createRoundedDrawable("#111827"));

        TextView title = new TextView(context);
        title.setText("节点体检");
        title.setTextSize(16f);
        title.setTextColor(Color.parseColor("#F8FAFC"));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        section.addView(title);

        Node weakest = null;
        ScientificTriageEngine.TriageReport weakestReport = null;
        if (nodes != null) {
            for (Node node : nodes.values()) {
                if (node == null) continue;
                ScientificTriageEngine.TriageReport report = ScientificTriageEngine.analyze(node, nodes, connections);
                if (weakestReport == null || report.healthScore < weakestReport.healthScore) {
                    weakest = node;
                    weakestReport = report;
                }
            }
        }

        TextView content = new TextView(context);
        content.setTextColor(Color.parseColor("#D6E3F5"));
        content.setTextSize(13f);
        if (weakest == null || weakestReport == null) {
            content.setText("当前没有可体检的节点");
        } else {
            content.setText("最需要处理：" + DashboardSectionBuilder.safe(weakest.getTitle()) + "
" + ScientificTriageEngine.buildSummary(weakestReport));
        }
        content.setPadding(0, dp(context, 8), 0, 0);
        section.addView(content);
        return section;
    }

    private View buildAnalyticsSection(Context context, Map<String, Node> nodes, Map<String, Connection> connections) {
        WorkflowAnalyticsEngine.AnalyticsReport report = WorkflowAnalyticsEngine.build(nodes);

        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(context, 12);
        section.setPadding(padding, padding, padding, padding);
        section.setBackground(createRoundedDrawable("#111827"));

        TextView title = new TextView(context);
        title.setText("科学反馈数据");
        title.setTextSize(16f);
        title.setTextColor(Color.parseColor("#F8FAFC"));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        section.addView(title);

        TextView content = new TextView(context);
        content.setText(WorkflowAnalyticsEngine.buildReadableSummary(report));
        content.setTextSize(14f);
        content.setTextColor(Color.parseColor("#D6E3F5"));
        content.setPadding(0, dp(context, 8), 0, 0);
        section.addView(content);

        return section;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null || activity.getMindMapView() == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        Map<String, Node> nodes = activity.getMindMapView().getNodesInternal();
        Map<String, Connection> connections = activity.getMindMapView().getConnectionsInternal();
        DashboardSectionBuilder.DashboardData data =
                DashboardSectionBuilder.build(nodes);
        java.util.List<Node> areaNodes = WorkflowEngine.getAreaNodes(nodes);
        java.util.List<Node> resourceNodes = WorkflowEngine.getResourceNodes(nodes);
        java.util.List<Node> archivedNodes = WorkflowEngine.getArchivedNodes(nodes);

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.parseColor("#0B1020"));
        scrollView.addView(root);

        TextView title = buildTitle("科学工作台", 20, true, "#F8FAFC");
        root.addView(title);

        TextView subtitle = buildTitle("已补上 workflow / quick action / PARA 收口", 13, false, "#94A3B8");
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subLp.topMargin = dp(6);
        subtitle.setLayoutParams(subLp);
        root.addView(subtitle);

        root.addView(buildSpacer(16));

        // 添加工作流概览卡片
        root.addView(buildWorkflowSummarySection(requireContext(), nodes, connections));
        root.addView(buildSpacer(12));
        root.addView(buildTriageSection(requireContext(), nodes, connections));
        root.addView(buildSpacer(12));
        root.addView(buildAnalyticsSection(requireContext(), nodes, connections));
        root.addView(buildSpacer(16));

        HorizontalScrollView hsv = new HorizontalScrollView(requireContext());
        hsv.setHorizontalScrollBarEnabled(false);

        LinearLayout cardRow = new LinearLayout(requireContext());
        cardRow.setOrientation(LinearLayout.HORIZONTAL);
        cardRow.addView(buildCard("Inbox", String.valueOf(data.inboxNodes.size()), "#1E293B"));
        cardRow.addView(buildCard("今日执行", String.valueOf(data.todayNodes.size()), "#0F766E"));
        cardRow.addView(buildCard("待复盘", String.valueOf(data.reviewNodes.size()), "#92400E"));
        cardRow.addView(buildCard("风险/受阻", String.valueOf(data.riskNodes.size()), "#7F1D1D"));
        cardRow.addView(buildCard("KR", String.valueOf(data.krNodes.size()), "#1D4ED8"));
        cardRow.addView(buildCard("Areas", String.valueOf(areaNodes.size()), "#3730A3"));
        cardRow.addView(buildCard("Resources", String.valueOf(resourceNodes.size()), "#14532D"));
        cardRow.addView(buildCard("Archives", String.valueOf(archivedNodes.size()), "#334155"));
        hsv.addView(cardRow);
        root.addView(hsv);

        root.addView(buildSpacer(18));

        addSection(root, "Inbox 待澄清", data.inboxNodes, activity);
        addSection(root, "今日最值得推进（已排序）", data.todayNodes, activity);
        addSection(root, "待复盘 / 待复习", data.reviewNodes, activity);
        addSection(root, "高风险 / 受阻节点", data.riskNodes, activity);
        addSection(root, "关键结果（KR）", data.krNodes, activity);
        addSection(root, "Areas 领域归属", areaNodes, activity);
        addSection(root, "Resources 资源池", resourceNodes, activity);
        addSection(root, "Archives 已归档", archivedNodes, activity);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(scrollView)
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
