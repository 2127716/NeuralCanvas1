package com.agui.neuralcanvas;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.multidex.MultiDex;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mindMapView = findViewById(R.id.mindMapView);
        mindMapView.setOnDataChangeListener(this);

        dataManager = new SimpleDataManager(getApplication());

        loadSavedData();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("NeuralCanvas");
            getSupportActionBar().setSubtitle("思维地图");
        }
    }

    public MindMapView getMindMapView() {
        return mindMapView;
    }

    public SimpleDataManager getDataManager() {
        return dataManager;
    }

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (MainMenuActionHandler.handle(this, item.getItemId())) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // =========================
    // 给 MainMenuActionHandler 用的公开方法
    // =========================

    public void showAddNodeDialog() {
        float baseX = 120f;
        float baseY = 120f;

        Node newNode = new Node(
                "新节点",
                "输入内容",
                baseX,
                baseY,
                Node.NodeType.CONCEPT
        );
        mindMapView.addNode(newNode);
        showNodeEditDialog(newNode);
    }

    public void showSearchDialog() {
        DialogFragment dialog = SearchDialog.newInstance(mindMapView);
        dialog.show(getSupportFragmentManager(), "search_dialog");
    }

    public void generateScientificTemplate(ScientificTemplateEngine.TemplateType type) {
        if (type == null) return;

        switch (type) {
            case WOOP:
                generateWoopFromSelectedNode();
                break;
            case IF_THEN:
                generateIfThenFromSelectedNode();
                break;
            case DAILY_REVIEW:
                generateDailyReviewFromSelectedNode();
                break;
            case WEEKLY_REVIEW:
                generateWeeklyReviewFromSelectedNode();
                break;
            case AAR:
                generateAarFromSelectedNode();
                break;
            case DECISION_TREE:
                generateDecisionTreeFromSelectedNode();
                break;
            case PREMORTEM:
                generatePremortemFromSelectedNode();
                break;
            case EVIDENCE_REVIEW:
                generateEvidenceReviewFromSelectedNode();
                break;
            case RETRIEVAL_PRACTICE:
                generateRetrievalPracticeFromSelectedNode();
                break;
            case CONCEPT_DEEPENING:
                generateConceptDeepeningFromSelectedNode();
                break;
            case TRANSFER_PRACTICE:
                generateTransferPracticeFromSelectedNode();
                break;
            default:
                Toast.makeText(this, "暂不支持该模板", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void runAiGapCheck() {
        generateAiGapCheckFromSelectedNode();
    }

    public void runAiExecutionPatch() {
        generateAiExecutionPatchFromSelectedNode();
    }

    public void runAiLearningPatch() {
        generateAiLearningPatchFromSelectedNode();
    }

    public void showAiAssistantDialog() {
        if (isFinishing() || isDestroyed()) return;
        if (getSupportFragmentManager().findFragmentByTag("ai_assistant_dialog") != null) return;
        AiAssistantDialog.newInstance().show(getSupportFragmentManager(), "ai_assistant_dialog");
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

    public void openDecisionLab(Node node) {
        if (node == null) return;
        mindMapView.selectOnlyNode(node.getId());
        openDecisionMatrix();
    }

    public void openFocusSession(Node node) {
        if (node == null) return;
        mindMapView.selectOnlyNode(node.getId());
        openFocusSession();
    }

    public void openGraphIntelligence() {
        openGraphInsights();
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

    // =========================
    // 公开的 Inbox Clarify 和 Weekly Review 方法
    // =========================

    public void openInboxClarifier() {
        InboxClarifierDialog.show(this, mindMapView.getNodesInternal(), new InboxClarifierDialog.Callback() {
            @Override
            public void onNodeConverted(Node node) {
                onNodeUpdated(node);
                mindMapView.requestRender();
                scheduleAutoSave();
            }

            @Override
            public void onBatchFinished() {
                mindMapView.requestRender();
                scheduleAutoSave();
            }
        });
    }

    public void openWeeklyReview() {
        WeeklyReviewDialog.show(
                this,
                mindMapView.getNodesInternal(),
                mindMapView.getConnectionsInternal()
        );
    }

    public void openDecisionMatrix() {
        Node baseNode = getSingleSelectedNode();
        if (baseNode == null) {
            Toast.makeText(this, "请先选中一个决策/项目/目标节点", Toast.LENGTH_SHORT).show();
            return;
        }
        DecisionMatrixDialog.newInstance(baseNode, mindMapView.getNodesInternal(), mindMapView.getConnectionsInternal(), new Runnable() {
            @Override public void run() { mindMapView.requestRender(); scheduleAutoSave(); }
        }).show(getSupportFragmentManager(), "decision_matrix_dialog");
    }

    public void openMemoryReview() {
        MemoryReviewDialog.newInstance(mindMapView.getNodesInternal(), new Runnable() {
            @Override public void run() { mindMapView.requestRender(); scheduleAutoSave(); }
        }).show(getSupportFragmentManager(), "memory_review_dialog");
    }

    public void openFocusSession() {
        Node baseNode = getSingleSelectedNode();
        FocusSessionDialog.newInstance(baseNode, mindMapView.getNodesInternal(), new Runnable() {
            @Override public void run() { mindMapView.requestRender(); scheduleAutoSave(); }
        }).show(getSupportFragmentManager(), "focus_session_dialog");
    }

    public void openGraphInsights() {
        Node baseNode = getSingleSelectedNode();
        GraphInsightDialog.newInstance(baseNode, mindMapView.getNodesInternal(), mindMapView.getConnectionsInternal())
                .show(getSupportFragmentManager(), "graph_insight_dialog");
    }

    // =========================
    // 新增的工作流和快捷操作方法
    // =========================

    public void openProjectsHubWorkflowView() {
        ProjectsHubDialog.newInstance()
                .show(getSupportFragmentManager(), "projects_hub");
    }

    public void openScientificDashboard() {
        ScientificDashboardDialog.newInstance()
                .show(getSupportFragmentManager(), "scientific_dashboard");
    }

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
            case WOOP:
                applyTemplateResult(ScientificTemplateEngine.generateWoop(node, mindMapView.getNodesInternal()));
                break;
            case IF_THEN:
                applyTemplateResult(ScientificTemplateEngine.generateIfThen(node, mindMapView.getNodesInternal()));
                break;
            case DAILY_REVIEW:
                applyTemplateResult(ScientificTemplateEngine.generateDailyReview(node, mindMapView.getNodesInternal()));
                break;
            case WEEKLY_REVIEW:
                applyTemplateResult(ScientificTemplateEngine.generateWeeklyReview(node, mindMapView.getNodesInternal()));
                break;
            case AAR:
                applyTemplateResult(ScientificTemplateEngine.generateAarReview(node, mindMapView.getNodesInternal()));
                break;
            case DECISION_TREE:
                applyTemplateResult(ScientificTemplateEngine.generateDecisionTree(node, mindMapView.getNodesInternal()));
                break;
            case PREMORTEM:
                applyTemplateResult(ScientificTemplateEngine.generatePremortem(node, mindMapView.getNodesInternal()));
                break;
            case EVIDENCE_REVIEW:
                applyTemplateResult(ScientificTemplateEngine.generateEvidenceReview(node, mindMapView.getNodesInternal()));
                break;
            case RETRIEVAL_PRACTICE:
                applyTemplateResult(ScientificTemplateEngine.generateRetrievalPractice(node, mindMapView.getNodesInternal()));
                break;
            case CONCEPT_DEEPENING:
                applyTemplateResult(ScientificTemplateEngine.generateConceptDeepening(node, mindMapView.getNodesInternal()));
                break;
            case TRANSFER_PRACTICE:
                applyTemplateResult(ScientificTemplateEngine.generateTransferPractice(node, mindMapView.getNodesInternal()));
                break;
            default:
                break;
        }

        mindMapView.focusNodeById(node.getId());
        mindMapView.requestRender();
        scheduleAutoSave();
    }

    public void editNodeFromQuickAction(Node node) {
        if (node == null) return;
        showNodeEditDialog(node);
    }

    public void deleteNodeFromQuickAction(Node node) {
        if (node == null) return;

        new AlertDialog.Builder(this)
                .setTitle("删除节点")
                .setMessage("确定删除“" + safeTitle(node) + "”？")
                .setPositiveButton("删除", (dialog, which) -> {
                    mindMapView.removeNode(node.getId());
                    mindMapView.requestRender();
                    scheduleAutoSave();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    public void openInboxClarifierForSingleNode(Node node) {
        if (node == null) return;

        final Node.NodeType[] choices = new Node.NodeType[] {
                Node.NodeType.TASK,
                Node.NodeType.ACTION,
                Node.NodeType.PROJECT,
                Node.NodeType.IDEA,
                Node.NodeType.CONCEPT,
                Node.NodeType.QUESTION,
                Node.NodeType.RESOURCE,
                Node.NodeType.DECISION,
                Node.NodeType.NOTE
        };

        String[] labels = new String[] {
                "任务", "动作", "项目", "想法", "概念", "问题", "资源", "决策", "笔记"
        };

        new AlertDialog.Builder(this)
                .setTitle("澄清分类")
                .setItems(labels, (dialog, which) -> {
                    Node.NodeType targetType = choices[which];
                    node.setType(targetType);
                    WorkflowEngine.normalizeNodeForWorkflow(node);

                    if (targetType == Node.NodeType.PROJECT) {
                        node.setProjectId(node.getId());
                        node.addTags("Project", "InboxConverted");
                    } else if (targetType == Node.NodeType.TASK
                            || targetType == Node.NodeType.ACTION) {
                        node.addTags("Actionable", "InboxConverted");
                    } else if (targetType == Node.NodeType.DECISION) {
                        node.addTags("Decision", "InboxConverted");
                    } else if (targetType == Node.NodeType.CONCEPT
                            || targetType == Node.NodeType.QUESTION
                            || targetType == Node.NodeType.RESOURCE
                            || targetType == Node.NodeType.NOTE) {
                        node.addTags("Learning", "InboxConverted");
                    } else {
                        node.addTag("InboxConverted");
                    }

                    onNodeUpdated(node);
                    mindMapView.requestRender();
                    scheduleAutoSave();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private String safeTitle(Node node) {
        if (node == null) return "(空节点)";
        String title = node.getTitle();
        if (title == null || title.trim().isEmpty()) return "(无标题)";
        return title.trim();
    }

    // =========================
    // 原有模板生成逻辑
    // =========================

    private void generateWoopFromSelectedNode() {
        Node baseNode = getSingleSelectedNode();
        if (baseNode == null) {
            Toast.makeText(this, "请先单击选中一个目标/任务/项目节点", Toast.LENGTH_SHORT).show();
            return;
        }
        applyTemplateResult(ScientificTemplateEngine.generateWoop(baseNode, mindMapView.getNodesInternal()));
        mindMapView.focusNodeById(baseNode.getId());
        Toast.makeText(this, "已生成 WOOP 子图", Toast.LENGTH_SHORT).show();
    }

    private void generateIfThenFromSelectedNode() {
        Node baseNode = getSingleSelectedNode();
        if (baseNode == null) {
            Toast.makeText(this, "请先单击选中一个任务/行动/目标节点", Toast.LENGTH_SHORT).show();
            return;
        }
        applyTemplateResult(ScientificTemplateEngine.generateIfThen(baseNode, mindMapView.getNodesInternal()));
        mindMapView.focusNodeById(baseNode.getId());
        Toast.makeText(this, "已生成 If-Then 子图", Toast.LENGTH_SHORT).show();
    }

    private void generateDailyReviewFromSelectedNode() {
        Node baseNode = getSingleSelectedNode();
        if (baseNode == null) {
            Toast.makeText(this, "请先单击选中一个要复盘的节点", Toast.LENGTH_SHORT).show();
            return;
        }
        applyTemplateResult(ScientificTemplateEngine.generateDailyReview(baseNode, mindMapView.getNodesInternal()));
        mindMapView.focusNodeById(baseNode.getId());
        Toast.makeText(this, "已生成每日复盘子图", Toast.LENGTH_SHORT).show();
    }

    private void generateWeeklyReviewFromSelectedNode() {
        Node baseNode = getSingleSelectedNode();
        if (baseNode == null) {
            Toast.makeText(this, "请先单击选中一个要周复盘的节点", Toast.LENGTH_SHORT).show();
            return;
        }
        applyTemplateResult(ScientificTemplateEngine.generateWeeklyReview(baseNode, mindMapView.getNodesInternal()));
        mindMapView.focusNodeById(baseNode.getId());
        Toast.makeText(this, "已生成每周复盘子图", Toast.LENGTH_SHORT).show();
    }

    private void generateAarFromSelectedNode() {
        Node baseNode = getSingleSelectedNode();
        if (baseNode == null) {
            Toast.makeText(this, "请先单击选中一个要做AAR的节点", Toast.LENGTH_SHORT).show();
            return;
        }
        applyTemplateResult(ScientificTemplateEngine.generateAarReview(baseNode, mindMapView.getNodesInternal()));
        mindMapView.focusNodeById(baseNode.getId());
        Toast.makeText(this, "已生成AAR复盘子图", Toast.LENGTH_SHORT).show();
    }

    private void generateDecisionTreeFromSelectedNode() {
        Node baseNode = getSingleSelectedNode();
        if (baseNode == null) {
            Toast.makeText(this, "请先单击选中一个要分析的节点", Toast.LENGTH_SHORT).show();
            return;
        }
        applyTemplateResult(ScientificTemplateEngine.generateDecisionTree(baseNode, mindMapView.getNodesInternal()));
        mindMapView.focusNodeById(baseNode.getId());
        Toast.makeText(this, "已生成科学决策子图", Toast.LENGTH_SHORT).show();
    }

    private void generatePremortemFromSelectedNode() {
        Node baseNode = getSingleSelectedNode();
        if (baseNode == null) {
            Toast.makeText(this, "请先单击选中一个项目/任务/决策节点", Toast.LENGTH_SHORT).show();
            return;
        }
        applyTemplateResult(ScientificTemplateEngine.generatePremortem(baseNode, mindMapView.getNodesInternal()));
        mindMapView.focusNodeById(baseNode.getId());
        Toast.makeText(this, "已生成 Premortem 子图", Toast.LENGTH_SHORT).show();
    }

    private void generateEvidenceReviewFromSelectedNode() {
        Node baseNode = getSingleSelectedNode();
        if (baseNode == null) {
            Toast.makeText(this, "请先单击选中一个要做证据审查的节点", Toast.LENGTH_SHORT).show();
            return;
        }
        applyTemplateResult(ScientificTemplateEngine.generateEvidenceReview(baseNode, mindMapView.getNodesInternal()));
        mindMapView.focusNodeById(baseNode.getId());
        Toast.makeText(this, "已生成证据评估子图", Toast.LENGTH_SHORT).show();
    }

    private void generateRetrievalPracticeFromSelectedNode() {
        Node baseNode = getSingleSelectedNode();
        if (baseNode == null) {
            Toast.makeText(this, "请先单击选中一个知识节点", Toast.LENGTH_SHORT).show();
            return;
        }
        applyTemplateResult(ScientificTemplateEngine.generateRetrievalPractice(baseNode, mindMapView.getNodesInternal()));
        mindMapView.focusNodeById(baseNode.getId());
        Toast.makeText(this, "已生成检索练习子图", Toast.LENGTH_SHORT).show();
    }

    private void generateConceptDeepeningFromSelectedNode() {
        Node baseNode = getSingleSelectedNode();
        if (baseNode == null) {
            Toast.makeText(this, "请先单击选中一个概念节点", Toast.LENGTH_SHORT).show();
            return;
        }
        applyTemplateResult(ScientificTemplateEngine.generateConceptDeepening(baseNode, mindMapView.getNodesInternal()));
        mindMapView.focusNodeById(baseNode.getId());
        Toast.makeText(this, "已生成概念深化子图", Toast.LENGTH_SHORT).show();
    }

    private void generateTransferPracticeFromSelectedNode() {
        Node baseNode = getSingleSelectedNode();
        if (baseNode == null) {
            Toast.makeText(this, "请先单击选中一个知识/方法节点", Toast.LENGTH_SHORT).show();
            return;
        }
        applyTemplateResult(ScientificTemplateEngine.generateTransferPractice(baseNode, mindMapView.getNodesInternal()));
        mindMapView.focusNodeById(baseNode.getId());
        Toast.makeText(this, "已生成应用迁移子图", Toast.LENGTH_SHORT).show();
    }

    private void generateAiGapCheckFromSelectedNode() {
        Node baseNode = getSingleSelectedNode();
        if (baseNode == null) {
            Toast.makeText(this, "请先单击选中一个节点", Toast.LENGTH_SHORT).show();
            return;
        }
        applyTemplateResult(ScientificTemplateEngine.generateAiGapCheck(baseNode, mindMapView.getNodesInternal()));
        mindMapView.focusNodeById(baseNode.getId());
        Toast.makeText(this, "已生成 AI缺口检查 子图", Toast.LENGTH_SHORT).show();
    }

    private void generateAiExecutionPatchFromSelectedNode() {
        Node baseNode = getSingleSelectedNode();
        if (baseNode == null) {
            Toast.makeText(this, "请先单击选中一个执行相关节点", Toast.LENGTH_SHORT).show();
            return;
        }
        applyTemplateResult(ScientificTemplateEngine.generateAiExecutionPatch(baseNode, mindMapView.getNodesInternal()));
        mindMapView.focusNodeById(baseNode.getId());
        Toast.makeText(this, "已生成 AI执行补全 子图", Toast.LENGTH_SHORT).show();
    }

    private void generateAiLearningPatchFromSelectedNode() {
        Node baseNode = getSingleSelectedNode();
        if (baseNode == null) {
            Toast.makeText(this, "请先单击选中一个学习相关节点", Toast.LENGTH_SHORT).show();
            return;
        }
        applyTemplateResult(ScientificTemplateEngine.generateAiLearningPatch(baseNode, mindMapView.getNodesInternal()));
        mindMapView.focusNodeById(baseNode.getId());
        Toast.makeText(this, "已生成 AI学习补全 子图", Toast.LENGTH_SHORT).show();
    }

    private Node getSingleSelectedNode() {
        List<String> selectedIds = mindMapView.getSelectedNodeIds();
        if (selectedIds == null || selectedIds.isEmpty()) return null;
        String firstId = selectedIds.get(0);
        return mindMapView.getNodesInternal().get(firstId);
    }

    private void applyTemplateResult(ScientificTemplateEngine.TemplateResult result) {
        if (result == null) return;

        Node baseNode = getSingleSelectedNode();
        TemplatePostProcessor.postProcess(baseNode, result, mindMapView.getNodesInternal());

        for (Node node : result.createdNodes) {
            mindMapView.addNode(node);
        }

        for (Connection connection : result.createdConnections) {
            mindMapView.addConnection(connection);
        }

        mindMapView.requestRender();
        scheduleAutoSave();
    }

    public void showNodeEditDialog(Node node) {
        DialogFragment dialog = NodeEditDialog.newInstance(node, mindMapView);
        dialog.show(getSupportFragmentManager(), "node_edit_dialog");
    }

    private void scheduleAutoSave() {
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        autoSaveHandler.postDelayed(autoSaveRunnable, 550);
    }

    private void saveCurrentDataSilently() {
        try {
            final Map<String, Node> nodes = mindMapView.getNodes();
            final Map<String, Connection> connections = mindMapView.getConnections();
            ioExecutor.execute(() -> {
                try {
                    dataManager.saveMindMap(nodes, connections);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNodeUpdated(Node node) {
        mindMapView.requestRender();
        scheduleAutoSave();
        maybeToast("节点已更新");
    }

    @Override
    public void onNodeDeleted(Node node) {
        scheduleAutoSave();
        maybeToast("节点已删除");
    }

    @Override
    public void onSearch(String keyword, List<Node.NodeType> types, boolean highlight) {
        mindMapView.search(keyword, types, highlight);
        int resultCount = mindMapView.getSearchResultCount();
        if (resultCount > 0) {
            maybeToast("找到 " + resultCount + " 个匹配节点");
        } else {
            maybeToast("未找到匹配节点");
        }
    }

    @Override
    public void onClearSearch() {
        mindMapView.clearSearch();
        maybeToast("搜索已清除");
    }

    @Override
    public void onDataChanged() {
        scheduleAutoSave();
    }

    @Override
    protected void onPause() {
        super.onPause();
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        saveCurrentDataSilently();
    }

    @Override
    protected void onDestroy() {
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        ioExecutor.shutdown();
        super.onDestroy();
    }

    private void maybeToast(String message) {
        long now = System.currentTimeMillis();
        if (now - lastToastAt < 500L) return;
        lastToastAt = now;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

