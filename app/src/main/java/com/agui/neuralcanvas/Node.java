package com.agui.neuralcanvas;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Node {
public enum NodeType {
        GOAL(Color.parseColor("#4CAF50"), "目标", 0),
        TASK(Color.parseColor("#2196F3"), "任务", 1),
        IDEA(Color.parseColor("#FF9800"), "想法", 2),
        NOTE(Color.parseColor("#9C27B0"), "笔记", 3),
        PROBLEM(Color.parseColor("#F44336"), "问题", 4),
        RESOURCE(Color.parseColor("#607D8B"), "资源", 5),
        PERSON(Color.parseColor("#E91E63"), "人物", 6),
        EVENT(Color.parseColor("#00BCD4"), "事件", 7),
        DECISION(Color.parseColor("#8BC34A"), "决策", 8),
        RISK(Color.parseColor("#FF5722"), "风险", 9);
        
        public final int color;
        public final String label;
        public final int styleIndex;
        
        NodeType(int color, String label, int styleIndex) {
            this.color = color;
            this.label = label;
            this.styleIndex = styleIndex;
        }
        
        public static NodeType fromIndex(int index) {
            for (NodeType type : values()) {
                if (type.styleIndex == index) {
                    return type;
                }
            }
            return NOTE; // 默认返回笔记类型
        }
    }
    
    private String id;
    private String title;
    private String content;
    private NodeType type;
    private float x, y;
    private float width = 200;
    private float height = 120;
    private boolean selected = false;
    private boolean dragging = false;
    private List<String> connectionIds = new ArrayList<>();
    
    // 绘制相关
    private Paint nodePaint;
    private Paint selectedPaint;
    private TextPaint textPaint;
    private Paint typePaint;
    
    // 默认构造函数，用于Gson反序列化
    public Node() {
        this.id = UUID.randomUUID().toString();
        this.title = "新节点";
        this.content = "";
        this.type = NodeType.NOTE;
        this.x = 0;
        this.y = 0;
        this.width = 200;
        this.height = 100;
        this.connectionIds = new ArrayList<>();
        
        // 初始化画笔
        initPaints();
    }
    
    public Node(String title, String content, NodeType type, float x, float y) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.content = content;
        this.type = type;
        this.x = x;
        this.y = y;
        
        initPaints();
    }
    
    private void initPaints() {
        nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nodePaint.setColor(type.color);
        nodePaint.setStyle(Paint.Style.FILL);
        nodePaint.setAlpha(180);
        
        selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedPaint.setColor(Color.WHITE);
        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setStrokeWidth(4);
        
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(24);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        
        typePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        typePaint.setColor(Color.WHITE);
        typePaint.setTextSize(18);
        typePaint.setAlpha(200);
    }
    
    public void draw(Canvas canvas, float scale, float offsetX, float offsetY, 
                     boolean isSearchResult, boolean highlight) {
        float drawX = (x + offsetX) * scale;
        float drawY = (y + offsetY) * scale;
        float drawWidth = width * scale;
        float drawHeight = height * scale;
        
        // 绘制圆角矩形节点
        RectF rect = new RectF(drawX, drawY, drawX + drawWidth, drawY + drawHeight);
        
        // 如果是搜索结果且需要高亮，绘制高亮背景
        if (isSearchResult && highlight) {
            Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            highlightPaint.setColor(Color.YELLOW);
            highlightPaint.setStyle(Paint.Style.FILL);
            highlightPaint.setAlpha(80);
            canvas.drawRoundRect(rect, 20 * scale, 20 * scale, highlightPaint);
        }
        
        canvas.drawRoundRect(rect, 20 * scale, 20 * scale, nodePaint);
        
        // 绘制选中边框
        if (selected) {
            canvas.drawRoundRect(rect, 20 * scale, 20 * scale, selectedPaint);
        }
        
        // 绘制搜索结果边框
        if (isSearchResult && highlight) {
            Paint searchBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            searchBorderPaint.setColor(Color.YELLOW);
            searchBorderPaint.setStyle(Paint.Style.STROKE);
            searchBorderPaint.setStrokeWidth(3 * scale);
            canvas.drawRoundRect(rect, 20 * scale, 20 * scale, searchBorderPaint);
        }
        
        // 绘制标题
        String displayTitle = title;
        if (displayTitle.length() > 15) {
            displayTitle = displayTitle.substring(0, 15) + "...";
        }
        float titleX = drawX + 10 * scale;
        float titleY = drawY + 30 * scale;
        canvas.drawText(displayTitle, titleX, titleY, textPaint);
        
        // 绘制类型标签
        float typeX = drawX + 10 * scale;
        float typeY = drawY + drawHeight - 15 * scale;
        canvas.drawText(type.label, typeX, typeY, typePaint);
        
        // 绘制连接点（节点中心）
        float centerX = drawX + drawWidth / 2;
        float centerY = drawY + drawHeight / 2;
        Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPaint.setColor(Color.WHITE);
        centerPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX, centerY, 5 * scale, centerPaint);
    }
    
    public boolean contains(float touchX, float touchY, float scale, float offsetX, float offsetY) {
        float drawX = (x + offsetX) * scale;
        float drawY = (y + offsetY) * scale;
        float drawWidth = width * scale;
        float drawHeight = height * scale;
        
        return touchX >= drawX && touchX <= drawX + drawWidth &&
               touchY >= drawY && touchY <= drawY + drawHeight;
    }
    
    public void move(float dx, float dy) {
        this.x += dx;
        this.y += dy;
    }
    
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public NodeType getType() { return type; }
    public void setType(NodeType type) { 
        this.type = type;
        nodePaint.setColor(type.color);
    }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
    public boolean isDragging() { return dragging; }
    public void setDragging(boolean dragging) { this.dragging = dragging; }
    public List<String> getConnectionIds() { return connectionIds; }
    
    public void addConnection(String connectionId) {
        if (!connectionIds.contains(connectionId)) {
            connectionIds.add(connectionId);
        }
    }
    
    public void removeConnection(String connectionId) {
        connectionIds.remove(connectionId);
    }
}