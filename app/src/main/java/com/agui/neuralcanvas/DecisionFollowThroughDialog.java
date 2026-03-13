package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DecisionFollowThroughDialog extends DialogFragment {
    private static Node currentNode;
    private static Map<String, Node> currentNodes;
    private static Map<String, Connection> currentConnections;
    private static Runnable onSaved;

    public static DecisionFollowThroughDialog newInstance(Node node, Map<String, Node> nodes, Map<String, Connection> connections, Runnable callback) {
        currentNode = node; currentNodes = nodes; currentConnections = connections; onSaved = callback; return new DecisionFollowThroughDialog();
    }

    private int dp(int value) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, requireContext().getResources().getDisplayMetrics()); }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (currentNode == null) return new AlertDialog.Builder(requireContext()).setMessage("没有可记录的决策节点").setPositiveButton("关闭", null).create();
        DecisionEngine.DecisionReport report = DecisionEngine.analyze(currentNode, currentNodes, currentConnections);
        List<Node> options = report.options;
        if (options.isEmpty()) return new AlertDialog.Builder(requireContext()).setTitle("决策落地").setMessage("当前附近没有方案节点（OPTION）。").setPositiveButton("关闭", null).create();

        ScrollView scroll = new ScrollView(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(18); root.setPadding(p,p,p,p); scroll.addView(root);

        TextView hint = new TextView(requireContext());
        hint.setText("把决策真正落地：选定当前方案、记录信心，并生成后续复盘锚点。");
        hint.setTextColor(Color.parseColor("#334155"));
        root.addView(hint);

        Spinner optionSpinner = new Spinner(requireContext());
        List<String> labels = new ArrayList<>();
        int defaultIndex = 0;
        String chosenId = GraphMetaHelper.getString(currentNode, "decision_chosen_option_id", "");
        for (int i = 0; i < options.size(); i++) {
            Node option = options.get(i);
            String label = (option.getTitle() == null || option.getTitle().trim().isEmpty()) ? "(无标题方案)" : option.getTitle().trim();
            labels.add(label);
            if (option.getId().equals(chosenId)) defaultIndex = i;
        }
        optionSpinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, labels));
        optionSpinner.setSelection(defaultIndex);
        root.addView(optionSpinner);

        EditText confidenceInput = new EditText(requireContext());
        confidenceInput.setHint("当前决策信心 0~1，例如 0.72");
        confidenceInput.setText(String.valueOf(currentNode.getConfidence() > 0 ? currentNode.getConfidence() : 0.65f));
        root.addView(confidenceInput);

        EditText noteInput = new EditText(requireContext());
        noteInput.setHint("为什么现在选这个方案？失败信号是什么？何时复盘？");
        noteInput.setMinLines(4);
        root.addView(noteInput);

        return new AlertDialog.Builder(requireContext())
                .setTitle("决策落地")
                .setView(scroll)
                .setPositiveButton("保存并生成复盘锚点", (d,w) -> {
                    int idx = optionSpinner.getSelectedItemPosition();
                    Node chosen = idx >= 0 && idx < options.size() ? options.get(idx) : options.get(0);
                    float conf = parseFloat(confidenceInput.getText().toString(), 0.65f);
                    currentNode.setConfidence(Math.max(0.05f, Math.min(0.99f, conf)));
                    GraphMetaHelper.put(currentNode, "decision_chosen_option_id", chosen.getId());
                    GraphMetaHelper.put(currentNode, "decision_chosen_option_title", safe(chosen.getTitle()));
                    GraphMetaHelper.put(currentNode, "decision_decided_at", String.valueOf(System.currentTimeMillis()));
                    String note = safe(noteInput.getText().toString());
                    if (!note.isEmpty()) GraphMetaHelper.put(currentNode, "decision_commit_note", note);

                    Node review = new Node("决策复盘｜" + safe(chosen.getTitle()),
                            "已选方案：" + safe(chosen.getTitle()) + "
初始信心：" + currentNode.getConfidence() + "
请在执行后复盘：实际发生了什么？哪些证据支持/反驳了原判断？",
                            currentNode.getX() + 340f, currentNode.getY() + 120f, Node.NodeType.REVIEW);
                    review.setProjectId(WorkflowEngine.resolveOwnerId(currentNode));
                    review.setPriority(Math.max(3, currentNode.getPriority()));
                    review.setReviewAt("7天后复盘");
                    if (!note.isEmpty()) review.setContent(review.getContent() + "

承诺说明：" + note);

                    if (currentNodes != null) currentNodes.put(review.getId(), review);
                    if (currentConnections != null) {
                        Connection c = new Connection(currentNode.getId(), review.getId(), Connection.ConnectionType.LEADS_TO, "结果复盘");
                        currentConnections.put(c.getId(), c);
                        currentNode.addConnection(c.getId());
                        review.addConnection(c.getId());
                    }
                    if (onSaved != null) onSaved.run();
                })
                .setNegativeButton("取消", null)
                .create();
    }

    private float parseFloat(String text, float fallback) { try { return Float.parseFloat(text == null || text.trim().isEmpty() ? String.valueOf(fallback) : text.trim()); } catch (Exception e) { return fallback; } }
    private String safe(String v) { return v == null ? "" : v.trim(); }
}
