package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class NodeEditDialog extends DialogFragment {

    public interface NodeEditListener {
        void onNodeUpdated(Node node);
        void onNodeDeleted(Node node);
    }

    private static Node currentNode;
    private static MindMapView currentMindMapView;

    public static NodeEditDialog newInstance(Node node, MindMapView mindMapView) {
        currentNode = node;
        currentMindMapView = mindMapView;
        return new NodeEditDialog();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                requireContext().getResources().getDisplayMetrics());
    }

    private TextView section(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(ThemeManager.getAccent());
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        return tv;
    }

    private EditText input(String hint, String value, int inputType, int minLines) {
        EditText et = new EditText(requireContext());
        et.setHint(hint);
        et.setText(value == null ? "" : value);
        et.setInputType(inputType);
        et.setMinLines(minLines);
        et.setTextColor(ThemeManager.getTextPrimary());
        et.setHintTextColor(ThemeManager.getTextSecondary());
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(ThemeManager.getEditTextBg());
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), ThemeManager.getEditTextStroke());
        et.setBackground(bg);
        return et;
    }

    private Spinner spinner(String[] labels, int selected) {
        Spinner spinner = new Spinner(requireContext());
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selected);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(ThemeManager.getSpinnerBg());
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), ThemeManager.getEditTextStroke());
        spinner.setBackground(bg);
        return spinner;
    }

    private View card(View... children) {
        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(ThemeManager.getChipBg());
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), ThemeManager.getChipStroke());
        box.setBackground(bg);
        for (int i = 0; i < children.length; i++) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.topMargin = dp(12);
            box.addView(children[i], lp);
        }
        return box;
    }

    private TextView chip(String text, View.OnClickListener l) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        tv.setTextColor(ThemeManager.getTextPrimary());
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        tv.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(ThemeManager.getSpinnerBg());
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), ThemeManager.getChipStroke());
        tv.setBackground(bg);
        tv.setOnClickListener(l);
        return tv;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getActivity() == null || currentNode == null || currentMindMapView == null) return super.onCreateDialog(savedInstanceState);

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(ThemeManager.getDialogBg());

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(12));
        scrollView.addView(root);

        TextView title = new TextView(requireContext());
        title.setText("节点详情");
        title.setTextColor(ThemeManager.getTextPrimary());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        root.addView(title);

        TextView summary = new TextView(requireContext());
        summary.setText("把原来塞满屏幕的表单拆成卡片。高频操作前置，低频字段收进下面。长按节点仍可直接打开。 ");
        summary.setTextColor(ThemeManager.getTextSecondary());
        summary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        LinearLayout.LayoutParams sumLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sumLp.topMargin = dp(6);
        root.addView(summary, sumLp);

        HorizontalScrollView actionScroll = new HorizontalScrollView(requireContext());
        actionScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout actionRow = new LinearLayout(requireContext());
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionScroll.addView(actionRow);
        actionRow.addView(chip("连线", v -> { currentMindMapView.startConnectionMode(currentNode); dismiss(); }));
        actionRow.addView(chip("智能补强", v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).getMindMapView().selectOnlyNode(currentNode.getId());
                ((MainActivity) getActivity()).runScientificEnhancement();
            }
            dismiss();
        }));
        actionRow.addView(chip("AI 助手", v -> {
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).openAiScienceCoach("recommend", currentNode);
            dismiss();
        }));
        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionLp.topMargin = dp(14);
        root.addView(actionScroll, actionLp);

        EditText titleInput = input("标题", currentNode.getTitle(), InputType.TYPE_CLASS_TEXT, 1);
        EditText contentInput = input("内容", currentNode.getContent(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 4);

        String[] typeLabels = new String[Node.NodeType.values().length];
        for (int i = 0; i < typeLabels.length; i++) typeLabels[i] = Node.NodeType.values()[i].label;
        Spinner typeSpinner = spinner(typeLabels, currentNode.getType().ordinal());

        String[] shapeLabels = new String[Node.NodeShape.values().length];
        for (int i = 0; i < shapeLabels.length; i++) shapeLabels[i] = Node.NodeShape.values()[i].label;
        Spinner shapeSpinner = spinner(shapeLabels, currentNode.getShape().ordinal());

        String[] statusLabels = new String[Node.NodeStatus.values().length];
        for (int i = 0; i < statusLabels.length; i++) statusLabels[i] = Node.NodeStatus.values()[i].label;
        Spinner statusSpinner = spinner(statusLabels, currentNode.getStatus().ordinal());

        root.addView(card(section("基础信息"), titleInput, contentInput, typeSpinner, shapeSpinner, statusSpinner), actionLp);

        EditText tagsInput = input("标签，多个用逗号分隔", currentNode.getTagsAsString(), InputType.TYPE_CLASS_TEXT, 1);
        EditText priorityInput = input("优先级 1-5", String.valueOf(currentNode.getPriority()), InputType.TYPE_CLASS_NUMBER, 1);
        EditText dueAtInput = input("截止时间", currentNode.getDueAt(), InputType.TYPE_CLASS_TEXT, 1);
        EditText reviewAtInput = input("复习时间", currentNode.getReviewAt(), InputType.TYPE_CLASS_TEXT, 1);
        root.addView(card(section("工作流字段"), tagsInput, priorityInput, dueAtInput, reviewAtInput), actionLp);

        EditText sourceInput = input("来源 / 出处", currentNode.getNoteSource(), InputType.TYPE_CLASS_TEXT, 1);
        EditText metaInput = input("扩展元数据", currentNode.getMetaJson(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 3);
        root.addView(card(section("证据与扩展"), sourceInput, metaInput), actionLp);

        LinearLayout footer = new LinearLayout(requireContext());
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerLp.topMargin = dp(18);
        root.addView(footer, footerLp);

        TextView delete = chip("删除", v -> {
            currentMindMapView.removeNode(currentNode.getId());
            if (getActivity() instanceof NodeEditListener) ((NodeEditListener) getActivity()).onNodeDeleted(currentNode);
            dismiss();
        });
        TextView cancel = chip("取消", v -> dismiss());
        TextView save = chip("保存", v -> {
            currentNode.setTitle(titleInput.getText().toString().trim());
            currentNode.setContent(contentInput.getText().toString().trim());
            currentNode.setType(Node.NodeType.values()[typeSpinner.getSelectedItemPosition()]);
            currentNode.setShape(Node.NodeShape.values()[shapeSpinner.getSelectedItemPosition()]);
            currentNode.setStatus(Node.NodeStatus.values()[statusSpinner.getSelectedItemPosition()]);
            currentNode.setTagsFromString(tagsInput.getText().toString().trim());
            try { currentNode.setPriority(Integer.parseInt(priorityInput.getText().toString().trim())); } catch (Exception ignored) {}
            currentNode.setDueAt(dueAtInput.getText().toString().trim());
            currentNode.setReviewAt(reviewAtInput.getText().toString().trim());
            currentNode.setNoteSource(sourceInput.getText().toString().trim());
            currentNode.setMetaJson(metaInput.getText().toString().trim());
            WorkflowEngine.normalizeNodeForWorkflow(currentNode);
            if (getActivity() instanceof NodeEditListener) ((NodeEditListener) getActivity()).onNodeUpdated(currentNode);
            dismiss();
        });
        footer.addView(delete);
        footer.addView(cancel);
        footer.addView(save);

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(scrollView).create();
        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                bg.setColor(ThemeManager.getDialogBg());
                bg.setCornerRadius(dp(26));
                bg.setStroke(dp(1), ThemeManager.getStroke());
                dialog.getWindow().setBackgroundDrawable(bg);
            }
        });
        return dialog;
    }
}
