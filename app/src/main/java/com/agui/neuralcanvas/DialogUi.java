package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

final class DialogUi {

    private DialogUi() {}

    static int dp(DialogFragment fragment, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                fragment.requireContext().getResources().getDisplayMetrics()
        );
    }

    static void styleWindow(DialogFragment fragment, Dialog dialog) {
        if (dialog == null) return;
        Window window = dialog.getWindow();
        if (window == null) return;

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getDialogBg());
        bg.setCornerRadius(dp(fragment, 28));
        bg.setStroke(dp(fragment, 1), ThemeManager.getStroke());
        window.setBackgroundDrawable(bg);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(ThemeManager.getDialogBg());
            window.setNavigationBarColor(ThemeManager.getDialogBg());
        }
    }

    static LinearLayout createRoot(DialogFragment fragment) {
        LinearLayout root = new LinearLayout(fragment.requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeManager.getDialogBg());
        root.setPadding(dp(fragment, 18), dp(fragment, 18), dp(fragment, 18), dp(fragment, 12));
        return root;
    }

    static ScrollView createScroll(DialogFragment fragment, LinearLayout content) {
        ScrollView scrollView = new ScrollView(fragment.requireContext());
        scrollView.setFillViewport(true);
        scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scrollView.setBackgroundColor(ThemeManager.getDialogBg());
        scrollView.addView(content);
        return scrollView;
    }

    static LinearLayout createContentColumn(DialogFragment fragment) {
        LinearLayout content = new LinearLayout(fragment.requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(fragment, 8));
        return content;
    }

    static LinearLayout createCard(DialogFragment fragment) {
        LinearLayout card = new LinearLayout(fragment.requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(fragment, 16), dp(fragment, 16), dp(fragment, 16), dp(fragment, 16));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getSurface());
        bg.setCornerRadius(dp(fragment, 24));
        bg.setStroke(dp(fragment, 1), ThemeManager.getStroke());
        card.setBackground(bg);
        return card;
    }

    static LinearLayout createHero(DialogFragment fragment, String eyebrow, String title, String subtitle) {
        LinearLayout hero = createCard(fragment);
        hero.setPadding(dp(fragment, 18), dp(fragment, 18), dp(fragment, 18), dp(fragment, 18));

        TextView eyebrowView = new TextView(fragment.requireContext());
        eyebrowView.setText(eyebrow);
        eyebrowView.setTextColor(ThemeManager.getAccent());
        eyebrowView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        eyebrowView.setTypeface(eyebrowView.getTypeface(), Typeface.BOLD);
        eyebrowView.setLetterSpacing(0.06f);
        hero.addView(eyebrowView);

        TextView titleView = new TextView(fragment.requireContext());
        titleView.setText(title);
        titleView.setTextColor(ThemeManager.getTextPrimary());
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 23f);
        titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleLp.topMargin = dp(fragment, 8);
        hero.addView(titleView, titleLp);

        if (subtitle != null && !subtitle.trim().isEmpty()) {
            TextView subtitleView = new TextView(fragment.requireContext());
            subtitleView.setText(subtitle);
            subtitleView.setTextColor(ThemeManager.getTextSecondary());
            subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
            subtitleView.setLineSpacing(0f, 1.15f);
            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            subLp.topMargin = dp(fragment, 8);
            hero.addView(subtitleView, subLp);
        }
        return hero;
    }

    static TextView createSectionTitle(DialogFragment fragment, String text) {
        TextView tv = new TextView(fragment.requireContext());
        tv.setText(text);
        tv.setTextColor(ThemeManager.getSectionTitleColor());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setLetterSpacing(0.05f);
        return tv;
    }

    static TextView createHelper(DialogFragment fragment, String text) {
        TextView tv = new TextView(fragment.requireContext());
        tv.setText(text);
        tv.setTextColor(ThemeManager.getTextSecondary());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        tv.setLineSpacing(0f, 1.12f);
        return tv;
    }

    static EditText createInput(DialogFragment fragment, String hint, String value, int inputType, int minLines) {
        EditText et = new EditText(fragment.requireContext());
        et.setHint(hint);
        et.setText(value == null ? "" : value);
        et.setInputType(inputType);
        et.setMinLines(minLines);
        et.setTextColor(ThemeManager.getTextPrimary());
        et.setHintTextColor(ThemeManager.getTextSecondary());
        et.setPadding(dp(fragment, 14), dp(fragment, 13), dp(fragment, 14), dp(fragment, 13));
        et.setBackground(createFieldBackground(fragment));
        et.setBackgroundTintList(null);
        return et;
    }

    static GradientDrawable createFieldBackground(DialogFragment fragment) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getEditTextBg());
        bg.setCornerRadius(dp(fragment, 18));
        bg.setStroke(dp(fragment, 1), ThemeManager.getEditTextStroke());
        return bg;
    }

    static TextView createChip(DialogFragment fragment, String text, boolean accent) {
        TextView tv = new TextView(fragment.requireContext());
        tv.setText(text);
        tv.setTextColor(accent ? ThemeManager.getAccent() : ThemeManager.getTextPrimary());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        tv.setTypeface(tv.getTypeface(), accent ? Typeface.BOLD : Typeface.NORMAL);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(fragment, 14), dp(fragment, 11), dp(fragment, 14), dp(fragment, 11));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(accent ? ThemeManager.getAccentSoft() : ThemeManager.getChipBg());
        bg.setCornerRadius(dp(fragment, 18));
        bg.setStroke(dp(fragment, 1), accent ? ThemeManager.getAccentStroke() : ThemeManager.getChipStroke());
        tv.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.rightMargin = dp(fragment, 8);
        tv.setLayoutParams(lp);
        return tv;
    }

    static HorizontalScrollView wrapChipRow(DialogFragment fragment, LinearLayout row) {
        HorizontalScrollView hsv = new HorizontalScrollView(fragment.requireContext());
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        hsv.addView(row);
        return hsv;
    }

    static TextView createFooterButton(DialogFragment fragment, String text, int textColor, int bgColor, boolean bold) {
        TextView tv = new TextView(fragment.requireContext());
        tv.setText(text);
        tv.setTextColor(textColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(fragment, 14), dp(fragment, 13), dp(fragment, 14), dp(fragment, 13));
        tv.setTypeface(tv.getTypeface(), bold ? Typeface.BOLD : Typeface.NORMAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(fragment, 18));
        bg.setStroke(dp(fragment, 1), ThemeManager.withAlpha(textColor, 56));
        tv.setBackground(bg);
        return tv;
    }

    static void addWithTopMargin(DialogFragment fragment, LinearLayout root, View view, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(fragment, topDp);
        root.addView(view, lp);
    }
}
