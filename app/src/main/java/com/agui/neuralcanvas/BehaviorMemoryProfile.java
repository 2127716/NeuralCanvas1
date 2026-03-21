package com.agui.neuralcanvas;

import java.util.LinkedHashMap;
import java.util.Map;

public class BehaviorMemoryProfile {
    public int totalPulses = 0;
    public int autoAppliedCount = 0;
    public int selfReviewRemovedCommands = 0;
    public int lowRiskAcceptedCount = 0;
    public int mediumOrHighRiskBlockedCount = 0;
    public long lastUpdatedAt = 0L;

    public Map<String, Integer> agentRunCount = new LinkedHashMap<>();
    public Map<String, Integer> nodeIssueCount = new LinkedHashMap<>();
    public Map<String, Integer> actionTypeCount = new LinkedHashMap<>();
    public Map<String, Integer> focusNodeHitCount = new LinkedHashMap<>();
}
