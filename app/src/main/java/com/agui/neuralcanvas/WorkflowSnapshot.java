package com.agui.neuralcanvas;

import java.util.List;
import java.util.Map;

public class WorkflowSnapshot {

    public final int inboxCount;
    public final int projectCount;
    public final int nextActionCount;
    public final int reviewDueCount;
    public final int stuckProjectCount;

    public WorkflowSnapshot(int inboxCount,
                            int projectCount,
                            int nextActionCount,
                            int reviewDueCount,
                            int stuckProjectCount) {
        this.inboxCount = inboxCount;
        this.projectCount = projectCount;
        this.nextActionCount = nextActionCount;
        this.reviewDueCount = reviewDueCount;
        this.stuckProjectCount = stuckProjectCount;
    }

    public static WorkflowSnapshot from(Map<String, Node> nodes, Map<String, Connection> connections) {
        List<Node> inbox = WorkflowEngine.getInboxNodes(nodes);
        List<Node> projects = WorkflowEngine.getProjectNodes(nodes);
        List<Node> nextActions = WorkflowEngine.getNextActions(nodes, connections);
        List<Node> reviewDue = WorkflowEngine.getReviewDueNodes(nodes);
        List<Node> stuckProjects = WorkflowEngine.getStuckProjects(nodes, connections);

        return new WorkflowSnapshot(
                inbox.size(),
                projects.size(),
                nextActions.size(),
                reviewDue.size(),
                stuckProjects.size()
        );
    }
}
