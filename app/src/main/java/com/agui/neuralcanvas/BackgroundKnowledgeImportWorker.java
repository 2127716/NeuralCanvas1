package com.agui.neuralcanvas;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;
import java.util.List;
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
        String[] uriStrings = getInputData().getStringArray("uri_strings");

        try {
            List<DocumentImportPipeline.ImportResult> imported = new ArrayList<>();
            if (uriStrings != null) {
                for (String uriString : uriStrings) {
                    if (uriString == null || uriString.trim().isEmpty()) continue;
                    imported.add(DocumentImportPipeline.importUri(context, Uri.parse(uriString)));
                }
            }

            String mergedText = DocumentImportPipeline.mergeForAi(
                    rawText,
                    imported.toArray(new DocumentImportPipeline.ImportResult[0])
            );

            if (mergedText.trim().isEmpty()) return Result.failure();

            Map<?, ?> saved = dataManager.loadMindMap();
            Map<String, Node> nodes = (Map<String, Node>) saved.get("nodes");
            Map<String, Connection> connections = (Map<String, Connection>) saved.get("connections");

            String prompt = "请将下面导入内容整理为思维导图/知识网络，节点标题简洁，建立高价值有方向连接，尽量不要重排旧节点。"
                    + "默认输出可审查的低风险改动，优先给出 create_node / create_connection / update_node。"
                    + ((extraRule == null || extraRule.trim().isEmpty()) ? "" : ("额外要求：" + extraRule.trim() + "。"))
                    + "\n\n导入内容：\n" + mergedText;

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

            latch.await(150, TimeUnit.SECONDS);

            if (errorRef.get() != null && !errorRef.get().trim().isEmpty()) return Result.retry();
            AiResponse response = responseRef.get();
            if (response == null) return Result.retry();

            if (response.getCommands() != null && !response.getCommands().isEmpty()) {
                PendingOperationBundle bundle = new PendingOperationBundle();
                bundle.createdAt = System.currentTimeMillis();
                bundle.summary = "后台导入已完成，已生成待确认改动";
                bundle.responseJson = AiJsonParser.toJson(response);
                bundle.riskLevel = "LOW";
                bundle.commandCount = response.getCommands().size();
                bundle.impactSummary = OperationImpactSummaryEngine.analyze(response).buildSummary();
                dataManager.savePendingOperationBundle(bundle);
            }

            StringBuilder importSummary = new StringBuilder();
            importSummary.append("后台导入已完成");
            if (!imported.isEmpty()) {
                importSummary.append("\n导入文件：").append(imported.size()).append(" 个");
                for (DocumentImportPipeline.ImportResult r : imported) {
                    importSummary.append("\n- ").append(r.sourceName).append("（").append(r.note).append("）");
                }
            }

            BrainPendingGuidance guidance = new BrainPendingGuidance();
            guidance.timestamp = System.currentTimeMillis();
            guidance.summary = importSummary + "\n\n"
                    + (response.getAnswer() == null || response.getAnswer().trim().isEmpty()
                    ? "已完成提取与结构建议，请回到 App 查看待确认改动"
                    : response.getAnswer());
            guidance.responseJson = AiJsonParser.toJson(response);
            guidance.autoApplied = false;
            guidance.riskLevel = "LOW";
            dataManager.savePendingBrainGuidance(guidance);

            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }
}
