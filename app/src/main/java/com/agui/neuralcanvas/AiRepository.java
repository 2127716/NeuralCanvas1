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
                    "你是思维导图助手。请基于当前图谱回答问题或生成编辑命令。" +
                    "只输出JSON：" +
                    "{\"answer\":\"\",\"commands\":[{\"action\":\"create_node|update_node|delete_node|create_connection|update_connection|delete_connection|focus_node|auto_layout\",\"nodeId\":\"\",\"fromNodeId\":\"\",\"toNodeId\":\"\",\"title\":\"\",\"content\":\"\",\"type\":\"\",\"shape\":\"\",\"label\":\"\",\"connectionType\":\"\",\"x\":0,\"y\":0,\"width\":168,\"height\":168,\"strokeWidth\":2}]}";

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

            Log.d(TAG, "请求URL=" + url);
            Log.d(TAG, "请求模型=" + config.getModel());
            Log.d(TAG, "用户请求长度=" + userPrompt.length());

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
                        } catch (Exception ex) {
                            Log.e(TAG, "读取错误响应体失败", ex);
                        }

                        Log.e(TAG, "AI接口错误 code=" + response.code() + ", body=" + errorBody);
                        callback.onError("AI接口错误：" + response.code() + (errorBody.isEmpty() ? "" : ("\n" + errorBody)));
                        return;
                    }

                    try {
                        String resp = response.body() == null ? "" : response.body().string();
                        Log.d(TAG, "AI原始响应：" + resp);

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
