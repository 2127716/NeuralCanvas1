package com.agui.neuralcanvas;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;

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
    
    // 绘制相关
private Paint linePaint;
    private Paint arrowPaint;
    private Paint labelPaint;
    
    // 默认构造函数，用于Gson反序列化
    public Connection() {
        this.id = UUID.randomUUID().toString();
        this.fromNodeId = "";
        this.toNodeId = "";
        this.type = ConnectionType.SEQUENCE;
        this.label = "";
        
        initPaints();
    }
    
    public Connection(String fromNodeId, String toNodeId, ConnectionType type, String label) {
        this.id = UUID.randomUUID().toString();
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.type = type;
        this.label = label;
        
        initPaints();
    }
    
    private void initPaints() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(type.color);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4);
        linePaint.setStrokeCap(type.lineCap);
        
        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(type.color);
        arrowPaint.setStyle(Paint.Style.FILL);
        
        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(20);
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }
    
    public void draw(Canvas canvas, Node fromNode, Node toNode, float scale, float offsetX, float offsetY) {
        if (fromNode == null || toNode == null) return;
        
        float fromX = (fromNode.getX() + offsetX + fromNode.getWidth() / 2) * scale;
        float fromY = (fromNode.getY() + offsetY + fromNode.getHeight() / 2) * scale;
        float toX = (toNode.getX() + offsetX + toNode.getWidth() / 2) * scale;
        float toY = (toNode.getY() + offsetY + toNode.getHeight() / 2) * scale;
        
        // 绘制连线
        canvas.drawLine(fromX, fromY, toX, toY, linePaint);
        
        // 绘制箭头
        drawArrow(canvas, fromX, fromY, toX, toY, scale);
        
        // 绘制标签
        if (label != null && !label.isEmpty()) {
            float midX = (fromX + toX) / 2;
            float midY = (fromY + toY) / 2;
            
            // 绘制标签背景
            Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setColor(type.color);
            bgPaint.setAlpha(200);
            
            float textWidth = labelPaint.measureText(label);
            float padding = 10 * scale;
            float bgLeft = midX - textWidth / 2 - padding;
            float bgTop = midY - 20 * scale;
            float bgRight = midX + textWidth / 2 + padding;
            float bgBottom = midY + 10 * scale;
            
            canvas.drawRoundRect(bgLeft, bgTop, bgRight, bgBottom, 8 * scale, 8 * scale, bgPaint);
            
            // 绘制标签文字
            canvas.drawText(label, midX, midY, labelPaint);
        }
    }
    
    private void drawArrow(Canvas canvas, float fromX, float fromY, float toX, float toY, float scale) {
        float angle = (float) Math.atan2(toY - fromY, toX - fromX);
        float arrowLength = 20 * scale;
        float arrowWidth = 10 * scale;
        
        // 计算箭头点
        float x1 = toX - arrowLength * (float) Math.cos(angle) + arrowWidth * (float) Math.sin(angle);
        float y1 = toY - arrowLength * (float) Math.sin(angle) - arrowWidth * (float) Math.cos(angle);
        
        float x2 = toX - arrowLength * (float) Math.cos(angle) - arrowWidth * (float) Math.sin(angle);
        float y2 = toY - arrowLength * (float) Math.sin(angle) + arrowWidth * (float) Math.cos(angle);
        
        // 绘制箭头三角形
        Path arrowPath = new Path();
        arrowPath.moveTo(toX, toY);
        arrowPath.lineTo(x1, y1);
        arrowPath.lineTo(x2, y2);
        arrowPath.close();
        
        canvas.drawPath(arrowPath, arrowPaint);
    }
    
    public boolean isNear(float x, float y, Node fromNode, Node toNode, float scale, float offsetX, float offsetY, float tolerance) {
        if (fromNode == null || toNode == null) return false;
        
        float fromX = (fromNode.getX() + offsetX + fromNode.getWidth() / 2) * scale;
        float fromY = (fromNode.getY() + offsetY + fromNode.getHeight() / 2) * scale;
        float toX = (toNode.getX() + offsetX + toNode.getWidth() / 2) * scale;
        float toY = (toNode.getY() + offsetY + toNode.getHeight() / 2) * scale;
        
        // 计算点到直线的距离
        float A = y - fromY;
        float B = fromX - x;
        float C = fromY * (fromX - toX) - fromX * (fromY - toY);
        
        float distance = Math.abs(A * (toX - fromX) + B * (toY - fromY) + C) / 
                        (float) Math.sqrt(Math.pow(toX - fromX, 2) + Math.pow(toY - fromY, 2));
        
        // 检查点是否在线段范围内
        float dot1 = (x - fromX) * (toX - fromX) + (y - fromY) * (toY - fromY);
        float dot2 = (x - toX) * (fromX - toX) + (y - toY) * (fromY - toY);
        
        return distance <= tolerance && dot1 >= 0 && dot2 >= 0;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public String getFromNodeId() { return fromNodeId; }
    public String getToNodeId() { return toNodeId; }
    public ConnectionType getType() { return type; }
    public void setType(ConnectionType type) { 
        this.type = type;
        linePaint.setColor(type.color);
        arrowPaint.setColor(type.color);
    }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}