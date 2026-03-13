package com.agui.neuralcanvas;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

/**
 * ThemeManager — 三种主题
 *
 * MODERN  现代深色（深紫黑，原始风格）
 * MONET   莫奈印象（取自莫奈《睡莲》色板：深靛蓝、水草绿、藕荷紫、暮光橙）
 * LIQUID  液态玻璃（Apple visionOS 风格：极深灰黑底，高透明磨砂白，冰蓝绿 accent）
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
    private static AppTheme current = AppTheme.MODERN;

    public static void init(Context ctx) {
        String saved = ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                .getString(KEY_THEME, AppTheme.MODERN.name());
        try { current = AppTheme.valueOf(saved); } catch (Exception e) { current = AppTheme.MODERN; }
    }

    public static AppTheme getCurrentTheme() { return current; }

    public static void setTheme(Context ctx, AppTheme t) {
        current = t;
        ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                .edit().putString(KEY_THEME, t.name()).apply();
    }

    // ─── Colors ─────────────────────────────────────────────────────────────

    /** Main canvas background */
    public static int getBg() {
        switch (current) {
            case MONET:  return Color.parseColor("#0D1520"); // 深靛夜蓝
            case LIQUID: return Color.parseColor("#08090C"); // 极深纯黑
            default:     return Color.parseColor("#070B14");
        }
    }

    /** Dialog / sheet surface */
    public static int getDialogBg() {
        switch (current) {
            case MONET:  return Color.parseColor("#111E30"); // 深蓝绿
            case LIQUID: return Color.parseColor("#18181E"); // 深磨砂黑
            default:     return Color.parseColor("#0F172A");
        }
    }

    /** Card / chip / input surface */
    public static int getSurface() {
        switch (current) {
            case MONET:  return Color.parseColor("#162338");
            case LIQUID: return Color.parseColor("#141418");
            default:     return Color.parseColor("#0F172A");
        }
    }

    /** Primary accent — interactive highlights */
    public static int getAccent() {
        switch (current) {
            case MONET:  return Color.parseColor("#7BA7BC"); // 莫奈水面蓝
            case LIQUID: return Color.parseColor("#64D2FF"); // visionOS 冰蓝
            default:     return Color.parseColor("#8B5CF6");
        }
    }

    /** Secondary accent */
    public static int getAccent2() {
        switch (current) {
            case MONET:  return Color.parseColor("#8FAF7E"); // 莫奈荷叶绿
            case LIQUID: return Color.parseColor("#30D158"); // Apple 绿
            default:     return Color.parseColor("#38BDF8");
        }
    }

    /** Primary text — headings, labels */
    public static int getTextPrimary() {
        switch (current) {
            case MONET:  return Color.parseColor("#E4EAF0"); // 晨雾白
            case LIQUID: return Color.parseColor("#F5F5F7"); // Apple 白
            default:     return Color.parseColor("#F8FAFC");
        }
    }

    /** Secondary text — hints, subtitles */
    public static int getTextSecondary() {
        switch (current) {
            case MONET:  return Color.parseColor("#8FAABB"); // 烟蓝灰
            case LIQUID: return Color.parseColor("#98989D"); // Apple 灰
            default:     return Color.parseColor("#A8B3CF");
        }
    }

    /** Dividers and borders */
    public static int getStroke() {
        switch (current) {
            case MONET:  return Color.parseColor("#1E3248"); // 深靛分隔线
            case LIQUID: return Color.parseColor("#FFFFFF1A"); // 半透明白线
            default:     return Color.parseColor("#23314D");
        }
    }

    /** Chip / action button background */
    public static int getChipBg() {
        switch (current) {
            case MONET:  return Color.parseColor("#152236"); // 深水蓝
            case LIQUID: return Color.parseColor("#FFFFFF0F"); // 极淡磨砂
            default:     return Color.parseColor("#182338");
        }
    }

    /** Chip border */
    public static int getChipStroke() {
        switch (current) {
            case MONET:  return Color.parseColor("#2B4060"); // 蓝灰边
            case LIQUID: return Color.parseColor("#FFFFFF28"); // 半透明白边
            default:     return Color.parseColor("#334155");
        }
    }

    /** EditText background */
    public static int getEditTextBg() {
        switch (current) {
            case MONET:  return Color.parseColor("#0F1A2A");
            case LIQUID: return Color.parseColor("#FFFFFF08");
            default:     return Color.parseColor("#111827");
        }
    }

    /** EditText stroke */
    public static int getEditTextStroke() {
        switch (current) {
            case MONET:  return Color.parseColor("#1E3248");
            case LIQUID: return Color.parseColor("#FFFFFF22");
            default:     return Color.parseColor("#24324D");
        }
    }

    /** Spinner background */
    public static int getSpinnerBg() {
        switch (current) {
            case MONET:  return Color.parseColor("#0F1A2A");
            case LIQUID: return Color.parseColor("#FFFFFF0F");
            default:     return Color.parseColor("#182338");
        }
    }

    /** Toolbar background color (used programmatically) */
    public static int getToolbarBg() {
        switch (current) {
            case MONET:  return Color.parseColor("#CC111E30");
            case LIQUID: return Color.parseColor("#AA08090C");
            default:     return Color.parseColor("#D90F172A");
        }
    }

    /** Popup / menu background */
    public static int getPopupBg() {
        switch (current) {
            case MONET:  return Color.parseColor("#111E30");
            case LIQUID: return Color.parseColor("#18181E");
            default:     return Color.parseColor("#0F172A");
        }
    }

    public static int getDanger() { return Color.parseColor("#F43F5E"); }

    /** Connect / link action color */
    public static int getLinkColor() {
        switch (current) {
            case MONET:  return Color.parseColor("#8FAF7E");
            case LIQUID: return Color.parseColor("#30D158");
            default:     return Color.parseColor("#34D399");
        }
    }

    /** Section title for monet: accent color of season (warm amber) */
    public static int getSectionTitleColor() {
        switch (current) {
            case MONET:  return Color.parseColor("#7BA7BC");
            case LIQUID: return Color.parseColor("#64D2FF");
            default:     return Color.parseColor("#8B5CF6");
        }
    }
}
