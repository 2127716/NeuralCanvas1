package com.agui.neuralcanvas;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class TemplateMethodInfoDialog extends DialogFragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_WHAT = "what";
    private static final String ARG_WHEN = "when";
    private static final String ARG_OUT = "out";

    public static TemplateMethodInfoDialog newInstance(String title, String what, String when, String out) {
        TemplateMethodInfoDialog dialog = new TemplateMethodInfoDialog();
        Bundle b = new Bundle();
        b.putString(ARG_TITLE, safe(title));
        b.putString(ARG_WHAT, safe(what));
        b.putString(ARG_WHEN, safe(when));
        b.putString(ARG_OUT, safe(out));
        dialog.setArguments(b);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String titleText = getArguments() == null ? "方法说明" : safe(getArguments().getString(ARG_TITLE));
        String what = getArguments() == null ? "" : safe(getArguments().getString(ARG_WHAT));
        String when = getArguments() == null ? "" : safe(getArguments().getString(ARG_WHEN));
        String out = getArguments() == null ? "" : safe(getArguments().getString(ARG_OUT));

        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(ThemeManager.getDialogBg());

        LinearLayout root = MonetDialogStyler.buildRoot(requireContext());
        scroll.addView(root);

        TextView title = new TextView(requireContext());
        title.setText(titleText);
        root.addView(title);

        TextView sub = new TextView(requireContext());
        sub.setText("长按模板卡片时出现的精简介绍");
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = MonetDialogStyler.dp(requireContext(), 6);
        root.addView(sub, subLp);
        MonetDialogStyler.styleHeader(title, sub);

        LinearLayout card1 = MonetDialogStyler.card(requireContext(), "它有什么用", what);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp1.topMargin = MonetDialogStyler.dp(requireContext(), 14);
        root.addView(card1, lp1);

        LinearLayout card2 = MonetDialogStyler.card(requireContext(), "什么情况下用", when);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp2.topMargin = MonetDialogStyler.dp(requireContext(), 12);
        root.addView(card2, lp2);

        LinearLayout card3 = MonetDialogStyler.card(requireContext(), "用了以后通常会产出什么", out);
        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp3.topMargin = MonetDialogStyler.dp(requireContext(), 12);
        root.addView(card3, lp3);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(scroll)
                .setPositiveButton("知道了", null)
                .create();
        dialog.setOnShowListener(d -> MonetDialogStyler.apply(dialog, requireContext()));
        return dialog;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
