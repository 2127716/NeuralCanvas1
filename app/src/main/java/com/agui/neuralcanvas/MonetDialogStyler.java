package com.agui.neuralcanvas;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MonetDialogStyler {
    private MonetDialogStyler() {}

    public static void apply(Dialog dialog, Context context) {
        if (dialog == null || context == null || dialog.getWindow() == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getDialogBg());
        bg.setCornerRadius(dp(context, 26));
        bg.setStroke(dp(context, 1), ThemeManager.getStroke());
        dialog.getWindow().setBackgroundDrawable(bg);
    }

    public static LinearLayout buildRoot(Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(context, 18);
        root.setPadding(p, p, p, p);
        root.setBackgroundColor(ThemeManager.getDialogBg());
        return root;
    }

    public static void styleHeader(TextView title, TextView subtitle) {
        if (title != null) {
            title.setTextColor(ThemeManager.getTextPrimary());
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
            title.setTypeface(title.getTypeface(), Typeface.BOLD);
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

    public static TextView cardTitle(Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextColor(ThemeManager.getTextPrimary());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        return tv;
    }

    public static TextView body(Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextColor(ThemeManager.getTextSecondary());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        return tv;
    }

    public static LinearLayout card(Context context, String title, String subtitle) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14));
        card.setBackground(cardBg());
        if (title != null && !title.trim().isEmpty()) {
            card.addView(cardTitle(context, title));
        }
        if (subtitle != null && !subtitle.trim().isEmpty()) {
            TextView sub = body(context, subtitle);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(context, 6);
            card.addView(sub, lp);
        }
        return card;
    }

    public static int dp(Context context, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()
        );
    }
}
