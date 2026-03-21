package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.List;

public final class AiAutopilotOrchestrator {

    public static final class OrchestratorResult {
        public final List<AiAgentRunResult> runs = new ArrayList<>();
        public final List<AiAgentRunResult> keptRuns = new ArrayList<>();
        public AiResponse mergedResponse = new AiResponse();
        public String planSummary = "";

        public String buildSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("已运行 ").append(runs.size()).append(" 个代理");
            if (planSummary != null && !planSummary.trim().isEmpty()) sb.append("\n").append(planSummary.trim());
            for (AiAgentRunResult run : runs) sb.append("\n- ").append(run.buildLabel()).append("：").append(run.commandCount).append(" 条命令");
            return sb.toString();
        }
    }

    private AiAutopilotOrchestrator() {}

    public static OrchestratorResult run(AiConfig config,
                                         AiGraphSnapshot fullSnapshot,
                                         BrainAutopilotSettings settings,
                                         SuggestionFeedbackProfile feedback,
                                         AgentRunHistoryProfile history) throws Exception {
        OrchestratorResult result = new OrchestratorResult();
        if (fullSnapshot == null) fullSnapshot = new AiGraphSnapshot();
        if (settings == null) settings = new BrainAutopilotSettings();
        if (feedback == null) feedback = new SuggestionFeedbackProfile();
        if (history == null) history = new AgentRunHistoryProfile();

        AgentScoringEngine.AgentPlan plan = AgentScoringEngine.buildPlan(fullSnapshot, settings, feedback, history);
        result.planSummary = plan.reason;

        for (BrainAgentProfile profile : plan.orderedProfiles) {
            if (result.runs.size() >= plan.maxAgents) break;
            AiAgentRunResult run = executeAgent(config, fullSnapshot, settings, profile);
            if (run.response != null && run.response.getCommands() != null && run.response.getCommands().size() > plan.maxCommandsPerRun) {
                run.response.setCommands(new ArrayList<>(run.response.getCommands().subList(0, plan.maxCommandsPerRun)));
                run.commandCount = run.response.getCommands().size();
            }
            result.runs.add(run);
        }

        AgentScoringEngine.scoreRuns(result.runs, feedback, history);
        result.keptRuns.addAll(AgentScoringEngine.trimToBudget(result.runs, plan.totalCommandBudget));
        result.mergedResponse = AiCommandMergeEngine.merge(result.keptRuns);
        result.mergedResponse.setAnswer((plan.reason == null ? "" : plan.reason) + "\n" + result.mergedResponse.getAnswer());
        return result;
    }

    private static AiAgentRunResult executeAgent(AiConfig config, AiGraphSnapshot fullSnapshot, BrainAutopilotSettings settings, BrainAgentProfile profile) throws Exception {
        long start = System.currentTimeMillis();
        AiAgentRunResult run = new AiAgentRunResult();
        run.profile = profile;

        BrainAutopilotSettings local = cloneSettings(settings);
        local.setPreferredAutopilotAgent(profile.key);

        AiGraphSnapshot summarized = AiGraphSummaryBuilder.build(fullSnapshot, profile);
        AiResponse response = new AiAutopilotApi().runAutopilot(config, summarized, local);

        run.response = response == null ? new AiResponse() : response;
        run.summary = run.response.getAnswer();
        run.commandCount = run.response.getCommands() == null ? 0 : run.response.getCommands().size();
        run.durationMs = System.currentTimeMillis() - start;
        return run;
    }

    private static BrainAutopilotSettings cloneSettings(BrainAutopilotSettings src) {
        BrainAutopilotSettings out = new BrainAutopilotSettings();
        out.setEnabled(src.isEnabled());
        out.setNotificationsEnabled(src.isNotificationsEnabled());
        out.setIntervalHours(src.getIntervalHours());
        out.setOpenModeOnNotificationTap(src.isOpenModeOnNotificationTap());
        out.setApiAutopilotEnabled(src.isApiAutopilotEnabled());
        out.setAutoApplyLowRiskChanges(src.isAutoApplyLowRiskChanges());
        out.setInAppPulseOnResume(src.isInAppPulseOnResume());
        out.setAssistantScope(src.getAssistantScope());
        out.setPreferredAutopilotAgent(src.getPreferredAutopilotAgent());
        out.setAutopilotInstruction(src.getAutopilotInstruction());
        return out;
    }
}
