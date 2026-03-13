package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class WorkflowRecommendationEngine {
    private WorkflowRecommendationEngine() {}

    public static String buildSummary(Map<String, Node> nodes, Map<String, Connection> connections) {
        List<String> lines = new ArrayList<>();
        if (nodes == null || nodes.isEmpty()) return "暂无节点，先从一个目标、一个任务或一个学习节点开始。";

        Node blocked = firstByStatus(nodes, Node.NodeStatus.BLOCKED);
        if (blocked != null) lines.add("先处理受阻节点：" + safeTitle(blocked) + "。给它补一个障碍拆解或替代动作。");

        MemoryEngine.MemorySnapshot memory = MemoryEngine.build(nodes);
        if (!memory.dueNodes.isEmpty()) lines.add("今天优先复习到期学习节点：" + safeTitle(memory.dueNodes.get(0)) + "，避免只输入不提取。");

        Node noNextAction = findActionGap(nodes);
        if (noNextAction != null) lines.add("给“" + safeTitle(noNextAction) + "”补一个最小下一步和 If-Then 触发器，减少启动摩擦。");

        Node weakDecision = findWeakDecision(nodes);
        if (weakDecision != null) lines.add("决策节点“" + safeTitle(weakDecision) + "”还缺稳定分析，建议补 MCDA 或写回当前推荐方案。");

        Node reviewGap = findReviewGap(nodes);
        if (reviewGap != null) lines.add("“" + safeTitle(reviewGap) + "”还没有复盘锚点，结束后很难提炼失败模式。");

        Node estimationGap = findEstimateGap(nodes);
        if (estimationGap != null) lines.add("给“" + safeTitle(estimationGap) + "”补估时或执行回填，后面参考类预测才会越来越准。");

        if (lines.isEmpty()) {
            lines.add("当前结构已经比较完整。下一步优先做一次执行回填或记忆复习，把真实数据写回系统。");
        }

        StringBuilder sb = new StringBuilder();
        int limit = Math.min(5, lines.size());
        for (int i = 0; i < limit; i++) {
            if (i > 0) sb.append("\n");
            sb.append(i + 1).append(". ").append(lines.get(i));
        }
        return sb.toString();
    }

    private static Node firstByStatus(Map<String, Node> nodes, Node.NodeStatus status) {
        for (Node node : nodes.values()) if (node != null && node.getStatus() == status) return node;
        return null;
    }

    private static Node findActionGap(Map<String, Node> nodes) {
        for (Node node : nodes.values()) {
            if (node == null) continue;
            if ((node.getType() == Node.NodeType.GOAL || node.getType() == Node.NodeType.PROJECT || node.getType() == Node.NodeType.TASK)
                    && WorkflowEngine.isBlank(node.getTriggerCondition())
                    && node.getStatus() != Node.NodeStatus.DONE) return node;
        }
        return null;
    }

    private static Node findWeakDecision(Map<String, Node> nodes) {
        List<Node> decisions = new ArrayList<>();
        for (Node node : nodes.values()) if (node != null && node.getType() == Node.NodeType.DECISION) decisions.add(node);
        Collections.sort(decisions, new Comparator<Node>() {
            @Override public int compare(Node a, Node b) {
                return Float.compare(GraphMetaHelper.getFloat(a, "decision_robustness_score", 0f), GraphMetaHelper.getFloat(b, "decision_robustness_score", 0f));
            }
        });
        return decisions.isEmpty() ? null : decisions.get(0);
    }

    private static Node findReviewGap(Map<String, Node> nodes) {
        for (Node node : nodes.values()) {
            if (node == null) continue;
            if ((node.getType() == Node.NodeType.PROJECT || node.getType() == Node.NodeType.GOAL || node.getType() == Node.NodeType.TASK)
                    && WorkflowEngine.isBlank(node.getReviewAt())
                    && node.getStatus() != Node.NodeStatus.DONE) return node;
        }
        return null;
    }

    private static Node findEstimateGap(Map<String, Node> nodes) {
        for (Node node : nodes.values()) {
            if (node == null) continue;
            if (node.isExecutionNode() && node.getEffortEstimate() <= 0f) return node;
        }
        return null;
    }

    private static String safeTitle(Node node) {
        String title = node == null ? "" : node.getTitle();
        return title == null || title.trim().isEmpty() ? "未命名节点" : title.trim();
    }
}
