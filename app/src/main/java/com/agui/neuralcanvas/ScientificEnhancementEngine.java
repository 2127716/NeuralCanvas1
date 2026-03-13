package com.agui.neuralcanvas;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ScientificEnhancementEngine {

    public static class EnhancementResult {
        public final ScientificTemplateEngine.TemplateResult templateResult = new ScientificTemplateEngine.TemplateResult();
        public final List<String> actions = new ArrayList<>();
        public boolean touchedBaseNode = false;
    }

    private ScientificEnhancementEngine() {}

    public static EnhancementResult enhance(Node baseNode,
                                            Map<String, Node> nodes,
                                            Map<String, Connection> connections) {
        EnhancementResult result = new EnhancementResult();
        if (baseNode == null) return result;

        WorkflowEngine.normalizeNodeForWorkflow(baseNode);
        maybeNormalizeBaseNode(baseNode, result);

        if (baseNode.isLearningNode()) {
            enhanceLearning(baseNode, nodes, connections, result);
        }
        if (baseNode.isDecisionNode() || baseNode.getType() == Node.NodeType.PROJECT || baseNode.getType() == Node.NodeType.GOAL) {
            enhanceDecision(baseNode, nodes, connections, result);
        }
        if (baseNode.isExecutionNode() || baseNode.getType() == Node.NodeType.PROJECT || baseNode.getType() == Node.NodeType.GOAL) {
            enhanceExecution(baseNode, nodes, connections, result);
        }

        ensureReviewAnchor(baseNode, nodes, result);
        return result;
    }

    public static String buildSummary(EnhancementResult result) {
        if (result == null) return "未生成任何增强内容";
        int nodeCount = result.templateResult.createdNodes.size();
        int connCount = result.templateResult.createdConnections.size();
        if (result.actions.isEmpty() && !result.touchedBaseNode && nodeCount == 0) {
            return "当前节点结构已经比较完整";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("已补强：");
        if (!result.actions.isEmpty()) {
            for (int i = 0; i < result.actions.size(); i++) {
                if (i > 0) sb.append(" / ");
                sb.append(result.actions.get(i));
            }
        } else {
            sb.append("元数据校准");
        }
        if (nodeCount > 0 || connCount > 0) {
            sb.append("（+").append(nodeCount).append(" 节点, ").append(connCount).append(" 连线）");
        }
        return sb.toString();
    }

    private static void maybeNormalizeBaseNode(Node baseNode, EnhancementResult result) {
        boolean changed = false;
        if (baseNode.getPriority() <= 0) {
            baseNode.setPriority(3);
            changed = true;
        }
        if ((baseNode.getType() == Node.NodeType.PROJECT || baseNode.getType() == Node.NodeType.GOAL)
                && baseNode.getEffortEstimate() <= 0f) {
            baseNode.setEffortEstimate(2f);
            changed = true;
        }
        if (baseNode.isLearningNode()) {
            long due = MemoryEngine.getDueAt(baseNode);
            if (due > 0L && WorkflowEngine.isBlank(baseNode.getReviewAt())) {
                baseNode.setReviewAt(formatDay(due));
                changed = true;
            }
        }
        if (changed) {
            result.touchedBaseNode = true;
            result.actions.add("基础字段校准");
        }
    }

    private static void enhanceLearning(Node baseNode,
                                        Map<String, Node> nodes,
                                        Map<String, Connection> connections,
                                        EnhancementResult result) {
        boolean hasRetrieval = hasTagNearby(baseNode, nodes, "Retrieval") || hasTagNearby(baseNode, nodes, "检索");
        boolean hasTransfer = hasTagNearby(baseNode, nodes, "Transfer") || hasTagNearby(baseNode, nodes, "迁移");
        boolean hasCounter = hasOpposition(baseNode, connections);

        if (!hasRetrieval) {
            merge(result.templateResult, ScientificTemplateEngine.generateRetrievalPractice(baseNode, nodes));
            result.actions.add("检索练习链");
        }
        if (!hasTransfer && (baseNode.getType() == Node.NodeType.CONCEPT || baseNode.getType() == Node.NodeType.INSIGHT || baseNode.getType() == Node.NodeType.NOTE)) {
            merge(result.templateResult, ScientificTemplateEngine.generateTransferPractice(baseNode, nodes));
            result.actions.add("迁移应用链");
        }
        if (!hasCounter) {
            Node counter = new Node("反例/边界｜别把理解错当掌握",
                    "这个概念在哪些条件下不成立？有哪些典型反例、边界条件或易混淆点？",
                    baseNode.getX() + 360f, baseNode.getY() - 20f,
                    Node.NodeType.QUESTION);
            counter.setShape(Node.NodeShape.DIAMOND);
            counter.setProjectId(WorkflowEngine.resolveOwnerId(baseNode));
            counter.setTagsFromString("Learning,Counterexample,反例");
            result.templateResult.createdNodes.add(counter);
            result.templateResult.createdConnections.add(
                    new Connection(counter.getId(), baseNode.getId(), Connection.ConnectionType.OPPOSES, "反例检验")
            );
            result.actions.add("反例校验");
        }
    }

    private static void enhanceDecision(Node baseNode,
                                        Map<String, Node> nodes,
                                        Map<String, Connection> connections,
                                        EnhancementResult result) {
        boolean hasEvidenceFor = countConnectionsTo(baseNode, connections, Connection.ConnectionType.EVIDENCE_FOR) > 0;
        boolean hasEvidenceAgainst = countConnectionsTo(baseNode, connections, Connection.ConnectionType.EVIDENCE_AGAINST) > 0;
        boolean hasRisk = hasTypeNearby(baseNode, nodes, Node.NodeType.RISK) || hasTypeNearby(baseNode, nodes, Node.NodeType.OBSTACLE);
        boolean hasOptions = hasTypeNearby(baseNode, nodes, Node.NodeType.OPTION);

        if (!hasOptions && (baseNode.getType() == Node.NodeType.DECISION || baseNode.getType() == Node.NodeType.GOAL)) {
            merge(result.templateResult, ScientificTemplateEngine.generateDecisionTree(baseNode, nodes));
            result.actions.add("候选方案与准则");
        }
        if (!hasEvidenceFor || !hasEvidenceAgainst) {
            merge(result.templateResult, ScientificTemplateEngine.generateEvidenceReview(baseNode, nodes));
            result.actions.add("正反证据审查");
        }
        if (!hasRisk) {
            merge(result.templateResult, ScientificTemplateEngine.generatePremortem(baseNode, nodes));
            result.actions.add("失败预演");
        }
        if (!hasTagNearby(baseNode, nodes, "WRAP")) {
            merge(result.templateResult, ScientificTemplateEngine.generateWrap(baseNode, nodes));
            result.actions.add("WRAP 护栏");
        }
    }

    private static void enhanceExecution(Node baseNode,
                                         Map<String, Node> nodes,
                                         Map<String, Connection> connections,
                                         EnhancementResult result) {
        boolean hasAction = hasActionNearby(baseNode, nodes, connections);
        boolean hasTrigger = hasTypeNearby(baseNode, nodes, Node.NodeType.TRIGGER) || !WorkflowEngine.isBlank(baseNode.getTriggerCondition());
        boolean hasObstacle = hasTypeNearby(baseNode, nodes, Node.NodeType.OBSTACLE);
        boolean hasForecast = hasTagNearby(baseNode, nodes, "Forecast") || hasTagNearby(baseNode, nodes, "参考类预测");

        if (!hasAction && (baseNode.getType() == Node.NodeType.PROJECT || baseNode.getType() == Node.NodeType.GOAL || baseNode.getType() == Node.NodeType.DECISION)) {
            merge(result.templateResult, ScientificTemplateEngine.generateWoop(baseNode, nodes));
            result.actions.add("目标拆解");
        }
        if (!hasTrigger) {
            merge(result.templateResult, ScientificTemplateEngine.generateIfThen(baseNode, nodes));
            result.actions.add("If-Then 启动器");
        }
        if (!hasObstacle && baseNode.getType() != Node.NodeType.OBSTACLE) {
            Node obstacle = new Node("关键阻碍｜最容易卡住我的点",
                    "这个任务真正的启动阻力是什么？时间、环境、情绪、能力、依赖还是信息不足？",
                    baseNode.getX() - 320f, baseNode.getY() + 220f,
                    Node.NodeType.OBSTACLE);
            obstacle.setShape(Node.NodeShape.DIAMOND);
            obstacle.setProjectId(WorkflowEngine.resolveOwnerId(baseNode));
            obstacle.setPriority(Math.max(3, baseNode.getPriority()));
            obstacle.setTagsFromString("Execution,Obstacle,阻碍");
            result.templateResult.createdNodes.add(obstacle);
            result.templateResult.createdConnections.add(
                    new Connection(obstacle.getId(), baseNode.getId(), Connection.ConnectionType.BLOCKS, "主要阻碍")
            );
            result.actions.add("阻碍显化");
        }
        if (!hasForecast && (baseNode.getType() == Node.NodeType.PROJECT || baseNode.getType() == Node.NodeType.TASK || baseNode.getType() == Node.NodeType.ACTION || baseNode.getType() == Node.NodeType.GOAL)) {
            merge(result.templateResult, ScientificTemplateEngine.generateReferenceClassForecast(baseNode, nodes));
            result.actions.add("参考类预测");
        }
    }

    private static void ensureReviewAnchor(Node baseNode,
                                           Map<String, Node> nodes,
                                           EnhancementResult result) {
        if (hasTypeNearby(baseNode, nodes, Node.NodeType.REVIEW) || !WorkflowEngine.isBlank(baseNode.getReviewAt())) return;
        Node review = new Node("AAR/复盘｜下一轮如何更强",
                "完成后对照预期与实际，记录偏差原因与可复用改进。",
                baseNode.getX() + 20f, baseNode.getY() + 320f,
                Node.NodeType.REVIEW);
        review.setShape(Node.NodeShape.OVAL);
        review.setStatus(Node.NodeStatus.REVIEW);
        review.setProjectId(WorkflowEngine.resolveOwnerId(baseNode));
        review.setReviewAt(formatDay(System.currentTimeMillis() + 3L * 24L * 60L * 60L * 1000L));
        review.setTagsFromString("Review,AAR,复盘");
        result.templateResult.createdNodes.add(review);
        result.templateResult.createdConnections.add(
                new Connection(baseNode.getId(), review.getId(), Connection.ConnectionType.LEADS_TO, "完成后复盘")
        );
        result.actions.add("复盘锚点");
    }

    private static boolean hasActionNearby(Node baseNode, Map<String, Node> nodes, Map<String, Connection> connections) {
        if (baseNode == null || nodes == null) return false;
        String ownerId = WorkflowEngine.resolveOwnerId(baseNode);
        for (Node node : nodes.values()) {
            if (node == null || node.getId().equals(baseNode.getId())) continue;
            if ((node.getType() == Node.NodeType.ACTION || node.getType() == Node.NodeType.TASK)
                    && (ownerId.equals(node.getProjectId()) || isDirectlyLinked(baseNode, node, connections))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTypeNearby(Node baseNode, Map<String, Node> nodes, Node.NodeType targetType) {
        if (baseNode == null || nodes == null || targetType == null) return false;
        String ownerId = WorkflowEngine.resolveOwnerId(baseNode);
        for (Node node : nodes.values()) {
            if (node == null || node.getId().equals(baseNode.getId())) continue;
            if (node.getType() != targetType) continue;
            if (ownerId.equals(WorkflowEngine.resolveOwnerId(node)) || ownerId.equals(node.getProjectId()) || baseNode.getId().equals(node.getProjectId())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTagNearby(Node baseNode, Map<String, Node> nodes, String tag) {
        if (baseNode == null || nodes == null || WorkflowEngine.isBlank(tag)) return false;
        String ownerId = WorkflowEngine.resolveOwnerId(baseNode);
        for (Node node : nodes.values()) {
            if (node == null || node.getId().equals(baseNode.getId())) continue;
            if (!node.hasTag(tag)) continue;
            if (ownerId.equals(WorkflowEngine.resolveOwnerId(node)) || ownerId.equals(node.getProjectId()) || baseNode.getId().equals(node.getProjectId())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasOpposition(Node baseNode, Map<String, Connection> connections) {
        if (baseNode == null || connections == null) return false;
        for (Connection c : connections.values()) {
            if (c == null || c.getType() != Connection.ConnectionType.OPPOSES) continue;
            if (baseNode.getId().equals(c.getFromNodeId()) || baseNode.getId().equals(c.getToNodeId())) return true;
        }
        return false;
    }

    private static boolean isDirectlyLinked(Node a, Node b, Map<String, Connection> connections) {
        if (a == null || b == null || connections == null) return false;
        for (Connection c : connections.values()) {
            if (c == null) continue;
            boolean linked = (a.getId().equals(c.getFromNodeId()) && b.getId().equals(c.getToNodeId()))
                    || (a.getId().equals(c.getToNodeId()) && b.getId().equals(c.getFromNodeId()));
            if (linked) return true;
        }
        return false;
    }

    private static int countConnectionsTo(Node target, Map<String, Connection> connections, Connection.ConnectionType type) {
        if (target == null || connections == null || type == null) return 0;
        int count = 0;
        for (Connection c : connections.values()) {
            if (c == null || c.getType() != type) continue;
            if (target.getId().equals(c.getToNodeId()) || target.getId().equals(c.getFromNodeId())) count++;
        }
        return count;
    }

    private static void merge(ScientificTemplateEngine.TemplateResult target,
                              ScientificTemplateEngine.TemplateResult source) {
        if (target == null || source == null) return;
        target.createdNodes.addAll(source.createdNodes);
        target.createdConnections.addAll(source.createdConnections);
    }

    private static String formatDay(long timeMs) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(timeMs));
    }
}
