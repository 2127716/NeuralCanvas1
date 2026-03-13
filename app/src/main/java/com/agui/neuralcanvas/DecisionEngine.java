package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DecisionEngine {
    private DecisionEngine() {}

    public static class OptionScore {
        public Node optionNode;
        public float weightedScore;
        public float supportEvidence;
        public float opposeEvidence;
        public float riskPenalty;
        public float finalScore;
        public final Map<String, Float> criterionScores = new LinkedHashMap<>();
    }

    public static class DecisionReport {
        public Node anchorNode;
        public final List<Node> criteria = new ArrayList<>();
        public final List<Node> options = new ArrayList<>();
        public final List<OptionScore> rankings = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();
        public float robustnessScore;
        public String robustnessLabel;
        public boolean rankingFlipsUnderSensitivity;
        public float confidenceSuggestion;
        public float topGap;
        public String recommendedOptionId;
        public String summaryText;
    }

    public static DecisionReport analyze(Node anchor, Map<String, Node> nodes, Map<String, Connection> connections) {
        DecisionReport report = new DecisionReport();
        report.anchorNode = anchor;
        if (anchor == null || nodes == null) return report;

        String ownerId = WorkflowEngine.resolveOwnerId(anchor);
        Set<String> connectedIds = collectConnected(anchor, connections);

        for (Node node : nodes.values()) {
            if (node == null) continue;
            boolean related = ownerId.equals(node.getProjectId()) || connectedIds.contains(node.getId()) || node.getId().equals(anchor.getId());
            if (!related) continue;
            if (node.getType() == Node.NodeType.OPTION) report.options.add(node);
            if (node.getType() == Node.NodeType.CRITERION) report.criteria.add(node);
        }

        float totalWeight = 0f;
        Map<String, Float> weights = new LinkedHashMap<>();
        for (Node criterion : report.criteria) {
            float w = GraphMetaHelper.getFloat(criterion, "decision_weight", Math.max(1f, criterion.getPriority()));
            w = Math.max(0.1f, w);
            weights.put(criterion.getId(), w);
            totalWeight += w;
        }
        if (totalWeight <= 0f) totalWeight = 1f;

        for (Node option : report.options) {
            OptionScore score = new OptionScore();
            score.optionNode = option;
            float weighted = 0f;
            for (Node criterion : report.criteria) {
                float raw = GraphMetaHelper.getFloat(option, "score_" + criterion.getId(), 5f);
                raw = clamp(raw, 0f, 10f);
                score.criterionScores.put(criterion.getId(), raw);
                weighted += raw * (weights.get(criterion.getId()) / totalWeight);
            }
            score.weightedScore = weighted;

            EvidenceTally tally = collectEvidence(option, ownerId, nodes, connections);
            score.supportEvidence = tally.support;
            score.opposeEvidence = tally.oppose;
            score.riskPenalty = tally.risk;
            score.finalScore = weighted + tally.support * 0.35f - tally.oppose * 0.35f - tally.risk * 0.45f;
            report.rankings.add(score);
        }

        Collections.sort(report.rankings, new Comparator<OptionScore>() {
            @Override public int compare(OptionScore a, OptionScore b) { return Float.compare(b.finalScore, a.finalScore); }
        });

        evaluateSensitivity(report, weights);
        enrichConfidence(report);
        buildWarnings(report);
        report.summaryText = buildSummaryText(report);
        return report;
    }

    private static class EvidenceTally { float support; float oppose; float risk; }

    private static EvidenceTally collectEvidence(Node option, String ownerId, Map<String, Node> nodes, Map<String, Connection> connections) {
        EvidenceTally tally = new EvidenceTally();
        if (option == null || nodes == null) return tally;
        for (Node node : nodes.values()) {
            if (node == null) continue;
            if (!(ownerId.equals(node.getProjectId()) || node.getId().equals(option.getId()))) continue;
            if (node.getType() == Node.NodeType.RISK) tally.risk += Math.max(0.4f, GraphMetaHelper.getFloat(node, "risk_impact", 1f));
        }
        if (connections != null) {
            for (Connection c : connections.values()) {
                if (c == null || !option.getId().equals(c.getToNodeId())) continue;
                Node from = nodes.get(c.getFromNodeId());
                if (from == null) continue;
                float strength = Math.max(0.2f, from.getEvidenceStrength());
                if (c.getType() == Connection.ConnectionType.EVIDENCE_FOR) tally.support += strength;
                if (c.getType() == Connection.ConnectionType.EVIDENCE_AGAINST) tally.oppose += strength;
                if (c.getType() == Connection.ConnectionType.BLOCKS && from.getType() == Node.NodeType.RISK) tally.risk += Math.max(0.4f, GraphMetaHelper.getFloat(from, "risk_impact", 1f));
            }
        }
        return tally;
    }

    private static void evaluateSensitivity(DecisionReport report, Map<String, Float> weights) {
        if (report.rankings.size() < 2 || report.criteria.isEmpty()) {
            report.robustnessScore = 1f; report.robustnessLabel = "稳定"; return;
        }
        String originalTop = report.rankings.get(0).optionNode.getId();
        int flips = 0;
        for (Node criterion : report.criteria) {
            Map<String, Float> mutated = new LinkedHashMap<>(weights);
            float w = mutated.get(criterion.getId());
            mutated.put(criterion.getId(), w * 1.25f);
            if (!originalTop.equals(simulateTop(report, mutated))) flips++;
            mutated = new LinkedHashMap<>(weights);
            mutated.put(criterion.getId(), Math.max(0.1f, w * 0.75f));
            if (!originalTop.equals(simulateTop(report, mutated))) flips++;
        }
        int tests = report.criteria.size() * 2;
        report.robustnessScore = clamp(1f - ((float) flips / Math.max(1, tests)), 0f, 1f);
        report.rankingFlipsUnderSensitivity = flips > 0;
        report.robustnessLabel = report.robustnessScore >= 0.85f ? "很稳健" : (report.robustnessScore >= 0.65f ? "中等稳健" : "脆弱");
    }

    private static String simulateTop(DecisionReport report, Map<String, Float> weights) {
        float total = 0f; for (Float v : weights.values()) total += v; if (total <= 0f) total = 1f;
        float best = -99999f; String bestId = "";
        for (OptionScore option : report.rankings) {
            float score = 0f;
            for (Node criterion : report.criteria) {
                float raw = option.criterionScores.containsKey(criterion.getId()) ? option.criterionScores.get(criterion.getId()) : 5f;
                score += raw * (weights.get(criterion.getId()) / total);
            }
            score += option.supportEvidence * 0.35f - option.opposeEvidence * 0.35f - option.riskPenalty * 0.45f;
            if (score > best) { best = score; bestId = option.optionNode.getId(); }
        }
        return bestId;
    }


    private static void enrichConfidence(DecisionReport report) {
        if (report.rankings.isEmpty()) {
            report.confidenceSuggestion = 0.35f;
            report.topGap = 0f;
            return;
        }
        report.recommendedOptionId = report.rankings.get(0).optionNode == null ? "" : report.rankings.get(0).optionNode.getId();
        float top = report.rankings.get(0).finalScore;
        float second = report.rankings.size() > 1 ? report.rankings.get(1).finalScore : top - 1.2f;
        report.topGap = top - second;
        float confidence = 0.45f + clamp(report.robustnessScore * 0.25f, 0f, 0.25f) + clamp(report.topGap / 6f, 0f, 0.22f);
        if (report.rankingFlipsUnderSensitivity) confidence -= 0.08f;
        report.confidenceSuggestion = clamp(confidence, 0.2f, 0.92f);
    }

    public static void persistReportToDecisionNode(Node anchor, DecisionReport report) {
        if (anchor == null || report == null) return;
        GraphMetaHelper.putFloat(anchor, "decision_robustness_score", report.robustnessScore);
        GraphMetaHelper.putFloat(anchor, "decision_confidence_suggestion", report.confidenceSuggestion);
        GraphMetaHelper.putFloat(anchor, "decision_top_gap", report.topGap);
        GraphMetaHelper.putString(anchor, "decision_robustness_label", report.robustnessLabel == null ? "" : report.robustnessLabel);
        GraphMetaHelper.putString(anchor, "decision_recommended_option_id", report.recommendedOptionId == null ? "" : report.recommendedOptionId);
        GraphMetaHelper.putString(anchor, "decision_summary", buildSummaryText(report));
        if (anchor.getConfidence() <= 0f || anchor.getConfidence() == 0.5f) {
            anchor.setConfidence(report.confidenceSuggestion);
        }
        String summary = buildSummaryText(report);
        String content = anchor.getContent() == null ? "" : anchor.getContent().trim();
        String marker = "【MCDA分析】";
        int idx = content.indexOf(marker);
        if (idx >= 0) content = content.substring(0, idx).trim();
        anchor.setContent((content.isEmpty() ? "" : content + "\n\n") + marker + "\n" + summary);
    }

    public static String buildSummaryText(DecisionReport report) {
        if (report == null) return "暂无分析结果";
        StringBuilder sb = new StringBuilder();
        if (!report.rankings.isEmpty()) {
            OptionScore top = report.rankings.get(0);
            sb.append("推荐方案：").append(safeTitle(top.optionNode))
                    .append("｜总分=").append(round(top.finalScore))
                    .append("｜稳健性=").append(report.robustnessLabel)
                    .append("（").append(round(report.robustnessScore)).append("）");
            sb.append("｜建议信心=").append(Math.round(report.confidenceSuggestion * 100f)).append("%");
            sb.append("｜领先差距=").append(round(report.topGap));
        } else {
            sb.append("当前没有足够的方案数据");
        }
        if (!report.warnings.isEmpty()) {
            sb.append("\n提醒：");
            int limit = Math.min(3, report.warnings.size());
            for (int i = 0; i < limit; i++) {
                if (i > 0) sb.append("；");
                sb.append(report.warnings.get(i));
            }
        }
        return sb.toString();
    }

    private static String round(float value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private static void buildWarnings(DecisionReport report) {
        if (report.criteria.isEmpty()) report.warnings.add("当前决策没有准则节点，建议至少建立 3 个准则。");
        if (report.options.size() < 2) report.warnings.add("当前候选方案太少，建议至少比较 2~3 个方案。");
        for (OptionScore score : report.rankings) {
            if (score.supportEvidence <= 0.1f && score.opposeEvidence <= 0.1f) report.warnings.add("方案“" + safeTitle(score.optionNode) + "”几乎没有证据节点支撑。");
            if (score.riskPenalty >= 2.5f) report.warnings.add("方案“" + safeTitle(score.optionNode) + "”风险累计偏高。");
        }
        if (report.rankingFlipsUnderSensitivity) report.warnings.add("当前排名对权重变化敏感，属于脆弱决策，建议增加证据或做小实验。");
    }

    public static void saveWeight(Node criterion, float weight) { if (criterion != null) GraphMetaHelper.putFloat(criterion, "decision_weight", Math.max(0.1f, weight)); }
    public static void saveScore(Node option, Node criterion, float score) { if (option != null && criterion != null) GraphMetaHelper.putFloat(option, "score_" + criterion.getId(), clamp(score, 0f, 10f)); }

    private static Set<String> collectConnected(Node anchor, Map<String, Connection> connections) {
        Set<String> ids = new LinkedHashSet<>(); ids.add(anchor.getId());
        if (connections == null) return ids;
        for (Connection c : connections.values()) {
            if (c == null) continue;
            if (anchor.getId().equals(c.getFromNodeId()) || anchor.getId().equals(c.getToNodeId())) {
                ids.add(c.getFromNodeId()); ids.add(c.getToNodeId());
            }
        }
        return ids;
    }

    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
    private static String safeTitle(Node node) { String title = node == null ? "" : node.getTitle(); return title == null || title.trim().isEmpty() ? "(无标题)" : title.trim(); }
}
