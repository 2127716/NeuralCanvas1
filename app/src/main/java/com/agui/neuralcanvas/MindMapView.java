package com.agui.neuralcanvas;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
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

    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    private float downX = 0f;
    private float downY = 0f;

    private boolean isDraggingCanvas = false;
    private boolean isDraggingNode = false;
    private boolean isScaling = false;
    private boolean hasMovedBeyondTapSlop = false;

    private float touchSlop;

    private Node touchDownNode = null;
    private Node draggingNode = null;
    private Node selectedNode = null;
    private Node previewNode = null;
    private Connection selectedConnection = null;

    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    private Paint previewCardPaint;
    private Paint previewShadowPaint;
    private Paint previewBorderPaint;
    private Paint previewTitlePaint;
    private Paint previewContentPaint;
    private Paint searchPaint;
    private Paint tempLinePaint;

    private RectF previewRect;

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
        init();
    }

    public MindMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MindMapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gestureDetector = new GestureDetector(getContext(), new GestureListener());
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        previewCardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewCardPaint.setColor(Color.parseColor("#F8FAFC"));

        previewShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewShadowPaint.setColor(Color.parseColor("#55000000"));

        previewBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewBorderPaint.setStyle(Paint.Style.STROKE);
        previewBorderPaint.setStrokeWidth(3f);
        previewBorderPaint.setColor(Color.parseColor("#CBD5E1"));

        previewTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewTitlePaint.setColor(Color.parseColor("#0F172A"));
        previewTitlePaint.setTextSize(30f);
        previewTitlePaint.setFakeBoldText(true);

        previewContentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewContentPaint.setColor(Color.parseColor("#334155"));
        previewContentPaint.setTextSize(22f);

        searchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        searchPaint.setStyle(Paint.Style.STROKE);
        searchPaint.setColor(Color.parseColor("#F8FAFC"));
        searchPaint.setAlpha(180);

        tempLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tempLinePaint.setColor(Color.parseColor("#93C5FD"));
        tempLinePaint.setStyle(Paint.Style.STROKE);
        tempLinePaint.setStrokeWidth(5f);

        previewRect = new RectF();

        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.parseColor("#081226"));

        for (Connection connection : connections.values()) {
            Node fromNode = nodes.get(connection.getFromNodeId());
            Node toNode = nodes.get(connection.getToNodeId());
            connection.draw(canvas, fromNode, toNode, scale, offsetX, offsetY);
        }

        if (pendingAction == PendingAction.CREATE_CONNECTION && pendingSourceNode != null) {
            float startX = (pendingSourceNode.getX() + offsetX + pendingSourceNode.getWidth() / 2f) * scale;
            float startY = (pendingSourceNode.getY() + offsetY + pendingSourceNode.getHeight() / 2f) * scale;
            tempLinePaint.setStrokeWidth(Math.max(3f, 4f * (0.85f + scale * 0.35f)));
            canvas.drawLine(startX, startY, pendingEndX, pendingEndY, tempLinePaint);
        }

        for (Node node : nodes.values()) {
            boolean isSearchResult = highlightSearchResults && searchResultNodeIds.contains(node.getId());
            node.draw(canvas, scale, offsetX, offsetY, isSearchResult, highlightSearchResults);

            if (isSearchResult) {
                drawSearchHighlight(canvas, node);
            }
        }

        if (previewNode != null) {
            drawPreviewCard(canvas, previewNode);
        }
    }

    private void drawSearchHighlight(Canvas canvas, Node node) {
        float left = (node.getX() + offsetX) * scale - 12f * scale;
        float top = (node.getY() + offsetY) * scale - 12f * scale;
        float right = left + node.getWidth() * scale + 24f * scale;
        float bottom = top + node.getHeight() * scale + 24f * scale;

        searchPaint.setStrokeWidth(Math.max(3f, 4f * scale));
        canvas.drawRoundRect(left, top, right, bottom, 24f * scale, 24f * scale, searchPaint);
    }

    private void drawPreviewCard(Canvas canvas, Node node) {
        float nodeLeft = (node.getX() + offsetX) * scale;
        float nodeTop = (node.getY() + offsetY) * scale;
        float nodeWidth = node.getWidth() * scale;
        float nodeHeight = node.getHeight() * scale;

        float cardWidth = Math.max(300f, nodeWidth * 1.15f);
        float cardHeight = Math.max(170f, nodeHeight * 1.25f);

        float left = nodeLeft + nodeWidth / 2f - cardWidth / 2f;
        float top = nodeTop + nodeHeight / 2f - cardHeight / 2f;
        float right = left + cardWidth;
        float bottom = top + cardHeight;

        if (left < 18f) {
            right += (18f - left);
            left = 18f;
        }
        if (right > getWidth() - 18f) {
            float diff = right - (getWidth() - 18f);
            left -= diff;
            right -= diff;
        }
        if (top < 18f) {
            bottom += (18f - top);
            top = 18f;
        }
        if (bottom > getHeight() - 18f) {
            float diff = bottom - (getHeight() - 18f);
            top -= diff;
            bottom -= diff;
        }

        previewRect.set(left, top, right, bottom);

        canvas.drawRoundRect(
                left + 8f,
                top + 10f,
                right + 8f,
                bottom + 10f,
                30f,
                30f,
                previewShadowPaint
        );

        canvas.drawRoundRect(previewRect, 28f, 28f, previewCardPaint);
        canvas.drawRoundRect(previewRect, 28f, 28f, previewBorderPaint);

        String title = node.getTitle() == null ? "" : node.getTitle();
        String content = node.getContent() == null ? "" : node.getContent();

        float paddingX = 24f;
        float y = top + 40f;

        previewTitlePaint.setTextSize(28f);
        previewContentPaint.setTextSize(20f);

        canvas.drawText(truncate(title, 18), left + paddingX, y, previewTitlePaint);

        y += 34f;
        List<String> lines = splitText(content, 22, 5);
        for (String line : lines) {
            canvas.drawText(line, left + paddingX, y, previewContentPaint);
            y += 28f;
        }
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private List<String> splitText(String text, int charsPerLine, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return lines;

        String normalized = text.replace("\n", " ");
        int start = 0;
        while (start < normalized.length() && lines.size() < maxLines) {
            int end = Math.min(start + charsPerLine, normalized.length());
            String line = normalized.substring(start, end);
            if (end < normalized.length() && lines.size() == maxLines - 1 && line.length() >= 2) {
                line = line.substring(0, line.length() - 1) + "…";
            }
            lines.add(line);
            start = end;
        }
        return lines;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                downX = x;
                downY = y;
                lastTouchX = x;
                lastTouchY = y;

                hasMovedBeyondTapSlop = false;
                isDraggingCanvas = false;
                isDraggingNode = false;

                touchDownNode = findNodeAt(x, y);
                draggingNode = null;

                Connection touchedConnection = findConnectionAt(x, y);

                if (touchDownNode != null) {
                    clearSelections();
                    touchDownNode.setSelected(true);
                    selectedNode = touchDownNode;
                    invalidate();
                } else if (touchedConnection != null) {
                    clearSelections();
                    touchedConnection.setSelected(true);
                    selectedConnection = touchedConnection;
                    invalidate();
                } else {
                    clearSelections();
                    invalidate();
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (isScaling) {
                    lastTouchX = x;
                    lastTouchY = y;
                    return true;
                }

                float rawDx = x - downX;
                float rawDy = y - downY;

                if (!hasMovedBeyondTapSlop) {
                    float dist2 = rawDx * rawDx + rawDy * rawDy;
                    if (dist2 > touchSlop * touchSlop) {
                        hasMovedBeyondTapSlop = true;

                        if (touchDownNode != null) {
                            draggingNode = touchDownNode;
                            draggingNode.setDragging(true);
                            isDraggingNode = true;
                        } else {
                            isDraggingCanvas = true;
                        }
                    }
                }

                float dx = (x - lastTouchX) / scale;
                float dy = (y - lastTouchY) / scale;

                if (pendingAction != PendingAction.NONE && pendingSourceNode != null) {
                    pendingEndX = x;
                    pendingEndY = y;
                    invalidate();
                }

                if (isDraggingNode && draggingNode != null) {
                    draggingNode.move(dx, dy);
                    invalidate();
                } else if (isDraggingCanvas) {
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
                if (!isScaling && !hasMovedBeyondTapSlop) {
                    Node tappedNode = findNodeAt(x, y);

                    if (pendingAction == PendingAction.CREATE_CONNECTION && pendingSourceNode != null) {
                        if (tappedNode != null && !pendingSourceNode.getId().equals(tappedNode.getId())) {
                            showEditConnectionLabelDialog(pendingSourceNode, tappedNode);
                            break;
                        }
                    }

                    if (tappedNode != null) {
                        if (previewNode != null && previewNode.getId().equals(tappedNode.getId())) {
                            previewNode = null;
                        } else {
                            previewNode = tappedNode;
                        }
                    } else {
                        previewNode = null;
                    }
                    invalidate();
                }

                if (draggingNode != null) {
                    draggingNode.setDragging(false);
                    draggingNode = null;
                    notifyDataChanged();
                }

                touchDownNode = null;
                isDraggingCanvas = false;
                isDraggingNode = false;
                hasMovedBeyondTapSlop = false;
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
            if (connection.isNear(x, y, from, to, scale, offsetX, offsetY, 18f)) {
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

    public void clearAll() {
        nodes.clear();
        connections.clear();
        selectedNode = null;
        selectedConnection = null;
        previewNode = null;
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

    public void setNodes(Map<String, Node> map) {
        nodes.clear();
        if (map != null) {
            nodes.putAll(map);
        }
        previewNode = null;
        invalidate();
    }

    public void setConnections(Map<String, Connection> map) {
        connections.clear();
        if (map != null) {
            connections.putAll(map);
        }
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
            if (!searchTypes.isEmpty() && !searchTypes.contains(node.getType())) {
                continue;
            }

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
        if (node != null) {
            focusNode(node);
        }
    }

    public void startConnectionMode(Node sourceNode) {
        pendingAction = PendingAction.CREATE_CONNECTION;
        pendingSourceNode = sourceNode;
        previewNode = null;
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

    private void showEditConnectionLabelDialog(Node from, Node to) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = 36;
        layout.setPadding(padding, padding, padding, padding);

        EditText input = new EditText(getContext());
        input.setHint("输入连线文字（可为空）");
        layout.addView(input);

        Spinner colorSpinner = new Spinner(getContext());
        String[] colorNames = {"默认蓝色", "绿色", "红色", "橙色", "紫色", "白色"};
        Integer[] colorValues = {
                null,
                Color.parseColor("#57D38C"),
                Color.parseColor("#FF6B6B"),
                Color.parseColor("#FFB84D"),
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
                if ((value == null && existingColor == null) || (value != null && value.equals(existingColor))) {
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
                })
                .show();
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Node node = findNodeAt(e.getX(), e.getY());
            if (node == null) {
                float worldX = e.getX() / scale - offsetX;
                float worldY = e.getY() / scale - offsetY;

                Node newNode = new Node("新节点", "输入内容", worldX - 110f, worldY - 65f, Node.NodeType.CONCEPT);
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
            if (hasMovedBeyondTapSlop || isScaling) return;

            Node touchedNode = findNodeAt(e.getX(), e.getY());

            if (pendingAction == PendingAction.CREATE_CONNECTION && pendingSourceNode != null) {
                if (touchedNode != null && !pendingSourceNode.getId().equals(touchedNode.getId())) {
                    showEditConnectionLabelDialog(pendingSourceNode, touchedNode);
                    return;
                }
            }

            if (touchedNode != null && getContext() instanceof MainActivity) {
                ((MainActivity) getContext()).showNodeEditDialog(touchedNode);
            }
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            isScaling = true;
            isDraggingCanvas = false;
            isDraggingNode = false;
            if (draggingNode != null) {
                draggingNode.setDragging(false);
                draggingNode = null;
            }
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float oldScale = scale;
            float newScale = oldScale * detector.getScaleFactor();

            if (newScale < 0.05f) newScale = 0.05f;
            if (newScale > 50f) newScale = 50f;

            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();

            float worldFocusX = (focusX / oldScale) - offsetX;
            float worldFocusY = (focusY / oldScale) - offsetY;

            scale = newScale;

            offsetX = (focusX / scale) - worldFocusX;
            offsetY = (focusY / scale) - worldFocusY;

            lastTouchX = focusX;
            lastTouchY = focusY;

            invalidate();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            isScaling = false;
        }
    }
}
