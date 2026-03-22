package com.agui.neuralcanvas;

import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.List;

public class ProjectsHubDialog extends DialogFragment {

    public static ProjectsHubDialog newInstance() {
        return new ProjectsHubDialog();
    }

    private TextView makeText(String text, float sp, boolean bold) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(sp);
        tv.setTextColor(bold ? ThemeManager.getTextPrimary() : ThemeManager.getTextSecondary());
        if (bold) tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        return tv;
    }

    private TextView buildNodeChip(Node node, MainActivity activity) {
        TextView tv = makeText(ProjectsHubBuilder.buildNodeChipText(node), 13, false);
        tv.setPadding(MonetDialogStyler.dp(requireContext(), 12), MonetDialogStyler.dp(requireContext(), 10),
                MonetDialogStyler.dp(requireContext(), 12), MonetDialogStyler.dp(requireContext(), 10));
        tv.setBackground(MonetDialogStyler.cardBg());

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = MonetDialogStyler.dp(requireContext(), 8);
        tv.setLayoutParams(lp);

        tv.setOnClickListener(v -> {
            if (activity != null && activity.getMindMapView() != null) {
                activity.getMindMapView().focusNodeById(node.getId());
                activity.getMindMapView().selectNodeById(node.getId());
                dismiss();
            }
        });
        return tv;
    }

    private void addSection(LinearLayout root, String title, List<Node> nodes, MainActivity activity) {
        TextView titleView = makeText(title, 14, true);
        root.addView(titleView);
        if (nodes.isEmpty()) {
            TextView empty = makeText("暂无", 12, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = MonetDialogStyler.dp(requireContext(), 6);
            empty.setLayoutParams(lp);
            root.addView(empty);
            return;
        }
        int limit = Math.min(5, nodes.size());
        for (int i = 0; i < limit; i++) root.addView(buildNodeChip(nodes.get(i), activity));
    }

    private LinearLayout buildProjectCard(ProjectsHubBuilder.ProjectGroup group, MainActivity activity) {
        LinearLayout card = MonetDialogStyler.card(requireContext(),
                "项目｜" + (ProjectsHubBuilder.safe(group.projectNode.getTitle()).isEmpty() ? "未命名项目" : ProjectsHubBuilder.safe(group.projectNode.getTitle())),
                "目标 " + group.goals.size() + " ｜ 动作 " + group.actions.size() + " ｜ KR " + group.krs.size()
                        + " ｜ 风险 " + group.risks.size() + " ｜ 复盘 " + group.reviews.size());

        String content = ProjectsHubBuilder.safe(group.projectNode.getContent());
        if (!content.isEmpty()) {
            TextView contentView = makeText(content, 13, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = MonetDialogStyler.dp(requireContext(), 8);
            card.addView(contentView, lp);
        }

        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gap.topMargin = MonetDialogStyler.dp(requireContext(), 12);

        TextView sec = makeText("项目工作流", 14, true);
        sec.setLayoutParams(gap);
        card.addView(sec);
        addSection(card, "目标", group.goals, activity);
        addSection(card, "关键结果 KR", group.krs, activity);
        addSection(card, "执行动作", group.actions, activity);
        addSection(card, "风险 / 障碍", group.risks, activity);
        addSection(card, "复盘", group.reviews, activity);

        return card;
    }

    private LinearLayout buildWorkflowOverview(MainActivity activity) {
        WorkflowSnapshot snapshot = WorkflowSnapshot.from(
                activity.getMindMapView().getNodesInternal(),
                activity.getMindMapView().getConnectionsInternal()
        );

        LinearLayout card = MonetDialogStyler.card(requireContext(), "项目工作流概览",
                "项目数：" + snapshot.projectCount
                        + "\n下一步动作：" + snapshot.nextActionCount
                        + "\n待复盘：" + snapshot.reviewDueCount
                        + "\n卡住项目：" + snapshot.stuckProjectCount);
        return card;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null || activity.getMindMapView() == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        ProjectsHubBuilder.ProjectsHubData data = ProjectsHubBuilder.build(activity.getMindMapView().getNodesInternal());

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(ThemeManager.getDialogBg());

        LinearLayout root = MonetDialogStyler.buildRoot(requireContext());
        scrollView.addView(root);

        TextView title = new TextView(requireContext());
        title.setText("项目中心");
        root.addView(title);

        TextView sub = new TextView(requireContext());
        sub.setText("项目归组、下一步动作、复盘与阻塞统一查看");
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = MonetDialogStyler.dp(requireContext(), 6);
        root.addView(sub, subLp);
        MonetDialogStyler.styleHeader(title, sub);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = MonetDialogStyler.dp(requireContext(), 14);
        root.addView(buildWorkflowOverview(activity), cardLp);

        if (data.groups.isEmpty()) {
            TextView empty = MonetDialogStyler.body(requireContext(),
                    "当前没有 PROJECT 类型节点。先建项目节点，再把相关节点挂到这个项目下。");
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = MonetDialogStyler.dp(requireContext(), 14);
            root.addView(empty, lp);
        } else {
            for (ProjectsHubBuilder.ProjectGroup group : data.groups) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = MonetDialogStyler.dp(requireContext(), 14);
                root.addView(buildProjectCard(group, activity), lp);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(scrollView)
                .setPositiveButton("关闭", null)
                .create();
        dialog.setOnShowListener(d -> MonetDialogStyler.apply(dialog, requireContext()));
        return dialog;
    }
}
