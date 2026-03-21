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
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

    private String safe(String v) { return v == null ? "" : v.trim(); }
    private float parseFloatSafe(String text, float def) {
        try { return (text == null || text.trim().isEmpty()) ? def : Float.parseFloat(text.trim()); }
        catch (Exception e) { return def; }
    }
    private int parseIntSafe(String text, int def) {
        try { return (text == null || text.trim().isEmpty()) ? def : Integer.parseInt(text.trim()); }
        catch (Exception e) { return def; }
    }

    private Spinner buildSpinner(String[] items, int selectedIndex) {
        Spinner spinner = new Spinner(requireContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int pos, View cv, ViewGroup parent) {
                TextView tv = (TextView) super.getView(pos, cv, parent);
                tv.setTextColor(ThemeManager.getTextPrimary());
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
                tv.setPadding(DialogUi.dp(NodeEditDialog.this, 12), DialogUi.dp(NodeEditDialog.this, 10), DialogUi.dp(NodeEditDialog.this, 12), DialogUi.dp(NodeEditDialog.this, 10));
                return tv;
            }

            @Override
            public View getDropDownView(int pos, View cv, ViewGroup parent) {
                TextView tv = new TextView(requireContext());
                tv.setText(getItem(pos));
                tv.setTextColor(ThemeManager.getTextPrimary());
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
                tv.setPadding(DialogUi.dp(NodeEditDialog.this, 16), DialogUi.dp(NodeEditDialog.this, 14), DialogUi.dp(NodeEditDialog.this, 16), DialogUi.dp(NodeEditDialog.this, 14));
                tv.setBackgroundColor(ThemeManager.getSpinnerBg());
                return tv;
            }
        };
        spinner.setAdapter(adapter);
        spinner.setSelection(selectedIndex);
        spinner.setBackground(DialogUi.createFieldBackground(this));
        spinner.setPadding(DialogUi.dp(this, 4), DialogUi.dp(this, 4), DialogUi.dp(this, 4), DialogUi.dp(this, 4));
        return spinner;
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
            list.add(new ProjectOption(node.getId(), title));
        }
        return list;
    }

    private int findProjectSelectionIndex(List<ProjectOption> opts, String target) {
        String t = safe(target);
        for (int i = 0; i < opts.size(); i++) if (safe(opts.get(i).id).equals(t)) return i;
        return 0;
    }

    private TextView buildActionChip(String text, boolean accent) {
        return DialogUi.createChip(this, text, accent);
    }

    private void showActionOverflow(MainActivity activity, Node node) {
        String[] moreActions = {
                "执行模式", "学习模式", "决策模式",
                "快捷动作", "智能补强", "WOOP", "If-Then", "周复盘",
                "检索练习", "Premortem", "决策实验室", "记忆复习", "Focus",
                "WRAP", "Bayes", "DSRP", "参考类预测", "全量推进",
                "AI缺口", "AI红队", "AI执行", "AI学习", "AI决策",
                "执行回填", "决策落地", "AI建议", "AI自动流", "方法体检"
        };
        new AlertDialog.Builder(requireContext())
                .setTitle("更多科学动作")
                .setItems(moreActions, (dialog, which) -> handleActionChip(moreActions[which], activity, node))
                .setNegativeButton("取消", null)
                .show();
    }

    private View buildActionBar(MainActivity activity, Node node) {
        LinearLayout wrap = new LinearLayout(requireContext());
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(DialogUi.createSectionTitle(this, "快捷科学动作"));
        DialogUi.addWithTopMargin(this, wrap, DialogUi.createHelper(this, "常用动作直接点，更多动作收进菜单，避免编辑页又挤又乱。"), 8);

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, DialogUi.dp(this, 10), 0, 0);

        String[] quick = {"推荐", "一键修复", "方法体检", "连线", "AI建议", "更多"};
        for (int i = 0; i < quick.length; i++) {
            final String label = quick[i];
            TextView chip = buildActionChip(label, i == 0 || "AI建议".equals(label));
            chip.setOnClickListener(v -> {
                if ("更多".equals(label)) showActionOverflow(activity, node);
                else handleActionChip(label, activity, node);
            });
            row.addView(chip);
        }
        HorizontalScrollView hsv = DialogUi.wrapChipRow(this, row);
        wrap.addView(hsv);
        return wrap;
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
            case "方法体检": WorkflowHealthDialog.show(activity, node); break;
            case "执行模式": WorkflowModeDialog.show(activity, node, "execution"); break;
            case "决策模式": WorkflowModeDialog.show(activity, node, "decision"); break;
            case "学习模式": WorkflowModeDialog.show(activity, node, "learning"); break;
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

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getActivity() == null || currentNode == null || currentMindMapView == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        MainActivity activity = (MainActivity) getActivity();

        LinearLayout shell = DialogUi.createRoot(this);
        LinearLayout content = DialogUi.createContentColumn(this);
        ScrollView scrollView = DialogUi.createScroll(this, content);
        shell.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout hero = DialogUi.createHero(
                this,
                currentNode.getType().label,
                safe(currentNode.getTitle()).isEmpty() ? "编辑节点" : safe(currentNode.getTitle()),
                "把基础信息、执行字段、追踪字段和证据字段一次性改全，避免很多设置被隐藏。"
        );
        content.addView(hero);

        LinearLayout actionCard = DialogUi.createCard(this);
        DialogUi.addWithTopMargin(this, content, actionCard, 12);
        actionCard.addView(buildActionBar(activity, currentNode));

        LinearLayout baseCard = DialogUi.createCard(this);
        DialogUi.addWithTopMargin(this, content, baseCard, 12);
        baseCard.addView(DialogUi.createSectionTitle(this, "基础信息"));
        DialogUi.addWithTopMargin(this, baseCard, DialogUi.createHelper(this, "标题、正文、节点类型、形状和当前状态都会在这里显示完整。"), 8);
        android.widget.EditText titleInput = DialogUi.createInput(this, "节点标题", currentNode.getTitle(), InputType.TYPE_CLASS_TEXT, 1);
        DialogUi.addWithTopMargin(this, baseCard, titleInput, 12);
        android.widget.EditText contentInput = DialogUi.createInput(this, "节点内容", currentNode.getContent(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 5);
        DialogUi.addWithTopMargin(this, baseCard, contentInput, 12);

        String[] typeLabels = new String[Node.NodeType.values().length];
        for (int i = 0; i < Node.NodeType.values().length; i++) typeLabels[i] = Node.NodeType.values()[i].label;
        Spinner typeSpinner = buildSpinner(typeLabels, currentNode.getType().ordinal());
        DialogUi.addWithTopMargin(this, baseCard, typeSpinner, 12);

        String[] shapeLabels = new String[Node.NodeShape.values().length];
        for (int i = 0; i < Node.NodeShape.values().length; i++) shapeLabels[i] = Node.NodeShape.values()[i].label;
        Spinner shapeSpinner = buildSpinner(shapeLabels, currentNode.getShape().ordinal());
        DialogUi.addWithTopMargin(this, baseCard, shapeSpinner, 12);

        String[] statusLabels = new String[Node.NodeStatus.values().length];
        for (int i = 0; i < Node.NodeStatus.values().length; i++) statusLabels[i] = Node.NodeStatus.values()[i].label;
        Spinner statusSpinner = buildSpinner(statusLabels, currentNode.getStatus().ordinal());
        DialogUi.addWithTopMargin(this, baseCard, statusSpinner, 12);

        LinearLayout workflowCard = DialogUi.createCard(this);
        DialogUi.addWithTopMargin(this, content, workflowCard, 12);
        workflowCard.addView(DialogUi.createSectionTitle(this, "执行与工作流"));
        DialogUi.addWithTopMargin(this, workflowCard, DialogUi.createHelper(this, "这里集中展示标签、优先级、截止、回顾、耗时、置信度和触发条件。"), 8);
        android.widget.EditText tagsInput = DialogUi.createInput(this, "标签，多个用逗号分隔", currentNode.getTagsAsString(), InputType.TYPE_CLASS_TEXT, 1);
        DialogUi.addWithTopMargin(this, workflowCard, tagsInput, 12);
        android.widget.EditText priorityInput = DialogUi.createInput(this, "优先级（1-5）", String.valueOf(currentNode.getPriority()), InputType.TYPE_CLASS_NUMBER, 1);
        DialogUi.addWithTopMargin(this, workflowCard, priorityInput, 12);
        android.widget.EditText dueAtInput = DialogUi.createInput(this, "截止时间，如：2026-03-20 20:00", currentNode.getDueAt(), InputType.TYPE_CLASS_TEXT, 1);
        DialogUi.addWithTopMargin(this, workflowCard, dueAtInput, 12);
        android.widget.EditText reviewAtInput = DialogUi.createInput(this, "复盘 / 复习时间，如：2026-03-18", currentNode.getReviewAt(), InputType.TYPE_CLASS_TEXT, 1);
        DialogUi.addWithTopMargin(this, workflowCard, reviewAtInput, 12);
        android.widget.EditText effortEstimateInput = DialogUi.createInput(this, "预计耗时（小时）", String.valueOf(currentNode.getEffortEstimate()), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, 1);
        DialogUi.addWithTopMargin(this, workflowCard, effortEstimateInput, 12);
        android.widget.EditText actualEffortInput = DialogUi.createInput(this, "实际耗时（小时）", String.valueOf(currentNode.getActualEffort()), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, 1);
        DialogUi.addWithTopMargin(this, workflowCard, actualEffortInput, 12);
        android.widget.EditText confidenceInput = DialogUi.createInput(this, "置信度（0~1）", String.valueOf(currentNode.getConfidence()), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, 1);
        DialogUi.addWithTopMargin(this, workflowCard, confidenceInput, 12);
        android.widget.EditText triggerInput = DialogUi.createInput(this, "触发条件 If，例如：如果晚上 7 点坐到书桌前", currentNode.getTriggerCondition(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 3);
        DialogUi.addWithTopMargin(this, workflowCard, triggerInput, 12);

        LinearLayout trackingCard = DialogUi.createCard(this);
        DialogUi.addWithTopMargin(this, content, trackingCard, 12);
        trackingCard.addView(DialogUi.createSectionTitle(this, "归属与追踪"));
        DialogUi.addWithTopMargin(this, trackingCard, DialogUi.createHelper(this, "项目归属、领域、KR 目标值和当前值都放在这里。"), 8);
        List<ProjectOption> projectOptions = buildProjectOptions();
        String[] projectLabels = new String[projectOptions.size()];
        for (int i = 0; i < projectOptions.size(); i++) projectLabels[i] = projectOptions.get(i).toString();
        Spinner projectSpinner = buildSpinner(projectLabels, findProjectSelectionIndex(projectOptions, currentNode.getProjectId()));
        DialogUi.addWithTopMargin(this, trackingCard, projectSpinner, 12);
        android.widget.EditText projectIdInput = DialogUi.createInput(this, "所属项目 ID（高级备用项）", currentNode.getProjectId(), InputType.TYPE_CLASS_TEXT, 1);
        DialogUi.addWithTopMargin(this, trackingCard, projectIdInput, 12);
        projectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { projectIdInput.setText(projectOptions.get(pos).id); }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });
        android.widget.EditText areaIdInput = DialogUi.createInput(this, "所属领域 ID", currentNode.getAreaId(), InputType.TYPE_CLASS_TEXT, 1);
        DialogUi.addWithTopMargin(this, trackingCard, areaIdInput, 12);
        android.widget.EditText krTargetInput = DialogUi.createInput(this, "KR 目标值", currentNode.getKrTarget() == 0f ? "" : String.valueOf(currentNode.getKrTarget()), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, 1);
        DialogUi.addWithTopMargin(this, trackingCard, krTargetInput, 12);
        android.widget.EditText krCurrentInput = DialogUi.createInput(this, "KR 当前值", currentNode.getKrCurrent() == 0f ? "" : String.valueOf(currentNode.getKrCurrent()), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, 1);
        DialogUi.addWithTopMargin(this, trackingCard, krCurrentInput, 12);

        LinearLayout evidenceCard = DialogUi.createCard(this);
        DialogUi.addWithTopMargin(this, content, evidenceCard, 12);
        evidenceCard.addView(DialogUi.createSectionTitle(this, "证据 / 来源 / 扩展元数据"));
        DialogUi.addWithTopMargin(this, evidenceCard, DialogUi.createHelper(this, "证据强度、来源和 AI/模板扩展字段全部完整展示。"), 8);
        android.widget.EditText evidenceStrengthInput = DialogUi.createInput(this, "证据强度（0~1）", String.valueOf(currentNode.getEvidenceStrength()), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, 1);
        DialogUi.addWithTopMargin(this, evidenceCard, evidenceStrengthInput, 12);
        android.widget.EditText noteSourceInput = DialogUi.createInput(this, "来源 / 出处", currentNode.getNoteSource(), InputType.TYPE_CLASS_TEXT, 1);
        DialogUi.addWithTopMargin(this, evidenceCard, noteSourceInput, 12);
        android.widget.EditText metaJsonInput = DialogUi.createInput(this, "扩展元数据（给 AI / 模板 / 决策记录使用）", currentNode.getMetaJson(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 4);
        DialogUi.addWithTopMargin(this, evidenceCard, metaJsonInput, 12);

        LinearLayout footer = new LinearLayout(requireContext());
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(0, DialogUi.dp(this, 12), 0, DialogUi.dp(this, 2));

        TextView btnDelete = DialogUi.createFooterButton(this, "删除", ThemeManager.getDanger(), ThemeManager.getDangerSoft(), false);
        TextView btnConnect = DialogUi.createFooterButton(this, "连线", ThemeManager.getAccent2(), ThemeManager.getAccentSoft(), false);
        TextView btnCancel = DialogUi.createFooterButton(this, "取消", ThemeManager.getTextSecondary(), ThemeManager.getSurface(), false);
        TextView btnSave = DialogUi.createFooterButton(this, "保存", ThemeManager.getTextPrimary(), ThemeManager.getAccentSoft(), true);

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btnLp.rightMargin = DialogUi.dp(this, 8);
        footer.addView(btnDelete, btnLp);

        LinearLayout.LayoutParams btnLp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btnLp2.rightMargin = DialogUi.dp(this, 8);
        footer.addView(btnConnect, btnLp2);

        LinearLayout.LayoutParams btnLp3 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btnLp3.rightMargin = DialogUi.dp(this, 8);
        footer.addView(btnCancel, btnLp3);
        footer.addView(btnSave, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        shell.addView(footer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(shell)
                .create();

        btnDelete.setOnClickListener(v -> {
            currentMindMapView.removeNode(currentNode.getId());
            if (getActivity() instanceof NodeEditListener) ((NodeEditListener) getActivity()).onNodeDeleted(currentNode);
            dialog.dismiss();
            dismiss();
        });

        btnConnect.setOnClickListener(v -> {
            currentMindMapView.startConnectionMode(currentNode);
            dialog.dismiss();
            dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
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
            if (projectSpinner.getSelectedItemPosition() > 0) {
                selectedProjectId = projectOptions.get(projectSpinner.getSelectedItemPosition()).id;
            }
            currentNode.setProjectId(selectedProjectId);
            currentNode.setAreaId(areaIdInput.getText().toString().trim());
            currentNode.setKrTarget(parseFloatSafe(krTargetInput.getText().toString(), 0f));
            currentNode.setKrCurrent(parseFloatSafe(krCurrentInput.getText().toString(), 0f));
            currentNode.setEvidenceStrength(parseFloatSafe(evidenceStrengthInput.getText().toString(), 0.5f));
            currentNode.setNoteSource(noteSourceInput.getText().toString().trim());
            currentNode.setMetaJson(metaJsonInput.getText().toString().trim());
            WorkflowEngine.normalizeNodeForWorkflow(currentNode);
            if (getActivity() instanceof NodeEditListener) ((NodeEditListener) getActivity()).onNodeUpdated(currentNode);
            dialog.dismiss();
            dismiss();
        });

        dialog.setOnShowListener(d -> DialogUi.styleWindow(this, dialog));
        return dialog;
    }
}
