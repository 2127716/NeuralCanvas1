
package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class FocusGuideDialog extends DialogFragment {

    private static NodeFocusGuideEngine.GuideReport pendingReport;

    public static void show(MainActivity activity, NodeFocusGuideEngine.GuideReport report) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || report == null) return;
        pendingReport = report;
        new FocusGuideDialog().show(activity.getSupportFragmentManager(), "focus_guide_dialog");
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, requireContext().getResources().getDisplayMetrics()
        );
    }

    private android.graphics.drawable.GradientDrawable bg() {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Color.parseColor("#111827"));
        gd.setCornerRadius(dp(18));
        gd.setStroke(dp(1), Color.parseColor("#24324A"));
        return gd;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null || pendingReport == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        ScrollView sv = new ScrollView(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.parseColor("#0B1020"));
        sv.addView(root);

        TextView title = new TextView(requireContext());
        title.setText("自动引导");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTextColor(Color.parseColor("#F8FAFC"));
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView sub = new TextView(requireContext());
        sub.setText(pendingReport.headline);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        sub.setTextColor(Color.parseColor("#94A3B8"));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(6);
        sub.setLayoutParams(subLp);
        root.addView(sub);

        for (NodeFocusGuideEngine.GuideItem item : pendingReport.items) {
            TextView row = new TextView(requireContext());
            row.setText(item.title + "\n" + item.hint);
            row.setTextColor(Color.parseColor("#E2E8F0"));
            row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            row.setPadding(dp(14), dp(12), dp(14), dp(12));
            row.setBackground(bg());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(10);
            row.setLayoutParams(lp);
            row.setOnClickListener(v -> {
                activity.getMindMapView().focusNodeById(item.nodeId);
                activity.getMindMapView().selectOnlyNode(item.nodeId);
                dismiss();
            });
            root.addView(row);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(sv)
                .setPositiveButton("聚焦第一项", (d, w) -> {
                    if (!pendingReport.items.isEmpty()) {
                        String id = pendingReport.items.get(0).nodeId;
                        activity.getMindMapView().focusNodeById(id);
                        activity.getMindMapView().selectOnlyNode(id);
                    }
                })
                .setNegativeButton("关闭", null)
                .create();

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.getWindow().setGravity(Gravity.CENTER);
            }
        });
        return dialog;
    }
}
