package com.agui.neuralcanvas;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
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

    private void loadSavedData() {
        try {
            Map<?, ?> savedData = dataManager.loadMindMap();
            Map<String, Node> nodes = (Map<String, Node>) savedData.get("nodes");
            Map<String, Connection> connections = (Map<String, Connection>) savedData.get("connections");

            if (nodes != null && !nodes.isEmpty()) {
                mindMapView.setNodes(nodes);
                mindMapView.setConnections(connections != null ? connections : new HashMap<>());
                Toast.makeText(this, "已加载保存的数据", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "没有保存的数据，开始新的思维地图", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "加载数据失败，开始新的思维地图", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add_node) {
            Node newNode = new Node("新节点", "输入内容", -110f, -65f, Node.NodeType.CONCEPT);
            mindMapView.addNode(newNode);
            showNodeEditDialog(newNode);
            return true;
        } else if (id == R.id.action_search) {
            showSearchDialog();
            return true;
        } else if (id == R.id.action_clear_all) {
            confirmClearAll();
            return true;
        } else if (id == R.id.action_help) {
            showHelpDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void confirmClearAll() {
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

    private void showHelpDialog() {
        TextView tv = new TextView(this);
        int p = 42;
        tv.setPadding(p, p, p, p);
        tv.setTextSize(15f);
        tv.setTextColor(0xFF0F172A);
        tv.setMovementMethod(new ScrollingMovementMethod());
        tv.setText(
                "NeuralCanvas 功能说明\n\n" +
                "1. 画布操作\n" +
                "• 双击空白处：在点击位置新建节点\n" +
                "• 单指拖拽空白处：移动整张画布\n" +
                "• 双指捏合：自由缩放画布\n\n" +
                "2. 节点操作\n" +
                "• 单击节点：弹出只读内容预览卡片，再点一次收起\n" +
                "• 拖拽节点：移动节点位置\n" +
                "• 长按节点：打开节点编辑弹窗\n\n" +
                "3. 节点编辑\n" +
                "• 可修改标题、内容、节点类型\n" +
                "• 可修改节点形状：正方形、圆形、椭圆、菱形、三角形、五边形、六边形\n" +
                "• 可从编辑弹窗进入“创建连线模式”\n" +
                "• 可删除当前节点\n\n" +
                "4. 连线操作\n" +
                "• 进入创建连线模式后，点击目标节点即可建立连线\n" +
                "• 建立连线时可设置连线文字、颜色、粗细\n" +
                "• 箭头会自动指向目标节点边缘\n" +
                "• 连线粗细会随缩放自适应，缩小时也尽量保留粗细差异\n\n" +
                "5. 搜索\n" +
                "• 顶部搜索可按关键词/类型筛选节点\n" +
                "• 搜索后会自动定位到第一个匹配节点\n" +
                "• 可高亮显示搜索结果\n\n" +
                "6. 保存\n" +
                "• 拖动、编辑、加点、删点、改线后自动保存\n" +
                "• 退出到后台时会再保存一次\n" +
                "• 重新打开 app 会自动恢复上次数据\n\n" +
                "7. 菜单\n" +
                "• 新建节点：手动添加新节点\n" +
                "• 搜索：打开搜索窗口\n" +
                "• 清除全部：删除全部节点和连线\n" +
                "• 帮助：显示本说明"
        );

        new AlertDialog.Builder(this)
                .setTitle("帮助")
                .setView(tv)
                .setPositiveButton("知道了", null)
                .show();
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

    private void showSearchDialog() {
        DialogFragment dialog = SearchDialog.newInstance(mindMapView);
        dialog.show(getSupportFragmentManager(), "search_dialog");
    }

    @Override
    protected void onPause() {
        super.onPause();
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        saveCurrentDataSilently();
    }
}
