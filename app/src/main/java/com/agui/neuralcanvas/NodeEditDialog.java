package com.agui.neuralcanvas;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
            throw new ClassCastException(context + " must implement NodeEditListener");
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

        // 自定义按钮
        TextView btnDelete = view.findViewById(R.id.btn_delete);
        TextView btnLink = view.findViewById(R.id.btn_link);
        TextView btnCancel = view.findViewById(R.id.btn_cancel);
        TextView btnSave = view.findViewById(R.id.btn_save);

        // 设置当前值
        titleEditText.setText(node.getTitle());
        contentEditText.setText(node.getContent());

        // 类型下拉
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"目标", "任务", "想法", "笔记", "问题", "资源", "人物", "事件", "决策", "风险"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
        typeSpinner.setSelection(node.getType().styleIndex);

        // 绑定按钮逻辑
        btnSave.setOnClickListener(v -> {
            updateNode();
            dismiss();
        });

        btnCancel.setOnClickListener(v -> dismiss());

        btnDelete.setOnClickListener(v -> {
            deleteNode();
            dismiss();
        });

        btnLink.setOnClickListener(v -> {
            // 先保存当前编辑内容（可选，但体验更好）
            updateNode();

            // 进入“选择目标节点创建连接”模式
            if (mindMapView != null) {
                mindMapView.startConnectionMode(node);
                Toast.makeText(getContext(), "请选择要链接的另一个节点", Toast.LENGTH_SHORT).show();
            }
            dismiss();
        });

        builder.setTitle("编辑节点");
        builder.setView(view);
        return builder.create();
    }

    private void updateNode() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();
        int selectedType = typeSpinner.getSelectedItemPosition();

        if (title.isEmpty()) title = "未命名节点";

        node.setTitle(title);
        node.setContent(content);
        node.setType(Node.NodeType.fromIndex(selectedType));

        if (listener != null) listener.onNodeUpdated(node);
        if (mindMapView != null) mindMapView.invalidate();
    }

    private void deleteNode() {
        if (listener != null) listener.onNodeDeleted(node);
        if (mindMapView != null) mindMapView.removeNode(node.getId());
    }
}
