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
        WorkflowAuditEngine.AuditReport report = WorkflowAuditEngine.analyze(node, nodes, connections);

        StringBuilder message = new StringBuilder();
        message.append("节点：").append(report.title)
                .append("\n主模式：").append(report.dominantLane)
                .append("\n").append(report.summary);

        if (!report.strengths.isEmpty()) {
            message.append("\n\n已有基础：");
            for (String item : report.strengths) {
                message.append("\n• ").append(item);
            }
        }

        if (!report.gaps.isEmpty()) {
            message.append("\n\n主要缺口：");
            for (String gap : report.gaps) {
                message.append("\n• ").append(gap);
            }
        }

        if (!report.checks.isEmpty()) {
            message.append("\n\n检查明细：");
            for (String check : report.checks) {
                message.append("\n• ").append(check);
            }
        }

        final List<WorkflowAuditEngine.SuggestedAction> top = new ArrayList<>(report.actions);
        String[] items = new String[top.size()];
        for (int i = 0; i < top.size(); i++) {
            WorkflowAuditEngine.SuggestedAction item = top.get(i);
            items[i] = item.label + "\n" + item.reason;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle("全方法体检")
                .setMessage(message.toString())
                .setNegativeButton("关闭", null)
                .setNeutralButton("一键修复", (dialog, which) -> {
                    WorkflowQuickFixEngine.FixResult fixResult = WorkflowQuickFixEngine.quickFixNode(activity, node);
                    android.widget.Toast.makeText(activity, fixResult.buildSummary(), android.widget.Toast.LENGTH_LONG).show();
                })
                .setPositiveButton("全量推进", (dialog, which) -> {
                    activity.getMindMapView().selectOnlyNode(node.getId());
                    activity.runScientificAutopilot();
                });

        if (items.length > 0) {
            builder.setItems(items, (dialog, which) -> WorkflowAuditEngine.execute(activity, node, top.get(which)));
        }

        builder.show();
    }
}
