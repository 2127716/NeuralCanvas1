package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GraphInsightEngine {
    public static class InsightReport {
        public final Map<String, List<Node>> backlinks = new LinkedHashMap<>();
        public final List<Node> isolatedNodes = new ArrayList<>();
        public final List<Node> conflictEvidenceTargets = new ArrayList<>();
        public final List<Node> highValueNodes = new ArrayList<>();
        public final List<String> gapFindings = new ArrayList<>();
    }

    private GraphInsightEngine() {}

    public static InsightReport analyze(Map<String, Node> nodes, Map<String, Connection> connections) {
        InsightReport report = new InsightReport();
        if (nodes == null) return report;

        Map<String, Integer> degreeMap = new LinkedHashMap<>();
        Map<String, Boolean> hasFor = new LinkedHashMap<>();
        Map<String, Boolean> hasAgainst = new LinkedHashMap<>();

        for (Node node : nodes.values()) {
            if (node == null) continue;
            report.backlinks.put(node.getId(), new ArrayList<Node>());
            degreeMap.put(node.getId(), 0);
            hasFor.put(node.getId(), false);
            hasAgainst.put(node.getId(), false);
        }

        if (connections != null) {
            for (Connection c : connections.values()) {
                if (c == null) continue;
                Node from = nodes.get(c.getFromNodeId());
                Node to = nodes.get(c.getToNodeId());
                if (from == null || to == null) continue;
                report.backlinks.get(to.getId()).add(from);
                degreeMap.put(from.getId(), degreeMap.get(from.getId()) + 1);
                degreeMap.put(to.getId(), degreeMap.get(to.getId()) + 1);
                if (c.getType() == Connection.ConnectionType.EVIDENCE_FOR) hasFor.put(to.getId(), true);
                if (c.getType() == Connection.ConnectionType.EVIDENCE_AGAINST) hasAgainst.put(to.getId(), true);
            }
        }

        for (Node node : nodes.values()) {
            if (node == null) continue;
            int degree = degreeMap.get(node.getId());
            if (degree == 0) report.isolatedNodes.add(node);
            if (hasFor.get(node.getId()) && hasAgainst.get(node.getId())) report.conflictEvidenceTargets.add(node);
            if (degree >= 3 || node.getPriority() >= 4 || node.getType() == Node.NodeType.PROJECT || node.getType() == Node.NodeType.DECISION) {
                report.highValueNodes.add(node);
            }
            collectGap(node, report.gapFindings, nodes, connections);
        }

        Collections.sort(report.highValueNodes, new Comparator<Node>() {
            @Override
            public int compare(Node a, Node b) {
                return Integer.compare(score(b, degreeMap), score(a, degreeMap));
            }
        });
        if (report.highValueNodes.size() > 10) report.highValueNodes.subList(10, report.highValueNodes.size()).clear();
        return report;
    }

    private static int score(Node node, Map<String, Integer> degreeMap) {
        int s = degreeMap.get(node.getId()) * 3 + node.getPriority() * 2;
        if (node.getType() == Node.NodeType.PROJECT || node.getType() == Node.NodeType.DECISION) s += 6;
        if (node.getType() == Node.NodeType.GOAL || node.getType() == Node.NodeType.KEY_RESULT) s += 4;
        return s;
    }

    private static void collectGap(Node node, List<String> list, Map<String, Node> nodes, Map<String, Connection> connections) {
        boolean needsEvidence = node.getType() == Node.NodeType.DECISION || node.getType() == Node.NodeType.OPTION || node.getType() == Node.NodeType.ASSUMPTION;
        boolean needsAction = node.getType() == Node.NodeType.PROJECT || node.getType() == Node.NodeType.GOAL || node.getType() == Node.NodeType.DECISION;
        boolean needsCounterexample = node.isLearningNode() || node.getType() == Node.NodeType.CONCEPT;
        boolean hasEvidence = false, hasAction = false, hasCounter = false;
        if (connections != null) {
            for (Connection c : connections.values()) {
                if (c == null) continue;
                if (!node.getId().equals(c.getToNodeId()) && !node.getId().equals(c.getFromNodeId())) continue;
                Node other = nodes.get(node.getId().equals(c.getToNodeId()) ? c.getFromNodeId() : c.getToNodeId());
                if (other == null) continue;
                if (c.getType() == Connection.ConnectionType.EVIDENCE_FOR || c.getType() == Connection.ConnectionType.EVIDENCE_AGAINST || other.getType() == Node.NodeType.EVIDENCE) hasEvidence = true;
                if (other.getType() == Node.NodeType.ACTION || other.getType() == Node.NodeType.TASK) hasAction = true;
                if (other.getType() == Node.NodeType.QUESTION && c.getType() == Connection.ConnectionType.OPPOSES) hasCounter = true;
            }
        }
        if (needsEvidence && !hasEvidence) list.add("缺证据：" + safeTitle(node));
        if (needsAction && !hasAction) list.add("缺下一步：" + safeTitle(node));
        if (needsCounterexample && !hasCounter) list.add("缺反例：" + safeTitle(node));
    }

    public static String buildReadableReport(InsightReport report, Node focusNode) {
        StringBuilder sb = new StringBuilder();
        if (focusNode != null) {
            sb.append("当前节点：").append(safeTitle(focusNode)).append("
");
            List<Node> backlinks = report.backlinks.get(focusNode.getId());
            sb.append("Backlinks：").append(backlinks == null ? 0 : backlinks.size()).append(" 个
");
            if (backlinks != null) {
                for (Node node : backlinks) sb.append("- ").append(safeTitle(node)).append("
");
            }
            sb.append("
");
        }
        sb.append("孤岛节点：").append(report.isolatedNodes.size()).append(" 个
");
        appendTop(sb, report.isolatedNodes);
        sb.append("
冲突证据目标：").append(report.conflictEvidenceTargets.size()).append(" 个
");
        appendTop(sb, report.conflictEvidenceTargets);
        sb.append("
高价值节点推荐：").append(report.highValueNodes.size()).append(" 个
");
        appendTop(sb, report.highValueNodes);
        sb.append("
全局缺口扫描：").append(report.gapFindings.size()).append(" 项
");
        int limit = Math.min(report.gapFindings.size(), 15);
        for (int i = 0; i < limit; i++) sb.append("- ").append(report.gapFindings.get(i)).append("
");
        if (report.gapFindings.size() > limit) sb.append("- ……还有 ").append(report.gapFindings.size() - limit).append(" 项
");
        return sb.toString();
    }

    private static void appendTop(StringBuilder sb, List<Node> nodes) {
        int limit = Math.min(nodes.size(), 8);
        for (int i = 0; i < limit; i++) sb.append("- ").append(safeTitle(nodes.get(i))).append("
");
        if (nodes.isEmpty()) sb.append("- 无
");
    }

    private static String safeTitle(Node node) {
        String title = node == null ? "" : node.getTitle();
        return title == null || title.trim().isEmpty() ? "(无标题)" : title.trim();
    }
}
