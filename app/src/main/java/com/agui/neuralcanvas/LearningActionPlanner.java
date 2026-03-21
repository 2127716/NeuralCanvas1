package com.agui.neuralcanvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LearningActionPlanner {

    public static final class ActionPlan {
        public final List<String> actions = new ArrayList<>();
        public String nextBestAction = "优先补一个检索问题";

        public String buildSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("学习动作建议：").append("\n下一最佳动作：").append(nextBestAction);
            for (String action : actions) sb.append("\n- ").append(action);
            return sb.toString();
        }
    }

    private LearningActionPlanner() {}

    public static ActionPlan build(Map<String, Node> nodes, Map<String, Connection> connections) {
        ActionPlan plan = new ActionPlan();
        LearningLoopEngine.LearningReport loop = LearningLoopEngine.analyze(nodes, connections);
        LearningTransferEngine.TransferReport transfer = LearningTransferEngine.analyze(nodes, connections);

        if (loop.missingQuestionCount > 0) {
            plan.actions.add("给高价值概念节点补检索问题，避免只看不回忆");
        }
        if (loop.missingSourceCount > 0) {
            plan.actions.add("给核心知识节点补来源，增强可追溯性");
        }
        if (loop.missingReviewCount > 0) {
            plan.actions.add("为缺复习锚点的学习节点自动安排 reviewAt");
        }
        if (transfer.missingTransferCount > 0) {
            plan.actions.add("为高频学习节点补迁移任务/小实验");
        }
        if (transfer.roteDefinitionCount > 0) {
            plan.actions.add("把定义型节点改造成 问题-答案-应用 三联结构");
        }
        if (transfer.sourceWithoutQuestionCount > 0) {
            plan.actions.add("把来源节点串成问题链，不要只堆参考资料");
        }

        plan.nextBestAction = transfer.nextBestAction;
        return plan;
    }
}
