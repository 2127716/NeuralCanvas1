package com.agui.neuralcanvas;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class AiModelSelfReviewEngine {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public static final class ReviewOutcome {
        public AiResponse response = new AiResponse();
        public String summary = "模型复审未生效";
        public boolean usedModelReview = false;
    }

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    public ReviewOutcome review(AiConfig config,
                                AiGraphSnapshot snapshot,
                                AiResponse firstPass) throws Exception {
        ReviewOutcome outcome = new ReviewOutcome();
        outcome.response = firstPass == null ? new AiResponse() : firstPass;
        if (config == null || !config.isEnabled() || firstPass == null || firstPass.getCommands().isEmpty()) {
            outcome.summary = "模型复审跳过：无可复审命令";
            return outcome;
        }

        String systemPrompt = "你是第二轮复审代理。你只负责审查另一代理给出的图谱 commands。"
                + "目标：删掉重复、空泛、低价值、不能闭环、会让图更乱的命令。"
                + "你必须返回严格 JSON：{\"answer\":\"...\",\"commands\":[...]}。"
                + "保留高价值命令，必要时可减少命令数量，但不要新增无关命令。";

        JSONObject userPayload = new JSONObject();
        userPayload.put("graph", new JSONObject(AiJsonParser.toJson(snapshot)));
        userPayload.put("first_pass", new JSONObject(AiJsonParser.toJson(firstPass)));

        JSONObject body = new JSONObject();
        body.put("model", config.getModel());

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
        messages.put(new JSONObject().put("role", "user").put("content",
                "请对以下第一轮结果进行复审，删除无效或低价值命令，只返回复审后的 JSON。\n" + userPayload.toString()));
        body.put("messages", messages);
        body.put("temperature", 0.05);
        body.put("top_p", 0.8);

        Request request = new Request.Builder()
                .url(normalizeChatCompletionsUrl(config.getBaseUrl()))
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String respBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                outcome.summary = "模型复审跳过：接口错误 " + response.code();
                return outcome;
            }

            String content = extractAssistantContent(respBody);
            if (content.trim().isEmpty()) {
                outcome.summary = "模型复审跳过：未返回有效内容";
                return outcome;
            }

            AiResponse parsed = AiJsonParser.parseResponse(stripMarkdownCodeFence(content));
            if (parsed != null && parsed.getCommands() != null && !parsed.getCommands().isEmpty()) {
                outcome.response = parsed;
                outcome.usedModelReview = true;
                outcome.summary = "模型复审已接管，命令数 "
                        + firstPass.getCommands().size() + " → " + parsed.getCommands().size();
            } else {
                outcome.summary = "模型复审跳过：未返回可执行命令";
            }
            return outcome;
        }
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
            if (firstNewLine >= 0) trimmed = trimmed.substring(firstNewLine + 1);
            if (trimmed.endsWith("```")) trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
