package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
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

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                requireContext().getResources().getDisplayMetrics()
        );
    }

    private TextView makeText(String text, int sp, boolean bold, String color) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        tv.setTextColor(Color.parseColor(color));
        if (bold) {
            tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        }
        return tv;
    }

    private View spacer(int hDp) {
        View v = new View(requireContext());
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(hDp)
        ));
        return v;
    }

    private android.graphics.drawable.GradientDrawable bg(String color) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Color.parseColor(color));
        gd.setCornerRadius(dp(16));
        gd.setStroke(dp(1), Color.parseColor("#24324A"));
        return gd;
    }

    private TextView buildNodeChip(Node node, MainActivity activity) {
        TextView tv = makeText(
                ProjectsHubBuilder.buildNodeChipText(node),
                13,
                false,
                "#E2E8F0"
        );
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        tv.setBackground(bg("#111827"));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(8);
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
        TextView titleView = makeText(title, 14, true, "#F8FAFC");
        root.addView(titleView);

        if (nodes.isEmpty()) {
            TextView empty = makeText("暂无", 12, false, "#64748B");
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.topMargin = dp(6);
            empty.setLayoutParams(lp);
            root.addView(empty);
            root.addView(spacer(10));
            return;
        }

        int limit = Math.min(5, nodes.size());
        for (int i = 0; i < limit; i++) {
            root.addView(buildNodeChip(nodes.get(i), activity));
        }

        if (nodes.size() > limit) {
            TextView more = makeText("还有 " + (nodes.size() - limit) + " 个", 12, false, "#64748B");
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.topMargin = dp(6);
            more.setLayoutParams(lp);
            root.addView(more);
        }

        root.addView(spacer(12));
    }

    private LinearLayout buildProjectCard(ProjectsHubBuilder.ProjectGroup group, MainActivity activity) {
        Node projectNode = group.projectNode;

        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(bg("#0F172A"));

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardLp.topMargin = dp(12);
        card.setLayoutParams(cardLp);

        String projectTitle = ProjectsHubBuilder.safe(projectNode.getTitle());
        if (projectTitle.isEmpty()) {
            projectTitle = "未命名项目";
        }

        TextView title = makeText("项目｜" + projectTitle, 16, true, "#F8FAFC");
        title.setOnClickListener(v -> {
            if (activity != null && activity.getMindMapView() != null) {
                activity.getMindMapView().focusNodeById(projectNode.getId());
                activity.getMindMapView().selectNodeById(projectNode.getId());
                dismiss();
            }
        });
        card.addView(title);

        String summary = "目标 " + group.goals.size()
                + " ｜ 动作 " + group.actions.size()
                + " ｜ KR " + group.krs.size()
                + " ｜ 风险 " + group.risks.size()
                + " ｜ 复盘 " + group.reviews.size();

        TextView sub = makeText(summary, 12, false, "#93C5FD");
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subLp.topMargin = dp(6);
        sub.setLayoutParams(subLp);
        card.addView(sub);

        String content = ProjectsHubBuilder.safe(projectNode.getContent());
        if (!content.isEmpty()) {
            TextView contentView = makeText(content, 13, false, "#CBD5E1");
            LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            contentLp.topMargin = dp(8);
            contentView.setLayoutParams(contentLp);
            card.addView(contentView);
        }

        card.addView(spacer(10));

        addSection(card, "目标", group.goals, activity);
        addSection(card, "关键结果 KR", group.krs, activity);
        addSection(card, "执行动作", group.actions, activity);
        addSection(card, "风险 / 障碍", group.risks, activity);
        addSection(card, "复盘", group.reviews, activity);

        return card;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null || activity.getMindMapView() == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        ProjectsHubBuilder.ProjectsHubData data =
                ProjectsHubBuilder.build(activity.getMindMapView().getNodesInternal());

        ScrollView scrollView = new ScrollView(requireContext());

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.parseColor("#0B1020"));
        scrollView.addView(root);

        TextView title = makeText("项目中心", 20, true, "#F8FAFC");
        root.addView(title);

        TextView sub = makeText("项目归组与排序已拆到 ProjectsHubBuilder", 13, false, "#94A3B8");
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subLp.topMargin = dp(6);
        sub.setLayoutParams(subLp);
        root.addView(sub);

        root.addView(spacer(14));

        if (data.groups.isEmpty()) {
            TextView empty = makeText(
                    "当前没有 PROJECT 类型节点。先建项目节点，再把相关节点挂到这个项目下。",
                    14,
                    false,
                    "#CBD5E1"
            );
            root.addView(empty);
        } else {
            for (ProjectsHubBuilder.ProjectGroup group : data.groups) {
                root.addView(buildProjectCard(group, activity));
            }
        }

        return new AlertDialog.Builder(requireContext())
                .setView(scrollView)
                .setPositiveButton("关闭", null)
                .create();
    }
}
