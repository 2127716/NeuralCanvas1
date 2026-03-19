package com.agui.neuralcanvas;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

public final class WorkflowRecommendationDialog {

    private WorkflowRecommendationDialog() {}

    public static void show(MainActivity activity, Node node) {
        if (activity == null || node == null) return;

        WorkflowMethodRecommendationEngine.Analysis analysis =
                WorkflowMethodRecommendationEngine.analyze(
                        node,
                        activity.getMindMapView().getNodesInternal(),
                        activity.getMindMapView().getConnectionsInternal()
                );

        final List<WorkflowMethodRecommendationEngine.Recommendation> list = new ArrayList<>(analysis.recommendations);
        String[] items = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            WorkflowMethodRecommendationEngine.Recommendation item = list.get(i);
            items[i] = (i + 1) + ". " + item.label + "\n" + item.reason;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle("方法推荐")
                .setMessage(WorkflowMethodRecommendationEngine.buildReadableReport(analysis));

        if (items.length > 0) {
            builder.setItems(items, (dialog, which) ->
                    WorkflowMethodRecommendationEngine.execute(activity, node, list.get(which))
            );
        }

        builder.setPositiveButton("主模式入口", (dialog, which) ->
                WorkflowModeDialog.show(activity, node, analysis.dominantMode)
        );
        builder.setNegativeButton("关闭", null);
        builder.show();
    }
}
