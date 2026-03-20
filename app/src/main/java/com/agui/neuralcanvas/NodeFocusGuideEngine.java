package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class NodeFocusGuideEngine {

    public static final class GuideItem {
        public final String nodeId;
        public final String title;
        public final String hint;
        public final int score;

        public GuideItem(String nodeId, String title, String hint, int score) {
            this.nodeId = nodeId;
            this.title = title;
            this.hint = hint;
            this.score = score;
        }
    }

    public static final class GuideReport {
        public final List<GuideItem> items = new ArrayList<>();
        public final List<String> notes = new ArrayList<>();
        public String headline = "下一步引导";
    }

    private NodeFocusGuideEngine() {}

    // 兼容 BrainCoachDialog 的调用
    public static GuideReport buildForNode(MainActivity activity, Node baseNode) {
        GuideReport report = new GuideReport();
        if (activity == null || activity.getMindMapView() == null || baseNode == null) return report;

        Map<String, Node> nodes = activity.getMindMapView().getNodesInternal();

        report.items.add(new GuideItem(baseNode.getId(), safeTitle(baseNode), "当前焦点节点", score(baseNode)));

        addIfPresent(report, findNearby(nodes, baseNode, Node.NodeType.ACTION, Node.NodeType.TASK));
        addIfPresent(report, findNearby(nodes, baseNode, Node.NodeType.TRIGGER));
        addIfPresent(report, findNearby(nodes, baseNode, Node.NodeType.REVIEW));
        addIfPresent(report, findNearby(nodes, baseNode, Node.NodeType.EVIDENCE));
        addIfPresent(report, findNearby(nodes, baseNode, Node.NodeType.OBSTACLE, Node.NodeType.RISK));
        addIfPresent(report, findNearby(nodes, baseNode, Node.NodeType.QUESTION));
        addIfPresent(report, findNearby(nodes, baseNode, Node.NodeType.EXPERIMENT));

        report.items.sort((a, b) -> Integer.compare(b.score, a.score));
        if (!report.items.isEmpty()) {
            report.headline = "建议先看：" + report.items.get(0).title;
        }
        report.notes.add("已根据当前节点及邻近结构生成聚焦引导");
        return report;
    }

    public static GuideReport buildForFix(MainActivity activity,
                                          Node baseNode,
                                          WorkflowQuickFixEngine.FixResult fixResult) {
        GuideReport report = new GuideReport();
        if (activity == null || activity.getMindMapView() == null || baseNode == null) return report;

        Map<String, Node> nodes = activity.getMindMapView().getNodesInternal();
        if (fixResult != null) {
            for (String note : fixResult.notes) {
                report.notes.add(note);
            }
        }

        report.items.add(new GuideItem(baseNode.getId(), safeTitle(baseNode), "当前焦点节点", score(baseNode)));

        GuideReport around = buildForNode(activity, baseNode);
        for (GuideItem item : around.items) {
            if (!contains(report, item.nodeId)) {
                report.items.add(item);
            }
        }

        report.items.sort((a, b) -> Integer.compare(b.score, a.score));
        if (!report.items.isEmpty()) {
            report.headline = "修复后建议先处理：" + report.items.get(0).title;
        }
        return report;
    }

    private static void addIfPresent(GuideReport report, Node node) {
        if (node == null || contains(report, node.getId())) return;
        report.items.add(new GuideItem(node.getId(), safeTitle(node), resolveHint(node), score(node)));
    }

    private static Node findNearby(Map<String, Node> nodes, Node baseNode, Node.NodeType... types) {
        if (nodes == null || baseNode == null || types == null) return null;
        Node best = null;
        int bestScore = Integer.MIN_VALUE;

        for (Node node : nodes.values()) {
            if (node == null || node.getId().equals(baseNode.getId())) continue;
            boolean matched = false;
            for (Node.NodeType type : types) {
                if (node.getType() == type) {
                    matched = true;
                    break;
                }
            }
            if (!matched) continue;

            int candidateScore = score(node);
            if (sameOwner(baseNode, node)) candidateScore += 25;
            if (distance(baseNode, node) <= 920f) candidateScore += 15;

            if (candidateScore > bestScore) {
                bestScore = candidateScore;
                best = node;
            }
        }
        return best;
    }

    private static boolean sameOwner(Node a, Node b) {
        String ownerA = WorkflowEngine.resolveOwnerId(a);
        String ownerB = WorkflowEngine.resolveOwnerId(b);
        return !WorkflowEngine.isBlank(ownerA) && ownerA.equals(ownerB);
    }

    private static double distance(Node a, Node b) {
        float dx = a.getX() - b.getX();
        float dy = a.getY() - b.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static boolean contains(GuideReport report, String id) {
        for (GuideItem item : report.items) {
            if (item.nodeId.equals(id)) return true;
        }
        return false;
    }

    private static String safeTitle(Node node) {
        String title = node == null ? "" : WorkflowEngine.safe(node.getTitle());
        return title.isEmpty() ? "(无标题)" : title;
    }

    private static int score(Node node) {
        if (node == null) return 0;
        int score = 40 + node.getPriority() * 3;
        switch (node.getType()) {
            case ACTION:
            case TASK: score += 50; break;
            case TRIGGER: score += 48; break;
            case REVIEW: score += 35; break;
            case KEY_RESULT: score += 32; break;
            case EVIDENCE: score += 30; break;
            case OBSTACLE:
            case RISK: score += 28; break;
            case QUESTION: score += 26; break;
            case EXPERIMENT: score += 24; break;
            default: break;
        }
        if (node.getStatus() == Node.NodeStatus.ACTIVE) score += 8;
        if (!WorkflowEngine.isBlank(node.getTriggerCondition())) score += 8;
        return score;
    }

    private static String resolveHint(Node node) {
        if (node == null) return "";
        switch (node.getType()) {
            case ACTION:
            case TASK: return "先做这一步，最容易产生真实推进";
            case TRIGGER: return "先设触发条件，系统才会自动化";
            case REVIEW: return "这里决定下一轮怎么修正";
            case KEY_RESULT: return "这里量化你是否真的前进";
            case EVIDENCE: return "这里决定判断是否站得住";
            case QUESTION: return "先主动回忆，再判断是否掌握";
            case EXPERIMENT: return "这里负责迁移验证";
            case OBSTACLE:
            case RISK: return "先看失败点，减少白忙";
            default: return "这是当前结构中的关键节点";
        }
    }
}
