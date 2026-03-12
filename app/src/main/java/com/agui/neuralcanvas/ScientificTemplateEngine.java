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

        // 默认给 plan 一个触发条件模板，后续你可以在编辑框里改
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

        // 如果基础节点本身就是任务/行动，就让 Then 更贴近它
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

    private static String safeTitle(Node node) {
        String title = node == null ? "" : node.getTitle();
        if (title == null || title.trim().isEmpty()) {
            return "当前目标";
        }
        return title.trim();
    }
}
