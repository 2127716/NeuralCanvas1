package com.agui.neuralcanvas;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;

public class ThemeManager {

    public enum AppTheme {
        MONET("Monet 动态"),
        MODERN("深色紫调"),
        LIQUID("液态玻璃");

        public final String label;
        AppTheme(String label) { this.label = label; }
    }

    private static final String PREF_FILE = "nc_prefs";
    private static final String KEY_THEME = "app_theme";
    private static AppTheme current = AppTheme.MONET;
    private static Context appContext;

    public static void init(Context ctx) {
        appContext = ctx.getApplicationContext();
        String saved = appContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                .getString(KEY_THEME, AppTheme.MONET.name());
        try {
            current = AppTheme.valueOf(saved);
        } catch (Exception e) {
            current = AppTheme.MONET;
        }
    }

    public static AppTheme getCurrentTheme() {
        return current;
    }

    public static void setTheme(Context ctx, AppTheme t) {
        appContext = ctx.getApplicationContext();
        current = t == null ? AppTheme.MONET : t;
        appContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_THEME, current.name())
                .apply();
    }

    private static int dynamicColor(int resId, String fallbackHex) {
        if (appContext == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return Color.parseColor(fallbackHex);
        }
        try {
            return appContext.getColor(resId);
        } catch (Exception e) {
            return Color.parseColor(fallbackHex);
        }
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static int getBg() {
        switch (current) {
            case MONET:
                return dynamicColor(android.R.color.system_neutral1_1000, "#0B1220");
            case LIQUID:
                return Color.parseColor("#08090C");
            default:
                return Color.parseColor("#0A0F1E");
        }
    }

    public static int getDialogBg() {
        switch (current) {
            case MONET:
                return dynamicColor(android.R.color.system_neutral1_900, "#121A29");
            case LIQUID:
                return Color.parseColor("#14161B");
            default:
                return Color.parseColor("#121826");
        }
    }

    public static int getSurface() {
        switch (current) {
            case MONET:
                return dynamicColor(android.R.color.system_neutral2_900, "#172133");
            case LIQUID:
                return Color.parseColor("#17191F");
            default:
                return Color.parseColor("#182235");
        }
    }

    public static int getAccent() {
        switch (current) {
            case MONET:
                return dynamicColor(android.R.color.system_accent1_300, "#8AA4FF");
            case LIQUID:
                return Color.parseColor("#64D2FF");
            default:
                return Color.parseColor("#8B5CF6");
        }
    }

    public static int getAccent2() {
        switch (current) {
            case MONET:
                return dynamicColor(android.R.color.system_accent2_300, "#7AC7C4");
            case LIQUID:
                return Color.parseColor("#30D158");
            default:
                return Color.parseColor("#38BDF8");
        }
    }

    public static int getTextPrimary() {
        switch (current) {
            case MONET:
                return dynamicColor(android.R.color.system_neutral1_50, "#F4F7FB");
            case LIQUID:
                return Color.parseColor("#F5F5F7");
            default:
                return Color.parseColor("#F8FAFC");
        }
    }

    public static int getTextSecondary() {
        switch (current) {
            case MONET:
                return dynamicColor(android.R.color.system_neutral2_200, "#B3C0D1");
            case LIQUID:
                return Color.parseColor("#A1A1AA");
            default:
                return Color.parseColor("#A8B3CF");
        }
    }

    public static int getStroke() {
        switch (current) {
            case MONET:
                return withAlpha(dynamicColor(android.R.color.system_neutral2_300, "#38516D"), 110);
            case LIQUID:
                return Color.parseColor("#33FFFFFF");
            default:
                return Color.parseColor("#23314D");
        }
    }

    public static int getChipBg() {
        switch (current) {
            case MONET:
                return withAlpha(dynamicColor(android.R.color.system_accent1_700, "#20314F"), 120);
            case LIQUID:
                return Color.parseColor("#1AFFFFFF");
            default:
                return Color.parseColor("#1A2438");
        }
    }

    public static int getChipStroke() {
        switch (current) {
            case MONET:
                return withAlpha(dynamicColor(android.R.color.system_accent1_300, "#7598D8"), 120);
            case LIQUID:
                return Color.parseColor("#33FFFFFF");
            default:
                return Color.parseColor("#334155");
        }
    }

    public static int getEditTextBg() {
        switch (current) {
            case MONET:
                return withAlpha(dynamicColor(android.R.color.system_neutral2_800, "#131D2E"), 180);
            case LIQUID:
                return Color.parseColor("#12FFFFFF");
            default:
                return Color.parseColor("#111827");
        }
    }

    public static int getEditTextStroke() {
        switch (current) {
            case MONET:
                return withAlpha(dynamicColor(android.R.color.system_accent1_200, "#8CA7D9"), 100);
            case LIQUID:
                return Color.parseColor("#26FFFFFF");
            default:
                return Color.parseColor("#24324D");
        }
    }

    public static int getSpinnerBg() {
        switch (current) {
            case MONET:
                return withAlpha(dynamicColor(android.R.color.system_neutral2_800, "#142033"), 190);
            case LIQUID:
                return Color.parseColor("#16FFFFFF");
            default:
                return Color.parseColor("#182338");
        }
    }

    public static int getToolbarBg() {
        switch (current) {
            case MONET:
                return withAlpha(dynamicColor(android.R.color.system_neutral1_900, "#122033"), 212);
            case LIQUID:
                return Color.parseColor("#B314161B");
            default:
                return Color.parseColor("#D9121826");
        }
    }

    public static int getPopupBg() {
        return getDialogBg();
    }

    public static int getDanger() {
        switch (current) {
            case MONET:
                return dynamicColor(android.R.color.system_accent3_300, "#FF7A90");
            case LIQUID:
                return Color.parseColor("#FF5C7A");
            default:
                return Color.parseColor("#F43F5E");
        }
    }

    public static int getLinkColor() {
        switch (current) {
            case MONET:
                return dynamicColor(android.R.color.system_accent2_300, "#77C6B1");
            case LIQUID:
                return Color.parseColor("#30D158");
            default:
                return Color.parseColor("#34D399");
        }
    }

    public static int getSectionTitleColor() {
        return getAccent();
    }
}
