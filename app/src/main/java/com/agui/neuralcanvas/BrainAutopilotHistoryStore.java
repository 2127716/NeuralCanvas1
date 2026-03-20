package com.agui.neuralcanvas;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class BrainAutopilotHistoryStore {
    private static final String PREFS_NAME = "NeuralCanvasPrefs";
    private static final String KEY_HISTORY = "brain_autopilot_history_v1";
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public BrainAutopilotHistoryStore(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void append(BrainAutopilotLogEntry entry) {
        if (entry == null) return;
        List<BrainAutopilotLogEntry> all = loadAll();
        all.add(0, entry);
        if (all.size() > 50) {
            all = new ArrayList<>(all.subList(0, 50));
        }
        prefs.edit().putString(KEY_HISTORY, gson.toJson(all)).apply();
    }

    public List<BrainAutopilotLogEntry> loadAll() {
        try {
            String json = prefs.getString(KEY_HISTORY, "[]");
            Type type = new TypeToken<List<BrainAutopilotLogEntry>>() {}.getType();
            List<BrainAutopilotLogEntry> list = gson.fromJson(json, type);
            return list == null ? new ArrayList<BrainAutopilotLogEntry>() : list;
        } catch (Exception e) {
            return new ArrayList<BrainAutopilotLogEntry>();
        }
    }

    public void clear() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }
}
