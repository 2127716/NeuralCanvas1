package com.agui.neuralcanvas;

import java.util.Locale;

public final class AiAgentPromptBuilder {

    public static final class AgentPlan {
        public BrainAgentProfile profile = BrainAgentProfile.GENERAL;
        public String systemPrompt = "";
        public String userPrompt = "";
    }

    private AiAgentPromptBuilder() {}

    public static AgentPlan build(AiGraphSnapshot snapshot, BrainAutopilotSettings settings) {
        AgentPlan plan = new AgentPlan();
        BrainAgentProfile preferred = BrainAgentProfile.fromKey(
                settings == null ? "auto" : settings.getPreferredAutopilotAgent()
        );
        BrainAgentProfile selected = preferred == BrainAgentProfile.AUTO
                ? chooseProfile(snapshot)
                : preferred;

        plan.profile = selected;
        plan.systemPrompt = buildSystemPrompt(selected);
        plan.userPrompt = buildUserPrompt(snapshot, settings, selected);
        return plan;
    }

    public static BrainAgentProfile chooseProfile(AiGraphSnapshot snapshot) {
        if (snapshot == null || snapshot.nodes == null || snapshot.nodes.isEmpty()) {
            return BrainAgentProfile.GENERAL;
        }

        int executionScore = 0;
        int decisionScore = 0;
        int learningScore = 0;
        int networkScore = 0;

        for (AiGraphSnapshot.SnapshotNode node : snapshot.nodes) {
            String type = safeLower(node == null ? "" : node.type);

            if (containsAny(type, "task", "action", "goal", "project", "key_result", "routine", "trigger", "obstacle")) {
                executionScore += 3;
            }
            if (containsAny(type, "decision", "option", "criterion", "assumption", "risk", "evidence")) {
                decisionScore += 3;
            }
            if (containsAny(type, "concept", "question", "resource", "source", "note", "insight", "experiment", "evidence")) {
                learningScore += 3;
            }
            if (containsAny(type, "idea", "note", "concept")) {
                networkScore += 1;
            }

            String title = safeLower(node == null ? "" : node.title);
            String content = safeLower(node == null ? "" : node.content);

            if (containsAny(title + " " + content, "复盘", "review", "执行", "推进", "下一步", "触发")) {
                executionScore += 2;
            }
            if (containsAny(title + " " + content, "证据", "基率", "概率", "方案", "风险", "决策")) {
                decisionScore += 2;
            }
            if (containsAny(title + " " + content, "定义", "例子", "反例", "迁移", "学习", "检索")) {
                learningScore += 2;
            }
        }

        int connectionCount = snapshot.connections == null ? 0 : snapshot.connections.size();
        int nodeCount = snapshot.nodes.size();
        if (nodeCount >= 20 && connectionCount <= Math.max(4, nodeCount / 3)) {
            networkScore += 8;
        }

        if (networkScore >= executionScore && networkScore >= decisionScore && networkScore >= learningScore && networkScore >= 8) {
            return BrainAgentProfile.NETWORK;
        }
        if (decisionScore >= executionScore && decisionScore >= learningScore) {
            return BrainAgentProfile.DECISION;
        }
        if (learningScore >= executionScore) {
            return BrainAgentProfile.LEARNING;
        }
        return BrainAgentProfile.EXECUTION;
    }

    private static String buildSystemPrompt(BrainAgentProfile profile) {
        String base = "你是 NeuralCanvas 的 API 自动代理，是用户的第二大脑。"
                + "输出必须是纯 JSON，格式固定为 {\"answer\":\"...\",\"commands\":[...] }。"
                + "commands 仅允许使用 create_node, update_node, delete_node, create_connection, update_connection, delete_connection, focus_node, auto_layout。"
                + "尽量保守，优先少量高价值命令。"
                + "如果没有必要，不要删除节点。"
                + "answer 必须用中文，简洁说明你发现了什么、你准备怎么改、为什么。";

        switch (profile) {
            case EXECUTION:
                return base
                        + "你现在是执行代理。"
                        + "优先补最小下一步、触发条件、障碍、复盘锚点。"
                        + "你要特别防止空泛规划，动作必须具体、可开始。";
            case DECISION:
                return base
                        + "你现在是决策代理。"
                        + "优先补方案、证据、反证、风险、止损线。"
                        + "你要特别防止高置信低证据。";
            case LEARNING:
                return base
                        + "你现在是学习代理。"
                        + "优先补检索问题、例子、反例、迁移任务、复习安排。"
                        + "你要特别防止只有概念、没有验证。";
            case NETWORK:
                return base
                        + "你现在是网络整理代理。"
                        + "优先修复断裂结构、冗余节点、孤立节点、缺失的高价值连接。"
                        + "除非用户明确要求，否则不要大规模自动布局。";
            default:
                return base + "你现在是通用代理。";
        }
    }

    private static String buildUserPrompt(AiGraphSnapshot snapshot,
                                          BrainAutopilotSettings settings,
                                          BrainAgentProfile profile) {
        int nodeCount = snapshot == null || snapshot.nodes == null ? 0 : snapshot.nodes.size();
        int connectionCount = snapshot == null || snapshot.connections == null ? 0 : snapshot.connections.size();
        String graphJson = AiJsonParser.toJson(snapshot);
        String userGoal = settings == null ? "" : settings.getAutopilotInstruction();

        String profileTask;
        switch (profile) {
            case EXECUTION:
                profileTask = "找出最阻碍推进的 1 个执行问题，优先生成低风险可执行命令。";
                break;
            case DECISION:
                profileTask = "找出最危险的决策缺口，优先补方案/证据/风险相关命令。";
                break;
            case LEARNING:
                profileTask = "找出最影响掌握效果的学习缺口，优先补检索/例子/迁移相关命令。";
                break;
            case NETWORK:
                profileTask = "找出图谱网络中最影响可用性的结构问题，优先补连接、清理弱结构、聚焦关键节点。";
                break;
            default:
                profileTask = "找出当前最值得优先处理的核心问题，并给出少量高价值命令。";
                break;
        }

        return "当前自动巡航模式：" + profile.label
                + "\n自动巡航目标：\n" + safe(userGoal)
                + "\n\n当前图谱：节点 " + nodeCount + " 个，连线 " + connectionCount + " 条。"
                + "\n图谱 JSON：\n" + graphJson
                + "\n\n请执行："
                + "\n1. " + profileTask
                + "\n2. 如果能低风险修复，就直接生成 1~8 条命令"
                + "\n3. 一定生成一个 focus_node，指向最关键节点"
                + "\n4. answer 用中文说明：发现了什么、改了什么、为什么"
                + "\n5. 如果不需要改图，commands 返回空数组，但 answer 仍要给出清晰建议。";
    }

    private static boolean containsAny(String text, String... needles) {
        String src = safeLower(text);
        for (String item : needles) {
            if (src.contains(safeLower(item))) return true;
        }
        return false;
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
