package com.agui.neuralcanvas;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Map;

public class WeeklyReviewDialog {

    public static void show(Context context,
                            Map<String, Node> nodes,
                            Map<String, Connection> connections) {
        if (context == null) return;

        WeeklyReviewEngine.ReviewReport report =
                WeeklyReviewEngine.build(nodes, connections);

        String content = WeeklyReviewEngine.buildReadableSummary(report);

        ScrollView scrollView = new ScrollView(context);
        int padding = dp(context, 16);

        TextView textView = new TextView(context);
        textView.setPadding(padding, padding, padding, padding);
        textView.setTextSize(15f);
        textView.setText(content);

        scrollView.addView(textView);

        new AlertDialog.Builder(context)
                .setTitle("Weekly Review")
                .setView(scrollView)
                .setPositiveButton("知道了", null)
                .show();
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }
}
