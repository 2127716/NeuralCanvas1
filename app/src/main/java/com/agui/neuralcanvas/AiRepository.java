package com.agui.neuralcanvas;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

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

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();

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
                    "你是一个思维导图/知识图谱编辑助手。\n" +
                    "你会读取当前图谱中的所有节点和连线，理解节点标题、内容、类型、形状、位置，以及连线方向和标签。\n\n" +
                    "你的任务有两种：\n" +
                    "1. 回答用户关于当前图谱的问题\n" +
                    "2. 生成可执行的图谱编辑命令\n\n" +
                    "请严格输出 JSON，格式如下：\n" +
                    "{\n" +
                    "  \"answer\": \"给用户的简洁回答\",\n" +
                    "  \"commands\": [\n" +
                    "    {\n" +
                    "      \"action\": \"create_node | update_node | delete_node | create_connection | update_connection | delete_connection | focus_node | auto_layout\",\n" +
                    "      \"nodeId\": \"\",\n" +
                    "      \"fromNodeId\": \"\",\n" +
                    "      \"toNodeId\": \"\",\n" +
                    "      \"title\": \"\",\n" +
                    "      \"content\": \"\",\n" +
                    "      \"type\": \"\",\n" +
                    "      \"shape\": \"\",\n" +
                    "      \"label\": \"\",\n" +
                    "      \"connectionType\": \"\",\n" +
                    "      \"x\": 0,\n" +
                    "      \"y\": 0,\n" +
                    "      \"width\": 168,\n" +
                    "      \"height\": 168,\n" +
                    "      \"strokeWidth\": 4\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}\n\n" +
                    "规则：\n" +
                    "- 不要输出 markdown\n" +
                    "- 不要输出解释性前缀\n" +
                    "- 只能输出合法 JSON\n" +
                    "- 如果只是回答问题，不需要改图谱，则 commands 返回空数组\n" +
                    "- 如果创建节点，尽量给出合理位置，避免重叠\n" +
                    "- 连线是有方向的：fromNodeId 指向 toNodeId\n" +
                    "- 若用户要求整理结构、避免重叠、优化排版，可加入 auto_layout 命令\n" +
                    "- 修改已有连线时优先使用 update_connection";

            String userPrompt =
                    "当前图谱JSON如下：\n" + graphJson + "\n\n" +
                    "用户请求：\n" + (userMessage == null ? "" : userMessage.trim());

            JSONObject body = new JSONObject();
            body.put("model", config.getModel());

            JSONArray messages = new JSONArray();

            JSONObject sys = new JSONObject();
            sys.put("role", "system");
            sys.put("content", systemPrompt);
            messages.put(sys);

            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content", userPrompt);
            messages.put(user);

            body.put("messages", messages);
            body.put("temperature", 0.2);

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
                    callback.onError("AI请求失败：" + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onError("AI接口错误：" + response.code());
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
                        callback.onError("AI响应解析失败：" + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            callback.onError("请求构建失败：" + e.getMessage());
        }
    }

    private String normalizeChatCompletionsUrl(String baseUrl) {
        String url = baseUrl == null ? "" : baseUrl.trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.endsWith("/chat/completions")) {
            return url;
        }
        if (url.endsWith("/v1")) {
            return url + "/chat/completions";
        }
        return url + "/v1/chat/completions";
    }
}
