package com.agui.neuralcanvas;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.widget.Toast;

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
    private final Runnable autoSaveRunnable = this::saveCurrentDataSilently;

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
        mindMapView.invalidate();
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
        Toast.makeText(this, "AI 助手功能待继续接入", Toast.LENGTH_SHORT).show();
    }

    public void showKnowledgeImportDialog() {
        Toast.makeText(this, "知识导入功能待继续接入", Toast.LENGTH_SHORT).show();
    }

    public void showHelpDialog() {
        Toast.makeText(this, "帮助页面待继续完善", Toast.LENGTH_SHORT).show();
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
                mindMapView.invalidate();
                scheduleAutoSave();
            }

            @Override
            public void onBatchFinished() {
                mindMapView.invalidate();
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

        mindMapView.invalidate();
        scheduleAutoSave();
    }

    public void showNodeEditDialog(Node node) {
        DialogFragment dialog = NodeEditDialog.newInstance(node, mindMapView);
        dialog.show(getSupportFragmentManager(), "node_edit_dialog");
    }

    private void scheduleAutoSave() {
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        autoSaveHandler.postDelayed(autoSaveRunnable, 700);
    }

    private void saveCurrentDataSilently() {
        try {
            Map<String, Node> nodes = mindMapView.getNodes();
            Map<String, Connection> connections = mindMapView.getConnections();
            dataManager.saveMindMap(nodes, connections);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNodeUpdated(Node node) {
        mindMapView.invalidate();
        scheduleAutoSave();
        Toast.makeText(this, "节点已更新", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNodeDeleted(Node node) {
        scheduleAutoSave();
        Toast.makeText(this, "节点已删除", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSearch(String keyword, List<Node.NodeType> types, boolean highlight) {
        mindMapView.search(keyword, types, highlight);
        int resultCount = mindMapView.getSearchResultCount();
        if (resultCount > 0) {
            Toast.makeText(this, "找到 " + resultCount + " 个匹配节点", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "未找到匹配节点", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClearSearch() {
        mindMapView.clearSearch();
        Toast.makeText(this, "搜索已清除", Toast.LENGTH_SHORT).show();
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
}
