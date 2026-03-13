package com.agui.neuralcanvas;

import java.util.LinkedHashMap;
import java.util.Map;

public final class GraphMetaHelper {
    private GraphMetaHelper() {}

    public static Map<String, String> parse(String meta) {
        Map<String, String> map = new LinkedHashMap<>();
        if (meta == null || meta.trim().isEmpty()) return map;
        String normalized = meta.replace(';', '
');
        String[] lines = normalized.split("\n");
        for (String line : lines) {
            if (line == null) continue;
            String item = line.trim();
            if (item.isEmpty()) continue;
            int index = item.indexOf('=');
            if (index <= 0) continue;
            String key = item.substring(0, index).trim();
            String value = item.substring(index + 1).trim();
            if (!key.isEmpty()) map.put(key, value);
        }
        return map;
    }

    public static String stringify(Map<String, String> map) {
        if (map == null || map.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append('
');
            sb.append(entry.getKey().trim()).append('=').append(entry.getValue() == null ? "" : entry.getValue().trim());
        }
        return sb.toString();
    }

    public static String getString(Node node, String key, String defaultValue) {
        if (node == null || key == null) return defaultValue;
        Map<String, String> map = parse(node.getMetaJson());
        String value = map.get(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    public static int getInt(Node node, String key, int defaultValue) {
        try { return Integer.parseInt(getString(node, key, String.valueOf(defaultValue))); }
        catch (Exception e) { return defaultValue; }
    }

    public static long getLong(Node node, String key, long defaultValue) {
        try { return Long.parseLong(getString(node, key, String.valueOf(defaultValue))); }
        catch (Exception e) { return defaultValue; }
    }

    public static float getFloat(Node node, String key, float defaultValue) {
        try { return Float.parseFloat(getString(node, key, String.valueOf(defaultValue))); }
        catch (Exception e) { return defaultValue; }
    }

    public static boolean getBoolean(Node node, String key, boolean defaultValue) {
        String value = getString(node, key, String.valueOf(defaultValue));
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    public static void put(Node node, String key, String value) {
        if (node == null || key == null || key.trim().isEmpty()) return;
        Map<String, String> map = parse(node.getMetaJson());
        map.put(key.trim(), value == null ? "" : value.trim());
        node.setMetaJson(stringify(map));
    }

    public static void putInt(Node node, String key, int value) { put(node, key, String.valueOf(value)); }
    public static void putLong(Node node, String key, long value) { put(node, key, String.valueOf(value)); }
    public static void putFloat(Node node, String key, float value) { put(node, key, String.valueOf(value)); }
    public static void putBoolean(Node node, String key, boolean value) { put(node, key, value ? "true" : "false"); }
}
