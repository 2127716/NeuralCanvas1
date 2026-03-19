
package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WorkflowQuickFixEngine {

    public static final class FixResult {
        public int changedNodeCount = 0;
        public int createdNodeCount = 0;
        public int createdConnectionCount = 0;
        public final List<String> notes = new ArrayList<>();
        public final Set<String> touchedNodeIds = new LinkedHashSet<>();
        public final Set<String> createdNodeIds = new LinkedHashSet<>();

        public int totalChanges() {
            return changedNodeCount + createdNodeCount + createdConnectionCount;
        }

        public String buildSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("已修复 ").append(totalChanges()).append(" 项变更");
            if (!notes.isEmpty()) {
                int limit = Math.min(4, notes.size());
                for (int i = 0; i < limit; i++) {
                    sb.append(i == 0 ? "：" : "；").append(notes.get(i));
                }
            }
            return sb.toString();
        }
    }

    private WorkflowQuickFixEngine() {}

    public static FixResult quickFixNode(MainActivity activity, Node baseNode) {
        FixResult result = new FixResult();
        if (activity == null || baseNode == null || activity.getMindMapView() == null) return result;

        MindMapView view = activity.getMindMapView();
        Map<String, Node> nodes = view.getNodesInternal();
        Map<String, Connection> connections = view.getConnectionsInternal();

        touch(result, baseNode);
        WorkflowEngine.normalizeNodeForWorkflow(baseNode);

        if (isProjectLike(baseNode)) {
            ensureProjectId(baseNode, result);
            if (!hasOwnedType(baseNode, nodes, Node.NodeType.KEY_RESULT)) {
                createKrNode(view, baseNode, result);
            }
            if (!hasOwnedReview(baseNode, nodes)) {
                createReviewNode(view, baseNode, result);
            }
            if (!WorkflowEngine.hasOpenChildAction(baseNode, nodes, connections)) {
                createNextActionNode(view, baseNode, result);
            }
        }

        if (WorkflowEngine.isActionable(baseNode) && baseNode.getStatus() != Node.NodeStatus.DONE) {
            if (WorkflowEngine.isBlank(baseNode.getTriggerCondition())) {
                baseNode.setTriggerCondition(defaultTriggerFor(baseNode));
                result.changedNodeCount++;
                result.notes.add("补了触发条件");
                touch(result, baseNode);
            }
        }

        if (baseNode.isExecutionNode() && !hasNearbyType(baseNode, nodes, Node.NodeType.OBSTACLE)) {
            createObstacleNode(view, baseNode, result);
        }

        if (baseNode.isDecisionNode() || baseNode.getType() == Node.NodeType.QUESTION) {
            if (!hasNearbyType(baseNode, nodes, Node.NodeType.EVIDENCE)) {
                createEvidenceNode(view, baseNode, result);
            }
            if (!hasNearbyType(baseNode, nodes, Node.NodeType.RISK)) {
                createRiskNode(view, baseNode, result);
            }
            if (baseNode.getConfidence() > 0.72f && baseNode.getEvidenceStrength() < 0.50f) {
                baseNode.setConfidence(0.65f);
                result.changedNodeCount++;
                result.notes.add("下调了过高置信度");
                touch(result, baseNode);
            }
        }

        if (baseNode.isLearningNode() || baseNode.getType() == Node.NodeType.RESOURCE) {
            if (WorkflowEngine.isBlank(baseNode.getReviewAt())) {
                baseNode.setReviewAt("尽快第一次回忆");
                result.changedNodeCount++;
                result.notes.add("安排了首次复习");
                touch(result, baseNode);
            }
            if (!hasNearbyType(baseNode, nodes, Node.NodeType.QUESTION)) {
                createRetrievalQuestion(view, baseNode, result);
            }
            if (!hasNearbyType(baseNode, nodes, Node.NodeType.RESOURCE, Node.NodeType.SOURCE)) {
                createExampleNode(view, baseNode, result);
            }
            if (!hasNearbyType(baseNode, nodes, Node.NodeType.EXPERIMENT)) {
                createTransferExperiment(view, baseNode, result);
            }
        }

        WorkflowEngine.normalizeNodeForWorkflow(baseNode);
        activity.onNodeUpdated(baseNode);
        activity.onGraphMutatedByAi();
        return result;
    }

    public static FixResult quickFixProjectHealth(MainActivity activity,
                                                  ProjectHealthEngine.ProjectHealthReport report) {
        FixResult total = new FixResult();
        if (activity == null || report == null) return total;

        Set<String> handled = new LinkedHashSet<>();
        int budget = 12;
        budget = applyList(activity, report.stuckProjects, handled, budget, total);
        budget = applyList(activity, report.projectsWithoutKr, handled, budget, total);
        budget = applyList(activity, report.projectsWithoutReview, handled, budget, total);
        budget = applyList(activity, report.actionsWithoutTrigger, handled, budget, total);
        budget = applyList(activity, report.weakEvidenceDecisions, handled, budget, total);
        budget = applyList(activity, report.staleLearningNodes, handled, budget, total);
        applyList(activity, report.overdueActions, handled, budget, total);
        activity.onGraphMutatedByAi();
        return total;
    }

    private static int applyList(MainActivity activity, List<Node> source, Set<String> handled, int budget, FixResult total) {
        if (source == null) return budget;
        for (Node node : source) {
            if (node == null || budget <= 0) break;
            if (handled.contains(node.getId())) continue;
            FixResult part = quickFixNode(activity, node);
            merge(total, part);
            handled.add(node.getId());
            budget--;
        }
        return budget;
    }

    private static void merge(FixResult into, FixResult from) {
        into.changedNodeCount += from.changedNodeCount;
        into.createdNodeCount += from.createdNodeCount;
        into.createdConnectionCount += from.createdConnectionCount;
        into.notes.addAll(from.notes);
        into.touchedNodeIds.addAll(from.touchedNodeIds);
        into.createdNodeIds.addAll(from.createdNodeIds);
    }

    private static void touch(FixResult result, Node node) {
        if (result != null && node != null) result.touchedNodeIds.add(node.getId());
    }

    private static void created(FixResult result, Node node) {
        if (result != null && node != null) {
            result.createdNodeIds.add(node.getId());
            result.touchedNodeIds.add(node.getId());
        }
    }

    private static void ensureProjectId(Node node, FixResult result) {
        if (node.getType() == Node.NodeType.PROJECT && WorkflowEngine.isBlank(node.getProjectId())) {
            node.setProjectId(node.getId());
            node.addTag("Project");
            result.changedNodeCount++;
            result.notes.add("补了项目归属");
            touch(result, node);
        }
    }

    private static boolean isProjectLike(Node node) {
        return node != null && (node.getType() == Node.NodeType.PROJECT
                || node.getType() == Node.NodeType.GOAL
                || node.getType() == Node.NodeType.DECISION);
    }

    private static boolean hasOwnedType(Node baseNode, Map<String, Node> nodes, Node.NodeType type) {
        if (baseNode == null || nodes == null || type == null) return false;
        String ownerId = WorkflowEngine.resolveOwnerId(baseNode);
        for (Node node : nodes.values()) {
            if (node == null || node.getId().equals(baseNode.getId())) continue;
            if (type != node.getType()) continue;
            if (ownerId.equals(node.getProjectId()) || distance(baseNode, node) <= 920f) return true;
        }
        return false;
    }

    private static boolean hasOwnedReview(Node baseNode, Map<String, Node> nodes) {
        if (!WorkflowEngine.isBlank(baseNode.getReviewAt())) return true;
        return hasOwnedType(baseNode, nodes, Node.NodeType.REVIEW);
    }

    private static boolean hasNearbyType(Node baseNode, Map<String, Node> nodes, Node.NodeType... types) {
        if (baseNode == null || nodes == null || types == null) return false;
        for (Node node : nodes.values()) {
            if (node == null || node.getId().equals(baseNode.getId())) continue;
            boolean matched = false;
            for (Node.NodeType type : types) {
                if (node.getType() == type) { matched = true; break; }
            }
            if (!matched) continue;
            String ownerA = WorkflowEngine.resolveOwnerId(baseNode);
            String ownerB = WorkflowEngine.resolveOwnerId(node);
            if ((!WorkflowEngine.isBlank(ownerA) && ownerA.equals(ownerB)) || distance(baseNode, node) <= 920f) {
                return true;
            }
        }
        return false;
    }

    private static double distance(Node a, Node b) {
        float dx = a.getX() - b.getX();
        float dy = a.getY() - b.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static String ownerIdOf(Node node) {
        String ownerId = WorkflowEngine.resolveOwnerId(node);
        return WorkflowEngine.isBlank(ownerId) ? node.getId() : ownerId;
    }

    private static String safeTitle(Node node) {
        String title = node == null ? "" : WorkflowEngine.safe(node.getTitle());
        return title.isEmpty() ? "当前节点" : title;
    }

    private static String defaultTriggerFor(Node node) {
        if (node == null) return "如果我要开始推进这件事，那么先做一个最小动作";
        if (node.getType() == Node.NodeType.ACTION || node.getType() == Node.NodeType.TASK) {
            return "如果我开始推进“" + safeTitle(node) + "”，那么先做第一个最小动作";
        }
        return "如果我要开始推进这件事，那么先做一个最小动作";
    }

    private static void createKrNode(MindMapView view, Node baseNode, FixResult result) {
        Node node = new Node("KR｜" + safeTitle(baseNode), "补一个可量化关键结果", baseNode.getX() - 260f, baseNode.getY() + 220f, Node.NodeType.KEY_RESULT);
        node.setShape(Node.NodeShape.HEXAGON);
        node.setStatus(Node.NodeStatus.ACTIVE);
        node.setProjectId(ownerIdOf(baseNode));
        node.setAreaId(WorkflowEngine.deriveAreaName(baseNode));
        node.setKrTarget(1f);
        node.setKrCurrent(0f);
        node.setTagsFromString("QuickFix,KR,关键结果");
        view.addNode(node);
        view.addConnection(new Connection(baseNode.getId(), node.getId(), Connection.ConnectionType.LEADS_TO, "补 KR"));
        result.createdNodeCount++;
        result.createdConnectionCount++;
        result.notes.add("补了 KR 节点");
        created(result, node);
    }

    private static void createReviewNode(MindMapView view, Node baseNode, FixResult result) {
        Node node = new Node("Review｜" + safeTitle(baseNode), "本周/本轮推进了什么？卡在哪？下次怎么调？", baseNode.getX(), baseNode.getY() + 420f, Node.NodeType.REVIEW);
        node.setShape(Node.NodeShape.OVAL);
        node.setStatus(Node.NodeStatus.REVIEW);
        node.setProjectId(ownerIdOf(baseNode));
        node.setAreaId(WorkflowEngine.deriveAreaName(baseNode));
        node.setReviewAt(baseNode.isLearningNode() ? "尽快第一次回忆" : "每周回顾");
        node.setTagsFromString("QuickFix,Review,复盘");
        view.addNode(node);
        view.addConnection(new Connection(baseNode.getId(), node.getId(), Connection.ConnectionType.LEADS_TO, "补复盘"));
        result.createdNodeCount++;
        result.createdConnectionCount++;
        result.notes.add("补了复盘节点");
        created(result, node);
    }

    private static void createNextActionNode(MindMapView view, Node baseNode, FixResult result) {
        Node node = new Node("Next Action｜" + safeTitle(baseNode), "把它缩到 2~10 分钟内能开始的一步", baseNode.getX() + 280f, baseNode.getY() + 220f, Node.NodeType.ACTION);
        node.setShape(Node.NodeShape.RECT);
        node.setStatus(Node.NodeStatus.PLANNED);
        node.setProjectId(ownerIdOf(baseNode));
        node.setAreaId(WorkflowEngine.deriveAreaName(baseNode));
        node.setTriggerCondition("如果我要开始推进“" + safeTitle(baseNode) + "”，那么先做这一步");
        node.setTagsFromString("QuickFix,Action,最小下一步");
        view.addNode(node);
        view.addConnection(new Connection(baseNode.getId(), node.getId(), Connection.ConnectionType.LEADS_TO, "补下一步"));
        result.createdNodeCount++;
        result.createdConnectionCount++;
        result.notes.add("补了最小下一步");
        created(result, node);
    }

    private static void createObstacleNode(MindMapView view, Node baseNode, FixResult result) {
        Node node = new Node("Obstacle｜" + safeTitle(baseNode), "最可能卡住我的干扰是什么？我如何提前削弱它？", baseNode.getX() - 300f, baseNode.getY() + 180f, Node.NodeType.OBSTACLE);
        node.setShape(Node.NodeShape.DIAMOND);
        node.setStatus(Node.NodeStatus.ACTIVE);
        node.setProjectId(ownerIdOf(baseNode));
        node.setAreaId(WorkflowEngine.deriveAreaName(baseNode));
        node.setTagsFromString("QuickFix,Obstacle,障碍");
        view.addNode(node);
        view.addConnection(new Connection(node.getId(), baseNode.getId(), Connection.ConnectionType.BLOCKS, "潜在阻碍"));
        result.createdNodeCount++;
        result.createdConnectionCount++;
        result.notes.add("补了障碍节点");
        created(result, node);
    }

    private static void createEvidenceNode(MindMapView view, Node baseNode, FixResult result) {
        Node node = new Node("Evidence｜" + safeTitle(baseNode), "先补一条支持/反驳判断的事实、数据或观察", baseNode.getX() - 280f, baseNode.getY() + 220f, Node.NodeType.EVIDENCE);
        node.setShape(Node.NodeShape.RECT);
        node.setStatus(Node.NodeStatus.ACTIVE);
        node.setProjectId(ownerIdOf(baseNode));
        node.setAreaId(WorkflowEngine.deriveAreaName(baseNode));
        node.setEvidenceStrength(0.55f);
        node.setTagsFromString("QuickFix,Evidence,证据");
        view.addNode(node);
        view.addConnection(new Connection(node.getId(), baseNode.getId(), Connection.ConnectionType.EVIDENCE_FOR, "补证据"));
        baseNode.setEvidenceStrength(Math.max(baseNode.getEvidenceStrength(), 0.55f));
        result.createdNodeCount++;
        result.createdConnectionCount++;
        result.changedNodeCount++;
        result.notes.add("补了证据节点");
        created(result, node);
        touch(result, baseNode);
    }

    private static void createRiskNode(MindMapView view, Node baseNode, FixResult result) {
        Node node = new Node("Risk｜" + safeTitle(baseNode), "最坏会出什么问题？最早的预警信号是什么？", baseNode.getX() + 300f, baseNode.getY() + 220f, Node.NodeType.RISK);
        node.setShape(Node.NodeShape.DIAMOND);
        node.setStatus(Node.NodeStatus.ACTIVE);
        node.setProjectId(ownerIdOf(baseNode));
        node.setAreaId(WorkflowEngine.deriveAreaName(baseNode));
        node.setTagsFromString("QuickFix,Risk,风险");
        view.addNode(node);
        view.addConnection(new Connection(node.getId(), baseNode.getId(), Connection.ConnectionType.BLOCKS, "补风险"));
        result.createdNodeCount++;
        result.createdConnectionCount++;
        result.notes.add("补了风险节点");
        created(result, node);
    }

    private static void createRetrievalQuestion(MindMapView view, Node baseNode, FixResult result) {
        Node node = new Node("Retrieval｜" + safeTitle(baseNode), "不用看原文，先回答：它是什么？核心机制/步骤是什么？", baseNode.getX() - 320f, baseNode.getY() + 180f, Node.NodeType.QUESTION);
        node.setShape(Node.NodeShape.OVAL);
        node.setStatus(Node.NodeStatus.ACTIVE);
        node.setProjectId(ownerIdOf(baseNode));
        node.setAreaId(WorkflowEngine.deriveAreaName(baseNode));
        node.setReviewAt("尽快第一次回忆");
        node.setTagsFromString("QuickFix,Retrieval,检索练习");
        view.addNode(node);
        view.addConnection(new Connection(baseNode.getId(), node.getId(), Connection.ConnectionType.LEADS_TO, "补检索问题"));
        result.createdNodeCount++;
        result.createdConnectionCount++;
        result.notes.add("补了检索问题");
        created(result, node);
    }

    private static void createExampleNode(MindMapView view, Node baseNode, FixResult result) {
        Node node = new Node("Example｜" + safeTitle(baseNode), "给它补一个真实例子或可信来源", baseNode.getX(), baseNode.getY() + 180f, Node.NodeType.RESOURCE);
        node.setShape(Node.NodeShape.RECT);
        node.setStatus(Node.NodeStatus.ACTIVE);
        node.setProjectId(ownerIdOf(baseNode));
        node.setAreaId(WorkflowEngine.deriveAreaName(baseNode));
        node.setTagsFromString("QuickFix,Example,例子");
        view.addNode(node);
        view.addConnection(new Connection(baseNode.getId(), node.getId(), Connection.ConnectionType.SUPPORTS, "补例子"));
        result.createdNodeCount++;
        result.createdConnectionCount++;
        result.notes.add("补了例子/来源");
        created(result, node);
    }

    private static void createTransferExperiment(MindMapView view, Node baseNode, FixResult result) {
        Node node = new Node("Transfer｜" + safeTitle(baseNode), "换一个场景应用它，做一个小验证任务", baseNode.getX() + 320f, baseNode.getY() + 180f, Node.NodeType.EXPERIMENT);
        node.setShape(Node.NodeShape.HEXAGON);
        node.setStatus(Node.NodeStatus.PLANNED);
        node.setProjectId(ownerIdOf(baseNode));
        node.setAreaId(WorkflowEngine.deriveAreaName(baseNode));
        node.setTriggerCondition("如果我要确认自己真的会了，那么先做这个迁移验证");
        node.setTagsFromString("QuickFix,Transfer,迁移");
        view.addNode(node);
        view.addConnection(new Connection(baseNode.getId(), node.getId(), Connection.ConnectionType.SUPPORTS, "补迁移验证"));
        result.createdNodeCount++;
        result.createdConnectionCount++;
        result.notes.add("补了迁移验证");
        created(result, node);
    }
}
