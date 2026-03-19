package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.List;

public final class AiAutopilotSafetyEngine {
    public enum RiskLevel { LOW, MEDIUM, HIGH }
    public static final class SafetyReport {
        public RiskLevel riskLevel = RiskLevel.LOW;
        public final List<String> reasons = new ArrayList<>();
        public String buildSummary() { StringBuilder sb = new StringBuilder(); sb.append("风险等级：").append(riskLevel.name()); for (String reason : reasons) sb.append("；").append(reason); return sb.toString(); }
    }
    private AiAutopilotSafetyEngine() {}
    public static SafetyReport analyze(AiResponse response) {
        SafetyReport report = new SafetyReport();
        if (response == null || response.getCommands() == null || response.getCommands().isEmpty()) { report.reasons.add("无改图命令"); return report; }
        int createCount = 0, updateCount = 0, deleteCount = 0, layoutCount = 0;
        for (AiCommand cmd : response.getCommands()) {
            if (cmd == null) continue;
            String action = safeLower(cmd.getAction());
            if ("create_node".equals(action) || "create_connection".equals(action) || "focus_node".equals(action)) createCount++;
            else if ("update_node".equals(action) || "update_connection".equals(action)) updateCount++;
            else if ("delete_node".equals(action) || "delete_connection".equals(action)) deleteCount++;
            else if ("auto_layout".equals(action)) layoutCount++;
        }
        if (deleteCount > 0) { report.riskLevel = RiskLevel.HIGH; report.reasons.add("包含删除操作"); }
        if (updateCount >= 6 || createCount >= 12) { report.riskLevel = RiskLevel.HIGH; report.reasons.add("一次性改动过多"); }
        if (report.riskLevel != RiskLevel.HIGH && (updateCount > 0 || layoutCount > 0)) {
            report.riskLevel = RiskLevel.MEDIUM;
            if (updateCount > 0) report.reasons.add("包含修改已有节点/连线");
            if (layoutCount > 0) report.reasons.add("包含自动布局");
        }
        if (report.reasons.isEmpty()) report.reasons.add("以新增结构和聚焦为主");
        return report;
    }
    private static String safeLower(String text) { return text == null ? "" : text.trim().toLowerCase(); }
}
