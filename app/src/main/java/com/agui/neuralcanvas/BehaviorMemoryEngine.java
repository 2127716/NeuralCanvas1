package com.agui.neuralcanvas;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BehaviorMemoryEngine {

    public static final class PulseRecord {
        public String agentProfile = "";
        public String riskLevel = "";
        public String focusNodeId = "";
        public boolean autoApplied = false;
        public int removedCommands = 0;
        public List<String> auditIssues;
        public List<AiCommand> commands;
    }

    private BehaviorMemoryEngine() {}

    public static void record(SimpleDataManager dataManager, PulseRecord record) {
        if (dataManager == null || record == null) return;

        BehaviorMemoryProfile profile = dataManager.loadBehaviorMemoryProfile();
        if (profile == null) profile = new BehaviorMemoryProfile();

        profile.totalPulses++;
        profile.lastUpdatedAt = System.currentTimeMillis();

        String agent = safeLower(record.agentProfile);
        bump(profile.agentRunCount, agent.isEmpty() ? "auto" : agent);

        if (record.autoApplied) {
            profile.autoAppliedCount++;
        }

        if ("low".equals(safeLower(record.riskLevel)) && record.autoApplied) {
            profile.lowRiskAcceptedCount++;
        } else if ("medium".equals(safeLower(record.riskLevel)) || "high".equals(safeLower(record.riskLevel))) {
            profile.mediumOrHighRiskBlockedCount++;
        }

        profile.selfReviewRemovedCommands += Math.max(0, record.removedCommands);

        if (!safe(record.focusNodeId).isEmpty()) {
            bump(profile.focusNodeHitCount, safe(record.focusNodeId));
        }

        if (record.auditIssues != null) {
            for (String issue : record.auditIssues) {
                String normalized = normalizeIssue(issue);
                if (!normalized.isEmpty()) bump(profile.nodeIssueCount, normalized);
            }
        }

        if (record.commands != null) {
            for (AiCommand cmd : record.commands) {
                if (cmd == null) continue;
                String action = safeLower(cmd.getAction());
                if (!action.isEmpty()) bump(profile.actionTypeCount, action);
            }
        }

        dataManager.saveBehaviorMemoryProfile(profile);
    }

    public static String buildSummary(BehaviorMemoryProfile profile) {
        if (profile == null || profile.totalPulses <= 0) {
            return "暂无长期行为记忆。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("长期记忆：已记录 ").append(profile.totalPulses).append(" 次自动巡航");
        sb.append("，自动执行低风险改动 ").append(profile.autoAppliedCount).append(" 次");
        if (profile.selfReviewRemovedCommands > 0) {
            sb.append("，自我复审已拦截 ").append(profile.selfReviewRemovedCommands).append(" 条低价值命令");
        }

        String topIssue = topKey(profile.nodeIssueCount);
        if (!topIssue.isEmpty()) {
            sb.append("。近期最常见结构问题：").append(topIssue);
        }

        String topAction = topKey(profile.actionTypeCount);
        if (!topAction.isEmpty()) {
            sb.append("。系统最常做的动作：").append(topAction);
        }

        return sb.toString();
    }

    private static void bump(Map<String, Integer> map, String key) {
        if (map == null || key == null || key.trim().isEmpty()) return;
        Integer old = map.get(key);
        map.put(key, old == null ? 1 : old + 1);
    }

    private static String normalizeIssue(String issue) {
        String s = safe(issue);
        if (s.isEmpty()) return "";
        int idx = s.indexOf('：');
        if (idx >= 0 && idx + 1 < s.length()) {
            return s.substring(0, idx).trim();
        }
        idx = s.indexOf(':');
        if (idx >= 0 && idx + 1 < s.length()) {
            return s.substring(0, idx).trim();
        }
        return s.length() > 18 ? s.substring(0, 18) : s;
    }

    private static String topKey(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) return "";
        String bestKey = "";
        int bestCount = Integer.MIN_VALUE;
        for (Map.Entry<String, Integer> item : map.entrySet()) {
            int count = item.getValue() == null ? 0 : item.getValue();
            if (count > bestCount) {
                bestCount = count;
                bestKey = safe(item.getKey());
            }
        }
        return bestKey;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safeLower(String value) {
        return safe(value).toLowerCase(Locale.ROOT);
    }
}
