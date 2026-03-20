package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BrainAutopilotHistoryDialog extends DialogFragment {

    public static BrainAutopilotHistoryDialog newInstance() {
        return new BrainAutopilotHistoryDialog();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, requireContext().getResources().getDisplayMetrics()
        );
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!(getActivity() instanceof MainActivity)) {
            return super.onCreateDialog(savedInstanceState);
        }
        MainActivity activity = (MainActivity) getActivity();
        List<BrainAutopilotLogEntry> list = new BrainAutopilotHistoryStore(activity).loadAll();

        ScrollView sv = new ScrollView(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        sv.addView(root);

        if (list.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("还没有自动巡航记录");
            empty.setTextSize(15);
            empty.setTextColor(Color.parseColor("#334155"));
            root.addView(empty);
        } else {
            for (BrainAutopilotLogEntry item : list) {
                root.addView(buildCard(activity, item));
            }
        }

        return new AlertDialog.Builder(requireContext())
                .setTitle("自动巡航历史")
                .setView(sv)
                .setPositiveButton("关闭", null)
                .setNeutralButton("清空", (d, w) -> new BrainAutopilotHistoryStore(activity).clear())
                .create();
    }

    private LinearLayout buildCard(MainActivity activity, BrainAutopilotLogEntry item) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundColor(Color.parseColor("#F8FAFC"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(10);
        card.setLayoutParams(lp);

        TextView t1 = new TextView(requireContext());
        t1.setText(formatTime(item.timestamp) + "｜" + safe(item.agentProfile) + "｜" + safe(item.riskLevel));
        t1.setTextSize(14);
        t1.setTextColor(Color.parseColor("#0F172A"));
        card.addView(t1);

        TextView t2 = new TextView(requireContext());
        t2.setText(safe(item.summary).isEmpty() ? "无摘要" : safe(item.summary));
        t2.setTextSize(14);
        t2.setTextColor(Color.parseColor("#334155"));
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp2.topMargin = dp(8);
        card.addView(t2, lp2);

        if (!safe(item.focusNodeId).isEmpty()) {
            TextView action = new TextView(requireContext());
            action.setText("聚焦：" + (safe(item.focusNodeTitle).isEmpty() ? item.focusNodeId : item.focusNodeTitle));
            action.setTextSize(13);
            action.setTextColor(Color.parseColor("#2563EB"));
            LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp3.topMargin = dp(8);
            card.addView(action, lp3);
            action.setOnClickListener(v -> {
                activity.getMindMapView().focusNodeById(item.focusNodeId);
                activity.getMindMapView().selectNodeById(item.focusNodeId);
                dismiss();
            });
        }
        return card;
    }

    private String formatTime(long ts) {
        return new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date(ts));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
