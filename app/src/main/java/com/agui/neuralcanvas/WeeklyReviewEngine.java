package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WeeklyReviewEngine {

    private WeeklyReviewEngine() {}

    public static class ReviewReport {
        public final List<Node> inboxNodes;
        public final List<Node> nextActions;
        public final List<Node> stuckProjects;
        public final List<Node> reviewDueNodes;
        public final List<Node> waitingNodes;
        public final List<Node> blockedNodes;

        public ReviewReport(List<Node> inboxNodes,
                            List<Node> nextActions,
                            List<Node> stuckProjects,
                            List<Node> reviewDueNodes,
                            List<Node> waitingNodes,
                            List<Node> blockedNodes) {
            this.inboxNodes = inboxNodes;
            this.nextActions = nextActions;
            this.stuckProjects = stuckProjects;
            this.reviewDueNodes = reviewDueNodes;
            this.waitingNodes = waitingNodes;
            this.blockedNodes = blockedNodes;
        }
    }

    public static ReviewReport build(Map<String, Node> nodes,
                                     Map<String, Connection> connections) {
        List<Node> inbox = WorkflowEngine.getInboxNodes(nodes);
        List<Node> next = WorkflowEngine.getNextActions(nodes, connections);
        List<Node> stuck = WorkflowEngine.getStuckProjects(nodes, connections);
        List<Node> reviewDue = WorkflowEngine.getReviewDueNodes(nodes);
        List<Node> waiting = new ArrayList<>();
        List<Node> blocked = new ArrayList<>();

        if (nodes != null) {
            for (Node node : nodes.values()) {
                if (node == null) continue;
                if (WorkflowEngine.isWaiting(node)) waiting.add(node);
                if (WorkflowEngine.isBlocked(node)) blocked.add(node);
            }
        }

        return new ReviewReport(inbox, next, stuck, reviewDue, waiting, blocked);
    }

    public static String buildReadableSummary(ReviewReport report) {
        if (report == null) return "暂无复盘结果。";

        StringBuilder sb = new StringBuilder();
        sb.append("每周复盘概览\n\n");

        sb.append("1. Inbox 待澄清：").append(report.inboxNodes.size()).append(" 个\n");
        appendTopTitles(sb, report.inboxNodes);

        sb.append("\n2. 可执行下一步：").append(report.nextActions.size()).append(" 个\n");
        appendTopTitles(sb, report.nextActions);

        sb.append("\n3. 卡住的项目：").append(report.stuckProjects.size()).append(" 个\n");
        appendTopTitles(sb, report.stuckProjects);

        sb.append("\n4. 待复盘 / 待回顾：").append(report.reviewDueNodes.size()).append(" 个\n");
        appendTopTitles(sb, report.reviewDueNodes);

        sb.append("\n5. 等待中的节点：").append(report.waitingNodes.size()).append(" 个\n");
        appendTopTitles(sb, report.waitingNodes);

        sb.append("\n6. 受阻节点：").append(report.blockedNodes.size()).append(" 个\n");
        appendTopTitles(sb, report.blockedNodes);

        sb.append("\n建议动作：\n");
        if (!report.inboxNodes.isEmpty()) {
            sb.append("- 先清空 Inbox，避免认知堆积。\n");
        }
        if (!report.stuckProjects.isEmpty()) {
            sb.append("- 给每个卡住项目补一个最小下一步动作。\n");
        }
        if (!report.reviewDueNodes.isEmpty()) {
            sb.append("- 把待复盘节点逐个处理，补出洞察和后续动作。\n");
        }
        if (!report.blockedNodes.isEmpty()) {
            sb.append("- 检查阻塞原因，转成风险或依赖节点。\n");
        }
        if (report.inboxNodes.isEmpty()
                && report.stuckProjects.isEmpty()
                && report.reviewDueNodes.isEmpty()
                && report.blockedNodes.isEmpty()) {
            sb.append("- 当前系统整体比较顺，维持节奏即可。\n");
        }

        return sb.toString();
    }

    private static void appendTopTitles(StringBuilder sb, List<Node> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            sb.append("  - 无\n");
            return;
        }

        int limit = Math.min(nodes.size(), 5);
        for (int i = 0; i < limit; i++) {
            Node node = nodes.get(i);
            sb.append("  - ").append(safeTitle(node)).append("\n");
        }
        if (nodes.size() > limit) {
            sb.append("  - ……还有 ").append(nodes.size() - limit).append(" 个\n");
        }
    }

    private static String safeTitle(Node node) {
        if (node == null) return "(空节点)";
        String title = node.getTitle();
        if (title == null || title.trim().isEmpty()) return "(无标题)";
        return title.trim();
    }
}
