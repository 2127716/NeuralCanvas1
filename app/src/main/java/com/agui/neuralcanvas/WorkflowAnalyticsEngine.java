package com.agui.neuralcanvas;

import java.util.Locale;
import java.util.Map;

public final class WorkflowAnalyticsEngine {

    public static class AnalyticsReport {
        public int completedNodes;
        public int focusSessions;
        public int triggeredWins;
        public int triggeredMisses;
        public int memoryDue;
        public int memoryUpcoming;
        public int estimatedCount;
        public float estimatedHours;
        public float actualHours;
        public float avgBiasRatio;
        public int forecastSampleNodes;
        public float suggestedHoursCovered;
    }

    private WorkflowAnalyticsEngine() {}

    public static AnalyticsReport build(Map<String, Node> nodes) {
        AnalyticsReport r = new AnalyticsReport();
        if (nodes == null) return r;
        MemoryEngine.MemorySnapshot memory = MemoryEngine.build(nodes);
        r.memoryDue = memory.dueNodes.size();
        r.memoryUpcoming = memory.upcomingNodes.size();
        float biasSum = 0f;

        for (Node node : nodes.values()) {
            if (node == null) continue;
            if (node.getStatus() == Node.NodeStatus.DONE) r.completedNodes++;
            r.focusSessions += GraphMetaHelper.getInt(node, "focus_sessions", 0);
            r.triggeredWins += GraphMetaHelper.getInt(node, "trigger_success", 0);
            r.triggeredMisses += GraphMetaHelper.getInt(node, "trigger_fail", 0);
            if (node.getEffortEstimate() > 0f) {
                r.estimatedCount++;
                r.estimatedHours += node.getEffortEstimate();
            }
            if (node.getActualEffort() > 0f) {
                r.actualHours += node.getActualEffort();
            }
            if (node.getEffortEstimate() > 0f && node.getActualEffort() > 0f) {
                biasSum += node.getActualEffort() / Math.max(0.1f, node.getEffortEstimate());
            }
            int samples = GraphMetaHelper.getInt(node, "forecast_sample_count", 0);
            if (samples > 0) {
                r.forecastSampleNodes++;
                r.suggestedHoursCovered += GraphMetaHelper.getFloat(node, "forecast_recommended_hours", 0f);
            }
        }
        if (r.estimatedCount > 0) r.avgBiasRatio = biasSum / r.estimatedCount;
        return r;
    }

    public static String buildReadableSummary(AnalyticsReport r) {
        if (r == null) return "暂无科学分析数据";
        StringBuilder sb = new StringBuilder();
        sb.append("完成节点：").append(r.completedNodes).append("\n");
        sb.append("深度工作 Session：").append(r.focusSessions).append("\n");
        sb.append("If-Then 命中/失手：").append(r.triggeredWins).append(" / ").append(r.triggeredMisses).append("\n");
        sb.append("记忆队列：到期 ").append(r.memoryDue).append("｜即将到期 ").append(r.memoryUpcoming).append("\n");
        sb.append("估时样本：").append(r.estimatedCount).append("\n");
        sb.append("预计/实际工时：")
                .append(format(r.estimatedHours)).append("h / ")
                .append(format(r.actualHours)).append("h\n");
        if (r.avgBiasRatio > 0f) {
            sb.append("平均估时偏差：").append(format(r.avgBiasRatio)).append("x");
            if (r.avgBiasRatio > 1.25f) sb.append("（普遍低估）");
            else if (r.avgBiasRatio < 0.85f) sb.append("（普遍高估）");
            else sb.append("（相对稳定）");
            sb.append("
");
        }
        sb.append("参考类预测覆盖：").append(r.forecastSampleNodes).append(" 个节点");
        if (r.suggestedHoursCovered > 0f) {
            sb.append("｜预测建议工时合计 ").append(format(r.suggestedHoursCovered)).append("h");
        }
        return sb.toString();
    }

    private static String format(float v) {
        return String.format(Locale.getDefault(), "%.1f", v);
    }
}
