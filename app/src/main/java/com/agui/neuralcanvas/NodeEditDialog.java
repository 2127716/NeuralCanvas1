package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private float parseFloatSafe(String text, float def) {
        try {
            return (text == null || text.trim().isEmpty()) ? def : Float.parseFloat(text.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private int parseIntSafe(String text, int def) {
        try {
            return (text == null || text.trim().isEmpty()) ? def : Integer.parseInt(text.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private View buildSectionCard(String title, String subtitle) {
        LinearLayout wrap = new LinearLayout(requireContext());
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(16), dp(16), dp(16), dp(16));

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(ThemeManager.getSurface());
        bg.setCornerRadius(dp(22));
        bg.setStroke(dp(1), ThemeManager.getStroke());
        wrap.setBackground(bg);

        TextView titleView = new TextView(requireContext());
        titleView.setText(title);
        titleView.setTextColor(ThemeManager.getSectionTitleColor());
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);
        wrap.addView(titleView);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView sub = new TextView(requireContext());
            sub.setText(subtitle);
            sub.setTextColor(ThemeManager.getTextSecondary());
            sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.topMargin = dp(4);
            wrap.addView(sub, lp);
        }
        return wrap;
    }

    private void addCard(LinearLayout root, View card, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(topDp);
        root.addView(card, lp);
    }

    private void addField(ViewGroup parent, View field, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(topDp);
        parent.addView(field, lp);
    }

    private TextView buildLabel(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(ThemeManager.getTextPrimary());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        return tv;
    }

    private EditText buildEditText(String hint, String value, int inputType) {
        EditText et = new EditText(requireContext());
        et.setHint(hint);
        et.setText(value == null ? "" : value);
        et.setInputType(inputType);
        et.setTextColor(ThemeManager.getTextPrimary());
        et.setHintTextColor(ThemeManager.getTextSecondary());
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(ThemeManager.getEditTextBg());
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), ThemeManager.getEditTextStroke());
        et.setBackground(bg);
        et.setBackgroundTintList(null);
        return et;
    }

    private Spinner buildSpinner(String[] items, int selectedIndex) {
        Spinner spinner = new Spinner(requireContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int pos, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(pos, convertView, parent);
                tv.setTextColor(ThemeManager.getTextPrimary());
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
                tv.setPadding(dp(12), dp(10), dp(12), dp(10));
                return tv;
            }

            @Override
            public View getDropDownView(int pos, View convertView, ViewGroup parent) {
                TextView tv = new TextView(requireContext());
                tv.setText(getItem(pos));
                tv.setTextColor(ThemeManager.getTextPrimary());
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
                tv.setPadding(dp(16), dp(14), dp(16), dp(14));
                tv.setBackgroundColor(ThemeManager.getSpinnerBg());
                return tv;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(Math.max(0, selectedIndex));

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(ThemeManager.getSpinnerBg());
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), ThemeManager.getEditTextStroke());
        spinner.setBackground(bg);
        spinner.setPadding(dp(4), dp(4), dp(4), dp(4));
        return spinner;
    }

    private TextView buildChip(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(ThemeManager.getTextPrimary());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        tv.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(ThemeManager.getChipBg());
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), ThemeManager.getChipStroke());
        tv.setBackground(bg);
        return tv;
    }

    private View buildActionBar(MainActivity activity, Node node) {
        LinearLayout card = (LinearLayout) buildSectionCard("科学动作", "把你整理的科学方法直接映射到节点工作流上");
        HorizontalScrollView hsv = new HorizontalScrollView(requireContext());
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        hsv.addView(row);

        String[] actions = new String[]{
                "推荐", "一键修复", "体检", "执行模式", "学习模式", "决策模式",
                "快捷动作", "智能补强", "WOOP", "If-Then", "周复盘", "检索练习",
                "Premortem", "决策实验室", "记忆复习", "Focus", "WRAP", "Bayes",
                "DSRP", "参考类预测", "全量推进", "AI缺口", "AI红队", "AI执行",
                "AI学习", "连线", "AI决策", "执行回填", "决策落地", "AI建议", "AI自动流"
        };

        for (String label : actions) {
            TextView chip = buildChip(label);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.rightMargin = dp(8);
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> handleActionChip(label, activity, node));
            row.addView(chip);
        }
        addField(card, hsv, 12);
        return card;
    }

    private void handleActionChip(String label, MainActivity activity, Node node) {
        dismiss();
        switch (label) {
            case "推荐": WorkflowRecommendationDialog.show(activity, node); break;
            case "一键修复": {
                WorkflowQuickFixEngine.FixResult fixResult = WorkflowQuickFixEngine.quickFixNode(activity, node);
                android.widget.Toast.makeText(activity, fixResult.buildSummary(), android.widget.Toast.LENGTH_LONG).show();
                break;
            }
            case "体检": WorkflowHealthDialog.show(activity, node); break;
            case "执行模式": WorkflowModeDialog.show(activity, node, "execution"); break;
            case "学习模式": WorkflowModeDialog.show(activity, node, "learning"); break;
            case "决策模式": WorkflowModeDialog.show(activity, node, "decision"); break;
            case "快捷动作": activity.showQuickActionsForNode(node); break;
            case "智能补强": activity.getMindMapView().selectOnlyNode(node.getId()); activity.runScientificEnhancement(); break;
            case "WOOP": activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.WOOP); break;
            case "If-Then": activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.IF_THEN); break;
            case "周复盘": activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.WEEKLY_REVIEW); break;
            case "检索练习": activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.RETRIEVAL_PRACTICE); break;
            case "Premortem": activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.PREMORTEM); break;
            case "决策实验室": activity.openDecisionLab(node); break;
            case "记忆复习": activity.openMemoryReview(); break;
            case "Focus": activity.openFocusSession(node); break;
            case "WRAP": activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.WRAP); break;
            case "Bayes": activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.BAYES_UPDATE); break;
            case "DSRP": activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.DSRP_ANALYSIS); break;
            case "参考类预测": activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.REFERENCE_CLASS_FORECAST); break;
            case "全量推进": activity.getMindMapView().selectOnlyNode(node.getId()); activity.runScientificAutopilot(); break;
            case "AI缺口": activity.openAiScienceCoach("gap", node); break;
            case "AI红队": activity.openAiScienceCoach("redteam", node); break;
            case "AI执行": activity.openAiScienceCoach("execution", node); break;
            case "AI学习": activity.openAiScienceCoach("learning", node); break;
            case "连线": currentMindMapView.startConnectionMode(node); break;
            case "AI决策": activity.openAiScienceCoach("decision", node); break;
            case "执行回填": activity.getMindMapView().selectOnlyNode(node.getId()); activity.openExecutionLog(); break;
            case "决策落地": activity.getMindMapView().selectOnlyNode(node.getId()); activity.openDecisionFollowThrough(); break;
            case "AI建议": activity.openAiScienceCoach("recommend", node); break;
            case "AI自动流": activity.openAiScienceCoach("autopilot", node); break;
        }
    }

    private static class ProjectOption {
        String id, label;
        ProjectOption(String id, String label) { this.id = id; this.label = label; }
        @NonNull @Override public String toString() { return label; }
    }

    private List<ProjectOption> buildProjectOptions() {
        List<ProjectOption> list = new ArrayList<>();
        list.add(new ProjectOption("", "无所属项目"));
        if (currentMindMapView == null) return list;
        Map<String, Node> allNodes = currentMindMapView.getNodesInternal();
        if (allNodes == null) return list;
        for (Node node : allNodes.values()) {
            if (node == null || node.getType() != Node.NodeType.PROJECT) continue;
            String title = safe(node.getTitle());
            if (title.isEmpty()) title = "未命名项目";
            list.add(new ProjectOption(node.getId(), title + "  (" + node.getId() + ")"));
        }
        return list;
    }

    private int findProjectSelectionIndex(List<ProjectOption> opts, String target) {
        String t = safe(target);
        for (int i = 0; i < opts.size(); i++) if (safe(opts.get(i).id).equals(t)) return i;
        return 0;
    }

    private TextView buildFooterBtn(String text, int color, boolean filled) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(filled ? ThemeManager.getBg() : color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setPadding(dp(16), dp(12), dp(16), dp(12));
        tv.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(18));
        if (filled) {
            bg.setColor(color);
        } else {
            bg.setColor(ThemeManager.getChipBg());
            bg.setStroke(dp(1), ThemeManager.getChipStroke());
        }
        tv.setBackground(bg);
        return tv;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getActivity() == null || currentNode == null || currentMindMapView == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(ThemeManager.getDialogBg());

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(14));
        scrollView.addView(root);

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) addCard(root, buildActionBar(activity, currentNode), 0);

        LinearLayout basicCard = (LinearLayout) buildSectionCard("基础信息", "标题、内容、类型、形状与状态");
        EditText titleInput = buildEditText("新节点", currentNode.getTitle(), InputType.TYPE_CLASS_TEXT);
        addField(basicCard, buildLabel("标题"), 12);
        addField(basicCard, titleInput, 8);

        EditText contentInput = buildEditText("输入内容", currentNode.getContent(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        contentInput.setMinLines(4);
        addField(basicCard, buildLabel("内容"), 14);
        addField(basicCard, contentInput, 8);

        String[] typeLabels = new String[Node.NodeType.values().length];
        for (int i = 0; i < Node.NodeType.values().length; i++) typeLabels[i] = Node.NodeType.values()[i].label;
        Spinner typeSpinner = buildSpinner(typeLabels, currentNode.getType().ordinal());
        addField(basicCard, buildLabel("类型"), 14);
        addField(basicCard, typeSpinner, 8);

        String[] shapeLabels = new String[Node.NodeShape.values().length];
        for (int i = 0; i < Node.NodeShape.values().length; i++) shapeLabels[i] = Node.NodeShape.values()[i].label;
        Spinner shapeSpinner = buildSpinner(shapeLabels, currentNode.getShape().ordinal());
        addField(basicCard, buildLabel("形状"), 14);
        addField(basicCard, shapeSpinner, 8);

        String[] statusLabels = new String[Node.NodeStatus.values().length];
        for (int i = 0; i < Node.NodeStatus.values().length; i++) statusLabels[i] = Node.NodeStatus.values()[i].label;
        Spinner statusSpinner = buildSpinner(statusLabels, currentNode.getStatus().ordinal());
        addField(basicCard, buildLabel("状态"), 14);
        addField(basicCard, statusSpinner, 8);
        addCard(root, basicCard, 14);

        LinearLayout workflowCard = (LinearLayout) buildSectionCard("执行与工作流", "WOOP / If-Then / 优先级 / 耗时 / 触发条件");
        EditText tagsInput = buildEditText("标签，多个用逗号分隔", currentNode.getTagsAsString(), InputType.TYPE_CLASS_TEXT);
        EditText priorityInput = buildEditText("优先级（1-5）", String.valueOf(currentNode.getPriority()), InputType.TYPE_CLASS_NUMBER);
        EditText dueAtInput = buildEditText("截止时间，如：2026-03-20 20:00", currentNode.getDueAt(), InputType.TYPE_CLASS_TEXT);
        EditText reviewAtInput = buildEditText("复习/回顾时间，如：2026-03-18", currentNode.getReviewAt(), InputType.TYPE_CLASS_TEXT);
        EditText effortEstimateInput = buildEditText("预计耗时（小时）", String.valueOf(currentNode.getEffortEstimate()), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText actualEffortInput = buildEditText("实际耗时（小时）", String.valueOf(currentNode.getActualEffort()), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText confidenceInput = buildEditText("置信度（0~1）", String.valueOf(currentNode.getConfidence()), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText triggerInput = buildEditText("触发条件 If，例如：如果晚上7点坐到书桌前", currentNode.getTriggerCondition(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        triggerInput.setMinLines(2);

        addField(workflowCard, buildLabel("标签"), 12); addField(workflowCard, tagsInput, 8);
        addField(workflowCard, buildLabel("优先级"), 14); addField(workflowCard, priorityInput, 8);
        addField(workflowCard, buildLabel("截止时间"), 14); addField(workflowCard, dueAtInput, 8);
        addField(workflowCard, buildLabel("复习时间"), 14); addField(workflowCard, reviewAtInput, 8);
        addField(workflowCard, buildLabel("预计耗时"), 14); addField(workflowCard, effortEstimateInput, 8);
        addField(workflowCard, buildLabel("实际耗时"), 14); addField(workflowCard, actualEffortInput, 8);
        addField(workflowCard, buildLabel("置信度"), 14); addField(workflowCard, confidenceInput, 8);
        addField(workflowCard, buildLabel("触发条件"), 14); addField(workflowCard, triggerInput, 8);
        addCard(root, workflowCard, 14);

        LinearLayout trackCard = (LinearLayout) buildSectionCard("归属与追踪", "项目 / 领域 / KR 目标与当前值");
        List<ProjectOption> projectOptions = buildProjectOptions();
        String[] projectLabels = new String[projectOptions.size()];
        for (int i = 0; i < projectOptions.size(); i++) projectLabels[i] = projectOptions.get(i).toString();
        Spinner projectSpinner = buildSpinner(projectLabels, findProjectSelectionIndex(projectOptions, currentNode.getProjectId()));
        EditText projectIdInput = buildEditText("所属项目ID（高级备用项）", currentNode.getProjectId(), InputType.TYPE_CLASS_TEXT);
        EditText areaIdInput = buildEditText("所属领域ID", currentNode.getAreaId(), InputType.TYPE_CLASS_TEXT);
        EditText krTargetInput = buildEditText("KR目标值", currentNode.getKrTarget() == 0f ? "" : String.valueOf(currentNode.getKrTarget()), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText krCurrentInput = buildEditText("KR当前值", currentNode.getKrCurrent() == 0f ? "" : String.valueOf(currentNode.getKrCurrent()), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        addField(trackCard, buildLabel("所属项目"), 12); addField(trackCard, projectSpinner, 8);
        addField(trackCard, buildLabel("项目ID"), 14); addField(trackCard, projectIdInput, 8);
        addField(trackCard, buildLabel("领域ID"), 14); addField(trackCard, areaIdInput, 8);
        addField(trackCard, buildLabel("KR目标值"), 14); addField(trackCard, krTargetInput, 8);
        addField(trackCard, buildLabel("KR当前值"), 14); addField(trackCard, krCurrentInput, 8);
        projectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                projectIdInput.setText(projectOptions.get(position).id);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        addCard(root, trackCard, 14);

        LinearLayout evidenceCard = (LinearLayout) buildSectionCard("证据 / 来源 / 扩展", "保留你的科学记录字段，不做阉割");
        EditText evidenceStrengthInput = buildEditText("证据强度（0~1）", String.valueOf(currentNode.getEvidenceStrength()), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText noteSourceInput = buildEditText("来源/出处", currentNode.getNoteSource(), InputType.TYPE_CLASS_TEXT);
        EditText metaJsonInput = buildEditText("扩展元数据（给 AI / 模板 / 导入用）", currentNode.getMetaJson(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        metaJsonInput.setMinLines(3);
        addField(evidenceCard, buildLabel("证据强度"), 12); addField(evidenceCard, evidenceStrengthInput, 8);
        addField(evidenceCard, buildLabel("来源"), 14); addField(evidenceCard, noteSourceInput, 8);
        addField(evidenceCard, buildLabel("扩展元数据"), 14); addField(evidenceCard, metaJsonInput, 8);
        addCard(root, evidenceCard, 14);

        LinearLayout footer = new LinearLayout(requireContext());
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setWeightSum(4f);

        TextView btnDelete = buildFooterBtn("删除", ThemeManager.getDanger(), false);
        TextView btnConnect = buildFooterBtn("连线", ThemeManager.getLinkColor(), false);
        TextView btnCancel = buildFooterBtn("取消", ThemeManager.getTextSecondary(), false);
        TextView btnSave = buildFooterBtn("保存", ThemeManager.getAccent(), true);

        View[] btns = new View[]{btnDelete, btnConnect, btnCancel, btnSave};
        for (View v : btns) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.leftMargin = dp(4);
            lp.rightMargin = dp(4);
            footer.addView(v, lp);
        }
        addCard(root, footer, 16);

        final AlertDialog[] dialogRef = {null};

        btnDelete.setOnClickListener(v -> {
            currentMindMapView.removeNode(currentNode.getId());
            if (getActivity() instanceof NodeEditListener) ((NodeEditListener) getActivity()).onNodeDeleted(currentNode);
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            dismiss();
        });

        btnConnect.setOnClickListener(v -> {
            currentMindMapView.startConnectionMode(currentNode);
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            dismiss();
        });

        btnSave.setOnClickListener(v -> {
            currentNode.setTitle(titleInput.getText().toString().trim());
            currentNode.setContent(contentInput.getText().toString().trim());
            currentNode.setType(Node.NodeType.values()[typeSpinner.getSelectedItemPosition()]);
            currentNode.setShape(Node.NodeShape.values()[shapeSpinner.getSelectedItemPosition()]);
            currentNode.setStatus(Node.NodeStatus.values()[statusSpinner.getSelectedItemPosition()]);
            currentNode.setTagsFromString(tagsInput.getText().toString().trim());
            currentNode.setPriority(parseIntSafe(priorityInput.getText().toString(), 3));
            currentNode.setDueAt(dueAtInput.getText().toString().trim());
            currentNode.setReviewAt(reviewAtInput.getText().toString().trim());
            currentNode.setEffortEstimate(parseFloatSafe(effortEstimateInput.getText().toString(), 0f));
            currentNode.setActualEffort(parseFloatSafe(actualEffortInput.getText().toString(), 0f));
            currentNode.setConfidence(parseFloatSafe(confidenceInput.getText().toString(), 0.5f));
            currentNode.setTriggerCondition(triggerInput.getText().toString().trim());
            String selectedProjectId = projectIdInput.getText().toString().trim();
            if (projectSpinner.getSelectedItemPosition() > 0) selectedProjectId = projectOptions.get(projectSpinner.getSelectedItemPosition()).id;
            currentNode.setProjectId(selectedProjectId);
            currentNode.setAreaId(areaIdInput.getText().toString().trim());
            currentNode.setKrTarget(parseFloatSafe(krTargetInput.getText().toString(), 0f));
            currentNode.setKrCurrent(parseFloatSafe(krCurrentInput.getText().toString(), 0f));
            currentNode.setEvidenceStrength(parseFloatSafe(evidenceStrengthInput.getText().toString(), 0.5f));
            currentNode.setNoteSource(noteSourceInput.getText().toString().trim());
            currentNode.setMetaJson(metaJsonInput.getText().toString().trim());
            WorkflowEngine.normalizeNodeForWorkflow(currentNode);
            if (getActivity() instanceof NodeEditListener) ((NodeEditListener) getActivity()).onNodeUpdated(currentNode);
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            dismiss();
        });

        AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Theme_AppCompat_Dialog))
                .setTitle("编辑节点")
                .setView(scrollView)
                .create();
        dialogRef[0] = dialog;

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                android.graphics.drawable.GradientDrawable winBg = new android.graphics.drawable.GradientDrawable();
                winBg.setColor(ThemeManager.getDialogBg());
                winBg.setCornerRadius(dp(30));
                winBg.setStroke(dp(1), ThemeManager.getStroke());
                dialog.getWindow().setBackgroundDrawable(winBg);
            }
        });

        return dialog;
    }
}
