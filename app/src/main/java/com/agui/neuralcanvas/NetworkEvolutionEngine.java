package com.agui.neuralcanvas;

import java.util.*;

public final class NetworkEvolutionEngine {

    public static final class NetworkReport {
        public int duplicateNodeCount = 0;
        public int similarNodeCount = 0;
        public int isolatedHighValueCount = 0;
        public int brokenProjectCount = 0;
        public int brokenKnowledgeCount = 0;
        public int lowValueEdgeCount = 0;
        public final List<String> highlights = new ArrayList<>();

        public String buildSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("网络进化：重复节点 ").append(duplicateNodeCount)
                    .append("，语义相似 ").append(similarNodeCount)
                    .append("，高价值孤点 ").append(isolatedHighValueCount)
                    .append("，项目断链 ").append(brokenProjectCount)
                    .append("，知识断链 ").append(brokenKnowledgeCount)
                    .append("，低价值边 ").append(lowValueEdgeCount);
            for (String item : highlights) sb.append("\n- ").append(item);
            return sb.toString();
        }
    }

    private NetworkEvolutionEngine() {}

    public static NetworkReport analyze(Map<String, Node> nodes, Map<String, Connection> connections) {
        NetworkReport report = new NetworkReport();
        if (nodes == null || nodes.isEmpty()) return report;

        Map<String, List<Node>> titleBuckets = new HashMap<>();
        for (Node node : nodes.values()) {
            if (node == null) continue;
            String key = normalize(node.getTitle());
            if (key.isEmpty()) continue;
            titleBuckets.computeIfAbsent(key, k -> new ArrayList<>()).add(node);
        }

        for (Map.Entry<String, List<Node>> item : titleBuckets.entrySet()) {
            if (item.getValue().size() > 1) {
                report.duplicateNodeCount += item.getValue().size() - 1;
                maybeAdd(report, "重复概念可能需要合并：" + safeTitle(item.getValue().get(0)));
            }
        }

        List<Node> nodeList = new ArrayList<>(nodes.values());
        for (int i = 0; i < nodeList.size(); i++) {
            Node a = nodeList.get(i);
            if (a == null) continue;
            for (int j = i + 1; j < nodeList.size(); j++) {
                Node b = nodeList.get(j);
                if (b == null) continue;
                if (isSimilar(a, b)) {
                    report.similarNodeCount++;
                    maybeAdd(report, "语义相似可桥接/合并：" + safeTitle(a) + " ↔ " + safeTitle(b));
                }
            }
        }

        for (Node node : nodes.values()) {
            if (node == null) continue;
            boolean isolated = isIsolated(node, connections);
            float score = GraphMetaHelper.getFloat(node, "node_priority_score", 0f);

            if (isolated && score >= 22f) {
                report.isolatedHighValueCount++;
                maybeAdd(report, "高价值孤点建议桥接：" + safeTitle(node));
            }

            if (node.getType() == Node.NodeType.PROJECT && !hasExecutionChild(node, nodes, connections)) {
                report.brokenProjectCount++;
                maybeAdd(report, "项目链断裂：" + safeTitle(node));
            }

            if (node.isLearningNode() && !hasKnowledgeChain(node, nodes, connections)) {
                report.brokenKnowledgeCount++;
                maybeAdd(report, "知识链断裂：" + safeTitle(node));
            }
        }

        if (connections != null) {
            Set<String> seen = new HashSet<>();
            for (Connection c : connections.values()) {
                if (c == null) continue;
                String key = safe(c.getFromNodeId()) + "|" + safe(c.getToNodeId()) + "|" + normalize(c.getLabel());
                if (seen.contains(key)) {
                    report.lowValueEdgeCount++;
                    maybeAdd(report, "重复低价值边：" + key);
                } else {
                    seen.add(key);
                }

                String type = c.getType() == null ? "" : c.getType().name();
                if (normalize(c.getLabel()).isEmpty()
                        && ("REFERENCES".equalsIgnoreCase(type) || "LEADS_TO".equalsIgnoreCase(type))) {
                    report.lowValueEdgeCount++;
                }
            }
        }

        return report;
    }

    private static boolean hasExecutionChild(Node node, Map<String, Node> nodes, Map<String, Connection> connections) {
        if (node == null || nodes == null || connections == null) return false;
        for (Connection c : connections.values()) {
            if (c == null || !node.getId().equals(c.getFromNodeId())) continue;
            Node other = nodes.get(c.getToNodeId());
            if (other != null && (other.getType() == Node.NodeType.TASK || other.getType() == Node.NodeType.ACTION)) return true;
        }
        return false;
    }

    private static boolean hasKnowledgeChain(Node node, Map<String, Node> nodes, Map<String, Connection> connections) {
        if (node == null || nodes == null || connections == null) return false;
        boolean hasQuestion = false, hasSource = false;
        for (Connection c : connections.values()) {
            if (c == null) continue;
            String otherId = null;
            if (node.getId().equals(c.getFromNodeId())) otherId = c.getToNodeId();
            else if (node.getId().equals(c.getToNodeId())) otherId = c.getFromNodeId();
            if (otherId == null) continue;
            Node other = nodes.get(otherId);
            if (other == null) continue;
            if (other.getType() == Node.NodeType.QUESTION) hasQuestion = true;
            if (other.getType() == Node.NodeType.SOURCE) hasSource = true;
        }
        return hasQuestion || hasSource;
    }

    private static boolean isIsolated(Node node, Map<String, Connection> connections) {
        if (node == null || connections == null || connections.isEmpty()) return true;
        for (Connection c : connections.values()) {
            if (c == null) continue;
            if (node.getId().equals(c.getFromNodeId()) || node.getId().equals(c.getToNodeId())) return false;
        }
        return true;
    }

    private static boolean isSimilar(Node a, Node b) {
        String ta = normalize(a.getTitle()), tb = normalize(b.getTitle());
        if (ta.isEmpty() || tb.isEmpty() || ta.equals(tb)) return false;
        if (ta.contains(tb) || tb.contains(ta)) return true;
        return tokenOverlap(ta, tb) >= 0.67f;
    }

    private static float tokenOverlap(String a, String b) {
        Set<String> sa = new HashSet<>(Arrays.asList(a.split("\\s+")));
        Set<String> sb = new HashSet<>(Arrays.asList(b.split("\\s+")));
        sa.remove(""); sb.remove("");
        if (sa.isEmpty() || sb.isEmpty()) return 0f;
        int hit = 0;
        for (String s : sa) if (sb.contains(s)) hit++;
        return (float) hit / Math.max(1, Math.min(sa.size(), sb.size()));
    }

    private static void maybeAdd(NetworkReport report, String text) {
        if (report != null && text != null && report.highlights.size() < 8) report.highlights.add(text);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }
    private static String safeTitle(Node node) {
        String title = node == null ? "" : node.getTitle();
        title = title == null ? "" : title.trim();
        return title.isEmpty() ? "未命名节点" : title;
    }
    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
