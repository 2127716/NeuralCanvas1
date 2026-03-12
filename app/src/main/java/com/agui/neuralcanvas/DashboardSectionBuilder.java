package com.agui.neuralcanvas;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardSectionBuilder {

    public static class DashboardData {
        public final List<Node> inboxNodes = new ArrayList<>();
        public final List<Node> todayNodes = new ArrayList<>();
        public final List<Node> reviewNodes = new ArrayList<>();
        public final List<Node> riskNodes = new ArrayList<>();
        public final List<Node> krNodes = new ArrayList<>();
        public String todayDate;
    }

    public static DashboardData build(Map<String, Node> allNodes) {
        DashboardData data = new DashboardData();
        data.todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (allNodes == null) return data;

        for (Node node : allNodes.values()) {
            if (node == null) continue;

            if (isInboxNode(node)) {
                data.inboxNodes.add(node);
            }

            if (isExecutionNode(node)) {
                boolean important =
                        node.getPriority() >= 4
                                || containsDateHint(node.getDueAt(), data.todayDate)
                                || !NodeUiTextFormatter.safe(node.getTriggerCondition()).isEmpty()
                                || node.getStatus() == Node.NodeStatus.ACTIVE
                                || node.getStatus() == Node.NodeStatus.PLANNED;

                if (important && node.getStatus() != Node.NodeStatus.DONE) {
                    data.todayNodes.add(node);
                }
            }

            if ((isReviewNode(node) || containsDateHint(node.getReviewAt(), data.todayDate))
                    && node.getStatus() != Node.NodeStatus.DONE) {
                data.reviewNodes.add(node);
            }

            if (isRiskOrBlocked(node) && node.getStatus() != Node.NodeStatus.DONE) {
                data.riskNodes.add(node);
            }

            if (isKrNode(node)) {
                data.krNodes.add(node);
            }
        }

        sortTodayNodes(data.todayNodes, data.todayDate);
        sortKrNodes(data.krNodes);

        return data;
    }

    public static boolean isInboxNode(Node node) {
        return node != null && node.getType() == Node.NodeType.INBOX;
    }

    public static boolean isKrNode(Node node) {
        return node != null && node.getType() == Node.NodeType.KEY_RESULT;
    }

    public static boolean isDecisionNode(Node node) {
        return node != null && node.getType() == Node.NodeType.DECISION;
    }

    public static boolean isLearningNode(Node node) {
        if (node == null) return false;
        return node.getType() == Node.NodeType.CONCEPT
                || node.getType() == Node.NodeType.NOTE
                || node.getType() == Node.NodeType.QUESTION
                || node.getType() == Node.NodeType.SOURCE
                || node.getType() == Node.NodeType.INSIGHT
                || node.getType() == Node.NodeType.EVIDENCE;
    }

    public static boolean isExecutionNode(Node node) {
        return node != null && node.isExecutionNode();
    }

    public static boolean isReviewNode(Node node) {
        return node != null &&
                (node.getType() == Node.NodeType.REVIEW
                        || node.getStatus() == Node.NodeStatus.REVIEW
                        || !NodeUiTextFormatter.safe(node.getReviewAt()).isEmpty());
    }

    public static boolean isRiskOrBlocked(Node node) {
        return node != null &&
                (node.getType() == Node.NodeType.RISK
                        || node.getType() == Node.NodeType.OBSTACLE
                        || node.getStatus() == Node.NodeStatus.BLOCKED);
    }

    public static boolean containsDateHint(String text, String today) {
        String s = NodeUiTextFormatter.safe(text);
        return !s.isEmpty() && s.contains(today);
    }

    public static int statusRank(Node.NodeStatus status) {
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

    public static void sortTodayNodes(List<Node> list, final String today) {
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

                boolean aHasTrigger = !NodeUiTextFormatter.safe(a.getTriggerCondition()).isEmpty();
                boolean bHasTrigger = !NodeUiTextFormatter.safe(b.getTriggerCondition()).isEmpty();
                if (aHasTrigger != bHasTrigger) return aHasTrigger ? -1 : 1;

                float ae = a.getEffortEstimate() <= 0f ? 9999f : a.getEffortEstimate();
                float be = b.getEffortEstimate() <= 0f ? 9999f : b.getEffortEstimate();
                int effortCompare = Float.compare(ae, be);
                if (effortCompare != 0) return effortCompare;

                return NodeUiTextFormatter.safe(a.getTitle())
                        .compareToIgnoreCase(NodeUiTextFormatter.safe(b.getTitle()));
            }
        });
    }

    public static void sortKrNodes(List<Node> list) {
        Collections.sort(list, new Comparator<Node>() {
            @Override
            public int compare(Node a, Node b) {
                float ap = a.getKrTarget() > 0f ? a.getKrCurrent() / a.getKrTarget() : -1f;
                float bp = b.getKrTarget() > 0f ? b.getKrCurrent() / b.getKrTarget() : -1f;
                return Float.compare(bp, ap);
            }
        });
    }

    public static String buildNodeExtra(Node node) {
        return NodeUiTextFormatter.buildInlineMeta(node);
    }
}
