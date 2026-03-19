package com.agui.neuralcanvas;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public final class BrainNotificationHelper {
    private static final String CHANNEL_ID = "brain_autopilot_channel";

    private BrainNotificationHelper() {}

    public static void ensureChannel(Context context) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "AI 自动巡航",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("第二大脑 API 自动巡航后的关键提醒");
        manager.createNotificationChannel(channel);
    }

    public static void showBrainPulse(Context context,
                                      BackgroundBrainAnalyzer.BrainPulseReport report,
                                      BrainAutopilotSettings settings) {
        if (context == null || report == null) return;
        ensureChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("brain_focus_node_id", report.focusNodeId);
        intent.putExtra("brain_focus_mode", report.suggestedMode);
        intent.putExtra("brain_open_mode", settings == null || settings.isOpenModeOnNotificationTap());
        intent.putExtra("brain_open_guidance", true);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1001, intent, flags);

        String title = report.autoApplied ? "AI 已自动优化网络" : "AI 建议你先处理一个关键节点";
        if (report.focusNodeTitle != null && !report.focusNodeTitle.trim().isEmpty()) {
            title += "：" + report.focusNodeTitle.trim();
        }
        String text = (report.summary == null || report.summary.trim().isEmpty())
                ? "AI 已完成一次自动巡航。"
                : report.summary.trim();
        String bigText = text + "\n" + (report.reason == null ? "" : report.reason);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify(1001, builder.build());
        } catch (SecurityException ignored) {
        }
    }
}
