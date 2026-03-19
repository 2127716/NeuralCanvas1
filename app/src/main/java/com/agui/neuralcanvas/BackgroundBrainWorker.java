package com.agui.neuralcanvas;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Map;

public class BackgroundBrainWorker extends Worker {
    public BackgroundBrainWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SimpleDataManager dataManager = new SimpleDataManager(context);
        BrainAutopilotSettings settings = dataManager.loadAutopilotSettings();
        AiConfig config = dataManager.loadAiConfig();

        if (settings == null || !settings.isEnabled() || !settings.isApiAutopilotEnabled()) {
            return Result.success();
        }

        try {
            Map<?, ?> saved = dataManager.loadMindMap();
            Map<String, Node> nodes = (Map<String, Node>) saved.get("nodes");
            Map<String, Connection> connections = (Map<String, Connection>) saved.get("connections");

            BackgroundBrainAnalyzer.BrainPulseReport report =
                    BackgroundBrainAnalyzer.analyze(nodes, connections, config, settings);

            if (report.responseJson != null && !report.responseJson.trim().isEmpty()) {
                AiResponse response = AiJsonParser.parseResponse(report.responseJson);
                AiAutopilotSafetyEngine.SafetyReport safety = AiAutopilotSafetyEngine.analyze(response);
                if (settings.isAutoApplyLowRiskChanges()
                        && safety.riskLevel == AiAutopilotSafetyEngine.RiskLevel.LOW
                        && response.getCommands() != null
                        && !response.getCommands().isEmpty()) {
                    new AiHeadlessExecutor(nodes, connections).execute(response.getCommands());
                    dataManager.saveMindMap(nodes, connections);
                    report.autoApplied = true;
                    report.summary = (report.summary == null ? "" : report.summary)
                            + "（已自动执行低风险改动）";
                }
            }

            BrainPendingGuidance guidance = new BrainPendingGuidance();
            guidance.timestamp = System.currentTimeMillis();
            guidance.summary = report.summary;
            guidance.focusNodeId = report.focusNodeId;
            guidance.focusNodeTitle = report.focusNodeTitle;
            guidance.mode = report.suggestedMode;
            guidance.responseJson = report.responseJson;
            guidance.autoApplied = report.autoApplied;
            guidance.riskLevel = report.riskLevel;
            dataManager.savePendingBrainGuidance(guidance);
            dataManager.saveLastBrainPulse(System.currentTimeMillis(), report.summary);

            if (settings.isNotificationsEnabled() && report.shouldNotify) {
                BrainNotificationHelper.showBrainPulse(context, report, settings);
            }
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }
}
