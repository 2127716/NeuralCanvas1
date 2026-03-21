package com.agui.neuralcanvas;

import java.util.Locale;
import java.util.Map;

public final class NodeIntelligenceEngine {

    public static final class NodeProfile {
        public int focusHits;
        public int focusSessions;
        public int triggerHits;
        public int triggerMisses;
        public int memoryReviews;
        public int memoryLapses;
        public int issueCount;
        public boolean isolated;
        public float priorityScore;

        public String buildSummary() {
            return "聚焦=" + focusHits
                    + "｜专注=" + focusSessions
                    + "｜触发命中=" + triggerHits
                    + "/" + (triggerHits + triggerMisses)
                    + "｜复习=" + memoryReviews
                    + "｜遗忘=" + memoryLapses
                    + "｜问题累计=" + issueCount
                    + "｜优先级="
                    + String.format(Locale.US, "%.1f", priorityScore);
        }
    }

    private NodeIntelligenceEngine() {}

    public static NodeProfile build(Node node,
                                    Map<String, Node> nodes,
                                    Map<String, Connection> connections,
                                    BehaviorMemoryProfile memoryProfile) {
        NodeProfile profile = new NodeProfile();
        if (node == null) return profile;

        profile.focusHits = GraphMetaHelper.getInt(node, "focus_hits", 0);
        profile.focusSessions = GraphMetaHelper.getInt(node, "focus_sessions", 0);
        profile.triggerHits = GraphMetaHelper.getInt(node, "trigger_hits", 0);
        profile.triggerMisses = GraphMetaHelper.getInt(node, "trigger_misses", 0);
        profile.memoryReviews = GraphMetaHelper.getInt(node, "memory_reviews", 0);
        profile.memoryLapses = GraphMetaHelper.getInt(node, "memory_lapses", 0);
        profile.issueCount = GraphMetaHelper.getInt(node, "issue_count", 0);
        profile.isolated = isIsolated(node, connections);
        profile.priorityScore = score(node, profile, memoryProfile);

        GraphMetaHelper.putInt(node, "node_focus_hits_cached", profile.focusHits);
        GraphMetaHelper.putInt(node, "node_focus_sessions_cached", profile.focusSessions);
        GraphMetaHelper.putInt(node, "node_memory_reviews_cached", profile.memoryReviews);
        GraphMetaHelper.putInt(node, "node_memory_lapses_cached", profile.memoryLapses);
        GraphMetaHelper.putBoolean(node, "node_isolated_cached", profile.isolated);
        GraphMetaHelper.putFloat(node, "node_priority_score", profile.priorityScore);

        return profile;
    }

    public static void markFocus(Node node) {
        if (node == null) return;
        GraphMetaHelper.putInt(node, "focus_hits", GraphMetaHelper.getInt(node, "focus_hits", 0) + 1);
    }

    public static void markIssue(Node node) {
        if (node == null) return;
        GraphMetaHelper.putInt(node, "issue_count", GraphMetaHelper.getInt(node, "issue_count", 0) + 1);
    }

    private static boolean isIsolated(Node node, Map<String, Connection> connections) {
        if (node == null || connections == null || connections.isEmpty()) return true;
        for (Connection c : connections.values()) {
            if (c == null) continue;
            if (node.getId().equals(c.getFromNodeId()) || node.getId().equals(c.getToNodeId())) return false;
        }
        return true;
    }

    private static float score(Node node, NodeProfile profile, BehaviorMemoryProfile memoryProfile) {
        float score = 0f;

        if (node.isExecutionNode()) score += 25f;
        if (node.isDecisionNode()) score += 22f;
        if (node.isLearningNode()) score += 18f;

        score += Math.min(18f, profile.issueCount * 3f);
        score += Math.min(12f, profile.focusHits * 1.5f);
        score += Math.min(10f, profile.memoryLapses * 2f);
        score += Math.min(8f, profile.triggerMisses * 2f);

        if (profile.isolated) score += 8f;

        if (node.isLearningNode()) {
            long dueAt = MemoryEngine.getDueAt(node);
            if (dueAt <= System.currentTimeMillis()) score += 20f;
        }

        if (node.isExecutionNode()) {
            if ((node.getType() == Node.NodeType.TASK || node.getType() == Node.NodeType.ACTION)
                    && (safe(node.getTriggerCondition()).isEmpty()
                    || (safe(node.getDueAt()).isEmpty() && safe(node.getReviewAt()).isEmpty()))) {
                score += 18f;
            }
        }

        if (memoryProfile != null && memoryProfile.totalPulses > 0) {
            score += Math.min(5f, memoryProfile.autoAppliedCount * 0.2f);
        }

        return score;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
