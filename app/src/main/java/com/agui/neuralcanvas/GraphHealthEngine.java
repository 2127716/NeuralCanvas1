
package com.agui.neuralcanvas;

import java.util.*;

public class GraphHealthEngine {
    public static class HealthReport {
        public int isolatedNodes;
        public int totalNodes;
        public float score;
    }

    public static HealthReport analyze(Map<String, Node> nodes, Map<String, Connection> connections) {
        HealthReport r = new HealthReport();
        if (nodes == null) return r;

        r.totalNodes = nodes.size();
        Set<String> connected = new HashSet<>();

        for (Connection c : connections.values()) {
            connected.add(c.getFromNodeId());
            connected.add(c.getToNodeId());
        }

        for (String id : nodes.keySet()) {
            if (!connected.contains(id)) r.isolatedNodes++;
        }

        if (r.totalNodes == 0) r.score = 100;
        else r.score = 100f * (1f - (float)r.isolatedNodes / r.totalNodes);

        return r;
    }
}
