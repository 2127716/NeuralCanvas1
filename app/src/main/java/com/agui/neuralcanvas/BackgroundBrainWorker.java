
package com.agui.neuralcanvas;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Map;

public class BackgroundBrainWorker extends Worker {

    public BackgroundBrainWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        Context context = getApplicationContext();
        SimpleDataManager dataManager = new SimpleDataManager(context);

        Map<?, ?> saved = dataManager.loadMindMap();
        Map<String, Node> nodes = (Map<String, Node>) saved.get("nodes");
        Map<String, Connection> connections = (Map<String, Connection>) saved.get("connections");

        // 原分析
        BackgroundBrainAnalyzer.BrainPulseReport report =
                BackgroundBrainAnalyzer.analyze(nodes, connections, null, null);

        // 新增：执行后再次审查
        WorkflowAuditEngine.AuditResult afterAudit =
                WorkflowAuditEngine.audit(nodes, connections);

        if (!afterAudit.isHealthy()) {
            report.summary += "\n\n⚠ 二次审查发现问题:";
            for (String s : afterAudit.issues) {
                report.summary += "\n- " + s;
            }
        }

        dataManager.saveLastBrainPulse(System.currentTimeMillis(), report.summary);

        return Result.success();
    }
}
