package com.agui.neuralcanvas;

import java.util.List;
import java.util.Map;

public final class TemplateStateSynchronizer {

    private TemplateStateSynchronizer() {}

    public static void synchronize(Node baseNode,
                                   ScientificTemplateEngine.TemplateResult result,
                                   Map<String, Node> nodes,
                                   Map<String, Connection> connections) {
        if (baseNode == null || result == null) return;

        boolean createdAction = hasType(result.createdNodes, Node.NodeType.ACTION, Node.NodeType.TASK);
        boolean createdTrigger = hasType(result.createdNodes, Node.NodeType.TRIGGER);
        boolean createdReview = hasType(result.createdNodes, Node.NodeType.REVIEW);
        boolean createdEvidence = hasType(result.createdNodes, Node.NodeType.EVIDENCE);
        boolean createdQuestion = hasType(result.createdNodes, Node.NodeType.QUESTION);
        boolean createdExperiment = hasType(result.createdNodes, Node.NodeType.EXPERIMENT);
        boolean createdKr = hasType(result.createdNodes, Node.NodeType.KEY_RESULT);

        if ((baseNode.isExecutionNode() || baseNode.getType() == Node.NodeType.PROJECT || baseNode.getType() == Node.NodeType.GOAL)
                && WorkflowEngine.isBlank(baseNode.getTriggerCondition())
                && (createdAction || createdTrigger)) {
            baseNode.setTriggerCondition("如果我要推进这个节点，那么先做已生成结构中的最小下一步");
        }

        if (createdReview && WorkflowEngine.isBlank(baseNode.getReviewAt())) {
            if (baseNode.getType() == Node.NodeType.PROJECT || baseNode.getType() == Node.NodeType.GOAL) {
                baseNode.setReviewAt("每周回顾");
            } else if (baseNode.isLearningNode()) {
                baseNode.setReviewAt("尽快第一次回忆");
            } else {
                baseNode.setReviewAt("待安排");
            }
        }

        if (createdEvidence) {
            float avgEvidence = averageEvidenceStrength(result.createdNodes);
            if (avgEvidence > 0f) {
                baseNode.setEvidenceStrength(Math.max(baseNode.getEvidenceStrength(), avgEvidence));
            }
            if (baseNode.isDecisionNode()) {
                float boosted = Math.min(1f, Math.max(baseNode.getConfidence(), 0.45f + avgEvidence * 0.35f));
                baseNode.setConfidence(boosted);
            }
        }

        if (baseNode.isLearningNode() && (createdQuestion || createdExperiment) && WorkflowEngine.isBlank(baseNode.getReviewAt())) {
            baseNode.setReviewAt("尽快第一次回忆");
        }

        if ((baseNode.getType() == Node.NodeType.PROJECT || baseNode.getType() == Node.NodeType.GOAL) && createdKr) {
            if (WorkflowEngine.isBlank(baseNode.getProjectId())) {
                baseNode.setProjectId(baseNode.getId());
            }
            baseNode.addTags("Project");
        }

        WorkflowEngine.normalizeNodeForWorkflow(baseNode);
    }

    private static boolean hasType(List<Node> nodes, Node.NodeType... types) {
        if (nodes == null || types == null) return false;
        for (Node node : nodes) {
            if (node == null) continue;
            for (Node.NodeType type : types) {
                if (node.getType() == type) return true;
            }
        }
        return false;
    }

    private static float averageEvidenceStrength(List<Node> nodes) {
        if (nodes == null || nodes.isEmpty()) return 0f;
        float sum = 0f;
        int count = 0;
        for (Node node : nodes) {
            if (node == null || node.getType() != Node.NodeType.EVIDENCE) continue;
            sum += node.getEvidenceStrength();
            count++;
        }
        return count == 0 ? 0f : (sum / count);
    }
}
