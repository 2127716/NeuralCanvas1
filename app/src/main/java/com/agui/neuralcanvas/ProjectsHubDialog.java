package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private String safe(String s, String fallback) {
        if (s == null || s.trim().isEmpty()) return fallback;
        return s.trim();
    }

    private boolean belongsToProject(Node node, String projectId) {
        if (node == null || projectId == null || projectId.trim().isEmpty()) return false;
        return projectId.equals(node.getProjectId());
    }

    private boolean isProject(Node node) {
        return node != null && node.getType() == Node.NodeType.PROJECT;
    }

    private boolean isGoal(Node node) {
        return node != null && node.getType() == Node.NodeType.GOAL;
    }

    private boolean isActionLike(Node node) {
        return node != null &&
                (node.getType() == Node.NodeType.ACTION
                        || node.getType() == Node.NodeType.TASK
                        || node.getType() == Node.NodeType.ROUTINE
                        || node.getType() == Node.NodeType.TRIGGER);
    }

    private boolean isKr(Node node) {
        return node != null && node.getType() == Node.NodeType.KEY_RESULT;
    }

    private boolean isRisk(Node node) {
        return node != null &&
                (node.getType() == Node.NodeType.RISK
                        || node.getType() == Node.NodeType.OBSTACLE);
    }

    private boolean isReview(Node node) {
        return node != null &&
                (node.getType() == Node.NodeType.REVIEW
                        || node.getStatus() == Node.NodeStatus.REVIEW);
    }

    private TextView buildNodeChip(Node node, MainActivity activity) {
        String title = safe(node.getTitle(), "未命名节点");
        String line = "• " + title + " [" + node.getType().label + "]";

        List<String> extras = new ArrayList<>();
        if (!safe(node.getDueAt(), "").isEmpty()) extras.add("截止:" + node.getDueAt());
        if (!safe(node.getReviewAt(), "").isEmpty()) extras.add("复盘:" + node.getReviewAt());
        if (node.getPriority() > 0) extras.add("P" + node.getPriority());
        if (node.getKrTarget() > 0f) extras.add("KR " + node.getKrCurrent() + "/" + node.getKrTarget());

        if (!extras.isEmpty()) {
            line += "\n  " + TextUtils.join(" ｜ ", extras);
        }

        TextView tv = makeText(line, 13, false, "#E2E8F0");
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

    private LinearLayout buildProjectCard(Node projectNode, List<Node> allNodes, MainActivity activity) {
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

        String projectId = projectNode.getId();

        List<Node> goals = new ArrayList<>();
        List<Node> actions = new ArrayList<>();
        List<Node> krs = new ArrayList<>();
        List<Node> risks = new ArrayList<>();
        List<Node> reviews = new ArrayList<>();

        for (Node node : allNodes) {
            if (node == null) continue;
            if (node.getId().equals(projectId)) continue;
            if (!belongsToProject(node, projectId)) continue;

            if (isGoal(node)) goals.add(node);
            else if (isKr(node)) krs.add(node);
            else if (isRisk(node)) risks.add(node);
            else if (isReview(node)) reviews.add(node);
            else if (isActionLike(node)) actions.add(node);
        }

        TextView title = makeText("项目｜" + safe(projectNode.getTitle(), "未命名项目"), 16, true, "#F8FAFC");
        title.setOnClickListener(v -> {
            if (activity != null && activity.getMindMapView() != null) {
                activity.getMindMapView().focusNodeById(projectNode.getId());
                activity.getMindMapView().selectNodeById(projectNode.getId());
                dismiss();
            }
        });
        card.addView(title);

        String summary = "目标 " + goals.size()
                + " ｜ 动作 " + actions.size()
                + " ｜ KR " + krs.size()
                + " ｜ 风险 " + risks.size()
                + " ｜ 复盘 " + reviews.size();

        TextView sub = makeText(summary, 12, false, "#93C5FD");
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subLp.topMargin = dp(6);
        sub.setLayoutParams(subLp);
        card.addView(sub);

        if (!safe(projectNode.getContent(), "").isEmpty()) {
            TextView content = makeText(projectNode.getContent(), 13, false, "#CBD5E1");
            LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            contentLp.topMargin = dp(8);
            content.setLayoutParams(contentLp);
            card.addView(content);
        }

        card.addView(spacer(10));

        addSection(card, "目标", goals, activity);
        addSection(card, "关键结果 KR", krs, activity);
        addSection(card, "执行动作", actions, activity);
        addSection(card, "风险 / 障碍", risks, activity);
        addSection(card, "复盘", reviews, activity);

        return card;
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

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null || activity.getMindMapView() == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        Map<String, Node> allMap = activity.getMindMapView().getNodesInternal();
        List<Node> allNodes = new ArrayList<>(allMap.values());
        List<Node> projects = new ArrayList<>();

        for (Node node : allNodes) {
            if (isProject(node)) {
                projects.add(node);
            }
        }

        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.parseColor("#0B1020"));
        scrollView.addView(root);

        TextView title = makeText("项目中心", 20, true, "#F8FAFC");
        root.addView(title);

        TextView sub = makeText("把 Project / Goal / KR / Action / Review 串起来", 13, false, "#94A3B8");
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subLp.topMargin = dp(6);
        sub.setLayoutParams(subLp);
        root.addView(sub);

        root.addView(spacer(14));

        if (projects.isEmpty()) {
            TextView empty = makeText("当前没有 PROJECT 类型节点。你可以先新建一个项目节点，再把相关子节点的 projectId 指向它。", 14, false, "#CBD5E1");
            root.addView(empty);
        } else {
            for (Node project : projects) {
                root.addView(buildProjectCard(project, allNodes, activity));
            }
        }

        return new AlertDialog.Builder(requireContext())
                .setView(scrollView)
                .setPositiveButton("关闭", null)
                .create();
    }
}
