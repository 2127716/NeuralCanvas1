package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WorkflowAuditEngine {

    public enum ActionType {
        OPEN_MODE_EXECUTION,
        OPEN_MODE_DECISION,
        OPEN_MODE_LEARNING,
        APPLY_WOOP,
        APPLY_IF_THEN,
        APPLY_WRAP,
        APPLY_PREMORTEM,
        APPLY_BAYES,
        APPLY_RETRIEVAL,
        APPLY_WEEKLY_REVIEW,
        OPEN_DECISION_LAB,
        OPEN_MEMORY_REVIEW,
        OPEN_EXECUTION_LOG,
        RUN_ENHANCEMENT,
        RUN_AUTOPILOT,
        QUICK_FIX,
        OPEN_AI_RECOMMEND,
        OPEN_AI_REDTREAM,
        OPEN_AI_EXECUTION,
        OPEN_AI_LEARNING,
        OPEN_AI_DECISION
    }

    public static final class SuggestedAction {
        public final String label;
        public final String reason;
        public final ActionType actionType;

        public SuggestedAction(String label, String reason, ActionType actionType) {
            this.label = label;
            this.reason = reason;
            this.actionType = actionType;
        }
    }

    public static final class AuditReport {
        public String title;
        public String dominantLane;
        public String summary;
        public final List<String> strengths = new ArrayList<>();
        public final List<String> gaps = new ArrayList<>();
        public final List<String> checks = new ArrayList<>();
        public final List<SuggestedAction> actions = new ArrayList<>();
    }

    private WorkflowAuditEngine() {}

    public static AuditReport analyze(Node node, Map<String, Node> nodes, Map<String, Connection> connections) {
        AuditReport report = new AuditReport();
        report.title = safeTitle(node);
        report.dominantLane = detectLane(node);

        analyzeExecution(node, nodes, connections, report);
        analyzeDecision(node, nodes, connections, report);
        analyzeLearning(node, nodes, connections, report);
        analyzeReview(node, nodes, connections, report);
        analyzeMetrics(node, report);

        if (report.strengths.isEmpty()) {
            report.strengths.add("这个节点已经具备基础字段，可以继续补成完整方法图。");
        }
        if (report.gaps.isEmpty()) {
            report.gaps.add("当前没有明显硬缺口，更适合进入一键推进或精修阶段。");
        }

        report.summary = buildSummary(report);
        fillActions(node, report);
        return report;
    }

    private static void analyzeExecution(Node node, Map<String, Node> nodes, Map<String, Connection> connections, AuditReport report) {
        boolean actionable = node.getType() == Node.NodeType.TASK
                || node.getType() == Node.NodeType.ACTION
                || node.getType() == Node.NodeType.PROJECT
                || node.getType() == Node.NodeType.GOAL
                || node.getType() == Node.NodeType.ROUTINE
                || node.getType() == Node.NodeType.TRIGGER;

        boolean hasActionNeighbor = hasRelatedType(node, nodes, Node.NodeType.ACTION, Node.NodeType.TASK);
        boolean hasTrigger = !WorkflowEngine.isBlank(node.getTriggerCondition()) || hasRelatedType(node, nodes, Node.NodeType.TRIGGER);
        boolean hasObstacle = hasRelatedType(node, nodes, Node.NodeType.OBSTACLE);
        boolean hasReview = !WorkflowEngine.isBlank(node.getReviewAt()) || hasRelatedType(node, nodes, Node.NodeType.REVIEW);

        report.checks.add("执行链：下一步=" + yesNo(hasActionNeighbor) + "，触发器=" + yesNo(hasTrigger) + "，障碍=" + yesNo(hasObstacle) + "，复盘=" + yesNo(hasReview));

        if (actionable && hasTrigger) report.strengths.add("执行侧已经有触发条件，可直接承载 If-Then。");
        if (actionable && hasActionNeighbor) report.strengths.add("执行侧已经有行动节点，适合继续做 WOOP / 专注 / 执行日志。");

        if (actionable && !hasActionNeighbor) report.gaps.add("缺少下一步行动节点，项目/目标还没有真正落到执行层。");
        if (actionable && !hasTrigger) report.gaps.add("缺少触发条件或触发器节点，不利于把计划变成自动执行。");
        if (actionable && !hasObstacle) report.gaps.add("缺少障碍节点，建议补 Premortem 或执行阻碍分析。");
        if (actionable && !hasReview) report.gaps.add("缺少复盘时间或复盘节点，执行闭环还没形成。");
    }

    private static void analyzeDecision(Node node, Map<String, Node> nodes, Map<String, Connection> connections, AuditReport report) {
        boolean decisionLike = node.getType() == Node.NodeType.DECISION
                || node.getType() == Node.NodeType.PROJECT
                || node.getType() == Node.NodeType.GOAL
                || node.getType() == Node.NodeType.QUESTION
                || node.getType() == Node.NodeType.ASSUMPTION;

        boolean hasOption = hasRelatedType(node, nodes, Node.NodeType.OPTION);
        boolean hasCriterion = hasRelatedType(node, nodes, Node.NodeType.CRITERION);
        boolean hasEvidence = hasRelatedType(node, nodes, Node.NodeType.EVIDENCE, Node.NodeType.SOURCE);
        boolean hasRisk = hasRelatedType(node, nodes, Node.NodeType.RISK);
        boolean hasAssumption = hasRelatedType(node, nodes, Node.NodeType.ASSUMPTION);

        report.checks.add("决策链：方案=" + yesNo(hasOption) + "，准则=" + yesNo(hasCriterion) + "，证据=" + yesNo(hasEvidence) + "，风险=" + yesNo(hasRisk) + "，假设=" + yesNo(hasAssumption));

        if (decisionLike && hasOption && hasCriterion) report.strengths.add("决策骨架已经存在，适合进入 WRAP / 决策矩阵。");
        if (decisionLike && hasEvidence) report.strengths.add("已经有证据节点，可继续做证据审查或 Bayes 更新。");

        if (decisionLike && !hasOption) report.gaps.add("缺少备选方案节点，当前更像单一路径而不是决策。");
        if (decisionLike && !hasCriterion) report.gaps.add("缺少评价准则节点，难以稳定比较方案优劣。");
        if (decisionLike && !hasEvidence) report.gaps.add("缺少证据或来源节点，判断更偏主观。");
        if (decisionLike && !hasRisk) report.gaps.add("缺少风险节点，建议补 Premortem。");
        if (decisionLike && !hasAssumption) report.gaps.add("缺少假设节点，不利于做 Bayes 式更新。");
    }

    private static void analyzeLearning(Node node, Map<String, Node> nodes, Map<String, Connection> connections, AuditReport report) {
        boolean learningLike = node.isLearningNode()
                || node.getType() == Node.NodeType.RESOURCE
                || node.getType() == Node.NodeType.SOURCE
                || node.getType() == Node.NodeType.EXPERIMENT;

        boolean hasQuestion = hasRelatedType(node, nodes, Node.NodeType.QUESTION);
        boolean hasSource = hasRelatedType(node, nodes, Node.NodeType.SOURCE, Node.NodeType.RESOURCE);
        boolean hasInsight = hasRelatedType(node, nodes, Node.NodeType.INSIGHT, Node.NodeType.NOTE, Node.NodeType.CONCEPT);
        boolean hasTransfer = hasRelatedType(node, nodes, Node.NodeType.EXPERIMENT, Node.NodeType.ACTION);

        report.checks.add("学习链：问题=" + yesNo(hasQuestion) + "，来源=" + yesNo(hasSource) + "，理解/洞察=" + yesNo(hasInsight) + "，迁移/应用=" + yesNo(hasTransfer));

        if (learningLike && hasQuestion) report.strengths.add("已经有问题节点，适合继续做检索练习。");
        if (learningLike && hasSource) report.strengths.add("已经有来源节点，知识链更可追溯。");

        if (learningLike && !hasQuestion) report.gaps.add("缺少自测问题节点，学习还没进入主动检索。");
        if (learningLike && !hasSource) report.gaps.add("缺少来源节点，知识不够可追溯。");
        if (learningLike && !hasInsight) report.gaps.add("缺少自己的解释/洞察节点，概念深化不足。");
        if (learningLike && !hasTransfer) report.gaps.add("缺少迁移/应用节点，容易停留在‘看懂了’。");
    }

    private static void analyzeReview(Node node, Map<String, Node> nodes, Map<String, Connection> connections, AuditReport report) {
        boolean hasReviewAt = !WorkflowEngine.isBlank(node.getReviewAt());
        boolean reviewNodeAround = hasRelatedType(node, nodes, Node.NodeType.REVIEW);
        boolean hasEffortData = node.getEffortEstimate() > 0f || node.getActualEffort() > 0f;

        report.checks.add("复盘链：reviewAt=" + yesNo(hasReviewAt) + "，复盘节点=" + yesNo(reviewNodeAround) + "，工时记录=" + yesNo(hasEffortData));

        if (hasReviewAt || reviewNodeAround) report.strengths.add("已经有复盘入口，可继续做 Daily / Weekly / AAR。");
        if (!hasReviewAt && !reviewNodeAround) report.gaps.add("缺少复盘时间或复盘节点，建议补 Daily / Weekly Review。");
        if (!hasEffortData && (node.getType() == Node.NodeType.TASK || node.getType() == Node.NodeType.ACTION || node.getType() == Node.NodeType.PROJECT)) {
            report.gaps.add("缺少预计/实际耗时数据，执行复盘颗粒度不够。");
        }
    }

    private static void analyzeMetrics(Node node, AuditReport report) {
        report.checks.add("关键字段：priority=" + node.getPriority()
                + "，confidence=" + format01(node.getConfidence())
                + "，evidence=" + format01(node.getEvidenceStrength())
                + "，KR=" + trimFloat(node.getKrCurrent()) + "/" + trimFloat(node.getKrTarget()));

        if (node.getConfidence() > 0.72f && node.getEvidenceStrength() < 0.5f) {
            report.gaps.add("置信度偏高但证据偏弱，建议补证据审查或 Bayes 更新。");
        }
        if (node.getType() == Node.NodeType.KEY_RESULT && node.getKrTarget() <= 0f) {
            report.gaps.add("KR 节点还没有目标值，量化闭环不完整。");
        }
        if (node.getType() == Node.NodeType.KEY_RESULT && node.getKrTarget() > 0f) {
            report.strengths.add("KR 已量化，可以继续挂行动节点和复盘节点。");
        }
    }

    private static void fillActions(Node node, AuditReport report) {
        if (containsGap(report, "触发")) {
            report.actions.add(new SuggestedAction("补 If-Then", "先把行动绑定到明确触发器。", ActionType.APPLY_IF_THEN));
            report.actions.add(new SuggestedAction("进执行模式", "优先把计划推到可执行状态。", ActionType.OPEN_MODE_EXECUTION));
        }

        if (containsGap(report, "下一步行动")) {
            report.actions.add(new SuggestedAction("补 WOOP", "把目标-障碍-计划转成可执行子图。", ActionType.APPLY_WOOP));
        }

        if (containsGap(report, "风险")) {
            report.actions.add(new SuggestedAction("补 Premortem", "先假设失败，再反推风险和缓解动作。", ActionType.APPLY_PREMORTEM));
        }

        if (containsGap(report, "准则") || containsGap(report, "方案")) {
            report.actions.add(new SuggestedAction("进决策实验室", "把方案、准则、风险拉到同一张图里。", ActionType.OPEN_DECISION_LAB));
            report.actions.add(new SuggestedAction("补 WRAP", "给决策加多方案、证据和护栏。", ActionType.APPLY_WRAP));
            report.actions.add(new SuggestedAction("进决策模式", "优先把节点切到决策工作流。", ActionType.OPEN_MODE_DECISION));
        }

        if (containsGap(report, "证据") || containsGap(report, "假设")) {
            report.actions.add(new SuggestedAction("补 Bayes 更新", "把假设、证据、更新结论补完整。", ActionType.APPLY_BAYES));
        }

        if (containsGap(report, "自测问题")) {
            report.actions.add(new SuggestedAction("补检索练习", "把‘我看懂了’改成主动回忆。", ActionType.APPLY_RETRIEVAL));
            report.actions.add(new SuggestedAction("进学习模式", "优先补问题-来源-迁移链。", ActionType.OPEN_MODE_LEARNING));
        }

        if (containsGap(report, "复盘")) {
            report.actions.add(new SuggestedAction("补周复盘", "让行动、阻碍、修正形成闭环。", ActionType.APPLY_WEEKLY_REVIEW));
            report.actions.add(new SuggestedAction("打开执行日志", "先把做了什么、卡在哪里记下来。", ActionType.OPEN_EXECUTION_LOG));
        }

        if (report.actions.isEmpty()) {
            report.actions.add(new SuggestedAction("一键智能补强", "当前结构不差，适合自动补全缺口。", ActionType.RUN_ENHANCEMENT));
            report.actions.add(new SuggestedAction("全量推进", "直接让系统围绕当前节点继续铺开。", ActionType.RUN_AUTOPILOT));
            report.actions.add(new SuggestedAction("AI建议", "让 AI 按当前节点给出下一步方法建议。", ActionType.OPEN_AI_RECOMMEND));
        } else {
            report.actions.add(new SuggestedAction("一键修复字段", "顺手补全通用字段和工作流默认值。", ActionType.QUICK_FIX));
        }
    }

    public static void execute(MainActivity activity, Node node, SuggestedAction action) {
        if (activity == null || node == null || action == null) return;
        activity.getMindMapView().selectOnlyNode(node.getId());

        switch (action.actionType) {
            case OPEN_MODE_EXECUTION:
                WorkflowModeDialog.show(activity, node, "execution");
                return;
            case OPEN_MODE_DECISION:
                WorkflowModeDialog.show(activity, node, "decision");
                return;
            case OPEN_MODE_LEARNING:
                WorkflowModeDialog.show(activity, node, "learning");
                return;
            case APPLY_WOOP:
                activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.WOOP);
                return;
            case APPLY_IF_THEN:
                activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.IF_THEN);
                return;
            case APPLY_WRAP:
                activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.WRAP);
                return;
            case APPLY_PREMORTEM:
                activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.PREMORTEM);
                return;
            case APPLY_BAYES:
                activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.BAYES_UPDATE);
                return;
            case APPLY_RETRIEVAL:
                activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.RETRIEVAL_PRACTICE);
                return;
            case APPLY_WEEKLY_REVIEW:
                activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.WEEKLY_REVIEW);
                return;
            case OPEN_DECISION_LAB:
                activity.openDecisionLab(node);
                return;
            case OPEN_MEMORY_REVIEW:
                activity.openMemoryReview();
                return;
            case OPEN_EXECUTION_LOG:
                activity.openExecutionLog();
                return;
            case RUN_ENHANCEMENT:
                activity.runScientificEnhancement();
                return;
            case RUN_AUTOPILOT:
                activity.runScientificAutopilot();
                return;
            case QUICK_FIX:
                WorkflowQuickFixEngine.FixResult fixResult = WorkflowQuickFixEngine.quickFixNode(activity, node);
                android.widget.Toast.makeText(activity, fixResult.buildSummary(), android.widget.Toast.LENGTH_LONG).show();
                return;
            case OPEN_AI_RECOMMEND:
                activity.openAiScienceCoach("recommend", node);
                return;
            case OPEN_AI_REDTREAM:
                activity.openAiScienceCoach("redteam", node);
                return;
            case OPEN_AI_EXECUTION:
                activity.openAiScienceCoach("execution", node);
                return;
            case OPEN_AI_LEARNING:
                activity.openAiScienceCoach("learning", node);
                return;
            case OPEN_AI_DECISION:
                activity.openAiScienceCoach("decision", node);
        }
    }

    private static boolean containsGap(AuditReport report, String keyword) {
        if (report == null || keyword == null) return false;
        for (String gap : report.gaps) {
            if (gap != null && gap.contains(keyword)) return true;
        }
        return false;
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

    private static String buildSummary(AuditReport report) {
        return "主模式：" + report.dominantLane
                + "\n强项：" + report.strengths.size()
                + "\n缺口：" + report.gaps.size()
                + "\n检查项：" + report.checks.size();
    }

    private static String detectLane(Node node) {
        if (node == null) return "综合";
        if (node.isDecisionNode()) return "决策";
        if (node.isLearningNode()) return "学习";
        if (node.isExecutionNode()) return "执行";
        if (node.getType() == Node.NodeType.PROJECT || node.getType() == Node.NodeType.GOAL) return "执行 / 决策";
        return "综合";
    }

    private static String yesNo(boolean value) {
        return value ? "有" : "缺";
    }

    private static String safeTitle(Node node) {
        if (node == null) return "(空节点)";
        String title = WorkflowEngine.safe(node.getTitle());
        return title.isEmpty() ? "(无标题)" : title;
    }

    private static String format01(float value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static String trimFloat(float value) {
        if (Math.abs(value - (int) value) < 0.0001f) return String.valueOf((int) value);
        return String.format(Locale.US, "%.2f", value);
    }
}
