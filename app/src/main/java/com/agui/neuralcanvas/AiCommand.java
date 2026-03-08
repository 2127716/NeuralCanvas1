package com.agui.neuralcanvas;

public class AiCommand {
    private String action;
    private String tempId;
    private String nodeId;
    private String fromNodeId;
    private String toNodeId;
    private String title;
    private String content;
    private String type;
    private String shape;
    private String label;
    private String connectionType;
    private String connectionColorHex;
    private String reason;
    private Float x;
    private Float y;
    private Float width;
    private Float height;
    private Float strokeWidth;
    private Boolean applyAutoLayoutAfter;

    public String getAction() { return action == null ? "" : action.trim(); }
    public void setAction(String action) { this.action = action; }

    public String getTempId() { return tempId == null ? "" : tempId.trim(); }
    public void setTempId(String tempId) { this.tempId = tempId; }

    public String getNodeId() { return nodeId == null ? "" : nodeId.trim(); }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getFromNodeId() { return fromNodeId == null ? "" : fromNodeId.trim(); }
    public void setFromNodeId(String fromNodeId) { this.fromNodeId = fromNodeId; }

    public String getToNodeId() { return toNodeId == null ? "" : toNodeId.trim(); }
    public void setToNodeId(String toNodeId) { this.toNodeId = toNodeId; }

    public String getTitle() { return title == null ? "" : title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content == null ? "" : content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type == null ? "" : type.trim(); }
    public void setType(String type) { this.type = type; }

    public String getShape() { return shape == null ? "" : shape.trim(); }
    public void setShape(String shape) { this.shape = shape; }

    public String getLabel() { return label == null ? "" : label; }
    public void setLabel(String label) { this.label = label; }

    public String getConnectionType() { return connectionType == null ? "" : connectionType.trim(); }
    public void setConnectionType(String connectionType) { this.connectionType = connectionType; }

    public String getConnectionColorHex() { return connectionColorHex == null ? "" : connectionColorHex.trim(); }
    public void setConnectionColorHex(String connectionColorHex) { this.connectionColorHex = connectionColorHex; }

    public String getReason() { return reason == null ? "" : reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Float getX() { return x; }
    public void setX(Float x) { this.x = x; }

    public Float getY() { return y; }
    public void setY(Float y) { this.y = y; }

    public Float getWidth() { return width; }
    public void setWidth(Float width) { this.width = width; }

    public Float getHeight() { return height; }
    public void setHeight(Float height) { this.height = height; }

    public Float getStrokeWidth() { return strokeWidth; }
    public void setStrokeWidth(Float strokeWidth) { this.strokeWidth = strokeWidth; }

    public Boolean getApplyAutoLayoutAfter() { return applyAutoLayoutAfter; }
    public void setApplyAutoLayoutAfter(Boolean applyAutoLayoutAfter) { this.applyAutoLayoutAfter = applyAutoLayoutAfter; }
}
