
package com.agui.neuralcanvas;

public class ExecutionValidator {
    public static boolean isExecutable(Node n) {
        if (n == null) return false;
        return n.getTriggerCondition() != null
                && !n.getTriggerCondition().isEmpty()
                && n.getDueAt() > 0;
    }
}
