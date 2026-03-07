package com.agui.neuralcanvas;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Node {

    public enum NodeType {
        CONCEPT("#4FC3F7", "概念", 0),
        IDEA("#FFCA28", "想法", 1),
        QUESTION("#EF5350", "问题", 2),
        RESOURCE("#66BB6A", "资源", 3),
        TASK("#AB47BC", "任务", 4),
        GOAL("#26A69A", "目标", 5),
        NOTE("#FFA726", "笔记", 6),
        DECISION("#5C6BC0", "决策", 7),

        // 兼容旧版本
        PROBLEM("#EF5350", "问题", 2),
        PERSON("#4FC3F7", "人物", 0),
        EVENT("#FFA726", "事件", 6),
        RISK("#EF5350", "风险", 2);

        public final String colorHex;
        public final String label;
        public final int styleIndex;

        NodeType(String colorHex, String label, int styleIndex) {
            this.colorHex = colorHex;
            this.label = label;
            this.styleIndex = styleIndex;
        }

        public static NodeType fromIndex(int index) {
            switch (index) {
                case 0: return CONCEPT;
                case 1: return IDEA;
                case 2: return QUESTION;
                case 3: return RESOURCE;
                case 4: return TASK;
                case 5: return GOAL;
                case 6: return NOTE;
                case 7: return DECISION;
                default: return CONCEPT;
            }
        }
    }

    public enum NodeShape {
        RECT("正方形"),
        CIRCLE("圆形"),
        OVAL("椭圆"),
        DIAMOND("菱形"),
        TRIANGLE("三角形"),
        PENTAGON("五边形"),
        HEXAGON("六边形");

        public final String label;

        NodeShape(String label) {
            this.label = label;
        }
    }

    private String id;
    private String title;
    private String content;
    private float x;
    private float y;
    private float width;
    private float height;
    private NodeType type;
    private NodeShape shape = NodeShape.RECT;
    private boolean selected;
    private boolean dragging;
    private List<String> connectionIds;

    private transient Paint fillPaint;
    private transient Paint strokePaint;
    private transient Paint titlePaint;
    private transient Paint contentPaint;
    private transient Paint selectedPaint;
    private transient Paint highlightPaint;
    private transient Paint badgePaint;
    private transient Paint badgeTextPaint;

    private transient RectF drawRect;
    private transient RectF shapeRect;
    private transient Path polygonPath;

    public Node() {
        this.id = UUID.randomUUID().toString();
        this.title = "";
        this.content = "";
        this.x = 100;
        this.y = 100;
        this.width = 220;
        this.height = 130;
        this.type = NodeType.CONCEPT;
        this.shape = NodeShape.RECT;
        this.selected = false;
        this.dragging = false;
        this.connectionIds = new ArrayList<>();
        ensurePaints();
    }

    public Node(String title, String content, float x, float y, NodeType type) {
        this.id = UUID.randomUUID().toString();
        this.title = title == null ? "" : title;
        this.content = content == null ? "" : content;
        this.x = x;
        this.y = y;
        this.width = 220;
        this.height = 130;
        this.type = type == null ? NodeType.CONCEPT : type;
        this.shape = NodeShape.RECT;
        this.selected = false;
        this.dragging = false;
        this.connectionIds = new ArrayList<>();
        ensurePaints();
    }

    // 兼容旧版构造参数顺序
    public Node(String title, String content, NodeType type, float x, float y) {
        this(title, content, x, y, type);
    }

    private void ensurePaints() {
        if (fillPaint == null) {
            fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            fillPaint.setStyle(Paint.Style.FILL);
        }

        if (strokePaint == null) {
            strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(3f);
        }

        if (titlePaint == null) {
            titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            titlePaint.setColor(Color.WHITE);
            titlePaint.setTextSize(28f);
            titlePaint.setFakeBoldText(true);
        }

        if (contentPaint == null) {
            contentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            contentPaint.setColor(Color.parseColor("#F8FAFC"));
            contentPaint.setTextSize(20f);
        }

        if (selectedPaint == null) {
            selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            selectedPaint.setStyle(Paint.Style.STROKE);
            selectedPaint.setColor(Color.WHITE);
            selectedPaint.setStrokeWidth(6f);
            selectedPaint.setAlpha(190);
        }

        if (highlightPaint == null) {
            highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            highlightPaint.setStyle(Paint.Style.STROKE);
            highlightPaint.setColor(Color.parseColor("#FFF176"));
            highlightPaint.setStrokeWidth(8f);
            highlightPaint.setAlpha(220);
        }

        if (badgePaint == null) {
            badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            badgePaint.setStyle(Paint.Style.FILL);
        }

        if (badgeTextPaint == null) {
            badgeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            badgeTextPaint.setColor(Color.WHITE);
            badgeTextPaint.setFakeBoldText(true);
            badgeTextPaint.setTextAlign(Paint.Align.CENTER);
        }

        if (drawRect == null) drawRect = new RectF();
        if (shapeRect == null) shapeRect = new RectF();
        if (polygonPath == null) polygonPath = new Path();

        applyTypeStyle();
    }

    private void applyTypeStyle() {
        int baseColor;
        try {
            baseColor = Color.parseColor(type != null ? type.colorHex : "#4FC3F7");
        } catch (Exception e) {
            baseColor = Color.parseColor("#4FC3F7");
        }

        fillPaint.setColor(baseColor);
        strokePaint.setColor(adjustColorBrightness(baseColor, 0.78f));
        badgePaint.setColor(adjustColorBrightness(baseColor, 0.88f));
    }

    private int adjustColorBrightness(int color, float factor) {
        int r = Math.max(0, Math.min(255, (int) (Color.red(color) * factor)));
        int g = Math.max(0, Math.min(255, (int) (Color.green(color) * factor)));
        int b = Math.max(0, Math.min(255, (int) (Color.blue(color) * factor)));
        return Color.rgb(r, g, b);
    }

    public void draw(Canvas canvas, float scale, float offsetX, float offsetY) {
        drawInternal(canvas, scale, offsetX, offsetY, false, false);
    }

    public void draw(Canvas canvas, float scale, float offsetX, float offsetY,
                     boolean isSearchResult, boolean highlightSearchResults) {
        drawInternal(canvas, scale, offsetX, offsetY, isSearchResult, highlightSearchResults);
    }

    private void drawInternal(Canvas canvas, float scale, float offsetX, float offsetY,
                              boolean isSearchResult, boolean highlightSearchResults) {
        ensurePaints();

        float drawX = (x + offsetX) * scale;
        float drawY = (y + offsetY) * scale;
        float drawW = width * scale;
        float drawH = height * scale;

        drawRect.set(drawX, drawY, drawX + drawW, drawY + drawH);
        computeShapeRect(drawRect, getShape(), shapeRect);

        if (selected) {
            drawShapeOutline(canvas, shapeRect, selectedPaint, scale);
        }

        if (highlightSearchResults && isSearchResult) {
            drawShapeOutline(canvas, shapeRect, highlightPaint, scale);
        }

        drawShape(canvas, shapeRect, fillPaint, scale);
        drawShapeOutline(canvas, shapeRect, strokePaint, scale);

        drawTexts(canvas, shapeRect, scale);
        drawTypeBadge(canvas, shapeRect, scale);
    }

    private void computeShapeRect(RectF baseRect, NodeShape shape, RectF outRect) {
        if (shape == NodeShape.OVAL) {
            outRect.set(baseRect);
            return;
        }

        float size = Math.min(baseRect.width(), baseRect.height());
        float cx = baseRect.centerX();
        float cy = baseRect.centerY();

        float half = size / 2f;
        outRect.set(cx - half, cy - half, cx + half, cy + half);
    }

    private void drawTexts(Canvas canvas, RectF rect, float scale) {
        float padding = Math.max(10f, 12f * scale);
        float titleSize = Math.max(20f, 26f * scale);
        float contentSize = Math.max(14f, 18f * scale);

        titlePaint.setTextSize(titleSize);
        contentPaint.setTextSize(contentSize);

        String safeTitle = title == null ? "" : title;
        String safeContent = content == null ? "" : content;

        float contentLeft = rect.left + padding;
        float titleY = rect.top + Math.max(26f, 30f * scale);

        canvas.drawText(
                truncateText(safeTitle, 12),
                contentLeft,
                titleY,
                titlePaint
        );

        List<String> lines = splitLines(safeContent, 12, 2);
        float lineY = titleY + Math.max(20f, 22f * scale);
        for (String line : lines) {
            canvas.drawText(line, contentLeft, lineY, contentPaint);
            lineY += Math.max(18f, 20f * scale);
        }
    }

    private void drawTypeBadge(Canvas canvas, RectF rect, float scale) {
        String badgeText = getType().label;
        if (badgeText == null || badgeText.trim().isEmpty()) return;

        float textSize = Math.max(11f, 12f * scale);
        badgeTextPaint.setTextSize(textSize);

        float textWidth = badgeTextPaint.measureText(badgeText);
        float padX = Math.max(7f, 8f * scale);
        float badgeHeight = Math.max(16f, 18f * scale);
        float badgeWidth = textWidth + padX * 2f;

        float left = rect.left + Math.max(10f, 10f * scale);
        float bottom = rect.bottom - Math.max(8f, 8f * scale);
        float top = bottom - badgeHeight;
        float right = left + badgeWidth;

        RectF badgeRect = new RectF(left, top, right, bottom);
        canvas.drawRoundRect(
                badgeRect,
                Math.max(7f, 8f * scale),
                Math.max(7f, 8f * scale),
                badgePaint
        );

        float textY = badgeRect.centerY() - ((badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2f);
        canvas.drawText(badgeText, badgeRect.centerX(), textY, badgeTextPaint);
    }

    private void drawShape(Canvas canvas, RectF rect, Paint paint, float scale) {
        switch (getShape()) {
            case CIRCLE:
                canvas.drawCircle(rect.centerX(), rect.centerY(), rect.width() / 2f, paint);
                break;
            case OVAL:
                canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, paint);
                break;
            case DIAMOND:
                canvas.drawPath(buildDiamondPath(rect), paint);
                break;
            case TRIANGLE:
                canvas.drawPath(buildRegularPolygonPath(rect, 3, -90f), paint);
                break;
            case PENTAGON:
                canvas.drawPath(buildRegularPolygonPath(rect, 5, -90f), paint);
                break;
            case HEXAGON:
                canvas.drawPath(buildRegularPolygonPath(rect, 6, -90f), paint);
                break;
            case RECT:
            default:
                canvas.drawRoundRect(rect, Math.max(10f, 14f * scale), Math.max(10f, 14f * scale), paint);
                break;
        }
    }

    private void drawShapeOutline(Canvas canvas, RectF rect, Paint paint, float scale) {
        Paint p = paint;
        p.setStrokeWidth(Math.max(2f, paint.getStrokeWidth() * Math.max(0.85f, scale * 0.7f)));

        switch (getShape()) {
            case CIRCLE:
                canvas.drawCircle(rect.centerX(), rect.centerY(), rect.width() / 2f, p);
                break;
            case OVAL:
                canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, p);
                break;
            case DIAMOND:
                canvas.drawPath(buildDiamondPath(rect), p);
                break;
            case TRIANGLE:
                canvas.drawPath(buildRegularPolygonPath(rect, 3, -90f), p);
                break;
            case PENTAGON:
                canvas.drawPath(buildRegularPolygonPath(rect, 5, -90f), p);
                break;
            case HEXAGON:
                canvas.drawPath(buildRegularPolygonPath(rect, 6, -90f), p);
                break;
            case RECT:
            default:
                canvas.drawRoundRect(rect, Math.max(10f, 14f * scale), Math.max(10f, 14f * scale), p);
                break;
        }
    }

    private Path buildDiamondPath(RectF rect) {
        polygonPath.reset();
        polygonPath.moveTo(rect.centerX(), rect.top);
        polygonPath.lineTo(rect.right, rect.centerY());
        polygonPath.lineTo(rect.centerX(), rect.bottom);
        polygonPath.lineTo(rect.left, rect.centerY());
        polygonPath.close();
        return polygonPath;
    }

    private Path buildRegularPolygonPath(RectF rect, int sides, float startAngleDeg) {
        polygonPath.reset();

        float cx = rect.centerX();
        float cy = rect.centerY();
        float radius = rect.width() / 2f;

        for (int i = 0; i < sides; i++) {
            double angle = Math.toRadians(startAngleDeg + i * (360.0 / sides));
            float px = cx + (float) (Math.cos(angle) * radius);
            float py = cy + (float) (Math.sin(angle) * radius);

            if (i == 0) polygonPath.moveTo(px, py);
            else polygonPath.lineTo(px, py);
        }

        polygonPath.close();
        return polygonPath;
    }

    private String truncateText(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private List<String> splitLines(String text, int charsPerLine, int maxLines) {
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

    public boolean contains(float touchX, float touchY, float scale, float offsetX, float offsetY) {
        float drawX = (x + offsetX) * scale;
        float drawY = (y + offsetY) * scale;
        float drawW = width * scale;
        float drawH = height * scale;

        drawRect.set(drawX, drawY, drawX + drawW, drawY + drawH);
        computeShapeRect(drawRect, getShape(), shapeRect);

        return shapeRect.contains(touchX, touchY);
    }

    public void move(float dx, float dy) {
        this.x += dx;
        this.y += dy;
    }

    public void addConnection(String connectionId) {
        if (connectionIds == null) connectionIds = new ArrayList<>();
        if (!connectionIds.contains(connectionId)) {
            connectionIds.add(connectionId);
        }
    }

    public void removeConnection(String connectionId) {
        if (connectionIds != null) {
            connectionIds.remove(connectionId);
        }
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public NodeType getType() { return type == null ? NodeType.CONCEPT : type; }
    public NodeShape getShape() { return shape == null ? NodeShape.RECT : shape; }
    public boolean isSelected() { return selected; }
    public boolean isDragging() { return dragging; }

    public List<String> getConnectionIds() {
        if (connectionIds == null) connectionIds = new ArrayList<>();
        return connectionIds;
    }

    public void setTitle(String title) { this.title = title == null ? "" : title; }
    public void setContent(String content) { this.content = content == null ? "" : content; }
    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
    public void setWidth(float width) { this.width = width; }
    public void setHeight(float height) { this.height = height; }
    public void setSelected(boolean selected) { this.selected = selected; }
    public void setDragging(boolean dragging) { this.dragging = dragging; }

    public void setType(NodeType type) {
        this.type = type == null ? NodeType.CONCEPT : type;
        ensurePaints();
        applyTypeStyle();
    }

    public void setShape(NodeShape shape) {
        this.shape = shape == null ? NodeShape.RECT : shape;
        ensurePaints();
    }

    public void setConnectionIds(List<String> connectionIds) {
        this.connectionIds = connectionIds == null ? new ArrayList<>() : connectionIds;
    }
}
