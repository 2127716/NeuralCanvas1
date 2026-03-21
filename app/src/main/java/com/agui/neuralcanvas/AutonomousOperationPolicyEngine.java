package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AutonomousOperationPolicyEngine {

    public static final class PolicyResult {
        public AiResponse autoResponse = new AiResponse();
        public AiResponse confirmResponse = new AiResponse();
        public AiResponse blockedResponse = new AiResponse();
        public boolean hasAuto = false;
        public boolean hasConfirm = false;
        public boolean hasBlocked = false;
        public String summary = "";
        public String riskLevel = "LOW";
        public OperationImpactSummaryEngine.ImpactSummary impactSummary =
                new OperationImpactSummaryEngine.ImpactSummary();
    }

    private AutonomousOperationPolicyEngine() {}

    public static PolicyResult split(AiResponse source) {
        PolicyResult result = new PolicyResult();
        if (source == null) {
            result.summary = "没有可执行命令";
            return result;
        }

        List<AiCommand> autoCommands = new ArrayList<>();
        List<AiCommand> confirmCommands = new ArrayList<>();
        List<AiCommand> blockedCommands = new ArrayList<>();

        result.impactSummary = OperationImpactSummaryEngine.analyze(source);

        if (source.getCommands() != null) {
            for (AiCommand cmd : source.getCommands()) {
                if (cmd == null) continue;
                Decision decision = classify(cmd, result.impactSummary);
                if (decision == Decision.AUTO) autoCommands.add(cmd);
                else if (decision == Decision.CONFIRM) confirmCommands.add(cmd);
                else blockedCommands.add(cmd);
            }
        }

        result.autoResponse.setAnswer(source.getAnswer());
        result.autoResponse.setCommands(autoCommands);
        result.confirmResponse.setAnswer(source.getAnswer());
        result.confirmResponse.setCommands(confirmCommands);
        result.blockedResponse.setAnswer(source.getAnswer());
        result.blockedResponse.setCommands(blockedCommands);

        result.hasAuto = !autoCommands.isEmpty();
        result.hasConfirm = !confirmCommands.isEmpty();
        result.hasBlocked = !blockedCommands.isEmpty();

        if (result.hasBlocked) result.riskLevel = "HIGH";
        else if (result.hasConfirm) result.riskLevel = "MEDIUM";
        else result.riskLevel = "LOW";

        StringBuilder sb = new StringBuilder();
        sb.append("自治执行策略：自动执行 ").append(autoCommands.size())
                .append(" 条，待确认 ").append(confirmCommands.size())
                .append(" 条，拦截 ").append(blockedCommands.size()).append(" 条");
        sb.append("\\n").append(result.impactSummary.buildSummary());
        result.summary = sb.toString();

        return result;
    }

    private enum Decision { AUTO, CONFIRM, BLOCK }

    private static Decision classify(AiCommand cmd, OperationImpactSummaryEngine.ImpactSummary impact) {
        String action = safeLower(cmd.getAction());

        if ("focus_node".equals(action) || "auto_layout".equals(action)) return Decision.AUTO;

        if ("update_node".equals(action)) {
            if (!safe(cmd.getNodeId()).isEmpty() && safe(cmd.getTitle()).isEmpty() && safe(cmd.getContent()).isEmpty()) {
                return Decision.CONFIRM;
            }
            return Decision.AUTO;
        }

        if ("create_connection".equals(action) || "update_connection".equals(action)) {
            if (impact.hasStructuralRefactor) return Decision.CONFIRM;
            return Decision.AUTO;
        }

        if ("create_node".equals(action)) {
            String type = safeLower(cmd.getType());
            if (containsAny(type, "question", "source", "action", "task", "trigger", "review", "insight")) {
                return impact.createNodeCount >= 4 ? Decision.CONFIRM : Decision.AUTO;
            }
            return Decision.CONFIRM;
        }

        if ("delete_connection".equals(action)) return Decision.CONFIRM;
        if ("delete_node".equals(action)) return Decision.BLOCK;

        return Decision.CONFIRM;
    }

    private static boolean containsAny(String text, String... values) {
        for (String v : values) if (text.contains(v)) return true;
        return false;
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
