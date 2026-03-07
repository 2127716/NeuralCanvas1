package com.agui.neuralcanvas;

import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MindMapView extends View {
    public interface OnDataChangeListener {
        void onDataChanged();
    }

    private OnDataChangeListener onDataChangeListener;

    public void setOnDataChangeListener(OnDataChangeListener listener) {
        this.onDataChangeListener = listener;
    }

    private void notifyDataChanged() {
        if (onDataChangeListener != null) {
            onDataChangeListener.onDataChanged();
        }
    }

    // 数据
    private final Map<String, Node> nodes = new HashMap<>();
    private final Map<String, Connection> connections = new HashMap<>();

    // 搜索状态
    private String searchKeyword = "";
    private List<Node.NodeType> searchTypes = new ArrayList<>();
    private boolean highlightSearchResults = false;
    private final List<String> searchResultNodeIds = new ArrayList<>();

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

    // ====== 你要的“链接 / 取消链接”模式 ======
    private enum PendingAction { NONE, CONNECT, DISCONNECT }
    private PendingAction pendingAction = PendingAction.NONE;
    private Node pendingSourceNode = null;
    private float pendingEndX = 0;
    private float pendingEndY = 0;

    // 手势检测
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    // 绘制相关
    private Paint gridPaint;
    private Paint pendingPreviewPaint;
    private Paint pendingTextPaint;

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

        // 示例数据
        addSampleData();
    }

    private void initPaints() {
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#333333"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1);

        pendingPreviewPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pendingPreviewPaint.setColor(Color.YELLOW);
        pendingPreviewPaint.setStyle(Paint.Style.STROKE);
        pendingPreviewPaint.setStrokeWidth(3);
        pendingPreviewPaint.setAlpha(180);

        pendingTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pendingTextPaint.setColor(Color.YELLOW);
        pendingTextPaint.setTextSize(24);
        pendingTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void addSampleData() {
        Node centerNode = new Node("NeuralCanvas", "思维地图应用", Node.NodeType.IDEA, 0, 0);
        addNode(centerNode);

        Node goalNode = new Node("学习目标", "掌握Android开发", Node.NodeType.GOAL, -300, -200);
        Node taskNode = new Node("开发任务", "实现节点拖拽功能", Node.NodeType.TASK, 300, -200);
        Node noteNode = new Node("设计思路", "无限画布+自由连接", Node.NodeType.NOTE, 0, 200);

        addNode(goalNode);
        addNode(taskNode);
        addNode(noteNode);

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

        // 网格
        drawGrid(canvas);

        // 连接
        for (Connection connection : connections.values()) {
            Node fromNode = nodes.get(connection.getFromNodeId());
            Node toNode = nodes.get(connection.getToNodeId());
            connection.draw(canvas, fromNode, toNode, scale, offsetX, offsetY);
        }

        // 链接/取消链接模式预览线 + 提示
        if (pendingAction != PendingAction.NONE && pendingSourceNode != null) {
            float startX = (pendingSourceNode.getX() + offsetX + pendingSourceNode.getWidth() / 2f) * scale;
            float startY = (pendingSourceNode.getY() + offsetY + pendingSourceNode.getHeight() / 2f) * scale;

            canvas.drawLine(startX, startY, pendingEndX, pendingEndY, pendingPreviewPaint);

            String tip = (pendingAction == PendingAction.CONNECT)
                    ? "请选择要链接的另一个节点（点空白取消）"
                    : "请选择要取消链接的另一个节点（点空白取消）";
            canvas.drawText(tip, getWidth() / 2f, 50, pendingTextPaint);
        }

        // 节点
        for (Node node : nodes.values()) {
            boolean isSearchResult = searchResultNodeIds.contains(node.getId());
            node.draw(canvas, scale, offsetX, offsetY, isSearchResult, highlightSearchResults);
        }

        // 搜索结果数量
        if (!searchResultNodeIds.isEmpty()) {
            Paint countPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            countPaint.setColor(Color.YELLOW);
            countPaint.setTextSize(28);
            countPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("找到 " + searchResultNodeIds.size() + " 个结果", getWidth() - 20, 50, countPaint);
        }
    }

    private void drawGrid(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        float gridSize = 50 * scale;

        float startX = offsetX * scale % gridSize;
        float startY = offsetY * scale % gridSize;

        for (float x = startX; x < width; x += gridSize) {
            canvas.drawLine(x, 0, x, height, gridPaint);
        }
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
            case MotionEvent.ACTION_DOWN: {
                lastTouchX = x;
                lastTouchY = y;

                // 如果在链接/取消链接模式，预览线终点初始化为当前点
                if (pendingAction != PendingAction.NONE && pendingSourceNode != null) {
                    pendingEndX = x;
                    pendingEndY = y;
                    invalidate();
                }

                Node touchedNode = findNodeAt(x, y);

                if (touchedNode != null) {

                    // ====== 优先处理：链接/取消链接模式 ======
                    if (pendingAction != PendingAction.NONE && pendingSourceNode != null) {

                        // 不允许对自己
                        if (touchedNode.getId().equals(pendingSourceNode.getId())) {
                            return true;
                        }

                        if (pendingAction == PendingAction.CONNECT) {
                            // 连接：弹框编辑连线文字（可编辑）
                            showEditConnectionLabelDialog(pendingSourceNode, touchedNode);
                            return true;
                        } else if (pendingAction == PendingAction.DISCONNECT) {
                            // 取消链接：删除两者之间连线
                            removeConnectionsBetween(pendingSourceNode.getId(), touchedNode.getId());
                            cancelPendingAction();
                            Toast.makeText(getContext(), "已取消连接", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    }

                    // 普通点击：选中+可拖拽
                    selectNode(touchedNode);
                    draggingNode = touchedNode;
                    draggingNode.setDragging(true);
                    return true;

                } else {
                    // 点击空白：如果正在链接/取消链接 => 退出模式
                    if (pendingAction != PendingAction.NONE) {
                        cancelPendingAction();
                        invalidate();
                        return true;
                    }

                    Connection touchedConnection = findConnectionAt(x, y, 20);
                    if (touchedConnection != null) {
                        selectConnection(touchedConnection);
                        return true;
                    } else {
                        isDragging = true;
                        clearSelection();
                        return true;
                    }
                }
            }

            case MotionEvent.ACTION_MOVE: {
                float dx = (x - lastTouchX) / scale;
                float dy = (y - lastTouchY) / scale;

                if (pendingAction != PendingAction.NONE && pendingSourceNode != null) {
                    pendingEndX = x;
                    pendingEndY = y;
                    invalidate();
                }

                if (draggingNode != null && !isScaling) {
                    draggingNode.move(dx, dy);
                    invalidate();
                } else if (isDragging && !isScaling) {
                    offsetX += dx;
                    offsetY += dy;
                    invalidate();
                }

                lastTouchX = x;
                lastTouchY = y;
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                isDragging = false;

                if (draggingNode != null) {
                    draggingNode.setDragging(false);
                    draggingNode = null;
                    notifyDataChanged(); // 节点拖动后自动保存
                }

                break;
            }
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

    // ====== 公开方法：节点/连接增删 ======
    public void addNode(Node node) {
        nodes.put(node.getId(), node);
        invalidate();
        notifyDataChanged();
    }

    public void removeNode(String nodeId) {
        Node node = nodes.get(nodeId);
        if (node != null) {
            List<String> connectionsToRemove = new ArrayList<>();
            for (Connection connection : connections.values()) {
                if (connection.getFromNodeId().equals(nodeId) ||
                        connection.getToNodeId().equals(nodeId)) {
                    connectionsToRemove.add(connection.getId());
                }
            }
            for (String connId : connectionsToRemove) {
                removeConnection(connId);
            }

            nodes.remove(nodeId);
            invalidate();
            notifyDataChanged();
        }
    }

    public void addConnection(Connection connection) {
        connections.put(connection.getId(), connection);

        Node fromNode = nodes.get(connection.getFromNodeId());
        Node toNode = nodes.get(connection.getToNodeId());
        if (fromNode != null) fromNode.addConnection(connection.getId());
        if (toNode != null) toNode.addConnection(connection.getId());

        invalidate();
        notifyDataChanged();
    }

    public void removeConnection(String connectionId) {
        Connection connection = connections.get(connectionId);
        if (connection != null) {
            Node fromNode = nodes.get(connection.getFromNodeId());
            Node toNode = nodes.get(connection.getToNodeId());
            if (fromNode != null) fromNode.removeConnection(connectionId);
            if (toNode != null) toNode.removeConnection(connectionId);

            connections.remove(connectionId);
            invalidate();
            notifyDataChanged();
        }
    }
    // ====== 你要的：链接/取消链接模式入口（给 NodeEditDialog 调） ======
    public void startConnectionMode(Node sourceNode) {
        pendingAction = PendingAction.CONNECT;
        pendingSourceNode = sourceNode;

        pendingEndX = (sourceNode.getX() + offsetX + sourceNode.getWidth() / 2f) * scale;
        pendingEndY = (sourceNode.getY() + offsetY + sourceNode.getHeight() / 2f) * scale;

        invalidate();
    }

    public void startDisconnectMode(Node sourceNode) {
        pendingAction = PendingAction.DISCONNECT;
        pendingSourceNode = sourceNode;

        pendingEndX = (sourceNode.getX() + offsetX + sourceNode.getWidth() / 2f) * scale;
        pendingEndY = (sourceNode.getY() + offsetY + sourceNode.getHeight() / 2f) * scale;

        invalidate();
    }

    public void cancelPendingAction() {
        pendingAction = PendingAction.NONE;
        pendingSourceNode = null;
        invalidate();
    }

    // ====== 核心：弹框编辑连线文字（可编辑），并保证样式一致 ======
    private void showEditConnectionLabelDialog(Node from, Node to) {
    private void showEditConnectionLabelDialog(Node from, Node to) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = 32;
        layout.setPadding(padding, padding, padding, padding);

        EditText input = new EditText(getContext());
        input.setHint("输入连线文字（可为空）");
        layout.addView(input);

        Spinner colorSpinner = new Spinner(getContext());
        String[] colorNames = {"默认蓝色", "绿色", "红色", "橙色", "紫色", "白色"};
        Integer[] colorValues = {
                null,
                Color.parseColor("#4CAF50"),
                Color.parseColor("#F44336"),
                Color.parseColor("#FF9800"),
                Color.parseColor("#9C27B0"),
                Color.WHITE
        };
        ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                colorNames
        );
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colorSpinner.setAdapter(colorAdapter);
        layout.addView(colorSpinner);

        Spinner widthSpinner = new Spinner(getContext());
        String[] widthNames = {"细", "中", "粗", "超粗"};
        float[] widthValues = {4f, 6f, 8f, 10f};
        ArrayAdapter<String> widthAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                widthNames
        );
        widthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        widthSpinner.setAdapter(widthAdapter);
        layout.addView(widthSpinner);

        Connection existing = findConnectionBetween(from.getId(), to.getId());
        if (existing != null) {
            if (existing.getLabel() != null) {
                input.setText(existing.getLabel());
                input.setSelection(input.getText().length());
            }

            Integer existingColor = existing.getCustomColor();
            int colorIndex = 0;
            for (int i = 0; i < colorValues.length; i++) {
                Integer value = colorValues[i];
                if ((value == null && existingColor == null) ||
                        (value != null && value.equals(existingColor))) {
                    colorIndex = i;
                    break;
                }
            }
            colorSpinner.setSelection(colorIndex);

            float w = existing.getStrokeWidth();
            int widthIndex = 0;
            if (w >= 10f) widthIndex = 3;
            else if (w >= 8f) widthIndex = 2;
            else if (w >= 6f) widthIndex = 1;
            widthSpinner.setSelection(widthIndex);
        }

        new AlertDialog.Builder(getContext())
                .setTitle("编辑连线")
                .setView(layout)
                .setNegativeButton("取消", (d, w) -> {
                    cancelPendingAction();
                    d.dismiss();
                })
                .setPositiveButton("确定", (d, w) -> {
                    String label = input.getText().toString().trim();
                    Integer selectedColor = colorValues[colorSpinner.getSelectedItemPosition()];
                    float selectedWidth = widthValues[widthSpinner.getSelectedItemPosition()];

                    Connection ex = findConnectionBetween(from.getId(), to.getId());
                    if (ex != null) {
                        ex.setLabel(label);
                        ex.setCustomColor(selectedColor);
                        ex.setStrokeWidth(selectedWidth);
                    } else {
                        Connection c = new Connection(
                                from.getId(),
                                to.getId(),
                                Connection.ConnectionType.SEQUENCE,
                                label
                        );
                        c.setCustomColor(selectedColor);
                        c.setStrokeWidth(selectedWidth);
                        addConnection(c);
                    }

                    cancelPendingAction();
                    invalidate();
                    notifyDataChanged();
                    d.dismiss();
                })
                .show();
    }

    private void removeConnectionsBetween(String idA, String idB) {
        List<String> toRemoveIds = new ArrayList<>();
        for (Connection c : connections.values()) {
            boolean ab = c.getFromNodeId().equals(idA) && c.getToNodeId().equals(idB);
            boolean ba = c.getFromNodeId().equals(idB) && c.getToNodeId().equals(idA);
            if (ab || ba) {
                toRemoveIds.add(c.getId());
            }
        }
        for (String cid : toRemoveIds) {
            removeConnection(cid);
        }
    }

    private Connection findConnectionBetween(String idA, String idB) {
        for (Connection c : connections.values()) {
            boolean ab = c.getFromNodeId().equals(idA) && c.getToNodeId().equals(idB);
            boolean ba = c.getFromNodeId().equals(idB) && c.getToNodeId().equals(idA);
            if (ab || ba) return c;
        }
        return null;
    }

    // ====== 搜索 ======
    private void focusNode(Node node) {
        if (node == null || getWidth() == 0 || getHeight() == 0) return;

        float nodeCenterX = node.getX() + node.getWidth() / 2f;
        float nodeCenterY = node.getY() + node.getHeight() / 2f;

        offsetX = (getWidth() / (2f * scale)) - nodeCenterX;
        offsetY = (getHeight() / (2f * scale)) - nodeCenterY;

        invalidate();
    }

    public void focusNodeById(String nodeId) {
        Node node = nodes.get(nodeId);
        if (node != null) {
            focusNode(node);
        }
    }
    public void search(String keyword, List<Node.NodeType> types, boolean highlight) {
        keyword = keyword == null ? "" : keyword.trim();
        searchKeyword = keyword.toLowerCase();
        searchTypes = types != null ? types : new ArrayList<>();
        highlightSearchResults = highlight;
        searchResultNodeIds.clear();

        if (searchKeyword.isEmpty() && searchTypes.isEmpty()) {
            clearSearch();
            return;
        }

        for (Node node : nodes.values()) {
            boolean matches;

            if (!searchTypes.isEmpty() && !searchTypes.contains(node.getType())) {
                continue;
            }

            if (!searchKeyword.isEmpty()) {
                String title = node.getTitle() == null ? "" : node.getTitle().toLowerCase();
                String content = node.getContent() == null ? "" : node.getContent().toLowerCase();
                matches = title.contains(searchKeyword) || content.contains(searchKeyword);
            } else {
                matches = true;
            }

            if (matches) {
                searchResultNodeIds.add(node.getId());
            }
        }

        if (!searchResultNodeIds.isEmpty()) {
            focusNodeById(searchResultNodeIds.get(0));
        } else {
            invalidate();
        }
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

    // ====== 手势 ======
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float worldX = (e.getX() / scale) - offsetX;
            float worldY = (e.getY() / scale) - offsetY;

            final Node newNode = new Node("新节点", "双击编辑内容", Node.NodeType.NOTE, worldX, worldY);
            addNode(newNode);
            selectNode(newNode);

            postDelayed(() -> {
                if (getContext() instanceof MainActivity) {
                    ((MainActivity) getContext()).showNodeEditDialog(newNode);
                }
            }, 100);

            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            final Node touchedNode = findNodeAt(e.getX(), e.getY());
            if (touchedNode != null) {
                selectNode(touchedNode);

                postDelayed(() -> {
                    if (getContext() instanceof MainActivity) {
                        ((MainActivity) getContext()).showNodeEditDialog(touchedNode);
                    }
                }, 100);
            }
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            isScaling = true;

            float oldScale = scale;
            float newScale = oldScale * detector.getScaleFactor();
            newScale = Math.max(0.3f, Math.min(newScale, 4.0f));

            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();

            float worldFocusX = (focusX / oldScale) - offsetX;
            float worldFocusY = (focusY / oldScale) - offsetY;

            scale = newScale;

            offsetX = (focusX / scale) - worldFocusX;
            offsetY = (focusY / scale) - worldFocusY;

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

    // 数据访问
    public Map<String, Node> getNodes() {
        return new HashMap<>(nodes);
    }

    public Map<String, Connection> getConnections() {
        return new HashMap<>(connections);
    }

    public void setNodes(Map<String, Node> nodes) {
        this.nodes.clear();
        if (nodes != null) this.nodes.putAll(nodes);
        invalidate();
    }

    public void setConnections(Map<String, Connection> connections) {
        this.connections.clear();
        if (connections != null) this.connections.putAll(connections);
        invalidate();
    }
}
