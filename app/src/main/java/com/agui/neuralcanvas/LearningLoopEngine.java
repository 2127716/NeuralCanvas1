package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LearningLoopEngine {

    public static final class LearningReport {
        public int dueCount = 0;
        public int upcomingCount = 0;
        public int missingQuestionCount = 0;
        public int missingSourceCount = 0;
        public int missingReviewCount = 0;
        public final List<String> highlights = new ArrayList<>();

        public String buildSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("学习闭环：到期 ").append(dueCount)
                    .append(" 个，待复习 ").append(upcomingCount).append(" 个");
            if (missingQuestionCount > 0) sb.append("，缺检索问题 ").append(missingQuestionCount).append(" 个");
            if (missingSourceCount > 0) sb.append("，缺来源 ").append(missingSourceCount).append(" 个");
            if (missingReviewCount > 0) sb.append("，缺复习锚点 ").append(missingReviewCount).append(" 个");
            for (String item : highlights) sb.append("\n- ").append(item);
            return sb.toString();
        }
    }

    private LearningLoopEngine() {}

    public static LearningReport analyze(Map<String, Node> nodes, Map<String, Connection> connections) {
        LearningReport report = new LearningReport();
        if (nodes == null || nodes.isEmpty()) return report;

        MemoryEngine.MemorySnapshot snapshot = MemoryEngine.build(nodes);
        report.dueCount = snapshot.dueNodes.size();
        report.upcomingCount = snapshot.upcomingNodes.size();

        for (Node node : nodes.values()) {
            if (node == null || !node.isLearningNode()) continue;

            boolean needsQuestionCheck = node.getType() == Node.NodeType.CONCEPT || node.getType() == Node.NodeType.NOTE;
            boolean hasQuestion = hasNeighborOfType(node, nodes, connections, Node.NodeType.QUESTION);
            boolean hasSource = hasNeighborOfType(node, nodes, connections, Node.NodeType.SOURCE);
            boolean hasReview = safe(node.getReviewAt()).length() > 0;

            if (needsQuestionCheck && !hasQuestion) {
                report.missingQuestionCount++;
                maybeAddHighlight(report, "学习节点缺检索问题：" + safeTitle(node));
            }
            if (!hasSource) {
                report.missingSourceCount++;
                maybeAddHighlight(report, "学习节点缺来源：" + safeTitle(node));
            }
            if (!hasReview) {
                report.missingReviewCount++;
                maybeAddHighlight(report, "学习节点缺复习时间：" + safeTitle(node));
            }
        }

        if (!snapshot.dueNodes.isEmpty()) {
            maybeAddHighlight(report, "优先复习：" + safeTitle(snapshot.dueNodes.get(0)));
        }

        return report;
    }

    public static void ensureReviewAnchor(Node node) {
        if (node == null || !node.isLearningNode()) return;
        if (safe(node.getReviewAt()).isEmpty()) {
            long dueAt = MemoryEngine.getDueAt(node);
            node.setReviewAt(String.valueOf(dueAt));
        }
    }

    private static boolean hasNeighborOfType(Node center,
                                             Map<String, Node> nodes,
                                             Map<String, Connection> connections,
                                             Node.NodeType targetType) {
        if (center == null || nodes == null || connections == null) return false;
        for (Connection c : connections.values()) {
            if (c == null) continue;

            if (center.getId().equals(c.getFromNodeId())) {
                Node other = nodes.get(c.getToNodeId());
                if (other != null && other.getType() == targetType) return true;
            }

            if (center.getId().equals(c.getToNodeId())) {
                Node other = nodes.get(c.getFromNodeId());
                if (other != null && other.getType() == targetType) return true;
            }
        }
        return false;
    }

    private static void maybeAddHighlight(LearningReport report, String text) {
        if (report == null || text == null || text.trim().isEmpty()) return;
        if (report.highlights.size() < 6) report.highlights.add(text.trim());
    }

    private static String safeTitle(Node node) {
        String title = node == null ? "" : safe(node.getTitle());
        return title.isEmpty() ? "未命名节点" : title;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
