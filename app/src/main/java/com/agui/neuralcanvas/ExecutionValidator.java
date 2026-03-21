package com.agui.neuralcanvas;

public final class ExecutionValidator {
    private ExecutionValidator() {}

    public static boolean isExecutable(Node n) {
        if (n == null) return false;
        return !safe(n.getTriggerCondition()).isEmpty()
                && (!safe(n.getDueAt()).isEmpty() || !safe(n.getReviewAt()).isEmpty());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
