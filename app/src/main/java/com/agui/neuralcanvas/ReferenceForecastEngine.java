package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ReferenceForecastEngine {

    public static class ForecastReport {
        public final List<Node> samples = new ArrayList<>();
        public int sampleCount;
        public float avgHours;
        public float p50Hours;
        public float p80Hours;
        public float recommendedHours;
        public float confidence;
        public String summary;
    }

    private ReferenceForecastEngine() {}

    public static ForecastReport analyze(Node target, Map<String, Node> nodes) {
        ForecastReport report = new ForecastReport();
        if (target == null || nodes == null) {
            report.summary = "暂无参考类预测数据";
            return report;
        }

        List<Float> hours = new ArrayList<>();
        for (Node node : nodes.values()) {
            if (node == null || node == target) continue;
            if (!WorkflowEngine.isDone(node)) continue;
            float actual = node.getActualEffort();
            if (actual <= 0.01f) continue;
            float similarity = similarity(target, node);
            if (similarity < 0.34f) continue;
            hours.add(actual);
            report.samples.add(node);
        }

        Collections.sort(hours, new Comparator<Float>() {
            @Override public int compare(Float a, Float b) { return Float.compare(a, b); }
        });

        report.sampleCount = hours.size();
        if (hours.isEmpty()) {
            report.summary = "暂无足够相似的历史样本，先按最小可验证工作量估时。";
            return report;
        }

        float sum = 0f;
        for (Float h : hours) sum += h;
        report.avgHours = sum / hours.size();
        report.p50Hours = percentile(hours, 0.50f);
        report.p80Hours = percentile(hours, 0.80f);
        report.recommendedHours = report.sampleCount >= 3 ? report.p80Hours : Math.max(report.avgHours, report.p50Hours);
        report.confidence = Math.min(0.95f, 0.35f + report.sampleCount * 0.1f);
        report.summary = buildSummary(report);
        return report;
    }

    public static boolean applyForecastToNode(Node target, ForecastReport report) {
        if (target == null || report == null || report.sampleCount <= 0) return false;
        if (target.getEffortEstimate() <= 0.01f) {
            target.setEffortEstimate(round(report.recommendedHours));
        }
        GraphMetaHelper.putFloat(target, "forecast_avg_hours", round(report.avgHours));
        GraphMetaHelper.putFloat(target, "forecast_p50_hours", round(report.p50Hours));
        GraphMetaHelper.putFloat(target, "forecast_p80_hours", round(report.p80Hours));
        GraphMetaHelper.putFloat(target, "forecast_recommended_hours", round(report.recommendedHours));
        GraphMetaHelper.putInt(target, "forecast_sample_count", report.sampleCount);
        GraphMetaHelper.putFloat(target, "forecast_confidence", round(report.confidence));
        return true;
    }

    public static String buildSummary(ForecastReport report) {
        if (report == null || report.sampleCount <= 0) return "暂无足够相似的历史样本";
        return "样本=" + report.sampleCount
                + "｜均值=" + fmt(report.avgHours) + "h"
                + "｜P50=" + fmt(report.p50Hours) + "h"
                + "｜P80=" + fmt(report.p80Hours) + "h"
                + "｜建议估时=" + fmt(report.recommendedHours) + "h"
                + "｜置信=" + fmt(report.confidence * 100f) + "%";
    }

    private static float percentile(List<Float> sorted, float p) {
        if (sorted == null || sorted.isEmpty()) return 0f;
        if (sorted.size() == 1) return sorted.get(0);
        float idx = (sorted.size() - 1) * p;
        int lo = (int) Math.floor(idx);
        int hi = (int) Math.ceil(idx);
        if (lo == hi) return sorted.get(lo);
        float ratio = idx - lo;
        return sorted.get(lo) * (1f - ratio) + sorted.get(hi) * ratio;
    }

    private static float similarity(Node a, Node b) {
        float score = 0f;
        if (a.getType() == b.getType()) score += 0.35f;
        if (WorkflowEngine.safe(a.getProjectId()).equals(WorkflowEngine.safe(b.getProjectId())) && !WorkflowEngine.safe(a.getProjectId()).isEmpty()) score += 0.15f;
        if (WorkflowEngine.safe(a.getAreaId()).equalsIgnoreCase(WorkflowEngine.safe(b.getAreaId())) && !WorkflowEngine.safe(a.getAreaId()).isEmpty()) score += 0.10f;
        score += tagOverlap(a, b) * 0.25f;
        score += titleOverlap(a, b) * 0.15f;
        return Math.min(1f, score);
    }

    private static float tagOverlap(Node a, Node b) {
        if (a.getTags().isEmpty() || b.getTags().isEmpty()) return 0f;
        int same = 0;
        for (String t1 : a.getTags()) {
            for (String t2 : b.getTags()) {
                if (WorkflowEngine.safe(t1).equalsIgnoreCase(WorkflowEngine.safe(t2)) && !WorkflowEngine.safe(t1).isEmpty()) {
                    same++;
                    break;
                }
            }
        }
        return Math.min(1f, same / (float) Math.max(1, Math.min(a.getTags().size(), b.getTags().size())));
    }

    private static float titleOverlap(Node a, Node b) {
        String[] as = WorkflowEngine.safe(a.getTitle()).toLowerCase(Locale.ROOT).split("\s+");
        String[] bs = WorkflowEngine.safe(b.getTitle()).toLowerCase(Locale.ROOT).split("\s+");
        if (as.length == 0 || bs.length == 0) return 0f;
        int same = 0;
        for (String x : as) {
            if (x.length() < 2) continue;
            for (String y : bs) {
                if (x.equals(y)) { same++; break; }
            }
        }
        return Math.min(1f, same / (float) Math.max(1, Math.min(as.length, bs.length)));
    }

    private static float round(float value) { return Math.round(value * 100f) / 100f; }
    private static String fmt(float value) { return String.format(Locale.US, "%.2f", value); }
}
