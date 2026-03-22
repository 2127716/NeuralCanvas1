package com.agui.neuralcanvas;

import android.content.Context;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public final class KnowledgeImportJobManager {
    private KnowledgeImportJobManager() {}

    public static void enqueue(Context context, String rawText, String extraRule, String[] uriStrings) {
        if (context == null) return;
        Data.Builder builder = new Data.Builder()
                .putString("raw_text", rawText == null ? "" : rawText)
                .putString("extra_rule", extraRule == null ? "" : extraRule);
        if (uriStrings != null) builder.putStringArray("uri_strings", uriStrings);

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BackgroundKnowledgeImportWorker.class)
                .setInputData(builder.build())
                .build();

        WorkManager.getInstance(context).enqueue(request);
    }
}
