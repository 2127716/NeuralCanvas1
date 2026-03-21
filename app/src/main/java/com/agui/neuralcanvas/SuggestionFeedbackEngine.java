package com.agui.neuralcanvas;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SuggestionFeedbackEngine {

    private SuggestionFeedbackEngine() {}

    public static void recordAccepted(SimpleDataManager dataManager, AiResponse response, String agentKey) {
        if (dataManager == null || response == null) return;
        SuggestionFeedbackProfile profile = dataManager.loadSuggestionFeedbackProfile();
        profile.acceptedCount++;
        profile.lastUpdatedAt = System.currentTimeMillis();
        bump(profile.agentAcceptedCount, safeLower(agentKey));
        bumpActions(profile.acceptedActionTypeCount, response.getCommands());
        dataManager.saveSuggestionFeedbackProfile(profile);
    }

    public static void recordRejected(SimpleDataManager dataManager, AiResponse response, String agentKey) {
        if (dataManager == null || response == null) return;
        SuggestionFeedbackProfile profile = dataManager.loadSuggestionFeedbackProfile();
        profile.rejectedCount++;
        profile.lastUpdatedAt = System.currentTimeMillis();
        bump(profile.agentRejectedCount, safeLower(agentKey));
        bumpActions(profile.rejectedActionTypeCount, response.getCommands());
        dataManager.saveSuggestionFeedbackProfile(profile);
    }

    public static void recordAutoApplied(SimpleDataManager dataManager, AiResponse response, String agentKey) {
        if (dataManager == null || response == null) return;
        SuggestionFeedbackProfile profile = dataManager.loadSuggestionFeedbackProfile();
        profile.autoAppliedCount++;
        profile.lastUpdatedAt = System.currentTimeMillis();
        bump(profile.agentAcceptedCount, safeLower(agentKey));
        bumpActions(profile.acceptedActionTypeCount, response.getCommands());
        dataManager.saveSuggestionFeedbackProfile(profile);
    }

    public static void recordEffectiveness(SimpleDataManager dataManager, AiResponse response) {
        if (dataManager == null || response == null) return;
        SuggestionFeedbackProfile profile = dataManager.loadSuggestionFeedbackProfile();
        profile.effectiveCount++;
        profile.lastUpdatedAt = System.currentTimeMillis();
        bumpActions(profile.effectiveActionTypeCount, response.getCommands());
        dataManager.saveSuggestionFeedbackProfile(profile);
    }

    public static float getAgentWeight(SuggestionFeedbackProfile profile, String agentKey) {
        if (profile == null) return 1.0f;
        String key = safeLower(agentKey);
        int accepted = get(profile.agentAcceptedCount, key) + get(profile.acceptedActionTypeCount, key);
        int rejected = get(profile.agentRejectedCount, key);
        int total = accepted + rejected;
        if (total <= 0) return 1.0f;
        float rate = (float) accepted / Math.max(1, total);
        return 0.75f + rate;
    }

    public static String buildSummary(SuggestionFeedbackProfile profile) {
        if (profile == null) return "暂无建议采纳反馈。";
        int total = profile.acceptedCount + profile.rejectedCount + profile.autoAppliedCount;
        if (total <= 0) return "暂无建议采纳反馈。";

        StringBuilder sb = new StringBuilder();
        sb.append("建议反馈：接受 ").append(profile.acceptedCount)
                .append(" 次，拒绝 ").append(profile.rejectedCount)
                .append(" 次，自动执行 ").append(profile.autoAppliedCount)
                .append(" 次，有效落地 ").append(profile.effectiveCount).append(" 次");

        String bestAgent = topKey(profile.agentAcceptedCount);
        if (!bestAgent.isEmpty()) sb.append("。当前更稳定代理：").append(bestAgent);

        String bestAction = topKey(profile.effectiveActionTypeCount);
        if (!bestAction.isEmpty()) sb.append("。长期更有效动作：").append(bestAction);

        return sb.toString();
    }

    private static void bumpActions(Map<String, Integer> map, List<AiCommand> commands) {
        if (map == null || commands == null) return;
        for (AiCommand cmd : commands) {
            if (cmd == null) continue;
            bump(map, safeLower(cmd.getAction()));
        }
    }

    private static void bump(Map<String, Integer> map, String key) {
        if (map == null || key == null || key.trim().isEmpty()) return;
        Integer old = map.get(key);
        map.put(key, old == null ? 1 : old + 1);
    }

    private static int get(Map<String, Integer> map, String key) {
        if (map == null || key == null) return 0;
        Integer value = map.get(key);
        return value == null ? 0 : value;
    }

    private static String topKey(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) return "";
        String best = "";
        int count = Integer.MIN_VALUE;
        for (Map.Entry<String, Integer> item : map.entrySet()) {
            int v = item.getValue() == null ? 0 : item.getValue();
            if (v > count) {
                count = v;
                best = item.getKey();
            }
        }
        return best;
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
