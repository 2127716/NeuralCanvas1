
package com.agui.neuralcanvas;

import java.util.Map;

public final class BackgroundBrainAnalyzer {

    public static final class BrainPulseReport {
        public String summary = "";
        public String focusNodeId = "";
        public String focusNodeTitle = "";
    }

    public static BrainPulseReport analyze(Map<String, Node> nodes,
                                           Map<String, Connection> connections,
                                           AiConfig config,
                                           BrainAutopilotSettings settings) {

        BrainPulseReport report = new BrainPulseReport();

        // 原有逻辑简化
        report.summary = "AI 已分析图谱";

        // 新增：结构检测
        WorkflowAuditEngine.AuditResult audit =
                WorkflowAuditEngine.audit(nodes, connections);

        GraphHealthEngine.HealthReport health =
                GraphHealthEngine.analyze(nodes, connections);

        report.summary += "\n\n【系统检测】";
        report.summary += "\n网络健康度: " + health.score;

        if (!audit.isHealthy()) {
            report.summary += "\n存在问题:";
            for (String s : audit.issues) {
                report.summary += "\n- " + s;
            }
        }

        return report;
    }
}
