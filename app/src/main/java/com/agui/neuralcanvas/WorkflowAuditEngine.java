
package com.agui.neuralcanvas;

import java.util.*;

public class WorkflowAuditEngine {
    public static class AuditResult {
        public List<String> issues = new ArrayList<>();
        public boolean isHealthy() { return issues.isEmpty(); }
    }

    public static AuditResult audit(Map<String, Node> nodes, Map<String, Connection> connections) {
        AuditResult result = new AuditResult();
        if (nodes == null) return result;

        for (Node n : nodes.values()) {
            if (n == null) continue;

            if (n.isTaskNode()) {
                if (n.getTriggerCondition() == null || n.getTriggerCondition().isEmpty()) {
                    result.issues.add("任务缺少触发器: " + n.getTitle());
                }
                if (n.getDueAt() <= 0) {
                    result.issues.add("任务缺少时间约束: " + n.getTitle());
                }
            }

            if (n.isDecisionNode()) {
                boolean hasEvidence = false;
                for (Connection c : connections.values()) {
                    if (n.getId().equals(c.getToNodeId())
                        && "EVIDENCE_FOR".equalsIgnoreCase(c.getType())) {
                        hasEvidence = true;
                        break;
                    }
                }
                if (!hasEvidence) {
                    result.issues.add("决策缺少证据: " + n.getTitle());
                }
            }
        }
        return result;
    }
}
