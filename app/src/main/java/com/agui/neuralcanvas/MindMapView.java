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
import android.view.ViewConfiguration;
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

    private OnDataChangeListener onDataChangeListener;

    public void setOnDataChangeListener(OnDataChangeListener listener) {
        this.onDataChangeListener = listener;
    }

    private void notifyDataChanged() {
        if (onDataChangeListener != null) {
            onDataChangeListener.onDataChanged();
        }
    }

    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final Map<String, Connection> connections = new LinkedHashMap<>();

    private String searchKeyword = "";
    private List<Node.NodeType> searchTypes = new ArrayList<>();
    private boolean highlightSearchResults = false;
    private final List<String> searchResultNodeIds = new ArrayList<>();

    private float scale = 1.0f;
    private float offsetX = 0f;
    private float offsetY = 0f;

    private float downX = 0f;
    private float downY = 0f;
    private float lastTouchX = 0f;
    private float lastTouchY = 0f;

    private boolean isDraggingCanvas = false;
    private boolean isDraggingNode = false;
    private boolean isScaling = false;
    private boolean movedEnough = false;

    private final float minScale = 0.08f;
    private final float maxScale = 24f;
    private final int touchSlop;

    private Node draggingNode = null;
    private Node selectedNode = null;
    private Connection selectedConnection = null;
    private Node previewNode = null;
    private RectF previewRect = null;

    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    private Paint previewCardPaint;
    private Paint previewBorderPaint;
    private Paint previewTitlePaint;
    private Paint previewContentPaint;
    private Paint previewShadowPaint;
    private Paint tempLinePaint;
    private Paint gridPaint;
    private Paint searchHighlightPaint;

    private enum PendingAction {
        NONE,
        CREATE_CONNECTION
    }

    private PendingAction pendingAction = PendingAction.NONE;
    private Node pendingSourceNode = null;
    private float pendingEndX = 0f;
    private float pendingEndY = 0f;

    public MindMapView(Context context) {
        super(context);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        init();
    }

    public MindMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        init();
    }

    public MindMapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_HARDWARE, null);

        gestureDetector = new GestureDetector(getContext(), new GestureListener());
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

        previewCardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewCardPaint.setColor(Color.parseColor("#F8FAFC"));

        previewBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewBorderPaint.setStyle(Paint.Style.STROKE);
        previewBorderPaint.setStrokeWidth(dp(1.2f));
        previewBorderPaint.setColor(Color.parseColor("#D9E2F1"));

        previewTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewTitlePaint.setColor(Color.parseColor("#0F172A"));
        previewTitlePaint.setTextSize(dp(15f));
        previewTitlePaint.setFakeBoldText(true);

        previewContentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewContentPaint.setColor(Color.parseColor("#334155"));
        previewContentPaint.setTextSize(dp(13f));

        previewShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewShadowPaint.setColor(Color.parseColor("#55000000"));

        tempLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tempLinePaint.setColor(Color.parseColor("#93C5FD"));
        tempLinePaint.setStyle(Paint.Style.STROKE);
        tempLinePaint.setStrokeWidth(dp(2f));

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#1D2638"));
        gridPaint.setStrokeWidth(1f);

        searchHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        searchHighlightPaint.setStyle(Paint.Style.STROKE);
        searchHighlightPaint.setColor(Color.parseColor("#F8FAFC"));
        searchHighlightPaint.setAlpha(185);
    }

    private float dp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.parseColor("#0B1020"));

        drawGrid(canvas);

        for (Connection connection : connections.values()) {
            Node fromNode = nodes.get(connection.getFromNodeId());
            Node toNode = nodes.get(connection.getToNodeId());
            if (fromNode == null || toNode == null) continue;
            if (!isConnectionLikelyVisible(fromNode, toNode)) continue;
            connection.draw(canvas, fromNode, toNode, scale, offsetX, offsetY);
        }

        if (pendingAction == PendingAction.CREATE_CONNECTION && pendingSourceNode != null) {
            float startX = (pendingSourceNode.getX() + offsetX + pendingSourceNode.getWidth() / 2f) * scale;
            float startY = (pendingSourceNode.getY() + offsetY + pendingSourceNode.getHeight() / 2f) * scale;
            tempLinePaint.setStrokeWidth(Math.max(dp(1.5f), dp(2f) * (0.8f + scale * 0.25f)));
            canvas.drawLine(startX, startY, pendingEndX, pendingEndY, tempLinePaint);
        }

        for (Node node : nodes.values()) {
            if (!isNodeVisible(node)) continue;

            node.draw(canvas, scale, offsetX, offsetY);

            if (highlightSearchResults && searchResultNodeIds.contains(node.getId())) {
                drawSearchHighlight(canvas, node);
            }
        }

        if (previewNode != null) {
            drawPreviewCard(canvas, previewNode);
        }
    }

    private void drawGrid(Canvas canvas) {
        float base = 36f * scale;
        if (base < 18f) return;

        float width = getWidth();
        float height = getHeight();

        float startX = ((offsetX * scale) % base + base) % base;
        float startY = ((offsetY * scale) % base + base) % base;

        for (float x = startX; x < width; x += base) {
            canvas.drawLine(x, 0, x, height, gridPaint);
        }
        for (float y = startY; y < height; y += base) {
            canvas.drawLine(0, y, width, y, gridPaint);
        }
    }

    private void drawSearchHighlight(Canvas canvas, Node node) {
        float left = (node.getX() + offsetX) * scale - 8f * scale;
        float top = (node.getY() + offsetY) * scale - 8f * scale;
        float right = left + node.getWidth() * scale + 16f * scale;
        float bottom = top + node.getHeight() * scale + 16f * scale;

        searchHighlightPaint.setStrokeWidth(Math.max(dp(1.4f), 2.8f * scale));
        canvas.drawRoundRect(new RectF(left, top, right, bottom), 22f * scale, 22f * scale, searchHighlightPaint);
    }

    private void drawPreviewCard(Canvas canvas, Node node) {
        float nodeLeft = (node.getX() + offsetX) * scale;
        float nodeTop = (node.getY() + offsetY) * scale;
        float nodeWidth = node.getWidth() * scale;
        float nodeHeight = node.getHeight() * scale;

        float cardWidth = Math.max(dp(250f), Math.min(dp(360f), Math.max(nodeWidth * 1.15f, dp(250f))));
        float cardHeight = Math.max(dp(180f), Math.min(dp(320f), Math.max(nodeHeight * 1.12f, dp(180f))));

        float left = nodeLeft + nodeWidth / 2f - cardWidth / 2f;
        float top = nodeTop + nodeHeight / 2f - cardHeight / 2f;
        float right = left + cardWidth;
        float bottom = top + cardHeight;

        float margin = dp(10f);
        if (left < margin) {
            right += (margin - left);
            left = margin;
        }
        if (right > getWidth() - margin) {
            float diff = right - (getWidth() - margin);
            left -= diff;
            right -= diff;
        }
        if (top < margin + dp(52f)) {
            bottom += (margin + dp(52f) - top);
            top = margin + dp(52f);
        }
        if (bottom > getHeight() - margin) {
            float diff = bottom - (getHeight() - margin);
            top -= diff;
            bottom -= diff;
        }

        previewRect = new RectF(left, top, right, bottom);

        canvas.drawRoundRect(
                new RectF(left + dp(3f), top + dp(5f), right + dp(3f), bottom + dp(5f)),
                dp(18f), dp(18f), previewShadowPaint
        );
        canvas.drawRoundRect(previewRect, dp(18f), dp(18f), previewCardPaint);
        canvas.drawRoundRect(previewRect, dp(18f), dp(18f), previewBorderPaint);

        String title = node.getTitle() == null ? "" : node.getTitle();
        String content = node.getContent() == null ? "" : node.getContent();

        float paddingX = dp(16f);
        float usableWidth = cardWidth - paddingX * 2f;

        previewTitlePaint.setTextSize(dp(15f));
        previewContentPaint.setTextSize(dp(13f));

        float y = top + dp(26f);
        for (String line : wrapTextByWidth(title, previewTitlePaint, usableWidth, 2)) {
            canvas.drawText(line, left + paddingX, y, previewTitlePaint);
            y += dp(18f);
        }

        y += dp(6f);

        List<String> lines = wrapTextByWidth(content, previewContentPaint, usableWidth, 10);
        for (String line : lines) {
            if (y > bottom - dp(18f)) break;
            canvas.drawText(line, left + paddingX, y, previewContentPaint);
            y += dp(17f);
        }
    }

    private List<String> wrapTextByWidth(String text, Paint paint, float maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return lines;

        String normalized = text.replace("\r", "");
        String[] paragraphs = normalized.split("\n");

        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                if (lines.size() < maxLines) lines.add("");
                if (lines.size() >= maxLines) break;
                continue;
            }

            int start = 0;
            while (start < paragraph.length()) {
                if (lines.size() >= maxLines) break;

                int end = start + 1;
                while (end <= paragraph.length() && paint.measureText(paragraph, start, end) <= maxWidth) {
                    end++;
                }
                end--;

                if (end <= start) {
                    end = Math.min(start + 1, paragraph.length());
                }

                String line = paragraph.substring(start, end);

                if (lines.size() == maxLines - 1 && end < paragraph.length()) {
                    while (paint.measureText(line + "…") > maxWidth && line.length() > 1) {
                        line = line.substring(0, line.length() - 1);
                    }
                    line = line + "…";
                    lines.add(line);
                    return lines;
                }

                lines.add(line);
                start = end;
            }

            if (lines.size() >= maxLines) break;
        }

        return lines;
    }

    private boolean isNodeVisible(Node node) {
        float left = (node.getX() + offsetX) * scale;
        float top = (node.getY() + offsetY) * scale;
        float right = left + node.getWidth() * scale;
        float bottom = top + node.getHeight() * scale;

        float pad = dp(80f);
        return !(right < -pad || bottom < -pad || left > getWidth() + pad || top > getHeight() + pad);
    }

    private boolean isConnectionLikelyVisible(Node fromNode, Node toNode) {
        float fromLeft = (fromNode.getX() + offsetX) * scale;
        float fromTop = (fromNode.getY() + offsetY) * scale;
        float fromRight = fromLeft + fromNode.getWidth() * scale;
        float fromBottom = fromTop + fromNode.getHeight() * scale;

        float toLeft = (toNode.getX() + offsetX) * scale;
        float toTop = (toNode.getY() + offsetY) * scale;
        float toRight = toLeft + toNode.getWidth() * scale;
        float toBottom = toTop + toNode.getHeight() * scale;

        float minX = Math.min(fromLeft, toLeft);
        float minY = Math.min(fromTop, toTop);
        float maxX = Math.max(fromRight, toRight);
        float maxY = Math.max(fromBottom, toBottom);

        float pad = dp(100f);
        return !(maxX < -pad || maxY < -pad || minX > getWidth() + pad || minY > getHeight() + pad);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);

        if (event.getPointerCount() > 1) {
            isScaling = true;
            draggingNode = null;
            isDraggingCanvas = false;
            isDraggingNode = false;
            movedEnough = true;
            lastTouchX = event.getX();
            lastTouchY = event.getY();
            return true;
        }

        gestureDetector.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                downX = x;
                downY = y;
                lastTouchX = x;
                lastTouchY = y;
                movedEnough = false;
                isDraggingCanvas = false;
                isDraggingNode = false;
                isScaling = false;

                if (previewRect != null && previewNode != null && previewRect.contains(x, y)) {
                    return true;
                }

                Node touchedNode = findNodeAt(x, y);
                Connection touchedConnection = touchedNode == null ? findConnectionAt(x, y) : null;

                clearSelections();

                if (touchedNode != null) {
                    touchedNode.setSelected(true);
                    selectedNode = touchedNode;
                    draggingNode = touchedNode;
                    invalidate();
                } else if (touchedConnection != null) {
                    touchedConnection.setSelected(true);
                    selectedConnection = touchedConnection;
                    draggingNode = null;
                    invalidate();
                } else {
                    draggingNode = null;
                    previewNode = null;
                    previewRect = null;
                    invalidate();
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (isScaling) return true;

                float totalDx = x - downX;
                float totalDy = y - downY;

                if (!movedEnough && Math.hypot(totalDx, totalDy) > touchSlop) {
                    movedEnough = true;
                }

                if (pendingAction != PendingAction.NONE && pendingSourceNode != null) {
                    pendingEndX = x;
                    pendingEndY = y;
                    invalidate();
                }

                if (movedEnough) {
                    float dx = (x - lastTouchX) / scale;
                    float dy = (y - lastTouchY) / scale;

                    if (draggingNode != null) {
                        isDraggingNode = true;
                        draggingNode.setDragging(true);
                        previewNode = null;
                        previewRect = null;
                        draggingNode.move(dx, dy);
                    } else {
                        isDraggingCanvas = true;
                        offsetX += dx;
                        offsetY += dy;
                    }
                    invalidate();
                }

                lastTouchX = x;
                lastTouchY = y;
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (draggingNode != null && isDraggingNode) {
                    draggingNode.setDragging(false);
                    notifyDataChanged();
                }
                draggingNode = null;
                isDraggingCanvas = false;
                isDraggingNode = false;
                isScaling = false;
                break;
            }
        }

        return true;
    }

    private void clearSelections() {
        for (Node node : nodes.values()) {
            node.setSelected(false);
        }
        for (Connection connection : connections.values()) {
            connection.setSelected(false);
        }
        selectedNode = null;
        selectedConnection = null;
    }

    private Node findNodeAt(float touchX, float touchY) {
        List<Node> nodeList = new ArrayList<>(nodes.values());
        for (int i = nodeList.size() - 1; i >= 0; i--) {
            Node node = nodeList.get(i);
            if (node.contains(touchX, touchY, scale, offsetX, offsetY)) {
                return node;
            }
        }
        return null;
    }

    private Connection findConnectionAt(float x, float y) {
        for (Connection connection : connections.values()) {
            Node from = nodes.get(connection.getFromNodeId());
            Node to = nodes.get(connection.getToNodeId());
            if (connection.isNear(x, y, from, to, scale, offsetX, offsetY, dp(10f))) {
                return connection;
            }
        }
        return null;
    }

    public void addNode(Node node) {
        nodes.put(node.getId(), node);
        invalidate();
        notifyDataChanged();
    }

    public void removeNode(String nodeId) {
        Node node = nodes.get(nodeId);
        if (node != null) {
            List<String> toRemove = new ArrayList<>();
            for (Connection c : connections.values()) {
                if (nodeId.equals(c.getFromNodeId()) || nodeId.equals(c.getToNodeId())) {
                    toRemove.add(c.getId());
                }
            }
            for (String id : toRemove) {
                removeConnection(id);
            }

            if (previewNode != null && nodeId.equals(previewNode.getId())) {
                previewNode = null;
                previewRect = null;
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

            if (selectedConnection != null && connectionId.equals(selectedConnection.getId())) {
                selectedConnection = null;
            }

            invalidate();
            notifyDataChanged();
        }
    }

    public void clearAll() {
        nodes.clear();
        connections.clear();
        selectedNode = null;
        selectedConnection = null;
        previewNode = null;
        previewRect = null;
        pendingAction = PendingAction.NONE;
        pendingSourceNode = null;
        searchResultNodeIds.clear();
        invalidate();
        notifyDataChanged();
    }

    public Map<String, Node> getNodes() {
        return new LinkedHashMap<>(nodes);
    }

    public Map<String, Connection> getConnections() {
        return new LinkedHashMap<>(connections);
    }

    public Map<String, Node> getNodesInternal() {
        return nodes;
    }

    public Map<String, Connection> getConnectionsInternal() {
        return connections;
    }

    public void setNodes(Map<String, Node> map) {
        nodes.clear();
        if (map != null) nodes.putAll(map);
        previewNode = null;
        previewRect = null;
        invalidate();
    }

    public void setConnections(Map<String, Connection> map) {
        connections.clear();
        if (map != null) connections.putAll(map);
        invalidate();
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
            if (!searchTypes.isEmpty() && !searchTypes.contains(node.getType())) continue;

            boolean matches;
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
        searchTypes = new ArrayList<>();
        highlightSearchResults = false;
        searchResultNodeIds.clear();
        invalidate();
    }

    public int getSearchResultCount() {
        return searchResultNodeIds.size();
    }

    private void focusNode(Node node) {
        if (node == null || getWidth() == 0 || getHeight() == 0) return;

        float nodeCenterX = node.getX() + node.getWidth() / 2f;
        float nodeCenterY = node.getY() + node.getHeight() / 2f;

        offsetX = (getWidth() / (2f * scale)) - nodeCenterX;
        offsetY = (getHeight() / (2f * scale)) - nodeCenterY;

        previewNode = node;
        invalidate();
    }

    public void focusNodeById(String nodeId) {
        Node node = nodes.get(nodeId);
        if (node != null) focusNode(node);
    }

    public void selectNodeById(String nodeId) {
        clearSelections();
        Node node = nodes.get(nodeId);
        if (node != null) {
            node.setSelected(true);
            selectedNode = node;
            invalidate();
        }
    }

    public List<String> getSelectedNodeIds() {
        List<String> ids = new ArrayList<>();
        for (Node node : nodes.values()) {
            if (node.isSelected()) {
                ids.add(node.getId());
            }
        }
        return ids;
    }

    public AiGraphSnapshot getSelectedGraphSnapshot() {
        LinkedHashSet<String> selectedIds = new LinkedHashSet<>(getSelectedNodeIds());
        AiGraphSnapshot snapshot = new AiGraphSnapshot();

        if (selectedIds.isEmpty()) {
            return AiGraphSnapshot.from(nodes, connections);
        }

        for (String nodeId : selectedIds) {
            Node node = nodes.get(nodeId);
            if (node == null) continue;

            AiGraphSnapshot.SnapshotNode item = new AiGraphSnapshot.SnapshotNode();
            item.id = node.getId();
            item.title = node.getTitle();
            item.content = node.getContent();
            item.type = node.getType() == null ? "" : node.getType().name();
            item.shape = node.getShape() == null ? "" : node.getShape().name();
            item.x = node.getX();
            item.y = node.getY();
            item.width = node.getWidth();
            item.height = node.getHeight();
            item.connectionIds = new ArrayList<>(node.getConnectionIds());
            snapshot.nodes.add(item);
        }

        for (Connection c : connections.values()) {
            if (!selectedIds.contains(c.getFromNodeId()) || !selectedIds.contains(c.getToNodeId())) {
                continue;
            }

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

    public void clearPreviewCard() {
        previewNode = null;
        previewRect = null;
        invalidate();
    }

    public void startConnectionMode(Node sourceNode) {
        pendingAction = PendingAction.CREATE_CONNECTION;
        pendingSourceNode = sourceNode;
        previewNode = null;
        previewRect = null;
        invalidate();
    }

    public void cancelPendingAction() {
        pendingAction = PendingAction.NONE;
        pendingSourceNode = null;
        invalidate();
    }

    private Connection findConnectionBetween(String fromId, String toId) {
        for (Connection c : connections.values()) {
            if (fromId.equals(c.getFromNodeId()) && toId.equals(c.getToNodeId())) {
                return c;
            }
        }
        return null;
    }

    private void showEditConnectionDialog(Node from, Node to) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) dp(18f);
        layout.setPadding(padding, padding, padding, padding);

        EditText input = new EditText(getContext());
        input.setHint("输入连线文字（可为空）");
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        inputParams.bottomMargin = (int) dp(14f);
        input.setLayoutParams(inputParams);
        layout.addView(input);

        Spinner colorSpinner = new Spinner(getContext());
        LinearLayout.LayoutParams colorParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        colorParams.bottomMargin = (int) dp(14f);
        colorSpinner.setLayoutParams(colorParams);

        String[] colorNames = {"默认蓝色", "绿色", "红色", "橙色", "黄色", "紫色", "白色"};
        Integer[] colorValues = {
                null,
                Color.parseColor("#57D38C"),
                Color.parseColor("#FF6B6B"),
                Color.parseColor("#FFB84D"),
                Color.parseColor("#FFD54F"),
                Color.parseColor("#B084F5"),
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
        LinearLayout.LayoutParams widthParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        widthSpinner.setLayoutParams(widthParams);

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
            input.setText(existing.getLabel() == null ? "" : existing.getLabel());

            Integer existingColor = existing.getCustomColor();
            int colorIndex = 0;
            for (int i = 0; i < colorValues.length; i++) {
                Integer value = colorValues[i];
                if ((value == null && existingColor == null)
                        || (value != null && value.equals(existingColor))) {
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
private void showEditConnectionDialog(Node from, Node to) {
    LinearLayout layout = new LinearLayout(getContext());
    layout.setOrientation(LinearLayout.VERTICAL);
    int padding = (int) dp(18f);
    layout.setPadding(padding, padding, padding, padding);

    EditText input = new EditText(getContext());
    input.setHint("输入连线文字（可为空）");
    LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
    );
    inputParams.bottomMargin = (int) dp(14f);
    input.setLayoutParams(inputParams);
    layout.addView(input);

    Spinner typeSpinner = new Spinner(getContext());
    LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
    );
    typeParams.bottomMargin = (int) dp(14f);
    typeSpinner.setLayoutParams(typeParams);

    String[] typeNames = new String[Connection.ConnectionType.values().length];
    for (int i = 0; i < Connection.ConnectionType.values().length; i++) {
        typeNames[i] = Connection.ConnectionType.values()[i].label;
    }

    ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
            getContext(),
            android.R.layout.simple_spinner_item,
            typeNames
    );
    typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    typeSpinner.setAdapter(typeAdapter);
    layout.addView(typeSpinner);

    Spinner colorSpinner = new Spinner(getContext());
    LinearLayout.LayoutParams colorParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
    );
    colorParams.bottomMargin = (int) dp(14f);
    colorSpinner.setLayoutParams(colorParams);

    String[] colorNames = {"默认跟随类型颜色", "绿色", "红色", "橙色", "黄色", "紫色", "白色", "蓝色"};
    Integer[] colorValues = {
            null,
            Color.parseColor("#57D38C"),
            Color.parseColor("#FF6B6B"),
            Color.parseColor("#FFB84D"),
            Color.parseColor("#FFD54F"),
            Color.parseColor("#B084F5"),
            Color.WHITE,
            Color.parseColor("#67B7FF")
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
    LinearLayout.LayoutParams widthParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
    );
    widthSpinner.setLayoutParams(widthParams);

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
        input.setText(existing.getLabel() == null ? "" : existing.getLabel());

        int typeIndex = 0;
        for (int i = 0; i < Connection.ConnectionType.values().length; i++) {
            if (Connection.ConnectionType.values()[i] == existing.getType()) {
                typeIndex = i;
                break;
            }
        }
        typeSpinner.setSelection(typeIndex);

        Integer existingColor = existing.getCustomColor();
        int colorIndex = 0;
        for (int i = 0; i < colorValues.length; i++) {
            Integer value = colorValues[i];
            if ((value == null && existingColor == null)
                    || (value != null && value.equals(existingColor))) {
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
    } else {
        typeSpinner.setSelection(Connection.ConnectionType.LEADS_TO.ordinal());
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
            .setTitle(existing != null ? "编辑连线" : "新建连线")
            .setView(layout)
            .setNegativeButton("取消", (d, w) -> cancelPendingAction())
            .setPositiveButton("确定", (d, w) -> {
                String label = input.getText().toString().trim();
                Connection.ConnectionType selectedType =
                        Connection.ConnectionType.values()[typeSpinner.getSelectedItemPosition()];
                Integer selectedColor = colorValues[colorSpinner.getSelectedItemPosition()];
                float selectedWidth = widthValues[widthSpinner.getSelectedItemPosition()];

                Connection ex = findConnectionBetween(from.getId(), to.getId());
                boolean changedOnly = false;

                if (ex != null) {
                    ex.setType(selectedType);
                    ex.setLabel(label);
                    ex.setCustomColor(selectedColor);
                    ex.setStrokeWidth(selectedWidth);
                    changedOnly = true;
                } else {
                    Connection c = new Connection(
                            from.getId(),
                            to.getId(),
                            selectedType,
                            label
                    );
                    c.setCustomColor(selectedColor);
                    c.setStrokeWidth(selectedWidth);
                    addConnection(c);
                }

                cancelPendingAction();
                invalidate();

                if (changedOnly) {
                    notifyDataChanged();
                }
            });

    if (existing != null) {
        builder.setNeutralButton("删除连线", (d, w) -> {
            removeConnection(existing.getId());
            cancelPendingAction();
        });
    }

    builder.show();
}

    private void showEditExistingConnectionDialog(Connection connection) {
        if (connection == null) return;

        Node from = nodes.get(connection.getFromNodeId());
        Node to = nodes.get(connection.getToNodeId());
        if (from == null || to == null) return;

        showEditConnectionDialog(from, to);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Node node = findNodeAt(e.getX(), e.getY());

            if (pendingAction == PendingAction.CREATE_CONNECTION && pendingSourceNode != null) {
                if (node != null && !pendingSourceNode.getId().equals(node.getId())) {
                    showEditConnectionDialog(pendingSourceNode, node);
                    return true;
                }
            }

            if (node != null) {
                if (previewNode != null && previewNode.getId().equals(node.getId())) {
                    previewNode = null;
                    previewRect = null;
                } else {
                    previewNode = node;
                    previewRect = null;
                }
                invalidate();
                return true;
            } else {
                if (previewNode != null) {
                    previewNode = null;
                    previewRect = null;
                    invalidate();
                    return true;
                }
            }
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Node node = findNodeAt(e.getX(), e.getY());
            if (node == null) {
                float worldX = e.getX() / scale - offsetX;
                float worldY = e.getY() / scale - offsetY;

                Node newNode = new Node("新节点", "输入内容", worldX - 84f, worldY - 84f, Node.NodeType.CONCEPT);
                addNode(newNode);

                if (getContext() instanceof MainActivity) {
                    ((MainActivity) getContext()).showNodeEditDialog(newNode);
                }
                return true;
            }
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Node touchedNode = findNodeAt(e.getX(), e.getY());

            if (pendingAction == PendingAction.CREATE_CONNECTION && pendingSourceNode != null) {
                if (touchedNode != null && !pendingSourceNode.getId().equals(touchedNode.getId())) {
                    showEditConnectionDialog(pendingSourceNode, touchedNode);
                    return;
                }
            }

            if (touchedNode != null && getContext() instanceof MainActivity) {
                ((MainActivity) getContext()).showNodeEditDialog(touchedNode);
                return;
            }

            Connection touchedConnection = findConnectionAt(e.getX(), e.getY());
            if (touchedConnection != null) {
                showEditExistingConnectionDialog(touchedConnection);
            }
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            isScaling = true;
            draggingNode = null;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float oldScale = scale;
            float newScale = oldScale * detector.getScaleFactor();
            newScale = Math.max(minScale, Math.min(newScale, maxScale));

            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();

            float worldFocusX = (focusX / oldScale) - offsetX;
            float worldFocusY = (focusY / oldScale) - offsetY;

            scale = newScale;
            offsetX = (focusX / scale) - worldFocusX;
            offsetY = (focusY / scale) - worldFocusY;

            previewRect = null;
            invalidate();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            isScaling = false;
        }
    }
}
