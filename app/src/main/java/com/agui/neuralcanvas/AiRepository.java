package com.agui.neuralcanvas;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
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

    private static final String TAG = "AiRepository";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .build();

    public void askGraph(
            AiConfig config,
            Map<String, Node> nodes,
            Map<String, Connection> connections,
            String userMessage,
            AiCallback callback
    ) {
        try {
            if (config == null || !config.isEnabled()) {
                callback.onError("AI配置不完整");
                return;
            }

            AiGraphSnapshot snapshot = AiGraphSnapshot.from(nodes, connections);
            String graphJson = AiJsonParser.toJson(snapshot);

            String systemPrompt =
                    "你是 NeuralCanvas 的图谱编辑 AI。\n" +
                    "你可以完整读取当前画布中的所有节点与连线，包括：\n" +
                    "1. 节点 id、标题、内容、类型、形状、坐标、尺寸\n" +
                    "2. 连线 id、起点节点、终点节点、关系类型、文字标签、粗细、颜色\n" +
                    "3. 箭头方向表示关系方向，fromNodeId 指向 toNodeId\n\n" +

                    "你的目标不是闲聊，而是像一个真正会操作图谱的软件助手那样工作。\n" +
                    "你应尽量直接完成用户要求，包括：\n" +
                    "- 分析图谱结构\n" +
                    "- 总结、提炼、补充节点\n" +
                    "- 创建、修改、删除节点\n" +
                    "- 创建、修改、删除连线\n" +
                    "- 设置连线文字、连线类型、粗细、颜色\n" +
                    "- 需要时执行自动布局\n\n" +

                    "你必须严格输出 JSON，不允许输出 markdown，不允许输出解释前缀。\n" +
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
                    "- 用户要求你修改图谱时，优先输出 commands，而不是只说不做\n" +
                    "- 创建多个新节点并互相连线时，必须给每个新节点分配 tempId，然后后续命令用 tempId 引用\n" +
                    "- 连线颜色用 connectionColorHex 输出\n" +
                    "- 若图谱会拥挤或重叠，最后追加 auto_layout\n" +
                    "- 不确定时尽量保守，不要删除过多已有节点\n" +
                    "- 若只是回答问题，不需要改图谱，则 commands 返回空数组";

            String userPrompt =
                    "当前图谱 JSON：\n" + graphJson + "\n\n" +
                    "用户请求：\n" + (userMessage == null ? "" : userMessage.trim());

            JSONObject body = new JSONObject();
            body.put("model", config.getModel());

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
            messages.put(new JSONObject().put("role", "user").put("content", userPrompt));

            body.put("messages", messages);
            body.put("temperature", 0.15);

            String url = normalizeChatCompletionsUrl(config.getBaseUrl());

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            Log.d(TAG, "请求URL=" + url);
            Log.d(TAG, "请求模型=" + config.getModel());

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "AI请求失败", e);
                    callback.onError("AI请求失败：" + e.getClass().getSimpleName() + " - " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = "";
                        try {
                            errorBody = response.body() == null ? "" : response.body().string();
                        } catch (Exception ignored) {
                        }
                        callback.onError("AI接口错误：" + response.code() + (errorBody.isEmpty() ? "" : ("\n" + errorBody)));
                        return;
                    }

                    try {
                        String resp = response.body() == null ? "" : response.body().string();
                        JSONObject obj = new JSONObject(resp);
                        JSONArray choices = obj.optJSONArray("choices");
                        if (choices == null || choices.length() == 0) {
                            callback.onError("AI未返回有效内容");
                            return;
                        }

                        JSONObject first = choices.getJSONObject(0);
                        JSONObject message = first.optJSONObject("message");
                        if (message == null) {
                            callback.onError("AI返回格式异常");
                            return;
                        }

                        String content = message.optString("content", "");
                        AiResponse parsed = AiJsonParser.parseResponse(content);
                        callback.onSuccess(parsed);

                    } catch (Exception e) {
                        Log.e(TAG, "AI响应解析失败", e);
                        callback.onError("AI响应解析失败：" + e.getClass().getSimpleName() + " - " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "请求构建失败", e);
            callback.onError("请求构建失败：" + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    private String normalizeChatCompletionsUrl(String baseUrl) {
        String url = baseUrl == null ? "" : baseUrl.trim();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (url.endsWith("/chat/completions")) return url;
        if (url.endsWith("/v1")) return url + "/chat/completions";
        return url + "/v1/chat/completions";
    }
}
