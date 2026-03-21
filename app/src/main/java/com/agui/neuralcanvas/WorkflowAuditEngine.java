package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WorkflowAuditEngine {

    public static final class AuditResult {
        public final List<String> issues = new ArrayList<>();
        public boolean isHealthy() { return issues.isEmpty(); }
        public String buildSummary() {
            if (issues.isEmpty()) return "未发现明显结构缺口";
            StringBuilder sb = new StringBuilder();
            sb.append("发现 ").append(issues.size()).append(" 个结构缺口");
            for (String issue : issues) sb.append("\n- ").append(issue);
            return sb.toString();
        }
    }

    private WorkflowAuditEngine() {}

    public static AuditResult audit(Map<String, Node> nodes, Map<String, Connection> connections) {
        AuditResult result = new AuditResult();
        if (nodes == null || nodes.isEmpty()) return result;

        for (Node node : nodes.values()) {
            if (node == null) continue;

            if (node.isExecutionNode()) {
                auditExecutionNode(node, connections, result);
            }
            if (node.isDecisionNode()) {
                auditDecisionNode(node, nodes, connections, result);
            }
            if (node.isLearningNode()) {
                auditLearningNode(node, nodes, connections, result);
            }
        }
        return result;
    }

    private static void auditExecutionNode(Node node, Map<String, Connection> connections, AuditResult result) {
        if ((node.getType() == Node.NodeType.TASK || node.getType() == Node.NodeType.ACTION)
                && safe(node.getTriggerCondition()).isEmpty()) {
            result.issues.add("执行节点缺少触发器：" + safeTitle(node));
        }

        if ((node.getType() == Node.NodeType.TASK || node.getType() == Node.NodeType.ACTION)
                && safe(node.getDueAt()).isEmpty() && safe(node.getReviewAt()).isEmpty()) {
            result.issues.add("执行节点缺少时间锚点：" + safeTitle(node));
        }

        if (node.getType() == Node.NodeType.PROJECT && !hasOutgoingToExecution(node, connections)) {
            result.issues.add("项目没有落地动作：" + safeTitle(node));
        }
    }

    private static void auditDecisionNode(Node node,
                                          Map<String, Node> nodes,
                                          Map<String, Connection> connections,
                                          AuditResult result) {
        if (node.getType() == Node.NodeType.DECISION && !hasOptionAround(node, nodes, connections)) {
            result.issues.add("决策缺少方案节点：" + safeTitle(node));
        }
        if (!hasEvidenceAround(node, nodes, connections)) {
            result.issues.add("决策缺少证据支持/反证：" + safeTitle(node));
        }
        if (!hasRiskAround(node, nodes, connections)) {
            result.issues.add("决策缺少风险节点：" + safeTitle(node));
        }
    }

    private static void auditLearningNode(Node node,
                                          Map<String, Node> nodes,
                                          Map<String, Connection> connections,
                                          AuditResult result) {
        if ((node.getType() == Node.NodeType.CONCEPT || node.getType() == Node.NodeType.NOTE)
                && !hasQuestionAround(node, nodes, connections)) {
            result.issues.add("学习节点缺少检索问题：" + safeTitle(node));
        }

        if ((node.getType() == Node.NodeType.CONCEPT || node.getType() == Node.NodeType.QUESTION || node.getType() == Node.NodeType.NOTE)
                && safe(node.getReviewAt()).isEmpty()) {
            result.issues.add("学习节点缺少复习时间：" + safeTitle(node));
        }
    }

    private static boolean hasOutgoingToExecution(Node node, Map<String, Connection> connections) {
        if (node == null || connections == null) return false;
        for (Connection c : connections.values()) {
            if (c == null) continue;
            if (!node.getId().equals(c.getFromNodeId())) continue;
            String type = c.getType() == null ? "" : c.getType().name();
            if ("LEADS_TO".equalsIgnoreCase(type) || "BELONGS_TO".equalsIgnoreCase(type) || "TRIGGERS".equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasOptionAround(Node node, Map<String, Node> nodes, Map<String, Connection> connections) {
        return hasNeighborOfType(node, nodes, connections, Node.NodeType.OPTION);
    }

    private static boolean hasRiskAround(Node node, Map<String, Node> nodes, Map<String, Connection> connections) {
        return hasNeighborOfType(node, nodes, connections, Node.NodeType.RISK)
                || hasNeighborOfType(node, nodes, connections, Node.NodeType.OBSTACLE);
    }

    private static boolean hasQuestionAround(Node node, Map<String, Node> nodes, Map<String, Connection> connections) {
        return hasNeighborOfType(node, nodes, connections, Node.NodeType.QUESTION);
    }

    private static boolean hasEvidenceAround(Node node, Map<String, Node> nodes, Map<String, Connection> connections) {
        return hasNeighborOfType(node, nodes, connections, Node.NodeType.EVIDENCE)
                || hasNeighborOfType(node, nodes, connections, Node.NodeType.SOURCE);
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

    private static String safeTitle(Node node) {
        if (node == null) return "未命名节点";
        String title = safe(node.getTitle());
        return title.isEmpty() ? "未命名节点" : title;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
