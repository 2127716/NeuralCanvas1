package com.agui.neuralcanvas;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

public final class FocusSessionEngine {
    public static class SessionInfo { public String nodeId = ""; public String nodeTitle = ""; public long startedAt = 0L; public int plannedMinutes = 25; public int interruptions = 0; }
    private static final String PREFS = "neural_focus_session";
    private static final String KEY_NODE_ID = "node_id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_STARTED_AT = "started_at";
    private static final String KEY_MINUTES = "minutes";
    private static final String KEY_INTERRUPTS = "interrupts";
    private FocusSessionEngine() {}

    public static void start(Context context, Node node, int plannedMinutes) {
        if (context == null || node == null) return;
        SharedPreferences.Editor editor = prefs(context).edit();
        editor.putString(KEY_NODE_ID, node.getId()); editor.putString(KEY_TITLE, safeTitle(node)); editor.putLong(KEY_STARTED_AT, System.currentTimeMillis()); editor.putInt(KEY_MINUTES, Math.max(5, plannedMinutes)); editor.putInt(KEY_INTERRUPTS, 0); editor.apply();
    }

    public static SessionInfo getCurrent(Context context) {
        SharedPreferences sp = prefs(context); long started = sp.getLong(KEY_STARTED_AT, 0L); if (started <= 0L) return null;
        SessionInfo info = new SessionInfo(); info.nodeId = sp.getString(KEY_NODE_ID, ""); info.nodeTitle = sp.getString(KEY_TITLE, ""); info.startedAt = started; info.plannedMinutes = sp.getInt(KEY_MINUTES, 25); info.interruptions = sp.getInt(KEY_INTERRUPTS, 0); return info;
    }

    public static void interrupt(Context context) { SessionInfo info = getCurrent(context); if (info != null) prefs(context).edit().putInt(KEY_INTERRUPTS, info.interruptions + 1).apply(); }

    public static float finish(Context context, Map<String, Node> nodes, boolean completed) {
        SessionInfo info = getCurrent(context); if (info == null) return 0f;
        long elapsedMs = Math.max(0L, System.currentTimeMillis() - info.startedAt); float hours = elapsedMs / 3600000f;
        Node node = nodes == null ? null : nodes.get(info.nodeId);
        if (node != null) {
            node.setActualEffort(node.getActualEffort() + hours);
            GraphMetaHelper.putInt(node, "focus_sessions", GraphMetaHelper.getInt(node, "focus_sessions", 0) + 1);
            GraphMetaHelper.putLong(node, "focus_total_ms", GraphMetaHelper.getLong(node, "focus_total_ms", 0L) + elapsedMs);
            GraphMetaHelper.putInt(node, "focus_interruptions", GraphMetaHelper.getInt(node, "focus_interruptions", 0) + info.interruptions);
            GraphMetaHelper.putBoolean(node, "focus_last_completed", completed);
            if (node.getEffortEstimate() > 0f) GraphMetaHelper.putFloat(node, "effort_ratio", node.getActualEffort() / Math.max(0.01f, node.getEffortEstimate()));
        }
        clear(context); return hours;
    }

    public static String getNodeStats(Node node) {
        int sessions = GraphMetaHelper.getInt(node, "focus_sessions", 0); long totalMs = GraphMetaHelper.getLong(node, "focus_total_ms", 0L); int interruptions = GraphMetaHelper.getInt(node, "focus_interruptions", 0); float ratio = GraphMetaHelper.getFloat(node, "effort_ratio", 0f);
        return "专注次数=" + sessions + "｜总专注=" + (totalMs / 60000L) + "分钟｜中断=" + interruptions + "｜估时比=" + (ratio <= 0f ? "-" : String.format(java.util.Locale.US, "%.2f", ratio));
    }

    public static String getTriggerStats(Node node) {
        int hits = GraphMetaHelper.getInt(node, "trigger_hits", 0); int misses = GraphMetaHelper.getInt(node, "trigger_misses", 0); int total = hits + misses; float rate = total == 0 ? 0f : (float) hits / total;
        return "触发命中=" + hits + "｜触发落空=" + misses + "｜命中率=" + (total == 0 ? "-" : String.format(java.util.Locale.US, "%.1f%%", rate * 100f));
    }

    public static void markTrigger(Node node, boolean hit) { if (node == null) return; if (hit) GraphMetaHelper.putInt(node, "trigger_hits", GraphMetaHelper.getInt(node, "trigger_hits", 0) + 1); else GraphMetaHelper.putInt(node, "trigger_misses", GraphMetaHelper.getInt(node, "trigger_misses", 0) + 1); }
    private static void clear(Context context) { prefs(context).edit().clear().apply(); }
    private static SharedPreferences prefs(Context context) { return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    private static String safeTitle(Node node) { String title = node == null ? "" : node.getTitle(); return title == null || title.trim().isEmpty() ? "(无标题)" : title.trim(); }
}
