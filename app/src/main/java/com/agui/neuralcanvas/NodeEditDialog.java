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
import android.widget.Spinner;
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
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                requireContext().getResources().getDisplayMetrics());
    }

    private String safe(String v) { return v == null ? "" : v.trim(); }
    private float parseFloatSafe(String text, float def) { try { return (text == null || text.trim().isEmpty()) ? def : Float.parseFloat(text.trim()); } catch (Exception e) { return def; } }
    private int parseIntSafe(String text, int def) { try { return (text == null || text.trim().isEmpty()) ? def : Integer.parseInt(text.trim()); } catch (Exception e) { return def; } }

    private View buildCard() {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(ThemeManager.getSurface());
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), ThemeManager.getStroke());
        card.setBackground(bg);
        return card;
    }

    private TextView buildSectionTitle(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(ThemeManager.getSectionTitleColor());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setLetterSpacing(0.04f);
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
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), ThemeManager.getEditTextStroke());
        et.setBackground(bg);
        et.setBackgroundTintList(null);
        return et;
    }

    private Spinner buildSpinner(String[] items, int selectedIndex) {
        Spinner spinner = new Spinner(requireContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int pos, View cv, ViewGroup parent) {
                TextView tv = (TextView) super.getView(pos, cv, parent);
                tv.setTextColor(ThemeManager.getTextPrimary());
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
                tv.setPadding(dp(12), dp(10), dp(12), dp(10));
                return tv;
            }

            @Override
            public View getDropDownView(int pos, View cv, ViewGroup parent) {
                TextView tv = new TextView(requireContext());
                tv.setText(getItem(pos));
                tv.setTextColor(ThemeManager.getTextPrimary());
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
                tv.setPadding(dp(16), dp(14), dp(16), dp(14));
                tv.setBackgroundColor(ThemeManager.getSpinnerBg());
                return tv;
            }
        };
        spinner.setAdapter(adapter);
        spinner.setSelection(selectedIndex);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(ThemeManager.getSpinnerBg());
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), ThemeManager.getEditTextStroke());
        spinner.setBackground(bg);
        spinner.setPadding(dp(4), dp(4), dp(4), dp(4));
        return spinner;
    }

    private void addWithTopMargin(LinearLayout root, View view, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(topDp);
        root.addView(view, lp);
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

    private TextView buildActionChip(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(ThemeManager.getTextPrimary());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(ThemeManager.getChipBg());
        bg.setStroke(dp(1), ThemeManager.getChipStroke());
        bg.setCornerRadius(dp(16));
        tv.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(8);
        tv.setLayoutParams(lp);
        return tv;
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
        wrap.addView(buildSectionTitle("科学动作"));

        HorizontalScrollView hsv = new HorizontalScrollView(requireContext());
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, 0);

        String[] quick = {"推荐", "一键修复", "方法体检", "连线", "AI建议", "更多"};
        for (String label : quick) {
            TextView chip = buildActionChip(label);
            chip.setOnClickListener(v -> {
                if ("更多".equals(label)) showActionOverflow(activity, node);
                else handleActionChip(label, activity, node);
            });
            row.addView(chip);
        }
        hsv.addView(row);
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

    private TextView buildFooterBtn(String text, int color, boolean bold) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        tv.setPadding(dp(12), dp(14), dp(12), dp(14));
        tv.setGravity(Gravity.CENTER);
        if (bold) tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        return tv;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getActivity() == null || currentNode == null || currentMindMapView == null) return super.onCreateDialog(savedInstanceState);

        MainActivity activity = (MainActivity) getActivity();

        LinearLayout shell = new LinearLayout(requireContext());
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(ThemeManager.getDialogBg());

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(ThemeManager.getDialogBg());

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(8));
        root.setBackgroundColor(ThemeManager.getDialogBg());
        scrollView.addView(root);

        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        shell.addView(scrollView, scrollLp);

        LinearLayout actionCard = (LinearLayout) buildCard();
        if (activity != null) actionCard.addView(buildActionBar(activity, currentNode));
        root.addView(actionCard);

        LinearLayout baseCard = (LinearLayout) buildCard();
        addWithTopMargin(root, baseCard, 12);
        baseCard.addView(buildSectionTitle("基础信息"));
        EditText titleInput = buildEditText("新节点", currentNode.getTitle(), InputType.TYPE_CLASS_TEXT);
        addWithTopMargin(baseCard, titleInput, 10);
        EditText contentInput = buildEditText("输入内容", currentNode.getContent(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        contentInput.setMinLines(4);
        addWithTopMargin(baseCard, contentInput, 12);
        String[] typeLabels = new String[Node.NodeType.values().length]; for (int i = 0; i < Node.NodeType.values().length; i++) typeLabels[i] = Node.NodeType.values()[i].label;
        Spinner typeSpinner = buildSpinner(typeLabels, currentNode.getType().ordinal()); addWithTopMargin(baseCard, typeSpinner, 12);
        String[] shapeLabels = new String[Node.NodeShape.values().length]; for (int i = 0; i < Node.NodeShape.values().length; i++) shapeLabels[i] = Node.NodeShape.values()[i].label;
        Spinner shapeSpinner = buildSpinner(shapeLabels, currentNode.getShape().ordinal()); addWithTopMargin(baseCard, shapeSpinner, 12);
        String[] statusLabels = new String[Node.NodeStatus.values().length]; for (int i = 0; i < Node.NodeStatus.values().length; i++) statusLabels[i] = Node.NodeStatus.values()[i].label;
        Spinner statusSpinner = buildSpinner(statusLabels, currentNode.getStatus().ordinal()); addWithTopMargin(baseCard, statusSpinner, 12);

        LinearLayout workflowCard = (LinearLayout) buildCard();
        addWithTopMargin(root, workflowCard, 12);
        workflowCard.addView(buildSectionTitle("工作流字段"));
        EditText tagsInput = buildEditText("标签，多个用逗号分隔", currentNode.getTagsAsString(), InputType.TYPE_CLASS_TEXT); addWithTopMargin(workflowCard, tagsInput, 10);
        EditText priorityInput = buildEditText("优先级（1-5）", String.valueOf(currentNode.getPriority()), InputType.TYPE_CLASS_NUMBER); addWithTopMargin(workflowCard, priorityInput, 12);
        EditText dueAtInput = buildEditText("截止时间，如：2026-03-20 20:00", currentNode.getDueAt(), InputType.TYPE_CLASS_TEXT); addWithTopMargin(workflowCard, dueAtInput, 12);
        EditText reviewAtInput = buildEditText("复习/回顾时间，如：2026-03-18", currentNode.getReviewAt(), InputType.TYPE_CLASS_TEXT); addWithTopMargin(workflowCard, reviewAtInput, 12);
        EditText effortEstimateInput = buildEditText("预计耗时（小时）", String.valueOf(currentNode.getEffortEstimate()), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); addWithTopMargin(workflowCard, effortEstimateInput, 12);
        EditText actualEffortInput = buildEditText("实际耗时（小时）", String.valueOf(currentNode.getActualEffort()), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); addWithTopMargin(workflowCard, actualEffortInput, 12);
        EditText confidenceInput = buildEditText("置信度（0~1）", String.valueOf(currentNode.getConfidence()), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); addWithTopMargin(workflowCard, confidenceInput, 12);
        EditText triggerInput = buildEditText("触发条件 If，例如：如果晚上7点坐到书桌前", currentNode.getTriggerCondition(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE); addWithTopMargin(workflowCard, triggerInput, 12);

        LinearLayout trackingCard = (LinearLayout) buildCard();
        addWithTopMargin(root, trackingCard, 12);
        trackingCard.addView(buildSectionTitle("归属与追踪"));
        List<ProjectOption> projectOptions = buildProjectOptions();
        String[] projectLabels = new String[projectOptions.size()]; for (int i = 0; i < projectOptions.size(); i++) projectLabels[i] = projectOptions.get(i).toString();
        Spinner projectSpinner = buildSpinner(projectLabels, findProjectSelectionIndex(projectOptions, currentNode.getProjectId())); addWithTopMargin(trackingCard, projectSpinner, 10);
        EditText projectIdInput = buildEditText("所属项目ID（高级备用项）", currentNode.getProjectId(), InputType.TYPE_CLASS_TEXT); addWithTopMargin(trackingCard, projectIdInput, 12);
        projectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { projectIdInput.setText(projectOptions.get(pos).id); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        EditText areaIdInput = buildEditText("所属领域ID", currentNode.getAreaId(), InputType.TYPE_CLASS_TEXT); addWithTopMargin(trackingCard, areaIdInput, 12);
        EditText krTargetInput = buildEditText("KR目标值", currentNode.getKrTarget() == 0f ? "" : String.valueOf(currentNode.getKrTarget()), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); addWithTopMargin(trackingCard, krTargetInput, 12);
        EditText krCurrentInput = buildEditText("KR当前值", currentNode.getKrCurrent() == 0f ? "" : String.valueOf(currentNode.getKrCurrent()), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); addWithTopMargin(trackingCard, krCurrentInput, 12);

        LinearLayout evidenceCard = (LinearLayout) buildCard();
        addWithTopMargin(root, evidenceCard, 12);
        evidenceCard.addView(buildSectionTitle("证据 / 来源 / 扩展"));
        EditText evidenceStrengthInput = buildEditText("证据强度（0~1）", String.valueOf(currentNode.getEvidenceStrength()), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); addWithTopMargin(evidenceCard, evidenceStrengthInput, 10);
        EditText noteSourceInput = buildEditText("来源/出处", currentNode.getNoteSource(), InputType.TYPE_CLASS_TEXT); addWithTopMargin(evidenceCard, noteSourceInput, 12);
        EditText metaJsonInput = buildEditText("扩展元数据（以后给 AI/模板用）", currentNode.getMetaJson(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE); metaJsonInput.setMinLines(3); addWithTopMargin(evidenceCard, metaJsonInput, 12);

        LinearLayout footer = new LinearLayout(requireContext());
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(dp(18), dp(6), dp(18), dp(10));
        footer.setBackgroundColor(ThemeManager.getDialogBg());
        final AlertDialog[] dialogRef = {null};

        TextView btnDelete = buildFooterBtn("删除", ThemeManager.getDanger(), false);
        btnDelete.setOnClickListener(v -> {
            currentMindMapView.removeNode(currentNode.getId());
            if (getActivity() instanceof NodeEditListener) ((NodeEditListener) getActivity()).onNodeDeleted(currentNode);
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            dismiss();
        });
        footer.addView(btnDelete);

        TextView btnConnect = buildFooterBtn("连线", ThemeManager.getLinkColor(), false);
        btnConnect.setOnClickListener(v -> {
            currentMindMapView.startConnectionMode(currentNode);
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            dismiss();
        });
        footer.addView(btnConnect);

        View spacer = new View(requireContext());
        footer.addView(spacer, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView btnCancel = buildFooterBtn("取消", ThemeManager.getTextSecondary(), false);
        btnCancel.setOnClickListener(v -> { if (dialogRef[0] != null) dialogRef[0].dismiss(); dismiss(); });
        footer.addView(btnCancel);

        TextView btnSave = buildFooterBtn("保存", ThemeManager.getAccent(), true);
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
        footer.addView(btnSave);

        shell.addView(footer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Theme_AppCompat_Dialog))
                .setTitle("编辑节点")
                .setView(shell)
                .create();
        dialogRef[0] = dialog;
        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                android.graphics.drawable.GradientDrawable winBg = new android.graphics.drawable.GradientDrawable();
                winBg.setColor(ThemeManager.getDialogBg());
                winBg.setCornerRadius(56f);
                winBg.setStroke(2, ThemeManager.getStroke());
                dialog.getWindow().setBackgroundDrawable(winBg);
            }
        });
        return dialog;
    }
}
