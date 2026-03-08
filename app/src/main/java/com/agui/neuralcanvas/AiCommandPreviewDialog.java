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

import java.util.List;

public class AiCommandPreviewDialog extends DialogFragment {

    private static AiResponse currentResponse;

    public static AiCommandPreviewDialog newInstance(AiResponse response) {
        currentResponse = response;
        return new AiCommandPreviewDialog();
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
        if (!(getActivity() instanceof MainActivity) || currentResponse == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        MainActivity activity = (MainActivity) getActivity();

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(20);
        root.setPadding(p, p, p, p);
        scrollView.addView(root);

        TextView answerView = new TextView(requireContext());
        answerView.setText("AI回复：\n" + currentResponse.getAnswer());
        answerView.setTextColor(Color.parseColor("#0F172A"));
        answerView.setTextSize(15);
        root.addView(answerView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        List<AiCommand> commands = currentResponse.getCommands();
        TextView countView = new TextView(requireContext());
        countView.setText("\n待执行命令：" + (commands == null ? 0 : commands.size()) + " 条");
        countView.setTextColor(Color.parseColor("#2563EB"));
        countView.setTextSize(14);
        root.addView(countView);

        if (commands != null) {
            for (int i = 0; i < commands.size(); i++) {
                AiCommand cmd = commands.get(i);

                TextView item = new TextView(requireContext());
                item.setText(formatCommand(i + 1, cmd));
                item.setTextColor(Color.parseColor("#334155"));
                item.setTextSize(14);
                item.setBackgroundColor(i % 2 == 0
                        ? Color.parseColor("#F8FAFC")
                        : Color.parseColor("#EEF4FF"));
                item.setPadding(dp(12), dp(12), dp(12), dp(12));

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                lp.topMargin = dp(10);
                root.addView(item, lp);
            }
        }

        return new AlertDialog.Builder(requireContext())
                .setTitle("AI命令预览")
                .setView(scrollView)
                .setNegativeButton("取消", null)
                .setPositiveButton("确认执行", (d, which) -> {
                    new AiGraphExecutor(activity.getMindMapView()).execute(currentResponse.getCommands());
                    activity.onGraphMutatedByAi();
                })
                .create();
    }

    private String formatCommand(int index, AiCommand cmd) {
        return index + ". action=" + cmd.getAction()
                + "\nnodeId=" + cmd.getNodeId()
                + "\nfrom=" + cmd.getFromNodeId()
                + "\nto=" + cmd.getToNodeId()
                + "\ntitle=" + cmd.getTitle()
                + "\nlabel=" + cmd.getLabel()
                + "\ntype=" + cmd.getType()
                + "\nshape=" + cmd.getShape()
                + "\nconnectionType=" + cmd.getConnectionType();
    }
}
