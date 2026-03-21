package com.agui.neuralcanvas;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
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
import java.util.Set;

public class MindMapView extends View {

    public interface OnDataChangeListener { void onDataChanged(); }
    private OnDataChangeListener onDataChangeListener;
    public void setOnDataChangeListener(OnDataChangeListener listener) { this.onDataChangeListener = listener; }
    private void notifyDataChanged() { if (onDataChangeListener != null) onDataChangeListener.onDataChanged(); }

    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final Map<String, Connection> connections = new LinkedHashMap<>();

    private final List<Node> nodeDrawCache = new ArrayList<>();
    private boolean nodeDrawCacheDirty = true;
    private final List<Node> visibleNodeCache = new ArrayList<>();
    private final List<Connection> visibleConnectionCache = new ArrayList<>();
    private boolean viewportCacheDirty = true;

    private final List<String> searchResultNodeIds = new ArrayList<>();
    private final Set<String> searchResultNodeIdSet = new LinkedHashSet<>();
    private boolean highlightSearchResults = false;

    private float scale = 1.0f;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private static final float MIN_SCALE = 0.18f;
    private static final float MAX_SCALE = 6.0f;

    private float downX = 0f;
    private float downY = 0f;
    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private boolean isDraggingCanvas = false;
    private boolean isDraggingNode = false;
    private boolean isScaling = false;
    private boolean movedEnough = false;
    private boolean suppressLongPressUntilUp = false;
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
    private Paint gridPaint;
    private Paint tempLinePaint;
    private Paint searchHighlightPaint;
    private Paint selectionFillPaint;
    private Paint selectionStrokePaint;
    private final Path gridPath = new Path();
    private boolean renderPosted = false;

    private float cacheScale = Float.NaN;
    private float cacheOffsetX = Float.NaN;
    private float cacheOffsetY = Float.NaN;
    private int cacheWidth = -1;
    private int cacheHeight = -1;
    private boolean gridCacheDirty = true;
    private float gridCacheScale = Float.NaN;
    private float gridCacheOffsetX = Float.NaN;
    private float gridCacheOffsetY = Float.NaN;
    private int gridCacheWidth = -1;
    private int gridCacheHeight = -1;

    private long lastScaleEndTime = 0L;
    private static final long LONG_PRESS_BLOCK_AFTER_SCALE_MS = 220L;
    private float longPressMoveTolerancePx;
    private static final float MIN_NODE_DRAG_EFFECTIVE_SCALE = 0.42f;
    private String pendingLongPressNodeId;
    private boolean pendingLongPressEligible = false;
    private boolean singleTapCandidate = false;

    private enum PendingAction { NONE, CREATE_CONNECTION }
    private PendingAction pendingAction = PendingAction.NONE;
    private Node pendingSourceNode = null;
    private float pendingEndX = 0f;
    private float pendingEndY = 0f;

    private boolean boxSelectionMode = false;
    private boolean isDrawingSelectionBox = false;
    private RectF selectionRect = null;
    private float selectionStartX = 0f;
    private float selectionStartY = 0f;

    public MindMapView(Context context) { super(context); touchSlop = ViewConfiguration.get(context).getScaledTouchSlop(); init(); }
    public MindMapView(Context context, AttributeSet attrs) { super(context, attrs); touchSlop = ViewConfiguration.get(context).getScaledTouchSlop(); init(); }
    public MindMapView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); touchSlop = ViewConfiguration.get(context).getScaledTouchSlop(); init(); }

    private void init() {
        setClickable(true);
        setFocusable(true);
        setLongClickable(true);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        gestureDetector = new GestureDetector(getContext(), new GestureListener());
        gestureDetector.setIsLongpressEnabled(true);
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        longPressMoveTolerancePx = dp(14f);

        previewCardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewBorderPaint.setStyle(Paint.Style.STROKE);
        previewBorderPaint.setStrokeWidth(dp(1.2f));
        previewTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewTitlePaint.setTextSize(dp(15f));
        previewTitlePaint.setFakeBoldText(true);
        previewContentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewContentPaint.setTextSize(dp(13f));
        previewShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStrokeWidth(1f);
        tempLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tempLinePaint.setStyle(Paint.Style.STROKE);
        tempLinePaint.setStrokeWidth(dp(2f));
        searchHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        searchHighlightPaint.setStyle(Paint.Style.STROKE);
        selectionFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectionFillPaint.setStyle(Paint.Style.FILL);
        selectionStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectionStrokePaint.setStyle(Paint.Style.STROKE);
        selectionStrokePaint.setStrokeWidth(dp(1.4f));
        applyTheme();
    }

    public void applyTheme() {
        previewCardPaint.setColor(ThemeManager.getSurface());
        previewBorderPaint.setColor(ThemeManager.getStroke());
        previewTitlePaint.setColor(ThemeManager.getTextPrimary());
        previewContentPaint.setColor(ThemeManager.getTextSecondary());
        previewShadowPaint.setColor(Color.parseColor(ThemeManager.isPureLightTheme() ? "#22000000" : "#77000000"));
        gridPaint.setColor(ThemeManager.getGridColor());
        tempLinePaint.setColor(ThemeManager.getAccent());
        searchHighlightPaint.setColor(ThemeManager.getAccent2());
        searchHighlightPaint.setAlpha(185);
        selectionFillPaint.setColor(ThemeManager.withAlpha(ThemeManager.getAccent(), 54));
        selectionStrokePaint.setColor(ThemeManager.getAccent());
        gridCacheDirty = true;
        requestRender();
    }

    private float dp(float value) { return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()); }

    public void requestRender() {
        if (renderPosted) return;
        renderPosted = true;
        Runnable renderRunnable = new Runnable() {
            @Override public void run() {
                renderPosted = false;
                invalidate();
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) postOnAnimation(renderRunnable); else post(renderRunnable);
    }

    private void markNodeCacheDirty() { nodeDrawCacheDirty = true; viewportCacheDirty = true; gridCacheDirty = true; }
    private List<Node> getNodeDrawCache() {
        if (nodeDrawCacheDirty) {
            nodeDrawCache.clear();
            nodeDrawCache.addAll(nodes.values());
            nodeDrawCacheDirty = false;
        }
        return nodeDrawCache;
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w,h,oldw,oldh);
        viewportCacheDirty = true;
        gridCacheDirty = true;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(ThemeManager.getBg());
        drawGrid(canvas);
        ensureViewportCaches();

        for (Connection connection : visibleConnectionCache) {
            if (connection == null) continue;
            Node fromNode = nodes.get(connection.getFromNodeId());
            Node toNode = nodes.get(connection.getToNodeId());
            if (fromNode == null || toNode == null) continue;
            connection.draw(canvas, fromNode, toNode, scale, offsetX, offsetY);
        }

        if (pendingAction == PendingAction.CREATE_CONNECTION && pendingSourceNode != null) {
            float startX = (pendingSourceNode.getX() + offsetX + pendingSourceNode.getWidth() / 2f) * scale;
            float startY = (pendingSourceNode.getY() + offsetY + pendingSourceNode.getHeight() / 2f) * scale;
            tempLinePaint.setStrokeWidth(Math.max(dp(1.6f), dp(2f)));
            canvas.drawLine(startX, startY, pendingEndX, pendingEndY, tempLinePaint);
        }

        for (Node node : visibleNodeCache) {
            if (node == null) continue;
            node.draw(canvas, scale, offsetX, offsetY);
            if (highlightSearchResults && searchResultNodeIdSet.contains(node.getId())) drawSearchHighlight(canvas, node);
        }

        if (previewNode != null) drawPreviewCard(canvas, previewNode);
        if (boxSelectionMode && selectionRect != null) {
            canvas.drawRoundRect(selectionRect, dp(12f), dp(12f), selectionFillPaint);
            canvas.drawRoundRect(selectionRect, dp(12f), dp(12f), selectionStrokePaint);
        }
    }

    private void ensureViewportCaches() {
        if (!viewportCacheDirty && cacheScale == scale && cacheOffsetX == offsetX && cacheOffsetY == offsetY && cacheWidth == getWidth() && cacheHeight == getHeight()) return;
        visibleNodeCache.clear();
        for (Node node : getNodeDrawCache()) if (node != null && isNodeVisible(node)) visibleNodeCache.add(node);
        visibleConnectionCache.clear();
        for (Connection connection : connections.values()) {
            if (connection == null) continue;
            Node from = nodes.get(connection.getFromNodeId());
            Node to = nodes.get(connection.getToNodeId());
            if (from == null || to == null) continue;
            if (isConnectionLikelyVisible(from, to)) visibleConnectionCache.add(connection);
        }
        viewportCacheDirty = false;
        cacheScale = scale;
        cacheOffsetX = offsetX;
        cacheOffsetY = offsetY;
        cacheWidth = getWidth();
        cacheHeight = getHeight();
    }

    private void drawGrid(Canvas canvas) {
        float spacing = 42f * scale;
        if (spacing < 20f) return;
        int width = getWidth(), height = getHeight();
        if (width <= 0 || height <= 0) return;
        if (gridCacheDirty || gridCacheScale != scale || gridCacheOffsetX != offsetX || gridCacheOffsetY != offsetY || gridCacheWidth != width || gridCacheHeight != height) {
            gridPath.reset();
            float startX = ((offsetX * scale) % spacing + spacing) % spacing;
            float startY = ((offsetY * scale) % spacing + spacing) % spacing;
            for (float x = startX; x < width; x += spacing) { gridPath.moveTo(x, 0f); gridPath.lineTo(x, height); }
            for (float y = startY; y < height; y += spacing) { gridPath.moveTo(0f, y); gridPath.lineTo(width, y); }
            gridCacheDirty = false;
            gridCacheScale = scale;
            gridCacheOffsetX = offsetX;
            gridCacheOffsetY = offsetY;
            gridCacheWidth = width;
            gridCacheHeight = height;
        }
        canvas.drawPath(gridPath, gridPaint);
    }

    private void drawSearchHighlight(Canvas canvas, Node node) {
        float left = (node.getX() + offsetX) * scale - 8f;
        float top = (node.getY() + offsetY) * scale - 8f;
        float right = left + node.getWidth() * scale + 16f;
        float bottom = top + node.getHeight() * scale + 16f;
        searchHighlightPaint.setStrokeWidth(Math.max(dp(1.4f), 2.5f));
        canvas.drawRoundRect(new RectF(left, top, right, bottom), dp(18f), dp(18f), searchHighlightPaint);
    }

    private void drawPreviewCard(Canvas canvas, Node node) {
        float nodeLeft = (node.getX() + offsetX) * scale;
        float nodeTop = (node.getY() + offsetY) * scale;
        float nodeWidth = node.getWidth() * scale;
        float nodeHeight = node.getHeight() * scale;
        float cardWidth = Math.max(dp(240f), Math.min(dp(360f), Math.max(nodeWidth * 1.15f, dp(240f))));
        float cardHeight = Math.max(dp(170f), Math.min(dp(320f), Math.max(nodeHeight * 1.12f, dp(170f))));
        float left = nodeLeft + nodeWidth / 2f - cardWidth / 2f;
        float top = nodeTop + nodeHeight / 2f - cardHeight / 2f;
        float right = left + cardWidth;
        float bottom = top + cardHeight;
        float margin = dp(10f);
        if (left < margin) { right += (margin - left); left = margin; }
        if (right > getWidth() - margin) { float diff = right - (getWidth() - margin); left -= diff; right -= diff; }
        if (top < margin + dp(52f)) { bottom += (margin + dp(52f) - top); top = margin + dp(52f); }
        if (bottom > getHeight() - margin) { float diff = bottom - (getHeight() - margin); top -= diff; bottom -= diff; }
        previewRect = new RectF(left, top, right, bottom);
        canvas.drawRoundRect(new RectF(left + dp(3f), top + dp(5f), right + dp(3f), bottom + dp(5f)), dp(18f), dp(18f), previewShadowPaint);
        canvas.drawRoundRect(previewRect, dp(18f), dp(18f), previewCardPaint);
        canvas.drawRoundRect(previewRect, dp(18f), dp(18f), previewBorderPaint);
        String title = node.getTitle() == null ? "" : node.getTitle();
        String content = node.getContent() == null ? "" : node.getContent();
        float paddingX = dp(16f), usableWidth = cardWidth - paddingX * 2f;
        float y = top + dp(26f);
        for (String line : wrapTextByWidth(title, previewTitlePaint, usableWidth, 2)) { canvas.drawText(line, left + paddingX, y, previewTitlePaint); y += dp(18f); }
        y += dp(6f);
        for (String line : wrapTextByWidth(content, previewContentPaint, usableWidth, 10)) {
            if (y > bottom - dp(18f)) break;
            canvas.drawText(line, left + paddingX, y, previewContentPaint);
            y += dp(17f);
        }
    }

    private List<String> wrapTextByWidth(String text, Paint paint, float maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return lines;
        String[] paragraphs = text.replace("\r", "").split("\n");
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) { if (lines.size() < maxLines) lines.add(""); if (lines.size() >= maxLines) break; continue; }
            int start = 0;
            while (start < paragraph.length()) {
                if (lines.size() >= maxLines) break;
                int end = start + 1;
                while (end <= paragraph.length() && paint.measureText(paragraph, start, end) <= maxWidth) end++;
                end--;
                if (end <= start) end = Math.min(start + 1, paragraph.length());
                String line = paragraph.substring(start, end);
                if (lines.size() == maxLines - 1 && end < paragraph.length()) {
                    while (paint.measureText(line + "…") > maxWidth && line.length() > 1) line = line.substring(0, line.length() - 1);
                    lines.add(line + "…");
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
        float fromLeft = (fromNode.getX() + offsetX) * scale, fromTop = (fromNode.getY() + offsetY) * scale;
        float fromRight = fromLeft + fromNode.getWidth() * scale, fromBottom = fromTop + fromNode.getHeight() * scale;
        float toLeft = (toNode.getX() + offsetX) * scale, toTop = (toNode.getY() + offsetY) * scale;
        float toRight = toLeft + toNode.getWidth() * scale, toBottom = toTop + toNode.getHeight() * scale;
        float minX = Math.min(fromLeft, toLeft), minY = Math.min(fromTop, toTop), maxX = Math.max(fromRight, toRight), maxY = Math.max(fromBottom, toBottom);
        float pad = dp(100f);
        return !(maxX < -pad || maxY < -pad || minX > getWidth() + pad || minY > getHeight() + pad);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
        scaleGestureDetector.onTouchEvent(event);

        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();

        if (boxSelectionMode) {
            float x = event.getX(Math.max(0, actionIndex));
            float y = event.getY(Math.max(0, actionIndex));
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    activePointerId = event.getPointerId(0);
                    isDrawingSelectionBox = true;
                    selectionStartX = x; selectionStartY = y;
                    selectionRect = new RectF(x, y, x, y);
                    requestRender();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (isDrawingSelectionBox && selectionRect != null) {
                        int idx = activePointerId == MotionEvent.INVALID_POINTER_ID ? 0 : event.findPointerIndex(activePointerId);
                        if (idx < 0) idx = 0;
                        float mx = event.getX(idx), my = event.getY(idx);
                        selectionRect.set(Math.min(selectionStartX, mx), Math.min(selectionStartY, my), Math.max(selectionStartX, mx), Math.max(selectionStartY, my));
                        requestRender();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (selectionRect != null) selectNodesInRect(selectionRect);
                    isDrawingSelectionBox = false;
                    boxSelectionMode = false;
                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                    requestRender();
                    return true;
            }
        }

        if (action == MotionEvent.ACTION_DOWN) {
            activePointerId = event.getPointerId(0);
        } else if (action == MotionEvent.ACTION_POINTER_UP) {
            int pointerId = event.getPointerId(actionIndex);
            if (pointerId == activePointerId) {
                int newIndex = actionIndex == 0 ? 1 : 0;
                if (newIndex < event.getPointerCount()) {
                    activePointerId = event.getPointerId(newIndex);
                    lastTouchX = event.getX(newIndex);
                    lastTouchY = event.getY(newIndex);
                    downX = lastTouchX;
                    downY = lastTouchY;
                } else {
                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                }
            }
        } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            activePointerId = MotionEvent.INVALID_POINTER_ID;
        }

        if (event.getPointerCount() > 1) {
            if (action == MotionEvent.ACTION_POINTER_UP && event.getPointerCount() - 1 <= 1) {
                isScaling = false;
                lastScaleEndTime = SystemClock.uptimeMillis();
                int remainingIndex = actionIndex == 0 ? 1 : 0;
                if (remainingIndex >= event.getPointerCount()) remainingIndex = 0;
                if (remainingIndex != actionIndex && remainingIndex < event.getPointerCount()) {
                    activePointerId = event.getPointerId(remainingIndex);
                    lastTouchX = event.getX(remainingIndex);
                    lastTouchY = event.getY(remainingIndex);
                    downX = lastTouchX;
                    downY = lastTouchY;
                } else {
                    lastTouchX = Float.NaN;
                    lastTouchY = Float.NaN;
                }
            } else {
                isScaling = true;
                suppressLongPressUntilUp = true;
                isDraggingCanvas = false;
                isDraggingNode = false;
                draggingNode = null;
                movedEnough = true;
                singleTapCandidate = false;
                cancelLongPressCandidate();
                int anchorIndex = activePointerId == MotionEvent.INVALID_POINTER_ID ? 0 : event.findPointerIndex(activePointerId);
                if (anchorIndex < 0 || anchorIndex == actionIndex) anchorIndex = 0;
                if (anchorIndex == actionIndex && event.getPointerCount() > 1) anchorIndex = 1;
                lastTouchX = event.getX(anchorIndex);
                lastTouchY = event.getY(anchorIndex);
                return true;
            }
        }

        int activeIndex = activePointerId == MotionEvent.INVALID_POINTER_ID ? 0 : event.findPointerIndex(activePointerId);
        if (activeIndex < 0) activeIndex = 0;
        float x = event.getX(activeIndex), y = event.getY(activeIndex);

        gestureDetector.onTouchEvent(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                downX = x;
                downY = y;
                lastTouchX = x;
                lastTouchY = y;
                movedEnough = false;
                isDraggingCanvas = false;
                isDraggingNode = false;
                singleTapCandidate = true;
                suppressLongPressUntilUp = SystemClock.uptimeMillis() - lastScaleEndTime < LONG_PRESS_BLOCK_AFTER_SCALE_MS;
                Node touchedNode = findNodeAt(x, y);
                Connection touchedConnection = touchedNode == null ? findConnectionAt(x, y) : null;
                updateLongPressCandidate(touchedNode);

                if (touchedNode != null) {
                    clearSelections();
                    touchedNode.setSelected(true);
                    selectedNode = touchedNode;
                    draggingNode = shouldAllowDirectNodeDragAtCurrentScale() ? touchedNode : null;
                    requestRender();
                } else if (touchedConnection != null) {
                    clearSelections();
                    touchedConnection.setSelected(true);
                    selectedConnection = touchedConnection;
                    draggingNode = null;
                    requestRender();
                } else {
                    draggingNode = null;
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (isScaling) {
                    lastTouchX = x;
                    lastTouchY = y;
                    return true;
                }
                if (Float.isNaN(lastTouchX) || Float.isNaN(lastTouchY)) {
                    lastTouchX = x;
                    lastTouchY = y;
                    return true;
                }
                float totalDx = x - downX, totalDy = y - downY;
                double moveDistance = Math.hypot(totalDx, totalDy);
                boolean pressingNode = draggingNode != null;
                float dragStartThreshold = pressingNode ? getNodeDragStartThresholdPx() : touchSlop;
                float lpTolerance = pressingNode ? getNodeLongPressMoveTolerancePx() : longPressMoveTolerancePx;
                if (pendingLongPressEligible && moveDistance > lpTolerance) { cancelLongPressCandidate(); suppressLongPressUntilUp = true; }
                if (!movedEnough && moveDistance > dragStartThreshold) {
                    movedEnough = true;
                    singleTapCandidate = false;
                    suppressLongPressUntilUp = true;
                    if (previewNode != null) { previewNode = null; previewRect = null; }
                    if (findNodeAt(downX, downY) == null && findConnectionAt(downX, downY) == null) clearSelections();
                }
                if (pendingAction != PendingAction.NONE && pendingSourceNode != null) { pendingEndX = x; pendingEndY = y; requestRender(); }
                if (movedEnough) {
                    float screenDx = x - lastTouchX, screenDy = y - lastTouchY;
                    if (Math.abs(screenDx) < 0.02f && Math.abs(screenDy) < 0.02f) {
                        lastTouchX = x; lastTouchY = y;
                        return true;
                    }
                    if (draggingNode != null) {
                        float effectiveScale = Math.max(scale, MIN_NODE_DRAG_EFFECTIVE_SCALE);
                        float dx = (screenDx / effectiveScale) * getNodeDragDamping(), dy = (screenDy / effectiveScale) * getNodeDragDamping();
                        if (dx != 0f || dy != 0f) {
                            isDraggingNode = true;
                            draggingNode.setDragging(true);
                            previewNode = null;
                            previewRect = null;
                            draggingNode.move(dx, dy);
                            viewportCacheDirty = true;
                            requestRender();
                        }
                    } else {
                        float dx = screenDx / scale, dy = screenDy / scale;
                        if (dx != 0f || dy != 0f) {
                            isDraggingCanvas = true;
                            offsetX += dx;
                            offsetY += dy;
                            previewRect = null;
                            viewportCacheDirty = true;
                            gridCacheDirty = true;
                            requestRender();
                        }
                    }
                }
                lastTouchX = x; lastTouchY = y;
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (draggingNode != null && isDraggingNode) {
                    draggingNode.setDragging(false);
                    notifyDataChanged();
                }
                if (singleTapCandidate && action == MotionEvent.ACTION_UP) performClick();
                draggingNode = null;
                isDraggingCanvas = false;
                isDraggingNode = false;
                movedEnough = false;
                singleTapCandidate = false;
                lastTouchX = Float.NaN;
                lastTouchY = Float.NaN;
                if (action == MotionEvent.ACTION_UP) postDelayed(new Runnable() { @Override public void run() { suppressLongPressUntilUp = false; } }, 30L); else suppressLongPressUntilUp = false;
                cancelLongPressCandidate();
                return true;
            }
            case MotionEvent.ACTION_POINTER_UP:
                return true;
        }
        return true;
    }

    @Override public boolean performClick() { return super.performClick(); }

    public void startBoxSelectionMode() {
        boxSelectionMode = true;
        isDrawingSelectionBox = false;
        selectionRect = null;
        previewNode = null;
        previewRect = null;
        requestRender();
    }

    public boolean isBoxSelectionMode() { return boxSelectionMode; }

    public void cancelBoxSelectionMode() {
        boxSelectionMode = false;
        isDrawingSelectionBox = false;
        selectionRect = null;
        requestRender();
    }

    public int deleteSelectedNodes() {
        List<String> ids = getSelectedNodeIds();
        for (String id : new ArrayList<>(ids)) removeNode(id);
        requestRender();
        return ids.size();
    }

    private void selectNodesInRect(RectF rect) {
        clearSelections();
        for (Node node : nodes.values()) {
            if (node == null) continue;
            RectF nodeRect = getNodeScreenRect(node);
            if (RectF.intersects(rect, nodeRect)) node.setSelected(true);
        }
        requestRender();
    }

    private void updateLongPressCandidate(Node node) { pendingLongPressNodeId = node == null ? null : node.getId(); pendingLongPressEligible = node != null; }
    private void cancelLongPressCandidate() { pendingLongPressEligible = false; }
    private Node getPendingLongPressNode() { if (!pendingLongPressEligible || pendingLongPressNodeId == null) return null; return nodes.get(pendingLongPressNodeId); }
    private float getNodeLongPressMoveTolerancePx() { return scale >= 1.0f ? dp(16f) : (scale >= 0.7f ? dp(20f) : dp(26f)); }
    private float getNodeDragStartThresholdPx() { return scale >= 1.0f ? Math.max(touchSlop, dp(12f)) : (scale >= 0.7f ? dp(18f) : dp(26f)); }
    private boolean shouldAllowDirectNodeDragAtCurrentScale() { return scale >= 0.58f; }
    private float getNodeDragDamping() { return scale >= 1.0f ? 1.0f : (scale >= 0.75f ? 0.8f : 0.62f); }

    private void performLongPressHaptic() {
        try {
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator == null || !vibrator.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE)); else vibrator.vibrate(18L);
        } catch (Exception ignored) {}
    }

    private void clearSelections() {
        for (Node node : nodes.values()) if (node != null) node.setSelected(false);
        for (Connection connection : connections.values()) if (connection != null) connection.setSelected(false);
        selectedNode = null;
        selectedConnection = null;
    }

    private RectF getNodeScreenRect(Node node) {
        float left = (node.getX() + offsetX) * scale;
        float top = (node.getY() + offsetY) * scale;
        float right = left + node.getWidth() * scale;
        float bottom = top + node.getHeight() * scale;
        return new RectF(left, top, right, bottom);
    }

    private Node findNodeAt(float touchX, float touchY) {
        List<Node> list = getNodeDrawCache();
        for (int i = list.size() - 1; i >= 0; i--) {
            Node node = list.get(i);
            if (node == null) continue;
            RectF rect = getNodeScreenRect(node);
            if (rect.contains(touchX, touchY)) return node;
        }
        return null;
    }

    private Connection findConnectionAt(float x, float y) {
        for (Connection connection : connections.values()) {
            if (connection == null) continue;
            Node from = nodes.get(connection.getFromNodeId());
            Node to = nodes.get(connection.getToNodeId());
            if (from == null || to == null) continue;
            if (connection.isNear(x, y, from, to, scale, offsetX, offsetY, dp(10f))) return connection;
        }
        return null;
    }

    private boolean shouldBlockNodeLongPress(Node node) { return node == null || isScaling || suppressLongPressUntilUp || (SystemClock.uptimeMillis() - lastScaleEndTime < LONG_PRESS_BLOCK_AFTER_SCALE_MS); }

    public void addNode(Node node) { if (node == null) return; nodes.put(node.getId(), node); markNodeCacheDirty(); requestRender(); notifyDataChanged(); }
    public void removeNode(String nodeId) {
        Node node = nodes.get(nodeId); if (node == null) return;
        List<String> toRemove = new ArrayList<>();
        for (Connection c : connections.values()) { if (c == null) continue; if (nodeId.equals(c.getFromNodeId()) || nodeId.equals(c.getToNodeId())) toRemove.add(c.getId()); }
        for (String id : toRemove) removeConnection(id);
        if (previewNode != null && nodeId.equals(previewNode.getId())) { previewNode = null; previewRect = null; }
        nodes.remove(nodeId); markNodeCacheDirty(); requestRender(); notifyDataChanged();
    }
    public void addConnection(Connection connection) {
        if (connection == null || connection.getId() == null) return;
        connections.put(connection.getId(), connection);
        Node from = nodes.get(connection.getFromNodeId()), to = nodes.get(connection.getToNodeId());
        if (from != null) from.addConnection(connection.getId()); if (to != null) to.addConnection(connection.getId());
        viewportCacheDirty = true; requestRender(); notifyDataChanged();
    }
    public void removeConnection(String connectionId) {
        Connection connection = connections.get(connectionId);
        if (connection == null) { connections.remove(connectionId); return; }
        Node from = nodes.get(connection.getFromNodeId()), to = nodes.get(connection.getToNodeId());
        if (from != null) from.removeConnection(connectionId); if (to != null) to.removeConnection(connectionId);
        connections.remove(connectionId); if (selectedConnection != null && connectionId.equals(selectedConnection.getId())) selectedConnection = null;
        viewportCacheDirty = true; requestRender(); notifyDataChanged();
    }
    public void clearAll() { nodes.clear(); connections.clear(); markNodeCacheDirty(); selectedNode = null; selectedConnection = null; previewNode = null; previewRect = null; pendingAction = PendingAction.NONE; pendingSourceNode = null; searchResultNodeIds.clear(); searchResultNodeIdSet.clear(); requestRender(); notifyDataChanged(); }
    public Map<String, Node> getNodes() { return new LinkedHashMap<>(nodes); }
    public Map<String, Connection> getConnections() { LinkedHashMap<String, Connection> safe = new LinkedHashMap<>(); for (Map.Entry<String, Connection> entry : connections.entrySet()) if (entry.getKey() != null && entry.getValue() != null) safe.put(entry.getKey(), entry.getValue()); return safe; }
    public Map<String, Node> getNodesInternal() { return nodes; }
    public Map<String, Connection> getConnectionsInternal() { return connections; }
    public void setNodes(Map<String, Node> map) { nodes.clear(); if (map != null) for (Map.Entry<String, Node> entry : map.entrySet()) if (entry.getKey() != null && entry.getValue() != null) nodes.put(entry.getKey(), entry.getValue()); markNodeCacheDirty(); previewNode = null; previewRect = null; requestRender(); }
    public void setConnections(Map<String, Connection> map) { connections.clear(); if (map != null) for (Map.Entry<String, Connection> entry : map.entrySet()) if (entry.getKey() != null && entry.getValue() != null) connections.put(entry.getKey(), entry.getValue()); viewportCacheDirty = true; requestRender(); }
    public void search(String keyword, List<Node.NodeType> types, boolean highlight) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Node.NodeType> searchTypes = types != null ? types : new ArrayList<>();
        highlightSearchResults = highlight; searchResultNodeIds.clear(); searchResultNodeIdSet.clear();
        if (normalized.isEmpty() && searchTypes.isEmpty()) { clearSearch(); return; }
        for (Node node : nodes.values()) {
            if (node == null) continue;
            if (!searchTypes.isEmpty() && !searchTypes.contains(node.getType())) continue;
            boolean matches = normalized.isEmpty() || (String.valueOf(node.getTitle()).toLowerCase().contains(normalized) || String.valueOf(node.getContent()).toLowerCase().contains(normalized));
            if (matches) { searchResultNodeIds.add(node.getId()); searchResultNodeIdSet.add(node.getId()); }
        }
        if (!searchResultNodeIds.isEmpty()) focusNodeById(searchResultNodeIds.get(0)); else requestRender();
    }
    public void clearSearch() { highlightSearchResults = false; searchResultNodeIds.clear(); searchResultNodeIdSet.clear(); requestRender(); }
    public int getSearchResultCount() { return searchResultNodeIds.size(); }
    private void focusNode(Node node) { if (node == null || getWidth() == 0 || getHeight() == 0) return; float nodeCenterX = node.getX() + node.getWidth() / 2f, nodeCenterY = node.getY() + node.getHeight() / 2f; offsetX = (getWidth() / (2f * scale)) - nodeCenterX; offsetY = (getHeight() / (2f * scale)) - nodeCenterY; previewNode = node; previewRect = null; viewportCacheDirty = true; gridCacheDirty = true; requestRender(); }
    public void focusNodeById(String nodeId) { Node node = nodes.get(nodeId); if (node != null) focusNode(node); }
    public void selectNodeById(String nodeId) { clearSelections(); Node node = nodes.get(nodeId); if (node != null) { node.setSelected(true); selectedNode = node; requestRender(); } }
    public void selectOnlyNode(String nodeId) { clearSelections(); if (nodeId != null && nodes.containsKey(nodeId)) { Node node = nodes.get(nodeId); if (node != null) { node.setSelected(true); selectedNode = node; } } requestRender(); }
    public List<String> getSelectedNodeIds() { List<String> ids = new ArrayList<>(); for (Node node : nodes.values()) if (node != null && node.isSelected()) ids.add(node.getId()); return ids; }

    public AiGraphSnapshot getSelectedGraphSnapshot() {
        LinkedHashSet<String> selectedIds = new LinkedHashSet<>(getSelectedNodeIds());
        AiGraphSnapshot snapshot = new AiGraphSnapshot();
        if (selectedIds.isEmpty()) return AiGraphSnapshot.from(nodes, connections);
        for (String nodeId : selectedIds) {
            Node node = nodes.get(nodeId); if (node == null) continue;
            AiGraphSnapshot.SnapshotNode item = new AiGraphSnapshot.SnapshotNode();
            item.id = node.getId(); item.title = node.getTitle(); item.content = node.getContent(); item.type = node.getType() == null ? "" : node.getType().name(); item.shape = node.getShape() == null ? "" : node.getShape().name(); item.x = node.getX(); item.y = node.getY(); item.width = node.getWidth(); item.height = node.getHeight(); item.connectionIds = new ArrayList<>(node.getConnectionIds()); snapshot.nodes.add(item);
        }
        for (Connection c : connections.values()) {
            if (c == null) continue; if (!selectedIds.contains(c.getFromNodeId()) || !selectedIds.contains(c.getToNodeId())) continue;
            AiGraphSnapshot.SnapshotConnection item = new AiGraphSnapshot.SnapshotConnection();
            item.id = c.getId(); item.fromNodeId = c.getFromNodeId(); item.toNodeId = c.getToNodeId(); item.type = c.getType() == null ? "" : c.getType().name(); item.label = c.getLabel(); item.strokeWidth = c.getStrokeWidth(); item.customColor = c.getCustomColor(); item.directed = true; snapshot.connections.add(item);
        }
        return snapshot;
    }

    public void clearPreviewCard() { previewNode = null; previewRect = null; requestRender(); }
    public void startConnectionMode(Node sourceNode) { pendingAction = PendingAction.CREATE_CONNECTION; pendingSourceNode = sourceNode; previewNode = null; previewRect = null; requestRender(); }
    public void cancelPendingAction() { pendingAction = PendingAction.NONE; pendingSourceNode = null; requestRender(); }
    private Connection findConnectionBetween(String fromId, String toId) { for (Connection c : connections.values()) { if (c == null) continue; if (fromId.equals(c.getFromNodeId()) && toId.equals(c.getToNodeId())) return c; } return null; }

    private void showEditConnectionDialog(Node from, Node to) {
        LinearLayout layout = new LinearLayout(getContext()); layout.setOrientation(LinearLayout.VERTICAL); int padding = (int) dp(18f); layout.setPadding(padding, padding, padding, padding);
        EditText input = new EditText(getContext()); input.setHint("输入连线文字（可为空）"); LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); inputParams.bottomMargin = (int) dp(14f); input.setLayoutParams(inputParams); layout.addView(input);
        Spinner typeSpinner = new Spinner(getContext()); LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); typeParams.bottomMargin = (int) dp(14f); typeSpinner.setLayoutParams(typeParams);
        String[] typeNames = new String[Connection.ConnectionType.values().length]; for (int i = 0; i < Connection.ConnectionType.values().length; i++) typeNames[i] = Connection.ConnectionType.values()[i].label;
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, typeNames); typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); typeSpinner.setAdapter(typeAdapter); layout.addView(typeSpinner);
        Spinner colorSpinner = new Spinner(getContext()); LinearLayout.LayoutParams colorParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); colorParams.bottomMargin = (int) dp(14f); colorSpinner.setLayoutParams(colorParams);
        String[] colorNames = {"默认跟随类型颜色", "绿色", "红色", "橙色", "黄色", "紫色", "白色", "蓝色"};
        Integer[] colorValues = {null, Color.parseColor("#34D399"), Color.parseColor("#FB7185"), Color.parseColor("#F59E0B"), Color.parseColor("#FBBF24"), Color.parseColor("#A78BFA"), Color.WHITE, Color.parseColor("#60A5FA")};
        ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, colorNames); colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); colorSpinner.setAdapter(colorAdapter); layout.addView(colorSpinner);
        Spinner widthSpinner = new Spinner(getContext()); String[] widthNames = {"细", "中", "粗", "超粗"}; float[] widthValues = {4f, 6f, 8f, 10f}; ArrayAdapter<String> widthAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, widthNames); widthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); widthSpinner.setAdapter(widthAdapter); layout.addView(widthSpinner);
        Connection existing = findConnectionBetween(from.getId(), to.getId());
        if (existing != null) {
            input.setText(existing.getLabel() == null ? "" : existing.getLabel());
            int typeIndex = 0; for (int i = 0; i < Connection.ConnectionType.values().length; i++) if (Connection.ConnectionType.values()[i] == existing.getType()) { typeIndex = i; break; } typeSpinner.setSelection(typeIndex);
            Integer existingColor = existing.getCustomColor(); int colorIndex = 0; for (int i = 0; i < colorValues.length; i++) { Integer value = colorValues[i]; if ((value == null && existingColor == null) || (value != null && value.equals(existingColor))) { colorIndex = i; break; } } colorSpinner.setSelection(colorIndex);
            float w = existing.getStrokeWidth(); int widthIndex = 0; if (w >= 10f) widthIndex = 3; else if (w >= 8f) widthIndex = 2; else if (w >= 6f) widthIndex = 1; widthSpinner.setSelection(widthIndex);
        } else typeSpinner.setSelection(Connection.ConnectionType.LEADS_TO.ordinal());
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext()).setTitle(existing != null ? "编辑连线" : "新建连线").setView(layout).setNegativeButton("取消", (dialog, which) -> cancelPendingAction()).setPositiveButton("确定", (dialog, which) -> {
            String label = input.getText().toString().trim(); Connection.ConnectionType selectedType = Connection.ConnectionType.values()[typeSpinner.getSelectedItemPosition()]; Integer selectedColor = colorValues[colorSpinner.getSelectedItemPosition()]; float selectedWidth = widthValues[widthSpinner.getSelectedItemPosition()];
            Connection ex = findConnectionBetween(from.getId(), to.getId()); boolean changedOnly = false;
            if (ex != null) { ex.setType(selectedType); ex.setLabel(label); ex.setCustomColor(selectedColor); ex.setStrokeWidth(selectedWidth); changedOnly = true; }
            else { Connection c = new Connection(from.getId(), to.getId(), selectedType, label); c.setCustomColor(selectedColor); c.setStrokeWidth(selectedWidth); addConnection(c); }
            cancelPendingAction(); viewportCacheDirty = true; requestRender(); if (changedOnly) notifyDataChanged();
        });
        if (existing != null) builder.setNeutralButton("删除连线", (dialog, which) -> { removeConnection(existing.getId()); cancelPendingAction(); });
        builder.show();
    }

    private void showEditExistingConnectionDialog(Connection connection) { if (connection == null) return; Node from = nodes.get(connection.getFromNodeId()); Node to = nodes.get(connection.getToNodeId()); if (from == null || to == null) return; showEditConnectionDialog(from, to); }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override public boolean onDown(MotionEvent e) { return true; }

        @Override public boolean onSingleTapConfirmed(MotionEvent e) {
            if (isScaling) return true;
            Node node = findNodeAt(e.getX(), e.getY());
            if (pendingAction == PendingAction.CREATE_CONNECTION && pendingSourceNode != null) {
                if (node != null && !pendingSourceNode.getId().equals(node.getId())) { showEditConnectionDialog(pendingSourceNode, node); return true; }
            }
            if (node != null) {
                clearSelections();
                node.setSelected(true);
                selectedNode = node;
                if (previewNode != null && previewNode.getId().equals(node.getId())) { previewNode = null; previewRect = null; }
                else { previewNode = node; previewRect = null; }
                requestRender();
                return true;
            } else if (previewNode != null) {
                previewNode = null;
                previewRect = null;
                requestRender();
                return true;
            } else {
                clearSelections();
                requestRender();
            }
            return super.onSingleTapConfirmed(e);
        }

        @Override public boolean onDoubleTap(MotionEvent e) {
            if (isScaling) return true;
            Node node = findNodeAt(e.getX(), e.getY());
            if (node == null) {
                float worldX = e.getX() / scale - offsetX, worldY = e.getY() / scale - offsetY;
                Node newNode = new Node("新节点", "输入内容", worldX - 84f, worldY - 84f, Node.NodeType.CONCEPT);
                addNode(newNode);
                clearSelections();
                newNode.setSelected(true);
                selectedNode = newNode;
                if (getContext() instanceof MainActivity) ((MainActivity) getContext()).showNodeEditDialog(newNode);
                return true;
            }
            return false;
        }

        @Override public void onLongPress(MotionEvent e) {
            if (isScaling || suppressLongPressUntilUp) return;
            Node touchedNode = getPendingLongPressNode(); if (touchedNode == null) touchedNode = findNodeAt(e.getX(), e.getY());
            if (pendingAction == PendingAction.CREATE_CONNECTION && pendingSourceNode != null) {
                if (touchedNode != null && !pendingSourceNode.getId().equals(touchedNode.getId())) { showEditConnectionDialog(pendingSourceNode, touchedNode); return; }
            }
            if (touchedNode != null) {
                if (shouldBlockNodeLongPress(touchedNode)) return;
                if (getContext() instanceof MainActivity) { performLongPressHaptic(); ((MainActivity) getContext()).showNodeEditDialog(touchedNode); }
                cancelLongPressCandidate(); return;
            }
            Connection touchedConnection = findConnectionAt(e.getX(), e.getY());
            if (touchedConnection != null) {
                if (SystemClock.uptimeMillis() - lastScaleEndTime < LONG_PRESS_BLOCK_AFTER_SCALE_MS) return;
                showEditExistingConnectionDialog(touchedConnection);
            }
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override public boolean onScaleBegin(ScaleGestureDetector detector) { isScaling = true; suppressLongPressUntilUp = true; draggingNode = null; isDraggingCanvas = false; isDraggingNode = false; previewRect = null; cancelLongPressCandidate(); return true; }
        @Override public boolean onScale(ScaleGestureDetector detector) {
            float oldScale = scale, newScale = oldScale * detector.getScaleFactor(); newScale = Math.max(MIN_SCALE, Math.min(newScale, MAX_SCALE));
            float focusX = detector.getFocusX(), focusY = detector.getFocusY();
            float worldFocusX = (focusX / oldScale) - offsetX, worldFocusY = (focusY / oldScale) - offsetY;
            scale = newScale; offsetX = (focusX / scale) - worldFocusX; offsetY = (focusY / scale) - worldFocusY;
            previewRect = null; viewportCacheDirty = true; gridCacheDirty = true; requestRender(); return true;
        }
        @Override public void onScaleEnd(ScaleGestureDetector detector) { isScaling = false; lastScaleEndTime = SystemClock.uptimeMillis(); lastTouchX = Float.NaN; lastTouchY = Float.NaN; }
    }
}
