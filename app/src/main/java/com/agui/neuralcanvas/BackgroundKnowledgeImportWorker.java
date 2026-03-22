package com.agui.neuralcanvas;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class BackgroundKnowledgeImportWorker extends Worker {
    public BackgroundKnowledgeImportWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SimpleDataManager dataManager = new SimpleDataManager(context);
        AiConfig config = dataManager.loadAiConfig();
        if (config == null || !config.isEnabled()) return Result.failure();

        String rawText = getInputData().getString("raw_text");
        String extraRule = getInputData().getString("extra_rule");
        if (rawText == null || rawText.trim().isEmpty()) return Result.failure();

        try {
            Map<?, ?> saved = dataManager.loadMindMap();
            Map<String, Node> nodes = (Map<String, Node>) saved.get("nodes");
            Map<String, Connection> connections = (Map<String, Connection>) saved.get("connections");

            String prompt = "请将下面文本整理为思维导图/知识网络，节点标题简洁，建立高价值有方向连接，尽量不要重排旧节点。"
                    + ((extraRule == null || extraRule.trim().isEmpty()) ? "" : ("额外要求：" + extraRule.trim() + "。"))
                    + "\n\n原文：\n" + rawText;

            AiRepository repository = new AiRepository();
            AiRepository.PreparedRequest prepared = repository.prepareRelevantRequest(nodes, connections, prompt, true);

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<AiResponse> responseRef = new AtomicReference<>();
            AtomicReference<String> errorRef = new AtomicReference<>();

            repository.askGraph(config, prepared.snapshot, prompt, false, new AiRepository.AiCallback() {
                @Override
                public void onSuccess(AiResponse response) {
                    responseRef.set(response);
                    latch.countDown();
                }

                @Override
                public void onError(String message) {
                    errorRef.set(message);
                    latch.countDown();
                }
            });

            latch.await(140, TimeUnit.SECONDS);

            if (errorRef.get() != null && !errorRef.get().trim().isEmpty()) return Result.retry();
            AiResponse response = responseRef.get();
            if (response == null) return Result.retry();

            if (response.getCommands() != null && !response.getCommands().isEmpty()) {
                new AiHeadlessExecutor(nodes, connections).execute(response.getCommands());
                dataManager.saveMindMap(nodes, connections);
                SuggestionFeedbackEngine.recordAutoApplied(dataManager, response, "background_import");
                SuggestionFeedbackEngine.recordEffectiveness(dataManager, response);
            }

            BrainPendingGuidance guidance = new BrainPendingGuidance();
            guidance.timestamp = System.currentTimeMillis();
            guidance.summary = "后台知识整理已完成\n\n"
                    + (response.getAnswer() == null || response.getAnswer().trim().isEmpty()
                    ? "已自动把导入内容整理进图谱"
                    : response.getAnswer());
            guidance.responseJson = AiJsonParser.toJson(response);
            guidance.autoApplied = response.getCommands() != null && !response.getCommands().isEmpty();
            guidance.riskLevel = "LOW";
            dataManager.savePendingBrainGuidance(guidance);

            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }
}
