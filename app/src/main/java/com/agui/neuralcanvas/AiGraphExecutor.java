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

            String action = safeLower(cmd.getAction());

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

        Node node = new Node(
                nonNull(cmd.getTitle()),
                nonNull(cmd.getContent()),
                x,
                y,
                type
        );

        node.setShape(shape);

        if (cmd.getWidth() != null) node.setWidth(Math.max(80f, cmd.getWidth()));
        if (cmd.getHeight() != null) node.setHeight(Math.max(80f, cmd.getHeight()));

        mindMapView.addNode(node);

        if (!isBlank(cmd.getTempId())) {
            tempNodeAliasMap.put(cmd.getTempId().trim(), node.getId());
        }
    }

    private void updateNode(AiCommand cmd, Map<String, String> tempNodeAliasMap) {
        Map<String, Node> nodes = mindMapView.getNodesInternal();
        String realNodeId = resolveNodeId(cmd.getNodeId(), tempNodeAliasMap);
        Node node = nodes.get(realNodeId);
        if (node == null) return;

        if (!isBlank(cmd.getTitle())) node.setTitle(cmd.getTitle());
        if (!isBlank(cmd.getContent())) node.setContent(cmd.getContent());
        if (!isBlank(cmd.getType())) node.setType(parseNodeType(cmd.getType()));
        if (!isBlank(cmd.getShape())) node.setShape(parseNodeShape(cmd.getShape()));
        if (cmd.getX() != null) node.setX(cmd.getX());
        if (cmd.getY() != null) node.setY(cmd.getY());
        if (cmd.getWidth() != null) node.setWidth(Math.max(80f, cmd.getWidth()));
        if (cmd.getHeight() != null) node.setHeight(Math.max(80f, cmd.getHeight()));

        mindMapView.invalidate();
    }

    private void deleteNode(AiCommand cmd, Map<String, String> tempNodeAliasMap) {
        String realNodeId = resolveNodeId(cmd.getNodeId(), tempNodeAliasMap);
        if (!isBlank(realNodeId)) {
            mindMapView.removeNode(realNodeId);
        }
    }

    private void createConnection(AiCommand cmd, Map<String, String> tempNodeAliasMap) {
        Map<String, Node> nodes = mindMapView.getNodesInternal();
        Map<String, Connection> connections = mindMapView.getConnectionsInternal();

        String fromId = resolveNodeId(cmd.getFromNodeId(), tempNodeAliasMap);
        String toId = resolveNodeId(cmd.getToNodeId(), tempNodeAliasMap);

        if (isBlank(fromId) || isBlank(toId)) return;
        if (!nodes.containsKey(fromId) || !nodes.containsKey(toId)) return;
        if (fromId.equals(toId)) return;

        for (Connection c : connections.values()) {
            if (fromId.equals(c.getFromNodeId()) && toId.equals(c.getToNodeId())) {
                applyConnectionValues(c, cmd);
                mindMapView.invalidate();
                return;
            }
        }

        Connection connection = new Connection(
                fromId,
                toId,
                parseConnectionType(cmd.getConnectionType()),
                nonNull(cmd.getLabel())
        );

        applyConnectionValues(connection, cmd);
        mindMapView.addConnection(connection);
    }

    private void updateConnection(AiCommand cmd, Map<String, String> tempNodeAliasMap) {
        Map<String, Connection> connections = mindMapView.getConnectionsInternal();

        String fromId = resolveNodeId(cmd.getFromNodeId(), tempNodeAliasMap);
        String toId = resolveNodeId(cmd.getToNodeId(), tempNodeAliasMap);

        if (isBlank(fromId) || isBlank(toId)) return;

        for (Connection c : connections.values()) {
            if (fromId.equals(c.getFromNodeId()) && toId.equals(c.getToNodeId())) {
                applyConnectionValues(c, cmd);
                mindMapView.invalidate();
                return;
            }
        }
    }

    private void deleteConnection(AiCommand cmd, Map<String, String> tempNodeAliasMap) {
        Map<String, Connection> connections = mindMapView.getConnectionsInternal();

        String fromId = resolveNodeId(cmd.getFromNodeId(), tempNodeAliasMap);
        String toId = resolveNodeId(cmd.getToNodeId(), tempNodeAliasMap);

        if (isBlank(fromId) || isBlank(toId)) return;

        for (Connection c : connections.values()) {
            if (fromId.equals(c.getFromNodeId()) && toId.equals(c.getToNodeId())) {
                mindMapView.removeConnection(c.getId());
                return;
            }
        }
    }

    private void focusNode(AiCommand cmd, Map<String, String> tempNodeAliasMap) {
        String realNodeId = resolveNodeId(cmd.getNodeId(), tempNodeAliasMap);
        if (!isBlank(realNodeId)) {
            mindMapView.focusNodeById(realNodeId);
        }
    }

    private String resolveNodeId(String rawId, Map<String, String> tempNodeAliasMap) {
        if (isBlank(rawId)) return "";
        String id = rawId.trim();
        if (tempNodeAliasMap.containsKey(id)) {
            return tempNodeAliasMap.get(id);
        }
        return id;
    }

    private void applyConnectionValues(Connection connection, AiCommand cmd) {
        if (!isBlank(cmd.getLabel())) {
            connection.setLabel(cmd.getLabel());
        }

        if (!isBlank(cmd.getConnectionType())) {
            connection.setType(parseConnectionType(cmd.getConnectionType()));
        }

        if (cmd.getStrokeWidth() != null) {
            connection.setStrokeWidth(Math.max(2f, cmd.getStrokeWidth()));
        }

        if (!isBlank(cmd.getConnectionColorHex())) {
            try {
                connection.setCustomColor(Color.parseColor(cmd.getConnectionColorHex()));
            } catch (Exception ignored) {
            }
        }
    }

    private Node.NodeType parseNodeType(String value) {
        try {
            return Node.NodeType.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return Node.NodeType.CONCEPT;
        }
    }

    private Node.NodeShape parseNodeShape(String value) {
        try {
            return Node.NodeShape.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return Node.NodeShape.RECT;
        }
    }

    private Connection.ConnectionType parseConnectionType(String value) {
        try {
            return Connection.ConnectionType.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return Connection.ConnectionType.LEADS_TO;
        }
    }

    private String safeLower(String text) {
        return text == null ? "" : text.trim().toLowerCase();
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private String nonNull(String text) {
        return text == null ? "" : text;
    }
}
