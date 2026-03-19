
package com.agui.neuralcanvas;

import java.util.Map;

public final class AiAutopilotApi {

    public interface Callback {
        void onSuccess(AiResponse response);
        void onError(String message);
    }

    private AiAutopilotApi() {}

    public static void run(
            AiConfig config,
            Map<String, Node> nodes,
            Map<String, Connection> connections,
            String prompt,
            Callback callback
    ) {
        if (callback == null) return;
        AiRepository repository = new AiRepository();
        AiRepository.PreparedRequest prepared = repository.prepareRelevantRequest(
                nodes,
                connections,
                prompt == null ? "" : prompt,
                false
        );
        repository.askGraph(
                config,
                prepared.snapshot,
                prepared.finalPrompt,
                prepared.layoutAllowed,
                new AiRepository.AiCallback() {
                    @Override
                    public void onSuccess(AiResponse response) {
                        callback.onSuccess(response);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                }
        );
    }

    public static String stripMarkdownCodeFence(String text) {
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
}
