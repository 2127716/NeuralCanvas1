
package com.agui.neuralcanvas;

import androidx.appcompat.app.AlertDialog;

public final class AiAutopilotGuideDialog {

    private AiAutopilotGuideDialog() {}

    public static void show(MainActivity activity, BrainPendingGuidance.Guidance guidance) {
        if (activity == null || guidance == null) return;

        StringBuilder message = new StringBuilder();
        if (guidance.reason != null && !guidance.reason.trim().isEmpty()) {
            message.append(guidance.reason.trim());
        } else {
            message.append("AI 已完成一轮自动巡航。");
        }

        if (guidance.riskLevel != null && !guidance.riskLevel.trim().isEmpty()) {
            message.append("\n\n风险等级：").append(guidance.riskLevel);
        }
        if (guidance.autoApplied) {
            message.append("\n\n已自动执行低风险改动。你现在最该看的是定位到的关键节点。");
        } else if (guidance.responseJson != null && !guidance.responseJson.trim().isEmpty()) {
            message.append("\n\nAI 已准备好建议改动，你可以先看后执行。");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle("AI 自动巡航")
                .setMessage(message.toString())
                .setNegativeButton("关闭", null);

        if (guidance.focusNodeId != null && !guidance.focusNodeId.trim().isEmpty()) {
            builder.setPositiveButton("定位节点", (d, w) -> {
                activity.getMindMapView().focusNodeById(guidance.focusNodeId);
                activity.getMindMapView().selectNodeById(guidance.focusNodeId);
            });
        }

        if (guidance.responseJson != null && !guidance.responseJson.trim().isEmpty()) {
            builder.setNeutralButton("查看建议", (d, w) -> {
                try {
                    AiResponse response = AiJsonParser.parseResponse(guidance.responseJson);
                    AiCommandPreviewDialog.newInstance(response)
                            .show(activity.getSupportFragmentManager(), "ai_command_preview");
                } catch (Exception ignored) {
                }
            });
        }

        builder.show();
    }
}
