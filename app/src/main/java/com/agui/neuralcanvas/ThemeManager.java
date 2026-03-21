package com.agui.neuralcanvas;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;

public class ThemeManager {

    public enum AppTheme {
        MONET_DYNAMIC("莫奈动态取色"),
        PURE_BLACK("黑色主题"),
        PURE_WHITE("白色主题");

        public final String label;
        AppTheme(String label) { this.label = label; }
    }

    private static final String PREF_FILE = "nc_prefs";
    private static final String KEY_THEME = "app_theme";
    private static AppTheme current = AppTheme.MONET_DYNAMIC;
    private static Context appContext;

    public static void init(Context ctx) {
        appContext = ctx.getApplicationContext();
        String saved = appContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                .getString(KEY_THEME, AppTheme.MONET_DYNAMIC.name());
        try { current = AppTheme.valueOf(saved); } catch (Exception e) { current = AppTheme.MONET_DYNAMIC; }
    }

    public static AppTheme getCurrentTheme() { return current; }

    public static void setTheme(Context ctx, AppTheme t) {
        appContext = ctx.getApplicationContext();
        current = t;
        appContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                .edit().putString(KEY_THEME, t.name()).apply();
    }

    public static boolean isPureLightTheme() { return current == AppTheme.PURE_WHITE; }
    public static boolean isPureDarkTheme() { return current == AppTheme.PURE_BLACK; }

    private static int getColorByName(String name, int fallback) {
        if (appContext == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return fallback;
        try {
            Resources res = appContext.getResources();
            int id = res.getIdentifier(name, "color", "android");
            if (id == 0) return fallback;
            return res.getColor(id, appContext.getTheme());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static int withAlpha(int color, int alpha) {
        return Color.argb(Math.max(0, Math.min(255, alpha)), Color.red(color), Color.green(color), Color.blue(color));
    }

    public static int blend(int from, int to, float ratio) {
        float r = Math.max(0f, Math.min(1f, ratio));
        int a = (int) (Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * r);
        int red = (int) (Color.red(from) + (Color.red(to) - Color.red(from)) * r);
        int green = (int) (Color.green(from) + (Color.green(to) - Color.green(from)) * r);
        int blue = (int) (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * r);
        return Color.argb(a, red, green, blue);
    }

    public static int getBg() {
        if (current == AppTheme.PURE_BLACK) return Color.parseColor("#000000");
        if (current == AppTheme.PURE_WHITE) return Color.parseColor("#FFFFFF");
        return getColorByName("system_neutral1_900", Color.parseColor("#0D1118"));
    }

    public static int getDialogBg() {
        if (current == AppTheme.PURE_BLACK) return Color.parseColor("#0A0A0A");
        if (current == AppTheme.PURE_WHITE) return Color.parseColor("#FFFFFF");
        return blend(getBg(), getColorByName("system_neutral1_800", Color.parseColor("#151C28")), 0.70f);
    }

    public static int getSurface() {
        if (current == AppTheme.PURE_BLACK) return Color.parseColor("#101010");
        if (current == AppTheme.PURE_WHITE) return Color.parseColor("#F8F9FB");
        return blend(getDialogBg(), getColorByName("system_neutral2_800", Color.parseColor("#1C2430")), 0.72f);
    }

    public static int getSurfaceHigh() {
        if (current == AppTheme.PURE_BLACK) return Color.parseColor("#141414");
        if (current == AppTheme.PURE_WHITE) return Color.parseColor("#FFFFFF");
        return blend(getSurface(), Color.WHITE, 0.08f);
    }

    public static int getAccent() {
        if (current == AppTheme.PURE_BLACK) return Color.parseColor("#7BB1FF");
        if (current == AppTheme.PURE_WHITE) return Color.parseColor("#295EA8");
        return getColorByName("system_accent1_400", Color.parseColor("#87A7F9"));
    }

    public static int getAccent2() {
        if (current == AppTheme.PURE_BLACK) return Color.parseColor("#82D8FF");
        if (current == AppTheme.PURE_WHITE) return Color.parseColor("#3B82F6");
        return getColorByName("system_accent2_400", Color.parseColor("#82C7C1"));
    }

    public static int getAccentSoft() {
        if (current == AppTheme.PURE_BLACK) return Color.parseColor("#1B2434");
        if (current == AppTheme.PURE_WHITE) return Color.parseColor("#EAF1FF");
        return withAlpha(getAccent(), 34);
    }

    public static int getAccentStroke() {
        if (current == AppTheme.PURE_BLACK) return Color.parseColor("#355589");
        if (current == AppTheme.PURE_WHITE) return Color.parseColor("#A9C1E8");
        return withAlpha(getAccent(), 92);
    }

    public static int getTextPrimary() {
        if (current == AppTheme.PURE_BLACK) return Color.parseColor("#F5F5F5");
        if (current == AppTheme.PURE_WHITE) return Color.parseColor("#111827");
        return getColorByName("system_neutral1_50", Color.parseColor("#EAF0F9"));
    }

    public static int getTextSecondary() {
        if (current == AppTheme.PURE_BLACK) return Color.parseColor("#B8BDC7");
        if (current == AppTheme.PURE_WHITE) return Color.parseColor("#5B6472");
        return getColorByName("system_neutral2_200", Color.parseColor("#AAB7C7"));
    }

    public static int getStroke() {
        if (current == AppTheme.PURE_BLACK) return Color.parseColor("#23252A");
        if (current == AppTheme.PURE_WHITE) return Color.parseColor("#D7DCE4");
        return withAlpha(blend(getAccent(), getSurface(), 0.60f), 146);
    }

    public static int getChipBg() {
        if (current == AppTheme.PURE_BLACK) return Color.parseColor("#141414");
        if (current == AppTheme.PURE_WHITE) return Color.parseColor("#EEF2F7");
        return withAlpha(getAccent(), 28);
    }

    public static int getChipStroke() {
        if (current == AppTheme.PURE_BLACK) return Color.parseColor("#2A2A2A");
        if (current == AppTheme.PURE_WHITE) return Color.parseColor("#CDD6E1");
        return withAlpha(getAccent(), 92);
    }

    public static int getEditTextBg() {
        if (current == AppTheme.PURE_BLACK) return Color.parseColor("#121212");
        if (current == AppTheme.PURE_WHITE) return Color.parseColor("#FAFBFD");
        return blend(getSurface(), getBg(), 0.35f);
    }

    public static int getEditTextStroke() {
        if (current == AppTheme.PURE_BLACK) return Color.parseColor("#282828");
        if (current == AppTheme.PURE_WHITE) return Color.parseColor("#D6DCE6");
        return withAlpha(getAccent(), 78);
    }

    public static int getSpinnerBg() { return getEditTextBg(); }

    public static int getToolbarBg() {
        if (current == AppTheme.PURE_BLACK) return Color.parseColor("#DD0A0A0A");
        if (current == AppTheme.PURE_WHITE) return Color.parseColor("#ECFFFFFF");
        return withAlpha(getSurfaceHigh(), 228);
    }

    public static int getPopupBg() { return getDialogBg(); }
    public static int getDanger() { return current == AppTheme.PURE_WHITE ? Color.parseColor("#D62839") : Color.parseColor("#FF6A82"); }
    public static int getDangerSoft() { return current == AppTheme.PURE_WHITE ? Color.parseColor("#FCEDEE") : Color.parseColor("#31171D"); }
    public static int getSuccess() { return current == AppTheme.PURE_WHITE ? Color.parseColor("#1E8E5A") : Color.parseColor("#5BE0A8"); }
    public static int getSuccessSoft() { return current == AppTheme.PURE_WHITE ? Color.parseColor("#E9F8F0") : Color.parseColor("#173025"); }
    public static int getLinkColor() { return getAccent2(); }
    public static int getSectionTitleColor() { return getAccent(); }

    public static int getInfoSoft() {
        if (current == AppTheme.PURE_WHITE) return Color.parseColor("#EEF4FF");
        return withAlpha(getAccent2(), 26);
    }

    public static int getGridColor() {
        if (current == AppTheme.PURE_BLACK) return Color.parseColor("#141414");
        if (current == AppTheme.PURE_WHITE) return Color.parseColor("#E7ECF2");
        return withAlpha(getAccent(), 42);
    }
}
