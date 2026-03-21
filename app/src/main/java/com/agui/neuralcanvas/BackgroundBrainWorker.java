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

        if (settings == null || !settings.isEnabled() || !settings.isApiAutopilotEnabled()) return Result.success();

        try {
            Map<?, ?> saved = dataManager.loadMindMap();
            Map<String, Node> nodes = (Map<String, Node>) saved.get("nodes");
            Map<String, Connection> connections = (Map<String, Connection>) saved.get("connections");
            SuggestionFeedbackProfile feedbackProfile = dataManager.loadSuggestionFeedbackProfile();
            AgentRunHistoryProfile historyProfile = dataManager.loadAgentRunHistoryProfile();

            BackgroundBrainAnalyzer.BrainPulseReport report =
                    BackgroundBrainAnalyzer.analyze(nodes, connections, config, settings, feedbackProfile, historyProfile);

            AiResponse response = null;
            AiSelfReviewEngine.ReviewResult reviewResult = null;

            if (report.responseJson != null && !report.responseJson.trim().isEmpty()) {
                response = AiJsonParser.parseResponse(report.responseJson);

                try {
                    AiModelSelfReviewEngine.ReviewOutcome modelReview =
                            new AiModelSelfReviewEngine().review(config, AiGraphSnapshot.from(nodes, connections), response);
                    if (modelReview != null) {
                        response = modelReview.response == null ? response : modelReview.response;
                        report.summary = safe(report.summary) + "\n\n【模型复审】\n" + modelReview.summary;
                    }
                } catch (Exception ignored) {
                    report.summary = safe(report.summary) + "\n\n【模型复审】\n模型复审跳过，已回退到规则复审";
                }

                reviewResult = AiSelfReviewEngine.review(response, nodes, connections);
                response = reviewResult.response;
                report.responseJson = AiJsonParser.toJson(response);

                if (reviewResult != null && (reviewResult.removedCount > 0 || !reviewResult.issues.isEmpty())) {
                    report.summary = safe(report.summary) + "\n\n【AI自我复审】\n" + reviewResult.buildSummary();
                }

                AutonomousOperationPolicyEngine.PolicyResult policy = AutonomousOperationPolicyEngine.split(response);
                report.summary = safe(report.summary) + "\n\n【自治执行】\n" + policy.summary;
                report.riskLevel = policy.riskLevel;

                boolean effective = false;

                if (policy.hasAuto && policy.autoResponse.getCommands() != null && !policy.autoResponse.getCommands().isEmpty()) {
                    new AiHeadlessExecutor(nodes, connections).execute(policy.autoResponse.getCommands());
                    OutcomeFeedbackEngine.backfillFromCommands(nodes, policy.autoResponse);
                    dataManager.saveMindMap(nodes, connections);
                    report.autoApplied = true;
                    effective = true;
                    report.summary = safe(report.summary) + "\n已自动执行低风险改动。";
                    SuggestionFeedbackEngine.recordAutoApplied(dataManager, policy.autoResponse, report.agentProfile);
                    SuggestionFeedbackEngine.recordEffectiveness(dataManager, policy.autoResponse);
                }

                if (policy.hasConfirm && policy.confirmResponse.getCommands() != null && !policy.confirmResponse.getCommands().isEmpty()) {
                    PendingOperationBundle bundle = new PendingOperationBundle();
                    bundle.createdAt = System.currentTimeMillis();
                    bundle.summary = "AI 自动巡航生成了需要人工确认的改动。";
                    bundle.responseJson = AiJsonParser.toJson(policy.confirmResponse);
                    bundle.riskLevel = policy.riskLevel;
                    bundle.focusNodeId = report.focusNodeId;
                    bundle.focusNodeTitle = report.focusNodeTitle;
                    bundle.commandCount = policy.confirmResponse.getCommands().size();
                    bundle.impactSummary = policy.impactSummary.buildSummary();
                    dataManager.savePendingOperationBundle(bundle);
                    report.summary = safe(report.summary) + "\n已生成待确认改动队列。";
                } else {
                    dataManager.clearPendingOperationBundle();
                }

                WorkflowAuditEngine.AuditResult afterAudit = WorkflowAuditEngine.audit(nodes, connections);
                if (!afterAudit.isHealthy()) {
                    report.summary = safe(report.summary) + "\n\n【执行后二次审查】\n" + afterAudit.buildSummary();
                    for (String issue : afterAudit.issues) {
                        Node target = findFirstMentionedNode(nodes, issue);
                        if (target != null) NodeIntelligenceEngine.markIssue(target);
                    }
                }

                LearningLoopEngine.LearningReport learningReport = LearningLoopEngine.analyze(nodes, connections);
                LearningTransferEngine.TransferReport transferReport = LearningTransferEngine.analyze(nodes, connections);
                NetworkEvolutionEngine.NetworkReport networkReport = NetworkEvolutionEngine.analyze(nodes, connections);
                BrainMaturityEngine.MaturityReport maturity = BrainMaturityEngine.analyze(
                        nodes, connections, dataManager.loadBehaviorMemoryProfile(), dataManager.loadSuggestionFeedbackProfile());

                report.summary = safe(report.summary)
                        + "\n\n【学习闭环】\n" + learningReport.buildSummary()
                        + "\n\n【学习迁移】\n" + transferReport.buildSummary()
                        + "\n\n【网络进化】\n" + networkReport.buildSummary()
                        + "\n\n【成熟度】\n" + maturity.buildSummary()
                        + "\n\n【补丁成效】\n" + SuggestionEffectivenessTracker.buildSummary(nodes);

                BehaviorMemoryEngine.PulseRecord record = new BehaviorMemoryEngine.PulseRecord();
                record.agentProfile = report.agentProfile;
                record.riskLevel = report.riskLevel;
                record.focusNodeId = report.focusNodeId;
                record.autoApplied = report.autoApplied;
                record.removedCommands = reviewResult == null ? 0 : reviewResult.removedCount;
                record.auditIssues = afterAudit.issues;
                record.commands = response == null ? null : response.getCommands();
                BehaviorMemoryEngine.record(dataManager, record);

                if (report.orchestratorResult != null) {
                    AgentRunHistoryEngine.record(
                            dataManager,
                            report.orchestratorResult.runs,
                            report.orchestratorResult.keptRuns,
                            effective
                    );
                }

                Node focusNode = nodes == null ? null : nodes.get(report.focusNodeId);
                if (focusNode != null) {
                    NodeIntelligenceEngine.markFocus(focusNode);
                    OutcomeFeedbackEngine.markFocused(focusNode);
                }

                BehaviorMemoryProfile profile = dataManager.loadBehaviorMemoryProfile();
                PrioritySchedulerEngine.PriorityBoard board = PrioritySchedulerEngine.build(nodes, connections, profile);
                feedbackProfile = dataManager.loadSuggestionFeedbackProfile();
                historyProfile = dataManager.loadAgentRunHistoryProfile();

                report.summary = safe(report.summary)
                        + "\n\n【长期记忆】\n" + BehaviorMemoryEngine.buildSummary(profile)
                        + "\n\n【建议反馈】\n" + SuggestionFeedbackEngine.buildSummary(feedbackProfile)
                        + "\n\n【代理历史】\n" + AgentRunHistoryEngine.buildSummary(historyProfile)
                        + "\n\n【今日优先级】\n" + board.buildSummary();
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

    private Node findFirstMentionedNode(Map<String, Node> nodes, String issue) {
        if (nodes == null || issue == null || issue.trim().isEmpty()) return null;
        for (Node node : nodes.values()) {
            if (node == null) continue;
            String title = node.getTitle() == null ? "" : node.getTitle().trim();
            if (!title.isEmpty() && issue.contains(title)) return node;
        }
        return null;
    }

    private String safe(String value) { return value == null ? "" : value.trim(); }
}
