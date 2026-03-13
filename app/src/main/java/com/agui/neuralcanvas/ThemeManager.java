package com.agui.neuralcanvas;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

/**
 * ThemeManager — 支持三种主题：
 *   MONET    莫奈印象派（蓝紫绿柔和渐变）
 *   LIQUID   液态玻璃（Apple 风格半透明磨砂）
 *   MODERN   现代深色（当前默认紫黑风格）
 */
public class ThemeManager {

    public enum AppTheme {
        MODERN("现代深色"),
        MONET("莫奈印象"),
        LIQUID("液态玻璃");

        public final String label;
        AppTheme(String label) { this.label = label; }
    }

    private static final String PREF_FILE = "nc_prefs";
    private static final String KEY_THEME = "app_theme";

    private static AppTheme currentTheme = AppTheme.MODERN;

    public static void init(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY_THEME, AppTheme.MODERN.name());
        try {
            currentTheme = AppTheme.valueOf(saved);
        } catch (Exception e) {
            currentTheme = AppTheme.MODERN;
        }
    }

    public static AppTheme getCurrentTheme() { return currentTheme; }

    public static void setTheme(Context ctx, AppTheme theme) {
        currentTheme = theme;
        ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                .edit().putString(KEY_THEME, theme.name()).apply();
    }

    // ─── Color getters ───────────────────────────────────────────────────────

    public static int getBg() {
        switch (currentTheme) {
            case MONET:  return Color.parseColor("#1A1E2E");   // 深靛蓝
            case LIQUID: return Color.parseColor("#0D1117");   // 极深灰黑
            default:     return Color.parseColor("#070B14");
        }
    }

    public static int getSurface() {
        switch (currentTheme) {
            case MONET:  return Color.parseColor("#1F2640");
            case LIQUID: return Color.parseColor("#141B26");
            default:     return Color.parseColor("#0F172A");
        }
    }

    public static int getAccent() {
        switch (currentTheme) {
            case MONET:  return Color.parseColor("#6B8DD6");   // 莫奈蓝
            case LIQUID: return Color.parseColor("#30D158");   // Apple 绿
            default:     return Color.parseColor("#8B5CF6");
        }
    }

    public static int getAccent2() {
        switch (currentTheme) {
            case MONET:  return Color.parseColor("#A8C5A0");   // 莫奈绿
            case LIQUID: return Color.parseColor("#0A84FF");   // Apple 蓝
            default:     return Color.parseColor("#38BDF8");
        }
    }

    public static int getTextPrimary() {
        switch (currentTheme) {
            case MONET:  return Color.parseColor("#E8EAF6");
            case LIQUID: return Color.parseColor("#F5F5F7");
            default:     return Color.parseColor("#F8FAFC");
        }
    }

    public static int getTextSecondary() {
        switch (currentTheme) {
            case MONET:  return Color.parseColor("#B0BECF");
            case LIQUID: return Color.parseColor("#AEAEB2");
            default:     return Color.parseColor("#A8B3CF");
        }
    }

    public static int getStroke() {
        switch (currentTheme) {
            case MONET:  return Color.parseColor("#2E3A5C");
            case LIQUID: return Color.parseColor("#FFFFFF22");
            default:     return Color.parseColor("#23314D");
        }
    }

    public static int getDialogBg() {
        switch (currentTheme) {
            case MONET:  return Color.parseColor("#1F2640");
            case LIQUID: return Color.parseColor("#1C1C1ECC"); // semi-transparent
            default:     return Color.parseColor("#0F172A");
        }
    }

    public static int getChipBg() {
        switch (currentTheme) {
            case MONET:  return Color.parseColor("#243058");
            case LIQUID: return Color.parseColor("#FFFFFF18");
            default:     return Color.parseColor("#182338");
        }
    }

    public static int getChipStroke() {
        switch (currentTheme) {
            case MONET:  return Color.parseColor("#3D5280");
            case LIQUID: return Color.parseColor("#FFFFFF35");
            default:     return Color.parseColor("#334155");
        }
    }

    public static int getEditTextBg() {
        switch (currentTheme) {
            case MONET:  return Color.parseColor("#16203A");
            case LIQUID: return Color.parseColor("#FFFFFF10");
            default:     return Color.parseColor("#111827");
        }
    }

    public static int getEditTextStroke() {
        switch (currentTheme) {
            case MONET:  return Color.parseColor("#2E3F60");
            case LIQUID: return Color.parseColor("#FFFFFF30");
            default:     return Color.parseColor("#24324D");
        }
    }

    public static int getSpinnerBg() {
        switch (currentTheme) {
            case MONET:  return Color.parseColor("#16203A");
            case LIQUID: return Color.parseColor("#FFFFFF15");
            default:     return Color.parseColor("#182338");
        }
    }

    public static int getToolbarBg() {
        switch (currentTheme) {
            case MONET:  return Color.parseColor("#CC1F2640");   // semi-transparent indigo
            case LIQUID: return Color.parseColor("#88101820");   // frosted glass
            default:     return Color.parseColor("#D90F172A");
        }
    }

    public static int getPopupBg() {
        switch (currentTheme) {
            case MONET:  return Color.parseColor("#1F2640");
            case LIQUID: return Color.parseColor("#1E1E2EEE");
            default:     return Color.parseColor("#0F172A");
        }
    }

    public static int getDanger() {
        return Color.parseColor("#F43F5E");
    }

    public static int getLinkColor() {
        switch (currentTheme) {
            case MONET:  return Color.parseColor("#A8C5A0");
            case LIQUID: return Color.parseColor("#30D158");
            default:     return Color.parseColor("#34D399");
        }
    }
}
