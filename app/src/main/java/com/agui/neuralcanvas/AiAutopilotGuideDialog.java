package com.agui.neuralcanvas;

import androidx.appcompat.app.AlertDialog;

public final class AiAutopilotGuideDialog {
    private AiAutopilotGuideDialog() {}

    public static void show(MainActivity activity, BrainPendingGuidance guidance) {
        if (activity == null || guidance == null) return;

        PendingOperationBundle pending = activity.getDataManager() == null
                ? null : activity.getDataManager().loadPendingOperationBundle();

        StringBuilder message = new StringBuilder();
        message.append(buildCompactSummary(guidance));

        if (guidance.riskLevel != null && !guidance.riskLevel.trim().isEmpty()) {
            message.append("\n\n风险等级：").append(guidance.riskLevel);
        }
        if (guidance.autoApplied) {
            message.append("\n已自动执行低风险改动");
        }
        if (pending != null && pending.commandCount > 0) {
            message.append("\n待确认改动：").append(pending.commandCount).append(" 条");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle("AI 自动巡航")
                .setMessage(message.toString())
                .setNegativeButton("关闭", null)
                .setPositiveButton("定位节点", (d, w) -> {
                    if (guidance.focusNodeId != null && !guidance.focusNodeId.trim().isEmpty()) {
                        activity.getMindMapView().focusNodeById(guidance.focusNodeId);
                        activity.getMindMapView().selectNodeById(guidance.focusNodeId);
                    }
                });

        if (pending != null && pending.commandCount > 0) {
            builder.setNeutralButton("确认改动", (d, w) ->
                    OperationApprovalDialog.newInstance()
                            .show(activity.getSupportFragmentManager(), "operation_approval_dialog"));
        } else if (guidance.responseJson != null
                && !guidance.responseJson.trim().isEmpty()
                && !guidance.autoApplied) {
            builder.setNeutralButton("查看改动", (d, w) ->
                    AiCommandPreviewDialog.newInstanceFromJson(guidance.responseJson)
                            .show(activity.getSupportFragmentManager(), "ai_command_preview"));
        }

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                bg.setColor(ThemeManager.getDialogBg());
                bg.setCornerRadius(dp(activity, 26));
                bg.setStroke(dp(activity, 1), ThemeManager.getStroke());
                dialog.getWindow().setBackgroundDrawable(bg);
            }
        });
        dialog.show();
    }

    private static String buildCompactSummary(BrainPendingGuidance guidance) {
        String focus = safe(guidance.focusNodeTitle);
        String mode = safe(guidance.mode);
        StringBuilder sb = new StringBuilder();
        if (!focus.isEmpty()) {
            sb.append("当前焦点：").append(focus);
        } else {
            sb.append("系统已完成一次自动巡航");
        }

        if (!mode.isEmpty()) {
            sb.append("\n建议模式：").append(mode);
        }

        String raw = safe(guidance.summary);
        String shortPart = firstUsefulChunk(raw);
        if (!shortPart.isEmpty()) {
            sb.append("\n\n").append(shortPart);
        }
        return sb.toString();
    }

    private static String firstUsefulChunk(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        String cleaned = raw.replace("\r", "").trim();
        String[] blocks = cleaned.split("\n\n");
        for (String block : blocks) {
            String b = block.trim();
            if (b.isEmpty()) continue;
            if (b.startsWith("【")) continue;
            if (b.length() > 220) return b.substring(0, 220) + "…";
            return b;
        }
        return cleaned.length() > 220 ? cleaned.substring(0, 220) + "…" : cleaned;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static int dp(MainActivity activity, int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density);
    }
}
