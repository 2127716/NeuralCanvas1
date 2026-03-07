package com.agui.neuralcanvas;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;

import java.util.UUID;

public class Connection {

    public enum ConnectionType {
        SEQUENCE(Color.parseColor("#67B7FF"), "顺序", Paint.Cap.ROUND),
        PARALLEL(Color.parseColor("#57D38C"), "并列", Paint.Cap.ROUND),
        BLOCKING(Color.parseColor("#FF6B6B"), "阻碍", Paint.Cap.ROUND),
        DEPENDENCY(Color.parseColor("#FFB84D"), "依赖", Paint.Cap.ROUND),
        REFERENCE(Color.parseColor("#B084F5"), "参考", Paint.Cap.ROUND);

        public final int color;
        public final String label;
        public final Paint.Cap lineCap;

        ConnectionType(int color, String label, Paint.Cap lineCap) {
            this.color = color;
            this.label = label;
            this.lineCap = lineCap;
        }
    }

    private String id;
    private String fromNodeId;
    private String toNodeId;
    private ConnectionType type;
    private String label;
    private boolean selected = false;
    private Integer customColor = null;
    private float strokeWidth = 4f;

    private transient Paint linePaint;
    private transient Paint arrowPaint;
    private transient Paint labelPaint;

    public Connection() {
        this.id = UUID.randomUUID().toString();
        this.fromNodeId = "";
        this.toNodeId = "";
        this.type = ConnectionType.SEQUENCE;
        this.label = "";
        ensurePaints();
    }

    public Connection(String fromNodeId, String toNodeId, ConnectionType type, String label) {
        this.id = UUID.randomUUID().toString();
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.type = type == null ? ConnectionType.SEQUENCE : type;
        this.label = label == null ? "" : label;
        ensurePaints();
    }

    private void ensurePaints() {
        if (linePaint == null) {
            linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            linePaint.setStyle(Paint.Style.STROKE);
        }
        if (arrowPaint == null) {
            arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            arrowPaint.setStyle(Paint.Style.FILL);
        }
        if (labelPaint == null) {
            labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            labelPaint.setColor(Color.WHITE);
            labelPaint.setTextAlign(Paint.Align.CENTER);
        }
        applyPaintStyle();
    }

    private int resolveColor() {
        return customColor != null ? customColor : (type != null ? type.color : Color.parseColor("#67B7FF"));
    }

    private void applyPaintStyle() {
        int color = resolveColor();
        linePaint.setColor(color);
        linePaint.setStrokeCap(type != null ? type.lineCap : Paint.Cap.ROUND);
        arrowPaint.setColor(color);
    }

    public void draw(Canvas canvas, Node fromNode, Node toNode, float scale, float offsetX, float offsetY) {
        if (fromNode == null || toNode == null) return;
        ensurePaints();

        PointF fromCenter = getNodeCenter(fromNode, scale, offsetX, offsetY);
        PointF toCenter = getNodeCenter(toNode, scale, offsetX, offsetY);

        PointF start = getRectEdgePoint(fromNode, fromCenter, toCenter, scale, offsetX, offsetY);
        PointF end = getRectEdgePoint(toNode, toCenter, fromCenter, scale, offsetX, offsetY);

        if (start == null) start = fromCenter;
        if (end == null) end = toCenter;

        // 关键：缩小时也保留粗细差异，不让所有线看起来都一样细
        float effectiveWidth = Math.max(2.2f, strokeWidth * (0.85f + scale * 0.35f));
        linePaint.setStrokeWidth(effectiveWidth);

        if (selected) {
            Paint haloPaint = new Paint(linePaint);
            haloPaint.setColor(Color.WHITE);
            haloPaint.setAlpha(110);
            haloPaint.setStrokeWidth(effectiveWidth + 3f);
            canvas.drawLine(start.x, start.y, end.x, end.y, haloPaint);
        }

        canvas.drawLine(start.x, start.y, end.x, end.y, linePaint);
        drawArrow(canvas, start.x, start.y, end.x, end.y, effectiveWidth, scale);

        if (label != null && !label.trim().isEmpty()) {
            drawLabel(canvas, start.x, start.y, end.x, end.y, scale);
        }
    }

    private void drawLabel(Canvas canvas, float fromX, float fromY, float toX, float toY, float scale) {
        float midX = (fromX + toX) / 2f;
        float midY = (fromY + toY) / 2f;

        float textSize = Math.max(18f, 18f * scale);
        labelPaint.setTextSize(textSize);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(resolveColor());
        bgPaint.setAlpha(220);

        float textWidth = labelPaint.measureText(label);
        float padding = Math.max(10f, 10f * scale);

        float left = midX - textWidth / 2f - padding;
        float top = midY - 20f * scale;
        float right = midX + textWidth / 2f + padding;
        float bottom = midY + 10f * scale;

        canvas.drawRoundRect(left, top, right, bottom, 10f * scale, 10f * scale, bgPaint);
        canvas.drawText(label, midX, midY, labelPaint);
    }

    private void drawArrow(Canvas canvas, float fromX, float fromY, float toX, float toY, float lineWidth, float scale) {
        float angle = (float) Math.atan2(toY - fromY, toX - fromX);

        float arrowLength = Math.max(16f, 14f + lineWidth * 1.7f);
        float arrowWidth = Math.max(8f, 7f + lineWidth * 0.9f);

        float x1 = toX - arrowLength * (float) Math.cos(angle) + arrowWidth * (float) Math.sin(angle);
        float y1 = toY - arrowLength * (float) Math.sin(angle) - arrowWidth * (float) Math.cos(angle);

        float x2 = toX - arrowLength * (float) Math.cos(angle) - arrowWidth * (float) Math.sin(angle);
        float y2 = toY - arrowLength * (float) Math.sin(angle) + arrowWidth * (float) Math.cos(angle);

        Path path = new Path();
        path.moveTo(toX, toY);
        path.lineTo(x1, y1);
        path.lineTo(x2, y2);
        path.close();

        canvas.drawPath(path, arrowPaint);
    }

    private PointF getNodeCenter(Node node, float scale, float offsetX, float offsetY) {
        float cx = (node.getX() + offsetX + node.getWidth() / 2f) * scale;
        float cy = (node.getY() + offsetY + node.getHeight() / 2f) * scale;
        return new PointF(cx, cy);
    }

    private PointF getRectEdgePoint(Node node, PointF center, PointF target, float scale, float offsetX, float offsetY) {
        float left = (node.getX() + offsetX) * scale;
        float top = (node.getY() + offsetY) * scale;
        float right = left + node.getWidth() * scale;
        float bottom = top + node.getHeight() * scale;

        RectF rect = new RectF(left, top, right, bottom);

        float dx = target.x - center.x;
        float dy = target.y - center.y;

        if (dx == 0 && dy == 0) return new PointF(center.x, center.y);

        float halfW = rect.width() / 2f;
        float halfH = rect.height() / 2f;

        float scaleX = halfW / Math.abs(dx == 0 ? 0.0001f : dx);
        float scaleY = halfH / Math.abs(dy == 0 ? 0.0001f : dy);
        float edgeScale = Math.min(scaleX, scaleY);

        return new PointF(center.x + dx * edgeScale, center.y + dy * edgeScale);
    }

    public boolean isNear(float x, float y, Node fromNode, Node toNode, float scale, float offsetX, float offsetY, float tolerance) {
        if (fromNode == null || toNode == null) return false;

        PointF fromCenter = getNodeCenter(fromNode, scale, offsetX, offsetY);
        PointF toCenter = getNodeCenter(toNode, scale, offsetX, offsetY);

        PointF start = getRectEdgePoint(fromNode, fromCenter, toCenter, scale, offsetX, offsetY);
        PointF end = getRectEdgePoint(toNode, toCenter, fromCenter, scale, offsetX, offsetY);

        float dx = end.x - start.x;
        float dy = end.y - start.y;
        float len2 = dx * dx + dy * dy;
        if (len2 == 0) return false;

        float t = ((x - start.x) * dx + (y - start.y) * dy) / len2;
        t = Math.max(0f, Math.min(1f, t));

        float px = start.x + t * dx;
        float py = start.y + t * dy;
        float distX = x - px;
        float distY = y - py;

        return Math.sqrt(distX * distX + distY * distY) <= tolerance + Math.max(10f, strokeWidth * scale);
    }

    public String getId() { return id; }
    public String getFromNodeId() { return fromNodeId; }
    public String getToNodeId() { return toNodeId; }
    public ConnectionType getType() { return type; }
    public String getLabel() { return label; }
    public boolean isSelected() { return selected; }
    public Integer getCustomColor() { return customColor; }
    public float getStrokeWidth() { return strokeWidth; }

    public void setType(ConnectionType type) {
        this.type = type == null ? ConnectionType.SEQUENCE : type;
        ensurePaints();
        applyPaintStyle();
    }

    public void setLabel(String label) {
        this.label = label == null ? "" : label;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setCustomColor(Integer customColor) {
        this.customColor = customColor;
        ensurePaints();
        applyPaintStyle();
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = Math.max(2f, strokeWidth);
        ensurePaints();
        applyPaintStyle();
    }
}
