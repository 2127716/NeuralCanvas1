package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WorkflowAuditEngine {

    public static final class SuggestedAction {
        public String label;
        public String reason;
        public String actionKey;

        public SuggestedAction(String label, String reason, String actionKey) {
            this.label = label;
            this.reason = reason;
            this.actionKey = actionKey;
        }
    }

    public static final class AuditReport {
        public String title = "";
        public String dominantLane = "执行";
        public String summary = "未发现明显结构缺口";
        public final List<String> strengths = new ArrayList<>();
        public final List<String> gaps = new ArrayList<>();
        public final List<String> checks = new ArrayList<>();
        public final List<SuggestedAction> actions = new ArrayList<>();
    }

    public static final class AuditResult {
        public final List<String> issues = new ArrayList<>();
        public boolean isHealthy() { return issues.isEmpty(); }
        public String buildSummary() {
            if (issues.isEmpty()) return "未发现明显结构缺口";
            StringBuilder sb = new StringBuilder();
            sb.append("发现 ").append(issues.size()).append(" 个结构缺口");
            for (String issue : issues) sb.append("\n- ").append(issue);
            return sb.toString();
        }
    }

    private WorkflowAuditEngine() {}

    public static AuditReport analyze(Node node, Map<String, Node> nodes, Map<String, Connection> connections) {
        AuditReport report = new AuditReport();
        if (node == null) return report;

        report.title = safeTitle(node);
        report.dominantLane = resolveLane(node);

        if (node.isExecutionNode()) {
            if (ExecutionValidator.isExecutable(node)) {
                report.strengths.add("已具备触发器/时间锚点中的关键执行条件");
                report.checks.add("执行可开始性：通过");
            } else {
                if (safe(node.getTriggerCondition()).isEmpty()) report.gaps.add("缺少触发器");
                if (safe(node.getDueAt()).isEmpty() && safe(node.getReviewAt()).isEmpty()) report.gaps.add("缺少时间锚点");
                report.actions.add(new SuggestedAction("补执行闭环", "为当前节点补触发器、下一步动作和时间锚点", "execution_patch"));
                report.checks.add("执行可开始性：未通过");
            }
        }

        if (node.isDecisionNode()) {
            boolean hasEvidence = hasNeighborOfType(node, nodes, connections, Node.NodeType.EVIDENCE)
                    || hasNeighborOfType(node, nodes, connections, Node.NodeType.SOURCE);
            boolean hasRisk = hasNeighborOfType(node, nodes, connections, Node.NodeType.RISK)
                    || hasNeighborOfType(node, nodes, connections, Node.NodeType.OBSTACLE);
            boolean hasOption = hasNeighborOfType(node, nodes, connections, Node.NodeType.OPTION);

            if (hasEvidence) report.strengths.add("已有证据基础");
            else report.gaps.add("缺少证据支持/反证");

            if (hasRisk) report.strengths.add("已有风险视角");
            else report.gaps.add("缺少风险节点");

            if (hasOption) report.strengths.add("已有方案比较基础");
            else report.gaps.add("缺少方案节点");

            if (!hasEvidence || !hasRisk || !hasOption) {
                report.actions.add(new SuggestedAction("补决策护栏", "补方案、证据、风险与反证", "decision_patch"));
            }

            report.checks.add("决策结构："
                    + (hasOption ? "有方案" : "缺方案") + " / "
                    + (hasEvidence ? "有证据" : "缺证据") + " / "
                    + (hasRisk ? "有风险" : "缺风险"));
        }

        if (node.isLearningNode()) {
            boolean hasQuestion = hasNeighborOfType(node, nodes, connections, Node.NodeType.QUESTION);
            boolean hasSource = hasNeighborOfType(node, nodes, connections, Node.NodeType.SOURCE);
            boolean hasReview = !safe(node.getReviewAt()).isEmpty();

            if (hasQuestion) report.strengths.add("已有检索问题");
            else report.gaps.add("缺少检索问题");

            if (hasSource) report.strengths.add("已有来源节点");
            else report.gaps.add("缺少来源节点");

            if (hasReview) report.strengths.add("已设置复习时间");
            else report.gaps.add("缺少复习时间");

            if (!hasQuestion || !hasSource || !hasReview) {
                report.actions.add(new SuggestedAction("补学习闭环", "补检索题、来源和复习锚点", "learning_patch"));
            }

            report.checks.add("学习结构："
                    + (hasQuestion ? "有问题" : "缺问题") + " / "
                    + (hasSource ? "有来源" : "缺来源") + " / "
                    + (hasReview ? "有复习" : "缺复习"));
        }

        if (report.gaps.isEmpty()) {
            report.summary = "该节点整体结构较完整，可以继续深入推进。";
        } else {
            report.summary = "该节点存在 " + report.gaps.size() + " 个主要缺口，建议优先补齐闭环。";
        }

        return report;
    }

    public static AuditResult audit(Map<String, Node> nodes, Map<String, Connection> connections) {
        AuditResult result = new AuditResult();
        if (nodes == null || nodes.isEmpty()) return result;

        for (Node node : nodes.values()) {
            if (node == null) continue;

            if (node.isExecutionNode()) {
                if ((node.getType() == Node.NodeType.TASK || node.getType() == Node.NodeType.ACTION)
                        && safe(node.getTriggerCondition()).isEmpty()) {
                    result.issues.add("执行节点缺少触发器：" + safeTitle(node));
                }
                if ((node.getType() == Node.NodeType.TASK || node.getType() == Node.NodeType.ACTION)
                        && safe(node.getDueAt()).isEmpty() && safe(node.getReviewAt()).isEmpty()) {
                    result.issues.add("执行节点缺少时间锚点：" + safeTitle(node));
                }
                if (node.getType() == Node.NodeType.PROJECT
                        && !hasNeighborOfType(node, nodes, connections, Node.NodeType.ACTION)
                        && !hasNeighborOfType(node, nodes, connections, Node.NodeType.TASK)) {
                    result.issues.add("项目没有落地动作：" + safeTitle(node));
                }
            }

            if (node.isDecisionNode()) {
                if (!hasNeighborOfType(node, nodes, connections, Node.NodeType.OPTION)) {
                    result.issues.add("决策缺少方案节点：" + safeTitle(node));
                }
                if (!hasNeighborOfType(node, nodes, connections, Node.NodeType.EVIDENCE)
                        && !hasNeighborOfType(node, nodes, connections, Node.NodeType.SOURCE)) {
                    result.issues.add("决策缺少证据支持/反证：" + safeTitle(node));
                }
                if (!hasNeighborOfType(node, nodes, connections, Node.NodeType.RISK)
                        && !hasNeighborOfType(node, nodes, connections, Node.NodeType.OBSTACLE)) {
                    result.issues.add("决策缺少风险节点：" + safeTitle(node));
                }
            }

            if (node.isLearningNode()) {
                if ((node.getType() == Node.NodeType.CONCEPT || node.getType() == Node.NodeType.NOTE)
                        && !hasNeighborOfType(node, nodes, connections, Node.NodeType.QUESTION)) {
                    result.issues.add("学习节点缺少检索问题：" + safeTitle(node));
                }
                if ((node.getType() == Node.NodeType.CONCEPT || node.getType() == Node.NodeType.NOTE || node.getType() == Node.NodeType.QUESTION)
                        && safe(node.getReviewAt()).isEmpty()) {
                    result.issues.add("学习节点缺少复习时间：" + safeTitle(node));
                }
            }
        }

        return result;
    }

    public static void execute(MainActivity activity, Node node, SuggestedAction action) {
        if (activity == null || node == null || action == null) return;
        activity.getMindMapView().selectOnlyNode(node.getId());

        if ("execution_patch".equals(action.actionKey)) {
            activity.runAiExecutionPatch();
        } else if ("decision_patch".equals(action.actionKey)) {
            activity.openAiScienceCoach("decision", node);
        } else if ("learning_patch".equals(action.actionKey)) {
            activity.runAiLearningPatch();
        } else {
            activity.runScientificEnhancement();
        }
    }

    private static boolean hasNeighborOfType(Node center,
                                             Map<String, Node> nodes,
                                             Map<String, Connection> connections,
                                             Node.NodeType targetType) {
        if (center == null || nodes == null || connections == null) return false;
        for (Connection c : connections.values()) {
            if (c == null) continue;

            if (center.getId().equals(c.getFromNodeId())) {
                Node other = nodes.get(c.getToNodeId());
                if (other != null && other.getType() == targetType) return true;
            }

            if (center.getId().equals(c.getToNodeId())) {
                Node other = nodes.get(c.getFromNodeId());
                if (other != null && other.getType() == targetType) return true;
            }
        }
        return false;
    }

    private static String resolveLane(Node node) {
        if (node == null) return "执行";
        if (node.isDecisionNode()) return "决策";
        if (node.isLearningNode()) return "学习";
        return "执行";
    }

    private static String safeTitle(Node node) {
        String title = node == null ? "" : safe(node.getTitle());
        return title.isEmpty() ? "未命名节点" : title;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
