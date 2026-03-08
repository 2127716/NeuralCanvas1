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

    private final SharedPreferences prefs;
    private final Gson gson;

    public SimpleDataManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveMindMap(Map<String, Node> nodes, Map<String, Connection> connections) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_NODES, gson.toJson(nodes));
        editor.putString(KEY_CONNECTIONS, gson.toJson(connections));
        editor.apply();
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

    public void saveAiConfig(AiConfig config) {
        prefs.edit().putString(KEY_AI_CONFIG, gson.toJson(config)).apply();
    }

    public AiConfig loadAiConfig() {
        try {
            String json = prefs.getString(KEY_AI_CONFIG, "");
            if (json == null || json.trim().isEmpty()) {
                return new AiConfig();
            }
            AiConfig config = gson.fromJson(json, AiConfig.class);
            return config == null ? new AiConfig() : config;
        } catch (Exception e) {
            e.printStackTrace();
            return new AiConfig();
        }
    }

    public void clearAll() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_NODES);
        editor.remove(KEY_CONNECTIONS);
        editor.apply();
    }
}
