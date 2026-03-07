package com.agui.neuralcanvas;

import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.multidex.MultiDex;
import android.content.Context;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

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
        
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        mindMapView = findViewById(R.id.mindMapView);
        mindMapView.setOnDataChangeListener(this);
        
        // 初始化简单数据管理器
        dataManager = new SimpleDataManager(getApplication());
        
        // 加载保存的数据
        loadSavedData();
        
        // 设置标题
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("NeuralCanvas - 思维地图");
            getSupportActionBar().setSubtitle("双击创建节点，长按编辑");
        }
        
        // 设置长按监听器
        setupLongPressListener();
    }
    
    private void setupLongPressListener() {
        // 这里可以添加长按节点的监听器
        // 由于MindMapView已经处理了长按手势，我们可以在那里触发对话框
    }
    
    private void loadSavedData() {
        try {
            Map<String, Object> savedData = dataManager.loadMindMap();
            Map<String, Node> nodes = (Map<String, Node>) savedData.get("nodes");
            Map<String, Connection> connections = (Map<String, Connection>) savedData.get("connections");
            
            if (nodes != null && !nodes.isEmpty()) {
                mindMapView.setNodes(nodes);
                mindMapView.setConnections(connections != null ? connections : new HashMap<String, Connection>());
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
            // 在中心位置添加新节点
            Node newNode = new Node("新目标", "描述你的目标", Node.NodeType.GOAL, 0, 0);
            mindMapView.addNode(newNode);
            
            // 立即打开编辑对话框
            showNodeEditDialog(newNode);
            
            Toast.makeText(this, "已添加新节点", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_zoom_in) {
            mindMapView.zoomIn();
            return true;
        } else if (id == R.id.action_zoom_out) {
            mindMapView.zoomOut();
            return true;
        } else if (id == R.id.action_reset_view) {
            mindMapView.resetView();
            Toast.makeText(this, "视图已重置", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_clear_all) {
            // 这里可以添加清除所有节点的功能
            Toast.makeText(this, "清除功能待实现", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_search) {
            showSearchDialog();
            return true;
        } else if (id == R.id.action_help) {
            showHelp();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    
private void saveCurrentData() {
        try {
            Map<String, Node> nodes = mindMapView.getNodes();
            Map<String, Connection> connections = mindMapView.getConnections();
            
            if (nodes != null && !nodes.isEmpty()) {
                dataManager.saveMindMap(nodes, connections);
                Toast.makeText(this, "数据已保存", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "没有数据可保存", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "保存数据失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void scheduleAutoSave() {
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        autoSaveHandler.postDelayed(autoSaveRunnable, 800);
    }

    private void saveCurrentDataSilently() {
        try {
            Map<String, Node> nodes = mindMapView.getNodes();
            Map<String, Connection> connections = mindMapView.getConnections();

            if (nodes != null && !nodes.isEmpty()) {
                dataManager.saveMindMap(nodes, connections);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void showHelp() {
        String helpText = "使用指南：\n" +
                         "• 双击画布：创建新节点\n" +
                         "• 拖拽节点：移动位置\n" +
                         "• 双指缩放：调整视图大小\n" +
                         "• 单指拖拽空白处：移动画布\n" +
                         "• 长按节点：显示编辑对话框\n" +
                         "• 点击连接线：选择连接\n" +
                         "• 菜单按钮：添加节点、缩放等操作\n" +
                         "• 数据自动保存到本地数据库";
        
        Toast.makeText(this, helpText, Toast.LENGTH_LONG).show();
    }
    
    public void showNodeEditDialog(Node node) {
        DialogFragment dialog = NodeEditDialog.newInstance(node, mindMapView);
        dialog.show(getSupportFragmentManager(), "node_edit_dialog");
    }
    
    // NodeEditListener 接口实现
    @Override
    public void onNodeUpdated(Node node) {
        Toast.makeText(this, "节点已更新", Toast.LENGTH_SHORT).show();
        mindMapView.invalidate();
        scheduleAutoSave();
    }

    @Override
    public void onNodeDeleted(Node node) {
        Toast.makeText(this, "节点已删除", Toast.LENGTH_SHORT).show();
        scheduleAutoSave();
    }
    
    // SearchListener 接口实现
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
    public void onDataChanged() {
        scheduleAutoSave();
    }
    
    @Override
    public void onClearSearch() {
        mindMapView.clearSearch();
        Toast.makeText(this, "搜索已清除", Toast.LENGTH_SHORT).show();
    }
    @Override
    protected void onPause() {
        super.onPause();
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        saveCurrentDataSilently();
    }
    private void showSearchDialog() {
        DialogFragment dialog = SearchDialog.newInstance(mindMapView);
        dialog.show(getSupportFragmentManager(), "search_dialog");
    }
}
