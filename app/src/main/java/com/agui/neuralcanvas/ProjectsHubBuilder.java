package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProjectsHubBuilder {

    public static class ProjectGroup {
        public Node projectNode;
        public final List<Node> goals = new ArrayList<>();
        public final List<Node> actions = new ArrayList<>();
        public final List<Node> krs = new ArrayList<>();
        public final List<Node> risks = new ArrayList<>();
        public final List<Node> reviews = new ArrayList<>();
    }

    public static class ProjectsHubData {
        public final List<ProjectGroup> groups = new ArrayList<>();
    }

    public static ProjectsHubData build(Map<String, Node> allMap) {
        ProjectsHubData data = new ProjectsHubData();
        if (allMap == null || allMap.isEmpty()) return data;

        List<Node> allNodes = new ArrayList<>(allMap.values());
        List<Node> projects = new ArrayList<>();

        for (Node node : allNodes) {
            if (isProject(node)) {
                projects.add(node);
            }
        }

        sortProjects(projects);

        for (Node project : projects) {
            ProjectGroup group = new ProjectGroup();
            group.projectNode = project;

            String projectId = project.getId();

            for (Node node : allNodes) {
                if (node == null) continue;
                if (node.getId().equals(projectId)) continue;
                if (!belongsToProject(node, projectId)) continue;

                if (isGoal(node)) group.goals.add(node);
                else if (isKr(node)) group.krs.add(node);
                else if (isRisk(node)) group.risks.add(node);
                else if (isReview(node)) group.reviews.add(node);
                else if (isActionLike(node)) group.actions.add(node);
            }

            sortGoals(group.goals);
            sortActions(group.actions);
            sortKrs(group.krs);
            sortRisks(group.risks);
            sortReviews(group.reviews);

            data.groups.add(group);
        }

        return data;
    }

    public static boolean belongsToProject(Node node, String projectId) {
        if (node == null || projectId == null || projectId.trim().isEmpty()) return false;
        return projectId.equals(node.getProjectId());
    }

    public static boolean isProject(Node node) {
        return node != null && node.getType() == Node.NodeType.PROJECT;
    }

    public static boolean isGoal(Node node) {
        return node != null && node.getType() == Node.NodeType.GOAL;
    }

    public static boolean isActionLike(Node node) {
        return node != null &&
                (node.getType() == Node.NodeType.ACTION
                        || node.getType() == Node.NodeType.TASK
                        || node.getType() == Node.NodeType.ROUTINE
                        || node.getType() == Node.NodeType.TRIGGER);
    }

    public static boolean isKr(Node node) {
        return node != null && node.getType() == Node.NodeType.KEY_RESULT;
    }

    public static boolean isRisk(Node node) {
        return node != null &&
                (node.getType() == Node.NodeType.RISK
                        || node.getType() == Node.NodeType.OBSTACLE);
    }

    public static boolean isReview(Node node) {
        return node != null &&
                (node.getType() == Node.NodeType.REVIEW
                        || node.getStatus() == Node.NodeStatus.REVIEW);
    }

    public static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    public static String formatPercent(float current, float target) {
        if (target <= 0f) return "";
        float percent = (current / target) * 100f;
        if (percent < 0f) percent = 0f;
        return String.format(Locale.getDefault(), "%.0f%%", percent);
    }

    public static String buildNodeChipText(Node node) {
        String title = safe(node.getTitle());
        if (title.isEmpty()) title = "未命名节点";

        String line = "• " + title + " [" + node.getType().label + "]";

        List<String> extras = new ArrayList<>();
        if (!safe(node.getDueAt()).isEmpty()) extras.add("截止:" + node.getDueAt());
        if (!safe(node.getReviewAt()).isEmpty()) extras.add("复盘:" + node.getReviewAt());
        if (node.getPriority() > 0) extras.add("P" + node.getPriority());
        if (node.getKrTarget() > 0f) {
            extras.add("KR " + node.getKrCurrent() + "/" + node.getKrTarget()
                    + " (" + formatPercent(node.getKrCurrent(), node.getKrTarget()) + ")");
        }

        if (!extras.isEmpty()) {
            line += "\n  " + android.text.TextUtils.join(" ｜ ", extras);
        }

        return line;
    }

    private static void sortProjects(List<Node> list) {
        Collections.sort(list, new Comparator<Node>() {
            @Override
            public int compare(Node a, Node b) {
                int pa = a.getPriority();
                int pb = b.getPriority();
                int p = Integer.compare(pb, pa);
                if (p != 0) return p;
                return safe(a.getTitle()).compareToIgnoreCase(safe(b.getTitle()));
            }
        });
    }

    private static void sortGoals(List<Node> list) {
        Collections.sort(list, new Comparator<Node>() {
            @Override
            public int compare(Node a, Node b) {
                int p = Integer.compare(b.getPriority(), a.getPriority());
                if (p != 0) return p;
                return safe(a.getTitle()).compareToIgnoreCase(safe(b.getTitle()));
            }
        });
    }

    private static void sortActions(List<Node> list) {
        Collections.sort(list, new Comparator<Node>() {
            @Override
            public int compare(Node a, Node b) {
                int p = Integer.compare(b.getPriority(), a.getPriority());
                if (p != 0) return p;
                return safe(a.getTitle()).compareToIgnoreCase(safe(b.getTitle()));
            }
        });
    }

    private static void sortKrs(List<Node> list) {
        Collections.sort(list, new Comparator<Node>() {
            @Override
            public int compare(Node a, Node b) {
                float ap = a.getKrTarget() > 0f ? a.getKrCurrent() / a.getKrTarget() : -1f;
                float bp = b.getKrTarget() > 0f ? b.getKrCurrent() / b.getKrTarget() : -1f;
                return Float.compare(bp, ap);
            }
        });
    }

    private static void sortRisks(List<Node> list) {
        Collections.sort(list, new Comparator<Node>() {
            @Override
            public int compare(Node a, Node b) {
                int p = Integer.compare(b.getPriority(), a.getPriority());
                if (p != 0) return p;
                return safe(a.getTitle()).compareToIgnoreCase(safe(b.getTitle()));
            }
        });
    }

    private static void sortReviews(List<Node> list) {
        Collections.sort(list, new Comparator<Node>() {
            @Override
            public int compare(Node a, Node b) {
                return safe(a.getTitle()).compareToIgnoreCase(safe(b.getTitle()));
            }
        });
    }
}
