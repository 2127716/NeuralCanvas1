package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class ExecutionLogDialog extends DialogFragment {
    private static Node currentNode;
    private static Runnable onSaved;
    private static java.util.Map<String, Node> currentNodes;

    public static ExecutionLogDialog newInstance(Node node, java.util.Map<String, Node> nodes, Runnable callback) {
        currentNode = node;
        currentNodes = nodes;
        onSaved = callback;
        return new ExecutionLogDialog();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, requireContext().getResources().getDisplayMetrics());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (currentNode == null) return new AlertDialog.Builder(requireContext()).setMessage("没有可记录的节点").setPositiveButton("关闭", null).create();

        ScrollView scroll = new ScrollView(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(18);
        root.setPadding(p, p, p, p);
        scroll.addView(root);

        TextView intro = new TextView(requireContext());
        intro.setText("把执行数据真正写回节点：这次推进了多少、触发器命中没有、现在状态怎样。");
        intro.setTextColor(Color.parseColor("#334155"));
        root.addView(intro);

        ReferenceForecastEngine.ForecastReport forecast = ReferenceForecastEngine.analyze(currentNode, currentNodes);
        TextView forecastView = new TextView(requireContext());
        forecastView.setText("参考类预测：" + ReferenceForecastEngine.buildSummary(forecast));
        forecastView.setTextColor(Color.parseColor("#0F172A"));
        forecastView.setPadding(0, dp(10), 0, 0);
        root.addView(forecastView);

        EditText addedHoursInput = new EditText(requireContext());
        addedHoursInput.setHint("本次实际投入小时，例如 0.5");
        addedHoursInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        addedHoursInput.setText("0.5");
        root.addView(addedHoursInput);

        EditText progressInput = new EditText(requireContext());
        progressInput.setHint("本次推进了什么、卡在哪、下一步是什么");
        progressInput.setMinLines(4);
        progressInput.setTextColor(Color.parseColor("#0F172A"));
        root.addView(progressInput);

        TextView triggerTitle = new TextView(requireContext());
        triggerTitle.setText("If-Then / 触发器结果");
        triggerTitle.setTextColor(Color.parseColor("#0F172A"));
        triggerTitle.setPadding(0, dp(10), 0, 0);
        root.addView(triggerTitle);

        RadioGroup triggerGroup = new RadioGroup(requireContext());
        triggerGroup.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton triggerNone = new RadioButton(requireContext()); triggerNone.setText("不记录"); triggerGroup.addView(triggerNone);
        RadioButton triggerHit = new RadioButton(requireContext()); triggerHit.setText("命中"); triggerGroup.addView(triggerHit);
        RadioButton triggerMiss = new RadioButton(requireContext()); triggerMiss.setText("落空"); triggerGroup.addView(triggerMiss);
        triggerNone.setChecked(true);
        root.addView(triggerGroup);

        TextView statusTitle = new TextView(requireContext());
        statusTitle.setText("当前状态");
        statusTitle.setTextColor(Color.parseColor("#0F172A"));
        statusTitle.setPadding(0, dp(10), 0, 0);
        root.addView(statusTitle);

        RadioGroup statusGroup = new RadioGroup(requireContext());
        statusGroup.setOrientation(RadioGroup.VERTICAL);
        RadioButton active = new RadioButton(requireContext()); active.setText("继续推进"); statusGroup.addView(active);
        RadioButton done = new RadioButton(requireContext()); done.setText("标记完成"); statusGroup.addView(done);
        RadioButton blocked = new RadioButton(requireContext()); blocked.setText("暂时受阻"); statusGroup.addView(blocked);
        active.setChecked(true);
        root.addView(statusGroup);

        return new AlertDialog.Builder(requireContext())
                .setTitle("执行回填")
                .setView(scroll)
                .setPositiveButton("保存", (d, w) -> {
                    float addHours = parseFloat(addedHoursInput.getText().toString(), 0f);
                    if (addHours > 0f) currentNode.setActualEffort(currentNode.getActualEffort() + addHours);
                    String note = safe(progressInput.getText().toString());
                    if (!note.isEmpty()) {
                        String old = GraphMetaHelper.getString(currentNode, "execution_log", "");
                        String merged = old.isEmpty() ? note : (old + "
---
" + note);
                        GraphMetaHelper.put(currentNode, "execution_log", merged);
                        GraphMetaHelper.put(currentNode, "last_progress_note", note);
                    }
                    if (triggerHit.isChecked()) FocusSessionEngine.markTrigger(currentNode, true);
                    else if (triggerMiss.isChecked()) FocusSessionEngine.markTrigger(currentNode, false);

                    if (done.isChecked()) currentNode.setStatus(Node.NodeStatus.DONE);
                    else if (blocked.isChecked()) currentNode.setStatus(Node.NodeStatus.BLOCKED);
                    else currentNode.setStatus(Node.NodeStatus.ACTIVE);

                    ReferenceForecastEngine.applyForecastToNode(currentNode, forecast);
                    GraphMetaHelper.putLong(currentNode, "last_execution_log_at", System.currentTimeMillis());
                    if (onSaved != null) onSaved.run();
                })
                .setNegativeButton("取消", null)
                .create();
    }

    private String safe(String v) { return v == null ? "" : v.trim(); }
    private float parseFloat(String text, float fallback) { try { return Float.parseFloat(text == null || text.trim().isEmpty() ? String.valueOf(fallback) : text.trim()); } catch (Exception e) { return fallback; } }
}
