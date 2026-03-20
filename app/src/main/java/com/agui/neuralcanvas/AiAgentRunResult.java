package com.agui.neuralcanvas;

public class AiAgentRunResult {
    public BrainAgentProfile profile = BrainAgentProfile.GENERAL;
    public AiResponse response = new AiResponse();
    public String summary = "";
    public int commandCount = 0;
    public long durationMs = 0L;

    public String buildLabel() {
        return profile == null ? "未知代理" : profile.label;
    }
}
