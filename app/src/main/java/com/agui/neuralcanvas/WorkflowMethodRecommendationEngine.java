package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class WorkflowMethodRecommendationEngine {

    public static final class Recommendation {
        public final String label;
        public final String reason;
        public final String actionId;
        public final String mode;
        public final int priority;

        public Recommendation(String label, String reason, String actionId, String mode, int priority) {
            this.label = label;
            this.reason = reason;
            this.actionId = actionId;
            this.mode = mode;
            this.priority = priority;
        }
    }

    public static final class Analysis {
        public final Node node;
        public final List<Recommendation> recommendations = new ArrayList<>();
        public final List<String> strengths = new ArrayList<>();
        public final List<String> gaps = new ArrayList<>();
        public String dominantMode = "execution";

        Analysis(Node node) {
            this.node = node;
        }
    }

    private WorkflowMethodRecommendationEngine() {}

    public static Analysis analyze(Node node,
                                   Map<String, Node> nodes,
                                   Map<String, Connection> connections) {
        Analysis analysis = new Analysis(node);
        if (node == null) return analysis;

        boolean executionLike = node.isExecutionNode() || node.getType() == Node.NodeType.PROJECT || node.getType() == Node.NodeType.GOAL;
        boolean decisionLike = node.isDecisionNode() || node.getType() == Node.NodeType.PROJECT || node.getType() == Node.NodeType.QUESTION;
        boolean learningLike = node.isLearningNode() || node.getType() == Node.NodeType.RESOURCE;

        int execScore = executionLike ? 2 : 0;
        int decisionScore = decisionLike ? 2 : 0;
        int learningScore = learningLike ? 2 : 0;

        boolean hasAction = hasNearbyType(node, nodes, Node.NodeType.ACTION, Node.NodeType.TASK);
        boolean hasTrigger = !WorkflowEngine.isBlank(node.getTriggerCondition())
                || hasNearbyType(node, nodes, Node.NodeType.TRIGGER);
        boolean hasObstacle = hasNearbyType(node, nodes, Node.NodeType.OBSTACLE);
        boolean hasReview = hasNearbyType(node, nodes, Node.NodeType.REVIEW);
        boolean hasRisk = hasNearbyType(node, nodes, Node.NodeType.RISK);
        boolean hasEvidence = hasNearbyType(node, nodes, Node.NodeType.EVIDENCE);
        boolean hasOption = hasNearbyType(node, nodes, Node.NodeType.OPTION);
        boolean hasQuestion = hasNearbyType(node, nodes, Node.NodeType.QUESTION);
        boolean hasExperiment = hasNearbyType(node, nodes, Node.NodeType.EXPERIMENT);
        boolean hasResource = hasNearbyType(node, nodes, Node.NodeType.RESOURCE, Node.NodeType.SOURCE);

        if (hasAction) analysis.strengths.add("已经有可执行下一步");
        if (hasTrigger) analysis.strengths.add("已经有触发条件/触发节点");
        if (hasReview) analysis.strengths.add("已经有反馈或复盘结构");
        if (hasEvidence) analysis.strengths.add("已经有证据支撑/反驳结构");
        if (hasExperiment) analysis.strengths.add("已经有验证或迁移任务");

        if (executionLike) {
            if (!hasTrigger) {
                analysis.gaps.add("执行链缺少明确触发条件");
                analysis.recommendations.add(new Recommendation(
                        "补 If-Then",
                        "把“想做”变成“遇到什么情境就立刻做什么”。",
                        "template:IF_THEN",
                        "execution",
                        100));
                execScore += 3;
            }
            if (!hasObstacle) {
                analysis.gaps.add("执行链缺少障碍分析");
                analysis.recommendations.add(new Recommendation(
                        "补 WOOP",
                        "把愿望、障碍和计划连起来，减少卡住后停摆。",
                        "template:WOOP",
                        "execution",
                        94));
                analysis.recommendations.add(new Recommendation(
                        "AI 执行补全",
                        "自动补最小下一步、触发条件和完成判据。",
                        "action:AI_EXECUTION_PATCH",
                        "execution",
                        92));
                execScore += 3;
            }
            if (!hasAction) {
                analysis.gaps.add("当前节点还没有清晰的下一步行动");
                analysis.recommendations.add(new Recommendation(
                        "执行补强",
                        "先生成最小下一步，再进入专注执行。",
                        "action:SCIENTIFIC_ENHANCEMENT",
                        "execution",
                        98));
                execScore += 4;
            }
            if (!hasReview) {
                analysis.gaps.add("执行后缺少反馈闭环");
                analysis.recommendations.add(new Recommendation(
                        "补每日复盘",
                        "把今天推进、卡点、经验和明日最小下一步接起来。",
                        "template:DAILY_REVIEW",
                        "execution",
                        84));
                execScore += 2;
            }
            if (node.getType() == Node.NodeType.PROJECT && !hasReview) {
                analysis.recommendations.add(new Recommendation(
                        "补每周复盘",
                        "项目类节点更适合建立周期性调整回路。",
                        "template:WEEKLY_REVIEW",
                        "execution",
                        88));
            }
            if (hasAction && node.getStatus() != Node.NodeStatus.DONE) {
                analysis.recommendations.add(new Recommendation(
                        "开始 Focus",
                        "已有行动结构，直接进入专注 session 最划算。",
                        "action:FOCUS",
                        "execution",
                        76));
            }
        }

        if (decisionLike) {
            if (!hasOption) {
                analysis.gaps.add("决策结构缺少备选方案");
                analysis.recommendations.add(new Recommendation(
                        "补决策树",
                        "先把候选方案和准则摊开，不要直接拍板。",
                        "template:DECISION_TREE",
                        "decision",
                        99));
                analysis.recommendations.add(new Recommendation(
                        "进入决策实验室",
                        "直接做多标准比较。",
                        "action:DECISION_LAB",
                        "decision",
                        94));
                decisionScore += 4;
            }
            if (!hasEvidence || node.getEvidenceStrength() < 0.45f) {
                analysis.gaps.add("当前决策证据不足或证据强度偏低");
                analysis.recommendations.add(new Recommendation(
                        "补证据审查",
                        "把支持证据、反对证据和待验证假设拉出来。",
                        "template:EVIDENCE_REVIEW",
                        "decision",
                        96));
                decisionScore += 3;
            }
            if (!hasRisk) {
                analysis.gaps.add("当前决策还没有失败预演或风险前置");
                analysis.recommendations.add(new Recommendation(
                        "补 Premortem",
                        "先假设失败，再倒推原因和预警信号。",
                        "template:PREMORTEM",
                        "decision",
                        90));
                decisionScore += 3;
            }
            if (!hasOption || !hasRisk || !hasEvidence) {
                analysis.recommendations.add(new Recommendation(
                        "补 WRAP",
                        "用扩展选项、现实检验、心理距离和纠错线做决策护栏。",
                        "template:WRAP",
                        "decision",
                        88));
            }
            if (node.getConfidence() > 0.72f && (node.getEvidenceStrength() < 0.50f || !hasEvidence)) {
                analysis.gaps.add("置信度偏高，但证据不足");
                analysis.recommendations.add(new Recommendation(
                        "补 Bayes 更新",
                        "把先验、基率、支持证据和反向证据写清楚。",
                        "template:BAYES_UPDATE",
                        "decision",
                        97));
            }
            if (node.getType() == Node.NodeType.PROJECT || node.getType() == Node.NodeType.GOAL) {
                analysis.recommendations.add(new Recommendation(
                        "补参考类预测",
                        "用外部视角修正时间/成功率估计。",
                        "template:REFERENCE_CLASS_FORECAST",
                        "decision",
                        82));
            }
            if (hasOption || hasEvidence || hasRisk) {
                analysis.recommendations.add(new Recommendation(
                        "决策落地",
                        "把当前判断转成承诺动作、止损线和跟进节点。",
                        "action:DECISION_FOLLOW_THROUGH",
                        "decision",
                        78));
            }
        }

        if (learningLike) {
            if (!hasQuestion) {
                analysis.gaps.add("当前知识结构缺少主动回忆问题");
                analysis.recommendations.add(new Recommendation(
                        "补检索练习",
                        "不用看原文回忆定义、机制和关键点。",
                        "template:RETRIEVAL_PRACTICE",
                        "learning",
                        100));
                learningScore += 4;
            }
            if (!hasResource) {
                analysis.gaps.add("当前知识结构缺少例子或来源");
                analysis.recommendations.add(new Recommendation(
                        "补概念深化",
                        "把定义、例子、反例和易错点补齐。",
                        "template:CONCEPT_DEEPENING",
                        "learning",
                        92));
                learningScore += 3;
            }
            if (!hasExperiment) {
                analysis.gaps.add("当前知识结构缺少迁移或验证任务");
                analysis.recommendations.add(new Recommendation(
                        "补迁移练习",
                        "换个场景应用它，检验自己是不是会用了。",
                        "template:TRANSFER_PRACTICE",
                        "learning",
                        88));
                learningScore += 3;
            }
            if (!hasReview && WorkflowEngine.isBlank(node.getReviewAt())) {
                analysis.gaps.add("当前知识结构缺少复习安排");
                analysis.recommendations.add(new Recommendation(
                        "打开记忆复习",
                        "为知识节点建立复习与检索节奏。",
                        "action:MEMORY_REVIEW",
                        "learning",
                        80));
            }
            analysis.recommendations.add(new Recommendation(
                    "AI 学习补全",
                    "自动补检索问题、例子、反例和迁移任务。",
                    "action:AI_LEARNING_PATCH",
                    "learning",
                    84));
        }

        if (execScore >= decisionScore && execScore >= learningScore) {
            analysis.dominantMode = "execution";
        } else if (decisionScore >= learningScore) {
            analysis.dominantMode = "decision";
        } else {
            analysis.dominantMode = "learning";
        }

        analysis.recommendations.add(new Recommendation(
                "工作流体检",
                "先看系统缺口，再决定先补哪一块。",
                "action:WORKFLOW_HEALTH",
                analysis.dominantMode,
                70));

        sortAndDedupe(analysis.recommendations);
        return analysis;
    }

    public static List<Recommendation> getModeRecommendations(Node node,
                                                              Map<String, Node> nodes,
                                                              Map<String, Connection> connections,
                                                              String mode) {
        Analysis analysis = analyze(node, nodes, connections);
        List<Recommendation> result = new ArrayList<>();
        for (Recommendation item : analysis.recommendations) {
            if (mode == null || mode.trim().isEmpty() || mode.equalsIgnoreCase(item.mode)) {
                result.add(item);
            }
        }
        return result;
    }

    public static String buildReadableReport(Analysis analysis) {
        if (analysis == null || analysis.node == null) return "未选中节点。";
        StringBuilder sb = new StringBuilder();
        sb.append("节点：").append(WorkflowEngine.safe(analysis.node.getTitle()).isEmpty() ? "(无标题)" : WorkflowEngine.safe(analysis.node.getTitle()));
        sb.append("\n主模式：").append(resolveModeLabel(analysis.dominantMode));

        if (!analysis.strengths.isEmpty()) {
            sb.append("\n\n当前已有结构：");
            for (String item : analysis.strengths) {
                sb.append("\n• ").append(item);
            }
        }

        if (!analysis.gaps.isEmpty()) {
            sb.append("\n\n当前主要缺口：");
            for (String item : analysis.gaps) {
                sb.append("\n• ").append(item);
            }
        }

        if (!analysis.recommendations.isEmpty()) {
            sb.append("\n\n推荐顺序：");
            int limit = Math.min(6, analysis.recommendations.size());
            for (int i = 0; i < limit; i++) {
                Recommendation item = analysis.recommendations.get(i);
                sb.append("\n").append(i + 1).append(". ").append(item.label).append(" —— ").append(item.reason);
            }
        }

        return sb.toString();
    }

    public static void execute(MainActivity activity, Node node, Recommendation recommendation) {
        if (activity == null || node == null || recommendation == null) return;
        execute(activity, node, recommendation.actionId);
    }

    public static void execute(MainActivity activity, Node node, String actionId) {
        if (activity == null || node == null || actionId == null) return;

        if (actionId.startsWith("template:")) {
            String typeName = actionId.substring("template:".length());
            try {
                ScientificTemplateEngine.TemplateType type = ScientificTemplateEngine.TemplateType.valueOf(typeName);
                activity.applyScientificTemplateToNode(node, type);
            } catch (Exception ignored) {
            }
            return;
        }

        switch (actionId) {
            case "action:SCIENTIFIC_ENHANCEMENT":
                activity.getMindMapView().selectOnlyNode(node.getId());
                activity.runScientificEnhancement();
                return;
            case "action:AI_EXECUTION_PATCH":
                activity.getMindMapView().selectOnlyNode(node.getId());
                activity.runAiExecutionPatch();
                return;
            case "action:AI_LEARNING_PATCH":
                activity.getMindMapView().selectOnlyNode(node.getId());
                activity.runAiLearningPatch();
                return;
            case "action:FOCUS":
                activity.openFocusSession(node);
                return;
            case "action:MEMORY_REVIEW":
                activity.openMemoryReview();
                return;
            case "action:DECISION_LAB":
                activity.openDecisionLab(node);
                return;
            case "action:DECISION_FOLLOW_THROUGH":
                activity.getMindMapView().selectOnlyNode(node.getId());
                activity.openDecisionFollowThrough();
                return;
            case "action:WORKFLOW_HEALTH":
                WorkflowHealthDialog.show(activity, node);
                return;
            default:
                break;
        }
    }

    public static String resolveModeLabel(String mode) {
        if ("decision".equalsIgnoreCase(mode)) return "决策模式";
        if ("learning".equalsIgnoreCase(mode)) return "学习模式";
        return "执行模式";
    }

    private static boolean hasNearbyType(Node baseNode,
                                         Map<String, Node> existingNodes,
                                         Node.NodeType... targetTypes) {
        if (baseNode == null || existingNodes == null || targetTypes == null || targetTypes.length == 0) {
            return false;
        }

        for (Node node : existingNodes.values()) {
            if (node == null || baseNode.getId().equals(node.getId())) continue;
            boolean matched = false;
            for (Node.NodeType type : targetTypes) {
                if (node.getType() == type) {
                    matched = true;
                    break;
                }
            }
            if (!matched) continue;

            if (sameOwner(baseNode, node) || distance(baseNode, node) <= 920f) {
                return true;
            }
        }
        return false;
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

    private static void sortAndDedupe(List<Recommendation> source) {
        Collections.sort(source, new Comparator<Recommendation>() {
            @Override
            public int compare(Recommendation a, Recommendation b) {
                return Integer.compare(b.priority, a.priority);
            }
        });

        List<String> seen = new ArrayList<>();
        for (int i = source.size() - 1; i >= 0; i--) {
            Recommendation item = source.get(i);
            String key = item.label + "|" + item.actionId;
            if (seen.contains(key)) {
                source.remove(i);
            } else {
                seen.add(key);
            }
        }
    }
}
