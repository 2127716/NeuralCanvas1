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

        String focusTitle = report.focusNodeTitle == null ? "" : report.focusNodeTitle.trim();
        String title;
        if (report.autoApplied) title = focusTitle.isEmpty() ? "我先替你补了一步" : "我先替你补了一步：" + focusTitle;
        else title = focusTitle.isEmpty() ? "该推进一下了" : "轮到你推进：" + focusTitle;

        String text = buildCompactNudge(report);
        String bigText = text + "\n\n点开后我会直接告诉你现在先做什么。";

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

    private static String buildCompactNudge(BackgroundBrainAnalyzer.BrainPulseReport report) {
        if (report == null) return "AI 已完成一次自动巡航。";
        if (report.reason != null && !report.reason.trim().isEmpty()) {
            return report.reason.trim();
        }
        if (report.summary != null && !report.summary.trim().isEmpty()) {
            String value = report.summary.trim().replace('\n', ' ');
            return value.length() > 110 ? value.substring(0, 109) + "…" : value;
        }
        if ("decision".equalsIgnoreCase(report.suggestedMode)) return "这个节点更像一个未落地的决定，先补证据、风险或方案比较。";
        if ("learning".equalsIgnoreCase(report.suggestedMode)) return "这个节点更像一个知识点，先做检索或迁移练习。";
        return "这个节点最缺的是可执行下一步，先补触发条件、障碍和最小动作。";
    }
}
