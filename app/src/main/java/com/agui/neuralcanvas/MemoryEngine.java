package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class MemoryEngine {
    public enum Grade { AGAIN, HARD, GOOD, EASY }
    public static class MemorySnapshot { public final List<Node> dueNodes = new ArrayList<>(); public final List<Node> upcomingNodes = new ArrayList<>(); }
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private MemoryEngine() {}

    public static MemorySnapshot build(Map<String, Node> nodes) {
        MemorySnapshot snapshot = new MemorySnapshot();
        if (nodes == null) return snapshot;
        long now = System.currentTimeMillis();
        for (Node node : nodes.values()) {
            if (node == null || !node.isLearningNode()) continue;
            long due = getDueAt(node);
            if (due <= now) snapshot.dueNodes.add(node); else snapshot.upcomingNodes.add(node);
        }
        sort(snapshot.dueNodes); sort(snapshot.upcomingNodes); return snapshot;
    }

    public static long getDueAt(Node node) {
        long due = GraphMetaHelper.getLong(node, "memory_due_at", 0L);
        if (due <= 0L) {
            due = System.currentTimeMillis();
            GraphMetaHelper.putLong(node, "memory_due_at", due);
            GraphMetaHelper.putInt(node, "memory_reviews", GraphMetaHelper.getInt(node, "memory_reviews", 0));
            GraphMetaHelper.putFloat(node, "memory_ease", Math.max(1.3f, GraphMetaHelper.getFloat(node, "memory_ease", 2.5f)));
            GraphMetaHelper.putInt(node, "memory_interval_days", Math.max(0, GraphMetaHelper.getInt(node, "memory_interval_days", 0)));
        }
        return due;
    }

    public static String getStatsText(Node node) {
        int reviews = GraphMetaHelper.getInt(node, "memory_reviews", 0);
        int lapses = GraphMetaHelper.getInt(node, "memory_lapses", 0);
        int interval = GraphMetaHelper.getInt(node, "memory_interval_days", 0);
        float ease = GraphMetaHelper.getFloat(node, "memory_ease", 2.5f);
        return "复习次数=" + reviews + "｜遗忘次数=" + lapses + "｜当前间隔=" + interval + "天｜易度=" + round(ease);
    }

    public static void review(Node node, Grade grade) {
        if (node == null || grade == null) return;
        int reviews = GraphMetaHelper.getInt(node, "memory_reviews", 0) + 1;
        int lapses = GraphMetaHelper.getInt(node, "memory_lapses", 0);
        int intervalDays = GraphMetaHelper.getInt(node, "memory_interval_days", 0);
        float ease = GraphMetaHelper.getFloat(node, "memory_ease", 2.5f);
        switch (grade) {
            case AGAIN: intervalDays = 0; ease = Math.max(1.3f, ease - 0.2f); lapses += 1; break;
            case HARD: intervalDays = Math.max(1, intervalDays == 0 ? 1 : Math.round(intervalDays * 1.2f)); ease = Math.max(1.3f, ease - 0.05f); break;
            case GOOD: if (intervalDays <= 0) intervalDays = 1; else if (intervalDays == 1) intervalDays = 3; else intervalDays = Math.max(intervalDays + 1, Math.round(intervalDays * ease)); break;
            case EASY: if (intervalDays <= 0) intervalDays = 3; else intervalDays = Math.max(intervalDays + 2, Math.round(intervalDays * (ease + 0.3f))); ease += 0.1f; break;
        }
        long nextDue = System.currentTimeMillis() + (intervalDays <= 0 ? 10L * 60L * 1000L : intervalDays * DAY_MS);
        GraphMetaHelper.putInt(node, "memory_reviews", reviews);
        GraphMetaHelper.putInt(node, "memory_lapses", lapses);
        GraphMetaHelper.putInt(node, "memory_interval_days", intervalDays);
        GraphMetaHelper.putFloat(node, "memory_ease", ease);
        GraphMetaHelper.putLong(node, "memory_due_at", nextDue);
        node.setReviewAt(String.valueOf(nextDue));
    }

    private static void sort(List<Node> list) { Collections.sort(list, new Comparator<Node>() { @Override public int compare(Node a, Node b) { return Long.compare(getDueAt(a), getDueAt(b)); } }); }
    private static String round(float value) { return String.format(java.util.Locale.US, "%.2f", value); }
}
