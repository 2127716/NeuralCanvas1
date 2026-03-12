package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScientificTemplateEngine {

    public static class TemplateResult {
        public final List<Node> createdNodes = new ArrayList<>();
        public final List<Connection> createdConnections = new ArrayList<>();
    }

    public static TemplateResult generateWoop(Node baseNode, Map<String, Node> existingNodes) {
        TemplateResult result = new TemplateResult();
        if (baseNode == null) return result;

        float cx = baseNode.getX();
        float cy = baseNode.getY();

        Node wishNode = new Node(
                "Wish｜愿望",
                safeTitle(baseNode) + "：我真正想实现什么？",
                cx - 260f, cy - 220f,
                Node.NodeType.GOAL
        );
        wishNode.setShape(Node.NodeShape.OVAL);
        wishNode.setStatus(Node.NodeStatus.ACTIVE);
        wishNode.setProjectId(baseNode.getId());
        wishNode.setTagsFromString("WOOP,Wish,目标");

        Node outcomeNode = new Node(
                "Outcome｜结果",
                "如果成功，最理想、最具体的结果是什么？",
                cx + 220f, cy - 220f,
                Node.NodeType.KEY_RESULT
        );
        outcomeNode.setShape(Node.NodeShape.OVAL);
        outcomeNode.setStatus(Node.NodeStatus.PLANNED);
        outcomeNode.setProjectId(baseNode.getId());
        outcomeNode.setTagsFromString("WOOP,Outcome,结果");

        Node obstacleNode = new Node(
                "Obstacle｜障碍",
                "我内部或外部最大的障碍是什么？最容易卡住我的点是什么？",
                cx - 260f, cy + 200f,
                Node.NodeType.OBSTACLE
        );
        obstacleNode.setShape(Node.NodeShape.DIAMOND);
        obstacleNode.setStatus(Node.NodeStatus.ACTIVE);
        obstacleNode.setProjectId(baseNode.getId());
        obstacleNode.setTagsFromString("WOOP,Obstacle,障碍");

        Node planNode = new Node(
                "Plan｜计划",
                "如果遇到障碍，我的具体行动是什么？",
                cx + 220f, cy + 200f,
                Node.NodeType.ACTION
        );
        planNode.setShape(Node.NodeShape.RECT);
        planNode.setStatus(Node.NodeStatus.PLANNED);
        planNode.setProjectId(baseNode.getId());
        planNode.setTagsFromString("WOOP,Plan,行动");
        planNode.setTriggerCondition("如果【障碍出现】，那么我立刻执行【最小下一步动作】");

        result.createdNodes.add(wishNode);
        result.createdNodes.add(outcomeNode);
        result.createdNodes.add(obstacleNode);
        result.createdNodes.add(planNode);

        Connection c1 = new Connection(baseNode.getId(), wishNode.getId(), Connection.ConnectionType.LEADS_TO, "目标澄清");
        Connection c2 = new Connection(wishNode.getId(), outcomeNode.getId(), Connection.ConnectionType.LEADS_TO, "期望结果");
        Connection c3 = new Connection(baseNode.getId(), obstacleNode.getId(), Connection.ConnectionType.BLOCKS, "关键障碍");
        Connection c4 = new Connection(obstacleNode.getId(), planNode.getId(), Connection.ConnectionType.TRIGGERS, "遇阻触发");
        Connection c5 = new Connection(planNode.getId(), outcomeNode.getId(), Connection.ConnectionType.SUPPORTS, "执行支持结果");
        Connection c6 = new Connection(planNode.getId(), baseNode.getId(), Connection.ConnectionType.BELONGS_TO, "属于该目标");

        result.createdConnections.add(c1);
        result.createdConnections.add(c2);
        result.createdConnections.add(c3);
        result.createdConnections.add(c4);
        result.createdConnections.add(c5);
        result.createdConnections.add(c6);

        return result;
    }

    public static TemplateResult generateIfThen(Node baseNode, Map<String, Node> existingNodes) {
        TemplateResult result = new TemplateResult();
        if (baseNode == null) return result;

        float cx = baseNode.getX();
        float cy = baseNode.getY();

        Node triggerNode = new Node(
                "If｜触发条件",
                "如果【时间/地点/情境/情绪/前置条件】出现……",
                cx - 240f, cy + 200f,
                Node.NodeType.TRIGGER
        );
        triggerNode.setShape(Node.NodeShape.HEXAGON);
        triggerNode.setStatus(Node.NodeStatus.PLANNED);
        triggerNode.setProjectId(baseNode.getProjectId().isEmpty() ? baseNode.getId() : baseNode.getProjectId());
        triggerNode.setTagsFromString("If-Then,Trigger,执行");

        Node actionNode = new Node(
                "Then｜执行动作",
                "那么我立刻执行【一个非常小、非常具体的动作】",
                cx + 220f, cy + 200f,
                Node.NodeType.ACTION
        );
        actionNode.setShape(Node.NodeShape.RECT);
        actionNode.setStatus(Node.NodeStatus.PLANNED);
        actionNode.setProjectId(baseNode.getProjectId().isEmpty() ? baseNode.getId() : baseNode.getProjectId());
        actionNode.setTagsFromString("If-Then,Action,执行");
        actionNode.setTriggerCondition("如果【触发条件】发生，那么我立刻【执行动作】");

        if (baseNode.getType() == Node.NodeType.TASK || baseNode.getType() == Node.NodeType.ACTION) {
            actionNode.setContent("那么我立刻执行：" + safeTitle(baseNode));
        }

        Node obstacleNode = new Node(
                "常见阻碍",
                "最可能让我不执行的干扰是什么？我如何提前削弱它？",
                cx, cy + 360f,
                Node.NodeType.OBSTACLE
        );
        obstacleNode.setShape(Node.NodeShape.DIAMOND);
        obstacleNode.setStatus(Node.NodeStatus.PLANNED);
        obstacleNode.setProjectId(baseNode.getProjectId().isEmpty() ? baseNode.getId() : baseNode.getProjectId());
        obstacleNode.setTagsFromString("If-Then,Obstacle,执行");

        result.createdNodes.add(triggerNode);
        result.createdNodes.add(actionNode);
        result.createdNodes.add(obstacleNode);

        Connection c1 = new Connection(triggerNode.getId(), actionNode.getId(), Connection.ConnectionType.TRIGGERS, "如果…那么…");
        Connection c2 = new Connection(obstacleNode.getId(), actionNode.getId(), Connection.ConnectionType.BLOCKS, "可能阻碍");
        Connection c3 = new Connection(actionNode.getId(), baseNode.getId(), Connection.ConnectionType.SUPPORTS, "推进原节点");
        Connection c4 = new Connection(triggerNode.getId(), baseNode.getId(), Connection.ConnectionType.BELONGS_TO, "服务于原节点");

        result.createdConnections.add(c1);
        result.createdConnections.add(c2);
        result.createdConnections.add(c3);
        result.createdConnections.add(c4);

        return result;
    }

    public static TemplateResult generateDailyReview(Node baseNode, Map<String, Node> existingNodes) {
        TemplateResult result = new TemplateResult();
        if (baseNode == null) return result;

        float cx = baseNode.getX();
        float cy = baseNode.getY();
        String ownerId = resolveOwnerId(baseNode);

        Node summaryNode = new Node(
                "今日推进",
                "今天我实际推进了什么？有没有哪一步真正前进？",
                cx - 280f, cy - 180f,
                Node.NodeType.REVIEW
        );
        summaryNode.setShape(Node.NodeShape.OVAL);
        summaryNode.setStatus(Node.NodeStatus.REVIEW);
        summaryNode.setProjectId(ownerId);
        summaryNode.setTagsFromString("每日复盘,推进,Review");

        Node blockerNode = new Node(
                "今日卡点",
                "我卡在哪？是注意力、时间、信息不足、情绪波动，还是任务过大？",
                cx + 220f, cy - 180f,
                Node.NodeType.OBSTACLE
        );
        blockerNode.setShape(Node.NodeShape.DIAMOND);
        blockerNode.setStatus(Node.NodeStatus.REVIEW);
        blockerNode.setProjectId(ownerId);
        blockerNode.setTagsFromString("每日复盘,卡点,Obstacle");

        Node lessonNode = new Node(
                "今日经验",
                "今天最值得保留或修正的一条经验是什么？",
                cx - 280f, cy + 180f,
                Node.NodeType.INSIGHT
        );
        lessonNode.setShape(Node.NodeShape.HEXAGON);
        lessonNode.setStatus(Node.NodeStatus.REVIEW);
        lessonNode.setProjectId(ownerId);
        lessonNode.setTagsFromString("每日复盘,经验,Insight");

        Node nextNode = new Node(
                "明日最小下一步",
                "明天最关键、最小、最明确的一步是什么？",
                cx + 220f, cy + 180f,
                Node.NodeType.ACTION
        );
        nextNode.setShape(Node.NodeShape.RECT);
        nextNode.setStatus(Node.NodeStatus.PLANNED);
        nextNode.setProjectId(ownerId);
        nextNode.setTagsFromString("每日复盘,下一步,Action");
        nextNode.setTriggerCondition("如果明天开始工作，那么我先执行这个最小下一步");

        result.createdNodes.add(summaryNode);
        result.createdNodes.add(blockerNode);
        result.createdNodes.add(lessonNode);
        result.createdNodes.add(nextNode);

        result.createdConnections.add(new Connection(baseNode.getId(), summaryNode.getId(), Connection.ConnectionType.LEADS_TO, "今日推进"));
        result.createdConnections.add(new Connection(baseNode.getId(), blockerNode.getId(), Connection.ConnectionType.BLOCKS, "今日卡点"));
        result.createdConnections.add(new Connection(summaryNode.getId(), lessonNode.getId(), Connection.ConnectionType.LEADS_TO, "提炼经验"));
        result.createdConnections.add(new Connection(lessonNode.getId(), nextNode.getId(), Connection.ConnectionType.TRIGGERS, "经验转行动"));
        result.createdConnections.add(new Connection(nextNode.getId(), baseNode.getId(), Connection.ConnectionType.SUPPORTS, "推动原节点"));

        return result;
    }

    public static TemplateResult generateWeeklyReview(Node baseNode, Map<String, Node> existingNodes) {
        TemplateResult result = new TemplateResult();
        if (baseNode == null) return result;

        float cx = baseNode.getX();
        float cy = baseNode.getY();
        String ownerId = resolveOwnerId(baseNode);

        Node progressNode = new Node(
                "本周进展",
                "本周有哪些真正的推进？哪些目标/KR有量化变化？",
                cx - 320f, cy - 220f,
                Node.NodeType.REVIEW
        );
        progressNode.setShape(Node.NodeShape.OVAL);
        progressNode.setStatus(Node.NodeStatus.REVIEW);
        progressNode.setProjectId(ownerId);
        progressNode.setTagsFromString("每周复盘,进展,Review");

        Node patternNode = new Node(
                "重复模式",
                "这周反复出现的好模式/坏模式是什么？",
                cx + 260f, cy - 220f,
                Node.NodeType.INSIGHT
        );
        patternNode.setShape(Node.NodeShape.HEXAGON);
        patternNode.setStatus(Node.NodeStatus.REVIEW);
        patternNode.setProjectId(ownerId);
        patternNode.setTagsFromString("每周复盘,模式,Insight");

        Node blockerNode = new Node(
                "系统性阻碍",
                "真正拖慢我的系统性因素是什么？例如计划过大、环境分心、估时错误。",
                cx - 320f, cy + 200f,
                Node.NodeType.OBSTACLE
        );
        blockerNode.setShape(Node.NodeShape.DIAMOND);
        blockerNode.setStatus(Node.NodeStatus.REVIEW);
        blockerNode.setProjectId(ownerId);
        blockerNode.setTagsFromString("每周复盘,阻碍,Obstacle");

        Node adjustNode = new Node(
                "下周调整",
                "下周要删掉什么、保留什么、强化什么？",
                cx + 260f, cy + 200f,
                Node.NodeType.ACTION
        );
        adjustNode.setShape(Node.NodeShape.RECT);
        adjustNode.setStatus(Node.NodeStatus.PLANNED);
        adjustNode.setProjectId(ownerId);
        adjustNode.setTagsFromString("每周复盘,调整,Action");
        adjustNode.setTriggerCondition("如果下周开始规划，那么我先执行这里的调整动作");

        result.createdNodes.add(progressNode);
        result.createdNodes.add(patternNode);
        result.createdNodes.add(blockerNode);
        result.createdNodes.add(adjustNode);

        result.createdConnections.add(new Connection(baseNode.getId(), progressNode.getId(), Connection.ConnectionType.LEADS_TO, "周进展"));
        result.createdConnections.add(new Connection(progressNode.getId(), patternNode.getId(), Connection.ConnectionType.LEADS_TO, "归纳模式"));
        result.createdConnections.add(new Connection(baseNode.getId(), blockerNode.getId(), Connection.ConnectionType.BLOCKS, "系统阻碍"));
        result.createdConnections.add(new Connection(patternNode.getId(), adjustNode.getId(), Connection.ConnectionType.TRIGGERS, "模式指导调整"));
        result.createdConnections.add(new Connection(blockerNode.getId(), adjustNode.getId(), Connection.ConnectionType.TRIGGERS, "阻碍倒逼调整"));
        result.createdConnections.add(new Connection(adjustNode.getId(), baseNode.getId(), Connection.ConnectionType.SUPPORTS, "推动下周"));

        return result;
    }

    public static TemplateResult generateAarReview(Node baseNode, Map<String, Node> existingNodes) {
        TemplateResult result = new TemplateResult();
        if (baseNode == null) return result;

        float cx = baseNode.getX();
        float cy = baseNode.getY();
        String ownerId = resolveOwnerId(baseNode);

        Node expectedNode = new Node(
                "AAR｜预期",
                "原本预期会发生什么？目标、时间、质量标准是什么？",
                cx - 320f, cy - 220f,
                Node.NodeType.REVIEW
        );
        expectedNode.setShape(Node.NodeShape.OVAL);
        expectedNode.setStatus(Node.NodeStatus.REVIEW);
        expectedNode.setProjectId(ownerId);
        expectedNode.setTagsFromString("AAR,预期,Review");

        Node actualNode = new Node(
                "AAR｜实际",
                "实际发生了什么？结果如何？和预期差在哪里？",
                cx + 260f, cy - 220f,
                Node.NodeType.REVIEW
        );
        actualNode.setShape(Node.NodeShape.OVAL);
        actualNode.setStatus(Node.NodeStatus.REVIEW);
        actualNode.setProjectId(ownerId);
        actualNode.setTagsFromString("AAR,实际,Review");

        Node reasonNode = new Node(
                "AAR｜原因",
                "为什么会有差异？是计划、执行、环境、资源、判断还是沟通问题？",
                cx - 320f, cy + 200f,
                Node.NodeType.OBSTACLE
        );
        reasonNode.setShape(Node.NodeShape.DIAMOND);
        reasonNode.setStatus(Node.NodeStatus.REVIEW);
        reasonNode.setProjectId(ownerId);
        reasonNode.setTagsFromString("AAR,原因,Obstacle");

        Node improveNode = new Node(
                "AAR｜改进",
                "下次我具体要怎么做得更好？哪些动作应该标准化？",
                cx + 260f, cy + 200f,
                Node.NodeType.ACTION
        );
        improveNode.setShape(Node.NodeShape.RECT);
        improveNode.setStatus(Node.NodeStatus.PLANNED);
        improveNode.setProjectId(ownerId);
        improveNode.setTagsFromString("AAR,改进,Action");
        improveNode.setTriggerCondition("如果下次遇到相似任务，那么我优先执行这些改进行动");

        result.createdNodes.add(expectedNode);
        result.createdNodes.add(actualNode);
        result.createdNodes.add(reasonNode);
        result.createdNodes.add(improveNode);

        result.createdConnections.add(new Connection(baseNode.getId(), expectedNode.getId(), Connection.ConnectionType.LEADS_TO, "原预期"));
        result.createdConnections.add(new Connection(baseNode.getId(), actualNode.getId(), Connection.ConnectionType.LEADS_TO, "实际结果"));
        result.createdConnections.add(new Connection(expectedNode.getId(), reasonNode.getId(), Connection.ConnectionType.LEADS_TO, "找偏差来源"));
        result.createdConnections.add(new Connection(actualNode.getId(), reasonNode.getId(), Connection.ConnectionType.EVIDENCE_FOR, "实际证据"));
        result.createdConnections.add(new Connection(reasonNode.getId(), improveNode.getId(), Connection.ConnectionType.TRIGGERS, "原因导出改进"));
        result.createdConnections.add(new Connection(improveNode.getId(), baseNode.getId(), Connection.ConnectionType.SUPPORTS, "提升下一轮"));

        return result;
    }

    private static String resolveOwnerId(Node baseNode) {
        if (baseNode == null) return "";
        if (baseNode.getProjectId() != null && !baseNode.getProjectId().trim().isEmpty()) {
            return baseNode.getProjectId().trim();
        }
        return baseNode.getId();
    }

    private static String safeTitle(Node node) {
        String title = node == null ? "" : node.getTitle();
        if (title == null || title.trim().isEmpty()) {
            return "当前目标";
        }
        return title.trim();
    }
}
