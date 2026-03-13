package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GraphInsightEngine {
    private GraphInsightEngine() {}

    public static class InsightReport {
        public final Map<String, List<Node>> backlinks = new LinkedHashMap<>();
        public final List<Node> isolatedNodes = new ArrayList<>();
        public final List<Node> conflictEvidenceTargets = new ArrayList<>();
        public final List<Node> highValueNodes = new ArrayList<>();
        public final List<String> gapFindings = new ArrayList<>();
    }

    public static InsightReport analyze(Map<String, Node> nodes, Map<String, Connection> connections) {
        InsightReport report = new InsightReport();
        if (nodes == null) return report;

        Map<String, Integer> degree = new LinkedHashMap<>();
        Map<String, Integer> evidenceFor = new LinkedHashMap<>();
        Map<String, Integer> evidenceAgainst = new LinkedHashMap<>();

        for (Node node : nodes.values()) {
            if (node == null) continue;
            degree.put(node.getId(), 0);
            report.backlinks.put(node.getId(), new ArrayList<Node>());
        }

        if (connections != null) {
            for (Connection c : connections.values()) {
                if (c == null) continue;
                addDegree(degree, c.getFromNodeId());
                addDegree(degree, c.getToNodeId());

                Node from = nodes.get(c.getFromNodeId());
                List<Node> backlinkList = report.backlinks.get(c.getToNodeId());
                if (from != null && backlinkList != null) backlinkList.add(from);

                if (c.getType() == Connection.ConnectionType.EVIDENCE_FOR) addDegree(evidenceFor, c.getToNodeId());
                if (c.getType() == Connection.ConnectionType.EVIDENCE_AGAINST) addDegree(evidenceAgainst, c.getToNodeId());
            }
        }

        for (Node node : nodes.values()) {
            if (node == null) continue;
            int deg = degree.containsKey(node.getId()) ? degree.get(node.getId()) : 0;
            if (deg == 0) report.isolatedNodes.add(node);

            int forCount = evidenceFor.containsKey(node.getId()) ? evidenceFor.get(node.getId()) : 0;
            int againstCount = evidenceAgainst.containsKey(node.getId()) ? evidenceAgainst.get(node.getId()) : 0;
            if (forCount > 0 && againstCount > 0) report.conflictEvidenceTargets.add(node);

            int backlinks = report.backlinks.containsKey(node.getId()) ? report.backlinks.get(node.getId()).size() : 0;
            if (deg >= 3 || backlinks >= 2 || node.getPriority() >= 4) report.highValueNodes.add(node);

            boolean needsEvidence = node.isDecisionNode() || node.getType() == Node.NodeType.PROJECT || node.getType() == Node.NodeType.GOAL;
            boolean needsAction = node.getType() == Node.NodeType.PROJECT || node.getType() == Node.NodeType.GOAL || node.getType() == Node.NodeType.DECISION;
            boolean needsCounterexample = node.getType() == Node.NodeType.CONCEPT || node.getType() == Node.NodeType.NOTE || node.getType() == Node.NodeType.QUESTION;

            boolean hasEvidence = forCount > 0 || againstCount > 0;
            boolean hasAction = hasActionNearby(node, nodes);
            boolean hasCounter = hasCounterexampleLink(node, connections);

            if (needsEvidence && !hasEvidence) report.gapFindings.add("缺证据：" + safeTitle(node));
            if (needsAction && !hasAction) report.gapFindings.add("缺下一步：" + safeTitle(node));
            if (needsCounterexample && !hasCounter) report.gapFindings.add("缺反例：" + safeTitle(node));
        }

        sortNodes(report.isolatedNodes);
        sortNodes(report.conflictEvidenceTargets);
        sortNodes(report.highValueNodes);
        return report;
    }

    public static String buildReadableReport(InsightReport report, Node focusNode) {
        StringBuilder sb = new StringBuilder();
        if (focusNode != null) {
            sb.append("当前节点：").append(safeTitle(focusNode)).append("\n");
            List<Node> backlinks = report.backlinks.get(focusNode.getId());
            sb.append("Backlinks：").append(backlinks == null ? 0 : backlinks.size()).append(" 个\n");
            if (backlinks != null) {
                for (Node node : backlinks) sb.append("- ").append(safeTitle(node)).append("\n");
            }
            sb.append("\n");
        }
        sb.append("孤岛节点：").append(report.isolatedNodes.size()).append(" 个\n");
        appendTop(sb, report.isolatedNodes);
        sb.append("\n冲突证据目标：").append(report.conflictEvidenceTargets.size()).append(" 个\n");
        appendTop(sb, report.conflictEvidenceTargets);
        sb.append("\n高价值节点推荐：").append(report.highValueNodes.size()).append(" 个\n");
        appendTop(sb, report.highValueNodes);
        sb.append("\n全局缺口扫描：").append(report.gapFindings.size()).append(" 项\n");
        int limit = Math.min(report.gapFindings.size(), 15);
        for (int i = 0; i < limit; i++) sb.append("- ").append(report.gapFindings.get(i)).append("\n");
        if (report.gapFindings.size() > limit) sb.append("- ……还有 ").append(report.gapFindings.size() - limit).append(" 项\n");
        return sb.toString();
    }

    private static boolean hasActionNearby(Node anchor, Map<String, Node> nodes) {
        if (anchor == null || nodes == null) return false;
        String ownerId = WorkflowEngine.resolveOwnerId(anchor);
        for (Node node : nodes.values()) {
            if (node == null || node.getId().equals(anchor.getId())) continue;
            if (!ownerId.equals(node.getProjectId())) continue;
            if ((node.getType() == Node.NodeType.ACTION || node.getType() == Node.NodeType.TASK) && node.getStatus() != Node.NodeStatus.DONE) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCounterexampleLink(Node anchor, Map<String, Connection> connections) {
        if (anchor == null || connections == null) return false;
        for (Connection c : connections.values()) {
            if (c == null || c.getType() != Connection.ConnectionType.OPPOSES) continue;
            if (anchor.getId().equals(c.getFromNodeId()) || anchor.getId().equals(c.getToNodeId())) return true;
        }
        return false;
    }

    private static void addDegree(Map<String, Integer> map, String id) {
        if (id == null || id.trim().isEmpty()) return;
        map.put(id, map.containsKey(id) ? map.get(id) + 1 : 1);
    }

    private static void sortNodes(List<Node> nodes) {
        Collections.sort(nodes, new Comparator<Node>() {
            @Override
            public int compare(Node a, Node b) {
                int p = Integer.compare(b.getPriority(), a.getPriority());
                if (p != 0) return p;
                return safeTitle(a).compareToIgnoreCase(safeTitle(b));
            }
        });
    }

    private static void appendTop(StringBuilder sb, List<Node> nodes) {
        int limit = Math.min(nodes.size(), 8);
        for (int i = 0; i < limit; i++) sb.append("- ").append(safeTitle(nodes.get(i))).append("\n");
        if (nodes.isEmpty()) sb.append("- 无\n");
    }

    private static String safeTitle(Node node) {
        String title = node == null ? "" : node.getTitle();
        return title == null || title.trim().isEmpty() ? "(无标题)" : title.trim();
    }
}
