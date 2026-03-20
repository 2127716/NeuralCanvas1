package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AiGraphSummaryBuilder {

    private AiGraphSummaryBuilder() {}

    public static AiGraphSnapshot build(AiGraphSnapshot full, BrainAgentProfile profile) {
        if (full == null || full.nodes == null || full.nodes.isEmpty()) {
            return full == null ? new AiGraphSnapshot() : full;
        }
        if (full.nodes.size() <= 24) {
            return full;
        }

        Set<String> keepNodeIds = new HashSet<>();
        List<AiGraphSnapshot.SnapshotNode> ranked = new ArrayList<>(full.nodes);
        ranked.sort(new Comparator<AiGraphSnapshot.SnapshotNode>() {
            @Override
            public int compare(AiGraphSnapshot.SnapshotNode a, AiGraphSnapshot.SnapshotNode b) {
                return Integer.compare(scoreNode(b, profile), scoreNode(a, profile));
            }
        });

        int budget = resolveBudget(profile, ranked.size());
        for (int i = 0; i < ranked.size() && i < budget; i++) {
            keepNodeIds.add(ranked.get(i).id);
        }

        for (AiGraphSnapshot.SnapshotConnection c : full.connections) {
            if (keepNodeIds.contains(c.fromNodeId) || keepNodeIds.contains(c.toNodeId)) {
                keepNodeIds.add(c.fromNodeId);
                keepNodeIds.add(c.toNodeId);
            }
        }

        AiGraphSnapshot result = new AiGraphSnapshot();
        for (AiGraphSnapshot.SnapshotNode node : full.nodes) {
            if (keepNodeIds.contains(node.id)) {
                result.nodes.add(trimNode(node));
            }
        }
        for (AiGraphSnapshot.SnapshotConnection c : full.connections) {
            if (keepNodeIds.contains(c.fromNodeId) && keepNodeIds.contains(c.toNodeId)) {
                result.connections.add(c);
            }
        }
        return result.nodes.isEmpty() ? full : result;
    }

    private static int resolveBudget(BrainAgentProfile profile, int nodeCount) {
        if (profile == BrainAgentProfile.NETWORK) return Math.min(36, Math.max(20, nodeCount / 2));
        if (profile == BrainAgentProfile.GENERAL) return Math.min(28, Math.max(18, nodeCount / 3));
        return Math.min(24, Math.max(16, nodeCount / 3));
    }

    private static int scoreNode(AiGraphSnapshot.SnapshotNode node, BrainAgentProfile profile) {
        if (node == null) return 0;
        String type = safeLower(node.type);
        String text = safeLower(node.title) + " " + safeLower(node.content);

        int score = 0;
        if (containsAny(type, "project", "goal", "decision", "key_result")) score += 10;
        if (containsAny(type, "action", "task", "review", "evidence", "question", "experiment", "risk")) score += 7;
        if (containsAny(text, "今天", "本周", "next", "下一步", "review", "风险", "证据", "复习", "迁移")) score += 5;

        switch (profile) {
            case EXECUTION:
                if (containsAny(type, "action", "task", "goal", "project", "review", "trigger", "obstacle")) score += 15;
                break;
            case DECISION:
                if (containsAny(type, "decision", "option", "criterion", "assumption", "risk", "evidence")) score += 15;
                break;
            case LEARNING:
                if (containsAny(type, "concept", "question", "resource", "source", "note", "experiment", "review")) score += 15;
                break;
            case NETWORK:
                if (containsAny(type, "project", "goal", "decision", "concept", "note")) score += 8;
                break;
            default:
                break;
        }
        return score;
    }

    private static AiGraphSnapshot.SnapshotNode trimNode(AiGraphSnapshot.SnapshotNode src) {
        AiGraphSnapshot.SnapshotNode node = new AiGraphSnapshot.SnapshotNode();
        node.id = src.id;
        node.title = trim(src.title, 48);
        node.content = trim(src.content, 160);
        node.type = src.type;
        node.shape = src.shape;
        node.x = src.x;
        node.y = src.y;
        node.width = src.width;
        node.height = src.height;
        return node;
    }

    private static boolean containsAny(String text, String... items) {
        String src = safeLower(text);
        for (String item : items) {
            if (src.contains(safeLower(item))) return true;
        }
        return false;
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
