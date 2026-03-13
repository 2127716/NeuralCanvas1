package com.agui.neuralcanvas;

public final class AiScientificPrompts {

    private AiScientificPrompts() {}

    public static String gapCheck(Node node) {
        String title = safeTitle(node);
        return "请作为科学方法教练，检查当前节点及其相邻子图的结构缺口。重点检查：目标是否清晰、是否有下一步动作、是否有障碍、是否有正反证据、是否有复盘锚点、是否有检索或迁移、是否有估时或触发器。请优先输出保守的 commands，补最关键的缺口，不要胡乱重排。当前焦点节点：" + title;
    }

    public static String executionCoach(Node node) {
        String title = safeTitle(node);
        return "请把当前节点升级成真正可执行的行动链。要求：补充一个最小下一步、一个 If-Then 触发器、一个关键阻碍、一个预防动作、一个复盘锚点；若估时缺失，请补一个保守估时；必要时补一个参考类预测节点。优先输出 commands。当前焦点节点：" + title;
    }

    public static String learningCoach(Node node) {
        String title = safeTitle(node);
        return "请把当前学习节点升级成高质量学习子图。要求：补检索练习、反例/边界、迁移应用、易错点、自测问题；若结构缺失，也补来源或证据节点。优先输出 commands，不要无意义扩写。当前焦点节点：" + title;
    }

    public static String decisionCoach(Node node) {
        String title = safeTitle(node);
        return "请把当前决策节点升级成科学决策子图。要求：至少检查候选方案、准则、正反证据、Premortem 风险、WRAP 护栏、参考类预测。若用户明显确认偏误，请补一个红队/反证节点。优先输出 commands，动作要保守且有用。当前焦点节点：" + title;
    }

    public static String redTeam(Node node) {
        String title = safeTitle(node);
        return "请切换到红队视角，攻击当前节点及其相邻结构。找出最可能的盲点、反例、脆弱假设、被忽略的替代方案和失败路径。优先输出 commands，新增少量高价值反证/风险/替代方案节点，并用明确关系连接。当前焦点节点：" + title;
    }


    public static String triage(Node node) {
        return "请作为科学工作流教练，先对当前节点做体检：从目标清晰度、执行性、证据质量、复盘闭环、学习检索、决策稳健性几个角度，找出最关键的 3 个缺口。然后只给最值得先做的 1~2 个改图动作，输出能直接落图的 commands，不要泛泛建议。\n\n节点：" + compactNode(node);
    }

    public static String workflowRecommendation(Node node) {
        String title = safeTitle(node);
        return "请基于当前节点类型和相邻结构，给出最有杠杆的下一步工作流建议。不要泛泛而谈，优先指出：现在最该补什么、最该删什么、最该先执行什么。若适合，直接输出少量 commands。当前焦点节点：" + title;
    }

    public static String autopilot(Node node) {
        String title = safeTitle(node);
        return "请按科学方法自动补强当前节点，但必须克制。先检查结构缺口，再只补最必要的 3 到 8 个节点和对应连线。优先顺序：执行闭环、证据闭环、学习闭环、复盘闭环。不要做大规模排版，不要无意义重复。当前焦点节点：" + title;
    }

    private static String safeTitle(Node node) {
        if (node == null) return "未命名节点";
        String title = node.getTitle();
        return title == null || title.trim().isEmpty() ? "未命名节点" : title.trim();
    }

    private static String compactNode(Node node) {
        if (node == null) return "(空节点)";
        StringBuilder sb = new StringBuilder();
        sb.append("标题=").append(safeTitle(node));
        sb.append("；类型=").append(node.getType() == null ? "未知" : node.getType().name());
        if (node.getStatus() != null) sb.append("；状态=").append(node.getStatus().name());
        if (node.getPriority() > 0) sb.append("；优先级=").append(node.getPriority());
        if (node.getEffortEstimate() > 0f) sb.append("；估时=").append(node.getEffortEstimate()).append("h");
        if (node.getConfidence() > 0f) sb.append("；信心=").append(Math.round(node.getConfidence() * 100f)).append("%");
        String content = node.getContent();
        if (content != null && !content.trim().isEmpty()) {
            content = content.trim();
            if (content.length() > 90) content = content.substring(0, 90) + "...";
            sb.append("；内容=").append(content);
        }
        return sb.toString();
    }
}
