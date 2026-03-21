package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AiSelfReviewEngine {

    public static final class ReviewResult {
        public AiResponse response = new AiResponse();
        public final List<String> issues = new ArrayList<>();
        public int removedCount = 0;

        public String buildSummary() {
            if (removedCount <= 0 && issues.isEmpty()) {
                return "自我复审未拦截命令";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("自我复审拦截 ").append(removedCount).append(" 条命令");
            for (String issue : issues) sb.append("\n- ").append(issue);
            return sb.toString();
        }
    }

    private AiSelfReviewEngine() {}

    public static ReviewResult review(AiResponse source,
                                      Map<String, Node> nodes,
                                      Map<String, Connection> connections) {
        ReviewResult result = new ReviewResult();
        if (source == null) {
            result.response = new AiResponse();
            result.issues.add("AI 未返回响应");
            return result;
        }

        AiResponse cleaned = new AiResponse();
        cleaned.setAnswer(source.getAnswer());

        List<AiCommand> reviewed = new ArrayList<>();
        Set<String> seenSignatures = new HashSet<>();

        for (AiCommand cmd : source.getCommands()) {
            if (cmd == null) {
                result.removedCount++;
                result.issues.add("移除空命令");
                continue;
            }

            String action = lower(cmd.getAction());
            if (action.isEmpty()) {
                result.removedCount++;
                result.issues.add("移除缺少 action 的命令");
                continue;
            }

            String signature = buildSignature(cmd);
            if (!signature.isEmpty() && seenSignatures.contains(signature)) {
                result.removedCount++;
                result.issues.add("移除重复命令：" + action);
                continue;
            }

            if (!passesReferenceCheck(cmd, nodes, connections, result)) {
                result.removedCount++;
                continue;
            }

            if ("create_node".equals(action) && existsNodeWithSameTitle(nodes, cmd.getTitle())) {
                result.removedCount++;
                result.issues.add("移除重复创建节点：" + safe(cmd.getTitle()));
                continue;
            }

            if ("create_connection".equals(action) && existsSameConnection(connections, cmd)) {
                result.removedCount++;
                result.issues.add("移除重复创建连线：" + safe(cmd.getLabel()));
                continue;
            }

            if (looksTooVague(cmd)) {
                result.removedCount++;
                result.issues.add("移除过于空泛的命令：" + action);
                continue;
            }

            reviewed.add(cmd);
            if (!signature.isEmpty()) seenSignatures.add(signature);
        }

        ensureFocusNode(reviewed, nodes);
        cleaned.setCommands(reviewed);
        result.response = cleaned;
        return result;
    }

    private static boolean passesReferenceCheck(AiCommand cmd,
                                                Map<String, Node> nodes,
                                                Map<String, Connection> connections,
                                                ReviewResult result) {
        String action = lower(cmd.getAction());

        if ("update_node".equals(action) || "delete_node".equals(action) || "focus_node".equals(action)) {
            if (!safe(cmd.getNodeId()).isEmpty() && (nodes == null || !nodes.containsKey(cmd.getNodeId()))) {
                result.issues.add("移除引用不存在节点的命令：" + cmd.getNodeId());
                return false;
            }
        }

        if ("create_connection".equals(action) || "update_connection".equals(action)) {
            if (safe(cmd.getFromNodeId()).isEmpty() || safe(cmd.getToNodeId()).isEmpty()) {
                result.issues.add("移除缺少连线端点的命令");
                return false;
            }
        }

        return true;
    }

    private static boolean existsNodeWithSameTitle(Map<String, Node> nodes, String title) {
        String target = lower(title);
        if (target.isEmpty() || nodes == null) return false;
        for (Node node : nodes.values()) {
            if (node == null) continue;
            if (target.equals(lower(node.getTitle()))) return true;
        }
        return false;
    }

    private static boolean existsSameConnection(Map<String, Connection> connections, AiCommand cmd) {
        if (connections == null || cmd == null) return false;
        String from = safe(cmd.getFromNodeId());
        String to = safe(cmd.getToNodeId());
        String label = lower(cmd.getLabel());
        for (Connection c : connections.values()) {
            if (c == null) continue;
            if (from.equals(safe(c.getFromNodeId())) && to.equals(safe(c.getToNodeId()))) {
                String currentLabel = lower(c.getLabel());
                if (label.isEmpty() || label.equals(currentLabel)) return true;
            }
        }
        return false;
    }

    private static boolean looksTooVague(AiCommand cmd) {
        String action = lower(cmd.getAction());
        if ("create_node".equals(action) || "update_node".equals(action)) {
            String title = lower(cmd.getTitle());
            String content = lower(cmd.getContent());
            return title.isEmpty()
                    || "新节点".equals(title)
                    || "节点".equals(title)
                    || (title.length() <= 2 && content.isEmpty());
        }
        return false;
    }

    private static void ensureFocusNode(List<AiCommand> commands, Map<String, Node> nodes) {
        boolean hasFocus = false;
        for (AiCommand cmd : commands) {
            if (cmd != null && "focus_node".equalsIgnoreCase(cmd.getAction())) {
                hasFocus = true;
                break;
            }
        }
        if (hasFocus) return;

        for (AiCommand cmd : commands) {
            if (cmd == null) continue;
            String candidate = safe(cmd.getNodeId());
            if (!candidate.isEmpty()) {
                AiCommand focus = new AiCommand();
                focus.setAction("focus_node");
                focus.setNodeId(candidate);
                focus.setReason("自我复审补充焦点节点");
                commands.add(focus);
                return;
            }
            candidate = safe(cmd.getFromNodeId());
            if (!candidate.isEmpty()) {
                AiCommand focus = new AiCommand();
                focus.setAction("focus_node");
                focus.setNodeId(candidate);
                focus.setReason("自我复审补充焦点节点");
                commands.add(focus);
                return;
            }
        }

        if (nodes != null && !nodes.isEmpty()) {
            String anyNodeId = nodes.keySet().iterator().next();
            AiCommand focus = new AiCommand();
            focus.setAction("focus_node");
            focus.setNodeId(anyNodeId);
            focus.setReason("自我复审补充默认焦点节点");
            commands.add(focus);
        }
    }

    private static String buildSignature(AiCommand cmd) {
        if (cmd == null) return "";
        return lower(cmd.getAction()) + "|"
                + safe(cmd.getNodeId()) + "|"
                + safe(cmd.getFromNodeId()) + "|"
                + safe(cmd.getToNodeId()) + "|"
                + lower(cmd.getTitle()) + "|"
                + lower(cmd.getLabel());
    }

    private static String lower(String value) {
        return safe(value).toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
