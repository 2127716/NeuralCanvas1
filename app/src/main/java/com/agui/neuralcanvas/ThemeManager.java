package com.agui.neuralcanvas;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;

public class ThemeManager {

    public enum AppTheme {
        SYSTEM_MONET("系统莫奈"),
        MONET("静态莫奈"),
        MODERN("现代深色");
        public final String label;
        AppTheme(String label) { this.label = label; }
    }

    private static final String PREF_FILE = "nc_prefs";
    private static final String KEY_THEME = "app_theme";
    private static AppTheme current = AppTheme.SYSTEM_MONET;
    private static Context appContext;

    public static void init(Context ctx) {
        appContext = ctx.getApplicationContext();
        String saved = appContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                .getString(KEY_THEME, AppTheme.SYSTEM_MONET.name());
        try { current = AppTheme.valueOf(saved); } catch (Exception e) { current = AppTheme.SYSTEM_MONET; }
    }

    public static AppTheme getCurrentTheme() { return current; }

    public static void setTheme(Context ctx, AppTheme t) {
        appContext = ctx.getApplicationContext();
        current = t;
        appContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                .edit().putString(KEY_THEME, t.name()).apply();
    }

    private static boolean isNight() {
        if (appContext == null) return true;
        int nightMode = appContext.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode != Configuration.UI_MODE_NIGHT_NO;
    }

    private static int dynamicOr(int fallbackDark, int fallbackLight) {
        if (current != AppTheme.SYSTEM_MONET || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return isNight() ? fallbackDark : fallbackLight;
        }
        return isNight() ? fallbackDark : fallbackLight;
    }

    public static int getBg() {
        if (current == AppTheme.MODERN) return Color.parseColor("#070B14");
        if (current == AppTheme.MONET) return Color.parseColor("#0E1520");
        return dynamicOr(Color.parseColor("#0E1520"), Color.parseColor("#F5F7FB"));
    }

    public static int getDialogBg() {
        if (current == AppTheme.MODERN) return Color.parseColor("#0F172A");
        if (current == AppTheme.MONET) return Color.parseColor("#111E30");
        return dynamicOr(Color.parseColor("#111E30"), Color.parseColor("#FFFFFF"));
    }

    public static int getSurface() {
        if (current == AppTheme.MODERN) return Color.parseColor("#111827");
        if (current == AppTheme.MONET) return Color.parseColor("#162338");
        return dynamicOr(Color.parseColor("#162338"), Color.parseColor("#EEF2F9"));
    }

    public static int getAccent() {
        if (current == AppTheme.MODERN) return Color.parseColor("#8B5CF6");
        if (current == AppTheme.MONET) return Color.parseColor("#7BA7BC");
        return dynamicOr(Color.parseColor("#7BA7BC"), Color.parseColor("#355C7D"));
    }

    public static int getAccent2() {
        if (current == AppTheme.MODERN) return Color.parseColor("#38BDF8");
        if (current == AppTheme.MONET) return Color.parseColor("#8FAF7E");
        return dynamicOr(Color.parseColor("#8FAF7E"), Color.parseColor("#577F4D"));
    }

    public static int getTextPrimary() {
        if (current == AppTheme.MODERN) return Color.parseColor("#F8FAFC");
        if (current == AppTheme.MONET) return Color.parseColor("#E4EAF0");
        return dynamicOr(Color.parseColor("#E4EAF0"), Color.parseColor("#152033"));
    }

    public static int getTextSecondary() {
        if (current == AppTheme.MODERN) return Color.parseColor("#A8B3CF");
        if (current == AppTheme.MONET) return Color.parseColor("#8FAABB");
        return dynamicOr(Color.parseColor("#8FAABB"), Color.parseColor("#5C6B7B"));
    }

    public static int getStroke() {
        if (current == AppTheme.MODERN) return Color.parseColor("#23314D");
        if (current == AppTheme.MONET) return Color.parseColor("#1E3248");
        return dynamicOr(Color.parseColor("#1E3248"), Color.parseColor("#C9D3E0"));
    }

    public static int getChipBg() {
        if (current == AppTheme.MODERN) return Color.parseColor("#182338");
        if (current == AppTheme.MONET) return Color.parseColor("#152236");
        return dynamicOr(Color.parseColor("#152236"), Color.parseColor("#E8EEF7"));
    }

    public static int getChipStroke() {
        if (current == AppTheme.MODERN) return Color.parseColor("#334155");
        if (current == AppTheme.MONET) return Color.parseColor("#2B4060");
        return dynamicOr(Color.parseColor("#2B4060"), Color.parseColor("#C5D0DF"));
    }

    public static int getEditTextBg() {
        if (current == AppTheme.MODERN) return Color.parseColor("#111827");
        if (current == AppTheme.MONET) return Color.parseColor("#0F1A2A");
        return dynamicOr(Color.parseColor("#0F1A2A"), Color.parseColor("#F7FAFD"));
    }

    public static int getEditTextStroke() {
        if (current == AppTheme.MODERN) return Color.parseColor("#24324D");
        if (current == AppTheme.MONET) return Color.parseColor("#1E3248");
        return dynamicOr(Color.parseColor("#1E3248"), Color.parseColor("#CDD7E4"));
    }

    public static int getSpinnerBg() { return getEditTextBg(); }

    public static int getToolbarBg() {
        if (current == AppTheme.MODERN) return Color.parseColor("#D90F172A");
        if (current == AppTheme.MONET) return Color.parseColor("#CC111E30");
        return dynamicOr(Color.parseColor("#CC111E30"), Color.parseColor("#E9EEF6"));
    }

    public static int getPopupBg() { return getDialogBg(); }
    public static int getDanger() { return Color.parseColor("#F43F5E"); }
    public static int getLinkColor() { return getAccent2(); }
    public static int getSectionTitleColor() { return getAccent(); }
}
