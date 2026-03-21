package com.agui.neuralcanvas;

import java.util.Map;

public final class BrainMaturityEngine {

    public static final class MaturityReport {
        public int aiEditing = 0;
        public int autopilot = 0;
        public int nodeAnalysis = 0;
        public int networkOptimization = 0;
        public int closureValidation = 0;
        public int learningSystem = 0;

        public String buildSummary() {
            return "成熟度估计：AI图谱编辑 " + aiEditing + "%，自动巡航 " + autopilot
                    + "%，节点分析 " + nodeAnalysis + "%，网络优化 " + networkOptimization
                    + "%，闭环验证 " + closureValidation + "%，学习系统 " + learningSystem + "%";
        }
    }

    private BrainMaturityEngine() {}

    public static MaturityReport analyze(Map<String, Node> nodes,
                                         Map<String, Connection> connections,
                                         BehaviorMemoryProfile behavior,
                                         SuggestionFeedbackProfile feedback) {
        MaturityReport r = new MaturityReport();
        r.aiEditing = 90;

        r.autopilot = 70;
        if (behavior != null && behavior.totalPulses > 0) r.autopilot += 8;
        if (feedback != null && feedback.autoAppliedCount > 0) r.autopilot += 6;

        r.nodeAnalysis = 68;
        if (nodes != null && !nodes.isEmpty()) r.nodeAnalysis += 8;
        if (behavior != null && !behavior.nodeIssueCount.isEmpty()) r.nodeAnalysis += 6;

        r.networkOptimization = 52;
        NetworkEvolutionEngine.NetworkReport network = NetworkEvolutionEngine.analyze(nodes, connections);
        if (network.duplicateNodeCount + network.similarNodeCount + network.brokenProjectCount + network.brokenKnowledgeCount == 0) r.networkOptimization += 18;
        else r.networkOptimization += 8;

        r.closureValidation = 60;
        WorkflowAuditEngine.AuditResult audit = WorkflowAuditEngine.audit(nodes, connections);
        if (audit.isHealthy()) r.closureValidation += 18;
        else r.closureValidation += 8;

        r.learningSystem = 62;
        LearningLoopEngine.LearningReport loop = LearningLoopEngine.analyze(nodes, connections);
        LearningTransferEngine.TransferReport transfer = LearningTransferEngine.analyze(nodes, connections);
        if (loop.dueCount + loop.upcomingCount > 0) r.learningSystem += 6;
        if (loop.missingQuestionCount == 0 && loop.missingSourceCount == 0) r.learningSystem += 8;
        if (transfer.missingTransferCount == 0) r.learningSystem += 8;

        return r;
    }
}
