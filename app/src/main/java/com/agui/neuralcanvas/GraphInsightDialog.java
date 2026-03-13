package com.agui.neuralcanvas;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.Map;

public class GraphInsightDialog extends DialogFragment {
    private static Node currentNode; private static Map<String, Node> currentNodes; private static Map<String, Connection> currentConnections;
    public static GraphInsightDialog newInstance(Node node, Map<String, Node> nodes, Map<String, Connection> connections) { currentNode = node; currentNodes = nodes; currentConnections = connections; return new GraphInsightDialog(); }
    @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        GraphInsightEngine.InsightReport report = GraphInsightEngine.analyze(currentNodes, currentConnections);
        TextView tv = new TextView(requireContext()); int p = (int) (16 * requireContext().getResources().getDisplayMetrics().density); tv.setPadding(p,p,p,p); tv.setText(GraphInsightEngine.buildReadableReport(report, currentNode));
        ScrollView scroll = new ScrollView(requireContext()); scroll.addView(tv);
        return new AlertDialog.Builder(requireContext()).setTitle("图谱结构智能").setView(scroll).setPositiveButton("关闭", null).create();
    }
}
