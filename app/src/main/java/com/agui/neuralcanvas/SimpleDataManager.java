package com.agui.neuralcanvas;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * 简单的数据管理器，使用SharedPreferences存储思维地图数据
 * 这是一个临时方案，让应用能先运行起来
 */
public class SimpleDataManager {
    private static final String PREFS_NAME = "NeuralCanvasPrefs";
    private static final String KEY_NODES = "nodes";
    private static final String KEY_CONNECTIONS = "connections";
    
    private SharedPreferences prefs;
    private Gson gson;
    
    public SimpleDataManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }
    
    /**
     * 保存整个思维地图
     */
    public void saveMindMap(Map<String, Node> nodes, Map<String, Connection> connections) {
        SharedPreferences.Editor editor = prefs.edit();
        
        // 将节点Map转换为JSON字符串
        String nodesJson = gson.toJson(nodes);
        editor.putString(KEY_NODES, nodesJson);
        
        // 将连接Map转换为JSON字符串
        String connectionsJson = gson.toJson(connections);
        editor.putString(KEY_CONNECTIONS, connectionsJson);
        
        editor.apply();
    }
    
    /**
     * 加载整个思维地图
     */
    public Map<String, Object> loadMindMap() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 加载节点
            String nodesJson = prefs.getString(KEY_NODES, "{}");
            Type nodeMapType = new TypeToken<Map<String, Node>>(){}.getType();
            Map<String, Node> nodes = gson.fromJson(nodesJson, nodeMapType);
            
            // 加载连接
            String connectionsJson = prefs.getString(KEY_CONNECTIONS, "{}");
            Type connectionMapType = new TypeToken<Map<String, Connection>>(){}.getType();
            Map<String, Connection> connections = gson.fromJson(connectionsJson, connectionMapType);
            
            result.put("nodes", nodes != null ? nodes : new HashMap<String, Node>());
            result.put("connections", connections != null ? connections : new HashMap<String, Connection>());
            
        } catch (Exception e) {
            e.printStackTrace();
            // 如果解析失败，返回空Map
            result.put("nodes", new HashMap<String, Node>());
            result.put("connections", new HashMap<String, Connection>());
        }
        
        return result;
    }
    
    /**
     * 清空所有数据
     */
    public void clearAll() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_NODES);
        editor.remove(KEY_CONNECTIONS);
        editor.apply();
    }
}