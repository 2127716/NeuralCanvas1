package com.agui.neuralcanvas;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class NodeEditDialog extends DialogFragment {
    
    public interface NodeEditListener {
        void onNodeUpdated(Node node);
        void onNodeDeleted(Node node);
    }
    
    private Node node;
    private NodeEditListener listener;
    private MindMapView mindMapView;
    
    private EditText titleEditText;
    private EditText contentEditText;
    private Spinner typeSpinner;
    
    public static NodeEditDialog newInstance(Node node, MindMapView mindMapView) {
        NodeEditDialog dialog = new NodeEditDialog();
        dialog.node = node;
        dialog.mindMapView = mindMapView;
        return dialog;
    }
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (NodeEditListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement NodeEditListener");
        }
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_node_edit, null);
        
        titleEditText = view.findViewById(R.id.edit_title);
        contentEditText = view.findViewById(R.id.edit_content);
        typeSpinner = view.findViewById(R.id.spinner_type);
        
        // 设置当前值
        titleEditText.setText(node.getTitle());
        contentEditText.setText(node.getContent());
        
        // 设置节点类型下拉框
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            getContext(),
            android.R.layout.simple_spinner_item,
            new String[]{"目标", "任务", "想法", "笔记", "问题", "资源", "人物", "事件", "决策", "风险"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
        
        // 设置当前选中的类型
        typeSpinner.setSelection(node.getType().styleIndex);
        
        builder.setView(view)
            .setTitle("编辑节点")
            .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    updateNode();
                }
            })
            .setNegativeButton("取消", null)
            .setNeutralButton("删除", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteNode();
                }
            });
        
        return builder.create();
    }
    
    private void updateNode() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();
        int selectedType = typeSpinner.getSelectedItemPosition();
        
        if (title.isEmpty()) {
            title = "未命名节点";
        }
        
        node.setTitle(title);
        node.setContent(content);
        
node.setType(Node.NodeType.fromIndex(selectedType));
        
        if (listener != null) {
            listener.onNodeUpdated(node);
        }
        
        if (mindMapView != null) {
            mindMapView.invalidate();
        }
    }
    
    private void deleteNode() {
        if (listener != null) {
            listener.onNodeDeleted(node);
        }
        
        if (mindMapView != null) {
            mindMapView.removeNode(node.getId());
        }
    }
}