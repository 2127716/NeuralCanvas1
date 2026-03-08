package com.agui.neuralcanvas;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GraphAutoLayout {

    public static void apply(MindMapView mindMapView) {
        if (mindMapView == null) return;

        Map<String, Node> nodes = mindMapView.getNodes();
        Map<String, Connection> connections = mindMapView.getConnections();

        if (nodes.isEmpty()) return;

        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> outgoing = new HashMap<>();

        for (String nodeId : nodes.keySet()) {
            indegree.put(nodeId, 0);
            outgoing.put(nodeId, new ArrayList<>());
        }

        for (Connection c : connections.values()) {
            String from = c.getFromNodeId();
            String to = c.getToNodeId();
            if (nodes.containsKey(from) && nodes.containsKey(to)) {
                outgoing.get(from).add(to);
                indegree.put(to, indegree.get(to) + 1);
            }
        }

        List<String> roots = new ArrayList<>();
        for (String nodeId : nodes.keySet()) {
            if (indegree.get(nodeId) == 0) {
                roots.add(nodeId);
            }
        }

        if (roots.isEmpty()) {
            roots.addAll(nodes.keySet());
        }

        Map<String, Integer> levelMap = new HashMap<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();

        for (String root : roots) {
            queue.offer(root);
            levelMap.put(root, 0);
            visited.add(root);
        }

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentLevel = levelMap.get(current);

            for (String next : outgoing.get(current)) {
                int nextLevel = currentLevel + 1;
                if (!levelMap.containsKey(next) || nextLevel > levelMap.get(next)) {
                    levelMap.put(next, nextLevel);
                }
                if (!visited.contains(next)) {
                    visited.add(next);
                    queue.offer(next);
                }
            }
        }

        for (String nodeId : nodes.keySet()) {
            if (!levelMap.containsKey(nodeId)) {
                levelMap.put(nodeId, 0);
            }
        }

        Map<Integer, List<Node>> grouped = new HashMap<>();
        for (Node node : nodes.values()) {
            int level = levelMap.get(node.getId());
            if (!grouped.containsKey(level)) {
                grouped.put(level, new ArrayList<>());
            }
            grouped.get(level).add(node);
        }

        float startX = 80f;
        float startY = 80f;
        float colGap = 260f;
        float rowGap = 220f;

        List<Integer> levels = new ArrayList<>(grouped.keySet());
        java.util.Collections.sort(levels);

        for (Integer level : levels) {
            List<Node> levelNodes = grouped.get(level);
            levelNodes.sort((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));

            for (int i = 0; i < levelNodes.size(); i++) {
                Node node = levelNodes.get(i);
                node.setX(startX + level * colGap);
                node.setY(startY + i * rowGap);
            }
        }

        mindMapView.invalidate();
    }
}
