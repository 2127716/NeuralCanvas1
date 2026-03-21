package com.agui.neuralcanvas;

import java.util.LinkedHashMap;
import java.util.Map;

public class AgentRunHistoryProfile {
    public int totalRuns = 0;
    public long lastUpdatedAt = 0L;
    public Map<String, Integer> runCountByAgent = new LinkedHashMap<>();
    public Map<String, Integer> keptCountByAgent = new LinkedHashMap<>();
    public Map<String, Integer> droppedCountByAgent = new LinkedHashMap<>();
    public Map<String, Integer> effectiveCountByAgent = new LinkedHashMap<>();
    public Map<String, Integer> avgCommandBucketsByAgent = new LinkedHashMap<>();
}
