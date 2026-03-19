package com.agui.neuralcanvas;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WorkflowHealthDialog {

    private WorkflowHealthDialog() {}

    public static void show(MainActivity activity, Node node) {
        if (activity == null || node == null) return;

        Map<String, Node> nodes = activity.getMindMapView().getNodesInternal();
        Map<String, Connection> connections = activity.getMindMapView().getConnectionsInternal();
        WorkflowMethodRecommendationEngine.Analysis analysis = WorkflowMethodRecommendationEngine.analyze(node, nodes, connections);

        List<String> sections = new ArrayList<>();
        sections.add(buildCoreHealth(node, nodes, connections));
        sections.add(buildProjectHealth(node, nodes));
        sections.add(buildEvidenceHealth(node));
        sections.add(buildScheduleHealth(node));

        StringBuilder message = new StringBuilder();
        for (String item : sections) {
            if (item == null || item.trim().isEmpty()) continue;
            if (message.length() > 0) message.append("\n\n");
            message.append(item);
        }
        if (!analysis.gaps.isEmpty()) {
            message.append("\n\n主要缺口：");
            for (String gap : analysis.gaps) {
                message.append("\n• ").append(gap);
            }
        }

        final List<WorkflowMethodRecommendationEngine.Recommendation> top = new ArrayList<>();
        int limit = Math.min(5, analysis.recommendations.size());
        String[] items = new String[limit];
        for (int i = 0; i < limit; i++) {
            WorkflowMethodRecommendationEngine.Recommendation item = analysis.recommendations.get(i);
            top.add(item);
            items[i] = item.label + "\n" + item.reason;
        }

        new AlertDialog.Builder(activity)
                .setTitle("工作流体检")
                .setMessage(message.toString())
                .setItems(items, (dialog, which) -> WorkflowMethodRecommendationEngine.execute(activity, node, top.get(which)))
                .setPositiveButton("一键修复", (dialog, which) -> {
                    WorkflowQuickFixEngine.FixResult fixResult = WorkflowQuickFixEngine.quickFixNode(activity, node);
                    android.widget.Toast.makeText(activity, fixResult.buildSummary(), android.widget.Toast.LENGTH_LONG).show();
                })
                .setNeutralButton("主模式入口", (dialog, which) -> WorkflowModeDialog.show(activity, node, analysis.dominantMode))
                .setNegativeButton("关闭", null)
                .show();
    }

    private static String buildCoreHealth(Node node, Map<String, Node> nodes, Map<String, Connection> connections) {
        boolean hasAction = hasRelatedType(node, nodes, Node.NodeType.ACTION, Node.NodeType.TASK);
        boolean hasTrigger = !WorkflowEngine.isBlank(node.getTriggerCondition()) || hasRelatedType(node, nodes, Node.NodeType.TRIGGER);
        boolean hasObstacle = hasRelatedType(node, nodes, Node.NodeType.OBSTACLE);
        boolean hasReview = hasRelatedType(node, nodes, Node.NodeType.REVIEW);

        StringBuilder sb = new StringBuilder();
        sb.append("执行链：");
        sb.append("\n• 下一步：").append(hasAction ? "有" : "缺");
        sb.append("\n• 触发条件：").append(hasTrigger ? "有" : "缺");
        sb.append("\n• 障碍分析：").append(hasObstacle ? "有" : "缺");
        sb.append("\n• 反馈复盘：").append(hasReview ? "有" : "缺");
        if (node.getStatus() == Node.NodeStatus.BLOCKED) {
            sb.append("\n• 当前状态：已阻塞，建议先补障碍→预防动作");
        }
        return sb.toString();
    }

    private static String buildProjectHealth(Node node, Map<String, Node> nodes) {
        String ownerId = WorkflowEngine.resolveOwnerId(node);
        if (WorkflowEngine.isBlank(ownerId)) return "";

        int actionCount = 0;
        int reviewCount = 0;
        int doneCount = 0;
        int blockedCount = 0;

        for (Node item : nodes.values()) {
            if (item == null) continue;
            if (!ownerId.equals(WorkflowEngine.resolveOwnerId(item)) && !ownerId.equals(item.getProjectId())) continue;
            if (item.isExecutionNode()) actionCount++;
            if (item.getType() == Node.NodeType.REVIEW) reviewCount++;
            if (item.getStatus() == Node.NodeStatus.DONE) doneCount++;
            if (item.getStatus() == Node.NodeStatus.BLOCKED) blockedCount++;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("项目聚合：");
        sb.append("\n• 执行类节点：").append(actionCount);
        sb.append("\n• 复盘类节点：").append(reviewCount);
        sb.append("\n• 已完成节点：").append(doneCount);
        sb.append("\n• 阻塞节点：").append(blockedCount);
        if (actionCount == 0 && (node.getType() == Node.NodeType.PROJECT || node.getType() == Node.NodeType.GOAL)) {
            sb.append("\n• 警告：项目/目标还没有开放行动节点");
        }
        return sb.toString();
    }

    private static String buildEvidenceHealth(Node node) {
        StringBuilder sb = new StringBuilder();
        sb.append("判断质量：");
        sb.append("\n• 置信度：").append(format01(node.getConfidence()));
        sb.append("\n• 证据强度：").append(format01(node.getEvidenceStrength()));
        if (node.getConfidence() > 0.72f && node.getEvidenceStrength() < 0.5f) {
            sb.append("\n• 提醒：置信度偏高但证据偏弱，建议做 Bayes / 证据审查");
        }
        return sb.toString();
    }

    private static String buildScheduleHealth(Node node) {
        StringBuilder sb = new StringBuilder();
        sb.append("节奏字段：");
        sb.append("\n• 截止时间：").append(WorkflowEngine.isBlank(node.getDueAt()) ? "未填" : node.getDueAt());
        sb.append("\n• 复习/回顾：").append(WorkflowEngine.isBlank(node.getReviewAt()) ? "未填" : node.getReviewAt());
        sb.append("\n• 预计耗时：").append(node.getEffortEstimate());
        sb.append("\n• 实际耗时：").append(node.getActualEffort());
        return sb.toString();
    }

    private static boolean hasRelatedType(Node baseNode, Map<String, Node> nodes, Node.NodeType... targetTypes) {
        if (baseNode == null || nodes == null) return false;
        String ownerId = WorkflowEngine.resolveOwnerId(baseNode);
        for (Node item : nodes.values()) {
            if (item == null || item.getId().equals(baseNode.getId())) continue;
            boolean matched = false;
            for (Node.NodeType type : targetTypes) {
                if (item.getType() == type) {
                    matched = true;
                    break;
                }
            }
            if (!matched) continue;
            if (!WorkflowEngine.isBlank(ownerId) && ownerId.equals(WorkflowEngine.resolveOwnerId(item))) {
                return true;
            }
            float dx = item.getX() - baseNode.getX();
            float dy = item.getY() - baseNode.getY();
            if (Math.sqrt(dx * dx + dy * dy) <= 920f) return true;
        }
        return false;
    }

    private static String format01(float value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
