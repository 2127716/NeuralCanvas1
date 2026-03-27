package com.agui.neuralcanvas;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NextStepCoachDialog {

    private NextStepCoachDialog() {}

    public static void show(MainActivity activity, Node node) {
        if (activity == null || node == null || activity.isFinishing() || activity.isDestroyed()) return;

        WorkflowMethodRecommendationEngine.Analysis analysis =
                WorkflowMethodRecommendationEngine.analyze(
                        node,
                        activity.getMindMapView().getNodesInternal(),
                        activity.getMindMapView().getConnectionsInternal()
                );

        final List<WorkflowMethodRecommendationEngine.Recommendation> recommendations =
                new ArrayList<>(analysis.recommendations);

        StringBuilder summary = new StringBuilder();
        summary.append("现在最值得你推进的节点：\n");
        summary.append("• ").append(safeTitle(node));
        summary.append("\n• 类型：").append(node.getType().label);
        summary.append("\n• 状态：").append(node.getStatus().label);
        summary.append("\n• 主模式：").append(WorkflowMethodRecommendationEngine.resolveModeLabel(analysis.dominantMode));

        if (!WorkflowEngine.isBlank(node.getDueAt())) {
            summary.append("\n• 截止：").append(node.getDueAt().trim());
        }
        if (!WorkflowEngine.isBlank(node.getReviewAt())) {
            summary.append("\n• 复盘/复习：").append(node.getReviewAt().trim());
        }
        if (node.getPriority() > 0) {
            summary.append("\n• 优先级：P").append(node.getPriority());
        }

        if (!analysis.gaps.isEmpty()) {
            summary.append("\n\n当前缺口：");
            int gapCount = Math.min(3, analysis.gaps.size());
            for (int i = 0; i < gapCount; i++) {
                summary.append("\n- ").append(analysis.gaps.get(i));
            }
        }

        if (!recommendations.isEmpty()) {
            summary.append("\n\n推荐顺序：");
            int limit = Math.min(3, recommendations.size());
            for (int i = 0; i < limit; i++) {
                WorkflowMethodRecommendationEngine.Recommendation r = recommendations.get(i);
                summary.append("\n").append(i + 1).append(". ").append(r.label).append("：").append(r.reason);
            }
        } else {
            summary.append("\n\n这个节点已经比较完整，建议直接推进或进入主模式复盘。\n");
        }

        String positiveLabel = recommendations.isEmpty() ? "进入主模式" : "先做第 1 步";
        String neutralLabel = resolveNeutralLabel(analysis.dominantMode);

        new AlertDialog.Builder(activity)
                .setTitle("下一步教练")
                .setMessage(summary.toString())
                .setPositiveButton(positiveLabel, (dialog, which) -> {
                    if (!recommendations.isEmpty()) {
                        WorkflowMethodRecommendationEngine.execute(activity, node, recommendations.get(0));
                    } else {
                        WorkflowModeDialog.show(activity, node, analysis.dominantMode);
                    }
                })
                .setNeutralButton(neutralLabel, (dialog, which) -> openNeutralAction(activity, node, analysis))
                .setNegativeButton("看看全部", (dialog, which) -> showAllRecommendations(activity, node, analysis, recommendations))
                .show();
    }

    private static void showAllRecommendations(MainActivity activity,
                                               Node node,
                                               WorkflowMethodRecommendationEngine.Analysis analysis,
                                               List<WorkflowMethodRecommendationEngine.Recommendation> recommendations) {
        if (activity == null || node == null) return;
        if (recommendations == null || recommendations.isEmpty()) {
            WorkflowModeDialog.show(activity, node, analysis == null ? "execution" : analysis.dominantMode);
            return;
        }

        String[] items = new String[recommendations.size()];
        for (int i = 0; i < recommendations.size(); i++) {
            WorkflowMethodRecommendationEngine.Recommendation item = recommendations.get(i);
            items[i] = String.format(Locale.getDefault(), "%d. %s\n%s", i + 1, item.label, item.reason);
        }

        new AlertDialog.Builder(activity)
                .setTitle("你可以直接点这一步")
                .setItems(items, (dialog, which) -> WorkflowMethodRecommendationEngine.execute(activity, node, recommendations.get(which)))
                .setPositiveButton("AI 教练", (dialog, which) -> activity.openAiScienceCoach(analysis.dominantMode, node))
                .setNegativeButton("关闭", null)
                .show();
    }

    private static String resolveNeutralLabel(String dominantMode) {
        if ("decision".equalsIgnoreCase(dominantMode)) return "AI 帮我想";
        if ("learning".equalsIgnoreCase(dominantMode)) return "安排学习链";
        return "进入专注";
    }

    private static void openNeutralAction(MainActivity activity, Node node, WorkflowMethodRecommendationEngine.Analysis analysis) {
        if (activity == null || node == null) return;
        String mode = analysis == null ? "execution" : analysis.dominantMode;
        if ("decision".equalsIgnoreCase(mode)) {
            activity.openAiScienceCoach("decision", node);
            return;
        }
        if ("learning".equalsIgnoreCase(mode)) {
            activity.openAiScienceCoach("learning", node);
            return;
        }
        activity.openFocusSession(node);
    }

    private static String safeTitle(Node node) {
        if (node == null) return "(空节点)";
        String title = node.getTitle();
        return title == null || title.trim().isEmpty() ? "(无标题)" : title.trim();
    }
}
