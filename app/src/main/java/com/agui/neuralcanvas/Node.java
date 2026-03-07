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
        CONCEPT("#4FC3F7", "概念"),
        IDEA("#FF9800", "想法"),
        QUESTION("#EF5350", "问题"),
        RESOURCE("#66BB6A", "资源"),
        TASK("#42A5F5", "任务"),
        GOAL("#43A047", "目标"),
        NOTE("#AB47BC", "笔记"),
        DECISION("#8BC34A", "决策");

        public final String colorHex;
        public final String label;

        NodeType(String colorHex, String label) {
            this.colorHex = colorHex;
            this.label = label;
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
    private transient Paint typePaint;
    private transient Paint selectedPaint;

    public Node() {
        this.id = UUID.randomUUID().toString();
        this.title = "";
        this.content = "";
        this.x = 100;
        this.y = 100;
        this.width = 168;
        this.height = 168;
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
        this.width = 168;
        this.height = 168;
        this.type = type == null ? NodeType.CONCEPT : type;
        this.shape = NodeShape.RECT;
        this.selected = false;
        this.dragging = false;
        this.connectionIds = new ArrayList<>();
        ensurePaints();
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
            contentPaint.setColor(Color.parseColor("#EAF2FF"));
            contentPaint.setTextSize(20f);
        }

        if (typePaint == null) {
            typePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            typePaint.setColor(Color.parseColor("#D8E6FF"));
            typePaint.setTextSize(18f);
        }

        if (selectedPaint == null) {
            selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            selectedPaint.setStyle(Paint.Style.STROKE);
            selectedPaint.setColor(Color.WHITE);
            selectedPaint.setStrokeWidth(6f);
            selectedPaint.setAlpha(200);
        }

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
        strokePaint.setColor(adjustColorBrightness(baseColor, 0.72f));
    }

    private int adjustColorBrightness(int color, float factor) {
        int r = Math.max(0, Math.min(255, (int) (Color.red(color) * factor)));
        int g = Math.max(0, Math.min(255, (int) (Color.green(color) * factor)));
        int b = Math.max(0, Math.min(255, (int) (Color.blue(color) * factor)));
        return Color.rgb(r, g, b);
    }

    public void draw(Canvas canvas, float scale, float offsetX, float offsetY) {
        ensurePaints();

        float drawX = (x + offsetX) * scale;
        float drawY = (y + offsetY) * scale;
        float drawW = width * scale;
        float drawH = height * scale;

        RectF bounds = new RectF(drawX, drawY, drawX + drawW, drawY + drawH);
        RectF shapeBounds = getRegularShapeBounds(bounds);

        if (selected) {
            drawShapeOutline(canvas, shapeBounds, selectedPaint, scale);
        }

        drawShape(canvas, shapeBounds, fillPaint, scale);
        drawShapeOutline(canvas, shapeBounds, strokePaint, scale);

        float padding = 15f * scale;
        titlePaint.setTextSize(Math.max(20f, 24f * scale));
        contentPaint.setTextSize(Math.max(15f, 17f * scale));
        typePaint.setTextSize(Math.max(13f, 14f * scale));

        String safeTitle = title == null ? "" : title;
        String safeContent = content == null ? "" : content;
        String safeType = type == null ? "" : type.label;

        float textLeft = bounds.left + padding;
        float textTop = bounds.top + 34f * scale;

        canvas.drawText(truncateText(safeTitle, 10), textLeft, textTop, titlePaint);
        canvas.drawText(truncateText(safeContent, 8), textLeft, bounds.top + 58f * scale, contentPaint);
        canvas.drawText(safeType, textLeft, bounds.bottom - 14f * scale, typePaint);
    }

    private RectF getRegularShapeBounds(RectF bounds) {
        switch (getShape()) {
            case CIRCLE:
            case RECT:
            case DIAMOND:
            case TRIANGLE:
            case PENTAGON:
            case HEXAGON: {
                float size = Math.min(bounds.width(), bounds.height());
                float cx = bounds.centerX();
                float cy = bounds.centerY();
                return new RectF(
                        cx - size / 2f,
                        cy - size / 2f,
                        cx + size / 2f,
                        cy + size / 2f
                );
            }
            case OVAL:
            default:
                return bounds;
        }
    }

    private void drawShape(Canvas canvas, RectF rect, Paint paint, float scale) {
        switch (getShape()) {
            case CIRCLE:
                canvas.drawOval(rect, paint);
                break;
            case OVAL:
                canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, paint);
                break;
            case DIAMOND:
                canvas.drawPath(createDiamondPath(rect), paint);
                break;
            case TRIANGLE:
                canvas.drawPath(createRegularPolygonPath(rect, 3, -90f), paint);
                break;
            case PENTAGON:
                canvas.drawPath(createRegularPolygonPath(rect, 5, -90f), paint);
                break;
            case HEXAGON:
                canvas.drawPath(createRegularPolygonPath(rect, 6, -90f), paint);
                break;
            case RECT:
            default:
                canvas.drawRoundRect(rect, 22f * scale, 22f * scale, paint);
                break;
        }
    }

    private void drawShapeOutline(Canvas canvas, RectF rect, Paint paint, float scale) {
        Paint p = new Paint(paint);
        p.setStrokeWidth(Math.max(2f, paint.getStrokeWidth() * (0.55f + 0.45f * scale)));

        switch (getShape()) {
            case CIRCLE:
                canvas.drawOval(rect, p);
                break;
            case OVAL:
                canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, p);
                break;
            case DIAMOND:
                canvas.drawPath(createDiamondPath(rect), p);
                break;
            case TRIANGLE:
                canvas.drawPath(createRegularPolygonPath(rect, 3, -90f), p);
                break;
            case PENTAGON:
                canvas.drawPath(createRegularPolygonPath(rect, 5, -90f), p);
                break;
            case HEXAGON:
                canvas.drawPath(createRegularPolygonPath(rect, 6, -90f), p);
                break;
            case RECT:
            default:
                canvas.drawRoundRect(rect, 22f * scale, 22f * scale, p);
                break;
        }
    }

    private Path createDiamondPath(RectF rect) {
        Path path = new Path();
        path.moveTo(rect.centerX(), rect.top);
        path.lineTo(rect.right, rect.centerY());
        path.lineTo(rect.centerX(), rect.bottom);
        path.lineTo(rect.left, rect.centerY());
        path.close();
        return path;
    }

    private Path createRegularPolygonPath(RectF rect, int sides, float startAngleDeg) {
        Path path = new Path();

        float cx = rect.centerX();
        float cy = rect.centerY();
        float radius = Math.min(rect.width(), rect.height()) / 2f;

        for (int i = 0; i < sides; i++) {
            double angle = Math.toRadians(startAngleDeg + i * 360f / sides);
            float px = cx + (float) (Math.cos(angle) * radius);
            float py = cy + (float) (Math.sin(angle) * radius);

            if (i == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }

        path.close();
        return path;
    }

    private String truncateText(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    public boolean contains(float touchX, float touchY, float scale, float offsetX, float offsetY) {
        float drawX = (x + offsetX) * scale;
        float drawY = (y + offsetY) * scale;
        float drawW = width * scale;
        float drawH = height * scale;

        return touchX >= drawX && touchX <= drawX + drawW &&
                touchY >= drawY && touchY <= drawY + drawH;
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
    public NodeType getType() { return type; }
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
