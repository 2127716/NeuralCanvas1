package com.agui.neuralcanvas;

import java.util.List;
import java.util.Map;

public class AiGraphExecutor {

    private final MindMapView mindMapView;

    public AiGraphExecutor(MindMapView mindMapView) {
        this.mindMapView = mindMapView;
    }

    public void execute(List<AiCommand> commands) {
        if (commands == null || commands.isEmpty()) return;

        for (AiCommand cmd : commands) {
            if (cmd == null) continue;

            String action = cmd.getAction().toLowerCase();
            switch (action) {
                case "create_node":
                    createNode(cmd);
                    break;
                case "update_node":
                    updateNode(cmd);
                    break;
                case "delete_node":
                    if (!cmd.getNodeId().isEmpty()) {
                        mindMapView.removeNode(cmd.getNodeId());
                    }
                    break;
                case "create_connection":
                    createConnection(cmd);
                    break;
                case "delete_connection":
                    deleteConnection(cmd);
                    break;
                case "focus_node":
                    if (!cmd.getNodeId().isEmpty()) {
                        mindMapView.focusNodeById(cmd.getNodeId());
                    }
                    break;
            }
        }

        mindMapView.invalidate();
    }

    private void createNode(AiCommand cmd) {
        Node.NodeType type = parseNodeType(cmd.getType());
        Node.NodeShape shape = parseNodeShape(cmd.getShape());

        float x = cmd.getX() != null ? cmd.getX() : 0f;
        float y = cmd.getY() != null ? cmd.getY() : 0f;

        Node node = new Node(cmd.getTitle(), cmd.getContent(), x, y, type);
        node.setShape(shape);

        if (cmd.getWidth() != null) node.setWidth(cmd.getWidth());
        if (cmd.getHeight() != null) node.setHeight(cmd.getHeight());

        mindMapView.addNode(node);
    }

    private void updateNode(AiCommand cmd) {
        Map<String, Node> nodes = mindMapView.getNodes();
        Node node = nodes.get(cmd.getNodeId());
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

    private void createConnection(AiCommand cmd) {
        Map<String, Node> nodes = mindMapView.getNodes();
        Map<String, Connection> connections = mindMapView.getConnections();

        if (!nodes.containsKey(cmd.getFromNodeId()) || !nodes.containsKey(cmd.getToNodeId())) {
            return;
        }

        for (Connection c : connections.values()) {
            if (cmd.getFromNodeId().equals(c.getFromNodeId())
                    && cmd.getToNodeId().equals(c.getToNodeId())) {
                c.setLabel(cmd.getLabel());
                return;
            }
        }

        Connection connection = new Connection(
                cmd.getFromNodeId(),
                cmd.getToNodeId(),
                Connection.ConnectionType.SEQUENCE,
                cmd.getLabel()
        );
        mindMapView.addConnection(connection);
    }

    private void deleteConnection(AiCommand cmd) {
        Map<String, Connection> connections = mindMapView.getConnections();
        for (Connection c : connections.values()) {
            if (cmd.getFromNodeId().equals(c.getFromNodeId())
                    && cmd.getToNodeId().equals(c.getToNodeId())) {
                mindMapView.removeConnection(c.getId());
                return;
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
}
