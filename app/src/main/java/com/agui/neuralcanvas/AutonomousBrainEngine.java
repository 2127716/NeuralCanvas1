package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AutonomousBrainEngine {

    public static final class BrainAgendaItem {
        public final Node node;
        public final String reason;
        public final String actionId;
        public final int score;

        public BrainAgendaItem(Node node, String reason, String actionId, int score) {
            this.node = node;
            this.reason = reason;
            this.actionId = actionId;
            this.score = score;
        }
    }

    public static final class BrainReport {
        public final List<BrainAgendaItem> agenda = new ArrayList<>();
        public final List<String> summaries = new ArrayList<>();
        public String headline = "当前没有明显高优先级问题";
    }

    private AutonomousBrainEngine() {}

    public static BrainReport analyze(Map<String, Node> nodes,
                                      Map<String, Connection> connections,
                                      Node selectedNode) {
        BrainReport report = new BrainReport();
        ProjectHealthEngine.ProjectHealthReport projectHealth = ProjectHealthEngine.analyze(nodes, connections);

        if (!projectHealth.stuckProjects.isEmpty()) {
            Node node = projectHealth.stuckProjects.get(0);
            report.agenda.add(new BrainAgendaItem(node, "项目卡住，缺少可推进动作", "action:QUICK_FIX", 100));
            report.summaries.add("有卡住项目需要立即补下一步");
        }
        if (!projectHealth.overdueActions.isEmpty()) {
            Node node = projectHealth.overdueActions.get(0);
            report.agenda.add(new BrainAgendaItem(node, "有逾期动作，需重新触发或重排", "action:QUICK_FIX", 95));
            report.summaries.add("有逾期动作正在拖慢系统");
        }
        if (!projectHealth.actionsWithoutTrigger.isEmpty()) {
            Node node = projectHealth.actionsWithoutTrigger.get(0);
            report.agenda.add(new BrainAgendaItem(node, "动作没有触发条件，容易一直悬着", "action:QUICK_FIX", 93));
            report.summaries.add("有动作节点缺少触发条件");
        }
        if (!projectHealth.weakEvidenceDecisions.isEmpty()) {
            Node node = projectHealth.weakEvidenceDecisions.get(0);
            report.agenda.add(new BrainAgendaItem(node, "高置信低证据，决策容易失真", "action:QUICK_FIX", 91));
            report.summaries.add("有决策节点证据不足");
        }
        if (!projectHealth.staleLearningNodes.isEmpty()) {
            Node node = projectHealth.staleLearningNodes.get(0);
            report.agenda.add(new BrainAgendaItem(node, "学习节点没有复习安排或迁移验证", "action:QUICK_FIX", 88));
            report.summaries.add("有学习节点没有进入记忆闭环");
        }

        if (selectedNode != null) {
            WorkflowMethodRecommendationEngine.Analysis analysis =
                    WorkflowMethodRecommendationEngine.analyze(selectedNode, nodes, connections);
            if (!analysis.gaps.isEmpty()) {
                report.agenda.add(0, new BrainAgendaItem(
                        selectedNode,
                        "当前焦点节点存在结构缺口，优先补它最省心",
                        "action:QUICK_FIX",
                        106
                ));
            }
        }

        report.agenda.sort((a, b) -> Integer.compare(b.score, a.score));

        if (!report.agenda.isEmpty()) {
            Node node = report.agenda.get(0).node;
            String title = WorkflowEngine.safe(node.getTitle());
            report.headline = "系统建议你先处理：" + (title.isEmpty() ? "(无标题)" : title);
        } else if (selectedNode != null) {
            String title = WorkflowEngine.safe(selectedNode.getTitle());
            report.headline = "当前焦点节点结构相对完整：" + (title.isEmpty() ? "(无标题)" : title);
        }

        return report;
    }

    public static String buildReadableSummary(BrainReport report) {
        if (report == null) return "暂无分析结果";
        StringBuilder sb = new StringBuilder(report.headline);
        int limit = Math.min(3, report.summaries.size());
        if (limit > 0) {
            sb.append("\n");
            for (int i = 0; i < limit; i++) {
                sb.append(i == 0 ? "• " : "\n• ").append(report.summaries.get(i));
            }
        }
        if (!report.agenda.isEmpty()) {
            BrainAgendaItem item = report.agenda.get(0);
            sb.append("\n\n最优先：")
                    .append(WorkflowEngine.safe(item.node.getTitle()).isEmpty() ? "(无标题)" : WorkflowEngine.safe(item.node.getTitle()))
                    .append(" —— ")
                    .append(item.reason);
        }
        return sb.toString();
    }
}
