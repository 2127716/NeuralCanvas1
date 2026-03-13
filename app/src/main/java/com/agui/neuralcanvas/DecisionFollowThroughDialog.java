package com.agui.neuralcanvas;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.Map;

public class DecisionFollowThroughDialog extends DialogFragment {

    private static final String ARG_NODE_ID = "node_id";
    private Runnable onSaved;

    public static DecisionFollowThroughDialog newInstance(String nodeId) {
        DecisionFollowThroughDialog dialog = new DecisionFollowThroughDialog();
        Bundle args = new Bundle();
        args.putString(ARG_NODE_ID, nodeId);
        dialog.setArguments(args);
        return dialog;
    }

    public static DecisionFollowThroughDialog newInstance(Node node, Map<String, Node> nodes, Map<String, Connection> connections, Runnable onSaved) {
        DecisionFollowThroughDialog dialog = newInstance(node == null ? "" : node.getId());
        dialog.onSaved = onSaved;
        return dialog;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                requireContext().getResources().getDisplayMetrics()
        );
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!(getActivity() instanceof MainActivity)) {
            return super.onCreateDialog(savedInstanceState);
        }

        final MainActivity activity = (MainActivity) getActivity();
        final String nodeId = getArguments() == null ? "" : getArguments().getString(ARG_NODE_ID, "");
        final Node currentNode = findNode(activity, nodeId);

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(18);
        root.setPadding(p, p, p, p);
        scrollView.addView(root);

        TextView tip = new TextView(requireContext());
        tip.setText("把决策真正落到执行层：记录承诺、下一步动作、If-Then 触发器、失败预警和复盘节点。");
        tip.setTextColor(Color.parseColor("#475569"));
        tip.setTextSize(14f);
        root.addView(tip);

        final EditText noteInput = buildInput("承诺说明，例如：为什么做这个决定、接受什么代价");
        addWithTopMargin(root, noteInput, 14);

        final EditText nextActionInput = buildInput("最小下一步，例如：今晚 8 点写出实验提纲");
        addWithTopMargin(root, nextActionInput, 12);

        final EditText triggerInput = buildInput("If-Then 触发器，例如：如果到 20:00 坐到桌前，那么立刻打开提纲");
        addWithTopMargin(root, triggerInput, 12);

        final EditText riskSignalInput = buildInput("失败预警，例如：连续两天没推进 / 一直回避最关键部分");
        addWithTopMargin(root, riskSignalInput, 12);

        final EditText reviewDateInput = buildInput("复盘日期（可留空），例如：2026-03-20");
        reviewDateInput.setInputType(InputType.TYPE_CLASS_DATETIME);
        addWithTopMargin(root, reviewDateInput, 12);

        final CheckBox createReviewNodeCheck = new CheckBox(requireContext());
        createReviewNodeCheck.setText("自动创建“决策复盘”节点");
        createReviewNodeCheck.setChecked(true);
        addWithTopMargin(root, createReviewNodeCheck, 12);

        if (currentNode != null) {
            String oldTrigger = safe(currentNode.getTriggerCondition());
            if (!oldTrigger.isEmpty()) {
                triggerInput.setText(oldTrigger);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("决策推进")
                .setView(scrollView)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (currentNode == null) {
                Toast.makeText(requireContext(), "找不到当前节点", Toast.LENGTH_SHORT).show();
                dismiss();
                return;
            }

            String note = safe(noteInput.getText().toString());
            String nextAction = safe(nextActionInput.getText().toString());
            String trigger = safe(triggerInput.getText().toString());
            String riskSignal = safe(riskSignalInput.getText().toString());
            String reviewDate = safe(reviewDateInput.getText().toString());

            if (!trigger.isEmpty()) currentNode.setTriggerCondition(trigger);
            if (!reviewDate.isEmpty()) currentNode.setReviewAt(reviewDate);

            StringBuilder merged = new StringBuilder(safe(currentNode.getContent()));
            appendLine(merged, "【决策承诺】");
            if (!note.isEmpty()) appendLine(merged, "承诺说明：" + note);
            if (!nextAction.isEmpty()) appendLine(merged, "最小下一步：" + nextAction);
            if (!trigger.isEmpty()) appendLine(merged, "触发器：" + trigger);
            if (!riskSignal.isEmpty()) appendLine(merged, "失败预警：" + riskSignal);
            if (!reviewDate.isEmpty()) appendLine(merged, "复盘日期：" + reviewDate);
            currentNode.setContent(merged.toString().trim());

            if (!nextAction.isEmpty()) {
                Node actionNode = new Node(
                        "决策下一步",
                        nextAction,
                        currentNode.getX() + 320f,
                        currentNode.getY() - 120f,
                        Node.NodeType.TASK
                );
                if (!trigger.isEmpty()) actionNode.setTriggerCondition(trigger);
                activity.getMindMapView().addNode(actionNode);
                activity.getMindMapView().addConnection(
                        new Connection(
                                currentNode.getId(),
                                actionNode.getId(),
                                Connection.ConnectionType.LEADS_TO,
                                "落实为行动"
                        )
                );
            }

            if (createReviewNodeCheck.isChecked()) {
                Node reviewNode = new Node(
                        "决策复盘",
                        "记录执行结果、偏差和下次修正。",
                        currentNode.getX() + 340f,
                        currentNode.getY() + 120f,
                        Node.NodeType.REVIEW
                );

                StringBuilder reviewContent = new StringBuilder(safe(reviewNode.getContent()));
                if (!note.isEmpty()) appendLine(reviewContent, "承诺说明：" + note);
                if (!riskSignal.isEmpty()) appendLine(reviewContent, "重点检查的失败预警：" + riskSignal);
                reviewNode.setContent(reviewContent.toString().trim());
                if (!reviewDate.isEmpty()) reviewNode.setReviewAt(reviewDate);

                activity.getMindMapView().addNode(reviewNode);
                activity.getMindMapView().addConnection(
                        new Connection(
                                currentNode.getId(),
                                reviewNode.getId(),
                                Connection.ConnectionType.LEADS_TO,
                                "完成后复盘"
                        )
                );
            }

            activity.getMindMapView().invalidate();
            if (onSaved != null) onSaved.run();
            Toast.makeText(requireContext(), "决策推进信息已保存", Toast.LENGTH_SHORT).show();
            dismiss();
        }));

        return dialog;
    }

    private EditText buildInput(String hint) {
        EditText input = new EditText(requireContext());
        input.setHint(hint);
        input.setTextColor(Color.parseColor("#0F172A"));
        input.setHintTextColor(Color.parseColor("#94A3B8"));
        input.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#60A5FA")));
        input.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return input;
    }

    private void addWithTopMargin(LinearLayout root, View view, int marginTopDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(marginTopDp);
        root.addView(view, lp);
    }

    private Node findNode(MainActivity activity, String nodeId) {
        if (activity == null || activity.getMindMapView() == null) return null;
        Map<String, Node> nodes = activity.getMindMapView().getNodesInternal();
        if (nodes == null) return null;
        return nodes.get(nodeId);
    }

    private static void appendLine(StringBuilder sb, String line) {
        if (line == null || line.trim().isEmpty()) return;
        if (sb.length() > 0) sb.append("\n");
        sb.append(line.trim());
    }

    private static String safe(String text) {
        return text == null ? "" : text.trim();
    }
}
