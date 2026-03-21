package com.agui.neuralcanvas;

import java.util.Map;

public final class SuggestionEffectivenessTracker {

    private SuggestionEffectivenessTracker() {}

    public static int countEffectivePatchedNodes(Map<String, Node> nodes) {
        if (nodes == null) return 0;
        int count = 0;
        for (Node node : nodes.values()) {
            if (node == null) continue;
            if (GraphMetaHelper.getInt(node, "ai_patch_effective_count", 0) > 0) count++;
        }
        return count;
    }

    public static String buildSummary(Map<String, Node> nodes) {
        int count = countEffectivePatchedNodes(nodes);
        return "AI补丁成效：已有 " + count + " 个节点出现了补丁后的真实使用/复习/执行反馈";
    }
}
