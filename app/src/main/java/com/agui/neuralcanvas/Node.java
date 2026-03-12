package com.agui.neuralcanvas;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Node {

    public enum NodeType {
        INBOX("#90A4AE", "收集"),
        CONCEPT("#4FC3F7", "概念"),
        IDEA("#FF9800", "想法"),
        QUESTION("#EF5350", "问题"),
        RESOURCE("#66BB6A", "资源"),
        TASK("#42A5F5", "任务"),
        ACTION("#29B6F6", "行动"),
        GOAL("#43A047", "目标"),
        PROJECT("#2E7D32", "项目"),
        KEY_RESULT("#00ACC1", "关键结果"),
        NOTE("#AB47BC", "笔记"),
        DECISION("#8BC34A", "决策"),
        OPTION("#7CB342", "方案"),
        CRITERION("#5C6BC0", "准则"),
        EVIDENCE("#26A69A", "证据"),
        ASSUMPTION("#8D6E63", "假设"),
        RISK("#E53935", "风险"),
        OBSTACLE("#F4511E", "障碍"),
        ROUTINE("#7E57C2", "习惯"),
        TRIGGER("#5E35B1", "触发器"),
        REVIEW("#FFCA28", "复盘"),
        SOURCE("#26C6DA", "来源"),
        INSIGHT("#EC407A", "洞察"),
        EXPERIMENT("#00897B", "实验");

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

    public enum NodeStatus {
        ACTIVE("进行中"),
        PLANNED("计划中"),
        SOMEDAY("将来"),
        BLOCKED("受阻"),
        WAITING("等待"),
        REVIEW("待复盘"),
        DONE("已完成");

        public final String label;

        NodeStatus(String label) {
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
    private NodeStatus status = NodeStatus.ACTIVE;
    private boolean selected;
    private boolean dragging;
    private List<String> connectionIds;

    // ===== 科学工作流字段 =====
    private List<String> tags;
    private int priority;                 // 1-5
    private String dueAt;                 // 文本先存，后续再升级成真正时间
    private String reviewAt;              // 文本先存
    private float effortEstimate;         // 预计耗时（小时）
    private float actualEffort;           // 实际耗时（小时）
    private float confidence;             // 0~1
    private String triggerCondition;      // If-Then 中的 If
    private String projectId;
    private String areaId;
    private float krTarget;
    private float krCurrent;
    private float evidenceStrength;       // 0~1
    private String noteSource;
    private String metaJson;

    private transient Paint fillPaint;
    private transient Paint strokePaint;
    private transient Paint titlePaint;
    private transient Paint contentPaint;
    private transient Paint typePaint;
    private transient Paint selectedPaint;
    private transient Paint badgePaint;
    private transient Paint badgeTextPaint;

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
        this.status = NodeStatus.ACTIVE;
        this.selected = false;
        this.dragging = false;
        this.connectionIds = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.priority = 3;
        this.dueAt = "";
        this.reviewAt = "";
        this.effortEstimate = 0f;
        this.actualEffort = 0f;
        this.confidence = 0.5f;
        this.triggerCondition = "";
        this.projectId = "";
        this.areaId = "";
        this.krTarget = 0f;
        this.krCurrent = 0f;
        this.evidenceStrength = 0.5f;
        this.noteSource = "";
        this.metaJson = "";
        ensurePaints();
    }

    public Node(String title, String content, float x, float y, NodeType type) {
        this();
        this.title = title == null ? "" : title;
        this.content = content == null ? "" : content;
        this.x = x;
        this.y = y;
        this.type = type == null ? NodeType.CONCEPT : type;
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

        if (badgePaint == null) {
            badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            badgePaint.setStyle(Paint.Style.FILL);
        }

        if (badgeTextPaint == null) {
            badgeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            badgeTextPaint.setColor(Color.WHITE);
            badgeTextPaint.setTextSize(16f);
            badgeTextPaint.setFakeBoldText(true);
            badgeTextPaint.setTextAlign(Paint.Align.CENTER);
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
        typePaint.setTextSize(Math.max(12f, 14f * scale));
        badgeTextPaint.setTextSize(Math.max(11f, 12f * scale));

        String safeTitle = title == null ? "" : title;
        String safeContent = content == null ? "" : content;
        String safeType = type == null ? "" : type.label;

        float textLeft = bounds.left + padding;
        float textTop = bounds.top + 34f * scale;

        canvas.drawText(truncateText(safeTitle, 10), textLeft, textTop, titlePaint);

        if (scale >= 0.55f) {
            canvas.drawText(truncateText(safeContent, 8), textLeft, bounds.top + 58f * scale, contentPaint);
        }

        canvas.drawText(safeType, textLeft, bounds.bottom - 14f * scale, typePaint);

        drawStatusBadge(canvas, bounds, scale);
    }

    private void drawStatusBadge(Canvas canvas, RectF bounds, float scale) {
        String badgeText = status == null ? "" : status.label;
        if (badgeText.isEmpty()) return;

        badgePaint.setColor(resolveStatusColor());

        float badgePaddingX = 10f * scale;
        float badgePaddingY = 7f * scale;
        float textWidth = badgeTextPaint.measureText(badgeText);
        float badgeWidth = textWidth + badgePaddingX * 2f;
        float badgeHeight = 18f * scale + badgePaddingY;

        float right = bounds.right - 10f * scale;
        float top = bounds.top + 10f * scale;
        float left = right - badgeWidth;
        float bottom = top + badgeHeight;

        canvas.drawRoundRect(left, top, right, bottom, 12f * scale, 12f * scale, badgePaint);
        canvas.drawText(badgeText, (left + right) / 2f, bottom - 6f * scale, badgeTextPaint);
    }

    private int resolveStatusColor() {
        NodeStatus s = status == null ? NodeStatus.ACTIVE : status;
        switch (s) {
            case PLANNED:
                return Color.parseColor("#5C6BC0");
            case SOMEDAY:
                return Color.parseColor("#8D6E63");
            case BLOCKED:
                return Color.parseColor("#E53935");
            case WAITING:
                return Color.parseColor("#FB8C00");
            case REVIEW:
                return Color.parseColor("#FDD835");
            case DONE:
                return Color.parseColor("#43A047");
            case ACTIVE:
            default:
                return Color.parseColor("#1E88E5");
        }
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

    public String getTagsAsString() {
        if (tags == null || tags.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(tags.get(i));
        }
        return sb.toString();
    }

    public void setTagsFromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            this.tags = new ArrayList<>();
            return;
        }
        String[] parts = value.split("[,，]");
        List<String> result = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) result.add(t);
        }
        this.tags = result;
    }

    public boolean hasTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) return false;
        String target = tag.trim();
        for (String t : getTags()) {
            if (target.equalsIgnoreCase(t == null ? "" : t.trim())) {
                return true;
            }
        }
        return false;
    }

    public void addTag(String tag) {
        if (tag == null) return;
        String value = tag.trim();
        if (value.isEmpty()) return;
        if (!hasTag(value)) {
            getTags().add(value);
        }
    }

    public void addTags(String... values) {
        if (values == null) return;
        for (String v : values) {
            addTag(v);
        }
    }

    public boolean belongsToProject(String targetProjectId) {
        if (targetProjectId == null || targetProjectId.trim().isEmpty()) return false;
        return targetProjectId.equals(getProjectId());
    }

    public boolean isLearningNode() {
        return type == NodeType.CONCEPT
                || type == NodeType.NOTE
                || type == NodeType.QUESTION
                || type == NodeType.SOURCE
                || type == NodeType.INSIGHT
                || type == NodeType.EVIDENCE;
    }

    public boolean isDecisionNode() {
        return type == NodeType.DECISION
                || type == NodeType.OPTION
                || type == NodeType.CRITERION
                || type == NodeType.ASSUMPTION
                || type == NodeType.RISK
                || type == NodeType.EVIDENCE;
    }

    public boolean isExecutionNode() {
        return type == NodeType.TASK
                || type == NodeType.ACTION
                || type == NodeType.GOAL
                || type == NodeType.PROJECT
                || type == NodeType.KEY_RESULT
                || type == NodeType.ROUTINE
                || type == NodeType.TRIGGER
                || type == NodeType.OBSTACLE;
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
    public NodeStatus getStatus() { return status == null ? NodeStatus.ACTIVE : status; }
    public boolean isSelected() { return selected; }
    public boolean isDragging() { return dragging; }

    public List<String> getConnectionIds() {
        if (connectionIds == null) connectionIds = new ArrayList<>();
        return connectionIds;
    }

    public List<String> getTags() {
        if (tags == null) tags = new ArrayList<>();
        return tags;
    }

    public int getPriority() { return priority; }
    public String getDueAt() { return dueAt == null ? "" : dueAt; }
    public String getReviewAt() { return reviewAt == null ? "" : reviewAt; }
    public float getEffortEstimate() { return effortEstimate; }
    public float getActualEffort() { return actualEffort; }
    public float getConfidence() { return confidence; }
    public String getTriggerCondition() { return triggerCondition == null ? "" : triggerCondition; }
    public String getProjectId() { return projectId == null ? "" : projectId; }
    public String getAreaId() { return areaId == null ? "" : areaId; }
    public float getKrTarget() { return krTarget; }
    public float getKrCurrent() { return krCurrent; }
    public float getEvidenceStrength() { return evidenceStrength; }
    public String getNoteSource() { return noteSource == null ? "" : noteSource; }
    public String getMetaJson() { return metaJson == null ? "" : metaJson; }

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

    public void setStatus(NodeStatus status) {
        this.status = status == null ? NodeStatus.ACTIVE : status;
    }

    public void setConnectionIds(List<String> connectionIds) {
        this.connectionIds = connectionIds == null ? new ArrayList<String>() : connectionIds;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<String>() : tags;
    }

    public void setPriority(int priority) {
        this.priority = Math.max(1, Math.min(5, priority));
    }

    public void setDueAt(String dueAt) {
        this.dueAt = dueAt == null ? "" : dueAt;
    }

    public void setReviewAt(String reviewAt) {
        this.reviewAt = reviewAt == null ? "" : reviewAt;
    }

    public void setEffortEstimate(float effortEstimate) {
        this.effortEstimate = Math.max(0f, effortEstimate);
    }

    public void setActualEffort(float actualEffort) {
        this.actualEffort = Math.max(0f, actualEffort);
    }

    public void setConfidence(float confidence) {
        this.confidence = clamp01(confidence);
    }

    public void setTriggerCondition(String triggerCondition) {
        this.triggerCondition = triggerCondition == null ? "" : triggerCondition;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId == null ? "" : projectId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId == null ? "" : areaId;
    }

    public void setKrTarget(float krTarget) {
        this.krTarget = Math.max(0f, krTarget);
    }

    public void setKrCurrent(float krCurrent) {
        this.krCurrent = Math.max(0f, krCurrent);
    }

    public void setEvidenceStrength(float evidenceStrength) {
        this.evidenceStrength = clamp01(evidenceStrength);
    }

    public void setNoteSource(String noteSource) {
        this.noteSource = noteSource == null ? "" : noteSource;
    }

    public void setMetaJson(String metaJson) {
        this.metaJson = metaJson == null ? "" : metaJson;
    }

    private float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
