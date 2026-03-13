package com.agui.neuralcanvas;

import java.util.Map;

public final class ScientificTriageEngine {

    public static class TriageReport {
        public Node baseNode;
        public int missingExecutionLinks;
        public int missingEvidenceLinks;
        public int missingReviewLinks;
        public int missingLearningLinks;
        public String suggestedAiMode = "gap";
        public String summary;
    }

    private ScientificTriageEngine() {}

    public static TriageReport analyze(Node baseNode, Map<String, Node> nodes, Map<String, Connection> connections) {
        TriageReport report = new TriageReport();
        report.baseNode = baseNode;
        if (baseNode == null) {
            report.summary = "未选中节点";
            return report;
        }

        boolean hasAction = false;
        boolean hasTrigger = !WorkflowEngine.isBlank(baseNode.getTriggerCondition());
        boolean hasEvidence = false;
        boolean hasReview = !WorkflowEngine.isBlank(baseNode.getReviewAt());
        boolean hasLearning = false;

        if (nodes != null) {
            String ownerId = WorkflowEngine.resolveOwnerId(baseNode);
            for (Node node : nodes.values()) {
                if (node == null || node.getId().equals(baseNode.getId())) continue;
                boolean related = ownerId.equals(WorkflowEngine.resolveOwnerId(node))
                        || ownerId.equals(node.getProjectId())
                        || baseNode.getId().equals(node.getProjectId());
                if (!related) continue;
                if (node.getType() == Node.NodeType.ACTION || node.getType() == Node.NodeType.TASK || node.getType() == Node.NodeType.TRIGGER) hasAction = true;
                if (node.getType() == Node.NodeType.EVIDENCE || node.getType() == Node.NodeType.SOURCE) hasEvidence = true;
                if (node.getType() == Node.NodeType.REVIEW) hasReview = true;
                if (node.isLearningNode() || node.getType() == Node.NodeType.QUESTION) hasLearning = true;
            }
        }

        if (!hasAction || !hasTrigger) report.missingExecutionLinks++;
        if (!hasEvidence) report.missingEvidenceLinks++;
        if (!hasReview) report.missingReviewLinks++;
        if (!hasLearning && baseNode.isLearningNode()) report.missingLearningLinks++;

        if (baseNode.isDecisionNode()) report.suggestedAiMode = "decision";
        else if (baseNode.isLearningNode()) report.suggestedAiMode = "learning";
        else if (baseNode.isExecutionNode() || baseNode.getType() == Node.NodeType.PROJECT || baseNode.getType() == Node.NodeType.GOAL) report.suggestedAiMode = "execution";
        else report.suggestedAiMode = "gap";

        report.summary = buildSummary(report);
        return report;
    }

    public static String buildSummary(TriageReport report) {
        if (report == null || report.baseNode == null) return "没有可分析节点";
        StringBuilder sb = new StringBuilder();
        sb.append("节点体检：").append(NodeUiTextFormatter.safeTitle(report.baseNode)).append("\n");
        sb.append("执行缺口：").append(report.missingExecutionLinks).append("\n");
        sb.append("证据缺口：").append(report.missingEvidenceLinks).append("\n");
        sb.append("复盘缺口：").append(report.missingReviewLinks).append("\n");
        sb.append("学习缺口：").append(report.missingLearningLinks).append("\n");
        sb.append("建议 AI 模式：").append(report.suggestedAiMode);
        return sb.toString();
    }
}
