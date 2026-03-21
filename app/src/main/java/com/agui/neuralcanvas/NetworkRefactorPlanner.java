package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class NetworkRefactorPlanner {

    public static final class RefactorPlan {
        public final List<String> actions = new ArrayList<>();

        public String buildSummary() {
            if (actions.isEmpty()) return "暂无强制网络重构动作";
            StringBuilder sb = new StringBuilder();
            sb.append("建议网络重构动作：");
            for (String action : actions) sb.append("\n- ").append(action);
            return sb.toString();
        }
    }

    private NetworkRefactorPlanner() {}

    public static RefactorPlan build(Map<String, Node> nodes, Map<String, Connection> connections) {
        RefactorPlan plan = new RefactorPlan();
        NetworkEvolutionEngine.NetworkReport report = NetworkEvolutionEngine.analyze(nodes, connections);

        if (report.duplicateNodeCount > 0) {
            plan.actions.add("优先合并重复标题/重复语义节点，避免知识碎片化");
        }
        if (report.isolatedHighValueCount > 0) {
            plan.actions.add("为高价值孤点补桥接边，接入项目链或知识链");
        }
        if (report.brokenProjectCount > 0) {
            plan.actions.add("为断裂项目补 ACTION/TASK 子节点，恢复执行闭环");
        }
        if (report.brokenKnowledgeCount > 0) {
            plan.actions.add("为断裂知识节点补 QUESTION/SOURCE 链");
        }
        if (report.lowValueEdgeCount > 0) {
            plan.actions.add("清理重复边、空标签边和低信息密度边");
        }
        if (plan.actions.isEmpty() && report.similarNodeCount > 0) {
            plan.actions.add("优先桥接语义相似节点，避免隐性重复网络");
        }
        return plan;
    }
}
