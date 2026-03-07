package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;

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
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                requireContext().getResources().getDisplayMetrics()
        );
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getActivity() == null || currentNode == null || currentMindMapView == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(20);
        root.setPadding(p, p, p, p);
        scrollView.addView(root);

        EditText titleInput = new EditText(requireContext());
        titleInput.setHint("标题");
        titleInput.setText(currentNode.getTitle());
        titleInput.setTextColor(Color.parseColor("#0F172A"));
        titleInput.setHintTextColor(Color.parseColor("#94A3B8"));
        titleInput.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#60A5FA")));
        root.addView(titleInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        EditText contentInput = new EditText(requireContext());
        contentInput.setHint("内容");
        contentInput.setText(currentNode.getContent());
        contentInput.setMinLines(5);
        contentInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        contentInput.setTextColor(Color.parseColor("#0F172A"));
        contentInput.setHintTextColor(Color.parseColor("#94A3B8"));
        contentInput.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#60A5FA")));

        LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        contentLp.topMargin = dp(14);
        root.addView(contentInput, contentLp);

        Spinner typeSpinner = new Spinner(requireContext());
        String[] typeLabels = new String[Node.NodeType.values().length];
        for (int i = 0; i < Node.NodeType.values().length; i++) {
            typeLabels[i] = Node.NodeType.values()[i].label;
        }
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                typeLabels
        );
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);
        typeSpinner.setSelection(currentNode.getType().ordinal());

        LinearLayout.LayoutParams typeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        typeLp.topMargin = dp(14);
        root.addView(typeSpinner, typeLp);

        Spinner shapeSpinner = new Spinner(requireContext());
        String[] shapeLabels = new String[Node.NodeShape.values().length];
        for (int i = 0; i < Node.NodeShape.values().length; i++) {
            shapeLabels[i] = Node.NodeShape.values()[i].label;
        }
        ArrayAdapter<String> shapeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                shapeLabels
        );
        shapeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        shapeSpinner.setAdapter(shapeAdapter);
        shapeSpinner.setSelection(currentNode.getShape().ordinal());

        LinearLayout.LayoutParams shapeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        shapeLp.topMargin = dp(14);
        root.addView(shapeSpinner, shapeLp);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("编辑节点")
                .setView(scrollView)
                .setNeutralButton("创建连线", (d, which) -> currentMindMapView.startConnectionMode(currentNode))
                .setNegativeButton("删除", (d, which) -> {
                    currentMindMapView.removeNode(currentNode.getId());
                    if (getActivity() instanceof NodeEditListener) {
                        ((NodeEditListener) getActivity()).onNodeDeleted(currentNode);
                    }
                })
                .setPositiveButton("保存", (d, which) -> {
                    currentNode.setTitle(titleInput.getText().toString().trim());
                    currentNode.setContent(contentInput.getText().toString().trim());
                    currentNode.setType(Node.NodeType.values()[typeSpinner.getSelectedItemPosition()]);
                    currentNode.setShape(Node.NodeShape.values()[shapeSpinner.getSelectedItemPosition()]);

                    if (getActivity() instanceof NodeEditListener) {
                        ((NodeEditListener) getActivity()).onNodeUpdated(currentNode);
                    }
                })
                .create();

        return dialog;
    }
}
