package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.List;

public class AiCommandPreviewDialog extends DialogFragment {

    private static final String ARG_JSON = "arg_json";

    public static AiCommandPreviewDialog newInstance(AiResponse response) {
        AiCommandPreviewDialog dialog = new AiCommandPreviewDialog();
        Bundle args = new Bundle();
        args.putString(ARG_JSON, AiJsonParser.toJson(response));
        dialog.setArguments(args);
        return dialog;
    }

    public static AiCommandPreviewDialog newInstanceFromJson(String responseJson) {
        AiCommandPreviewDialog dialog = new AiCommandPreviewDialog();
        Bundle args = new Bundle();
        args.putString(ARG_JSON, responseJson == null ? "{}" : responseJson);
        dialog.setArguments(args);
        return dialog;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, requireContext().getResources().getDisplayMetrics()
        );
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!(getActivity() instanceof MainActivity) || getArguments() == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        MainActivity activity = (MainActivity) getActivity();
        final SimpleDataManager dataManager = activity.getDataManager();
        AiResponse response;
        try {
            response = AiJsonParser.parseResponse(getArguments().getString(ARG_JSON, "{}"));
        } catch (Exception e) {
            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setTitle("AI命令预览")
                    .setMessage("命令解析失败：" + e.getMessage())
                    .setPositiveButton("关闭", null)
                    .create();
            dialog.setOnShowListener(d -> MonetDialogStyler.apply(dialog, requireContext()));
            return dialog;
        }

        final AiResponse finalResponse = response;
        final boolean[] decided = {false};

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(ThemeManager.getDialogBg());

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(ThemeManager.getDialogBg());
        scrollView.addView(root);

        TextView title = new TextView(requireContext());
        title.setText("AI 命令预览");
        root.addView(title);

        TextView sub = new TextView(requireContext());
        sub.setText("先看摘要，再决定是否执行");
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(6);
        root.addView(sub, subLp);
        MonetDialogStyler.styleHeader(title, sub);

        TextView answer = new TextView(requireContext());
        answer.setText(response.getAnswer().isEmpty() ? "AI未提供额外说明" : response.getAnswer());
        answer.setTextColor(ThemeManager.getTextSecondary());
        answer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        answer.setPadding(dp(16), dp(14), dp(16), dp(14));
        answer.setBackground(MonetDialogStyler.cardBg());
        LinearLayout.LayoutParams answerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        answerLp.topMargin = dp(14);
        root.addView(answer, answerLp);

        List<AiCommand> commands = response.getCommands();
        TextView count = new TextView(requireContext());
        count.setText("待执行操作：" + (commands == null ? 0 : commands.size()) + " 条");
        count.setTextColor(ThemeManager.getAccent());
        count.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        LinearLayout.LayoutParams countLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        countLp.topMargin = dp(16);
        root.addView(count, countLp);

        if (commands != null) {
            for (int i = 0; i < commands.size(); i++) {
                root.addView(buildCommandCard(i + 1, commands.get(i)));
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(scrollView)
                .setNegativeButton("取消", (d, which) -> {
                    decided[0] = true;
                    SuggestionFeedbackEngine.recordRejected(dataManager, finalResponse, "manual_preview");
                })
                .setPositiveButton("确认执行", (d, which) -> {
                    decided[0] = true;
                    new AiGraphExecutor(activity.getMindMapView()).execute(finalResponse.getCommands());
                    activity.onGraphMutatedByAi();
                    SuggestionFeedbackEngine.recordAccepted(dataManager, finalResponse, "manual_preview");
                    SuggestionFeedbackEngine.recordEffectiveness(dataManager, finalResponse);
                })
                .create();

        dialog.setOnDismissListener(d -> {
            if (!decided[0]) {
                SuggestionFeedbackEngine.recordRejected(dataManager, finalResponse, "manual_preview");
            }
        });
        dialog.setOnShowListener(d -> MonetDialogStyler.apply(dialog, requireContext()));
        return dialog;
    }

    private LinearLayout buildCommandCard(int index, AiCommand cmd) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(MonetDialogStyler.cardBg());

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        card.setLayoutParams(lp);

        TextView title = new TextView(requireContext());
        title.setText(index + ". " + readableAction(cmd.getAction()));
        title.setTextColor(ThemeManager.getTextPrimary());
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        card.addView(title);

        TextView detail = new TextView(requireContext());
        detail.setText(buildReadableDetail(cmd));
        detail.setTextColor(ThemeManager.getTextSecondary());
        detail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        LinearLayout.LayoutParams detailLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        detailLp.topMargin = dp(8);
        card.addView(detail, detailLp);

        if (!cmd.getReason().isEmpty()) {
            TextView reason = new TextView(requireContext());
            reason.setText("原因：" + cmd.getReason());
            reason.setTextColor(ThemeManager.getAccent());
            reason.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.topMargin = dp(8);
            card.addView(reason, rlp);
        }

        return card;
    }

    private String readableAction(String action) {
        if (action == null) return "未知操作";
        switch (action) {
            case "create_node": return "创建节点";
            case "update_node": return "修改节点";
            case "delete_node": return "删除节点";
            case "create_connection": return "创建连线";
            case "update_connection": return "修改连线";
            case "delete_connection": return "删除连线";
            case "focus_node": return "聚焦节点";
            case "auto_layout": return "自动布局";
            default: return action;
        }
    }

    private String buildReadableDetail(AiCommand cmd) {
        StringBuilder sb = new StringBuilder();
        if (!cmd.getTempId().isEmpty()) sb.append("临时引用：").append(cmd.getTempId()).append("\n");
        if (!cmd.getNodeId().isEmpty()) sb.append("节点：").append(cmd.getNodeId()).append("\n");
        if (!cmd.getTitle().isEmpty()) sb.append("标题：").append(cmd.getTitle()).append("\n");
        if (!cmd.getContent().isEmpty()) sb.append("内容：").append(trimLong(cmd.getContent(), 120)).append("\n");
        if (!cmd.getType().isEmpty()) sb.append("类型：").append(cmd.getType()).append("\n");
        if (!cmd.getShape().isEmpty()) sb.append("形状：").append(cmd.getShape()).append("\n");
        if (!cmd.getFromNodeId().isEmpty() || !cmd.getToNodeId().isEmpty()) {
            sb.append("连接：").append(cmd.getFromNodeId()).append(" -> ").append(cmd.getToNodeId()).append("\n");
        }
        if (!cmd.getLabel().isEmpty()) sb.append("连线文字：").append(cmd.getLabel()).append("\n");
        if (!cmd.getConnectionType().isEmpty()) sb.append("连线类型：").append(cmd.getConnectionType()).append("\n");
        if (!cmd.getConnectionColorHex().isEmpty()) sb.append("连线颜色：").append(cmd.getConnectionColorHex()).append("\n");
        if (cmd.getStrokeWidth() != null) sb.append("连线粗细：").append(cmd.getStrokeWidth()).append("\n");
        if (cmd.getX() != null || cmd.getY() != null) sb.append("位置：(").append(cmd.getX()).append(", ").append(cmd.getY()).append(")\n");
        if (cmd.getWidth() != null || cmd.getHeight() != null) sb.append("尺寸：").append(cmd.getWidth()).append(" x ").append(cmd.getHeight()).append("\n");
        if (Boolean.TRUE.equals(cmd.getApplyAutoLayoutAfter())) sb.append("执行后自动布局：是\n");
        String text = sb.toString().trim();
        return text.isEmpty() ? "无附加详情" : text;
    }

    private String trimLong(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
