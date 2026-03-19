package com.agui.neuralcanvas;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuickActionEngine {

    public static void markDone(Node node, MainActivity activity) {
        if (node == null || activity == null) return;
        node.setStatus(Node.NodeStatus.DONE);
        activity.onNodeUpdated(node);
    }

    public static void addKrValue(Node node, float delta, MainActivity activity) {
        if (node == null || activity == null) return;
        node.setKrCurrent(node.getKrCurrent() + delta);
        activity.onNodeUpdated(node);
    }

    public static void convertInboxNode(Node node, Node.NodeType targetType, MainActivity activity) {
        if (node == null || activity == null) return;

        node.setType(targetType);
        WorkflowEngine.normalizeNodeForWorkflow(node);

        if (targetType == Node.NodeType.PROJECT) {
            node.setProjectId(node.getId());
            node.addTags("Project", "InboxConverted");
        } else if (targetType == Node.NodeType.TASK || targetType == Node.NodeType.ACTION) {
            node.addTags("Actionable", "InboxConverted");
        } else if (targetType == Node.NodeType.DECISION) {
            node.addTags("Decision", "InboxConverted");
        } else if (targetType == Node.NodeType.NOTE
                || targetType == Node.NodeType.CONCEPT
                || targetType == Node.NodeType.QUESTION
                || targetType == Node.NodeType.RESOURCE) {
            node.addTags("Learning", "InboxConverted");
        } else {
            node.addTag("InboxConverted");
        }

        activity.onNodeUpdated(node);
    }

    public static List<String> getDynamicActions(Node node) {
        return NodeTypeBehaviorRegistry.getActions(node);
    }

    public static void executeDynamicAction(MainActivity activity, Node node, String action) {
        if (activity == null || node == null || action == null) return;

        switch (action) {
            case "编辑节点":
                activity.editNodeFromQuickAction(node);
                return;

            case "删除节点":
                activity.deleteNodeFromQuickAction(node);
                return;

            case "方法推荐":
                WorkflowRecommendationDialog.show(activity, node);
                return;

            case "执行模式":
                WorkflowModeDialog.show(activity, node, "execution");
                return;

            case "决策模式":
                WorkflowModeDialog.show(activity, node, "decision");
                return;

            case "学习模式":
                WorkflowModeDialog.show(activity, node, "learning");
                return;

            case "工作流体检":
                WorkflowHealthDialog.show(activity, node);
                return;

            case "澄清到工作流":
                activity.openInboxClarifierForSingleNode(node);
                return;

            case "生成项目起步结构":
                generateProjectStarterNodes(node, activity);
                return;

            case "生成决策起步结构":
                generateDecisionStarterNodes(node, activity);
                return;

            case "生成学习起步结构":
                generateLearningStarterNodes(node, activity);
                return;

            case "标记完成":
                markDone(node, activity);
                return;

            case "KR +0.1":
                addKrValue(node, 0.1f, activity);
                return;

            case "KR +1":
                addKrValue(node, 1f, activity);
                return;

            case "移动到 Someday":
                node.setStatus(Node.NodeStatus.SOMEDAY);
                activity.onNodeUpdated(node);
                return;

            case "移动到 Waiting":
                node.setStatus(Node.NodeStatus.WAITING);
                activity.onNodeUpdated(node);
                return;

            case "安排到今天复盘":
                node.setStatus(Node.NodeStatus.REVIEW);
                node.setReviewAt(todayString());
                activity.onNodeUpdated(node);
                return;

            case "提升优先级":
                node.setPriority(Math.min(5, node.getPriority() + 1));
                activity.onNodeUpdated(node);
                return;

            case "移动到 Areas":
                moveToAreas(node, activity);
                return;

            case "清除 Area 归属":
                node.setAreaId("");
                activity.onNodeUpdated(node);
                return;

            case "移动到 Resources":
                moveToResources(node, activity);
                return;

            case "归档到 Archives":
                archiveNode(node, activity);
                return;

            default:
                break;
        }
    }

    private static void moveToAreas(Node node, MainActivity activity) {
        if (node == null || activity == null) return;
        String areaName = WorkflowEngine.deriveAreaName(node);
        node.setAreaId(areaName);
        node.addTag("Area");
        activity.onNodeUpdated(node);
    }

    private static void moveToResources(Node node, MainActivity activity) {
        if (node == null || activity == null) return;
        if (node.getType() != Node.NodeType.RESOURCE
                && node.getType() != Node.NodeType.SOURCE) {
            node.setType(Node.NodeType.RESOURCE);
        }
        node.addTag("Resource");
        WorkflowEngine.normalizeNodeForWorkflow(node);
        activity.onNodeUpdated(node);
    }

    private static void archiveNode(Node node, MainActivity activity) {
        if (node == null || activity == null) return;
        node.addTag("Archive");
        if (node.getStatus() != Node.NodeStatus.DONE) {
            node.setStatus(Node.NodeStatus.DONE);
        }
        activity.onNodeUpdated(node);
    }

    private static String todayString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    public static void generateProjectStarterNodes(Node projectNode, MainActivity activity) {
        if (projectNode == null || activity == null || activity.getMindMapView() == null) return;

        MindMapView view = activity.getMindMapView();

        float baseX = projectNode.getX();
        float baseY = projectNode.getY();
        String projectId = projectNode.getId();
        String title = DashboardSectionBuilder.safe(projectNode.getTitle());
        if (title.isEmpty()) title = "项目";

        Node goalNode = new Node("Goal｜" + title, "这个项目真正要实现的结果是什么？",
                baseX - 320f, baseY + 220f, Node.NodeType.GOAL);
        goalNode.setShape(Node.NodeShape.OVAL);
        goalNode.setStatus(Node.NodeStatus.ACTIVE);
        goalNode.setProjectId(projectId);
        goalNode.setAreaId(WorkflowEngine.deriveAreaName(projectNode));
        goalNode.setTagsFromString("Project,Goal,目标");

        Node krNode = new Node("KR｜" + title, "写一个可量化关键结果",
                baseX, baseY + 220f, Node.NodeType.KEY_RESULT);
        krNode.setShape(Node.NodeShape.HEXAGON);
        krNode.setStatus(Node.NodeStatus.ACTIVE);
        krNode.setProjectId(projectId);
        krNode.setAreaId(WorkflowEngine.deriveAreaName(projectNode));
        krNode.setKrTarget(1f);
        krNode.setKrCurrent(0f);
        krNode.setTagsFromString("Project,KR,关键结果");

        Node actionNode = new Node("First Action｜" + title, "这个项目最小的下一步是什么？",
                baseX + 320f, baseY + 220f, Node.NodeType.ACTION);
        actionNode.setShape(Node.NodeShape.RECT);
        actionNode.setStatus(Node.NodeStatus.PLANNED);
        actionNode.setProjectId(projectId);
        actionNode.setAreaId(WorkflowEngine.deriveAreaName(projectNode));
        actionNode.setTriggerCondition("如果我要开始推进这个项目，那么先做这个最小动作");
        actionNode.setTagsFromString("Project,FirstAction,执行");

        Node reviewNode = new Node("Weekly Review｜" + title, "本周这个项目推进了什么？卡在哪？下周怎么调？",
                baseX, baseY + 460f, Node.NodeType.REVIEW);
        reviewNode.setShape(Node.NodeShape.OVAL);
        reviewNode.setStatus(Node.NodeStatus.REVIEW);
        reviewNode.setProjectId(projectId);
        reviewNode.setAreaId(WorkflowEngine.deriveAreaName(projectNode));
        reviewNode.setTagsFromString("Project,WeeklyReview,复盘");

        view.addNode(goalNode);
        view.addNode(krNode);
        view.addNode(actionNode);
        view.addNode(reviewNode);

        view.addConnection(new Connection(projectNode.getId(), goalNode.getId(), Connection.ConnectionType.LEADS_TO, "项目目标"));
        view.addConnection(new Connection(projectNode.getId(), krNode.getId(), Connection.ConnectionType.LEADS_TO, "关键结果"));
        view.addConnection(new Connection(projectNode.getId(), actionNode.getId(), Connection.ConnectionType.LEADS_TO, "第一步"));
        view.addConnection(new Connection(projectNode.getId(), reviewNode.getId(), Connection.ConnectionType.LEADS_TO, "每周复盘"));
        view.addConnection(new Connection(goalNode.getId(), krNode.getId(), Connection.ConnectionType.SUPPORTS, "目标量化"));
        view.addConnection(new Connection(actionNode.getId(), goalNode.getId(), Connection.ConnectionType.SUPPORTS, "行动推进目标"));
        view.addConnection(new Connection(reviewNode.getId(), actionNode.getId(), Connection.ConnectionType.TRIGGERS, "复盘指导下一步"));

        activity.onGraphMutatedByAi();
    }

    public static void generateDecisionStarterNodes(Node decisionNode, MainActivity activity) {
        if (decisionNode == null || activity == null || activity.getMindMapView() == null) return;

        MindMapView view = activity.getMindMapView();

        float baseX = decisionNode.getX();
        float baseY = decisionNode.getY();
        String ownerId = DashboardSectionBuilder.safe(decisionNode.getProjectId());
        if (ownerId.isEmpty()) ownerId = decisionNode.getId();

        Node optionA = new Node("Option A", "方案A的核心做法、成本、收益是什么？",
                baseX - 420f, baseY + 60f, Node.NodeType.OPTION);
        optionA.setShape(Node.NodeShape.RECT);
        optionA.setStatus(Node.NodeStatus.PLANNED);
        optionA.setProjectId(ownerId);
        optionA.setAreaId(WorkflowEngine.deriveAreaName(decisionNode));
        optionA.setTagsFromString("Decision,Option,A");

        Node optionB = new Node("Option B", "方案B的核心做法、成本、收益是什么？",
                baseX, baseY + 60f, Node.NodeType.OPTION);
        optionB.setShape(Node.NodeShape.RECT);
        optionB.setStatus(Node.NodeStatus.PLANNED);
        optionB.setProjectId(ownerId);
        optionB.setAreaId(WorkflowEngine.deriveAreaName(decisionNode));
        optionB.setTagsFromString("Decision,Option,B");

        Node optionC = new Node("Option C", "方案C的核心做法、成本、收益是什么？",
                baseX + 420f, baseY + 60f, Node.NodeType.OPTION);
        optionC.setShape(Node.NodeShape.RECT);
        optionC.setStatus(Node.NodeStatus.PLANNED);
        optionC.setProjectId(ownerId);
        optionC.setAreaId(WorkflowEngine.deriveAreaName(decisionNode));
        optionC.setTagsFromString("Decision,Option,C");

        Node criterionNode = new Node("Criterion", "写 3~6 个准则：时间、成本、长期收益、可逆性、风险等",
                baseX - 260f, baseY + 320f, Node.NodeType.CRITERION);
        criterionNode.setShape(Node.NodeShape.HEXAGON);
        criterionNode.setStatus(Node.NodeStatus.ACTIVE);
        criterionNode.setProjectId(ownerId);
        criterionNode.setAreaId(WorkflowEngine.deriveAreaName(decisionNode));
        criterionNode.setTagsFromString("Decision,Criterion,准则");

        Node riskNode = new Node("Risk", "每个方案最可能失败在哪？最坏情况是什么？",
                baseX + 260f, baseY + 320f, Node.NodeType.RISK);
        riskNode.setShape(Node.NodeShape.DIAMOND);
        riskNode.setStatus(Node.NodeStatus.ACTIVE);
        riskNode.setProjectId(ownerId);
        riskNode.setAreaId(WorkflowEngine.deriveAreaName(decisionNode));
        riskNode.setTagsFromString("Decision,Risk,风险");

        Node evidenceNode = new Node("Evidence", "有哪些事实、数据、经验在支持或反驳当前判断？",
                baseX - 260f, baseY + 540f, Node.NodeType.EVIDENCE);
        evidenceNode.setShape(Node.NodeShape.RECT);
        evidenceNode.setStatus(Node.NodeStatus.ACTIVE);
        evidenceNode.setProjectId(ownerId);
        evidenceNode.setAreaId(WorkflowEngine.deriveAreaName(decisionNode));
        evidenceNode.setEvidenceStrength(0.5f);
        evidenceNode.setTagsFromString("Decision,Evidence,证据");

        Node nextActionNode = new Node("Next Action", "下一步是直接选，还是先做一个小验证？",
                baseX + 260f, baseY + 540f, Node.NodeType.ACTION);
        nextActionNode.setShape(Node.NodeShape.RECT);
        nextActionNode.setStatus(Node.NodeStatus.PLANNED);
        nextActionNode.setProjectId(ownerId);
        nextActionNode.setAreaId(WorkflowEngine.deriveAreaName(decisionNode));
        nextActionNode.setTriggerCondition("如果完成方案比较，那么先执行这个最小验证动作");
        nextActionNode.setTagsFromString("Decision,NextAction,执行");

        view.addNode(optionA);
        view.addNode(optionB);
        view.addNode(optionC);
        view.addNode(criterionNode);
        view.addNode(riskNode);
        view.addNode(evidenceNode);
        view.addNode(nextActionNode);

        view.addConnection(new Connection(decisionNode.getId(), optionA.getId(), Connection.ConnectionType.LEADS_TO, "候选"));
        view.addConnection(new Connection(decisionNode.getId(), optionB.getId(), Connection.ConnectionType.LEADS_TO, "候选"));
        view.addConnection(new Connection(decisionNode.getId(), optionC.getId(), Connection.ConnectionType.LEADS_TO, "候选"));

        view.addConnection(new Connection(criterionNode.getId(), optionA.getId(), Connection.ConnectionType.SUPPORTS, "按准则评估"));
        view.addConnection(new Connection(criterionNode.getId(), optionB.getId(), Connection.ConnectionType.SUPPORTS, "按准则评估"));
        view.addConnection(new Connection(criterionNode.getId(), optionC.getId(), Connection.ConnectionType.SUPPORTS, "按准则评估"));

        view.addConnection(new Connection(riskNode.getId(), optionA.getId(), Connection.ConnectionType.BLOCKS, "风险审查"));
        view.addConnection(new Connection(riskNode.getId(), optionB.getId(), Connection.ConnectionType.BLOCKS, "风险审查"));
        view.addConnection(new Connection(riskNode.getId(), optionC.getId(), Connection.ConnectionType.BLOCKS, "风险审查"));

        view.addConnection(new Connection(evidenceNode.getId(), decisionNode.getId(), Connection.ConnectionType.EVIDENCE_FOR, "证据"));
        view.addConnection(new Connection(optionA.getId(), nextActionNode.getId(), Connection.ConnectionType.LEADS_TO, "候选"));
        view.addConnection(new Connection(optionB.getId(), nextActionNode.getId(), Connection.ConnectionType.LEADS_TO, "候选"));
        view.addConnection(new Connection(optionC.getId(), nextActionNode.getId(), Connection.ConnectionType.LEADS_TO, "候选"));

        activity.onGraphMutatedByAi();
    }

    public static void generateLearningStarterNodes(Node learningNode, MainActivity activity) {
        if (learningNode == null || activity == null || activity.getMindMapView() == null) return;

        MindMapView view = activity.getMindMapView();

        float baseX = learningNode.getX();
        float baseY = learningNode.getY();
        String ownerId = DashboardSectionBuilder.safe(learningNode.getProjectId());
        if (ownerId.isEmpty()) ownerId = learningNode.getId();
        String title = DashboardSectionBuilder.safe(learningNode.getTitle());
        if (title.isEmpty()) title = "知识点";

        Node retrievalNode = new Node("Retrieval｜" + title,
                "不用看原文，试着回答：它是什么？核心机制/步骤是什么？",
                baseX - 360f, baseY + 180f, Node.NodeType.QUESTION);
        retrievalNode.setShape(Node.NodeShape.OVAL);
        retrievalNode.setStatus(Node.NodeStatus.ACTIVE);
        retrievalNode.setProjectId(ownerId);
        retrievalNode.setAreaId(WorkflowEngine.deriveAreaName(learningNode));
        retrievalNode.setReviewAt("尽快第一次回忆");
        retrievalNode.setTagsFromString("Learning,Retrieval,检索练习");

        Node deepeningNode = new Node("Deepening｜" + title,
                "用你自己的话重述定义，举例，并找一个反例。",
                baseX, baseY + 180f, Node.NodeType.CONCEPT);
        deepeningNode.setShape(Node.NodeShape.HEXAGON);
        deepeningNode.setStatus(Node.NodeStatus.ACTIVE);
        deepeningNode.setProjectId(ownerId);
        deepeningNode.setAreaId(WorkflowEngine.deriveAreaName(learningNode));
        deepeningNode.setTagsFromString("Learning,Deepening,概念深化");

        Node transferNode = new Node("Transfer｜" + title,
                "把它放到一个新场景中，设计一个小应用任务验证迁移能力。",
                baseX + 360f, baseY + 180f, Node.NodeType.EXPERIMENT);
        transferNode.setShape(Node.NodeShape.RECT);
        transferNode.setStatus(Node.NodeStatus.PLANNED);
        transferNode.setProjectId(ownerId);
        transferNode.setAreaId(WorkflowEngine.deriveAreaName(learningNode));
        transferNode.setTriggerCondition("如果我要确认自己真的会了，那么先做这个迁移验证");
        transferNode.setTagsFromString("Learning,Transfer,迁移练习");

        view.addNode(retrievalNode);
        view.addNode(deepeningNode);
        view.addNode(transferNode);

        view.addConnection(new Connection(learningNode.getId(), retrievalNode.getId(), Connection.ConnectionType.LEADS_TO, "主动回忆"));
        view.addConnection(new Connection(learningNode.getId(), deepeningNode.getId(), Connection.ConnectionType.LEADS_TO, "概念深化"));
        view.addConnection(new Connection(learningNode.getId(), transferNode.getId(), Connection.ConnectionType.LEADS_TO, "迁移验证"));
        view.addConnection(new Connection(retrievalNode.getId(), deepeningNode.getId(), Connection.ConnectionType.TRIGGERS, "回忆暴露薄弱点"));
        view.addConnection(new Connection(deepeningNode.getId(), transferNode.getId(), Connection.ConnectionType.TRIGGERS, "理解后迁移"));

        activity.onGraphMutatedByAi();
    }
}
