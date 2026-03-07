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
        SEQUENCE(Color.parseColor("#2196F3"), "顺序", Paint.Cap.ROUND),
        PARALLEL(Color.parseColor("#4CAF50"), "并列", Paint.Cap.BUTT),
        BLOCKING(Color.parseColor("#F44336"), "阻碍", Paint.Cap.SQUARE),
        DEPENDENCY(Color.parseColor("#FF9800"), "依赖", Paint.Cap.ROUND),
        REFERENCE(Color.parseColor("#9C27B0"), "参考", Paint.Cap.ROUND);

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

    // 新增：可自定义颜色/粗细
    private Integer customColor = null;
    private float strokeWidth = 4f;

    // 绘制相关
    private transient Paint linePaint;
    private transient Paint arrowPaint;
    private transient Paint labelPaint;

    // 默认构造函数，用于 Gson 反序列化
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
        this.type = type != null ? type : ConnectionType.SEQUENCE;
        this.label = label != null ? label : "";
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
        return customColor != null ? customColor : type.color;
    }

    private void applyPaintStyle() {
        int color = resolveColor();

        linePaint.setColor(color);
        linePaint.setStrokeWidth(strokeWidth);
        linePaint.setStrokeCap(type.lineCap);

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

        linePaint.setStrokeWidth(Math.max(2f, strokeWidth * scale));

        if (selected) {
            Paint selectedPaint = new Paint(linePaint);
            selectedPaint.setStrokeWidth(Math.max(4f, (strokeWidth + 2f) * scale));
            selectedPaint.setColor(Color.WHITE);
            selectedPaint.setAlpha(110);
            canvas.drawLine(start.x, start.y, end.x, end.y, selectedPaint);
        }

        canvas.drawLine(start.x, start.y, end.x, end.y, linePaint);
        drawArrow(canvas, start.x, start.y, end.x, end.y, scale);

        if (label != null && !label.trim().isEmpty()) {
            drawLabel(canvas, start.x, start.y, end.x, end.y, scale);
        }
    }

    private void drawLabel(Canvas canvas, float fromX, float fromY, float toX, float toY, float scale) {
        float midX = (fromX + toX) / 2f;
        float midY = (fromY + toY) / 2f;

        labelPaint.setTextSize(Math.max(20f, 20f * scale));

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(resolveColor());
        bgPaint.setAlpha(210);

        float textWidth = labelPaint.measureText(label);
        float padding = Math.max(10f, 10f * scale);
        float bgLeft = midX - textWidth / 2f - padding;
        float bgTop = midY - 20f * scale;
        float bgRight = midX + textWidth / 2f + padding;
        float bgBottom = midY + 10f * scale;

        canvas.drawRoundRect(bgLeft, bgTop, bgRight, bgBottom, 8f * scale, 8f * scale, bgPaint);
        canvas.drawText(label, midX, midY, labelPaint);
    }

    private void drawArrow(Canvas canvas, float fromX, float fromY, float toX, float toY, float scale) {
        float angle = (float) Math.atan2(toY - fromY, toX - fromX);
        float arrowLength = Math.max(14f, 18f * scale);
        float arrowWidth = Math.max(7f, 9f * scale);

        float x1 = toX - arrowLength * (float) Math.cos(angle) + arrowWidth * (float) Math.sin(angle);
        float y1 = toY - arrowLength * (float) Math.sin(angle) - arrowWidth * (float) Math.cos(angle);

        float x2 = toX - arrowLength * (float) Math.cos(angle) - arrowWidth * (float) Math.sin(angle);
        float y2 = toY - arrowLength * (float) Math.sin(angle) + arrowWidth * (float) Math.cos(angle);

        Path arrowPath = new Path();
        arrowPath.moveTo(toX, toY);
        arrowPath.lineTo(x1, y1);
        arrowPath.lineTo(x2, y2);
        arrowPath.close();

        canvas.drawPath(arrowPath, arrowPaint);
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

        if (dx == 0 && dy == 0) {
            return new PointF(center.x, center.y);
        }

        float halfW = rect.width() / 2f;
        float halfH = rect.height() / 2f;

        float scaleX = halfW / Math.abs(dx == 0 ? 0.0001f : dx);
        float scaleY = halfH / Math.abs(dy == 0 ? 0.0001f : dy);
        float edgeScale = Math.min(scaleX, scaleY);

        float px = center.x + dx * edgeScale;
        float py = center.y + dy * edgeScale;

        return new PointF(px, py);
    }

    public boolean isNear(float x, float y, Node fromNode, Node toNode, float scale, float offsetX, float offsetY, float tolerance) {
        if (fromNode == null || toNode == null) return false;

        PointF fromCenter = getNodeCenter(fromNode, scale, offsetX, offsetY);
        PointF toCenter = getNodeCenter(toNode, scale, offsetX, offsetY);

        PointF start = getRectEdgePoint(fromNode, fromCenter, toCenter, scale, offsetX, offsetY);
        PointF end = getRectEdgePoint(toNode, toCenter, fromCenter, scale, offsetX, offsetY);

        if (start == null) start = fromCenter;
        if (end == null) end = toCenter;

        float dx = end.x - start.x;
        float dy = end.y - start.y;
        float lengthSquared = dx * dx + dy * dy;
        if (lengthSquared == 0) return false;

        float t = ((x - start.x) * dx + (y - start.y) * dy) / lengthSquared;
        t = Math.max(0f, Math.min(1f, t));

        float projX = start.x + t * dx;
        float projY = start.y + t * dy;

        float distX = x - projX;
        float distY = y - projY;
        float distance = (float) Math.sqrt(distX * distX + distY * distY);

        return distance <= tolerance + Math.max(8f, strokeWidth * scale);
    }

    public String getId() { return id; }
    public String getFromNodeId() { return fromNodeId; }
    public String getToNodeId() { return toNodeId; }
    public ConnectionType getType() { return type; }

    public void setType(ConnectionType type) {
        this.type = type != null ? type : ConnectionType.SEQUENCE;
        ensurePaints();
        applyPaintStyle();
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label != null ? label : ""; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    public Integer getCustomColor() { return customColor; }
    public void setCustomColor(Integer customColor) {
        this.customColor = customColor;
        ensurePaints();
        applyPaintStyle();
    }

    public float getStrokeWidth() { return strokeWidth; }
    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = Math.max(2f, strokeWidth);
        ensurePaints();
        applyPaintStyle();
    }
}
