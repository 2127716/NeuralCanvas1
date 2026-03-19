package com.agui.neuralcanvas;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

public final class WorkflowModeDialog {

    private WorkflowModeDialog() {}

    public static void show(MainActivity activity, Node node, String mode) {
        if (activity == null || node == null) return;

        final String normalized = mode == null ? "execution" : mode.trim().toLowerCase();
        final List<WorkflowMethodRecommendationEngine.Recommendation> list =
                WorkflowMethodRecommendationEngine.getModeRecommendations(
                        node,
                        activity.getMindMapView().getNodesInternal(),
                        activity.getMindMapView().getConnectionsInternal(),
                        normalized
                );

        String[] items = new String[list.size() + 3];
        int index = 0;

        if ("execution".equals(normalized)) {
            items[index++] = "打开 Focus\n直接进入专注执行";
            items[index++] = "执行回填\n记录实际耗时、完成情况和进度";
            items[index++] = "科学体检\n检查执行链缺口";
        } else if ("decision".equals(normalized)) {
            items[index++] = "进入决策实验室\n做决策矩阵与多标准比较";
            items[index++] = "决策落地\n把判断转成承诺动作与跟进";
            items[index++] = "AI 决策教练\n让 AI 按决策逻辑追问";
        } else {
            items[index++] = "打开记忆复习\n查看待复习知识节点";
            items[index++] = "AI 学习补全\n补检索问题、例子、反例和迁移任务";
            items[index++] = "科学体检\n检查学习链缺口";
        }

        for (WorkflowMethodRecommendationEngine.Recommendation item : list) {
            items[index++] = item.label + "\n" + item.reason;
        }

        new AlertDialog.Builder(activity)
                .setTitle(WorkflowMethodRecommendationEngine.resolveModeLabel(normalized))
                .setItems(items, (dialog, which) -> handleClick(activity, node, normalized, list, which))
                .setPositiveButton("方法推荐", (dialog, which) -> WorkflowRecommendationDialog.show(activity, node))
                .setNegativeButton("关闭", null)
                .show();
    }

    private static void handleClick(MainActivity activity,
                                    Node node,
                                    String mode,
                                    List<WorkflowMethodRecommendationEngine.Recommendation> list,
                                    int which) {
        if ("execution".equals(mode)) {
            if (which == 0) {
                activity.openFocusSession(node);
                return;
            } else if (which == 1) {
                activity.getMindMapView().selectOnlyNode(node.getId());
                activity.openExecutionLog();
                return;
            } else if (which == 2) {
                WorkflowHealthDialog.show(activity, node);
                return;
            }
            WorkflowMethodRecommendationEngine.execute(activity, node, list.get(which - 3));
            return;
        }

        if ("decision".equals(mode)) {
            if (which == 0) {
                activity.openDecisionLab(node);
                return;
            } else if (which == 1) {
                activity.getMindMapView().selectOnlyNode(node.getId());
                activity.openDecisionFollowThrough();
                return;
            } else if (which == 2) {
                activity.openAiScienceCoach("decision", node);
                return;
            }
            WorkflowMethodRecommendationEngine.execute(activity, node, list.get(which - 3));
            return;
        }

        if (which == 0) {
            activity.openMemoryReview();
            return;
        } else if (which == 1) {
            activity.getMindMapView().selectOnlyNode(node.getId());
            activity.runAiLearningPatch();
            return;
        } else if (which == 2) {
            WorkflowHealthDialog.show(activity, node);
            return;
        }
        WorkflowMethodRecommendationEngine.execute(activity, node, list.get(which - 3));
    }
}
