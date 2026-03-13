package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ReferenceForecastEngine {

    public static class ForecastResult {
        public int sampleCount;
        public float avgEstimatedHours;
        public float avgActualHours;
        public float medianActualHours;
        public float p80ActualHours;
        public float avgBiasRatio;
        public final List<Node> matchedNodes = new ArrayList<>();

        public String toReadableText() {
            if (sampleCount <= 0) {
                return "没有找到足够相似的历史样本";
            }
            return "样本数=" + sampleCount
                    + "｜平均预估=" + fmt(avgEstimatedHours) + "h"
                    + "｜平均实际=" + fmt(avgActualHours) + "h"
                    + "｜中位实际=" + fmt(medianActualHours) + "h"
                    + "｜P80实际=" + fmt(p80ActualHours) + "h"
                    + "｜平均偏差=" + fmt(avgBiasRatio) + "x";
        }

        private String fmt(float value) {
            return String.format(Locale.getDefault(), "%.1f", value);
        }
    }

    private ReferenceForecastEngine() {
    }

    public static ForecastResult build(Node currentNode, Map<String, Node> nodes) {
        ForecastResult result = new ForecastResult();
        if (currentNode == null || nodes == null || nodes.isEmpty()) {
            return result;
        }

        List<Node> candidates = new ArrayList<>();
        for (Node node : nodes.values()) {
            if (node == null) continue;
            if (currentNode.getId().equals(node.getId())) continue;
            if (node.getActualEffort() <= 0f) continue;
            if (!isSimilarEnough(currentNode, node)) continue;
            candidates.add(node);
        }

        Collections.sort(candidates, new Comparator<Node>() {
            @Override
            public int compare(Node a, Node b) {
                return Float.compare(similarityScore(currentNode, b), similarityScore(currentNode, a));
            }
        });

        int limit = Math.min(12, candidates.size());
        List<Float> actuals = new ArrayList<>();
        float estimatedSum = 0f;
        float actualSum = 0f;
        float biasSum = 0f;
        int biasCount = 0;

        for (int i = 0; i < limit; i++) {
            Node node = candidates.get(i);
            result.matchedNodes.add(node);

            if (node.getEffortEstimate() > 0f) {
                estimatedSum += node.getEffortEstimate();
            }
            actualSum += node.getActualEffort();
            actuals.add(node.getActualEffort());

            if (node.getEffortEstimate() > 0f) {
                biasSum += node.getActualEffort() / Math.max(0.1f, node.getEffortEstimate());
                biasCount++;
            }
        }

        result.sampleCount = result.matchedNodes.size();
        if (result.sampleCount <= 0) return result;

        result.avgEstimatedHours = estimatedSum / Math.max(1, result.sampleCount);
        result.avgActualHours = actualSum / result.sampleCount;

        Collections.sort(actuals);
        result.medianActualHours = percentile(actuals, 0.5f);
        result.p80ActualHours = percentile(actuals, 0.8f);
        result.avgBiasRatio = biasCount > 0 ? (biasSum / biasCount) : 0f;

        return result;
    }

    public static String buildPlanningHint(Node currentNode, Map<String, Node> nodes) {
        ForecastResult result = build(currentNode, nodes);
        if (result.sampleCount <= 0) {
            return "参考类预测：暂无足够历史样本，先保守预估并记录实际耗时。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("参考类预测：建议优先按 P80 实际耗时 ");
        sb.append(String.format(Locale.getDefault(), "%.1f", result.p80ActualHours));
        sb.append("h 规划。");

        if (result.avgBiasRatio > 1.15f) {
            sb.append(" 你过往普遍低估耗时。");
        } else if (result.avgBiasRatio > 0f && result.avgBiasRatio < 0.85f) {
            sb.append(" 你过往普遍高估耗时。");
        } else if (result.avgBiasRatio > 0f) {
            sb.append(" 你的估时相对稳定。");
        }

        sb.append(" 样本数=").append(result.sampleCount).append("。");
        return sb.toString();
    }

    private static boolean isSimilarEnough(Node a, Node b) {
        if (a == null || b == null) return false;
        float score = similarityScore(a, b);
        if (a.getType() == b.getType() && score >= 0.18f) return true;
        return score >= 0.28f;
    }

    private static float similarityScore(Node a, Node b) {
        String[] as = tokens(a.getTitle());
        String[] bs = tokens(b.getTitle());

        if (as.length == 0 || bs.length == 0) {
            return a.getType() == b.getType() ? 0.2f : 0f;
        }

        int overlap = 0;
        for (String x : as) {
            for (String y : bs) {
                if (x.equals(y)) {
                    overlap++;
                    break;
                }
            }
        }

        float lexical = (float) overlap / (float) Math.max(as.length, bs.length);
        float typeBonus = a.getType() == b.getType() ? 0.15f : 0f;
        return lexical + typeBonus;
    }

    private static String[] tokens(String raw) {
        String text = WorkflowEngine.safe(raw).toLowerCase(Locale.ROOT);
        if (text.isEmpty()) return new String[0];
        return text.split("\\s+");
    }

    private static float percentile(List<Float> values, float p) {
        if (values == null || values.isEmpty()) return 0f;
        if (values.size() == 1) return values.get(0);

        float index = p * (values.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);

        if (lower == upper) return values.get(lower);

        float lowerValue = values.get(lower);
        float upperValue = values.get(upper);
        float fraction = index - lower;
        return lowerValue + (upperValue - lowerValue) * fraction;
    }
}
