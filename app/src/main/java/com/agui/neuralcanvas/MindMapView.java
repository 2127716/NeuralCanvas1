package com.agui.neuralcanvas;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Looper;
import android.os.Build;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final Set<String> searchResultNodeIdSet = new HashSet<>();
    private final List<Node> nodeDrawCache = new ArrayList<>();
    private boolean nodeDrawCacheDirty = true;

    private float scale = 1.0f;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private static final float MIN_SCALE = 0.02f;
    private static final float MAX_SCALE = 120f;

    // ── Touch tracking ─────────────────────────────────────────────────────────
    private float downX = 0f;
    private float downY = 0f;
    /** Screen position of the *single* active pointer, updated every MOVE event. */
    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    /** True once cumulative displacement exceeds drag threshold. */
    private boolean movedEnough = false;

    private boolean isDraggingCanvas = false;
    private boolean isDraggingNode = false;
    /** True while two+ fingers are on screen (pinch-zoom). */
    private boolean isScaling = false;
    /**
     * True after a scale gesture or after the finger moved enough.
     * Blocks long-press and single-tap-triggered edits so they don't
     * fire accidentally right after a pinch.
     */
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
    private boolean viewportCacheDirty = true;
    private final List<Node> visibleNodeCache = new ArrayList<>();
    private final List<Connection> visibleConnectionCache = new ArrayList<>();
    private boolean renderPosted = false;
    private final Path gridPath = new Path();
    private boolean gridCacheDirty = true;
    private float gridCacheScale = -1f;
    private float gridCacheOffsetX = Float.NaN;
    private float gridCacheOffsetY = Float.NaN;
    private int gridCacheWidth = -1;
    private int gridCacheHeight = -1;
    private float cacheScale = Float.NaN;
    private float cacheOffsetX = Float.NaN;
    private float cacheOffsetY = Float.NaN;
    private int cacheWidth = -1;
    private int cacheHeight = -1;
    private static final int BG_COLOR = Color.parseColor("#070B14");
    private Paint tempLinePaint;
    private Paint gridPaint;
    private Paint searchHighlightPaint;

    private long lastScaleEndTime = 0L;
    private static final long LONG_PRESS_BLOCK_AFTER_SCALE_MS = 260L;

    private float minLongPressNodeScreenSizePx;
    private float longPressMoveTolerancePx;
    private static final float MIN_NODE_DRAG_EFFECTIVE_SCALE = 0.28f;
    private String pendingLongPressNodeId;
    private boolean pendingLongPressEligible = false;

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
        // Disable the built-in long-press inside GestureDetector; we handle it
        // ourselves below so we can guard against accidental triggers.
        gestureDetector.setIsLongpressEnabled(false);
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

        minLongPressNodeScreenSizePx = dp(44f);
        longPressMoveTolerancePx = dp(14f);

        previewCardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewCardPaint.setColor(Color.parseColor("#0A0F18"));

        previewBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewBorderPaint.setStyle(Paint.Style.STROKE);
        previewBorderPaint.setStrokeWidth(dp(1.2f));
        previewBorderPaint.setColor(Color.parseColor("#334155"));

        previewTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewTitlePaint.setColor(Color.parseColor("#F8FAFC"));
        previewTitlePaint.setTextSize(dp(15f));
        previewTitlePaint.setFakeBoldText(true);

        previewContentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewContentPaint.setColor(Color.parseColor("#A8B3CF"));
        previewContentPaint.setTextSize(dp(13f));

        previewShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        previewShadowPaint.setColor(Color.parseColor("#99000000"));

        tempLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tempLinePaint.setColor(Color.parseColor("#8B5CF6"));
        tempLinePaint.setStyle(Paint.Style.STROKE);
        tempLinePaint.setStrokeWidth(dp(2f));

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#162033"));
        gridPaint.setStrokeWidth(1f);

        searchHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        searchHighlightPaint.setStyle(Paint.Style.STROKE);
        searchHighlightPaint.setColor(Color.parseColor("#E9D5FF"));
        searchHighlightPaint.setAlpha(185);
    }

    public void selectOnlyNode(String nodeId) {
        clearSelections();
        if (nodeId != null && nodes.containsKey(nodeId)) {
            Node node = nodes.get(nodeId);
            node.setSelected(true);
            selectedNode = node;
        }
        requestRender();
    }

    public void requestRender() {
        if (renderPosted) return;
        renderPosted = true;
        Runnable renderRunnable = new Runnable() {
            @Override public void run() {
                renderPosted = false;
                invalidate();
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            postOnAnimation(renderRunnable);
        } else {
            post(renderRunnable);
        }
    }

    private void markNodeCacheDirty() {
        nodeDrawCacheDirty = true;
        viewportCacheDirty = true;
        gridCacheDirty = true;
    }

    private List<Node> getNodeDrawCache() {
        if (nodeDrawCacheDirty) {
            nodeDrawCache.clear();
            nodeDrawCache.addAll(nodes.values());
            nodeDrawCacheDirty = false;
        }
        return nodeDrawCache;
    }

    private float dp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewportCacheDirty = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(BG_COLOR);

        drawGrid(canvas);
        ensureViewportCaches();

        for (Connection connection : visibleConnectionCache) {
            Node fromNode = nodes.get(connection.getFromNodeId());
            Node toNode = nodes.get(connection.getToNodeId());
            if (fromNode == null || toNode == null) continue;
            connection.draw(canvas, fromNode, toNode, scale, offsetX, offsetY);
        }

        if (pendingAction == PendingAction.CREATE_CONNECTION && pendingSourceNode != null) {
            float startX = (pendingSourceNode.getX() + offsetX + pendingSourceNode.getWidth() / 2f) * scale;
            float startY = (pendingSourceNode.getY() + offsetY + pendingSourceNode.getHeight() / 2f) * scale;
            tempLinePaint.setStrokeWidth(Math.max(dp(1.5f), dp(2f) * (0.8f + scale * 0.25f)));
            canvas.drawLine(startX, startY, pendingEndX, pendingEndY, tempLinePaint);
        }

        for (Node node : visibleNodeCache) {
            node.draw(canvas, scale, offsetX, offsetY);

            if (highlightSearchResults && searchResultNodeIdSet.contains(node.getId())) {
                drawSearchHighlight(canvas, node);
            }
        }

        if (previewNode != null) {
            drawPreviewCard(canvas, previewNode);
        }
    }

    private void ensureViewportCaches() {
        if (!viewportCacheDirty
                && cacheScale == scale
                && cacheOffsetX == offsetX
                && cacheOffsetY == offsetY
                && cacheWidth == getWidth()
                && cacheHeight == getHeight()) {
            return;
        }

        visibleNodeCache.clear();
        for (Node node : getNodeDrawCache()) {
            if (isNodeVisible(node)) visibleNodeCache.add(node);
        }

        visibleConnectionCache.clear();
        for (Connection connection : connections.values()) {
            if (connection == null) continue;
            Node fromNode = nodes.get(connection.getFromNodeId());
            Node toNode = nodes.get(connection.getToNodeId());
            if (fromNode == null || toNode == null) continue;
            if (isConnectionLikelyVisible(fromNode, toNode)) visibleConnectionCache.add(connection);
        }

        viewportCacheDirty = false;
        cacheScale = scale;
        cacheOffsetX = offsetX;
        cacheOffsetY = offsetY;
        cacheWidth = getWidth();
        cacheHeight = getHeight();
    }

    private void drawGrid(Canvas canvas) {
        float base = 36f * scale;
        if (base < 18f) return;

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        if (gridCacheDirty || gridCacheScale != scale || gridCacheOffsetX != offsetX || gridCacheOffsetY != offsetY
                || gridCacheWidth != width || gridCacheHeight != height) {
            gridPath.reset();
            float startX = ((offsetX * scale) % base + base) % base;
            float startY = ((offsetY * scale) % base + base) % base;
            for (float x = startX; x < width; x += base) {
                gridPath.moveTo(x, 0f);
                gridPath.lineTo(x, height);
            }
            for (float y = startY; y < height; y += base) {
                gridPath.moveTo(0f, y);
                gridPath.lineTo(width, y);
            }
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
            if (lines.size() >= maxLines) break;

            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }

            StringBuilder current = new StringBuilder();
            for (int i = 0; i < paragraph.length(); i++) {
                current.append(paragraph.charAt(i));
                if (paint.measureText(current.toString()) > maxWidth) {
                    if (current.length() > 1) {
                        lines.add(current.substring(0, current.length() - 1));
                        current = new StringBuilder();
                        current.append(paragraph.charAt(i));
                    }
                    if (lines.size() >= maxLines) break;
                }
            }
            if (current.length() > 0 && lines.size() < maxLines) {
                lines.add(current.toString());
            }
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

    // ══════════════════════════════════════════════════════════════════════════
    //  TOUCH HANDLING — completely rewritten for reliability
    //
    //  Strategy:
    //  • ScaleGestureDetector is always fed events first so pinch works.
    //  • When pointer count > 1 we are in "scale mode"; no canvas/node drag.
    //  • Single-finger drag is handled directly in ACTION_MOVE with delta from
    //    the *previous* move position (lastTouchX/Y), NOT from down position.
    //    This is the most reliable approach and avoids the "stutter then jump"
    //    problem that occurred with the old code.
    //  • Long-press is detected manually via a postDelayed runnable instead of
    //    relying on GestureDetector's long-press (which is prone to false fires
    //    after scale gestures and multi-touch).
    // ══════════════════════════════════════════════════════════════════════════

    /** Long-press detection delay — slightly longer than Android default (500 ms). */
    private static final long LONG_PRESS_DELAY_MS = 520L;

    private final Runnable longPressRunnable = new Runnable() {
        @Override public void run() {
            if (!pendingLongPressEligible || isScaling || suppressLongPressUntilUp) return;
            Node node = getPendingLongPressNode();
            if (node != null) {
                if (shouldBlockNodeLongPress(node)) return;
                performLongPressHaptic();
                if (getContext() instanceof MainActivity) {
                    ((MainActivity) getContext()).showNodeEditDialog(node);
                }
                cancelLongPressCandidate();
            } else {
                // Maybe tapped a connection
                Connection c = findConnectionAt(downX, downY);
                if (c != null) {
                    long now = SystemClock.uptimeMillis();
                    if (now - lastScaleEndTime >= LONG_PRESS_BLOCK_AFTER_SCALE_MS) {
                        showEditExistingConnectionDialog(c);
                    }
                }
            }
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Always allow us to intercept events from parent scroll containers.
        getParent().requestDisallowInterceptTouchEvent(true);

        // Feed to scale detector first.
        scaleGestureDetector.onTouchEvent(event);

        final int action = event.getActionMasked();
        final int pointerCount = event.getPointerCount();

        // ── Multi-finger: pinch-zoom mode ──────────────────────────────────────
        if (pointerCount > 1) {
            enterScaleMode();
            // Update lastTouch to the primary pointer so that when the second
            // finger lifts we don't get a sudden jump.
            lastTouchX = event.getX(0);
            lastTouchY = event.getY(0);
            return true;
        }

        // ── Single finger ──────────────────────────────────────────────────────
        float x = event.getX();
        float y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                onSingleFingerDown(x, y);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (!isScaling) {
                    onSingleFingerMove(x, y);
                } else {
                    // Scale just ended on this pointer; update reference so the
                    // next MOVE doesn't produce a delta from a stale position.
                    lastTouchX = x;
                    lastTouchY = y;
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                onSingleFingerUp(x, y, action == MotionEvent.ACTION_UP);
                break;
            }

            // Second pointer was added (ACTION_POINTER_DOWN) — already covered
            // by the pointerCount > 1 branch above.
        }

        // Also feed single-touch events to GestureDetector for double-tap & tap.
        gestureDetector.onTouchEvent(event);

        return true;
    }

    private void onSingleFingerDown(float x, float y) {
        downX = x;
        downY = y;
        lastTouchX = x;
        lastTouchY = y;

        movedEnough = false;
        isDraggingCanvas = false;
        isDraggingNode = false;
        suppressLongPressUntilUp = false;
        cancelLongPressCandidate();

        // Check if touching the preview card (open it for edit on long-press).
        if (previewRect != null && previewNode != null && previewRect.contains(x, y)) {
            updateLongPressCandidate(previewNode);
            scheduleManualLongPress();
            return;
        }

        Node touchedNode = findNodeAt(x, y);
        Connection touchedConnection = (touchedNode == null) ? findConnectionAt(x, y) : null;

        clearSelections();

        if (touchedNode != null) {
            touchedNode.setSelected(true);
            selectedNode = touchedNode;
            draggingNode = shouldAllowDirectNodeDragAtCurrentScale() ? touchedNode : null;
            requestRender();
        } else if (touchedConnection != null) {
            touchedConnection.setSelected(true);
            selectedConnection = touchedConnection;
            draggingNode = null;
            requestRender();
        } else {
            draggingNode = null;
            previewNode = null;
            previewRect = null;
            requestRender();
        }

        updateLongPressCandidate(touchedNode);
        scheduleManualLongPress();
    }

    private void onSingleFingerMove(float x, float y) {
        float totalDx = x - downX;
        float totalDy = y - downY;
        double totalDist = Math.hypot(totalDx, totalDy);

        // Delta from *last* MOVE (or DOWN) — used for incremental drag.
        float dx = x - lastTouchX;
        float dy = y - lastTouchY;

        // Cancel long-press if finger moved too far.
        boolean pressingNode = draggingNode != null;
        float lpTolerance = pressingNode ? getNodeLongPressMoveTolerancePx() : longPressMoveTolerancePx;
        if (pendingLongPressEligible && totalDist > lpTolerance) {
            cancelLongPressCandidate();
            removeCallbacks(longPressRunnable);
            suppressLongPressUntilUp = true;
        }

        // Threshold to begin drag.
        float dragThreshold = pressingNode ? getNodeDragStartThresholdPx() : (float) touchSlop;
        if (!movedEnough && totalDist > dragThreshold) {
            movedEnough = true;
            suppressLongPressUntilUp = true;
            removeCallbacks(longPressRunnable);
        }

        // Update pending connection preview line.
        if (pendingAction != PendingAction.NONE && pendingSourceNode != null) {
            pendingEndX = x;
            pendingEndY = y;
            requestRender();
        }

        if (movedEnough && (dx != 0f || dy != 0f)) {
            if (draggingNode != null) {
                // Node drag
                float effectiveScale = Math.max(scale, MIN_NODE_DRAG_EFFECTIVE_SCALE);
                float damping = getNodeDragDamping();
                float ndx = (dx / effectiveScale) * damping;
                float ndy = (dy / effectiveScale) * damping;
                isDraggingNode = true;
                draggingNode.setDragging(true);
                previewNode = null;
                previewRect = null;
                draggingNode.move(ndx, ndy);
                viewportCacheDirty = true;
                requestRender();
            } else {
                // Canvas pan — translate by the *world-space* delta.
                isDraggingCanvas = true;
                offsetX += dx / scale;
                offsetY += dy / scale;
                viewportCacheDirty = true;
                requestRender();
            }
        }

        lastTouchX = x;
        lastTouchY = y;
    }

    private void onSingleFingerUp(float x, float y, boolean isUp) {
        removeCallbacks(longPressRunnable);

        if (draggingNode != null && isDraggingNode) {
            draggingNode.setDragging(false);
            notifyDataChanged();
        }

        if (!movedEnough && isUp) {
            performClick();
        }

        draggingNode = null;
        isDraggingCanvas = false;
        isDraggingNode = false;
        movedEnough = false;
        suppressLongPressUntilUp = false;
        cancelLongPressCandidate();
    }

    /** Called when two+ fingers are detected; cleans up single-finger state. */
    private void enterScaleMode() {
        if (!isScaling) {
            isScaling = true;
        }
        suppressLongPressUntilUp = true;
        removeCallbacks(longPressRunnable);
        cancelLongPressCandidate();
        if (draggingNode != null) {
            draggingNode.setDragging(false);
            draggingNode = null;
        }
        isDraggingCanvas = false;
        isDraggingNode = false;
        movedEnough = true;
        previewRect = null;
    }

    private void scheduleManualLongPress() {
        removeCallbacks(longPressRunnable);
        postDelayed(longPressRunnable, LONG_PRESS_DELAY_MS);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    // ── Gesture listener: handles double-tap and single-tap-confirmed ──────────

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (isScaling || suppressLongPressUntilUp) return false;

            float x = e.getX();
            float y = e.getY();
            Node node = findNodeAt(x, y);

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
                requestRender();
                return true;
            } else {
                if (previewNode != null) {
                    previewNode = null;
                    previewRect = null;
                    requestRender();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (isScaling) return true;

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
    }

    // ── Scale listener ─────────────────────────────────────────────────────────

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            enterScaleMode();
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float oldScale = scale;
            float newScale = oldScale * detector.getScaleFactor();
            newScale = Math.max(MIN_SCALE, Math.min(newScale, MAX_SCALE));

            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();

            // Keep the world point under the pinch focus stationary.
            float worldFocusX = (focusX / oldScale) - offsetX;
            float worldFocusY = (focusY / oldScale) - offsetY;

            scale = newScale;
            offsetX = (focusX / scale) - worldFocusX;
            offsetY = (focusY / scale) - worldFocusY;

            viewportCacheDirty = true;
            previewRect = null;
            requestRender();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            isScaling = false;
            lastScaleEndTime = SystemClock.uptimeMillis();
            // Reset lastTouch so the first single-finger MOVE after pinch doesn't jump.
            lastTouchX = Float.NaN;
            lastTouchY = Float.NaN;
        }
    }

    // ── Long-press candidate helpers ───────────────────────────────────────────

    private void updateLongPressCandidate(Node node) {
        pendingLongPressNodeId = node == null ? null : node.getId();
        pendingLongPressEligible = node != null;
    }

    private void cancelLongPressCandidate() {
        pendingLongPressEligible = false;
    }

    private Node getPendingLongPressNode() {
        if (!pendingLongPressEligible || pendingLongPressNodeId == null) return null;
        return nodes.get(pendingLongPressNodeId);
    }

    private float getNodeLongPressMoveTolerancePx() {
        if (scale >= 1.0f) return dp(16f);
        if (scale >= 0.6f) return dp(20f);
        if (scale >= 0.35f) return dp(26f);
        if (scale >= 0.22f) return dp(34f);
        return dp(42f);
    }

    private float getNodeDragStartThresholdPx() {
        if (scale >= 1.0f) return Math.max(touchSlop, dp(12f));
        if (scale >= 0.75f) return dp(18f);
        if (scale >= 0.55f) return dp(28f);
        if (scale >= 0.4f) return dp(40f);
        return dp(52f);
    }

    private boolean shouldAllowDirectNodeDragAtCurrentScale() {
        return scale >= 0.55f;
    }

    private float getNodeDragDamping() {
        if (scale >= 1.0f) return 1.0f;
        if (scale >= 0.75f) return 0.9f;
        if (scale >= 0.55f) return 0.75f;
        return 0.6f;
    }

    private void performLongPressHaptic() {
        try {
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator == null || !vibrator.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(18L);
            }
        } catch (Exception ignored) {
        }
    }

    private boolean shouldBlockNodeLongPress(Node node) {
        if (node == null) return true;
        if (isScaling) return true;
        if (suppressLongPressUntilUp) return true;
        long now = SystemClock.uptimeMillis();
        return now - lastScaleEndTime < LONG_PRESS_BLOCK_AFTER_SCALE_MS;
    }

    // ── Hit testing ────────────────────────────────────────────────────────────

    private void clearSelections() {
        for (Node node : nodes.values()) node.setSelected(false);
        for (Connection connection : connections.values()) connection.setSelected(false);
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
        List<Node> nodeList = getNodeDrawCache();
        // Reverse iteration = top-most node wins.
        for (int i = nodeList.size() - 1; i >= 0; i--) {
            Node node = nodeList.get(i);
            RectF rect = getNodeScreenRect(node);
            if (rect.contains(touchX, touchY)) return node;
        }
        return null;
    }

    private Connection findConnectionAt(float x, float y) {
        for (Connection connection : connections.values()) {
            Node from = nodes.get(connection.getFromNodeId());
            Node to = nodes.get(connection.getToNodeId());
            if (from == null || to == null) continue;
            if (connection.isNear(x, y, from, to, scale, offsetX, offsetY, dp(10f))) {
                return connection;
            }
        }
        return null;
    }

    // ── Node / connection management ───────────────────────────────────────────

    public void addNode(Node node) {
        nodes.put(node.getId(), node);
        markNodeCacheDirty();
        viewportCacheDirty = true;
        requestRender();
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
            for (String id : toRemove) removeConnection(id);

            if (previewNode != null && nodeId.equals(previewNode.getId())) {
                previewNode = null;
                previewRect = null;
            }

            nodes.remove(nodeId);
            markNodeCacheDirty();
            viewportCacheDirty = true;
            requestRender();
            notifyDataChanged();
        }
    }

    public void addConnection(Connection connection) {
        connections.put(connection.getId(), connection);

        Node fromNode = nodes.get(connection.getFromNodeId());
        Node toNode = nodes.get(connection.getToNodeId());

        if (fromNode != null) fromNode.addConnection(connection.getId());
        if (toNode != null) toNode.addConnection(connection.getId());

        viewportCacheDirty = true;
        requestRender();
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

            requestRender();
            notifyDataChanged();
        }
    }

    public void clearAll() {
        nodes.clear();
        connections.clear();
        markNodeCacheDirty();
        selectedNode = null;
        selectedConnection = null;
        previewNode = null;
        previewRect = null;
        pendingAction = PendingAction.NONE;
        pendingSourceNode = null;
        searchResultNodeIds.clear();
        searchResultNodeIdSet.clear();
        requestRender();
        notifyDataChanged();
    }

    public Map<String, Node> getNodes() { return new LinkedHashMap<>(nodes); }
    public Map<String, Connection> getConnections() { return new LinkedHashMap<>(connections); }
    public Map<String, Node> getNodesInternal() { return nodes; }
    public Map<String, Connection> getConnectionsInternal() { return connections; }

    public void setNodes(Map<String, Node> map) {
        nodes.clear();
        if (map != null) nodes.putAll(map);
        markNodeCacheDirty();
        previewNode = null;
        previewRect = null;
        requestRender();
    }

    public void setConnections(Map<String, Connection> map) {
        connections.clear();
        if (map != null) connections.putAll(map);
        requestRender();
    }

    // ── Search ─────────────────────────────────────────────────────────────────

    public void search(String keyword, List<Node.NodeType> types, boolean highlight) {
        keyword = keyword == null ? "" : keyword.trim();
        searchKeyword = keyword.toLowerCase();
        searchTypes = types != null ? types : new ArrayList<>();
        highlightSearchResults = highlight;
        searchResultNodeIds.clear();
        searchResultNodeIdSet.clear();

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
                searchResultNodeIdSet.add(node.getId());
            }
        }

        if (!searchResultNodeIds.isEmpty()) {
            focusNodeById(searchResultNodeIds.get(0));
        } else {
            requestRender();
        }
    }

    public void clearSearch() {
        searchKeyword = "";
        searchTypes = new ArrayList<>();
        highlightSearchResults = false;
        searchResultNodeIds.clear();
        searchResultNodeIdSet.clear();
        requestRender();
    }

    public int getSearchResultCount() { return searchResultNodeIds.size(); }

    // ── Focus / select helpers ─────────────────────────────────────────────────

    private void focusNode(Node node) {
        if (node == null || getWidth() == 0 || getHeight() == 0) return;
        float nodeCenterX = node.getX() + node.getWidth() / 2f;
        float nodeCenterY = node.getY() + node.getHeight() / 2f;
        offsetX = (getWidth() / (2f * scale)) - nodeCenterX;
        offsetY = (getHeight() / (2f * scale)) - nodeCenterY;
        previewNode = node;
        previewRect = null;
        requestRender();
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
            requestRender();
        }
    }

    public void selectOnlyNode2(String nodeId) { selectOnlyNode(nodeId); }

    public List<String> getSelectedNodeIds() {
        List<String> ids = new ArrayList<>();
        for (Node node : nodes.values()) {
            if (node.isSelected()) ids.add(node.getId());
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

    public void clearPreviewCard() {
        previewNode = null;
        previewRect = null;
        requestRender();
    }

    public void startConnectionMode(Node sourceNode) {
        pendingAction = PendingAction.CREATE_CONNECTION;
        pendingSourceNode = sourceNode;
        previewNode = null;
        previewRect = null;
        requestRender();
    }

    public void cancelPendingAction() {
        pendingAction = PendingAction.NONE;
        pendingSourceNode = null;
        requestRender();
    }

    private Connection findConnectionBetween(String fromId, String toId) {
        for (Connection c : connections.values()) {
            if (fromId.equals(c.getFromNodeId()) && toId.equals(c.getToNodeId())) return c;
        }
        return null;
    }

    // ── Connection edit dialog ─────────────────────────────────────────────────

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
                getContext(), android.R.layout.simple_spinner_item, typeNames);
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
                Color.parseColor("#34D399"),
                Color.parseColor("#FB7185"),
                Color.parseColor("#F59E0B"),
                Color.parseColor("#FBBF24"),
                Color.parseColor("#A78BFA"),
                Color.WHITE,
                Color.parseColor("#60A5FA")
        };

        ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, colorNames);
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
                getContext(), android.R.layout.simple_spinner_item, widthNames);
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
                .setNegativeButton("取消", (dialog, which) -> cancelPendingAction())
                .setPositiveButton("确定", (dialog, which) -> {
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
                        Connection c = new Connection(from.getId(), to.getId(), selectedType, label);
                        c.setCustomColor(selectedColor);
                        c.setStrokeWidth(selectedWidth);
                        addConnection(c);
                    }

                    cancelPendingAction();
                    requestRender();
                    if (changedOnly) notifyDataChanged();
                });

        if (existing != null) {
            builder.setNeutralButton("删除连线", (dialog, which) -> {
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

    // ── Box selection (框选) ───────────────────────────────────────────────────
    // These are called by MainMenuActionHandler; kept as no-ops if not yet
    // fully implemented so the project at least compiles.

    private boolean isBoxSelectionMode = false;
    private RectF boxSelectionRect = null;
    private float boxStartX = 0f;
    private float boxStartY = 0f;

    public void startBoxSelectionMode() {
        isBoxSelectionMode = true;
        boxSelectionRect = null;
        clearSelections();
        requestRender();
    }

    public void cancelBoxSelectionMode() {
        isBoxSelectionMode = false;
        boxSelectionRect = null;
        requestRender();
    }

    /**
     * Delete all currently selected nodes (and their connected edges).
     * @return number of nodes deleted
     */
    public int deleteSelectedNodes() {
        List<String> toDelete = getSelectedNodeIds();
        for (String id : toDelete) {
            removeNode(id);
        }
        return toDelete.size();
    }
}
