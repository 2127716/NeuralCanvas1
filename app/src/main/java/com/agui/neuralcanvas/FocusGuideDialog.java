package com.agui.neuralcanvas;

import android.app.Dialog;
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
        return MonetDialogStyler.dp(requireContext(), value);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null || pendingReport == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        ScrollView sv = new ScrollView(requireContext());
        sv.setFillViewport(true);
        sv.setBackgroundColor(ThemeManager.getDialogBg());

        LinearLayout root = MonetDialogStyler.buildRoot(requireContext());
        sv.addView(root);

        TextView title = new TextView(requireContext());
        title.setText("自动引导");
        root.addView(title);

        TextView sub = new TextView(requireContext());
        sub.setText(pendingReport.headline);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(6);
        root.addView(sub, subLp);
        MonetDialogStyler.styleHeader(title, sub);

        for (NodeFocusGuideEngine.GuideItem item : pendingReport.items) {
            TextView row = new TextView(requireContext());
            row.setText(item.title + "\n" + item.hint);
            row.setTextColor(ThemeManager.getTextPrimary());
            row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            row.setPadding(dp(14), dp(12), dp(14), dp(12));
            row.setBackground(MonetDialogStyler.cardBg());
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
            MonetDialogStyler.apply(dialog, requireContext());
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.getWindow().setGravity(Gravity.CENTER);
            }
        });
        return dialog;
    }
}
