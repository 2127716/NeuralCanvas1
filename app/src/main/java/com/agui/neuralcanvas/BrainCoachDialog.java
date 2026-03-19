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

import java.util.List;

public class BrainCoachDialog extends DialogFragment {

    private static Node pendingNode;

    public static void show(MainActivity activity, Node node) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        pendingNode = node;
        new BrainCoachDialog().show(activity.getSupportFragmentManager(), "brain_coach_dialog");
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, requireContext().getResources().getDisplayMetrics()
        );
    }

    private android.graphics.drawable.GradientDrawable bg(String color, String stroke) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Color.parseColor(color));
        gd.setCornerRadius(dp(20));
        gd.setStroke(dp(1), Color.parseColor(stroke));
        return gd;
    }

    private TextView buildCard(String title, String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(title + "\n" + text);
        tv.setTextColor(Color.parseColor("#E2E8F0"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setPadding(dp(14), dp(14), dp(14), dp(14));
        tv.setBackground(bg("#111827", "#24324A"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(10);
        tv.setLayoutParams(lp);
        return tv;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final MainActivity activity = (MainActivity) getActivity();
        if (activity == null || activity.getMindMapView() == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        Node workingNode = pendingNode;
        if (workingNode == null) {
            List<String> selectedIds = activity.getMindMapView().getSelectedNodeIds();
            if (selectedIds != null && !selectedIds.isEmpty()) {
                workingNode = activity.getMindMapView().getNodesInternal().get(selectedIds.get(0));
            }
        }
        final Node node = workingNode;

        ProjectHealthEngine.ProjectHealthReport health = ProjectHealthEngine.analyze(
                activity.getMindMapView().getNodesInternal(),
                activity.getMindMapView().getConnectionsInternal()
        );

        ScrollView sv = new ScrollView(requireContext());
        sv.setFillViewport(true);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.parseColor("#0B1020"));
        sv.addView(root);

        TextView title = new TextView(requireContext());
        title.setText("智能节点神经系统");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTextColor(Color.parseColor("#F8FAFC"));
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView sub = new TextView(requireContext());
        sub.setText(node == null
                ? "先选中一个节点，系统会给出更聚焦的建议。"
                : "当前焦点：" + (WorkflowEngine.safe(node.getTitle()).isEmpty() ? "(无标题)" : WorkflowEngine.safe(node.getTitle())));
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        sub.setTextColor(Color.parseColor("#94A3B8"));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(6);
        sub.setLayoutParams(subLp);
        root.addView(sub);

        root.addView(buildCard("全局巡检", ProjectHealthEngine.buildSummary(health)));

        if (node != null) {
            WorkflowMethodRecommendationEngine.Analysis analysis =
                    WorkflowMethodRecommendationEngine.analyze(
                            node,
                            activity.getMindMapView().getNodesInternal(),
                            activity.getMindMapView().getConnectionsInternal()
                    );
            root.addView(buildCard("当前节点分析", WorkflowMethodRecommendationEngine.buildReadableReport(analysis)));

            NodeFocusGuideEngine.GuideReport guide = NodeFocusGuideEngine.buildForNode(activity, node);
            if (!guide.items.isEmpty()) {
                NodeFocusGuideEngine.GuideItem first = guide.items.get(0);
                root.addView(buildCard("先做什么", first.title + " —— " + first.hint));
            }
        }

        root.addView(buildCard("智能建议",
                "建议把 AI 助手当成图谱操作员，而不是普通聊天框：\n" +
                        "1. 先体检/推荐\n" +
                        "2. 再一键修复\n" +
                        "3. 再交给 AI 做红队、执行补全或学习补全\n" +
                        "4. 修复后跟着引导聚焦到第一关键节点"));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(sv)
                .setPositiveButton("项目巡检", (d, w) ->
                        ProjectHealthDialog.newInstance().show(activity.getSupportFragmentManager(), "project_health_dialog"))
                .setNeutralButton("AI 助手", (d, w) -> {
                    final String preset = node == null
                            ? "请先对当前图谱做系统体检，告诉我现在最该先补哪一块，并给出保守的命令式建议。"
                            : "请把当前节点作为核心，做系统分析：缺口、最优下一步、风险、复盘锚点，并优先给出保守可执行建议。";
                    activity.showAiAssistantDialogWithPrompt(preset);
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
