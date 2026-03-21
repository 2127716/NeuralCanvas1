package com.agui.neuralcanvas;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AgentRunHistoryEngine {

    private AgentRunHistoryEngine() {}

    public static void record(SimpleDataManager dataManager,
                              List<AiAgentRunResult> runs,
                              List<AiAgentRunResult> keptRuns,
                              boolean effective) {
        if (dataManager == null || runs == null) return;
        AgentRunHistoryProfile profile = dataManager.loadAgentRunHistoryProfile();
        profile.totalRuns++;
        profile.lastUpdatedAt = System.currentTimeMillis();

        for (AiAgentRunResult run : runs) {
            if (run == null || run.profile == null) continue;
            String key = safeLower(run.profile.key);
            bump(profile.runCountByAgent, key);
            bump(profile.avgCommandBucketsByAgent, key + ":" + bucket(run.commandCount));
        }

        if (keptRuns != null) {
            for (AiAgentRunResult run : keptRuns) {
                if (run == null || run.profile == null) continue;
                String key = safeLower(run.profile.key);
                bump(profile.keptCountByAgent, key);
                if (effective) bump(profile.effectiveCountByAgent, key);
            }
        }

        if (keptRuns != null) {
            for (AiAgentRunResult run : runs) {
                if (run == null || run.profile == null) continue;
                if (!containsProfile(keptRuns, run.profile)) {
                    bump(profile.droppedCountByAgent, safeLower(run.profile.key));
                }
            }
        }

        dataManager.saveAgentRunHistoryProfile(profile);
    }

    public static float getAgentHistoryWeight(AgentRunHistoryProfile profile, String agentKey) {
        if (profile == null) return 1.0f;
        String key = safeLower(agentKey);
        int runs = get(profile.runCountByAgent, key);
        int kept = get(profile.keptCountByAgent, key);
        int effective = get(profile.effectiveCountByAgent, key);
        if (runs <= 0) return 1.0f;
        float keepRate = (float) kept / Math.max(1, runs);
        float effRate = (float) effective / Math.max(1, runs);
        return 0.8f + keepRate * 0.5f + effRate * 0.7f;
    }

    public static String buildSummary(AgentRunHistoryProfile profile) {
        if (profile == null || profile.totalRuns <= 0) return "暂无代理历史画像。";
        String top = topKey(profile.effectiveCountByAgent);
        StringBuilder sb = new StringBuilder();
        sb.append("代理历史：累计 ").append(profile.totalRuns).append(" 次编排");
        if (!top.isEmpty()) sb.append("，长期表现更好的代理：").append(top);
        return sb.toString();
    }

    private static boolean containsProfile(List<AiAgentRunResult> runs, BrainAgentProfile profile) {
        if (runs == null || profile == null) return false;
        for (AiAgentRunResult run : runs) {
            if (run != null && run.profile == profile) return true;
        }
        return false;
    }

    private static String bucket(int count) {
        if (count <= 2) return "0_2";
        if (count <= 5) return "3_5";
        if (count <= 8) return "6_8";
        return "9_plus";
    }

    private static void bump(Map<String, Integer> map, String key) {
        if (map == null || key == null || key.trim().isEmpty()) return;
        Integer old = map.get(key);
        map.put(key, old == null ? 1 : old + 1);
    }

    private static int get(Map<String, Integer> map, String key) {
        if (map == null || key == null) return 0;
        Integer v = map.get(key);
        return v == null ? 0 : v;
    }

    private static String topKey(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) return "";
        String best = "";
        int bestCount = Integer.MIN_VALUE;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            int c = e.getValue() == null ? 0 : e.getValue();
            if (c > bestCount) {
                best = e.getKey();
                bestCount = c;
            }
        }
        return best;
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
