package com.agui.neuralcanvas;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class TemplatePostProcessor {

    private TemplatePostProcessor() {}

    public static void postProcess(Node baseNode,
                                   ScientificTemplateEngine.TemplateResult result,
                                   Map<String, Node> existingNodes) {
        if (result == null) return;

        String ownerId = WorkflowEngine.resolveOwnerId(baseNode);
        String templateTag = inferTemplateTag(result);

        for (Node node : result.createdNodes) {
            if (node == null) continue;

            if (WorkflowEngine.isBlank(node.getProjectId()) && !WorkflowEngine.isBlank(ownerId)) {
                node.setProjectId(ownerId);
            }

            if (!WorkflowEngine.isBlank(templateTag)) {
                node.addTag(templateTag);
            }

            if (baseNode != null) {
                mergeBaseContext(baseNode, node);
            }

            fillDefaults(node);
            WorkflowEngine.normalizeNodeForWorkflow(node);
        }

        ensureBackBelongsToConnection(baseNode, result);
    }

    private static void mergeBaseContext(Node baseNode, Node createdNode) {
        if (baseNode == null || createdNode == null) return;

        if (!WorkflowEngine.isBlank(baseNode.getAreaId()) && WorkflowEngine.isBlank(createdNode.getAreaId())) {
            createdNode.setAreaId(baseNode.getAreaId());
        }

        if (createdNode.getPriority() <= 0) {
            createdNode.setPriority(baseNode.getPriority());
        }

        if (WorkflowEngine.isBlank(createdNode.getNoteSource()) && !WorkflowEngine.isBlank(baseNode.getNoteSource())) {
            createdNode.setNoteSource(baseNode.getNoteSource());
        }

        if (baseNode.hasTag("Project")) createdNode.addTag("Project");
        if (baseNode.hasTag("Learning")) createdNode.addTag("Learning");
        if (baseNode.hasTag("Decision")) createdNode.addTag("Decision");
    }

    private static void fillDefaults(Node node) {
        if (node == null) return;

        switch (node.getType()) {
            case ACTION:
            case TASK:
                if (WorkflowEngine.isBlank(node.getTriggerCondition())) {
                    node.setTriggerCondition("如果我要推进这件事，那么先做一个最小动作");
                }
                break;

            case REVIEW:
                if (WorkflowEngine.isBlank(node.getReviewAt())) {
                    node.setReviewAt("待安排");
                }
                break;

            case QUESTION:
                if (WorkflowEngine.isBlank(node.getReviewAt())) {
                    node.setReviewAt("尽快第一次回忆");
                }
                break;

            case KEY_RESULT:
                if (node.getKrTarget() <= 0f) {
                    node.setKrTarget(1f);
                }
                break;

            case EVIDENCE:
                if (node.getEvidenceStrength() <= 0f) {
                    node.setEvidenceStrength(0.5f);
                }
                break;

            default:
                break;
        }

        if (node.getConfidence() <= 0f) {
            node.setConfidence(0.5f);
        }

        if (node.getPriority() <= 0) {
            node.setPriority(3);
        }
    }

    private static String inferTemplateTag(ScientificTemplateEngine.TemplateResult result) {
        if (result == null || result.createdNodes.isEmpty()) return "";

        Set<String> tags = new HashSet<>();
        for (Node node : result.createdNodes) {
            if (node == null) continue;
            tags.addAll(node.getTags());
        }

        if (tags.contains("WOOP")) return "WOOP";
        if (tags.contains("If-Then")) return "If-Then";
        if (tags.contains("每日复盘")) return "每日复盘";
        if (tags.contains("每周复盘")) return "每周复盘";
        if (tags.contains("AAR")) return "AAR";
        if (tags.contains("Decision")) return "Decision";
        if (tags.contains("Learning")) return "Learning";

        return "TemplateGenerated";
    }

    private static void ensureBackBelongsToConnection(Node baseNode,
                                                      ScientificTemplateEngine.TemplateResult result) {
        if (baseNode == null || result == null) return;

        boolean alreadyHasBelongsTo = false;
        for (Connection connection : result.createdConnections) {
            if (connection == null) continue;
            if (connection.getType() != Connection.ConnectionType.BELONGS_TO) continue;
            if (baseNode.getId().equals(connection.getToNodeId())) {
                alreadyHasBelongsTo = true;
                break;
            }
        }

        if (alreadyHasBelongsTo) return;

        for (Node node : result.createdNodes) {
            if (node == null) continue;
            if (node.getType() == Node.NodeType.REVIEW
                    || node.getType() == Node.NodeType.ACTION
                    || node.getType() == Node.NodeType.KEY_RESULT
                    || node.getType() == Node.NodeType.TRIGGER) {
                result.createdConnections.add(new Connection(
                        node.getId(),
                        baseNode.getId(),
                        Connection.ConnectionType.BELONGS_TO,
                        "回指原节点"
                ));
                return;
            }
        }
    }
}
