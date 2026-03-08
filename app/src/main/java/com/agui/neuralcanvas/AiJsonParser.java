package com.agui.neuralcanvas;

import com.google.gson.Gson;

public class AiJsonParser {

    private static final Gson gson = new Gson();

    public static AiResponse parseResponse(String raw) throws Exception {
        if (raw == null) {
            throw new Exception("AI返回为空");
        }

        String cleaned = extractJson(raw.trim());
        AiResponse response = gson.fromJson(cleaned, AiResponse.class);

        if (response == null) {
            throw new Exception("AI响应解析失败");
        }

        return response;
    }

    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    private static String extractJson(String text) throws Exception {
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start < 0 || end <= start) {
            throw new Exception("未找到合法JSON");
        }
        return text.substring(start, end + 1);
    }
}
