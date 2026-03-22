package com.agui.neuralcanvas;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.Map;

public class GraphInsightDialog extends DialogFragment {
    private static Node currentNode;
    private static Map<String, Node> currentNodes;
    private static Map<String, Connection> currentConnections;

    public static GraphInsightDialog newInstance(Node node, Map<String, Node> nodes, Map<String, Connection> connections) {
        currentNode = node;
        currentNodes = nodes;
        currentConnections = connections;
        return new GraphInsightDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        GraphInsightEngine.InsightReport report = GraphInsightEngine.analyze(currentNodes, currentConnections);

        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(ThemeManager.getDialogBg());

        LinearLayout root = MonetDialogStyler.buildRoot(requireContext());
        scroll.addView(root);

        TextView title = new TextView(requireContext());
        title.setText("图谱结构智能");
        root.addView(title);

        TextView sub = new TextView(requireContext());
        sub.setText("结构缺口、孤岛、证据与下一步统一显示");
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = MonetDialogStyler.dp(requireContext(), 6);
        root.addView(sub, subLp);
        MonetDialogStyler.styleHeader(title, sub);

        TextView tv = MonetDialogStyler.body(requireContext(), GraphInsightEngine.buildReadableReport(report, currentNode));
        tv.setPadding(MonetDialogStyler.dp(requireContext(), 16), MonetDialogStyler.dp(requireContext(), 14),
                MonetDialogStyler.dp(requireContext(), 16), MonetDialogStyler.dp(requireContext(), 14));
        tv.setBackground(MonetDialogStyler.cardBg());
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tvLp.topMargin = MonetDialogStyler.dp(requireContext(), 14);
        root.addView(tv, tvLp);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(scroll)
                .setPositiveButton("关闭", null)
                .create();
        dialog.setOnShowListener(d -> MonetDialogStyler.apply(dialog, requireContext()));
        return dialog;
    }
}
