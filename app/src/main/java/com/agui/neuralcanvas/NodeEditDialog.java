package com.agui.neuralcanvas;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                requireContext().getResources().getDisplayMetrics()
        );
    }

    private TextView buildSectionTitle(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#EDE9FE"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        return tv;
    }

    private EditText buildEditText(String hint, String value, int inputType) {
        EditText et = new EditText(requireContext());
        et.setHint(hint);
        et.setText(value == null ? "" : value);
        et.setInputType(inputType);
        et.setTextColor(Color.parseColor("#F8FAFC"));
        et.setHintTextColor(Color.parseColor("#7C8AA8"));
        et.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#8B5CF6")));
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.parseColor("#111827"));
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), Color.parseColor("#24324D"));
        et.setBackground(bg);
        return et;
    }

    private void addWithTopMargin(LinearLayout root, View view, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(topDp);
        root.addView(view, lp);
    }

    private float parseFloatSafe(String text, float defaultValue) {
        try {
            if (text == null || text.trim().isEmpty()) return defaultValue;
            return Float.parseFloat(text.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private int parseIntSafe(String text, int defaultValue) {
        try {
            if (text == null || text.trim().isEmpty()) return defaultValue;
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static class ProjectOption {
        String id;
        String label;

        ProjectOption(String id, String label) {
            this.id = id;
            this.label = label;
        }

        @NonNull
        @Override
        public String toString() {
            return label;
        }
    }

    private List<ProjectOption> buildProjectOptions() {
        List<ProjectOption> list = new ArrayList<>();
        list.add(new ProjectOption("", "无所属项目"));

        if (currentMindMapView == null) return list;

        Map<String, Node> allNodes = currentMindMapView.getNodesInternal();
        if (allNodes == null) return list;

        for (Node node : allNodes.values()) {
            if (node == null) continue;
            if (node.getType() != Node.NodeType.PROJECT) continue;

            String title = safe(node.getTitle());
            if (title.isEmpty()) {
                title = "未命名项目";
            }

            String label = title + "  (" + node.getId() + ")";
            list.add(new ProjectOption(node.getId(), label));
        }

        return list;
    }

    private int findProjectSelectionIndex(List<ProjectOption> options, String targetProjectId) {
        String target = safe(targetProjectId);
        for (int i = 0; i < options.size(); i++) {
            if (safe(options.get(i).id).equals(target)) {
                return i;
            }
        }
        return 0;
    }

    // 新增：构建科学动作区（快捷动作、WOOP、If-Then、周复盘、检索练习、Premortem）
    private View buildActionBar(MainActivity activity, Node node) {
        LinearLayout wrap = new LinearLayout(requireContext());
        wrap.setOrientation(LinearLayout.VERTICAL);

        TextView title = buildSectionTitle("科学动作");
        wrap.addView(title);

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView quick = buildActionChip("快捷动作");
        quick.setOnClickListener(v -> {
            dismiss();
            activity.showQuickActionsForNode(node);
        });
        row.addView(quick);

        TextView smart = buildActionChip("智能补强");
        smart.setOnClickListener(v -> {
            dismiss();
            activity.getMindMapView().selectOnlyNode(node.getId());
            activity.runScientificEnhancement();
        });
        row.addView(smart);

        TextView woop = buildActionChip("WOOP");
        woop.setOnClickListener(v -> {
            dismiss();
            activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.WOOP);
        });
        row.addView(woop);

        TextView ifThen = buildActionChip("If-Then");
        ifThen.setOnClickListener(v -> {
            dismiss();
            activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.IF_THEN);
        });
        row.addView(ifThen);

        wrap.addView(row);

        LinearLayout row2 = new LinearLayout(requireContext());
        row2.setOrientation(LinearLayout.HORIZONTAL);

        TextView weekly = buildActionChip("周复盘");
        weekly.setOnClickListener(v -> {
            dismiss();
            activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.WEEKLY_REVIEW);
        });
        row2.addView(weekly);

        TextView retrieval = buildActionChip("检索练习");
        retrieval.setOnClickListener(v -> {
            dismiss();
            activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.RETRIEVAL_PRACTICE);
        });
        row2.addView(retrieval);

        TextView premortem = buildActionChip("Premortem");
        premortem.setOnClickListener(v -> {
            dismiss();
            activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.PREMORTEM);
        });
        row2.addView(premortem);

        wrap.addView(row2);

        LinearLayout row3 = new LinearLayout(requireContext());
        row3.setOrientation(LinearLayout.HORIZONTAL);

        TextView decisionLab = buildActionChip("决策实验室");
        decisionLab.setOnClickListener(v -> {
            dismiss();
            activity.openDecisionLab(node);
        });
        row3.addView(decisionLab);

        TextView memory = buildActionChip("记忆复习");
        memory.setOnClickListener(v -> {
            dismiss();
            activity.openMemoryReview();
        });
        row3.addView(memory);

        TextView focus = buildActionChip("Focus");
        focus.setOnClickListener(v -> {
            dismiss();
            activity.openFocusSession(node);
        });
        row3.addView(focus);

        wrap.addView(row3);

        LinearLayout row4 = new LinearLayout(requireContext());
        row4.setOrientation(LinearLayout.HORIZONTAL);

        TextView wrapDecision = buildActionChip("WRAP");
        wrapDecision.setOnClickListener(v -> {
            dismiss();
            activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.WRAP);
        });
        row4.addView(wrapDecision);

        TextView bayes = buildActionChip("Bayes");
        bayes.setOnClickListener(v -> {
            dismiss();
            activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.BAYES_UPDATE);
        });
        row4.addView(bayes);

        TextView dsrp = buildActionChip("DSRP");
        dsrp.setOnClickListener(v -> {
            dismiss();
            activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.DSRP_ANALYSIS);
        });
        row4.addView(dsrp);

        wrap.addView(row4);

        LinearLayout row5 = new LinearLayout(requireContext());
        row5.setOrientation(LinearLayout.HORIZONTAL);

        TextView forecast = buildActionChip("参考类预测");
        forecast.setOnClickListener(v -> {
            dismiss();
            activity.applyScientificTemplateToNode(node, ScientificTemplateEngine.TemplateType.REFERENCE_CLASS_FORECAST);
        });
        row5.addView(forecast);

        TextView autopilot = buildActionChip("全量推进");
        autopilot.setOnClickListener(v -> {
            dismiss();
            activity.getMindMapView().selectOnlyNode(node.getId());
            activity.runScientificAutopilot();
        });
        row5.addView(autopilot);

        TextView aiGap = buildActionChip("AI缺口");
        aiGap.setOnClickListener(v -> {
            dismiss();
            activity.openAiScienceCoach("gap", node);
        });
        row5.addView(aiGap);

        wrap.addView(row5);

        LinearLayout row6 = new LinearLayout(requireContext());
        row6.setOrientation(LinearLayout.HORIZONTAL);

        TextView aiRed = buildActionChip("AI红队");
        aiRed.setOnClickListener(v -> {
            dismiss();
            activity.openAiScienceCoach("redteam", node);
        });
        row6.addView(aiRed);

        TextView aiExec = buildActionChip("AI执行");
        aiExec.setOnClickListener(v -> {
            dismiss();
            activity.openAiScienceCoach("execution", node);
        });
        row6.addView(aiExec);

        TextView aiLearn = buildActionChip("AI学习");
        aiLearn.setOnClickListener(v -> {
            dismiss();
            activity.openAiScienceCoach("learning", node);
        });
        row6.addView(aiLearn);

        TextView link = buildActionChip("连线");
        link.setOnClickListener(v -> {
            dismiss();
            currentMindMapView.startConnectionMode(node);
        });
        row6.addView(link);

        TextView aiDecision = buildActionChip("AI决策");
        aiDecision.setOnClickListener(v -> {
            dismiss();
            activity.openAiScienceCoach("decision", node);
        });
        row6.addView(aiDecision);

        wrap.addView(row6);

        LinearLayout row7 = new LinearLayout(requireContext());
        row7.setOrientation(LinearLayout.HORIZONTAL);

        TextView triage = buildActionChip("体检");
        triage.setOnClickListener(v -> {
            dismiss();
            activity.getMindMapView().selectOnlyNode(node.getId());
            activity.openScientificTriage();
        });
        row7.addView(triage);

        TextView executionLog = buildActionChip("执行回填");
        executionLog.setOnClickListener(v -> {
            dismiss();
            activity.getMindMapView().selectOnlyNode(node.getId());
            activity.openExecutionLog();
        });
        row7.addView(executionLog);

        TextView decisionFollow = buildActionChip("决策落地");
        decisionFollow.setOnClickListener(v -> {
            dismiss();
            activity.getMindMapView().selectOnlyNode(node.getId());
            activity.openDecisionFollowThrough();
        });
        row7.addView(decisionFollow);

        TextView aiSuggest = buildActionChip("AI建议");
        aiSuggest.setOnClickListener(v -> {
            dismiss();
            activity.openAiScienceCoach("recommend", node);
        });
        row7.addView(aiSuggest);

        TextView aiAuto = buildActionChip("AI自动流");
        aiAuto.setOnClickListener(v -> {
            dismiss();
            activity.openAiScienceCoach("autopilot", node);
        });
        row7.addView(aiAuto);

        wrap.addView(row7);
        return wrap;
    }

    // 新增：构建动作区的单个 Chip 按钮
    private TextView buildActionChip(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#EDE9FE"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(dp(12), dp(11), dp(12), dp(11));

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.parseColor("#182338"));
        bg.setStroke(dp(1), Color.parseColor("#334155"));
        bg.setCornerRadius(dp(16));
        tv.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        lp.leftMargin = dp(4);
        lp.rightMargin = dp(4);
        lp.topMargin = dp(8);
        tv.setLayoutParams(lp);

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
        scrollView.setBackgroundColor(Color.parseColor("#0F172A"));

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(20);
        root.setPadding(p, p, p, dp(8));
        root.setBackgroundColor(Color.parseColor("#0F172A"));
        scrollView.addView(root);

        // 新增：在编辑页最顶部插入科学动作区
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            root.addView(buildActionBar(activity, currentNode));
            addWithTopMargin(root, new View(requireContext()), 12);
        }

        TextView basicTitle = buildSectionTitle("基础信息");
        root.addView(basicTitle);

        EditText titleInput = buildEditText(
                "标题",
                currentNode.getTitle(),
                InputType.TYPE_CLASS_TEXT
        );
        addWithTopMargin(root, titleInput, 10);

        EditText contentInput = buildEditText(
                "内容",
                currentNode.getContent(),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );
        contentInput.setMinLines(5);
        addWithTopMargin(root, contentInput, 14);

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
        addWithTopMargin(root, typeSpinner, 14);

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
        addWithTopMargin(root, shapeSpinner, 14);

        Spinner statusSpinner = new Spinner(requireContext());
        String[] statusLabels = new String[Node.NodeStatus.values().length];
        for (int i = 0; i < Node.NodeStatus.values().length; i++) {
            statusLabels[i] = Node.NodeStatus.values()[i].label;
        }
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                statusLabels
        );
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);
        statusSpinner.setSelection(currentNode.getStatus().ordinal());
        addWithTopMargin(root, statusSpinner, 14);

        TextView workflowTitle = buildSectionTitle("工作流字段");
        addWithTopMargin(root, workflowTitle, 22);

        EditText tagsInput = buildEditText(
                "标签，多个用逗号分隔",
                currentNode.getTagsAsString(),
                InputType.TYPE_CLASS_TEXT
        );
        addWithTopMargin(root, tagsInput, 10);

        EditText priorityInput = buildEditText(
                "优先级（1-5）",
                String.valueOf(currentNode.getPriority()),
                InputType.TYPE_CLASS_NUMBER
        );
        addWithTopMargin(root, priorityInput, 14);

        EditText dueAtInput = buildEditText(
                "截止时间，如：2026-03-20 20:00",
                currentNode.getDueAt(),
                InputType.TYPE_CLASS_TEXT
        );
        addWithTopMargin(root, dueAtInput, 14);

        EditText reviewAtInput = buildEditText(
                "复习/回顾时间，如：2026-03-18",
                currentNode.getReviewAt(),
                InputType.TYPE_CLASS_TEXT
        );
        addWithTopMargin(root, reviewAtInput, 14);

        EditText effortEstimateInput = buildEditText(
                "预计耗时（小时）",
                String.valueOf(currentNode.getEffortEstimate()),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        addWithTopMargin(root, effortEstimateInput, 14);

        EditText actualEffortInput = buildEditText(
                "实际耗时（小时）",
                String.valueOf(currentNode.getActualEffort()),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        addWithTopMargin(root, actualEffortInput, 14);

        EditText confidenceInput = buildEditText(
                "置信度（0~1）",
                String.valueOf(currentNode.getConfidence()),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        addWithTopMargin(root, confidenceInput, 14);

        EditText triggerInput = buildEditText(
                "触发条件 If，例如：如果晚上7点坐到书桌前",
                currentNode.getTriggerCondition(),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );
        addWithTopMargin(root, triggerInput, 14);

        TextView relationTitle = buildSectionTitle("归属与追踪");
        addWithTopMargin(root, relationTitle, 22);

        TextView projectHint = new TextView(requireContext());
        projectHint.setText("所属项目（优先用选择器，不要再手填 UUID）");
        projectHint.setTextColor(Color.parseColor("#93A4C3"));
        projectHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        addWithTopMargin(root, projectHint, 10);

        Spinner projectSpinner = new Spinner(requireContext());
        List<ProjectOption> projectOptions = buildProjectOptions();
        ArrayAdapter<ProjectOption> projectAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                projectOptions
        );
        projectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        projectSpinner.setAdapter(projectAdapter);
        projectSpinner.setSelection(findProjectSelectionIndex(projectOptions, currentNode.getProjectId()));
        addWithTopMargin(root, projectSpinner, 8);

        EditText projectIdInput = buildEditText(
                "所属项目ID（高级备用项，通常不用填）",
                currentNode.getProjectId(),
                InputType.TYPE_CLASS_TEXT
        );
        addWithTopMargin(root, projectIdInput, 12);

        EditText areaIdInput = buildEditText(
                "所属领域ID",
                currentNode.getAreaId(),
                InputType.TYPE_CLASS_TEXT
        );
        addWithTopMargin(root, areaIdInput, 14);

        EditText krTargetInput = buildEditText(
                "KR目标值",
                currentNode.getKrTarget() == 0f ? "" : String.valueOf(currentNode.getKrTarget()),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        addWithTopMargin(root, krTargetInput, 14);

        EditText krCurrentInput = buildEditText(
                "KR当前值",
                currentNode.getKrCurrent() == 0f ? "" : String.valueOf(currentNode.getKrCurrent()),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        addWithTopMargin(root, krCurrentInput, 14);

        TextView krHint = new TextView(requireContext());
        krHint.setText("KR 节点建议填写目标值和当前值；普通节点可以留空");
        krHint.setTextColor(Color.parseColor("#7C8AA8"));
        krHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        addWithTopMargin(root, krHint, 8);

        TextView advancedTitle = buildSectionTitle("证据 / 来源 / 扩展");
        addWithTopMargin(root, advancedTitle, 22);

        EditText evidenceStrengthInput = buildEditText(
                "证据强度（0~1）",
                String.valueOf(currentNode.getEvidenceStrength()),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        addWithTopMargin(root, evidenceStrengthInput, 10);

        EditText noteSourceInput = buildEditText(
                "来源/出处",
                currentNode.getNoteSource(),
                InputType.TYPE_CLASS_TEXT
        );
        addWithTopMargin(root, noteSourceInput, 14);

        EditText metaJsonInput = buildEditText(
                "扩展元数据（先留作以后 AI/模板用）",
                currentNode.getMetaJson(),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );
        metaJsonInput.setMinLines(3);
        addWithTopMargin(root, metaJsonInput, 14);

        AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Theme_AppCompat_Dialog))
                .setTitle("编辑节点")
                .setView(scrollView)
                .setNegativeButton("取消", (d, which) -> d.dismiss())
                .setNeutralButton("删除", (d, which) -> {
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
                    currentNode.setStatus(Node.NodeStatus.values()[statusSpinner.getSelectedItemPosition()]);

                    currentNode.setTagsFromString(tagsInput.getText().toString().trim());
                    currentNode.setPriority(parseIntSafe(priorityInput.getText().toString(), 3));
                    currentNode.setDueAt(dueAtInput.getText().toString().trim());
                    currentNode.setReviewAt(reviewAtInput.getText().toString().trim());
                    currentNode.setEffortEstimate(parseFloatSafe(effortEstimateInput.getText().toString(), 0f));
                    currentNode.setActualEffort(parseFloatSafe(actualEffortInput.getText().toString(), 0f));
                    currentNode.setConfidence(parseFloatSafe(confidenceInput.getText().toString(), 0.5f));
                    currentNode.setTriggerCondition(triggerInput.getText().toString().trim());

                    ProjectOption selectedProject = (ProjectOption) projectSpinner.getSelectedItem();
                    String selectedProjectId = selectedProject == null ? "" : safe(selectedProject.id);
                    String backupProjectId = projectIdInput.getText().toString().trim();
                    if (!selectedProjectId.isEmpty()) {
                        currentNode.setProjectId(selectedProjectId);
                    } else {
                        currentNode.setProjectId(backupProjectId);
                    }

                    currentNode.setAreaId(areaIdInput.getText().toString().trim());
                    currentNode.setKrTarget(parseFloatSafe(krTargetInput.getText().toString(), 0f));
                    currentNode.setKrCurrent(parseFloatSafe(krCurrentInput.getText().toString(), 0f));
                    currentNode.setEvidenceStrength(parseFloatSafe(evidenceStrengthInput.getText().toString(), 0.5f));
                    currentNode.setNoteSource(noteSourceInput.getText().toString().trim());
                    currentNode.setMetaJson(metaJsonInput.getText().toString().trim());

                    // 新增：保存时执行工作流归一化
                    WorkflowEngine.normalizeNodeForWorkflow(currentNode);

                    if (getActivity() instanceof NodeEditListener) {
                        ((NodeEditListener) getActivity()).onNodeUpdated(currentNode);
                    }
                })
                .create();

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_panel_popup);
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#F8FAFC"));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#A8B3CF"));
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.parseColor("#FB7185"));
        });

        return dialog;
    }
}
