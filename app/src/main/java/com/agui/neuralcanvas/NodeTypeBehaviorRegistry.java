package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.List;

/**
 * 第一阶段收尾：把“不同节点类型支持哪些动作”集中管理，
 * 避免继续把行为散落在 MainActivity / Dashboard / QuickActionEngine 各处。
 */
public final class NodeTypeBehaviorRegistry {

    private NodeTypeBehaviorRegistry() {}

    public static List<String> getActions(Node node) {
        List<String> actions = new ArrayList<>();
        if (node == null) return actions;

        actions.add("编辑节点");

        switch (node.getType()) {
            case INBOX:
                actions.add("澄清到工作流");
                break;

            case PROJECT:
                actions.add("生成项目起步结构");
                actions.add("标记完成");
                actions.add("归档到 Archives");
                break;

            case DECISION:
                actions.add("生成决策起步结构");
                actions.add("归档到 Archives");
                break;

            case CONCEPT:
            case NOTE:
            case QUESTION:
            case SOURCE:
            case INSIGHT:
            case EVIDENCE:
                actions.add("生成学习起步结构");
                actions.add("移动到 Resources");
                if (!WorkflowEngine.safe(node.getAreaId()).isEmpty()) {
                    actions.add("清除 Area 归属");
                }
                break;

            case KEY_RESULT:
                actions.add("KR +0.1");
                actions.add("KR +1");
                break;

            case TASK:
            case ACTION:
            case ROUTINE:
            case TRIGGER:
            case OBSTACLE:
                if (node.getStatus() != Node.NodeStatus.DONE) {
                    actions.add("标记完成");
                }
                actions.add("移动到 Someday");
                actions.add("移动到 Waiting");
                break;

            case REVIEW:
                if (node.getStatus() != Node.NodeStatus.DONE) {
                    actions.add("标记完成");
                }
                actions.add("安排到今天复盘");
                break;

            case RISK:
                if (node.getStatus() != Node.NodeStatus.DONE) {
                    actions.add("标记完成");
                }
                actions.add("提升优先级");
                break;

            default:
                if (node.isExecutionNode() && node.getStatus() != Node.NodeStatus.DONE) {
                    actions.add("标记完成");
                }
                break;
        }

        if (canMoveToArea(node)) {
            actions.add("移动到 Areas");
        }
        if (canMoveToResources(node)) {
            actions.add("移动到 Resources");
        }
        if (canArchive(node)) {
            actions.add("归档到 Archives");
        }

        actions.add("删除节点");
        return dedupe(actions);
    }

    public static boolean canMoveToArea(Node node) {
        if (node == null) return false;
        return node.getType() == Node.NodeType.GOAL
                || node.getType() == Node.NodeType.PROJECT
                || node.getType() == Node.NodeType.ROUTINE
                || node.getType() == Node.NodeType.DECISION;
    }

    public static boolean canMoveToResources(Node node) {
        if (node == null) return false;
        return node.isLearningNode()
                || node.getType() == Node.NodeType.RESOURCE
                || node.getType() == Node.NodeType.SOURCE;
    }

    public static boolean canArchive(Node node) {
        return node != null && node.getStatus() == Node.NodeStatus.DONE;
    }

    private static List<String> dedupe(List<String> source) {
        List<String> out = new ArrayList<>();
        for (String item : source) {
            if (!out.contains(item)) out.add(item);
        }
        return out;
    }
}
