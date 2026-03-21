package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LearningTransferEngine {

    public static final class TransferReport {
        public int missingTransferCount = 0;
        public int roteDefinitionCount = 0;
        public int sourceWithoutQuestionCount = 0;
        public String nextBestAction = "优先补一个检索问题";
        public final List<String> highlights = new ArrayList<>();

        public String buildSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("学习迁移：缺迁移任务 ").append(missingTransferCount)
                    .append(" 个，疑似只会定义不会应用 ").append(roteDefinitionCount)
                    .append(" 个，来源多但没形成问题链 ").append(sourceWithoutQuestionCount)
                    .append(" 个");
            sb.append("\n下一最佳动作：").append(nextBestAction);
            for (String item : highlights) sb.append("\n- ").append(item);
            return sb.toString();
        }
    }

    private LearningTransferEngine() {}

    public static TransferReport analyze(Map<String, Node> nodes, Map<String, Connection> connections) {
        TransferReport report = new TransferReport();
        if (nodes == null || nodes.isEmpty()) return report;

        for (Node node : nodes.values()) {
            if (node == null || !node.isLearningNode()) continue;

            boolean hasQuestion = hasNeighborOfType(node, nodes, connections, Node.NodeType.QUESTION);
            boolean hasSource = hasNeighborOfType(node, nodes, connections, Node.NodeType.SOURCE);
            boolean hasTransfer = hasNeighborOfType(node, nodes, connections, Node.NodeType.EXPERIMENT)
                    || hasNeighborOfType(node, nodes, connections, Node.NodeType.TASK)
                    || hasNeighborOfType(node, nodes, connections, Node.NodeType.ACTION)
                    || hasNeighborOfType(node, nodes, connections, Node.NodeType.INSIGHT);

            if (!hasTransfer && (node.getType() == Node.NodeType.CONCEPT || node.getType() == Node.NodeType.NOTE)) {
                report.missingTransferCount++;
                maybeAdd(report, "缺迁移任务：" + safeTitle(node));
            }

            String content = safe(node.getContent());
            if ((content.contains("是") || content.contains("指")) && !hasQuestion && !hasTransfer) {
                report.roteDefinitionCount++;
                maybeAdd(report, "疑似只会定义不会用：" + safeTitle(node));
            }

            if (hasSource && !hasQuestion) {
                report.sourceWithoutQuestionCount++;
                maybeAdd(report, "来源多但未形成问题链：" + safeTitle(node));
            }
        }

        if (report.missingTransferCount >= report.roteDefinitionCount && report.missingTransferCount >= report.sourceWithoutQuestionCount) {
            report.nextBestAction = "优先给高价值概念补一个迁移任务或小实验";
        } else if (report.roteDefinitionCount >= report.sourceWithoutQuestionCount) {
            report.nextBestAction = "优先把定义型节点改造成问题-答案-应用三联结构";
        } else {
            report.nextBestAction = "优先把来源节点串成检索问题链";
        }

        return report;
    }

    private static boolean hasNeighborOfType(Node center,
                                             Map<String, Node> nodes,
                                             Map<String, Connection> connections,
                                             Node.NodeType targetType) {
        if (center == null || nodes == null || connections == null) return false;
        for (Connection c : connections.values()) {
            if (c == null) continue;
            String otherId = null;
            if (center.getId().equals(c.getFromNodeId())) otherId = c.getToNodeId();
            else if (center.getId().equals(c.getToNodeId())) otherId = c.getFromNodeId();
            if (otherId == null) continue;
            Node other = nodes.get(otherId);
            if (other != null && other.getType() == targetType) return true;
        }
        return false;
    }

    private static void maybeAdd(TransferReport report, String text) {
        if (report != null && text != null && report.highlights.size() < 6) report.highlights.add(text);
    }

    private static String safeTitle(Node node) {
        String title = node == null ? "" : node.getTitle();
        title = title == null ? "" : title.trim();
        return title.isEmpty() ? "未命名节点" : title;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
