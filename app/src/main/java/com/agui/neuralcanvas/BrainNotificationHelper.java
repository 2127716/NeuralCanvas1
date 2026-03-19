
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
    public static final String CHANNEL_ID = "brain_autopilot_channel";
    public static final int NOTIFY_ID_AUTOPILOT = 41021;

    private BrainNotificationHelper() {}

    public static void ensureChannel(Context context) {
        if (context == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Brain Autopilot",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("AI autopilot analysis and guidance");
            nm.createNotificationChannel(channel);
        }
    }

    public static void notifyBrainReport(Context context, AutonomousBrainEngine.BrainReport report) {
        if (context == null || report == null) return;
        ensureChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("brain_autopilot_node_id", report.focusNodeId == null ? "" : report.focusNodeId);
        intent.putExtra("brain_autopilot_mode", report.mode == null ? "" : report.mode);
        intent.putExtra("brain_autopilot_reason", report.reason == null ? "" : report.reason);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        String title = (report.title == null || report.title.trim().isEmpty()) ? "AI 自动巡航建议" : report.title.trim();
        String text = (report.reason == null || report.reason.trim().isEmpty()) ? "发现一个值得优先处理的节点" : report.reason.trim();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(context).notify(NOTIFY_ID_AUTOPILOT, builder.build());
    }

    public static void cancel(Context context) {
        if (context == null) return;
        NotificationManagerCompat.from(context).cancel(NOTIFY_ID_AUTOPILOT);
    }
}
