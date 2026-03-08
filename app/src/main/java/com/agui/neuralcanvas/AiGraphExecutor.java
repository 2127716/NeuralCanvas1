package com.agui.neuralcanvas;

import android.graphics.Color;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AiGraphExecutor {

    private final MindMapView mindMapView;

    public AiGraphExecutor(MindMapView mindMapView) {
        this.mindMapView = mindMapView;
    }

    public void execute(List<AiCommand> commands) {
        if (commands == null || commands.isEmpty()) return;

        Map<String, String> tempNodeAliasMap = new HashMap<>();

        for (AiCommand cmd : commands) {
            if (cmd == null) continue;

            String action = cmd.getAction().toLowerCase();
            switch (action) {
                case "create_node":
                    createNode(cmd, tempNodeAliasMap);
                    break;
                case "update_node":
                    updateNode(cmd, tempNodeAliasMap);
                    break;
                case "delete_node":
                    deleteNode(cmd, tempNodeAliasMap);
                    break;
                case "create_connection":
                    createConnection(cmd, tempNodeAliasMap);
                    break;
                case "update_connection":
                    updateConnection(cmd, tempNodeAliasMap);
                    break;
                case "delete_connection":
                    deleteConnection(cmd, tempNodeAliasMap);
                    break;
                case "focus_node":
                    focusNode(cmd, tempNodeAliasMap);
                    break;
                case "auto_layout":
                    GraphAutoLayout.apply(mindMapView);
                    break;
            }

            if (Boolean.TRUE.equals(cmd.getApplyAutoLayoutAfter())) {
                GraphAutoLayout.apply(mindMapView);
            }
        }

        mindMapView.invalidate();
    }

    private void createNode(AiCommand cmd, Map<String, String> tempNodeAliasMap) {
        Node.NodeType type = parseNodeType(cmd.getType());
        Node.NodeShape shape = parseNodeShape(cmd.getShape());

        float x = cmd.getX() != null ? cmd.getX() : 0f;
        float y = cmd.getY() != null ? cmd.getY() : 0f;

        Node node = new Node(cmd.getTitle(), cmd.getContent(), x, y, type);
        node.setShape(shape);

        if (cmd.getWidth() != null) node.setWidth(cmd.getWidth());
        if (cmd.getHeight() != null) node.setHeight(cmd.getHeight());

        mindMapView.addNode(node);

        if (!cmd.getTempId().isEmpty()) {
            tempNodeAliasMap.put(cmd.getTempId(), node.getId());
        }
    }

    private void updateNode(AiCommand cmd, Map<String, String> tempNodeAliasMap) {
        Node node = mindMapView.getNodes().get(resolveNodeId(cmd.getNodeId(), tempNodeAliasMap));
        if (node == null) return;

        if (!cmd.getTitle().isEmpty()) node.setTitle(cmd.getTitle());
        if (!cmd.getContent().isEmpty()) node.setContent(cmd.getContent());
        if (!cmd.getType().isEmpty()) node.setType(parseNodeType(cmd.getType()));
        if (!cmd.getShape().isEmpty()) node.setShape(parseNodeShape(cmd.getShape()));
        if (cmd.getX() != null) node.setX(cmd.getX());
        if (cmd.getY() != null) node.setY(cmd.getY());
        if (cmd.getWidth() != null) node.setWidth(cmd.getWidth());
        if (cmd.getHeight() != null) node.setHeight(cmd.getHeight());
    }

    private void deleteNode(AiCommand cmd, Map<String, String> tempNodeAliasMap) {
        String realNodeId = resolveNodeId(cmd.getNodeId(), tempNodeAliasMap);
        if (!realNodeId.isEmpty()) {
            mindMapView.removeNode(realNodeId);
        }
    }

    private void createConnection(AiCommand cmd, Map<String, String> tempNodeAliasMap) {
        Map<String, Node> nodes = mindMapView.getNodes();
        Map<String, Connection> connections = mindMapView.getConnections();

        String fromId = resolveNodeId(cmd.getFromNodeId(), tempNodeAliasMap);
        String toId = resolveNodeId(cmd.getToNodeId(), tempNodeAliasMap);

        if (!nodes.containsKey(fromId) || !nodes.containsKey(toId) || fromId.equals(toId)) {
            return;
        }

        for (Connection c : connections.values()) {
            if (fromId.equals(c.getFromNodeId()) && toId.equals(c.getToNodeId())) {
                applyConnectionValues(c, cmd);
                return;
            }
        }

        Connection connection = new Connection(
                fromId,
                toId,
                parseConnectionType(cmd.getConnectionType()),
                cmd.getLabel()
        );
        applyConnectionValues(connection, cmd);
        mindMapView.addConnection(connection);
    }

    private void updateConnection(AiCommand cmd, Map<String, String> tempNodeAliasMap) {
        String fromId = resolveNodeId(cmd.getFromNodeId(), tempNodeAliasMap);
        String toId = resolveNodeId(cmd.getToNodeId(), tempNodeAliasMap);

        for (Connection c : mindMapView.getConnections().values()) {
            if (fromId.equals(c.getFromNodeId()) && toId.equals(c.getToNodeId())) {
                applyConnectionValues(c, cmd);
                return;
            }
        }
    }

    private void deleteConnection(AiCommand cmd, Map<String, String> tempNodeAliasMap) {
        String fromId = resolveNodeId(cmd.getFromNodeId(), tempNodeAliasMap);
        String toId = resolveNodeId(cmd.getToNodeId(), tempNodeAliasMap);

        for (Connection c : mindMapView.getConnections().values()) {
            if (fromId.equals(c.getFromNodeId()) && toId.equals(c.getToNodeId())) {
                mindMapView.removeConnection(c.getId());
                return;
            }
        }
    }

    private void focusNode(AiCommand cmd, Map<String, String> tempNodeAliasMap) {
        String realNodeId = resolveNodeId(cmd.getNodeId(), tempNodeAliasMap);
        if (!realNodeId.isEmpty()) {
            mindMapView.focusNodeById(realNodeId);
        }
    }

    private String resolveNodeId(String rawId, Map<String, String> tempNodeAliasMap) {
        if (rawId == null || rawId.trim().isEmpty()) return "";
        String id = rawId.trim();
        return tempNodeAliasMap.containsKey(id) ? tempNodeAliasMap.get(id) : id;
    }

    private void applyConnectionValues(Connection connection, AiCommand cmd) {
        if (!cmd.getLabel().isEmpty()) {
            connection.setLabel(cmd.getLabel());
        }
        if (!cmd.getConnectionType().isEmpty()) {
            connection.setType(parseConnectionType(cmd.getConnectionType()));
        }
        if (cmd.getStrokeWidth() != null) {
            connection.setStrokeWidth(cmd.getStrokeWidth());
        }
        if (!cmd.getConnectionColorHex().isEmpty()) {
            try {
                connection.setCustomColor(Color.parseColor(cmd.getConnectionColorHex()));
            } catch (Exception ignored) {
            }
        }
    }

    private Node.NodeType parseNodeType(String value) {
        try {
            return Node.NodeType.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return Node.NodeType.CONCEPT;
        }
    }

    private Node.NodeShape parseNodeShape(String value) {
        try {
            return Node.NodeShape.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return Node.NodeShape.RECT;
        }
    }

    private Connection.ConnectionType parseConnectionType(String value) {
        try {
            return Connection.ConnectionType.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return Connection.ConnectionType.SEQUENCE;
        }
    }
}
