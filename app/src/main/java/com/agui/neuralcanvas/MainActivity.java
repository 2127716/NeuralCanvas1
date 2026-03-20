package com.agui.neuralcanvas;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.multidex.MultiDex;

public class MainActivity extends AppCompatActivity
        implements NodeEditDialog.NodeEditListener,
        SearchDialog.SearchListener,
        MindMapView.OnDataChangeListener {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        MultiDex.install(this);
    }

    private MindMapView mindMapView;
    private SimpleDataManager dataManager;

    private final Handler autoSaveHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Runnable autoSaveRunnable = this::saveCurrentDataSilently;

    private long lastToastAt = 0L;
    private long lastGuidanceShownAt = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.init(this);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        mindMapView = findViewById(R.id.mindMapView);
        mindMapView.setOnDataChangeListener(this);

        dataManager = new SimpleDataManager(getApplication());

        ImageButton btnAdd = findViewById(R.id.btn_add_node);
        if (btnAdd != null) btnAdd.setOnClickListener(v -> showAddNodeDialog());

        ImageButton btnSearch = findViewById(R.id.btn_search);
        if (btnSearch != null) btnSearch.setOnClickListener(v -> showSearchDialog());

        ImageButton btnMore = findViewById(R.id.btn_more);
        if (btnMore != null) btnMore.setOnClickListener(v -> showMoreMenu());

        applyToolbarTheme();

        android.view.View root = findViewById(android.R.id.content);
        if (root != null) root.setBackgroundColor(ThemeManager.getBg());

        loadSavedData();
        BrainAutopilotScheduler.ensureScheduled(this);
        BrainAutopilotScheduler.requestImmediatePulse(this);
        handleBrainLaunchIntent(getIntent());
        maybeShowPendingBrainGuidance(false);
    }

    private void handleBrainLaunchIntent(Intent intent) {
        if (intent == null || mindMapView == null) return;
        final String focusNodeId = intent.getStringExtra("brain_focus_node_id");
        final String focusMode = intent.getStringExtra("brain_focus_mode");
        final boolean openMode = intent.getBooleanExtra("brain_open_mode", false);
        if (focusNodeId == null || focusNodeId.trim().isEmpty()) return;

        mindMapView.post(() -> {
            Node node = mindMapView.getNodesInternal().get(focusNodeId);
            if (node == null) return;
            mindMapView.focusNodeById(focusNodeId);
            mindMapView.selectNodeById(focusNodeId);
            maybeToast("智能巡检已定位到关键节点");
            if (openMode) {
                WorkflowModeDialog.show(this, node, focusMode);
            }
        });

        intent.removeExtra("brain_focus_node_id");
        intent.removeExtra("brain_focus_mode");
        intent.removeExtra("brain_open_mode");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleBrainLaunchIntent(intent);
        maybeShowPendingBrainGuidance(true);
    }

    private void maybeShowPendingBrainGuidance(boolean force) {
        if (dataManager == null || mindMapView == null) return;
        BrainPendingGuidance guidance = dataManager.loadPendingBrainGuidance();
        if (guidance == null) return;
        long now = System.currentTimeMillis();
        if (!force && now - lastGuidanceShownAt < 1200L) return;
        lastGuidanceShownAt = now;
        dataManager.clearPendingBrainGuidance();

        if (guidance.focusNodeId != null && !guidance.focusNodeId.trim().isEmpty()) {
            mindMapView.post(() -> {
                mindMapView.focusNodeById(guidance.focusNodeId);
                mindMapView.selectNodeById(guidance.focusNodeId);
            });
        }

        mindMapView.post(() -> AiAutopilotGuideDialog.show(this, guidance));
    }

    public MindMapView getMindMapView() { return mindMapView; }
    public SimpleDataManager getDataManager() { return dataManager; }

    public void onGraphMutatedByAi() {
        mindMapView.requestRender();
        scheduleAutoSave();
    }

    private void loadSavedData() {
        try {
            Map<?, ?> savedData = dataManager.loadMindMap();
            Map<String, Node> nodes = (Map<String, Node>) savedData.get("nodes");
            Map<String, Connection> connections = (Map<String, Connection>) savedData.get("connections");
            if (nodes != null && !nodes.isEmpty()) {
                mindMapView.setNodes(nodes);
                mindMapView.setConnections(connections != null ? connections : new HashMap<>());
                Toast.makeText(this, "已加载保存的数据", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "加载数据失败", Toast.LENGTH_SHORT).show();
        }
    }

    public void showMoreMenu() {
        MoreMenuDialog.newInstance(id -> MainMenuActionHandler.handle(this, id))
                .show(getSupportFragmentManager(), "more_menu");
    }

    private void applyToolbarTheme() {
        android.view.View toolbar = findViewById(R.id.custom_toolbar);
        if (toolbar != null) {
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(ThemeManager.getToolbarBg());
            bg.setCornerRadius(dpToPx(20));
            bg.setStroke(dpToPx(1), ThemeManager.getStroke());
            toolbar.setBackground(bg);
        }
        android.widget.TextView title = findViewById(R.id.toolbar_title);
        if (title != null) title.setTextColor(ThemeManager.getTextPrimary());
        android.widget.TextView subtitle = findViewById(R.id.toolbar_subtitle);
        if (subtitle != null) subtitle.setTextColor(ThemeManager.getTextSecondary());

        int iconTint = ThemeManager.getTextPrimary();
        int[] btnIds = {R.id.btn_add_node, R.id.btn_search, R.id.btn_more};
        for (int id : btnIds) {
            android.widget.ImageButton btn = findViewById(id);
            if (btn != null) btn.setColorFilter(iconTint);
        }
    }

    private int dpToPx(int dp) { return (int) (dp * getResources().getDisplayMetrics().density); }

    public void showAddNodeDialog() {
        Node newNode = new Node("新节点", "输入内容", 120f, 120f, Node.NodeType.CONCEPT);
        mindMapView.addNode(newNode);
        showNodeEditDialog(newNode);
    }

    public void showSearchDialog() {
        DialogFragment dialog = SearchDialog.newInstance(mindMapView);
        dialog.show(getSupportFragmentManager(), "search_dialog");
    }

    public void showAiAssistantDialog() {
        if (isFinishing() || isDestroyed()) return;
        if (getSupportFragmentManager().findFragmentByTag("ai_assistant_dialog") != null) return;
        AiAssistantDialog.newInstance().show(getSupportFragmentManager(), "ai_assistant_dialog");
    }

    public void showAiAssistantDialogWithPrompt(String presetPrompt) {
        if (isFinishing() || isDestroyed()) return;
        if (getSupportFragmentManager().findFragmentByTag("ai_assistant_dialog") != null) return;
        AiAssistantDialog.newInstance(presetPrompt).show(getSupportFragmentManager(), "ai_assistant_dialog");
    }

    public void openAiScienceCoach(String mode, Node node) {
        if (node == null) {
            Toast.makeText(this, "请先选中一个节点", Toast.LENGTH_SHORT).show();
            return;
        }
        mindMapView.selectOnlyNode(node.getId());
        String prompt = AiScientificPrompts.gapCheck(node);
        if ("execution".equalsIgnoreCase(mode)) prompt = AiScientificPrompts.executionCoach(node);
        else if ("learning".equalsIgnoreCase(mode)) prompt = AiScientificPrompts.learningCoach(node);
        else if ("decision".equalsIgnoreCase(mode)) prompt = AiScientificPrompts.decisionCoach(node);
        else if ("redteam".equalsIgnoreCase(mode)) prompt = AiScientificPrompts.redTeam(node);
        else if ("recommend".equalsIgnoreCase(mode)) prompt = AiScientificPrompts.workflowRecommendation(node);
        else if ("triage".equalsIgnoreCase(mode)) prompt = AiScientificPrompts.triage(node);
        else if ("autopilot".equalsIgnoreCase(mode)) prompt = AiScientificPrompts.autopilot(node);
        showAiAssistantDialogWithPrompt(prompt);
    }

    public void showKnowledgeImportDialog() {
        if (isFinishing() || isDestroyed()) return;
        if (getSupportFragmentManager().findFragmentByTag("knowledge_import_dialog") != null) return;
        KnowledgeImportDialog.newInstance().show(getSupportFragmentManager(), "knowledge_import_dialog");
    }

    public void showHelpDialog() {
        if (isFinishing() || isDestroyed()) return;
        startActivity(new Intent(this, HelpActivity.class));
    }

    public void showThemePicker() {
        ThemeManager.AppTheme[] themes = ThemeManager.AppTheme.values();
        String[] labels = new String[themes.length];
        for (int i = 0; i < themes.length; i++) labels[i] = themes[i].label;
        int current = ThemeManager.getCurrentTheme().ordinal();
        new AlertDialog.Builder(this)
                .setTitle("切换主题")
                .setSingleChoiceItems(labels, current, (dialog, which) -> {
                    ThemeManager.setTheme(this, themes[which]);
                    dialog.dismiss();
                    recreate();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    public void confirmClearAll() {
        new AlertDialog.Builder(this)
                .setTitle("清除全部")
                .setMessage("确定删除全部节点和连线吗？此操作会覆盖当前自动保存数据。")
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (dialog, which) -> {
                    mindMapView.clearAll();
                    saveCurrentDataSilently();
                    Toast.makeText(this, "已清除全部内容", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    public void openInboxClarifier() {
        InboxClarifierDialog.show(this, mindMapView.getNodesInternal(), new InboxClarifierDialog.Callback() {
            @Override public void onNodeConverted(Node node) { onNodeUpdated(node); mindMapView.requestRender(); scheduleAutoSave(); }
            @Override public void onBatchFinished() { mindMapView.requestRender(); scheduleAutoSave(); }
        });
    }

    public void openWeeklyReview() {
        WeeklyReviewDialog.show(this, mindMapView.getNodesInternal(), mindMapView.getConnectionsInternal());
    }

    public void openDecisionMatrix() {
        Node baseNode = getSingleSelectedNode();
        if (baseNode == null) { Toast.makeText(this, "请先选中一个决策/项目/目标节点", Toast.LENGTH_SHORT).show(); return; }
        DecisionMatrixDialog.newInstance(baseNode, mindMapView.getNodesInternal(), mindMapView.getConnectionsInternal(), new Runnable() {
            @Override public void run() { mindMapView.requestRender(); scheduleAutoSave(); }
        }).show(getSupportFragmentManager(), "decision_matrix_dialog");
    }

    public void openMemoryReview() {
        MemoryReviewDialog.newInstance(mindMapView.getNodesInternal(), new Runnable() {
            @Override public void run() { mindMapView.requestRender(); scheduleAutoSave(); }
        }).show(getSupportFragmentManager(), "memory_review_dialog");
    }

    public void openExecutionLog() {
        Node baseNode = getSingleSelectedNode();
        if (baseNode == null) { Toast.makeText(this, "请先选中一个任务/行动/项目节点", Toast.LENGTH_SHORT).show(); return; }
        ExecutionLogDialog.newInstance(baseNode, mindMapView.getNodesInternal(), new Runnable() {
            @Override public void run() { mindMapView.requestRender(); scheduleAutoSave(); }
        }).show(getSupportFragmentManager(), "execution_log_dialog");
    }

    public void openDecisionFollowThrough() {
        Node baseNode = getSingleSelectedNode();
        if (baseNode == null) { Toast.makeText(this, "请先选中一个决策/目标/项目节点", Toast.LENGTH_SHORT).show(); return; }
        DecisionFollowThroughDialog.newInstance(baseNode, mindMapView.getNodesInternal(), mindMapView.getConnectionsInternal(), new Runnable() {
            @Override public void run() { mindMapView.requestRender(); scheduleAutoSave(); }
        }).show(getSupportFragmentManager(), "decision_follow_dialog");
    }

    public void openFocusSession() {
        Node baseNode = getSingleSelectedNode();
        if (baseNode == null) { Toast.makeText(this, "请先选中一个任务/行动节点", Toast.LENGTH_SHORT).show(); return; }
        FocusSessionDialog.newInstance(this, baseNode).show(getSupportFragmentManager(), "focus_session_dialog");
    }

    public void openFocusSession(Node node) {
        if (node == null) return; mindMapView.selectOnlyNode(node.getId()); openFocusSession();
    }
    public void startFocusSession(Node node, int minutes) {
        if (node == null) { Toast.makeText(this, "未选中节点", Toast.LENGTH_SHORT).show(); return; }
        FocusSessionEngine.start(this, node, minutes); mindMapView.requestRender(); scheduleAutoSave(); Toast.makeText(this, "已开始专注 Session", Toast.LENGTH_SHORT).show();
    }
    public void finishRunningFocusSession(boolean interrupted) {
        FocusSessionEngine.SessionInfo info = FocusSessionEngine.getCurrent(this);
        if (info == null) { Toast.makeText(this, "当前没有进行中的 Session", Toast.LENGTH_SHORT).show(); return; }
        Node node = mindMapView.getNodesInternal().get(info.nodeId);
        if (node != null) FocusSessionEngine.markTrigger(node, !interrupted);
        if (interrupted) FocusSessionEngine.interrupt(this);
        float hours = FocusSessionEngine.finish(this, mindMapView.getNodesInternal(), !interrupted);
        mindMapView.requestRender(); scheduleAutoSave();
        Toast.makeText(this, (interrupted ? "Session 已中断，记录时长 " : "Session 已完成，记录时长 ") + String.format(java.util.Locale.US, "%.2f", hours) + " 小时", Toast.LENGTH_SHORT).show();
    }

    public void openGraphInsights() {
        Node baseNode = getSingleSelectedNode();
        GraphInsightDialog.newInstance(baseNode, mindMapView.getNodesInternal(), mindMapView.getConnectionsInternal())
                .show(getSupportFragmentManager(), "graph_insight_dialog");
    }
    public void openGraphIntelligence() { openGraphInsights(); }
    public void openProjectsHubWorkflowView() { ProjectsHubDialog.newInstance().show(getSupportFragmentManager(), "projects_hub"); }
    public void openScientificDashboard() { ScientificDashboardDialog.newInstance().show(getSupportFragmentManager(), "scientific_dashboard"); }
    public void openDecisionLab(Node node) { if (node == null) return; mindMapView.selectOnlyNode(node.getId()); openDecisionMatrix(); }

    public void showQuickActionsForNode(Node node) {
        if (node == null) return;
        java.util.List<String> actions = QuickActionEngine.getDynamicActions(node);
        String[] items = actions.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle(safeTitle(node))
                .setItems(items, (dialog, which) -> {
                    String action = actions.get(which);
                    QuickActionEngine.executeDynamicAction(this, node, action);
                    mindMapView.requestRender();
                    scheduleAutoSave();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    public void applyScientificTemplateToNode(Node node, ScientificTemplateEngine.TemplateType type) {
        if (node == null || type == null) return;
        mindMapView.selectOnlyNode(node.getId());
        switch (type) {
            case WOOP: applyTemplateResult(ScientificTemplateEngine.generateWoop(node, mindMapView.getNodesInternal())); break;
            case IF_THEN: applyTemplateResult(ScientificTemplateEngine.generateIfThen(node, mindMapView.getNodesInternal())); break;
            case DAILY_REVIEW: applyTemplateResult(ScientificTemplateEngine.generateDailyReview(node, mindMapView.getNodesInternal())); break;
            case WEEKLY_REVIEW: applyTemplateResult(ScientificTemplateEngine.generateWeeklyReview(node, mindMapView.getNodesInternal())); break;
            case AAR: applyTemplateResult(ScientificTemplateEngine.generateAarReview(node, mindMapView.getNodesInternal())); break;
            case DECISION_TREE: applyTemplateResult(ScientificTemplateEngine.generateDecisionTree(node, mindMapView.getNodesInternal())); break;
            case PREMORTEM: applyTemplateResult(ScientificTemplateEngine.generatePremortem(node, mindMapView.getNodesInternal())); break;
            case EVIDENCE_REVIEW: applyTemplateResult(ScientificTemplateEngine.generateEvidenceReview(node, mindMapView.getNodesInternal())); break;
            case RETRIEVAL_PRACTICE: applyTemplateResult(ScientificTemplateEngine.generateRetrievalPractice(node, mindMapView.getNodesInternal())); break;
            case CONCEPT_DEEPENING: applyTemplateResult(ScientificTemplateEngine.generateConceptDeepening(node, mindMapView.getNodesInternal())); break;
            case TRANSFER_PRACTICE: applyTemplateResult(ScientificTemplateEngine.generateTransferPractice(node, mindMapView.getNodesInternal())); break;
            case WRAP: applyTemplateResult(ScientificTemplateEngine.generateWrap(node, mindMapView.getNodesInternal())); break;
            case BAYES_UPDATE: applyTemplateResult(ScientificTemplateEngine.generateBayesUpdate(node, mindMapView.getNodesInternal())); break;
            case DSRP_ANALYSIS: applyTemplateResult(ScientificTemplateEngine.generateDsrpAnalysis(node, mindMapView.getNodesInternal())); break;
            case REFERENCE_CLASS_FORECAST: applyTemplateResult(ScientificTemplateEngine.generateReferenceClassForecast(node, mindMapView.getNodesInternal())); break;
            default: break;
        }
        mindMapView.focusNodeById(node.getId());
        mindMapView.requestRender();
        scheduleAutoSave();
    }

    public void editNodeFromQuickAction(Node node) { if (node != null) showNodeEditDialog(node); }
    public void deleteNodeFromQuickAction(Node node) {
        if (node == null) return;
        new AlertDialog.Builder(this)
                .setTitle("删除节点")
                .setMessage("确定删除“" + safeTitle(node) + "”？")
                .setPositiveButton("删除", (dialog, which) -> { mindMapView.removeNode(node.getId()); mindMapView.requestRender(); scheduleAutoSave(); })
                .setNegativeButton("取消", null)
                .show();
    }

    public void generateScientificTemplate(ScientificTemplateEngine.TemplateType type) {
        if (type == null) return;
        switch (type) {
            case WOOP: generateWoopFromSelectedNode(); break;
            case IF_THEN: generateIfThenFromSelectedNode(); break;
            case DAILY_REVIEW: generateDailyReviewFromSelectedNode(); break;
            case WEEKLY_REVIEW: generateWeeklyReviewFromSelectedNode(); break;
            case AAR: generateAarFromSelectedNode(); break;
            case DECISION_TREE: generateDecisionTreeFromSelectedNode(); break;
            case PREMORTEM: generatePremortemFromSelectedNode(); break;
            case EVIDENCE_REVIEW: generateEvidenceReviewFromSelectedNode(); break;
            case RETRIEVAL_PRACTICE: generateRetrievalPracticeFromSelectedNode(); break;
            case CONCEPT_DEEPENING: generateConceptDeepeningFromSelectedNode(); break;
            case TRANSFER_PRACTICE: generateTransferPracticeFromSelectedNode(); break;
            case WRAP: generateWrapFromSelectedNode(); break;
            case BAYES_UPDATE: generateBayesUpdateFromSelectedNode(); break;
            case DSRP_ANALYSIS: generateDsrpAnalysisFromSelectedNode(); break;
            case REFERENCE_CLASS_FORECAST: generateReferenceClassForecastFromSelectedNode(); break;
            default: Toast.makeText(this, "暂不支持该模板", Toast.LENGTH_SHORT).show();
        }
    }
    public void runScientificEnhancement() { Node baseNode = getSingleSelectedNode(); if (baseNode == null) { Toast.makeText(this, "请先单击选中一个节点", Toast.LENGTH_SHORT).show(); return; } ScientificEnhancementEngine.EnhancementResult result = ScientificEnhancementEngine.enhance(baseNode, mindMapView.getNodesInternal(), mindMapView.getConnectionsInternal()); applyTemplateResult(result.templateResult); if (result.touchedBaseNode) onNodeUpdated(baseNode); mindMapView.focusNodeById(baseNode.getId()); Toast.makeText(this, ScientificEnhancementEngine.buildSummary(result), Toast.LENGTH_LONG).show(); }
    public void runScientificAutopilot() { Node baseNode = getSingleSelectedNode(); if (baseNode == null) { Toast.makeText(this, "请先单击选中一个节点", Toast.LENGTH_SHORT).show(); return; } applyTemplateResult(ScientificAutopilotEngine.run(baseNode, mindMapView.getNodesInternal(), mindMapView.getConnectionsInternal())); mindMapView.focusNodeById(baseNode.getId()); Toast.makeText(this, "已对当前节点执行全量科学推进", Toast.LENGTH_LONG).show(); }
    public void runAiGapCheck() { generateAiGapCheckFromSelectedNode(); }
    public void runAiExecutionPatch() { generateAiExecutionPatchFromSelectedNode(); }
    public void runAiLearningPatch() { generateAiLearningPatchFromSelectedNode(); }

    private String safeTitle(Node node) { if (node == null) return "(空节点)"; String title = node.getTitle(); return (title == null || title.trim().isEmpty()) ? "(无标题)" : title.trim(); }
    private Node getSingleSelectedNode() { List<String> selectedIds = mindMapView.getSelectedNodeIds(); if (selectedIds == null || selectedIds.isEmpty()) return null; return mindMapView.getNodesInternal().get(selectedIds.get(0)); }
    private void applyTemplateResult(ScientificTemplateEngine.TemplateResult result) {
        if (result == null) return;
        Node baseNode = getSingleSelectedNode();
        TemplatePostProcessor.postProcess(baseNode, result, mindMapView.getNodesInternal());
        for (Node node : result.createdNodes) mindMapView.addNode(node);
        for (Connection connection : result.createdConnections) mindMapView.addConnection(connection);
        TemplateStateSynchronizer.synchronize(baseNode, result, mindMapView.getNodesInternal(), mindMapView.getConnectionsInternal());
        mindMapView.requestRender(); scheduleAutoSave();
    }

    private void generateWoopFromSelectedNode() { Node baseNode = getSingleSelectedNode(); if (baseNode == null) { Toast.makeText(this, "请先单击选中一个目标/任务/项目节点", Toast.LENGTH_SHORT).show(); return; } applyTemplateResult(ScientificTemplateEngine.generateWoop(baseNode, mindMapView.getNodesInternal())); mindMapView.focusNodeById(baseNode.getId()); Toast.makeText(this, "已生成 WOOP 子图", Toast.LENGTH_SHORT).show(); }
    private void generateIfThenFromSelectedNode() { Node baseNode = getSingleSelectedNode(); if (baseNode == null) { Toast.makeText(this, "请先单击选中一个任务/行动/目标节点", Toast.LENGTH_SHORT).show(); return; } applyTemplateResult(ScientificTemplateEngine.generateIfThen(baseNode, mindMapView.getNodesInternal())); mindMapView.focusNodeById(baseNode.getId()); Toast.makeText(this, "已生成 If-Then 子图", Toast.LENGTH_SHORT).show(); }
    private void generateDailyReviewFromSelectedNode() { Node baseNode = getSingleSelectedNode(); if (baseNode == null) { Toast.makeText(this, "请先单击选中一个要复盘的节点", Toast.LENGTH_SHORT).show(); return; } applyTemplateResult(ScientificTemplateEngine.generateDailyReview(baseNode, mindMapView.getNodesInternal())); mindMapView.focusNodeById(baseNode.getId()); Toast.makeText(this, "已生成每日复盘子图", Toast.LENGTH_SHORT).show(); }
    private void generateWeeklyReviewFromSelectedNode() { Node baseNode = getSingleSelectedNode(); if (baseNode == null) { Toast.makeText(this, "请先单击选中一个要周复盘的节点", Toast.LENGTH_SHORT).show(); return; } applyTemplateResult(ScientificTemplateEngine.generateWeeklyReview(baseNode, mindMapView.getNodesInternal())); mindMapView.focusNodeById(baseNode.getId()); Toast.makeText(this, "已生成每周复盘子图", Toast.LENGTH_SHORT).show(); }
    private void generateAarFromSelectedNode() { Node baseNode = getSingleSelectedNode(); if (baseNode == null) { Toast.makeText(this, "请先单击选中一个要做AAR的节点", Toast.LENGTH_SHORT).show(); return; } applyTemplateResult(ScientificTemplateEngine.generateAarReview(baseNode, mindMapView.getNodesInternal())); mindMapView.focusNodeById(baseNode.getId()); Toast.makeText(this, "已生成AAR复盘子图", Toast.LENGTH_SHORT).show(); }
    private void generateDecisionTreeFromSelectedNode() { Node baseNode = getSingleSelectedNode(); if (baseNode == null) { Toast.makeText(this, "请先单击选中一个要分析的节点", Toast.LENGTH_SHORT).show(); return; } applyTemplateResult(ScientificTemplateEngine.generateDecisionTree(baseNode, mindMapView.getNodesInternal())); mindMapView.focusNodeById(baseNode.getId()); Toast.makeText(this, "已生成科学决策子图", Toast.LENGTH_SHORT).show(); }
    private void generatePremortemFromSelectedNode() { Node baseNode = getSingleSelectedNode(); if (baseNode == null) { Toast.makeText(this, "请先单击选中一个项目/任务/决策节点", Toast.LENGTH_SHORT).show(); return; } applyTemplateResult(ScientificTemplateEngine.generatePremortem(baseNode, mindMapView.getNodesInternal())); mindMapView.focusNodeById(baseNode.getId()); Toast.makeText(this, "已生成 Premortem 子图", Toast.LENGTH_SHORT).show(); }
    private void generateEvidenceReviewFromSelectedNode() { Node baseNode = getSingleSelectedNode(); if (baseNode == null) { Toast.makeText(this, "请先单击选中一个要做证据审查的节点", Toast.LENGTH_SHORT).show(); return; } applyTemplateResult(ScientificTemplateEngine.generateEvidenceReview(baseNode, mindMapView.getNodesInternal())); mindMapView.focusNodeById(baseNode.getId()); Toast.makeText(this, "已生成证据评估子图", Toast.LENGTH_SHORT).show(); }
    private void generateRetrievalPracticeFromSelectedNode() { Node baseNode = getSingleSelectedNode(); if (baseNode == null) { Toast.makeText(this, "请先单击选中一个知识节点", Toast.LENGTH_SHORT).show(); return; } applyTemplateResult(ScientificTemplateEngine.generateRetrievalPractice(baseNode, mindMapView.getNodesInternal())); mindMapView.focusNodeById(baseNode.getId()); Toast.makeText(this, "已生成检索练习子图", Toast.LENGTH_SHORT).show(); }
    private void generateConceptDeepeningFromSelectedNode() { Node baseNode = getSingleSelectedNode(); if (baseNode == null) { Toast.makeText(this, "请先单击选中一个概念节点", Toast.LENGTH_SHORT).show(); return; } applyTemplateResult(ScientificTemplateEngine.generateConceptDeepening(baseNode, mindMapView.getNodesInternal())); mindMapView.focusNodeById(baseNode.getId()); Toast.makeText(this, "已生成概念深化子图", Toast.LENGTH_SHORT).show(); }
    private void generateWrapFromSelectedNode() { Node baseNode = getSingleSelectedNode(); if (baseNode == null) { Toast.makeText(this, "请先单击选中一个决策/项目/目标节点", Toast.LENGTH_SHORT).show(); return; } applyTemplateResult(ScientificTemplateEngine.generateWrap(baseNode, mindMapView.getNodesInternal())); mindMapView.focusNodeById(baseNode.getId()); Toast.makeText(this, "已生成 WRAP 决策护栏子图", Toast.LENGTH_SHORT).show(); }
    private void generateBayesUpdateFromSelectedNode() { Node baseNode = getSingleSelectedNode(); if (baseNode == null) { Toast.makeText(this, "请先单击选中一个假设/决策/问题节点", Toast.LENGTH_SHORT).show(); return; } applyTemplateResult(ScientificTemplateEngine.generateBayesUpdate(baseNode, mindMapView.getNodesInternal())); mindMapView.focusNodeById(baseNode.getId()); Toast.makeText(this, "已生成贝叶斯更新子图", Toast.LENGTH_SHORT).show(); }
    private void generateDsrpAnalysisFromSelectedNode() { Node baseNode = getSingleSelectedNode(); if (baseNode == null) { Toast.makeText(this, "请先单击选中一个概念/问题/项目节点", Toast.LENGTH_SHORT).show(); return; } applyTemplateResult(ScientificTemplateEngine.generateDsrpAnalysis(baseNode, mindMapView.getNodesInternal())); mindMapView.focusNodeById(baseNode.getId()); Toast.makeText(this, "已生成 DSRP 结构分析子图", Toast.LENGTH_SHORT).show(); }
    private void generateReferenceClassForecastFromSelectedNode() { Node baseNode = getSingleSelectedNode(); if (baseNode == null) { Toast.makeText(this, "请先单击选中一个项目/任务/目标节点", Toast.LENGTH_SHORT).show(); return; } applyTemplateResult(ScientificTemplateEngine.generateReferenceClassForecast(baseNode, mindMapView.getNodesInternal())); mindMapView.focusNodeById(baseNode.getId()); Toast.makeText(this, "已生成参考类预测子图", Toast.LENGTH_SHORT).show(); }
    private void generateAiGapCheckFromSelectedNode() { Node baseNode = getSingleSelectedNode(); if (baseNode == null) { Toast.makeText(this, "请先单击选中一个节点", Toast.LENGTH_SHORT).show(); return; } applyTemplateResult(ScientificTemplateEngine.generateAiGapCheck(baseNode, mindMapView.getNodesInternal())); mindMapView.focusNodeById(baseNode.getId()); Toast.makeText(this, "已生成 AI缺口检查 子图", Toast.LENGTH_SHORT).show(); }
    private void generateAiExecutionPatchFromSelectedNode() { Node baseNode = getSingleSelectedNode(); if (baseNode == null) { Toast.makeText(this, "请先单击选中一个执行相关节点", Toast.LENGTH_SHORT).show(); return; } applyTemplateResult(ScientificTemplateEngine.generateAiExecutionPatch(baseNode, mindMapView.getNodesInternal())); mindMapView.focusNodeById(baseNode.getId()); Toast.makeText(this, "已生成 AI执行补全 子图", Toast.LENGTH_SHORT).show(); }
    private void generateAiLearningPatchFromSelectedNode() { Node baseNode = getSingleSelectedNode(); if (baseNode == null) { Toast.makeText(this, "请先单击选中一个学习相关节点", Toast.LENGTH_SHORT).show(); return; } applyTemplateResult(ScientificTemplateEngine.generateAiLearningPatch(baseNode, mindMapView.getNodesInternal())); mindMapView.focusNodeById(baseNode.getId()); Toast.makeText(this, "已生成 AI学习补全 子图", Toast.LENGTH_SHORT).show(); }

    public void showNodeEditDialog(Node node) {
        DialogFragment dialog = NodeEditDialog.newInstance(node, mindMapView);
        dialog.show(getSupportFragmentManager(), "node_edit_dialog");
    }

    private void scheduleAutoSave() { autoSaveHandler.removeCallbacks(autoSaveRunnable); autoSaveHandler.postDelayed(autoSaveRunnable, 550); }
    private void saveCurrentDataSilently() {
        try {
            final Map<String, Node> nodes = mindMapView.getNodes();
            final Map<String, Connection> connections = mindMapView.getConnections();
            ioExecutor.execute(() -> {
                try { dataManager.saveMindMap(nodes, connections); } catch (Exception e) { e.printStackTrace(); }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override public void onNodeUpdated(Node node) { mindMapView.requestRender(); scheduleAutoSave(); maybeToast("节点已更新"); }
    @Override public void onNodeDeleted(Node node) { scheduleAutoSave(); maybeToast("节点已删除"); }
    @Override public void onSearch(String keyword, List<Node.NodeType> types, boolean highlight) { mindMapView.search(keyword, types, highlight); int resultCount = mindMapView.getSearchResultCount(); maybeToast(resultCount > 0 ? "找到 " + resultCount + " 个匹配节点" : "未找到匹配节点"); }
    @Override public void onClearSearch() { mindMapView.clearSearch(); maybeToast("搜索已清除"); }
    @Override public void onDataChanged() { scheduleAutoSave(); }

    @Override
    protected void onResume() {
        super.onResume();
        maybeShowPendingBrainGuidance(false);
        try {
            BrainAutopilotSettings settings = dataManager == null ? null : dataManager.loadAutopilotSettings();
            if (settings != null && settings.isEnabled() && settings.isApiAutopilotEnabled() && settings.isInAppPulseOnResume()) {
                BrainAutopilotScheduler.requestImmediatePulse(this);
            }
        } catch (Exception ignored) {}
    }

    @Override protected void onPause() { super.onPause(); autoSaveHandler.removeCallbacks(autoSaveRunnable); saveCurrentDataSilently(); }
    @Override protected void onDestroy() { autoSaveHandler.removeCallbacks(autoSaveRunnable); ioExecutor.shutdown(); super.onDestroy(); }

    private void maybeToast(String message) {
        long now = System.currentTimeMillis();
        if (now - lastToastAt < 500L) return;
        lastToastAt = now;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
