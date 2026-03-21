package com.agui.neuralcanvas;

import java.util.LinkedHashMap;
import java.util.Map;

public class SuggestionFeedbackProfile {
    public int acceptedCount = 0;
    public int rejectedCount = 0;
    public int autoAppliedCount = 0;
    public int effectiveCount = 0;
    public long lastUpdatedAt = 0L;

    public Map<String, Integer> acceptedActionTypeCount = new LinkedHashMap<>();
    public Map<String, Integer> rejectedActionTypeCount = new LinkedHashMap<>();
    public Map<String, Integer> effectiveActionTypeCount = new LinkedHashMap<>();
    public Map<String, Integer> agentAcceptedCount = new LinkedHashMap<>();
    public Map<String, Integer> agentRejectedCount = new LinkedHashMap<>();
}
