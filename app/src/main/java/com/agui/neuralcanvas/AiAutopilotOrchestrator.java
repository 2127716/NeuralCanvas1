package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.List;

public final class AiAutopilotOrchestrator {

    public static final class OrchestratorResult {
        public final List<AiAgentRunResult> runs = new ArrayList<>();
        public AiResponse mergedResponse = new AiResponse();

        public String buildSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("已运行 ").append(runs.size()).append(" 个代理");
            for (AiAgentRunResult run : runs) {
                sb.append("\n- ").append(run.buildLabel()).append("：").append(run.commandCount).append(" 条命令");
            }
            return sb.toString();
        }
    }

    private AiAutopilotOrchestrator() {}

    public static OrchestratorResult run(AiConfig config,
                                         AiGraphSnapshot fullSnapshot,
                                         BrainAutopilotSettings settings) throws Exception {
        OrchestratorResult result = new OrchestratorResult();
        if (fullSnapshot == null) fullSnapshot = new AiGraphSnapshot();
        if (settings == null) settings = new BrainAutopilotSettings();

        BrainAgentProfile preferred = BrainAgentProfile.fromKey(settings.getPreferredAutopilotAgent());
        BrainAgentProfile specialist = preferred == BrainAgentProfile.AUTO
                ? AiAgentPromptBuilder.chooseProfile(fullSnapshot)
                : preferred;

        List<BrainAgentProfile> plan = new ArrayList<>();
        plan.add(BrainAgentProfile.NETWORK);
        if (specialist != BrainAgentProfile.NETWORK && specialist != BrainAgentProfile.GENERAL) {
            plan.add(specialist);
        }
        plan.add(BrainAgentProfile.GENERAL);

        for (BrainAgentProfile profile : plan) {
            AiAgentRunResult run = executeAgent(config, fullSnapshot, settings, profile);
            result.runs.add(run);
        }

        result.mergedResponse = AiCommandMergeEngine.merge(result.runs);
        return result;
    }

    private static AiAgentRunResult executeAgent(AiConfig config,
                                                 AiGraphSnapshot fullSnapshot,
                                                 BrainAutopilotSettings settings,
                                                 BrainAgentProfile profile) throws Exception {
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
