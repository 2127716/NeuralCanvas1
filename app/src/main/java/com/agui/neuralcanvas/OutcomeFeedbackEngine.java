package com.agui.neuralcanvas;

import java.util.Map;

public final class OutcomeFeedbackEngine {
    private static final long EFFECTIVE_WINDOW_MS = 7L * 24L * 60L * 60L * 1000L;

    private OutcomeFeedbackEngine() {}

    public static void markExecutionLogged(Node node) {
        if (node == null) return;
        GraphMetaHelper.putInt(node, "execution_logs", GraphMetaHelper.getInt(node, "execution_logs", 0) + 1);
        GraphMetaHelper.putLong(node, "last_execution_log_at", System.currentTimeMillis());
        maybeMarkSuggestionEffective(node, "execution");
    }

    public static void markAiPatched(Node node) {
        if (node == null) return;
        GraphMetaHelper.putInt(node, "ai_patch_count", GraphMetaHelper.getInt(node, "ai_patch_count", 0) + 1);
        GraphMetaHelper.putLong(node, "last_ai_patch_at", System.currentTimeMillis());
        GraphMetaHelper.putBoolean(node, "ai_patch_waiting_effectiveness", true);
    }

    public static void markReviewed(Node node) {
        if (node == null) return;
        GraphMetaHelper.putInt(node, "learning_review_count", GraphMetaHelper.getInt(node, "learning_review_count", 0) + 1);
        GraphMetaHelper.putLong(node, "last_learning_review_at", System.currentTimeMillis());
        maybeMarkSuggestionEffective(node, "learning");
    }

    public static void markFocused(Node node) {
        if (node == null) return;
        GraphMetaHelper.putLong(node, "last_focus_touch_at", System.currentTimeMillis());
        maybeMarkSuggestionEffective(node, "focus");
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

    private static void maybeMarkSuggestionEffective(Node node, String channel) {
        if (node == null) return;
        boolean waiting = GraphMetaHelper.getBoolean(node, "ai_patch_waiting_effectiveness", false);
        long lastPatchAt = GraphMetaHelper.getLong(node, "last_ai_patch_at", 0L);
        long now = System.currentTimeMillis();

        if (waiting && lastPatchAt > 0L && now - lastPatchAt <= EFFECTIVE_WINDOW_MS) {
            GraphMetaHelper.putInt(node, "ai_patch_effective_count", GraphMetaHelper.getInt(node, "ai_patch_effective_count", 0) + 1);
            GraphMetaHelper.putString(node, "ai_patch_effective_channel", channel);
            GraphMetaHelper.putBoolean(node, "ai_patch_waiting_effectiveness", false);
        }
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
