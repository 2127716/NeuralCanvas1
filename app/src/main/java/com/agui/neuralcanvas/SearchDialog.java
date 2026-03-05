package com.agui.neuralcanvas;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import java.util.ArrayList;
import java.util.List;

public class SearchDialog extends DialogFragment {
    
    public interface SearchListener {
        void onSearch(String keyword, List<Node.NodeType> types, boolean highlight);
        void onClearSearch();
    }
    
    private SearchListener listener;
    private MindMapView mindMapView;
    
    private EditText keywordEditText;
    private CheckBox goalCheckBox;
    private CheckBox taskCheckBox;
    private CheckBox ideaCheckBox;
    private CheckBox noteCheckBox;
    private CheckBox problemCheckBox;
    private CheckBox resourceCheckBox;
    private CheckBox personCheckBox;
    private CheckBox eventCheckBox;
    private CheckBox decisionCheckBox;
    private CheckBox riskCheckBox;
    private CheckBox highlightCheckBox;
    
    public static SearchDialog newInstance(MindMapView mindMapView) {
        SearchDialog dialog = new SearchDialog();
        dialog.mindMapView = mindMapView;
        return dialog;
    }
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (SearchListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement SearchListener");
        }
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_search, null);
        
        keywordEditText = view.findViewById(R.id.edit_keyword);
        goalCheckBox = view.findViewById(R.id.checkbox_goal);
        taskCheckBox = view.findViewById(R.id.checkbox_task);
        ideaCheckBox = view.findViewById(R.id.checkbox_idea);
        noteCheckBox = view.findViewById(R.id.checkbox_note);
        problemCheckBox = view.findViewById(R.id.checkbox_problem);
        resourceCheckBox = view.findViewById(R.id.checkbox_resource);
        personCheckBox = view.findViewById(R.id.checkbox_person);
        eventCheckBox = view.findViewById(R.id.checkbox_event);
        decisionCheckBox = view.findViewById(R.id.checkbox_decision);
        riskCheckBox = view.findViewById(R.id.checkbox_risk);
        highlightCheckBox = view.findViewById(R.id.checkbox_highlight);
        
// 默认选中所有类型
        goalCheckBox.setChecked(true);
        taskCheckBox.setChecked(true);
        ideaCheckBox.setChecked(true);
        noteCheckBox.setChecked(true);
        problemCheckBox.setChecked(true);
        resourceCheckBox.setChecked(true);
        personCheckBox.setChecked(true);
        eventCheckBox.setChecked(true);
        decisionCheckBox.setChecked(true);
        riskCheckBox.setChecked(true);
        highlightCheckBox.setChecked(true);
        
        builder.setView(view)
            .setTitle("搜索节点")
            .setPositiveButton("搜索", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    performSearch();
                }
            })
            .setNegativeButton("取消", null)
            .setNeutralButton("清除搜索", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    clearSearch();
                }
            });
        
        return builder.create();
    }
    
    private void performSearch() {
        String keyword = keywordEditText.getText().toString().trim();
        boolean highlight = highlightCheckBox.isChecked();
        
List<Node.NodeType> selectedTypes = new ArrayList<>();
        if (goalCheckBox.isChecked()) selectedTypes.add(Node.NodeType.GOAL);
        if (taskCheckBox.isChecked()) selectedTypes.add(Node.NodeType.TASK);
        if (ideaCheckBox.isChecked()) selectedTypes.add(Node.NodeType.IDEA);
        if (noteCheckBox.isChecked()) selectedTypes.add(Node.NodeType.NOTE);
        if (problemCheckBox.isChecked()) selectedTypes.add(Node.NodeType.PROBLEM);
        if (resourceCheckBox.isChecked()) selectedTypes.add(Node.NodeType.RESOURCE);
        if (personCheckBox.isChecked()) selectedTypes.add(Node.NodeType.PERSON);
        if (eventCheckBox.isChecked()) selectedTypes.add(Node.NodeType.EVENT);
        if (decisionCheckBox.isChecked()) selectedTypes.add(Node.NodeType.DECISION);
        if (riskCheckBox.isChecked()) selectedTypes.add(Node.NodeType.RISK);
        
        if (listener != null) {
            listener.onSearch(keyword, selectedTypes, highlight);
        }
        
        if (mindMapView != null) {
            mindMapView.invalidate();
        }
    }
    
    private void clearSearch() {
        if (listener != null) {
            listener.onClearSearch();
        }
        
        if (mindMapView != null) {
            mindMapView.invalidate();
        }
    }
}