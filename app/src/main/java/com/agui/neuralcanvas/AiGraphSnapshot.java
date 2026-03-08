package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AiGraphSnapshot {

    public static class SnapshotNode {
        public String id;
        public String title;
        public String content;
        public String type;
        public String shape;
        public float x;
        public float y;
        public float width;
        public float height;
        public List<String> connectionIds = new ArrayList<>();
    }

    public static class SnapshotConnection {
        public String id;
        public String fromNodeId;
        public String toNodeId;
        public String type;
        public String label;
        public Float strokeWidth;
        public Integer customColor;
        public boolean directed = true;
    }

    public List<SnapshotNode> nodes = new ArrayList<>();
    public List<SnapshotConnection> connections = new ArrayList<>();

    public static AiGraphSnapshot from(Map<String, Node> nodeMap, Map<String, Connection> connectionMap) {
        AiGraphSnapshot snapshot = new AiGraphSnapshot();

        if (nodeMap != null) {
            for (Node node : nodeMap.values()) {
                SnapshotNode item = new SnapshotNode();
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
        }

        if (connectionMap != null) {
            for (Connection c : connectionMap.values()) {
                SnapshotConnection item = new SnapshotConnection();
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
        }

        return snapshot;
    }
}
