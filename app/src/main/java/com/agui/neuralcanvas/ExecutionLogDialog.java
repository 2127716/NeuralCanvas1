package com.agui.neuralcanvas;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.Map;

public class ExecutionLogDialog extends DialogFragment {

    private static final String ARG_NODE_ID = "node_id";

    public static ExecutionLogDialog newInstance(String nodeId) {
        ExecutionLogDialog dialog = new ExecutionLogDialog();
        Bundle args = new Bundle();
        args.putString(ARG_NODE_ID, nodeId);
        dialog.setArguments(args);
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

        MainActivity activity = (MainActivity) getActivity();
        String nodeId = getArguments() == null ? "" : getArguments().getString(ARG_NODE_ID, "");
        Node node = findNode(activity, nodeId);

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(18);
        root.setPadding(p, p, p, p);
        scrollView.addView(root);

        EditText noteInput = new EditText(requireContext());
        noteInput.setHint("记录这次执行结果、偏差、阻碍或复盘结论");
        noteInput.setMinLines(6);
        noteInput.setTextColor(Color.parseColor("#0F172A"));
        noteInput.setHintTextColor(Color.parseColor("#94A3B8"));
        noteInput.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#60A5FA")));
        root.addView(noteInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("执行记录")
                .setView(scrollView)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (node == null) {
                Toast.makeText(requireContext(), "找不到当前节点", Toast.LENGTH_SHORT).show();
                dismiss();
                return;
            }

            String note = safe(noteInput.getText().toString());
            if (note.isEmpty()) {
                noteInput.setError("请输入记录内容");
                return;
            }

            String old = safe(node.getContent());
            String merged = old.isEmpty() ? note : (old + "\n" + note);
            node.setContent(merged);

            if (activity.getMindMapView() != null) {
                activity.getMindMapView().invalidate();
            }

            Toast.makeText(requireContext(), "执行记录已保存", Toast.LENGTH_SHORT).show();
            dismiss();
        }));

        return dialog;
    }

    private Node findNode(MainActivity activity, String nodeId) {
        if (activity == null || activity.getMindMapView() == null) return null;
        Map<String, Node> nodes = activity.getMindMapView().getNodesInternal();
        if (nodes == null) return null;
        return nodes.get(nodeId);
    }

    private static String safe(String text) {
        return text == null ? "" : text.trim();
    }
}
