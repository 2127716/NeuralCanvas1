package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class NetworkBridgePlanner {

    public static final class BridgeReport {
        public final List<String> suggestions = new ArrayList<>();

        public String buildSummary() {
            if (suggestions.isEmpty()) return "暂无明显桥接建议";
            StringBuilder sb = new StringBuilder();
            sb.append("桥接建议：");
            for (String s : suggestions) sb.append("\n- ").append(s);
            return sb.toString();
        }
    }

    private NetworkBridgePlanner() {}

    public static BridgeReport analyze(Map<String, Node> nodes, Map<String, Connection> connections) {
        BridgeReport report = new BridgeReport();
        if (nodes == null || nodes.isEmpty()) return report;

        List<Node> all = new ArrayList<>(nodes.values());
        for (int i = 0; i < all.size(); i++) {
            Node a = all.get(i);
            if (a == null) continue;
            boolean isolatedA = isIsolated(a, connections);
            float scoreA = GraphMetaHelper.getFloat(a, "node_priority_score", 0f);
            if (!isolatedA || scoreA < 18f) continue;

            Node best = null;
            float bestScore = 0f;
            for (int j = 0; j < all.size(); j++) {
                if (i == j) continue;
                Node b = all.get(j);
                if (b == null) continue;
                float sim = similarity(a, b);
                if (sim > bestScore) {
                    bestScore = sim;
                    best = b;
                }
            }

            if (best != null && bestScore >= 0.45f) {
                report.suggestions.add("将“" + safeTitle(a) + "”桥接到“" + safeTitle(best) + "”附近，减少高价值孤点");
                if (report.suggestions.size() >= 6) break;
            }
        }
        return report;
    }

    private static boolean isIsolated(Node node, Map<String, Connection> connections) {
        if (node == null || connections == null) return true;
        for (Connection c : connections.values()) {
            if (c == null) continue;
            if (node.getId().equals(c.getFromNodeId()) || node.getId().equals(c.getToNodeId())) return false;
        }
        return true;
    }

    private static float similarity(Node a, Node b) {
        String ta = norm(a.getTitle());
        String tb = norm(b.getTitle());
        if (ta.isEmpty() || tb.isEmpty()) return 0f;
        if (ta.equals(tb)) return 1f;
        if (ta.contains(tb) || tb.contains(ta)) return 0.8f;
        String ca = norm(a.getContent());
        String cb = norm(b.getContent());
        float titleHit = overlap(ta, tb);
        float contentHit = overlap(ca, cb);
        return Math.max(titleHit, contentHit * 0.8f);
    }

    private static float overlap(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) return 0f;
        String[] aa = a.split("\\s+");
        String[] bb = b.split("\\s+");
        int hit = 0;
        for (String x : aa) {
            if (x.isEmpty()) continue;
            for (String y : bb) {
                if (x.equals(y)) { hit++; break; }
            }
        }
        return (float) hit / Math.max(1, Math.min(aa.length, bb.length));
    }

    private static String safeTitle(Node node) {
        String t = node == null ? "" : node.getTitle();
        t = t == null ? "" : t.trim();
        return t.isEmpty() ? "未命名节点" : t;
    }

    private static String norm(String value) {
        return value == null ? "" : value.trim().toLowerCase().replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }
}
