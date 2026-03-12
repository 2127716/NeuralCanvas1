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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

    private android.graphics.drawable.GradientDrawable createRoundedDrawable(String color) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Color.parseColor(color));
        gd.setCornerRadius(dp(16));
        gd.setStroke(dp(1), Color.parseColor("#24324A"));
        return gd;
    }

    private String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        return value.trim();
    }

    private boolean containsDateHint(String text, String today) {
        if (text == null) return false;
        String s = text.trim();
        return !s.isEmpty() && s.contains(today);
    }

    private boolean isExecutionNode(Node node) {
        return node != null && node.isExecutionNode();
    }

    private boolean isReviewNode(Node node) {
        return node != null &&
                (node.getType() == Node.NodeType.REVIEW ||
                        node.getStatus() == Node.NodeStatus.REVIEW ||
                        !safe(node.getReviewAt(), "").isEmpty());
    }

    private boolean isInboxNode(Node node) {
        return node != null && node.getType() == Node.NodeType.INBOX;
    }

    private boolean isRiskOrBlocked(Node node) {
        return node != null &&
                (node.getType() == Node.NodeType.RISK
                        || node.getType() == Node.NodeType.OBSTACLE
                        || node.getStatus() == Node.NodeStatus.BLOCKED);
    }

    private boolean isKrNode(Node node) {
        return node != null && node.getType() == Node.NodeType.KEY_RESULT;
    }

    private int statusRank(Node.NodeStatus status) {
        if (status == null) return 99;
        switch (status) {
            case ACTIVE: return 0;
            case PLANNED: return 1;
            case WAITING: return 2;
            case BLOCKED: return 3;
            case REVIEW: return 4;
            case SOMEDAY: return 5;
            case DONE: return 6;
            default: return 99;
        }
    }

    private String formatPercent(float current, float target) {
        if (target <= 0f) return "";
        float percent = (current / target) * 100f;
        if (percent < 0f) percent = 0f;
        return String.format(Locale.getDefault(), "%.0f%%", percent);
    }

    private String buildNodeExtra(Node node) {
        List<String> parts = new ArrayList<>();

        if (!safe(node.getDueAt(), "").isEmpty()) {
            parts.add("截止: " + node.getDueAt());
        }
        if (!safe(node.getReviewAt(), "").isEmpty()) {
            parts.add("复盘/复习: " + node.getReviewAt());
        }
        if (!safe(node.getTriggerCondition(), "").isEmpty()) {
            parts.add("触发: " + node.getTriggerCondition());
        }
        if (node.getPriority() > 0) {
            parts.add("优先级: " + node.getPriority());
        }
        if (node.getEffortEstimate() > 0f) {
            parts.add("预计耗时: " + node.getEffortEstimate() + "h");
        }
        if (node.getKrTarget() > 0f) {
            parts.add("KR: " + node.getKrCurrent() + " / " + node.getKrTarget() + " (" +
                    formatPercent(node.getKrCurrent(), node.getKrTarget()) + ")");
        }
        if (node.getType() == Node.NodeType.EVIDENCE) {
            parts.add("证据强度: " + node.getEvidenceStrength());
        }

        if (parts.isEmpty()) {
            String content = safe(node.getContent(), "");
            if (!content.isEmpty()) {
                if (content.length() > 40) content = content.substring(0, 40) + "…";
                parts.add(content);
            }
        }

        return TextUtils.join(" ｜ ", parts);
    }

    private void markDone(Node node, MainActivity activity) {
        if (node == null || activity == null) return;
        node.setStatus(Node.NodeStatus.DONE);
        activity.onNodeUpdated(node);
    }

    private void addKrValue(Node node, float delta, MainActivity activity) {
        if (node == null || activity == null) return;
        node.setKrCurrent(node.getKrCurrent() + delta);
        activity.onNodeUpdated(node);
    }

    private TextView buildNodeRow(final Node node, String extra, final MainActivity activity) {
        TextView tv = new TextView(requireContext());
        String title = safe(node.getTitle(), "未命名节点");
        String type = node.getType() == null ? "" : node.getType().label;
        String line = "• " + title + "  [" + type + "]";
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
            if (activity != null && activity.getMindMapView() != null) {
                activity.getMindMapView().focusNodeById(node.getId());
                activity.getMindMapView().selectNodeById(node.getId());
                dismiss();
            }
        });

        tv.setOnLongClickListener(v -> {
            if (activity == null) return true;

            if (isKrNode(node)) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("快速更新 KR")
                        .setMessage("当前值：" + node.getKrCurrent() + " / " + node.getKrTarget())
                        .setNegativeButton("取消", null)
                        .setNeutralButton("+1", (d, w) -> addKrValue(node, 1f, activity))
                        .setPositiveButton("+5", (d, w) -> addKrValue(node, 5f, activity))
                        .show();
                return true;
            }

            if (isExecutionNode(node) || isReviewNode(node)) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("快速操作")
                        .setMessage("把这个节点标记为已完成？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("标记 DONE", (d, w) -> markDone(node, activity))
                        .show();
                return true;
            }

            return true;
        });

        return tv;
    }

    private void addSection(LinearLayout root, String title, List<Node> nodes, MainActivity activity) {
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
            Node node = nodes.get(i);
            root.addView(buildNodeRow(node, buildNodeExtra(node), activity));
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

    private void sortTodayNodes(List<Node> list, final String today) {
        Collections.sort(list, new Comparator<Node>() {
            @Override
            public int compare(Node a, Node b) {
                boolean aDueToday = containsDateHint(a.getDueAt(), today);
                boolean bDueToday = containsDateHint(b.getDueAt(), today);
                if (aDueToday != bDueToday) return aDueToday ? -1 : 1;

                int p = Integer.compare(b.getPriority(), a.getPriority());
                if (p != 0) return p;

                int sr = Integer.compare(statusRank(a.getStatus()), statusRank(b.getStatus()));
                if (sr != 0) return sr;

                boolean aHasTrigger = !safe(a.getTriggerCondition(), "").isEmpty();
                boolean bHasTrigger = !safe(b.getTriggerCondition(), "").isEmpty();
                if (aHasTrigger != bHasTrigger) return aHasTrigger ? -1 : 1;

                float ae = a.getEffortEstimate() <= 0f ? 9999f : a.getEffortEstimate();
                float be = b.getEffortEstimate() <= 0f ? 9999f : b.getEffortEstimate();
                int effortCompare = Float.compare(ae, be);
                if (effortCompare != 0) return effortCompare;

                return safe(a.getTitle(), "").compareToIgnoreCase(safe(b.getTitle(), ""));
            }
        });
    }

    private void sortKrNodes(List<Node> list) {
        Collections.sort(list, new Comparator<Node>() {
            @Override
            public int compare(Node a, Node b) {
                float ap = a.getKrTarget() > 0f ? a.getKrCurrent() / a.getKrTarget() : -1f;
                float bp = b.getKrTarget() > 0f ? b.getKrCurrent() / b.getKrTarget() : -1f;
                return Float.compare(bp, ap);
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null || activity.getMindMapView() == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        Map<String, Node> allNodes = activity.getMindMapView().getNodesInternal();
        List<Node> inboxNodes = new ArrayList<>();
        List<Node> todayNodes = new ArrayList<>();
        List<Node> reviewNodes = new ArrayList<>();
        List<Node> riskNodes = new ArrayList<>();
        List<Node> krNodes = new ArrayList<>();

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        for (Node node : allNodes.values()) {
            if (node == null) continue;

            if (isInboxNode(node)) {
                inboxNodes.add(node);
            }

            if (isExecutionNode(node)) {
                boolean important =
                        node.getPriority() >= 4
                                || containsDateHint(node.getDueAt(), today)
                                || !safe(node.getTriggerCondition(), "").isEmpty()
                                || node.getStatus() == Node.NodeStatus.ACTIVE
                                || node.getStatus() == Node.NodeStatus.PLANNED;
                if (important && node.getStatus() != Node.NodeStatus.DONE) {
                    todayNodes.add(node);
                }
            }

            if ((isReviewNode(node) || containsDateHint(node.getReviewAt(), today))
                    && node.getStatus() != Node.NodeStatus.DONE) {
                reviewNodes.add(node);
            }

            if (isRiskOrBlocked(node) && node.getStatus() != Node.NodeStatus.DONE) {
                riskNodes.add(node);
            }

            if (isKrNode(node)) {
                krNodes.add(node);
            }
        }

        sortTodayNodes(todayNodes, today);
        sortKrNodes(krNodes);

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.parseColor("#0B1020"));
        scrollView.addView(root);

        TextView title = buildTitle("科学工作台", 20, true, "#F8FAFC");
        root.addView(title);

        TextView subtitle = buildTitle("单击聚焦节点；长按可快速完成或推进 KR", 13, false, "#94A3B8");
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subLp.topMargin = dp(6);
        subtitle.setLayoutParams(subLp);
        root.addView(subtitle);

        root.addView(buildSpacer(16));

        HorizontalScrollView hsv = new HorizontalScrollView(requireContext());
        hsv.setHorizontalScrollBarEnabled(false);

        LinearLayout cardRow = new LinearLayout(requireContext());
        cardRow.setOrientation(LinearLayout.HORIZONTAL);
        cardRow.addView(buildCard("Inbox", String.valueOf(inboxNodes.size()), "#1E293B"));
        cardRow.addView(buildCard("今日执行", String.valueOf(todayNodes.size()), "#0F766E"));
        cardRow.addView(buildCard("待复盘", String.valueOf(reviewNodes.size()), "#92400E"));
        cardRow.addView(buildCard("风险/受阻", String.valueOf(riskNodes.size()), "#7F1D1D"));
        cardRow.addView(buildCard("KR", String.valueOf(krNodes.size()), "#1D4ED8"));

        hsv.addView(cardRow);
        root.addView(hsv);

        root.addView(buildSpacer(18));

        addSection(root, "Inbox 待澄清", inboxNodes, activity);
        addSection(root, "今日最值得推进（已排序）", todayNodes, activity);
        addSection(root, "待复盘 / 待复习", reviewNodes, activity);
        addSection(root, "高风险 / 受阻节点", riskNodes, activity);
        addSection(root, "关键结果（KR）", krNodes, activity);

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
