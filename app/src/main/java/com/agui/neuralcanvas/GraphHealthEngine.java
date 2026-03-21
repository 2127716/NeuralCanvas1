package com.agui.neuralcanvas;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class GraphHealthEngine {

    public static final class HealthReport {
        public int totalNodes = 0;
        public int totalConnections = 0;
        public int isolatedNodes = 0;
        public int decisionNodes = 0;
        public int decisionNodesWithEvidence = 0;
        public int executionNodes = 0;
        public int executionNodesWithTrigger = 0;
        public float score = 100f;

        public String buildSummary() {
            return "网络健康度 "
                    + String.format(java.util.Locale.US, "%.1f", score)
                    + "，孤立节点 " + isolatedNodes
                    + "，决策证据覆盖 " + decisionNodesWithEvidence + "/" + decisionNodes
                    + "，执行触发覆盖 " + executionNodesWithTrigger + "/" + executionNodes;
        }
    }

    private GraphHealthEngine() {}

    public static HealthReport analyze(Map<String, Node> nodes, Map<String, Connection> connections) {
        HealthReport report = new HealthReport();
        if (nodes == null) return report;

        report.totalNodes = nodes.size();
        report.totalConnections = connections == null ? 0 : connections.size();

        Set<String> touchedNodeIds = new HashSet<>();
        if (connections != null) {
            for (Connection c : connections.values()) {
                if (c == null) continue;
                touchedNodeIds.add(c.getFromNodeId());
                touchedNodeIds.add(c.getToNodeId());
            }
        }

        for (Node node : nodes.values()) {
            if (node == null) continue;

            if (!touchedNodeIds.contains(node.getId())) {
                report.isolatedNodes++;
            }

            if (node.isDecisionNode()) {
                report.decisionNodes++;
                if (hasDecisionEvidence(node, nodes, connections)) {
                    report.decisionNodesWithEvidence++;
                }
            }

            if (node.isExecutionNode()) {
                report.executionNodes++;
                if (!safe(node.getTriggerCondition()).isEmpty()) {
                    report.executionNodesWithTrigger++;
                }
            }
        }

        float isolatedPenalty = report.totalNodes <= 0 ? 0f : (30f * report.isolatedNodes / Math.max(1f, report.totalNodes));
        float decisionPenalty = report.decisionNodes <= 0 ? 0f : (35f * (report.decisionNodes - report.decisionNodesWithEvidence) / Math.max(1f, report.decisionNodes));
        float executionPenalty = report.executionNodes <= 0 ? 0f : (35f * (report.executionNodes - report.executionNodesWithTrigger) / Math.max(1f, report.executionNodes));

        report.score = Math.max(0f, 100f - isolatedPenalty - decisionPenalty - executionPenalty);
        return report;
    }

    private static boolean hasDecisionEvidence(Node node, Map<String, Node> nodes, Map<String, Connection> connections) {
        if (node == null || connections == null) return false;
        for (Connection c : connections.values()) {
            if (c == null) continue;
            if (!node.getId().equals(c.getToNodeId())) continue;

            String type = safe(c.getType() == null ? "" : c.getType().name());
            if ("EVIDENCE_FOR".equalsIgnoreCase(type) || "EVIDENCE_AGAINST".equalsIgnoreCase(type) || "SUPPORTS".equalsIgnoreCase(type)) {
                return true;
            }

            Node from = nodes == null ? null : nodes.get(c.getFromNodeId());
            if (from != null && from.getType() == Node.NodeType.EVIDENCE) {
                return true;
            }
        }
        return false;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
