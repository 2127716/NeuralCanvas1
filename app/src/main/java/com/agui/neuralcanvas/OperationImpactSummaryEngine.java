package com.agui.neuralcanvas;

import java.util.List;
import java.util.Locale;

public final class OperationImpactSummaryEngine {

    public static final class ImpactSummary {
        public int createNodeCount = 0;
        public int updateNodeCount = 0;
        public int deleteNodeCount = 0;
        public int createConnectionCount = 0;
        public int updateConnectionCount = 0;
        public int deleteConnectionCount = 0;
        public int focusCount = 0;
        public boolean hasStructuralRefactor = false;
        public boolean hasDestructiveChange = false;

        public String buildSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("改动摘要：");
            if (createNodeCount > 0) sb.append("\\n- 创建节点 ").append(createNodeCount).append(" 个");
            if (updateNodeCount > 0) sb.append("\\n- 修改节点 ").append(updateNodeCount).append(" 个");
            if (deleteNodeCount > 0) sb.append("\\n- 删除节点 ").append(deleteNodeCount).append(" 个");
            if (createConnectionCount > 0) sb.append("\\n- 创建连线 ").append(createConnectionCount).append(" 条");
            if (updateConnectionCount > 0) sb.append("\\n- 修改连线 ").append(updateConnectionCount).append(" 条");
            if (deleteConnectionCount > 0) sb.append("\\n- 删除连线 ").append(deleteConnectionCount).append(" 条");
            if (focusCount > 0) sb.append("\\n- 聚焦节点 ").append(focusCount).append(" 次");
            if (hasStructuralRefactor) sb.append("\\n- 涉及结构性重构");
            if (hasDestructiveChange) sb.append("\\n- 涉及破坏性改动，建议先确认");
            return sb.toString();
        }
    }

    private OperationImpactSummaryEngine() {}

    public static ImpactSummary analyze(AiResponse response) {
        ImpactSummary summary = new ImpactSummary();
        if (response == null || response.getCommands() == null) return summary;

        List<AiCommand> commands = response.getCommands();
        for (AiCommand cmd : commands) {
            if (cmd == null) continue;
            String action = safeLower(cmd.getAction());
            if ("create_node".equals(action)) summary.createNodeCount++;
            else if ("update_node".equals(action)) summary.updateNodeCount++;
            else if ("delete_node".equals(action)) {
                summary.deleteNodeCount++;
                summary.hasDestructiveChange = true;
            } else if ("create_connection".equals(action)) summary.createConnectionCount++;
            else if ("update_connection".equals(action)) summary.updateConnectionCount++;
            else if ("delete_connection".equals(action)) {
                summary.deleteConnectionCount++;
                summary.hasDestructiveChange = true;
            } else if ("focus_node".equals(action)) summary.focusCount++;

            if (summary.createNodeCount + summary.createConnectionCount >= 4
                    || summary.updateNodeCount + summary.updateConnectionCount >= 5) {
                summary.hasStructuralRefactor = true;
            }
        }
        return summary;
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
