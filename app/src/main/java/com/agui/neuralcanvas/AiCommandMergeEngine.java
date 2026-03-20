package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AiCommandMergeEngine {

    private AiCommandMergeEngine() {}

    public static AiResponse merge(List<AiAgentRunResult> runs) {
        AiResponse merged = new AiResponse();
        if (runs == null || runs.isEmpty()) {
            merged.setAnswer("没有代理结果");
            return merged;
        }

        StringBuilder answer = new StringBuilder();
        Map<String, AiCommand> commandMap = new LinkedHashMap<>();
        AiCommand focusCandidate = null;

        for (AiAgentRunResult run : runs) {
            if (run == null || run.response == null) continue;

            String part = safe(run.response.getAnswer());
            if (!part.isEmpty()) {
                if (answer.length() > 0) answer.append("\n");
                answer.append("[").append(run.buildLabel()).append("] ").append(part);
            }

            for (AiCommand cmd : run.response.getCommands()) {
                if (cmd == null) continue;
                String action = safeLower(cmd.getAction());

                if ("focus_node".equals(action)) {
                    focusCandidate = cmd;
                    continue;
                }

                String key = buildKey(cmd);
                if (commandMap.containsKey(key)) {
                    commandMap.put(key, mergeCommand(commandMap.get(key), cmd));
                } else {
                    commandMap.put(key, cmd);
                }
            }
        }

        List<AiCommand> commands = new ArrayList<>(commandMap.values());
        if (focusCandidate != null) {
            commands.add(focusCandidate);
        }

        merged.setAnswer(answer.length() == 0 ? "已完成多代理协作分析" : answer.toString());
        merged.setCommands(commands);
        return merged;
    }

    private static String buildKey(AiCommand cmd) {
        String action = safeLower(cmd.getAction());

        if ("create_node".equals(action)) {
            return action + "|" + safeLower(cmd.getTitle()) + "|" + safeLower(cmd.getType());
        }
        if ("update_node".equals(action)) {
            return action + "|" + safe(cmd.getNodeId());
        }
        if ("create_connection".equals(action) || "update_connection".equals(action)) {
            return action + "|" + safe(cmd.getFromNodeId()) + "|" + safe(cmd.getToNodeId()) + "|" + safeLower(cmd.getLabel());
        }
        if ("delete_node".equals(action) || "focus_node".equals(action)) {
            return action + "|" + safe(cmd.getNodeId());
        }
        if ("delete_connection".equals(action)) {
            return action + "|" + safe(cmd.getFromNodeId()) + "|" + safe(cmd.getToNodeId());
        }
        return action + "|" + safe(cmd.getNodeId()) + "|" + safe(cmd.getTempId()) + "|" + safe(cmd.getTitle());
    }

    private static AiCommand mergeCommand(AiCommand a, AiCommand b) {
        AiCommand out = a;
        if (isBlank(out.getContent()) && !isBlank(b.getContent())) out.setContent(b.getContent());
        if (isBlank(out.getReason()) && !isBlank(b.getReason())) out.setReason(b.getReason());
        if (isBlank(out.getLabel()) && !isBlank(b.getLabel())) out.setLabel(b.getLabel());
        if (out.getStrokeWidth() == null && b.getStrokeWidth() != null) out.setStrokeWidth(b.getStrokeWidth());
        if (out.getWidth() == null && b.getWidth() != null) out.setWidth(b.getWidth());
        if (out.getHeight() == null && b.getHeight() != null) out.setHeight(b.getHeight());
        if (out.getX() == null && b.getX() != null) out.setX(b.getX());
        if (out.getY() == null && b.getY() != null) out.setY(b.getY());
        if (isBlank(out.getType()) && !isBlank(b.getType())) out.setType(b.getType());
        if (isBlank(out.getShape()) && !isBlank(b.getShape())) out.setShape(b.getShape());
        return out;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return safe(value).isEmpty();
    }
}
