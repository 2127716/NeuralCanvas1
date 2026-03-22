package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

public final class MonetDialogStyler {
    private MonetDialogStyler() {}

    public static void apply(Dialog dialog, android.content.Context context) {
        if (dialog == null || context == null || dialog.getWindow() == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getDialogBg());
        bg.setCornerRadius(dp(context, 26));
        bg.setStroke(dp(context, 1), ThemeManager.getStroke());
        dialog.getWindow().setBackgroundDrawable(bg);
    }

    public static void styleHeader(TextView title, TextView subtitle) {
        if (title != null) {
            title.setTextColor(ThemeManager.getTextPrimary());
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
            title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        }
        if (subtitle != null) {
            subtitle.setTextColor(ThemeManager.getTextSecondary());
            subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        }
    }

    public static GradientDrawable cardBg() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getChipBg());
        bg.setCornerRadius(20f);
        bg.setStroke(1, ThemeManager.getChipStroke());
        return bg;
    }

    public static void styleActionText(TextView textView) {
        if (textView == null) return;
        textView.setTextColor(ThemeManager.getAccent());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
    }

    private static int dp(android.content.Context context, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()
        );
    }
}
