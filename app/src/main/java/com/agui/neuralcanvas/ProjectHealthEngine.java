package com.agui.neuralcanvas;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ProjectHealthEngine {

    private ProjectHealthEngine() {}

    public static class ProjectHealthReport {
        public final List<Node> projects = new ArrayList<>();
        public final List<Node> stuckProjects = new ArrayList<>();
        public final List<Node> projectsWithoutKr = new ArrayList<>();
        public final List<Node> projectsWithoutReview = new ArrayList<>();
        public final List<Node> overdueActions = new ArrayList<>();
        public final List<Node> actionsWithoutTrigger = new ArrayList<>();
        public final List<Node> weakEvidenceDecisions = new ArrayList<>();
        public final List<Node> staleLearningNodes = new ArrayList<>();
    }

    public static ProjectHealthReport analyze(Map<String, Node> nodes,
                                              Map<String, Connection> connections) {
        ProjectHealthReport report = new ProjectHealthReport();
        if (nodes == null) return report;

        report.projects.addAll(WorkflowEngine.getProjectNodes(nodes));

        for (Node project : report.projects) {
            String ownerId = WorkflowEngine.resolveOwnerId(project);

            if (!WorkflowEngine.hasOpenChildAction(project, nodes, connections)) {
                report.stuckProjects.add(project);
            }

            if (!hasChildType(ownerId, nodes, Node.NodeType.KEY_RESULT)) {
                report.projectsWithoutKr.add(project);
            }

            if (!hasReviewCoverage(project, ownerId, nodes)) {
                report.projectsWithoutReview.add(project);
            }
        }

        for (Node node : nodes.values()) {
            if (node == null) continue;

            if (isOpenAction(node) && isOverdue(node.getDueAt())) {
                report.overdueActions.add(node);
            }

            if (isOpenAction(node) && WorkflowEngine.isBlank(node.getTriggerCondition())) {
                report.actionsWithoutTrigger.add(node);
            }

            if (isWeakEvidenceDecision(node, nodes)) {
                report.weakEvidenceDecisions.add(node);
            }

            if (isStaleLearningNode(node)) {
                report.staleLearningNodes.add(node);
            }
        }

        return report;
    }

    public static String buildSummary(ProjectHealthReport report) {
        if (report == null) return "暂无数据";
        StringBuilder sb = new StringBuilder();
        sb.append("项目总数：").append(report.projects.size());
        sb.append("\n卡住项目：").append(report.stuckProjects.size());
        sb.append("\n缺 KR 项目：").append(report.projectsWithoutKr.size());
        sb.append("\n缺复盘项目：").append(report.projectsWithoutReview.size());
        sb.append("\n逾期动作：").append(report.overdueActions.size());
        sb.append("\n无触发条件动作：").append(report.actionsWithoutTrigger.size());
        sb.append("\n高置信低证据决策：").append(report.weakEvidenceDecisions.size());
        sb.append("\n待安排复习的学习节点：").append(report.staleLearningNodes.size());
        return sb.toString();
    }

    private static boolean hasChildType(String ownerId,
                                        Map<String, Node> nodes,
                                        Node.NodeType type) {
        if (WorkflowEngine.isBlank(ownerId) || nodes == null || type == null) return false;
        for (Node node : nodes.values()) {
            if (node == null) continue;
            if (!ownerId.equals(node.getProjectId())) continue;
            if (node.getType() == type) return true;
        }
        return false;
    }

    private static boolean hasReviewCoverage(Node project,
                                             String ownerId,
                                             Map<String, Node> nodes) {
        if (project == null || nodes == null) return false;
        if (!WorkflowEngine.isBlank(project.getReviewAt())) return true;

        for (Node node : nodes.values()) {
            if (node == null) continue;
            if (!ownerId.equals(node.getProjectId())) continue;
            if (node.getType() == Node.NodeType.REVIEW) return true;
            if (!WorkflowEngine.isBlank(node.getReviewAt())) return true;
        }
        return false;
    }

    private static boolean isOpenAction(Node node) {
        if (node == null) return false;
        if (!WorkflowEngine.isActionable(node)) return false;
        return node.getStatus() != Node.NodeStatus.DONE
                && node.getStatus() != Node.NodeStatus.SOMEDAY
                && node.getStatus() != Node.NodeStatus.WAITING;
    }

    private static boolean isWeakEvidenceDecision(Node node, Map<String, Node> nodes) {
        if (node == null || nodes == null) return false;
        if (node.getType() != Node.NodeType.DECISION) return false;
        if (node.getConfidence() < 0.7f) return false;

        String ownerId = WorkflowEngine.resolveOwnerId(node);
        int evidenceCount = 0;
        float totalStrength = 0f;
        for (Node other : nodes.values()) {
            if (other == null) continue;
            if (other.getType() != Node.NodeType.EVIDENCE) continue;
            boolean sameOwner = ownerId.equals(other.getProjectId());
            boolean near = distance(node, other) <= 900f;
            if (!sameOwner && !near) continue;
            evidenceCount++;
            totalStrength += other.getEvidenceStrength();
        }

        if (evidenceCount == 0) return true;
        float avg = totalStrength / evidenceCount;
        return avg < 0.55f;
    }

    private static boolean isStaleLearningNode(Node node) {
        if (node == null) return false;
        if (!node.isLearningNode()) return false;
        if (node.getStatus() == Node.NodeStatus.DONE) return false;
        return WorkflowEngine.isBlank(node.getReviewAt());
    }

    private static double distance(Node a, Node b) {
        float dx = a.getX() - b.getX();
        float dy = a.getY() - b.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static boolean isOverdue(String dueAt) {
        Date date = parseDueDate(dueAt);
        if (date == null) return false;
        Date today = truncate(new Date());
        return date.before(today);
    }

    private static Date parseDueDate(String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() < 10) return null;
        String datePart = text.substring(0, 10);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setLenient(false);
        try {
            return sdf.parse(datePart);
        } catch (ParseException e) {
            return null;
        }
    }

    private static Date truncate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            return sdf.parse(sdf.format(date));
        } catch (ParseException e) {
            return date;
        }
    }
}
