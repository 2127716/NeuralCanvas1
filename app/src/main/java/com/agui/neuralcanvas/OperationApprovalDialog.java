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

public class OperationApprovalDialog extends DialogFragment {

    public static OperationApprovalDialog newInstance() {
        return new OperationApprovalDialog();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, requireContext().getResources().getDisplayMetrics()
        );
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!(getActivity() instanceof MainActivity)) return super.onCreateDialog(savedInstanceState);
        MainActivity activity = (MainActivity) getActivity();
        SimpleDataManager dataManager = activity.getDataManager();
        PendingOperationBundle bundle = dataManager.loadPendingOperationBundle();
        if (bundle == null || bundle.responseJson == null || bundle.responseJson.trim().isEmpty()) {
            return new AlertDialog.Builder(requireContext())
                    .setTitle("待确认改动")
                    .setMessage("当前没有待确认的 AI 改动。")
                    .setPositiveButton("关闭", null)
                    .create();
        }

        AiResponse response;
        try {
            response = AiJsonParser.parseResponse(bundle.responseJson);
        } catch (Exception e) {
            return new AlertDialog.Builder(requireContext())
                    .setTitle("待确认改动")
                    .setMessage("解析待确认改动失败：" + e.getMessage())
                    .setPositiveButton("关闭", null)
                    .create();
        }

        ScrollView scroll = new ScrollView(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(18);
        root.setPadding(p, p, p, p);
        scroll.addView(root);

        TextView summary = new TextView(requireContext());
        summary.setTextColor(Color.parseColor("#0F172A"));
        summary.setTextSize(14);
        summary.setText((bundle.summary == null ? "" : bundle.summary)
                + "\n\n"
                + (bundle.impactSummary == null ? "" : bundle.impactSummary));
        root.addView(summary);

        TextView hint = new TextView(requireContext());
        hint.setTextColor(Color.parseColor("#475569"));
        hint.setTextSize(13);
        hint.setText("\n这些改动被判定为中高风险或结构改动较大，建议人工确认后再执行。");
        root.addView(hint);

        return new AlertDialog.Builder(requireContext())
                .setTitle("AI 待确认改动")
                .setView(scroll)
                .setNegativeButton("拒绝", (d, w) -> {
                    SuggestionFeedbackEngine.recordRejected(dataManager, response, "approval_queue");
                    dataManager.clearPendingOperationBundle();
                })
                .setNeutralButton("查看详情", (d, w) -> {
                    dismiss();
                    AiCommandPreviewDialog.newInstanceFromJson(bundle.responseJson)
                            .show(activity.getSupportFragmentManager(), "ai_command_preview");
                })
                .setPositiveButton("确认执行", (d, w) -> {
                    new AiGraphExecutor(activity.getMindMapView()).execute(response.getCommands());
                    activity.onGraphMutatedByAi();
                    SuggestionFeedbackEngine.recordAccepted(dataManager, response, "approval_queue");
                    SuggestionFeedbackEngine.recordEffectiveness(dataManager, response);
                    dataManager.clearPendingOperationBundle();
                })
                .create();
    }
}
