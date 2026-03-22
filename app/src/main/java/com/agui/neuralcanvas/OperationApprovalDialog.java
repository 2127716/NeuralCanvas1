package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
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
            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setTitle("待确认改动")
                    .setMessage("当前没有待确认的 AI 改动。")
                    .setPositiveButton("关闭", null)
                    .create();
            dialog.setOnShowListener(d -> MonetDialogStyler.apply(dialog, requireContext()));
            return dialog;
        }

        AiResponse response;
        try {
            response = AiJsonParser.parseResponse(bundle.responseJson);
        } catch (Exception e) {
            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setTitle("待确认改动")
                    .setMessage("解析待确认改动失败：" + e.getMessage())
                    .setPositiveButton("关闭", null)
                    .create();
            dialog.setOnShowListener(d -> MonetDialogStyler.apply(dialog, requireContext()));
            return dialog;
        }

        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(ThemeManager.getDialogBg());

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(ThemeManager.getDialogBg());
        scroll.addView(root);

        TextView title = new TextView(requireContext());
        title.setText("AI 待确认改动");
        root.addView(title);

        TextView sub = new TextView(requireContext());
        sub.setText("中高风险改动先确认，再执行");
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(6);
        root.addView(sub, subLp);
        MonetDialogStyler.styleHeader(title, sub);

        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(MonetDialogStyler.cardBg());
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = dp(16);
        root.addView(card, cardLp);

        TextView focus = new TextView(requireContext());
        focus.setText("焦点节点：" + (bundle.focusNodeTitle == null || bundle.focusNodeTitle.trim().isEmpty() ? "未指定" : bundle.focusNodeTitle));
        focus.setTextColor(ThemeManager.getTextPrimary());
        focus.setTypeface(focus.getTypeface(), Typeface.BOLD);
        card.addView(focus);

        TextView impact = new TextView(requireContext());
        impact.setText((bundle.impactSummary == null ? "" : bundle.impactSummary));
        impact.setTextColor(ThemeManager.getTextSecondary());
        impact.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        LinearLayout.LayoutParams impactLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        impactLp.topMargin = dp(10);
        card.addView(impact, impactLp);

        TextView hint = new TextView(requireContext());
        hint.setText("这批改动已被自治策略判定为需要人工确认。");
        hint.setTextColor(ThemeManager.getTextSecondary());
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = dp(14);
        root.addView(hint, hintLp);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
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
        dialog.setOnShowListener(d -> MonetDialogStyler.apply(dialog, requireContext()));
        return dialog;
    }
}
