package com.agui.neuralcanvas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MindMapView extends View {
    
// 数据
    private Map<String, Node> nodes = new HashMap<>();
    private Map<String, Connection> connections = new HashMap<>();
    
    // 搜索状态
    private String searchKeyword = "";
    private List<Node.NodeType> searchTypes = new ArrayList<>();
    private boolean highlightSearchResults = false;
    private List<String> searchResultNodeIds = new ArrayList<>();
    
    // 视图状态
    private float scale = 1.0f;
    private float offsetX = 0;
    private float offsetY = 0;
    private float lastTouchX, lastTouchY;
    private boolean isDragging = false;
    private boolean isScaling = false;
    
    // 交互状态
    private Node selectedNode = null;
    private Connection selectedConnection = null;
    private Node draggingNode = null;
    private boolean creatingConnection = false;
    private Node connectionStartNode = null;
    private float connectionEndX, connectionEndY;
    
    // 连接创建模式
    private boolean connectionMode = false;
    private Node connectionSourceNode = null;
    
    // 手势检测
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    
    // 绘制相关
    private Paint gridPaint;
    private Paint connectionPreviewPaint;
    
    public MindMapView(Context context) {
        super(context);
        init();
    }
    
    public MindMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public MindMapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }
    
    private void init() {
        setBackgroundColor(Color.parseColor("#1E1E1E"));
        
        // 初始化手势检测
        gestureDetector = new GestureDetector(getContext(), new GestureListener());
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        
        // 初始化绘制工具
        initPaints();
        
        // 添加一些示例数据
        addSampleData();
    }
    
    private void initPaints() {
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#333333"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1);
        
        connectionPreviewPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        connectionPreviewPaint.setColor(Color.WHITE);
        connectionPreviewPaint.setStyle(Paint.Style.STROKE);
        connectionPreviewPaint.setStrokeWidth(3);
        connectionPreviewPaint.setAlpha(150);
    }
    
    private void addSampleData() {
        // 添加中心节点
        Node centerNode = new Node("NeuralCanvas", "思维地图应用", Node.NodeType.IDEA, 0, 0);
        addNode(centerNode);
        
        // 添加一些示例节点
        Node goalNode = new Node("学习目标", "掌握Android开发", Node.NodeType.GOAL, -300, -200);
        Node taskNode = new Node("开发任务", "实现节点拖拽功能", Node.NodeType.TASK, 300, -200);
        Node noteNode = new Node("设计思路", "无限画布+自由连接", Node.NodeType.NOTE, 0, 200);
        
        addNode(goalNode);
        addNode(taskNode);
        addNode(noteNode);
        
        // 添加连接
        Connection conn1 = new Connection(centerNode.getId(), goalNode.getId(), 
                                         Connection.ConnectionType.SEQUENCE, "目标设定");
        Connection conn2 = new Connection(centerNode.getId(), taskNode.getId(), 
                                         Connection.ConnectionType.PARALLEL, "并行开发");
        Connection conn3 = new Connection(centerNode.getId(), noteNode.getId(), 
                                         Connection.ConnectionType.REFERENCE, "设计参考");
        
        addConnection(conn1);
        addConnection(conn2);
        addConnection(conn3);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 绘制网格
        drawGrid(canvas);
        
        // 绘制所有连接
        for (Connection connection : connections.values()) {
            Node fromNode = nodes.get(connection.getFromNodeId());
            Node toNode = nodes.get(connection.getToNodeId());
            connection.draw(canvas, fromNode, toNode, scale, offsetX, offsetY);
        }
        
// 绘制正在创建的连接预览
        if (creatingConnection && connectionStartNode != null) {
            float startX = (connectionStartNode.getX() + offsetX + connectionStartNode.getWidth() / 2) * scale;
            float startY = (connectionStartNode.getY() + offsetY + connectionStartNode.getHeight() / 2) * scale;
            canvas.drawLine(startX, startY, connectionEndX, connectionEndY, connectionPreviewPaint);
        }
        
        // 绘制连接模式下的预览
        if (connectionMode && connectionSourceNode != null) {
            float startX = (connectionSourceNode.getX() + offsetX + connectionSourceNode.getWidth() / 2) * scale;
            float startY = (connectionSourceNode.getY() + offsetY + connectionSourceNode.getHeight() / 2) * scale;
            
            // 绘制虚线预览
            Paint dashedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dashedPaint.setColor(Color.YELLOW);
            dashedPaint.setStyle(Paint.Style.STROKE);
            dashedPaint.setStrokeWidth(3);
            dashedPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{10, 10}, 0));
            
            canvas.drawLine(startX, startY, connectionEndX, connectionEndY, dashedPaint);
            
            // 绘制提示文字
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.YELLOW);
            textPaint.setTextSize(24);
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("拖拽到目标节点创建连接", getWidth() / 2, 50, textPaint);
        }
        
// 绘制所有节点
        for (Node node : nodes.values()) {
            boolean isSearchResult = searchResultNodeIds.contains(node.getId());
            node.draw(canvas, scale, offsetX, offsetY, isSearchResult, highlightSearchResults);
        }
        
        // 绘制搜索结果计数
        if (!searchResultNodeIds.isEmpty()) {
            Paint countPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            countPaint.setColor(Color.YELLOW);
            countPaint.setTextSize(28);
            countPaint.setTextAlign(Paint.Align.RIGHT);
            String countText = "找到 " + searchResultNodeIds.size() + " 个结果";
            canvas.drawText(countText, getWidth() - 20, 50, countPaint);
        }
    }
    
    private void drawGrid(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        float gridSize = 50 * scale;
        
        // 计算网格起始位置
        float startX = offsetX * scale % gridSize;
        float startY = offsetY * scale % gridSize;
        
        // 绘制垂直线
        for (float x = startX; x < width; x += gridSize) {
            canvas.drawLine(x, 0, x, height, gridPaint);
        }
        
        // 绘制水平线
        for (float y = startY; y < height; y += gridSize) {
            canvas.drawLine(0, y, width, y, gridPaint);
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = scaleGestureDetector.onTouchEvent(event);
        handled = gestureDetector.onTouchEvent(event) || handled;
        
        float x = event.getX();
        float y = event.getY();
        
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;
                
                // 检查是否点击了节点
                Node touchedNode = findNodeAt(x, y);
                if (touchedNode != null) {
                    if (creatingConnection && connectionStartNode != null && 
                        !touchedNode.getId().equals(connectionStartNode.getId())) {
                        // 完成连接创建
                        Connection newConnection = new Connection(
                            connectionStartNode.getId(), 
                            touchedNode.getId(),
                            Connection.ConnectionType.SEQUENCE,
                            "新连接"
                        );
                        addConnection(newConnection);
                        creatingConnection = false;
                        connectionStartNode = null;
                        invalidate();
                    } else {
                        // 选择节点
                        selectNode(touchedNode);
                        draggingNode = touchedNode;
                        draggingNode.setDragging(true);
                        
                        // 开始拖拽创建连接（长按后拖拽）
                        // 这里可以添加连接创建模式
                    }
                } else {
                    // 检查是否点击了连接
                    Connection touchedConnection = findConnectionAt(x, y, 20);
                    if (touchedConnection != null) {
                        selectConnection(touchedConnection);
                    } else {
                        // 开始画布拖拽
                        isDragging = true;
                        clearSelection();
                    }
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                float dx = (x - lastTouchX) / scale;
                float dy = (y - lastTouchY) / scale;
                
                if (isDragging && !isScaling) {
                    // 拖拽画布
                    offsetX += dx;
                    offsetY += dy;
                    invalidate();
                } else if (draggingNode != null) {
                    // 拖拽节点
                    draggingNode.move(dx, dy);
                    invalidate();
                    
                    // 如果在连接模式下拖拽节点，更新连接预览
                    if (connectionMode && connectionSourceNode != null && 
                        !draggingNode.getId().equals(connectionSourceNode.getId())) {
                        connectionEndX = x;
                        connectionEndY = y;
                        invalidate();
                    }
                } else if (creatingConnection && connectionStartNode != null) {
                    // 更新连接预览
                    connectionEndX = x;
                    connectionEndY = y;
                    invalidate();
                } else if (connectionMode && connectionSourceNode != null) {
                    // 连接模式下移动，更新连接预览
                    connectionEndX = x;
                    connectionEndY = y;
                    invalidate();
                }
                
                lastTouchX = x;
                lastTouchY = y;
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                if (draggingNode != null) {
                    draggingNode.setDragging(false);
                    draggingNode = null;
                }
                break;
        }
        
        return true;
    }
    
    private Node findNodeAt(float x, float y) {
        for (Node node : nodes.values()) {
            if (node.contains(x, y, scale, offsetX, offsetY)) {
                return node;
            }
        }
        return null;
    }
    
    private Connection findConnectionAt(float x, float y, float tolerance) {
        for (Connection connection : connections.values()) {
            Node fromNode = nodes.get(connection.getFromNodeId());
            Node toNode = nodes.get(connection.getToNodeId());
            if (connection.isNear(x, y, fromNode, toNode, scale, offsetX, offsetY, tolerance)) {
                return connection;
            }
        }
        return null;
    }
    
    private void selectNode(Node node) {
        clearSelection();
        selectedNode = node;
        selectedNode.setSelected(true);
        invalidate();
    }
    
    private void selectConnection(Connection connection) {
        clearSelection();
        selectedConnection = connection;
        selectedConnection.setSelected(true);
        invalidate();
    }
    
    private void clearSelection() {
        if (selectedNode != null) {
            selectedNode.setSelected(false);
            selectedNode = null;
        }
        if (selectedConnection != null) {
            selectedConnection.setSelected(false);
            selectedConnection = null;
        }
    }
    
    // 公开方法
    public void addNode(Node node) {
        nodes.put(node.getId(), node);
        invalidate();
    }
    
    public void removeNode(String nodeId) {
        Node node = nodes.get(nodeId);
        if (node != null) {
            // 移除相关的连接
            List<String> connectionsToRemove = new ArrayList<>();
            for (Connection connection : connections.values()) {
                if (connection.getFromNodeId().equals(nodeId) || 
                    connection.getToNodeId().equals(nodeId)) {
                    connectionsToRemove.add(connection.getId());
                }
            }
            for (String connId : connectionsToRemove) {
                connections.remove(connId);
            }
            
            nodes.remove(nodeId);
            invalidate();
        }
    }
    
    public void addConnection(Connection connection) {
        connections.put(connection.getId(), connection);
        
        // 更新节点的连接引用
        Node fromNode = nodes.get(connection.getFromNodeId());
        Node toNode = nodes.get(connection.getToNodeId());
        if (fromNode != null) fromNode.addConnection(connection.getId());
        if (toNode != null) toNode.addConnection(connection.getId());
        
        invalidate();
    }
    
    public void removeConnection(String connectionId) {
        Connection connection = connections.get(connectionId);
        if (connection != null) {
            // 更新节点的连接引用
            Node fromNode = nodes.get(connection.getFromNodeId());
            Node toNode = nodes.get(connection.getToNodeId());
            if (fromNode != null) fromNode.removeConnection(connectionId);
            if (toNode != null) toNode.removeConnection(connectionId);
            
            connections.remove(connectionId);
            invalidate();
        }
    }
    
public void startCreatingConnection(Node fromNode) {
        creatingConnection = true;
        connectionStartNode = fromNode;
    }
    
    public void cancelCreatingConnection() {
        creatingConnection = false;
        connectionStartNode = null;
        invalidate();
    }
    
    // 连接模式方法
    public void startConnectionMode(Node sourceNode) {
        connectionMode = true;
        connectionSourceNode = sourceNode;
        connectionEndX = (sourceNode.getX() + offsetX + sourceNode.getWidth() / 2) * scale;
        connectionEndY = (sourceNode.getY() + offsetY + sourceNode.getHeight() / 2) * scale;
        invalidate();
    }
    
    public void completeConnection(Node targetNode) {
        if (connectionMode && connectionSourceNode != null && targetNode != null &&
            !connectionSourceNode.getId().equals(targetNode.getId())) {
            
            // 检查是否已存在相同连接
            boolean connectionExists = false;
            for (Connection conn : connections.values()) {
                if ((conn.getFromNodeId().equals(connectionSourceNode.getId()) && 
                     conn.getToNodeId().equals(targetNode.getId())) ||
                    (conn.getFromNodeId().equals(targetNode.getId()) && 
                     conn.getToNodeId().equals(connectionSourceNode.getId()))) {
                    connectionExists = true;
                    break;
                }
            }
            
            if (!connectionExists) {
                Connection newConnection = new Connection(
                    connectionSourceNode.getId(),
                    targetNode.getId(),
                    Connection.ConnectionType.SEQUENCE,
                    "连接"
                );
                addConnection(newConnection);
            }
        }
        
        cancelConnectionMode();
    }
    
    public void cancelConnectionMode() {
        connectionMode = false;
        connectionSourceNode = null;
        invalidate();
    }
    
public boolean isInConnectionMode() {
        return connectionMode;
    }
    
    // 搜索方法
    public void search(String keyword, List<Node.NodeType> types, boolean highlight) {
        searchKeyword = keyword.toLowerCase();
        searchTypes = types;
        highlightSearchResults = highlight;
        searchResultNodeIds.clear();
        
        if (keyword.isEmpty() && types.isEmpty()) {
            // 清空搜索
            clearSearch();
            return;
        }
        
        for (Node node : nodes.values()) {
            boolean matches = false;
            
            // 检查类型
            if (!types.isEmpty() && !types.contains(node.getType())) {
                continue;
            }
            
            // 检查关键词
            if (!keyword.isEmpty()) {
                String title = node.getTitle().toLowerCase();
                String content = node.getContent().toLowerCase();
                if (title.contains(searchKeyword) || content.contains(searchKeyword)) {
                    matches = true;
                }
            } else {
                matches = true; // 只有类型筛选
            }
            
            if (matches) {
                searchResultNodeIds.add(node.getId());
            }
        }
        
        invalidate();
    }
    
    public void clearSearch() {
        searchKeyword = "";
        searchTypes.clear();
        highlightSearchResults = false;
        searchResultNodeIds.clear();
        invalidate();
    }
    
    public List<String> getSearchResults() {
        return new ArrayList<>(searchResultNodeIds);
    }
    
    public int getSearchResultCount() {
        return searchResultNodeIds.size();
    }
    
    public void resetView() {
        scale = 1.0f;
        offsetX = 0;
        offsetY = 0;
        invalidate();
    }
    
    public void zoomIn() {
        scale *= 1.2f;
        invalidate();
    }
    
    public void zoomOut() {
        scale /= 1.2f;
        invalidate();
    }
    
    // 手势监听器
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // 双击创建新节点
            float worldX = (e.getX() / scale) - offsetX;
            float worldY = (e.getY() / scale) - offsetY;
            
            final Node newNode = new Node("新节点", "双击编辑内容", Node.NodeType.NOTE, worldX, worldY);
            addNode(newNode);
            selectNode(newNode);
            
            // 延迟触发编辑对话框，确保UI线程安全
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (getContext() instanceof MainActivity) {
                        MainActivity activity = (MainActivity) getContext();
                        activity.showNodeEditDialog(newNode);
                    }
                }
            }, 100);
            
            return true;
        }
        
@Override
        public void onLongPress(MotionEvent e) {
            // 长按显示编辑对话框
            final Node touchedNode = findNodeAt(e.getX(), e.getY());
            if (touchedNode != null) {
                selectNode(touchedNode);
                
                // 延迟触发编辑对话框
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (getContext() instanceof MainActivity) {
                            MainActivity activity = (MainActivity) getContext();
                            activity.showNodeEditDialog(touchedNode);
                        }
                    }
                }, 100);
            }
        }
    }
    
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            isScaling = true;
            float scaleFactor = detector.getScaleFactor();
            
            // 应用缩放并限制范围
            scale *= scaleFactor;
            scale = Math.max(0.1f, Math.min(scale, 5.0f));
            
            invalidate();
            return true;
        }
        
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            isScaling = true;
            return true;
        }
        
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            isScaling = false;
        }
    }
    
    // 数据访问方法
    public Map<String, Node> getNodes() {
        return new HashMap<>(nodes);
    }
    
    public Map<String, Connection> getConnections() {
        return new HashMap<>(connections);
    }
    
    public void setNodes(Map<String, Node> nodes) {
        this.nodes.clear();
        if (nodes != null) {
            this.nodes.putAll(nodes);
        }
        invalidate();
    }
    
    public void setConnections(Map<String, Connection> connections) {
        this.connections.clear();
        if (connections != null) {
            this.connections.putAll(connections);
        }
        invalidate();
    }
}