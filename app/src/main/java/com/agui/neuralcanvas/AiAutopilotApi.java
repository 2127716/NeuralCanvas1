package com.agui.neuralcanvas;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiAutopilotApi {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .callTimeout(150, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    public AiAutopilotApi() {}

    public AiResponse runAutopilot(AiConfig config,
                                   AiGraphSnapshot snapshot,
                                   BrainAutopilotSettings settings) throws Exception {
        if (config == null || !config.isEnabled()) {
            throw new IllegalStateException("AI配置不完整");
        }
        if (snapshot == null) snapshot = new AiGraphSnapshot();

        AiAgentPromptBuilder.AgentPlan plan = AiAgentPromptBuilder.build(snapshot, settings);

        JSONObject body = new JSONObject();
        body.put("model", config.getModel());

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", plan.systemPrompt));
        messages.put(new JSONObject().put("role", "user").put("content", plan.userPrompt));
        body.put("messages", messages);
        body.put("temperature", 0.1);
        body.put("top_p", 0.9);

        Request request = new Request.Builder()
                .url(normalizeChatCompletionsUrl(config.getBaseUrl()))
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String respBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw new IllegalStateException("AI接口错误：" + response.code() + "\n" + trim(respBody, 400));
            }
            String content = extractAssistantContent(respBody);
            if (content.trim().isEmpty()) {
                throw new IllegalStateException("AI未返回有效内容");
            }
            AiResponse parsed = AiJsonParser.parseResponse(stripMarkdownCodeFence(content));
            sanitize(parsed);
            if (parsed != null && (parsed.getAnswer() == null || parsed.getAnswer().trim().isEmpty())) {
                parsed.setAnswer("AI 已完成一次 " + plan.profile.label + " 分析");
            }
            return parsed;
        }
    }

    private void sanitize(AiResponse response) {
        if (response == null || response.getCommands() == null) return;
        List<AiCommand> cleaned = new ArrayList<>();
        for (AiCommand cmd : response.getCommands()) {
            if (cmd == null) continue;
            String action = safeLower(cmd.getAction());
            if (action.isEmpty()) continue;
            if (cmd.getStrokeWidth() != null) {
                float w = cmd.getStrokeWidth();
                if (w < 2f) cmd.setStrokeWidth(2f);
                if (w > 10f) cmd.setStrokeWidth(10f);
            }
            cleaned.add(cmd);
        }
        response.setCommands(cleaned);
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
        if (choices == null || choices.length() == 0) return "";
        JSONObject message = choices.getJSONObject(0).optJSONObject("message");
        if (message == null) return "";
        Object contentObj = message.opt("content");
        if (contentObj instanceof String) return (String) contentObj;
        if (contentObj instanceof JSONArray) {
            JSONArray arr = (JSONArray) contentObj;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arr.length(); i++) {
                Object item = arr.opt(i);
                if (item instanceof JSONObject) {
                    JSONObject part = (JSONObject) item;
                    if (part.has("text")) sb.append(part.optString("text", ""));
                    else if (part.has("content")) sb.append(part.optString("content", ""));
                    else if (part.has("value")) sb.append(part.optString("value", ""));
                } else if (item != null) {
                    sb.append(String.valueOf(item));
                }
            }
            return sb.toString();
        }
        return String.valueOf(contentObj);
    }

    private String stripMarkdownCodeFence(String text) {
        String trimmed = text == null ? "" : text.trim();
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

    private String safeLower(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private String trim(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
