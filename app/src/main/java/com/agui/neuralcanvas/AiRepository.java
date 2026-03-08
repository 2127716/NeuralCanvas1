package com.agui.neuralcanvas;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiRepository {

    public interface AiCallback {
        void onSuccess(AiResponse response);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess(String message);
        void onError(String message);
    }

    public static class PreparedRequest {
        public AiGraphSnapshot snapshot;
        public String finalPrompt;
        public boolean layoutAllowed;
    }

    private static final String TAG = "AiRepository";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    public PreparedRequest prepareRelevantRequest(
            Map<String, Node> nodes,
            Map<String, Connection> connections,
            String userMessage,
            boolean forceFullGraph
    ) {
        PreparedRequest prepared = new PreparedRequest();
        prepared.layoutAllowed = containsLayoutIntent(userMessage);

        if (forceFullGraph || nodes == null || nodes.isEmpty()) {
            prepared.snapshot = AiGraphSnapshot.from(nodes, connections);
            prepared.finalPrompt = safeText(userMessage);
            return prepared;
        }

        AiGraphSnapshot full = AiGraphSnapshot.from(nodes, connections);
        AiGraphSnapshot relevant = selectRelevantSubgraph(full, userMessage);

        int fullNodeCount = full.nodes == null ? 0 : full.nodes.size();
        int relevantNodeCount = relevant.nodes == null ? 0 : relevant.nodes.size();

        prepared.snapshot = relevantNodeCount > 0 ? relevant : full;

        if (relevantNodeCount > 0 && relevantNodeCount < fullNodeCount) {
            prepared.finalPrompt =
                    "【系统已为你筛选与当前问题更相关的子图，不是整张图。若你判断仍需全图，请基于当前子图先回答，再尽量保守。】\n"
                            + safeText(userMessage);
        } else {
            prepared.finalPrompt = safeText(userMessage);
        }

        return prepared;
    }

    public void askGraph(
            AiConfig config,
            AiGraphSnapshot snapshot,
            String userMessage,
            boolean layoutAllowed,
            AiCallback callback
    ) {
        try {
            if (config == null || !config.isEnabled()) {
                callback.onError("AI配置不完整");
                return;
            }

            String graphJson = AiJsonParser.toJson(snapshot);

            int nodeCount = snapshot == null || snapshot.nodes == null ? 0 : snapshot.nodes.size();
            int connectionCount = snapshot == null || snapshot.connections == null ? 0 : snapshot.connections.size();

            String systemPrompt =
                    "你是 NeuralCanvas 的图谱编辑 AI。\n" +
                    "你能完整读取当前传给你的节点与连线，包括：\n" +
                    "1. 节点 id、标题、内容、类型、形状、坐标、尺寸\n" +
                    "2. 连线 id、起点节点、终点节点、关系类型、文字标签、粗细、颜色\n" +
                    "3. 箭头方向表示关系方向，fromNodeId 指向 toNodeId\n\n" +

                    "你的工作方式必须像真正会操作图谱的软件助手，而不是空谈。\n" +
                    "用户要求改图时，优先输出 commands，而不是只解释。\n\n" +

                    "你可执行的操作包括：\n" +
                    "- 分析图谱结构\n" +
                    "- 总结、提炼、补充节点\n" +
                    "- 创建、修改、删除节点\n" +
                    "- 创建、修改、删除连线\n" +
                    "- 设置连线文字、关系类型、粗细、颜色\n" +
                    "- 聚焦某节点\n" +
                    "- 在明确需要时执行自动布局\n\n" +

                    "你必须严格输出 JSON，不允许输出 markdown，不允许输出解释性前缀。\n" +
                    "输出格式固定为：\n" +
                    "{\n" +
                    "  \"answer\": \"给用户看的简洁说明，说明你如何理解图谱和准备做什么\",\n" +
                    "  \"commands\": [\n" +
                    "    {\n" +
                    "      \"action\": \"create_node|update_node|delete_node|create_connection|update_connection|delete_connection|focus_node|auto_layout\",\n" +
                    "      \"tempId\": \"仅 create_node 时可填，例如 n1、task_a\",\n" +
                    "      \"nodeId\": \"已有节点 id；若引用新创建节点，也可直接写 tempId\",\n" +
                    "      \"fromNodeId\": \"已有节点 id 或 tempId\",\n" +
                    "      \"toNodeId\": \"已有节点 id 或 tempId\",\n" +
                    "      \"title\": \"节点标题\",\n" +
                    "      \"content\": \"节点内容\",\n" +
                    "      \"type\": \"CONCEPT|IDEA|QUESTION|RESOURCE|TASK|GOAL|NOTE|DECISION\",\n" +
                    "      \"shape\": \"RECT|CIRCLE|OVAL|DIAMOND|TRIANGLE|PENTAGON|HEXAGON\",\n" +
                    "      \"label\": \"连线文字\",\n" +
                    "      \"connectionType\": \"SEQUENCE|PARALLEL|BLOCKING|DEPENDENCY|REFERENCE\",\n" +
                    "      \"connectionColorHex\": \"如 #67B7FF\",\n" +
                    "      \"x\": 0,\n" +
                    "      \"y\": 0,\n" +
                    "      \"width\": 168,\n" +
                    "      \"height\": 168,\n" +
                    "      \"strokeWidth\": 4,\n" +
                    "      \"reason\": \"这条命令的目的\",\n" +
                    "      \"applyAutoLayoutAfter\": false\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}\n\n" +

                    "强规则：\n" +
                    "- 用户要求修改图谱时，优先输出 commands\n" +
                    "- 创建多个新节点并互相连线时，必须给每个新节点分配 tempId，然后后续命令用 tempId 引用\n" +
                    "- 连线颜色必须用 connectionColorHex 输出，格式为 #RRGGBB\n" +
                    "- 连线粗细用 strokeWidth 输出，推荐范围 2 到 10\n" +
                    "- 不确定时尽量保守，不要大量删除已有节点\n" +
                    "- 节点标题应简洁，内容可稍详细\n" +
                    "- 连线文字要体现关系语义，不要总是空白\n" +
                    "- 若用户只是问答，不需要改图谱，则 commands 返回空数组\n" +
                    "- 不要为了凑结构而创建无关紧要的连线\n" +
                    "- 除非用户明确提出“布局、重排、整理布局、重新排列、排版”之类要求，否则不要输出 auto_layout，也不要把 applyAutoLayoutAfter 设为 true\n";

            String userPrompt =
                    "当前图谱概况：节点 " + nodeCount + " 个，连线 " + connectionCount + " 条。\n" +
                    "当前图谱 JSON：\n" + graphJson + "\n\n" +
                    "用户请求：\n" + safeText(userMessage);

            JSONObject body = new JSONObject();
            body.put("model", config.getModel());

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
            messages.put(new JSONObject().put("role", "user").put("content", userPrompt));

            body.put("messages", messages);
            body.put("temperature", 0.08);
            body.put("top_p", 0.9);

            String url = normalizeChatCompletionsUrl(config.getBaseUrl());

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            Log.d(TAG, "请求URL=" + url);
            Log.d(TAG, "请求模型=" + config.getModel());
            Log.d(TAG, "图谱节点数=" + nodeCount + ", 连线数=" + connectionCount);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "AI请求失败", e);
                    callback.onError("AI请求失败：" + e.getClass().getSimpleName() + " - " + safeText(e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String respBody = response.body() == null ? "" : response.body().string();

                    if (!response.isSuccessful()) {
                        Log.e(TAG, "AI接口错误 code=" + response.code() + ", body=" + respBody);
                        callback.onError("AI接口错误：" + response.code() + formatErrorBody(respBody));
                        return;
                    }

                    try {
                        String content = extractAssistantContent(respBody);
                        if (content.trim().isEmpty()) {
                            callback.onError("AI未返回有效内容");
                            return;
                        }

                        AiResponse parsed = AiJsonParser.parseResponse(stripMarkdownCodeFence(content));
                        sanitizeResponse(parsed, layoutAllowed);
                        callback.onSuccess(parsed);

                    } catch (Exception e) {
                        Log.e(TAG, "AI响应解析失败", e);
                        callback.onError("AI响应解析失败：" + e.getClass().getSimpleName() + " - " + safeText(e.getMessage()));
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "请求构建失败", e);
            callback.onError("请求构建失败：" + e.getClass().getSimpleName() + " - " + safeText(e.getMessage()));
        }
    }

    public void testConnection(AiConfig config, SimpleCallback callback) {
        try {
            if (config == null || !config.isEnabled()) {
                callback.onError("AI配置不完整");
                return;
            }

            JSONObject body = new JSONObject();
            body.put("model", config.getModel());

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", "你是一个接口连通性测试助手，只返回纯文本 ok"));
            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", "请回复 ok"));
            body.put("messages", messages);
            body.put("temperature", 0);

            String url = normalizeChatCompletionsUrl(config.getBaseUrl());

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("测试失败：" + e.getClass().getSimpleName() + " - " + safeText(e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String respBody = response.body() == null ? "" : response.body().string();

                    if (!response.isSuccessful()) {
                        callback.onError("测试失败：" + response.code() + formatErrorBody(respBody));
                        return;
                    }

                    try {
                        String content = extractAssistantContent(respBody);
                        callback.onSuccess("连接成功：" + safeText(content));
                    } catch (Exception e) {
                        callback.onSuccess("连接成功，但返回内容解析异常，接口大概率可用");
                    }
                }
            });

        } catch (Exception e) {
            callback.onError("测试失败：" + e.getClass().getSimpleName() + " - " + safeText(e.getMessage()));
        }
    }

    private AiGraphSnapshot selectRelevantSubgraph(AiGraphSnapshot full, String userMessage) {
        if (full == null || full.nodes == null || full.nodes.isEmpty()) {
            return full == null ? new AiGraphSnapshot() : full;
        }

        List<String> keywords = tokenize(userMessage);
        if (keywords.isEmpty()) {
            return full;
        }

        Map<String, Integer> scoreMap = new LinkedHashMap<>();
        Set<String> keepNodeIds = new LinkedHashSet<>();

        for (AiGraphSnapshot.SnapshotNode node : full.nodes) {
            int score = scoreNode(node, keywords);
            if (score > 0) {
                scoreMap.put(node.id, score);
            }
        }

        if (scoreMap.isEmpty()) {
            return full;
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(scoreMap.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        int limit = Math.min(Math.max(8, sorted.size()), 18);
        for (int i = 0; i < sorted.size() && i < limit; i++) {
            keepNodeIds.add(sorted.get(i).getKey());
        }

        for (AiGraphSnapshot.SnapshotConnection c : full.connections) {
            if (keepNodeIds.contains(c.fromNodeId) || keepNodeIds.contains(c.toNodeId)) {
                keepNodeIds.add(c.fromNodeId);
                keepNodeIds.add(c.toNodeId);
            }
        }

        AiGraphSnapshot result = new AiGraphSnapshot();
        for (AiGraphSnapshot.SnapshotNode node : full.nodes) {
            if (keepNodeIds.contains(node.id)) {
                result.nodes.add(node);
            }
        }
        for (AiGraphSnapshot.SnapshotConnection c : full.connections) {
            if (keepNodeIds.contains(c.fromNodeId) && keepNodeIds.contains(c.toNodeId)) {
                result.connections.add(c);
            }
        }

        return result.nodes.isEmpty() ? full : result;
    }

    private int scoreNode(AiGraphSnapshot.SnapshotNode node, List<String> keywords) {
        String title = safeText(node.title).toLowerCase(Locale.ROOT);
        String content = safeText(node.content).toLowerCase(Locale.ROOT);
        String type = safeText(node.type).toLowerCase(Locale.ROOT);

        int score = 0;
        for (String kw : keywords) {
            if (kw.length() < 2) continue;

            if (title.contains(kw)) score += 6;
            if (content.contains(kw)) score += 3;
            if (type.contains(kw)) score += 2;
        }

        return score;
    }

    private List<String> tokenize(String text) {
        List<String> result = new ArrayList<>();
        if (text == null) return result;

        String normalized = text
                .replace("，", " ")
                .replace("。", " ")
                .replace("：", " ")
                .replace("；", " ")
                .replace(",", " ")
                .replace(".", " ")
                .replace(":", " ")
                .replace(";", " ")
                .replace("\n", " ")
                .toLowerCase(Locale.ROOT);

        String[] parts = normalized.split("\\s+");
        for (String p : parts) {
            String s = p.trim();
            if (s.length() >= 2) {
                result.add(s);
            }
        }
        return result;
    }

    private void sanitizeResponse(AiResponse response, boolean layoutAllowed) {
        if (response == null || response.getCommands() == null) return;

        List<AiCommand> cleaned = new ArrayList<>();
        for (AiCommand cmd : response.getCommands()) {
            if (cmd == null) continue;

            String action = safeLower(cmd.getAction());

            if (!layoutAllowed) {
                if ("auto_layout".equals(action)) {
                    continue;
                }
                if (Boolean.TRUE.equals(cmd.getApplyAutoLayoutAfter())) {
                    cmd.setApplyAutoLayoutAfter(false);
                }
            }

            // 限制离谱线宽
            if (cmd.getStrokeWidth() != null) {
                float w = cmd.getStrokeWidth();
                if (w < 2f) cmd.setStrokeWidth(2f);
                if (w > 10f) cmd.setStrokeWidth(10f);
            }

            cleaned.add(cmd);
        }
        response.setCommands(cleaned);
    }

    private boolean containsLayoutIntent(String text) {
        String s = safeLower(text);
        return s.contains("布局")
                || s.contains("重排")
                || s.contains("整理")
                || s.contains("排列")
                || s.contains("排版")
                || s.contains("重新排列")
                || s.contains("自动布局");
    }

    private String normalizeChatCompletionsUrl(String baseUrl) {
        String url = baseUrl == null ? "" : baseUrl.trim();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (url.endsWith("/chat/completions")) return url;
        if (url.endsWith("/v1")) return url + "/chat/completions";
        return url + "/v1/chat/completions";
    }

    private String extractAssistantContent(String responseJson) throws Exception {
        JSONObject obj = new JSONObject(responseJson);
        JSONArray choices = obj.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            return "";
        }

        JSONObject first = choices.optJSONObject(0);
        if (first == null) return "";

        JSONObject message = first.optJSONObject("message");
        if (message == null) return "";

        Object contentObj = message.opt("content");
        if (contentObj == null) return "";

        if (contentObj instanceof String) {
            return (String) contentObj;
        }

        if (contentObj instanceof JSONArray) {
            JSONArray arr = (JSONArray) contentObj;
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < arr.length(); i++) {
                Object item = arr.opt(i);
                if (item instanceof JSONObject) {
                    JSONObject part = (JSONObject) item;

                    if (part.has("text")) {
                        sb.append(part.optString("text", ""));
                    } else if (part.has("content")) {
                        sb.append(part.optString("content", ""));
                    } else if (part.has("value")) {
                        sb.append(part.optString("value", ""));
                    }
                } else if (item != null) {
                    sb.append(String.valueOf(item));
                }
            }
            return sb.toString();
        }

        return String.valueOf(contentObj);
    }

    private String stripMarkdownCodeFence(String text) {
        if (text == null) return "";
        String trimmed = text.trim();

        if (trimmed.startsWith("```")) {
            int firstNewLine = trimmed.indexOf('\n');
            if (firstNewLine >= 0) {
                trimmed = trimmed.substring(firstNewLine + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }

        return trimmed.trim();
    }

    private String formatErrorBody(String respBody) {
        if (respBody == null || respBody.trim().isEmpty()) return "";

        String cleaned = respBody.trim();
        if (cleaned.length() > 300) {
            cleaned = cleaned.substring(0, 300) + "…";
        }
        return "\n" + cleaned;
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }

    private String safeLower(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }
}
