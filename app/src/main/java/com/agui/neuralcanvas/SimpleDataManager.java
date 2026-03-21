package com.agui.neuralcanvas;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class SimpleDataManager {
    private static final String PREFS_NAME = "NeuralCanvasPrefs";
    private static final String KEY_NODES = "nodes";
    private static final String KEY_CONNECTIONS = "connections";
    private static final String KEY_AI_CONFIG = "ai_config";
    private static final String KEY_AUTOPILOT_SETTINGS = "autopilot_settings";
    private static final String KEY_LAST_BRAIN_PULSE_AT = "last_brain_pulse_at";
    private static final String KEY_LAST_BRAIN_PULSE_SUMMARY = "last_brain_pulse_summary";
    private static final String KEY_PENDING_BRAIN_GUIDANCE = "pending_brain_guidance";
    private static final String KEY_BEHAVIOR_MEMORY = "behavior_memory_profile";
    private static final String KEY_SUGGESTION_FEEDBACK = "suggestion_feedback_profile";
    private final SharedPreferences prefs;
    private final Gson gson;

    public SimpleDataManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveMindMap(Map<String, Node> nodes, Map<String, Connection> connections) {
        prefs.edit()
                .putString(KEY_NODES, gson.toJson(nodes))
                .putString(KEY_CONNECTIONS, gson.toJson(connections))
                .apply();
    }

    public Map<String, Object> loadMindMap() {
        Map<String, Object> result = new HashMap<>();
        try {
            String nodesJson = prefs.getString(KEY_NODES, "{}");
            Type nodeMapType = new TypeToken<Map<String, Node>>() {}.getType();
            Map<String, Node> nodes = gson.fromJson(nodesJson, nodeMapType);

            String connectionsJson = prefs.getString(KEY_CONNECTIONS, "{}");
            Type connectionMapType = new TypeToken<Map<String, Connection>>() {}.getType();
            Map<String, Connection> connections = gson.fromJson(connectionsJson, connectionMapType);

            result.put("nodes", nodes != null ? nodes : new HashMap<String, Node>());
            result.put("connections", connections != null ? connections : new HashMap<String, Connection>());
        } catch (Exception e) {
            e.printStackTrace();
            result.put("nodes", new HashMap<String, Node>());
            result.put("connections", new HashMap<String, Connection>());
        }
        return result;
    }

    public void saveAiConfig(AiConfig config) { prefs.edit().putString(KEY_AI_CONFIG, gson.toJson(config)).apply(); }

    public AiConfig loadAiConfig() {
        try {
            String json = prefs.getString(KEY_AI_CONFIG, "");
            if (json == null || json.trim().isEmpty()) return new AiConfig();
            AiConfig config = gson.fromJson(json, AiConfig.class);
            return config == null ? new AiConfig() : config;
        } catch (Exception e) {
            e.printStackTrace();
            return new AiConfig();
        }
    }

    public void saveAutopilotSettings(BrainAutopilotSettings settings) { prefs.edit().putString(KEY_AUTOPILOT_SETTINGS, gson.toJson(settings)).apply(); }

    public BrainAutopilotSettings loadAutopilotSettings() {
        try {
            String json = prefs.getString(KEY_AUTOPILOT_SETTINGS, "");
            if (json == null || json.trim().isEmpty()) return new BrainAutopilotSettings();
            BrainAutopilotSettings settings = gson.fromJson(json, BrainAutopilotSettings.class);
            return settings == null ? new BrainAutopilotSettings() : settings;
        } catch (Exception e) {
            e.printStackTrace();
            return new BrainAutopilotSettings();
        }
    }

    public void saveLastBrainPulse(long timestamp, String summary) {
        prefs.edit().putLong(KEY_LAST_BRAIN_PULSE_AT, timestamp).putString(KEY_LAST_BRAIN_PULSE_SUMMARY, summary == null ? "" : summary).apply();
    }

    public long loadLastBrainPulseAt() { return prefs.getLong(KEY_LAST_BRAIN_PULSE_AT, 0L); }
    public String loadLastBrainPulseSummary() {
        String value = prefs.getString(KEY_LAST_BRAIN_PULSE_SUMMARY, "");
        return value == null ? "" : value;
    }

    public void savePendingBrainGuidance(BrainPendingGuidance guidance) { prefs.edit().putString(KEY_PENDING_BRAIN_GUIDANCE, gson.toJson(guidance)).apply(); }

    public BrainPendingGuidance loadPendingBrainGuidance() {
        try {
            String json = prefs.getString(KEY_PENDING_BRAIN_GUIDANCE, "");
            if (json == null || json.trim().isEmpty()) return null;
            return gson.fromJson(json, BrainPendingGuidance.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void clearPendingBrainGuidance() { prefs.edit().remove(KEY_PENDING_BRAIN_GUIDANCE).apply(); }

    public void saveBehaviorMemoryProfile(BehaviorMemoryProfile profile) { prefs.edit().putString(KEY_BEHAVIOR_MEMORY, gson.toJson(profile)).apply(); }

    public BehaviorMemoryProfile loadBehaviorMemoryProfile() {
        try {
            String json = prefs.getString(KEY_BEHAVIOR_MEMORY, "");
            if (json == null || json.trim().isEmpty()) return new BehaviorMemoryProfile();
            BehaviorMemoryProfile profile = gson.fromJson(json, BehaviorMemoryProfile.class);
            return profile == null ? new BehaviorMemoryProfile() : profile;
        } catch (Exception e) {
            e.printStackTrace();
            return new BehaviorMemoryProfile();
        }
    }

    public void saveSuggestionFeedbackProfile(SuggestionFeedbackProfile profile) {
        prefs.edit().putString(KEY_SUGGESTION_FEEDBACK, gson.toJson(profile)).apply();
    }

    public SuggestionFeedbackProfile loadSuggestionFeedbackProfile() {
        try {
            String json = prefs.getString(KEY_SUGGESTION_FEEDBACK, "");
            if (json == null || json.trim().isEmpty()) return new SuggestionFeedbackProfile();
            SuggestionFeedbackProfile profile = gson.fromJson(json, SuggestionFeedbackProfile.class);
            return profile == null ? new SuggestionFeedbackProfile() : profile;
        } catch (Exception e) {
            e.printStackTrace();
            return new SuggestionFeedbackProfile();
        }
    }

    public void clearAll() {
        prefs.edit()
                .remove(KEY_NODES)
                .remove(KEY_CONNECTIONS)
                .remove(KEY_PENDING_BRAIN_GUIDANCE)
                .remove(KEY_BEHAVIOR_MEMORY)
                .remove(KEY_SUGGESTION_FEEDBACK)
                .apply();
    }
}
