package com.agui.neuralcanvas;

import java.util.Map;

public final class BackgroundBrainAnalyzer {
    public static final class BrainPulseReport {
        public String summary = "";
        public String focusNodeId = "";
        public String focusNodeTitle = "";
        public String reason = "";
        public String suggestedMode = "execution";
        public int severity = 0;
        public boolean shouldNotify = false;
        public boolean autoApplied = false;
        public String responseJson = "";
        public String riskLevel = "LOW";
    }

    private BackgroundBrainAnalyzer() {}

    public static BrainPulseReport analyze(Map<String, Node> nodes,
                                           Map<String, Connection> connections,
                                           AiConfig config,
                                           BrainAutopilotSettings settings) throws Exception {
        BrainPulseReport report = new BrainPulseReport();
        if (settings == null || !settings.isEnabled() || !settings.isApiAutopilotEnabled()) {
            report.summary = "AI自动巡航已关闭";
            return report;
        }
        if (config == null || !config.isEnabled()) {
            report.summary = "AI配置不完整，无法执行 API 自动巡航";
            return report;
        }

        AiGraphSnapshot snapshot = AiGraphSnapshot.from(nodes, connections);
        AiResponse response = new AiAutopilotApi().runAutopilot(config, snapshot, settings);
        report.responseJson = AiJsonParser.toJson(response);
        report.summary = response == null ? "AI没有返回结果" : safe(response.getAnswer());

        AiAutopilotSafetyEngine.SafetyReport safety = AiAutopilotSafetyEngine.analyze(response);
        report.riskLevel = safety.riskLevel.name();
        report.reason = safety.buildSummary();
        report.severity = safety.riskLevel == AiAutopilotSafetyEngine.RiskLevel.HIGH ? 95
                : safety.riskLevel == AiAutopilotSafetyEngine.RiskLevel.MEDIUM ? 78 : 64;
        report.shouldNotify = true;

        if (response != null && response.getCommands() != null) {
            for (AiCommand cmd : response.getCommands()) {
                if (cmd != null && "focus_node".equalsIgnoreCase(cmd.getAction())) {
                    report.focusNodeId = safe(cmd.getNodeId());
                    break;
                }
            }
            if (report.focusNodeId.isEmpty()) {
                for (AiCommand cmd : response.getCommands()) {
                    if (cmd == null) continue;
                    if (!safe(cmd.getNodeId()).isEmpty()) {
                        report.focusNodeId = safe(cmd.getNodeId());
                        break;
                    }
                    if (!safe(cmd.getFromNodeId()).isEmpty()) {
                        report.focusNodeId = safe(cmd.getFromNodeId());
                        break;
                    }
                }
            }
        }

        Node focusNode = nodes == null ? null : nodes.get(report.focusNodeId);
        report.focusNodeTitle = focusNode == null ? "" : safe(focusNode.getTitle());
        report.suggestedMode = inferMode(focusNode, response);
        if (report.summary.isEmpty()) report.summary = "AI 已完成一次自动巡航";
        return report;
    }

    private static String inferMode(Node focusNode, AiResponse response) {
        if (focusNode != null) {
            if (focusNode.isLearningNode()) return "learning";
            if (focusNode.isDecisionNode()) return "decision";
            return "execution";
        }
        if (response != null && response.getAnswer().contains("学习")) return "learning";
        if (response != null && response.getAnswer().contains("决策")) return "decision";
        return "execution";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
