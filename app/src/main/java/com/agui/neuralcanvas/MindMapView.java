package com.agui.neuralcanvas;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class MindMapView extends View {

    public interface OnDataChangeListener {
        void onDataChanged();
    }

    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final Map<String, Connection> connections = new LinkedHashMap<>();

    private OnDataChangeListener onDataChangeListener;
    private float scale = 1f;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private float lastX;
    private float lastY;
    private boolean draggingCanvas;
    private Node draggingNode;
    private Node selectedNode;
    private Connection selectedConnection;
    private Node previewNode;

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tempLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    private String searchKeyword = "";
    private List<Node.NodeType> searchTypes = new ArrayList<>();
    private boolean highlightSearchResults = false;
    private final List<String> searchResultNodeIds = new ArrayList<>();

    private enum PendingAction { NONE, CREATE_CONNECTION }
    private PendingAction pendingAction = PendingAction.NONE;
    private Node pendingSourceNode;
    private float pendingEndX;
    private float pendingEndY;

    private boolean boxSelectionMode = false;
    private boolean selectingBox = false;
    private RectF selectionRect = new RectF();
    private float selectionStartX;
    private float selectionStartY;

    public MindMapView(Context context) { super(context); init(); }
    public MindMapView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public MindMapView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        gestureDetector = new GestureDetector(getContext(), new GestureListener());
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

        gridPaint.setColor(Color.parseColor("#162033"));
        gridPaint.setStrokeWidth(1f);

        tempLinePaint.setColor(Color.parseColor("#8B5CF6"));
        tempLinePaint.setStyle(Paint.Style.STROKE);
        tempLinePaint.setStrokeWidth(dp(2f));

        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setColor(Color.parseColor("#E9D5FF"));
        highlightPaint.setStrokeWidth(dp(2f));

        selectFillPaint.setColor(Color.parseColor("#228B5CF6"));
        selectFillPaint.setStyle(Paint.Style.FILL);
        selectStrokePaint.setColor(Color.parseColor("#8B5CF6"));
        selectStrokePaint.setStyle(Paint.Style.STROKE);
        selectStrokePaint.setStrokeWidth(dp(1.5f));
        selectTextPaint.setColor(Color.parseColor("#E2E8F0"));
        selectTextPaint.setTextSize(dp(12f));
        selectTextPaint.setFakeBoldText(true);
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    public void setOnDataChangeListener(OnDataChangeListener listener) {
        this.onDataChangeListener = listener;
    }

    private void notifyDataChanged() {
        if (onDataChangeListener != null) onDataChangeListener.onDataChanged();
    }

    public void requestRender() { invalidate(); }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.parseColor("#070B14"));
        drawGrid(canvas);

        for (Connection c : connections.values()) {
            Node from = nodes.get(c.getFromNodeId());
            Node to = nodes.get(c.getToNodeId());
            if (from != null && to != null) c.draw(canvas, from, to, scale, offsetX, offsetY);
        }

        if (pendingAction == PendingAction.CREATE_CONNECTION && pendingSourceNode != null) {
            float sx = (pendingSourceNode.getX() + offsetX + pendingSourceNode.getWidth() / 2f) * scale;
            float sy = (pendingSourceNode.getY() + offsetY + pendingSourceNode.getHeight() / 2f) * scale;
            canvas.drawLine(sx, sy, pendingEndX, pendingEndY, tempLinePaint);
        }

        for (Node node : nodes.values()) {
            node.draw(canvas, scale, offsetX, offsetY);
            if (highlightSearchResults && searchResultNodeIds.contains(node.getId())) {
                RectF r = getNodeScreenRect(node);
                canvas.drawRoundRect(r.left - dp(6), r.top - dp(6), r.right + dp(6), r.bottom + dp(6), dp(12), dp(12), highlightPaint);
            }
        }

        if (boxSelectionMode) {
            if (selectingBox) {
                canvas.drawRoundRect(selectionRect, dp(12), dp(12), selectFillPaint);
                canvas.drawRoundRect(selectionRect, dp(12), dp(12), selectStrokePaint);
            }
            int count = getSelectedNodeIds().size();
            String tip = selectingBox ? "框选中：" + count : (count > 0 ? "已选中 " + count + " 个节点，可在菜单里批量删除" : "框选模式：拖出矩形可批量选节点");
            canvas.drawText(tip, dp(16), getHeight() - dp(18), selectTextPaint);
        }
    }

    private void drawGrid(Canvas canvas) {
        float step = 36f * scale;
        if (step < 18f) return;
        float startX = ((offsetX * scale) % step + step) % step;
        float startY = ((offsetY * scale) % step + step) % step;
        for (float x = startX; x < getWidth(); x += step) canvas.drawLine(x, 0, x, getHeight(), gridPaint);
        for (float y = startY; y < getHeight(); y += step) canvas.drawLine(0, y, getWidth(), y, gridPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        if (event.getPointerCount() > 1) return true;

        if (boxSelectionMode) {
            return handleBoxSelectionTouch(event);
        }

        gestureDetector.onTouchEvent(event);
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;
                draggingNode = findNodeAt(x, y);
                selectedConnection = draggingNode == null ? findConnectionAt(x, y) : null;
                if (draggingNode != null) {
                    clearSelections();
                    draggingNode.setSelected(true);
                    selectedNode = draggingNode;
                    previewNode = null;
                } else if (selectedConnection != null) {
                    clearSelections();
                    selectedConnection.setSelected(true);
                } else {
                    clearSelections();
                    draggingCanvas = true;
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (pendingAction == PendingAction.CREATE_CONNECTION && pendingSourceNode != null) {
                    pendingEndX = x;
                    pendingEndY = y;
                    invalidate();
                    return true;
                }
                float dx = (x - lastX) / scale;
                float dy = (y - lastY) / scale;
                if (draggingNode != null) {
                    draggingNode.move(dx, dy);
                    notifyDataChanged();
                } else if (draggingCanvas) {
                    offsetX += dx;
                    offsetY += dy;
                }
                lastX = x;
                lastY = y;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                draggingCanvas = false;
                draggingNode = null;
                return true;
            default:
                return true;
        }
    }

    private boolean handleBoxSelectionTouch(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                selectingBox = true;
                selectionStartX = x;
                selectionStartY = y;
                selectionRect.set(x, y, x, y);
                clearSelections();
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                selectionRect.set(Math.min(selectionStartX, x), Math.min(selectionStartY, y), Math.max(selectionStartX, x), Math.max(selectionStartY, y));
                updateSelectionByRect();
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                selectionRect.set(Math.min(selectionStartX, x), Math.min(selectionStartY, y), Math.max(selectionStartX, x), Math.max(selectionStartY, y));
                updateSelectionByRect();
                selectingBox = false;
                invalidate();
                return true;
            default:
                return true;
        }
    }

    private void updateSelectionByRect() {
        for (Node node : nodes.values()) {
            node.setSelected(RectF.intersects(selectionRect, getNodeScreenRect(node)));
        }
    }

    private RectF getNodeScreenRect(Node node) {
        float left = (node.getX() + offsetX) * scale;
        float top = (node.getY() + offsetY) * scale;
        return new RectF(left, top, left + node.getWidth() * scale, top + node.getHeight() * scale);
    }

    private Node findNodeAt(float x, float y) {
        List<Node> list = new ArrayList<>(nodes.values());
        for (int i = list.size() - 1; i >= 0; i--) {
            if (getNodeScreenRect(list.get(i)).contains(x, y)) return list.get(i);
        }
        return null;
    }

    private Connection findConnectionAt(float x, float y) {
        for (Connection c : connections.values()) {
            Node from = nodes.get(c.getFromNodeId());
            Node to = nodes.get(c.getToNodeId());
            if (from != null && to != null && c.isNear(x, y, from, to, scale, offsetX, offsetY, dp(10f))) return c;
        }
        return null;
    }

    private void clearSelections() {
        for (Node node : nodes.values()) node.setSelected(false);
        for (Connection c : connections.values()) c.setSelected(false);
        selectedNode = null;
        selectedConnection = null;
    }

    public void selectOnlyNode(String nodeId) {
        clearSelections();
        Node node = nodes.get(nodeId);
        if (node != null) {
            node.setSelected(true);
            selectedNode = node;
        }
        invalidate();
    }

    public void addNode(Node node) {
        nodes.put(node.getId(), node);
        invalidate();
        notifyDataChanged();
    }

    public void removeNode(String nodeId) {
        Node node = nodes.remove(nodeId);
        if (node == null) return;
        List<String> toRemove = new ArrayList<>();
        for (Connection c : connections.values()) {
            if (nodeId.equals(c.getFromNodeId()) || nodeId.equals(c.getToNodeId())) toRemove.add(c.getId());
        }
        for (String id : toRemove) removeConnection(id);
        if (previewNode != null && nodeId.equals(previewNode.getId())) previewNode = null;
        invalidate();
        notifyDataChanged();
    }

    public void addConnection(Connection connection) {
        connections.put(connection.getId(), connection);
        Node from = nodes.get(connection.getFromNodeId());
        Node to = nodes.get(connection.getToNodeId());
        if (from != null) from.addConnection(connection.getId());
        if (to != null) to.addConnection(connection.getId());
        invalidate();
        notifyDataChanged();
    }

    public void removeConnection(String connectionId) {
        Connection connection = connections.remove(connectionId);
        if (connection == null) return;
        Node from = nodes.get(connection.getFromNodeId());
        Node to = nodes.get(connection.getToNodeId());
        if (from != null) from.removeConnection(connectionId);
        if (to != null) to.removeConnection(connectionId);
        invalidate();
        notifyDataChanged();
    }

    public void clearAll() {
        nodes.clear();
        connections.clear();
        clearSelections();
        previewNode = null;
        pendingAction = PendingAction.NONE;
        pendingSourceNode = null;
        selectionRect.setEmpty();
        invalidate();
        notifyDataChanged();
    }

    public Map<String, Node> getNodes() { return new LinkedHashMap<>(nodes); }
    public Map<String, Connection> getConnections() { return new LinkedHashMap<>(connections); }
    public Map<String, Node> getNodesInternal() { return nodes; }
    public Map<String, Connection> getConnectionsInternal() { return connections; }
    public void setNodes(Map<String, Node> map) { nodes.clear(); if (map != null) nodes.putAll(map); invalidate(); }
    public void setConnections(Map<String, Connection> map) { connections.clear(); if (map != null) connections.putAll(map); invalidate(); }

    public void search(String keyword, List<Node.NodeType> types, boolean highlight) {
        searchKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        searchTypes = types == null ? new ArrayList<>() : types;
        highlightSearchResults = highlight;
        searchResultNodeIds.clear();
        for (Node node : nodes.values()) {
            if (!searchTypes.isEmpty() && !searchTypes.contains(node.getType())) continue;
            boolean matched = searchKeyword.isEmpty()
                    || (node.getTitle() != null && node.getTitle().toLowerCase().contains(searchKeyword))
                    || (node.getContent() != null && node.getContent().toLowerCase().contains(searchKeyword));
            if (matched) searchResultNodeIds.add(node.getId());
        }
        if (!searchResultNodeIds.isEmpty()) focusNodeById(searchResultNodeIds.get(0));
        invalidate();
    }

    public void clearSearch() {
        searchKeyword = "";
        searchTypes = new ArrayList<>();
        highlightSearchResults = false;
        searchResultNodeIds.clear();
        invalidate();
    }

    public int getSearchResultCount() { return searchResultNodeIds.size(); }

    public void focusNodeById(String nodeId) {
        Node node = nodes.get(nodeId);
        if (node == null || getWidth() == 0 || getHeight() == 0) return;
        float centerX = node.getX() + node.getWidth() / 2f;
        float centerY = node.getY() + node.getHeight() / 2f;
        offsetX = getWidth() / (2f * scale) - centerX;
        offsetY = getHeight() / (2f * scale) - centerY;
        previewNode = node;
        invalidate();
    }

    public void selectNodeById(String nodeId) { selectOnlyNode(nodeId); }

    public List<String> getSelectedNodeIds() {
        List<String> ids = new ArrayList<>();
        for (Node node : nodes.values()) if (node.isSelected()) ids.add(node.getId());
        return ids;
    }

    public boolean hasSelectedNodes() {
        return !getSelectedNodeIds().isEmpty();
    }

    public AiGraphSnapshot getSelectedGraphSnapshot() {
        LinkedHashSet<String> selectedIds = new LinkedHashSet<>(getSelectedNodeIds());
        if (selectedIds.isEmpty()) return AiGraphSnapshot.from(nodes, connections);
        AiGraphSnapshot snapshot = new AiGraphSnapshot();
        for (String nodeId : selectedIds) {
            Node node = nodes.get(nodeId);
            if (node == null) continue;
            AiGraphSnapshot.SnapshotNode item = new AiGraphSnapshot.SnapshotNode();
            item.id = node.getId();
            item.title = node.getTitle();
            item.content = node.getContent();
            item.type = node.getType().name();
            item.shape = node.getShape().name();
            item.x = node.getX();
            item.y = node.getY();
            item.width = node.getWidth();
            item.height = node.getHeight();
            item.connectionIds = new ArrayList<>(node.getConnectionIds());
            snapshot.nodes.add(item);
        }
        for (Connection c : connections.values()) {
            if (!selectedIds.contains(c.getFromNodeId()) || !selectedIds.contains(c.getToNodeId())) continue;
            AiGraphSnapshot.SnapshotConnection item = new AiGraphSnapshot.SnapshotConnection();
            item.id = c.getId();
            item.fromNodeId = c.getFromNodeId();
            item.toNodeId = c.getToNodeId();
            item.type = c.getType() == null ? "" : c.getType().name();
            item.label = c.getLabel();
            item.strokeWidth = c.getStrokeWidth();
            item.customColor = c.getCustomColor();
            item.directed = true;
            snapshot.connections.add(item);
        }
        return snapshot;
    }

    public void clearPreviewCard() { previewNode = null; invalidate(); }
    public void startConnectionMode(Node sourceNode) { pendingAction = PendingAction.CREATE_CONNECTION; pendingSourceNode = sourceNode; }
    public void cancelPendingAction() { pendingAction = PendingAction.NONE; pendingSourceNode = null; invalidate(); }

    public void startBoxSelectionMode() {
        boxSelectionMode = true;
        selectingBox = false;
        selectionRect.setEmpty();
        invalidate();
    }

    public void cancelBoxSelectionMode() {
        boxSelectionMode = false;
        selectingBox = false;
        selectionRect.setEmpty();
        invalidate();
    }

    public boolean isBoxSelectionMode() { return boxSelectionMode; }

    public int deleteSelectedNodes() {
        List<String> ids = getSelectedNodeIds();
        for (String id : new ArrayList<>(ids)) removeNode(id);
        cancelBoxSelectionMode();
        return ids.size();
    }

    private Connection findConnectionBetween(String fromId, String toId) {
        for (Connection c : connections.values()) if (fromId.equals(c.getFromNodeId()) && toId.equals(c.getToNodeId())) return c;
        return null;
    }

    private void showEditConnectionDialog(Node from, Node to) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) dp(18f);
        layout.setPadding(padding, padding, padding, padding);

        EditText input = new EditText(getContext());
        input.setHint("输入连线文字（可为空）");
        layout.addView(input);

        Spinner typeSpinner = new Spinner(getContext());
        String[] names = new String[Connection.ConnectionType.values().length];
        for (int i = 0; i < names.length; i++) names[i] = Connection.ConnectionType.values()[i].label;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
        layout.addView(typeSpinner);

        Connection existing = findConnectionBetween(from.getId(), to.getId());
        if (existing != null) input.setText(existing.getLabel() == null ? "" : existing.getLabel());

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(existing == null ? "新建连线" : "编辑连线")
                .setView(layout)
                .setNegativeButton("取消", (d, w) -> cancelPendingAction())
                .setPositiveButton("确定", (d, w) -> {
                    String label = input.getText().toString().trim();
                    Connection.ConnectionType type = Connection.ConnectionType.values()[typeSpinner.getSelectedItemPosition()];
                    if (existing != null) {
                        existing.setLabel(label);
                        existing.setType(type);
                        notifyDataChanged();
                    } else {
                        addConnection(new Connection(from.getId(), to.getId(), type, label));
                    }
                    cancelPendingAction();
                });
        if (existing != null) builder.setNeutralButton("删除连线", (d, w) -> removeConnection(existing.getId()));
        builder.show();
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override public boolean onDown(MotionEvent e) { return true; }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Node node = findNodeAt(e.getX(), e.getY());
            if (pendingAction == PendingAction.CREATE_CONNECTION && pendingSourceNode != null && node != null && !pendingSourceNode.getId().equals(node.getId())) {
                showEditConnectionDialog(pendingSourceNode, node);
                return true;
            }
            if (node != null) {
                previewNode = previewNode != null && previewNode.getId().equals(node.getId()) ? null : node;
                invalidate();
                return true;
            }
            previewNode = null;
            invalidate();
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Node node = findNodeAt(e.getX(), e.getY());
            if (node != null) return false;
            float worldX = e.getX() / scale - offsetX;
            float worldY = e.getY() / scale - offsetY;
            Node newNode = new Node("新节点", "输入内容", worldX - 84f, worldY - 84f, Node.NodeType.CONCEPT);
            addNode(newNode);
            if (getContext() instanceof MainActivity) ((MainActivity) getContext()).showNodeEditDialog(newNode);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Node node = findNodeAt(e.getX(), e.getY());
            if (pendingAction == PendingAction.CREATE_CONNECTION && pendingSourceNode != null && node != null && !pendingSourceNode.getId().equals(node.getId())) {
                showEditConnectionDialog(pendingSourceNode, node);
                return;
            }
            if (node != null && getContext() instanceof MainActivity) {
                ((MainActivity) getContext()).showNodeEditDialog(node);
                return;
            }
            Connection connection = findConnectionAt(e.getX(), e.getY());
            if (connection != null) {
                Node from = nodes.get(connection.getFromNodeId());
                Node to = nodes.get(connection.getToNodeId());
                if (from != null && to != null) showEditConnectionDialog(from, to);
            }
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float oldScale = scale;
            scale = Math.max(0.08f, Math.min(scale * detector.getScaleFactor(), 10f));
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            float worldX = focusX / oldScale - offsetX;
            float worldY = focusY / oldScale - offsetY;
            offsetX = focusX / scale - worldX;
            offsetY = focusY / scale - worldY;
            invalidate();
            return true;
        }
    }
}
