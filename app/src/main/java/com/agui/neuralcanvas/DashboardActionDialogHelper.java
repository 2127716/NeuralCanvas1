package com.agui.neuralcanvas;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

public class DashboardActionDialogHelper {

    public static void showInboxClarifyDialog(FragmentActivity activity, Node node, Runnable onRefresh) {
        if (activity == null || node == null) return;

        String[] items = {
                "转成 TASK（下一步任务）",
                "转成 GOAL（目标）",
                "转成 NOTE（笔记）",
                "转成 PROJECT（项目）",
                "转成 DECISION（决策）"
        };

        new AlertDialog.Builder(activity)
                .setTitle("Inbox 快速澄清")
                .setItems(items, (dialog, which) -> {
                    Node.NodeType targetType;
                    switch (which) {
                        case 0: targetType = Node.NodeType.TASK; break;
                        case 1: targetType = Node.NodeType.GOAL; break;
                        case 2: targetType = Node.NodeType.NOTE; break;
                        case 3: targetType = Node.NodeType.PROJECT; break;
                        case 4: targetType = Node.NodeType.DECISION; break;
                        default: return;
                    }

                    QuickActionEngine.convertInboxNode(node, targetType, (MainActivity) activity);

                    if (targetType == Node.NodeType.PROJECT) {
                        askGenerateProjectStarter(activity, node, onRefresh);
                    } else if (targetType == Node.NodeType.DECISION) {
                        askGenerateDecisionStarter(activity, node, onRefresh);
                    }

                    if (onRefresh != null) onRefresh.run();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    public static void askGenerateProjectStarter(FragmentActivity activity, Node node, Runnable onRefresh) {
        new AlertDialog.Builder(activity)
                .setTitle("项目创建向导")
                .setMessage("已转成 PROJECT。要不要顺手生成 Goal、KR、First Action、Weekly Review？")
                .setNegativeButton("先不用", null)
                .setPositiveButton("立即生成", (d, w) -> {
                    QuickActionEngine.generateProjectStarterNodes(node, (MainActivity) activity);
                    if (onRefresh != null) onRefresh.run();
                })
                .show();
    }

    public static void askGenerateDecisionStarter(FragmentActivity activity, Node node, Runnable onRefresh) {
        new AlertDialog.Builder(activity)
                .setTitle("决策创建向导")
                .setMessage("要不要立刻生成 Option A/B/C、Criterion、Risk、Evidence、Next Action？")
                .setNegativeButton("先不用", null)
                .setPositiveButton("立即生成", (d, w) -> {
                    QuickActionEngine.generateDecisionStarterNodes(node, (MainActivity) activity);
                    if (onRefresh != null) onRefresh.run();
                })
                .show();
    }

    public static void askGenerateLearningStarter(FragmentActivity activity, Node node, Runnable onRefresh) {
        new AlertDialog.Builder(activity)
                .setTitle("学习创建向导")
                .setMessage("要不要立刻生成 Retrieval Practice、Concept Deepening、Transfer Practice？")
                .setNegativeButton("先不用", null)
                .setPositiveButton("立即生成", (d, w) -> {
                    QuickActionEngine.generateLearningStarterNodes(node, (MainActivity) activity);
                    if (onRefresh != null) onRefresh.run();
                })
                .show();
    }

    public static void showKrQuickUpdateDialog(FragmentActivity activity, Node node, Runnable onRefresh) {
        new AlertDialog.Builder(activity)
                .setTitle("快速更新 KR")
                .setMessage("当前值：" + node.getKrCurrent() + " / " + node.getKrTarget())
                .setNegativeButton("取消", null)
                .setNeutralButton("+1", (d, w) -> {
                    QuickActionEngine.addKrValue(node, 1f, (MainActivity) activity);
                    if (onRefresh != null) onRefresh.run();
                })
                .setPositiveButton("+5", (d, w) -> {
                    QuickActionEngine.addKrValue(node, 5f, (MainActivity) activity);
                    if (onRefresh != null) onRefresh.run();
                })
                .show();
    }

    public static void showMarkDoneDialog(FragmentActivity activity, Node node, Runnable onRefresh) {
        new AlertDialog.Builder(activity)
                .setTitle("快速操作")
                .setMessage("把这个节点标记为已完成？")
                .setNegativeButton("取消", null)
                .setPositiveButton("标记 DONE", (d, w) -> {
                    QuickActionEngine.markDone(node, (MainActivity) activity);
                    if (onRefresh != null) onRefresh.run();
                })
                .show();
    }
}
