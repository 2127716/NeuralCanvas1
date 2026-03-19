package com.agui.neuralcanvas;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public final class BrainAutopilotScheduler {
    private static final String PERIODIC_WORK_NAME = "brain_autopilot_periodic";
    private static final String IMMEDIATE_WORK_NAME = "brain_autopilot_immediate";

    private BrainAutopilotScheduler() {}

    public static void ensureScheduled(Context context) {
        if (context == null) return;
        BrainAutopilotSettings settings = new SimpleDataManager(context).loadAutopilotSettings();
        if (settings == null || !settings.isEnabled()) return;

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                BackgroundBrainWorker.class,
                Math.max(1, settings.getIntervalHours()),
                TimeUnit.HOURS
        ).setConstraints(new Constraints.Builder().build()).build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
        );
    }

    public static void requestImmediatePulse(Context context) {
        if (context == null) return;
        SimpleDataManager dataManager = new SimpleDataManager(context);
        BrainAutopilotSettings settings = dataManager.loadAutopilotSettings();
        if (settings == null || !settings.isEnabled()) return;

        long last = dataManager.loadLastBrainPulseAt();
        long now = System.currentTimeMillis();
        if (now - last < TimeUnit.MINUTES.toMillis(30)) return;

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BackgroundBrainWorker.class).build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
        );
    }
}
