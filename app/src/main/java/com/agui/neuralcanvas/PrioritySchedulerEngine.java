package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class PrioritySchedulerEngine {

    public static final class PriorityItem {
        public String nodeId = "";
        public String title = "";
        public String lane = "执行";
        public float score = 0f;
        public String reason = "";

        public String buildLine() {
            return lane + "｜" + title + "｜分数 " + String.format(java.util.Locale.US, "%.1f", score) + "｜" + reason;
        }
    }

    public static final class PriorityBoard {
        public PriorityItem executionTop;
        public PriorityItem learningTop;
        public PriorityItem decisionTop;

        public String buildSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("今日焦点：");
            if (executionTop != null) sb.append("\n- 执行：").append(executionTop.buildLine());
            if (learningTop != null) sb.append("\n- 学习：").append(learningTop.buildLine());
            if (decisionTop != null) sb.append("\n- 决策：").append(decisionTop.buildLine());
            return sb.toString();
        }
    }

    private PrioritySchedulerEngine() {}

    public static PriorityBoard build(Map<String, Node> nodes,
                                      Map<String, Connection> connections,
                                      BehaviorMemoryProfile memoryProfile) {
        PriorityBoard board = new PriorityBoard();
        if (nodes == null || nodes.isEmpty()) return board;

        List<PriorityItem> execution = new ArrayList<>();
        List<PriorityItem> learning = new ArrayList<>();
        List<PriorityItem> decision = new ArrayList<>();

        for (Node node : nodes.values()) {
            if (node == null) continue;

            NodeIntelligenceEngine.NodeProfile profile =
                    NodeIntelligenceEngine.build(node, nodes, connections, memoryProfile);

            PriorityItem item = new PriorityItem();
            item.nodeId = node.getId();
            item.title = safeTitle(node);
            item.score = profile.priorityScore;
            item.reason = buildReason(node, profile);

            if (node.isExecutionNode()) {
                item.lane = "执行";
                execution.add(item);
            } else if (node.isLearningNode()) {
                item.lane = "学习";
                learning.add(item);
            } else if (node.isDecisionNode()) {
                item.lane = "决策";
                decision.add(item);
            }
        }

        Comparator<PriorityItem> cmp = new Comparator<PriorityItem>() {
            @Override
            public int compare(PriorityItem a, PriorityItem b) {
                return Float.compare(b.score, a.score);
            }
        };

        Collections.sort(execution, cmp);
        Collections.sort(learning, cmp);
        Collections.sort(decision, cmp);

        board.executionTop = execution.isEmpty() ? null : execution.get(0);
        board.learningTop = learning.isEmpty() ? null : learning.get(0);
        board.decisionTop = decision.isEmpty() ? null : decision.get(0);
        return board;
    }

    private static String buildReason(Node node, NodeIntelligenceEngine.NodeProfile profile) {
        if (node == null || profile == null) return "系统推荐";
        if (node.isExecutionNode()) {
            if (safe(node.getTriggerCondition()).isEmpty()) return "缺触发器";
            if (safe(node.getDueAt()).isEmpty() && safe(node.getReviewAt()).isEmpty()) return "缺时间锚点";
            if (profile.triggerMisses > profile.triggerHits) return "触发命中率低";
            return "执行价值高";
        }
        if (node.isLearningNode()) {
            if (profile.memoryLapses > 0) return "重复遗忘";
            if (MemoryEngine.getDueAt(node) <= System.currentTimeMillis()) return "到期待复习";
            return "学习链待加强";
        }
        if (node.isDecisionNode()) {
            if (profile.issueCount > 0) return "结构问题较多";
            return "决策风险高";
        }
        return "系统推荐";
    }

    private static String safeTitle(Node node) {
        String title = node == null ? "" : safe(node.getTitle());
        return title.isEmpty() ? "未命名节点" : title;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
