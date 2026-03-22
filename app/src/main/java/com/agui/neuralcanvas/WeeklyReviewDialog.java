package com.agui.neuralcanvas;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.Map;

public class WeeklyReviewDialog {

    public static void show(Context context,
                            Map<String, Node> nodes,
                            Map<String, Connection> connections) {
        if (context == null) return;

        WeeklyReviewEngine.ReviewReport report = WeeklyReviewEngine.build(nodes, connections);
        String content = WeeklyReviewEngine.buildReadableSummary(report);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(ThemeManager.getDialogBg());

        LinearLayout root = MonetDialogStyler.buildRoot(context);
        scrollView.addView(root);

        TextView title = new TextView(context);
        title.setText("Weekly Review");
        root.addView(title);

        TextView sub = new TextView(context);
        sub.setText("每周复盘概览");
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = MonetDialogStyler.dp(context, 6);
        root.addView(sub, subLp);
        MonetDialogStyler.styleHeader(title, sub);

        TextView body = MonetDialogStyler.body(context, content);
        body.setPadding(MonetDialogStyler.dp(context, 16), MonetDialogStyler.dp(context, 14),
                MonetDialogStyler.dp(context, 16), MonetDialogStyler.dp(context, 14));
        body.setBackground(MonetDialogStyler.cardBg());
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = MonetDialogStyler.dp(context, 14);
        root.addView(body, bodyLp);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(scrollView)
                .setPositiveButton("知道了", null)
                .create();
        dialog.setOnShowListener(d -> MonetDialogStyler.apply(dialog, context));
        dialog.show();
    }
}
