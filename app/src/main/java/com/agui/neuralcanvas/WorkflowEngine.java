package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class WorkflowEngine {

    private WorkflowEngine() {}

    public static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static boolean isBlank(String value) {
        return safe(value).isEmpty();
    }

    public static String resolveOwnerId(Node baseNode) {
        if (baseNode == null) return "";
        if (!isBlank(baseNode.getProjectId())) return baseNode.getProjectId();
        if (isProjectLike(baseNode)) return baseNode.getId();
        return baseNode.getId();
    }

    public static boolean isInboxNode(Node node) {
        return node != null && node.getType() == Node.NodeType.INBOX;
    }

    public static boolean isProjectLike(Node node) {
        if (node == null) return false;
        return node.getType() == Node.NodeType.PROJECT
                || node.getType() == Node.NodeType.GOAL
                || node.getType() == Node.NodeType.DECISION;
    }

    public static boolean isActionable(Node node) {
        if (node == null) return false;
        switch (node.getType()) {
            case TASK:
            case ACTION:
            case ROUTINE:
            case TRIGGER:
            case OBSTACLE:
                return true;
            default:
                return false;
        }
    }

    public static boolean isWaiting(Node node) {
        return node != null && node.getStatus() == Node.NodeStatus.WAITING;
    }

    public static boolean isDone(Node node) {
        return node != null && node.getStatus() == Node.NodeStatus.DONE;
    }

    public static boolean isBlocked(Node node) {
        return node != null && node.getStatus() == Node.NodeStatus.BLOCKED;
    }



    public static boolean hasTag(Node node, String tag) {
        if (node == null || tag == null) return false;
        String target = safe(tag).toLowerCase();
        for (String item : node.getTags()) {
            if (safe(item).toLowerCase().equals(target)) return true;
        }
        return false;
    }

    public static boolean isResourceNode(Node node) {
        if (node == null) return false;
        return node.getType() == Node.NodeType.RESOURCE
                || node.getType() == Node.NodeType.SOURCE
                || hasTag(node, "Resource");
    }

    public static boolean isArchived(Node node) {
        return node != null && hasTag(node, "Archive");
    }

    public static String deriveAreaName(Node node) {
        if (node == null) return "";
        if (!isBlank(node.getAreaId())) return node.getAreaId();
        String title = safe(node.getTitle());
        if (title.isEmpty()) return "General";
        if (title.length() > 18) return title.substring(0, 18);
        return title;
    }

    public static boolean isReviewNode(Node node) {
        return node != null && node.getType() == Node.NodeType.REVIEW;
    }

    public static boolean canBeNextAction(Node node) {
        if (!isActionable(node)) return false;
        if (isDone(node) || isBlocked(node) || isWaiting(node)) return false;
        return node.getStatus() == Node.NodeStatus.ACTIVE
                || node.getStatus() == Node.NodeStatus.PLANNED;
    }

    public static void normalizeNodeForWorkflow(Node node) {
        if (node == null) return;

        switch (node.getType()) {
            case INBOX:
                node.setStatus(Node.NodeStatus.ACTIVE);
                break;

            case TASK:
            case ACTION:
                if (node.getStatus() == null
                        || node.getStatus() == Node.NodeStatus.REVIEW
                        || node.getStatus() == Node.NodeStatus.SOMEDAY) {
                    node.setStatus(Node.NodeStatus.PLANNED);
                }
                if (isBlank(node.getTriggerCondition())) {
                    node.setTriggerCondition("如果我要开始推进这件事，那么先做最小下一步");
                }
                break;

            case PROJECT:
                node.setStatus(Node.NodeStatus.ACTIVE);
                if (isBlank(node.getProjectId())) {
                    node.setProjectId(node.getId());
                }
                break;

            case GOAL:
            case DECISION:
                if (node.getStatus() == Node.NodeStatus.DONE) {
                    // 保留已完成状态
                } else {
                    node.setStatus(Node.NodeStatus.ACTIVE);
                }
                break;

            case KEY_RESULT:
                if (node.getStatus() == Node.NodeStatus.DONE) {
                    // 保留
                } else {
                    node.setStatus(Node.NodeStatus.ACTIVE);
                }
                if (node.getKrTarget() <= 0f) {
                    node.setKrTarget(1f);
                }
                break;

            case REVIEW:
                node.setStatus(Node.NodeStatus.REVIEW);
                if (isBlank(node.getReviewAt())) {
                    node.setReviewAt("待安排");
                }
                break;

            case QUESTION:
            case CONCEPT:
            case NOTE:
            case INSIGHT:
            case SOURCE:
            case EVIDENCE:
                if (node.getStatus() == null) {
                    node.setStatus(Node.NodeStatus.ACTIVE);
                }
                break;

            case RISK:
            case OBSTACLE:
                if (node.getStatus() != Node.NodeStatus.DONE) {
                    node.setStatus(Node.NodeStatus.ACTIVE);
                }
                break;

            case TRIGGER:
            case ROUTINE:
                if (node.getStatus() == null || node.getStatus() == Node.NodeStatus.REVIEW) {
                    node.setStatus(Node.NodeStatus.PLANNED);
                }
                break;

            default:
                if (node.getStatus() == null) {
                    node.setStatus(Node.NodeStatus.ACTIVE);
                }
                break;
        }

        if (node.getPriority() < 1 || node.getPriority() > 5) {
            node.setPriority(3);
        }

        if (node.getConfidence() <= 0f) {
            node.setConfidence(0.5f);
        }
    }

    public static List<Node> getInboxNodes(Map<String, Node> nodes) {
        List<Node> result = new ArrayList<>();
        if (nodes == null) return result;

        for (Node node : nodes.values()) {
            if (isInboxNode(node)) {
                result.add(node);
            }
        }
        sortByPriorityThenTitle(result);
        return result;
    }

    public static List<Node> getProjectNodes(Map<String, Node> nodes) {
        List<Node> result = new ArrayList<>();
        if (nodes == null) return result;

        for (Node node : nodes.values()) {
            if (node != null && node.getType() == Node.NodeType.PROJECT) {
                result.add(node);
            }
        }
        sortByPriorityThenTitle(result);
        return result;
    }

    public static List<Node> getReviewDueNodes(Map<String, Node> nodes) {
        List<Node> result = new ArrayList<>();
        if (nodes == null) return result;

        for (Node node : nodes.values()) {
            if (node == null) continue;
            if (node.getType() == Node.NodeType.REVIEW) {
                result.add(node);
                continue;
            }
            if (!isBlank(node.getReviewAt())) {
                result.add(node);
            }
        }
        sortByPriorityThenTitle(result);
        return result;
    }

    public static List<Node> getNextActions(Map<String, Node> nodes, Map<String, Connection> connections) {
        List<Node> result = new ArrayList<>();
        if (nodes == null) return result;

        for (Node node : nodes.values()) {
            if (!canBeNextAction(node)) continue;
            if (!hasUnfinishedDependency(node, nodes, connections)) {
                result.add(node);
            }
        }

        sortByPriorityThenTitle(result);
        return result;
    }

    public static List<Node> getStuckProjects(Map<String, Node> nodes, Map<String, Connection> connections) {
        List<Node> result = new ArrayList<>();
        if (nodes == null) return result;

        List<Node> projects = getProjectNodes(nodes);
        for (Node project : projects) {
            if (!hasOpenChildAction(project, nodes, connections)) {
                result.add(project);
            }
        }

        sortByPriorityThenTitle(result);
        return result;
    }

    public static boolean hasOpenChildAction(Node projectNode, Map<String, Node> nodes, Map<String, Connection> connections) {
        if (projectNode == null || nodes == null) return false;

        String ownerId = resolveOwnerId(projectNode);

        for (Node node : nodes.values()) {
            if (node == null) continue;
            if (!ownerId.equals(node.getProjectId())) continue;
            if (node.getId().equals(projectNode.getId())) continue;
            if (!isActionable(node)) continue;
            if (isDone(node)) continue;
            return true;
        }

        if (connections != null) {
            for (Connection connection : connections.values()) {
                if (connection == null) continue;
                if (!projectNode.getId().equals(connection.getFromNodeId())) continue;

                Node target = nodes.get(connection.getToNodeId());
                if (target == null) continue;
                if (isActionable(target) && !isDone(target)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean hasUnfinishedDependency(Node targetNode, Map<String, Node> nodes, Map<String, Connection> connections) {
        if (targetNode == null || nodes == null || connections == null) return false;

        for (Connection connection : connections.values()) {
            if (connection == null) continue;
            if (!targetNode.getId().equals(connection.getToNodeId())) continue;
            if (connection.getType() != Connection.ConnectionType.DEPENDS_ON) continue;

            Node dependency = nodes.get(connection.getFromNodeId());
            if (dependency != null && !isDone(dependency)) {
                return true;
            }
        }
        return false;
    }


    public static List<Node> getAreaNodes(Map<String, Node> nodes) {
        List<Node> result = new ArrayList<>();
        if (nodes == null) return result;
        for (Node node : nodes.values()) {
            if (node == null) continue;
            if (!isBlank(node.getAreaId())) {
                result.add(node);
                continue;
            }
            if (hasTag(node, "Area")) {
                result.add(node);
            }
        }
        sortByPriorityThenTitle(result);
        return result;
    }

    public static List<Node> getResourceNodes(Map<String, Node> nodes) {
        List<Node> result = new ArrayList<>();
        if (nodes == null) return result;
        for (Node node : nodes.values()) {
            if (isResourceNode(node) && !isArchived(node)) result.add(node);
        }
        sortByPriorityThenTitle(result);
        return result;
    }

    public static List<Node> getArchivedNodes(Map<String, Node> nodes) {
        List<Node> result = new ArrayList<>();
        if (nodes == null) return result;
        for (Node node : nodes.values()) {
            if (isArchived(node)) result.add(node);
        }
        sortByPriorityThenTitle(result);
        return result;
    }


    private static void sortByPriorityThenTitle(List<Node> nodes) {
        Collections.sort(nodes, new Comparator<Node>() {
            @Override
            public int compare(Node a, Node b) {
                int p = Integer.compare(b.getPriority(), a.getPriority());
                if (p != 0) return p;
                return safe(a.getTitle()).compareToIgnoreCase(safe(b.getTitle()));
            }
        });
    }
}
