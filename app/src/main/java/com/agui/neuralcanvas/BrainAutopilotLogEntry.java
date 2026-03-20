package com.agui.neuralcanvas;

public class BrainAutopilotLogEntry {
    public long timestamp = System.currentTimeMillis();
    public String agentProfile = "auto";
    public String summary = "";
    public String focusNodeId = "";
    public String focusNodeTitle = "";
    public String riskLevel = "LOW";
    public boolean autoApplied = false;
    public String responseJson = "";
}
