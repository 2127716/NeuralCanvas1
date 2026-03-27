package com.agui.neuralcanvas;

public class BrainAutopilotSettings {
    private boolean enabled = true;
    private boolean notificationsEnabled = true;
    private int intervalHours = 8;
    private boolean openModeOnNotificationTap = true;
    private boolean apiAutopilotEnabled = true;
    private boolean autoApplyLowRiskChanges = true;
    private boolean inAppPulseOnResume = true;
    private boolean coachPopupOnLaunch = true;
    private String assistantScope = "relevant";
    private String preferredAutopilotAgent = "auto";
    private String autopilotInstruction = "请作为我的第二大脑，自动分析图谱，优先补执行闭环、学习闭环、决策证据和关键连接。保守修改，尽量少但高价值。";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }
    public int getIntervalHours() { return intervalHours < 1 ? 8 : intervalHours; }
    public void setIntervalHours(int intervalHours) { this.intervalHours = Math.max(1, intervalHours); }
    public boolean isOpenModeOnNotificationTap() { return openModeOnNotificationTap; }
    public void setOpenModeOnNotificationTap(boolean openModeOnNotificationTap) { this.openModeOnNotificationTap = openModeOnNotificationTap; }
    public boolean isApiAutopilotEnabled() { return apiAutopilotEnabled; }
    public void setApiAutopilotEnabled(boolean apiAutopilotEnabled) { this.apiAutopilotEnabled = apiAutopilotEnabled; }
    public boolean isAutoApplyLowRiskChanges() { return autoApplyLowRiskChanges; }
    public void setAutoApplyLowRiskChanges(boolean autoApplyLowRiskChanges) { this.autoApplyLowRiskChanges = autoApplyLowRiskChanges; }
    public boolean isInAppPulseOnResume() { return inAppPulseOnResume; }
    public void setInAppPulseOnResume(boolean inAppPulseOnResume) { this.inAppPulseOnResume = inAppPulseOnResume; }
    public boolean isCoachPopupOnLaunch() { return coachPopupOnLaunch; }
    public void setCoachPopupOnLaunch(boolean coachPopupOnLaunch) { this.coachPopupOnLaunch = coachPopupOnLaunch; }
    public String getAssistantScope() { String v = assistantScope == null ? "relevant" : assistantScope.trim().toLowerCase(); return "full".equals(v) ? "full" : "relevant"; }
    public void setAssistantScope(String assistantScope) { this.assistantScope = assistantScope; }
    public String getPreferredAutopilotAgent() {
        String v = preferredAutopilotAgent == null ? "auto" : preferredAutopilotAgent.trim().toLowerCase();
        if ("execution".equals(v) || "decision".equals(v) || "learning".equals(v) || "network".equals(v) || "general".equals(v)) return v;
        return "auto";
    }
    public void setPreferredAutopilotAgent(String preferredAutopilotAgent) { this.preferredAutopilotAgent = preferredAutopilotAgent; }
    public String getAutopilotInstruction() {
        String v = autopilotInstruction == null ? "" : autopilotInstruction.trim();
        return v.isEmpty() ? "请作为我的第二大脑，自动分析图谱，优先补执行闭环、学习闭环、决策证据和关键连接。保守修改，尽量少但高价值。" : v;
    }
    public void setAutopilotInstruction(String autopilotInstruction) { this.autopilotInstruction = autopilotInstruction; }
}
