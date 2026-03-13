package com.agui.neuralcanvas;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NodeUiTextFormatter {

    public static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static String safeTitle(Node node) {
        if (node == null) return "未命名节点";
        String title = safe(node.getTitle());
        return title.isEmpty() ? "未命名节点" : title;
    }

    public static String safeProjectTitle(Node node) {
        if (node == null) return "未命名项目";
        String title = safe(node.getTitle());
        return title.isEmpty() ? "未命名项目" : title;
    }

    public static String formatPercent(float current, float target) {
        if (target <= 0f) return "";
        float percent = (current / target) * 100f;
        if (percent < 0f) percent = 0f;
        return String.format(Locale.getDefault(), "%.0f%%", percent);
    }

    public static String buildInlineMeta(Node node) {
        if (node == null) return "";

        List<String> parts = new ArrayList<>();

        if (!safe(node.getDueAt()).isEmpty()) {
            parts.add("截止: " + node.getDueAt());
        }
        if (!safe(node.getReviewAt()).isEmpty()) {
            parts.add("复盘/复习: " + node.getReviewAt());
        }
        if (!safe(node.getTriggerCondition()).isEmpty()) {
            parts.add("触发: " + node.getTriggerCondition());
        }
        if (node.getPriority() > 0) {
            parts.add("优先级: " + node.getPriority());
        }
        if (node.getEffortEstimate() > 0f) {
            parts.add("预计耗时: " + node.getEffortEstimate() + "h");
        }
        if (node.getKrTarget() > 0f) {
            parts.add("KR: " + node.getKrCurrent() + " / " + node.getKrTarget()
                    + " (" + formatPercent(node.getKrCurrent(), node.getKrTarget()) + ")");
        }
        if (node.getType() == Node.NodeType.EVIDENCE) {
            parts.add("证据强度: " + node.getEvidenceStrength());
        }

        if (parts.isEmpty()) {
            String content = safe(node.getContent());
            if (!content.isEmpty()) {
                content = content.replace("\n", " ").replace("\r", " ").trim();
                if (content.length() > 32) {
                    content = content.substring(0, 32) + "…";
                }
                parts.add(content);
            }
        }

        return TextUtils.join(" ｜ ", parts);
    }

    public static String buildChipText(Node node) {
        if (node == null) return "• 未命名节点";

        String line = "• " + safeTitle(node) + " [" + node.getType().label + "]";
        List<String> extras = new ArrayList<>();

        if (!safe(node.getDueAt()).isEmpty()) extras.add("截止:" + node.getDueAt());
        if (!safe(node.getReviewAt()).isEmpty()) extras.add("复盘:" + node.getReviewAt());
        if (node.getPriority() > 0) extras.add("P" + node.getPriority());

        if (node.getKrTarget() > 0f) {
            extras.add("KR " + node.getKrCurrent() + "/" + node.getKrTarget()
                    + " (" + formatPercent(node.getKrCurrent(), node.getKrTarget()) + ")");
        }

        if (!extras.isEmpty()) {
            line += "\n  " + TextUtils.join(" ｜ ", extras);
        }

        return line;
    }
}
