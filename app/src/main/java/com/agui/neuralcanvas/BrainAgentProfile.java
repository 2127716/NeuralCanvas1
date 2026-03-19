package com.agui.neuralcanvas;

public enum BrainAgentProfile {
    AUTO("auto", "自动选择"),
    EXECUTION("execution", "执行代理"),
    DECISION("decision", "决策代理"),
    LEARNING("learning", "学习代理"),
    NETWORK("network", "网络整理代理"),
    GENERAL("general", "通用代理");

    public final String key;
    public final String label;

    BrainAgentProfile(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public static BrainAgentProfile fromKey(String value) {
        String v = value == null ? "" : value.trim().toLowerCase();
        for (BrainAgentProfile item : values()) {
            if (item.key.equals(v)) return item;
        }
        return AUTO;
    }
}
