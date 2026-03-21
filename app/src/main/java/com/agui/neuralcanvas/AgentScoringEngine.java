package com.agui.neuralcanvas;

import java.util.*;

public final class AgentScoringEngine {

    public static final class AgentPlan {
        public final List<BrainAgentProfile> orderedProfiles = new ArrayList<>();
        public int maxAgents = 3;
        public int maxCommandsPerRun = 8;
        public int totalCommandBudget = 14;
        public String reason = "";
    }

    private AgentScoringEngine() {}

    public static AgentPlan buildPlan(AiGraphSnapshot snapshot, BrainAutopilotSettings settings, SuggestionFeedbackProfile feedback) {
        AgentPlan plan = new AgentPlan();
        if (settings == null) settings = new BrainAutopilotSettings();

        int executionSignals = 0, decisionSignals = 0, learningSignals = 0, networkSignals = 0;

        if (snapshot != null && snapshot.nodes != null) {
            for (AiGraphSnapshot.SnapshotNode node : snapshot.nodes) {
                String type = safeLower(node.type);
                if (containsAny(type, "task", "action", "project", "goal", "routine", "trigger", "obstacle")) executionSignals += 2;
                if (containsAny(type, "decision", "option", "criterion", "risk", "assumption", "evidence")) decisionSignals += 2;
                if (containsAny(type, "concept", "question", "source", "note", "insight", "experiment")) learningSignals += 2;
            }
            if (snapshot.connections == null || snapshot.connections.size() < Math.max(4, snapshot.nodes.size() / 3)) networkSignals += 4;
        }

        BrainAgentProfile preferred = BrainAgentProfile.fromKey(settings.getPreferredAutopilotAgent());
        BrainAgentProfile specialist = preferred == BrainAgentProfile.AUTO
                ? chooseSpecialist(executionSignals, decisionSignals, learningSignals, networkSignals)
                : preferred;

        if (networkSignals >= 4 || specialist == BrainAgentProfile.NETWORK) plan.orderedProfiles.add(BrainAgentProfile.NETWORK);
        if (!plan.orderedProfiles.contains(specialist)) plan.orderedProfiles.add(specialist);

        float generalWeight = SuggestionFeedbackEngine.getAgentWeight(feedback, BrainAgentProfile.GENERAL.key);
        if (generalWeight >= 0.9f || plan.orderedProfiles.size() < 2) plan.orderedProfiles.add(BrainAgentProfile.GENERAL);

        plan.maxAgents = Math.min(3, plan.orderedProfiles.size());
        plan.totalCommandBudget = specialist == BrainAgentProfile.NETWORK ? 12 : 16;
        plan.reason = "动态编排：" + specialist.label + " 优先，GENERAL 权重=" + String.format(Locale.US, "%.2f", generalWeight);
        return plan;
    }

    public static void scoreRuns(List<AiAgentRunResult> runs, SuggestionFeedbackProfile feedback) {
        if (runs == null) return;
        for (AiAgentRunResult run : runs) {
            if (run == null) continue;
            float score = 0f;
            score += SuggestionFeedbackEngine.getAgentWeight(feedback, run.profile == null ? "" : run.profile.key) * 20f;
            score += Math.max(0, 8 - run.commandCount) * 1.8f;
            if (run.summary != null && !run.summary.trim().isEmpty()) score += 8f;
            if (run.durationMs > 0 && run.durationMs < 45000L) score += 4f;
                        if (run.commandCount > 0 && run.commandCount <= 6) score += 6f;
            if (run.commandCount > 10) score -= 8f;
            run.summary = "[score=" + String.format(Locale.US, "%.1f", score) + "] " + safe(run.summary);
        }

        Collections.sort(runs, new Comparator<AiAgentRunResult>() {
            @Override
            public int compare(AiAgentRunResult a, AiAgentRunResult b) {
                return Float.compare(extractScore(b.summary), extractScore(a.summary));
            }
        });
    }

    public static List<AiAgentRunResult> trimToBudget(List<AiAgentRunResult> sortedRuns, int totalCommandBudget) {
        List<AiAgentRunResult> kept = new ArrayList<>();
        if (sortedRuns == null) return kept;
        int used = 0;
        for (AiAgentRunResult run : sortedRuns) {
            if (run == null) continue;
            if (used > 0 && used + run.commandCount > totalCommandBudget) continue;
            kept.add(run);
            used += Math.max(0, run.commandCount);
        }
        if (kept.isEmpty() && !sortedRuns.isEmpty()) kept.add(sortedRuns.get(0));
        return kept;
    }

    private static BrainAgentProfile chooseSpecialist(int executionSignals, int decisionSignals, int learningSignals, int networkSignals) {
        if (networkSignals >= executionSignals && networkSignals >= decisionSignals && networkSignals >= learningSignals && networkSignals >= 4) return BrainAgentProfile.NETWORK;
        if (decisionSignals >= executionSignals && decisionSignals >= learningSignals) return BrainAgentProfile.DECISION;
        if (learningSignals >= executionSignals) return BrainAgentProfile.LEARNING;
        return BrainAgentProfile.EXECUTION;
    }

    private static boolean containsAny(String text, String... items) {
        String src = safeLower(text);
        for (String item : items) if (src.contains(safeLower(item))) return true;
        return false;
    }

    private static float extractScore(String summary) {
        String s = safe(summary);
        int start = s.indexOf("[score="), end = s.indexOf("]", start);
        if (start >= 0 && end > start) {
            try { return Float.parseFloat(s.substring(start + 7, end)); } catch (Exception ignored) {}
        }
        return 0f;
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
    private static String safeLower(String value) { return safe(value).toLowerCase(Locale.ROOT); }
}
