package com.agui.neuralcanvas;

import android.content.Context;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public final class KnowledgeImportJobManager {
    private KnowledgeImportJobManager() {}

    public static void enqueue(Context context, String rawText, String extraRule) {
        if (context == null) return;
        Data data = new Data.Builder()
                .putString("raw_text", rawText == null ? "" : rawText)
                .putString("extra_rule", extraRule == null ? "" : extraRule)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BackgroundKnowledgeImportWorker.class)
                .setInputData(data)
                .build();

        WorkManager.getInstance(context).enqueue(request);
    }
}
