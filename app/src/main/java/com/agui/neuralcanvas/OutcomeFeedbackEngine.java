package com.agui.neuralcanvas;

import java.util.Map;

public final class OutcomeFeedbackEngine {

    private OutcomeFeedbackEngine() {}

    public static void markExecutionLogged(Node node) {
        if (node == null) return;
        GraphMetaHelper.putInt(node, "execution_logs", GraphMetaHelper.getInt(node, "execution_logs", 0) + 1);
        GraphMetaHelper.putLong(node, "last_execution_log_at", System.currentTimeMillis());
    }

    public static void markAiPatched(Node node) {
        if (node == null) return;
        GraphMetaHelper.putInt(node, "ai_patch_count", GraphMetaHelper.getInt(node, "ai_patch_count", 0) + 1);
        GraphMetaHelper.putLong(node, "last_ai_patch_at", System.currentTimeMillis());
    }

    public static void markReviewed(Node node) {
        if (node == null) return;
        GraphMetaHelper.putInt(node, "learning_review_count", GraphMetaHelper.getInt(node, "learning_review_count", 0) + 1);
        GraphMetaHelper.putLong(node, "last_learning_review_at", System.currentTimeMillis());
    }

    public static void backfillFromCommands(Map<String, Node> nodes, AiResponse response) {
        if (nodes == null || response == null || response.getCommands() == null) return;
        for (AiCommand cmd : response.getCommands()) {
            if (cmd == null) continue;
            String nodeId = safe(cmd.getNodeId());
            if (nodeId.isEmpty()) nodeId = safe(cmd.getFromNodeId());
            if (nodeId.isEmpty()) continue;

            Node node = nodes.get(nodeId);
            if (node == null) continue;

            String action = safe(cmd.getAction()).toLowerCase(java.util.Locale.ROOT);
            if ("update_node".equals(action) || "create_connection".equals(action) || "update_connection".equals(action)) {
                markAiPatched(node);
            }
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
