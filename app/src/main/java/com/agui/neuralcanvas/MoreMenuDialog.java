package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

public class MoreMenuDialog extends DialogFragment {

    public static final int ID_WORKSPACE_DASHBOARD = 2001;
    public static final int ID_WORKSPACE_PROJECTS = 2002;
    public static final int ID_WORKSPACE_INBOX = 2003;
    public static final int ID_WORKSPACE_WEEKLY = 2004;
    public static final int ID_WORKSPACE_MEMORY = 2005;
    public static final int ID_WORKSPACE_FOCUS = 2006;
    public static final int ID_WORKSPACE_GRAPH = 2007;

    public static final int ID_TEMPLATE_WOOP = 2101;
    public static final int ID_TEMPLATE_IF_THEN = 2102;
    public static final int ID_TEMPLATE_WEEKLY_REVIEW = 2103;
    public static final int ID_TEMPLATE_PREMORTEM = 2104;
    public static final int ID_TEMPLATE_WRAP = 2105;
    public static final int ID_TEMPLATE_BAYES = 2106;
    public static final int ID_TEMPLATE_DSRP = 2107;
    public static final int ID_TEMPLATE_REFERENCE = 2108;
    public static final int ID_TEMPLATE_RETRIEVAL = 2109;
    public static final int ID_TEMPLATE_TRANSFER = 2110;

    public static final int ID_AI_ENHANCE = 2201;
    public static final int ID_AI_AUTOPILOT = 2202;
    public static final int ID_AI_GAP = 2203;
    public static final int ID_AI_EXECUTION = 2204;
    public static final int ID_AI_LEARNING = 2205;
    public static final int ID_AI_ASSISTANT = 2206;

    public static final int ID_IMPORT_KNOWLEDGE = 2301;
    public static final int ID_BOX_SELECT = 2302;
    public static final int ID_DELETE_SELECTED = 2303;
    public static final int ID_CANCEL_BOX_SELECT = 2304;

    public static final int ID_SYSTEM_THEME = 2401;
    public static final int ID_SYSTEM_HELP = 2402;
    public static final int ID_SYSTEM_CLEAR = 2403;

    private static final int NAV_ROOT = -1;
    private static final int NAV_WORKSPACE = -2;
    private static final int NAV_TEMPLATES = -3;
    private static final int NAV_AI = -4;
    private static final int NAV_IMPORT = -5;
    private static final int NAV_SYSTEM = -6;

    public interface OnMenuItemSelectedListener {
        void onMenuItemSelected(int id);
    }

    private static OnMenuItemSelectedListener listener;
    private String currentLevel = "root";

    public static MoreMenuDialog newInstance(OnMenuItemSelectedListener l) {
        listener = l;
        return new MoreMenuDialog();
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                requireContext().getResources().getDisplayMetrics());
    }

    private static class MenuItem {
        final String title;
        final String subtitle;
        final int id;
        final boolean nav;

        MenuItem(String title, String subtitle, int id, boolean nav) {
            this.title = title;
            this.subtitle = subtitle;
            this.id = id;
            this.nav = nav;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(ThemeManager.getDialogBg());

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(ThemeManager.getDialogBg());
        scrollView.addView(root);

        TextView title = new TextView(requireContext());
        title.setText(getTitleText());
        title.setTextColor(ThemeManager.getTextPrimary());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(requireContext());
        subtitle.setText(getSubtitleText());
        subtitle.setTextColor(ThemeManager.getTextSecondary());
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        LinearLayout.LayoutParams subtitleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleLp.topMargin = dp(6);
        root.addView(subtitle, subtitleLp);

        for (MenuItem item : buildItems()) {
            root.addView(buildCard(item));
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(scrollView).create();
        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                bg.setColor(ThemeManager.getDialogBg());
                bg.setCornerRadius(dp(26));
                bg.setStroke(dp(1), ThemeManager.getStroke());
                dialog.getWindow().setBackgroundDrawable(bg);
            }
        });
        return dialog;
    }

    private String getTitleText() {
        switch (currentLevel) {
            case "workspace": return "工作区";
            case "templates": return "模板库";
            case "ai": return "AI 工具";
            case "import": return "导入与批量";
            case "system": return "系统";
            default: return "更多";
        }
    }

    private String getSubtitleText() {
        switch (currentLevel) {
            case "workspace": return "高频工作流集中到一层，减少主界面按钮污染";
            case "templates": return "模板统一收纳，不把菜单拉成超长列表";
            case "ai": return "把 AI 相关动作压成一个分类";
            case "import": return "知识导入、矩形框选、多选删除都放这里";
            case "system": return "低频系统项单独放底层";
            default: return "三级结构：分类 → 子功能 → 执行，入口更干净";
        }
    }

    private List<MenuItem> buildItems() {
        List<MenuItem> items = new ArrayList<>();
        if (!"root".equals(currentLevel)) {
            items.add(new MenuItem("返回上一级", "回到主分类", NAV_ROOT, true));
        }
        switch (currentLevel) {
            case "workspace":
                items.add(new MenuItem("科学工作台", "查看总览", ID_WORKSPACE_DASHBOARD, false));
                items.add(new MenuItem("项目中心", "项目聚合", ID_WORKSPACE_PROJECTS, false));
                items.add(new MenuItem("Inbox 澄清", "快速分类", ID_WORKSPACE_INBOX, false));
                items.add(new MenuItem("Weekly Review", "周回顾", ID_WORKSPACE_WEEKLY, false));
                items.add(new MenuItem("记忆复习", "间隔复习", ID_WORKSPACE_MEMORY, false));
                items.add(new MenuItem("专注 Session", "专注计时", ID_WORKSPACE_FOCUS, false));
                items.add(new MenuItem("图谱智能", "结构分析", ID_WORKSPACE_GRAPH, false));
                break;
            case "templates":
                items.add(new MenuItem("WOOP", "目标计划", ID_TEMPLATE_WOOP, false));
                items.add(new MenuItem("If-Then", "条件触发", ID_TEMPLATE_IF_THEN, false));
                items.add(new MenuItem("Weekly Review", "周复盘", ID_TEMPLATE_WEEKLY_REVIEW, false));
                items.add(new MenuItem("Premortem", "预想失败", ID_TEMPLATE_PREMORTEM, false));
                items.add(new MenuItem("WRAP", "决策护栏", ID_TEMPLATE_WRAP, false));
                items.add(new MenuItem("Bayes", "证据更新", ID_TEMPLATE_BAYES, false));
                items.add(new MenuItem("DSRP", "结构分析", ID_TEMPLATE_DSRP, false));
                items.add(new MenuItem("参考类预测", "校正预估", ID_TEMPLATE_REFERENCE, false));
                items.add(new MenuItem("检索练习", "学习强化", ID_TEMPLATE_RETRIEVAL, false));
                items.add(new MenuItem("迁移练习", "应用迁移", ID_TEMPLATE_TRANSFER, false));
                break;
            case "ai":
                items.add(new MenuItem("智能补强", "补节点结构", ID_AI_ENHANCE, false));
                items.add(new MenuItem("全量推进", "整套推进", ID_AI_AUTOPILOT, false));
                items.add(new MenuItem("AI 缺口检查", "查漏补缺", ID_AI_GAP, false));
                items.add(new MenuItem("AI 执行补全", "补执行链", ID_AI_EXECUTION, false));
                items.add(new MenuItem("AI 学习补全", "补知识链", ID_AI_LEARNING, false));
                items.add(new MenuItem("AI 助手", "自由输入", ID_AI_ASSISTANT, false));
                break;
            case "import":
                items.add(new MenuItem("知识导入", "文本 / 文档 / OCR / 语音统一入口", ID_IMPORT_KNOWLEDGE, false));
                items.add(new MenuItem("矩形框选", "进入框选模式", ID_BOX_SELECT, false));
                items.add(new MenuItem("删除已选节点", "批量删除当前选中节点", ID_DELETE_SELECTED, false));
                items.add(new MenuItem("退出框选", "恢复普通操作", ID_CANCEL_BOX_SELECT, false));
                break;
            case "system":
                items.add(new MenuItem("切换主题", "颜色和氛围", ID_SYSTEM_THEME, false));
                items.add(new MenuItem("帮助", "全部功能说明", ID_SYSTEM_HELP, false));
                items.add(new MenuItem("清除全部", "危险操作", ID_SYSTEM_CLEAR, false));
                break;
            default:
                items.add(new MenuItem("工作区", "工作流与面板入口", NAV_WORKSPACE, true));
                items.add(new MenuItem("模板库", "所有科学模板", NAV_TEMPLATES, true));
                items.add(new MenuItem("AI 工具", "补强、自动推进、AI助手", NAV_AI, true));
                items.add(new MenuItem("导入与批量", "导入、框选、多删", NAV_IMPORT, true));
                items.add(new MenuItem("系统", "主题、帮助、清空", NAV_SYSTEM, true));
                break;
        }
        return items;
    }

    private View buildCard(MenuItem item) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        card.setLayoutParams(lp);

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(ThemeManager.getChipBg());
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), ThemeManager.getChipStroke());
        card.setBackground(bg);

        TextView title = new TextView(requireContext());
        title.setText(item.title + (item.nav ? " ›" : ""));
        title.setTextColor(ThemeManager.getTextPrimary());
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        card.addView(title);

        TextView subtitle = new TextView(requireContext());
        subtitle.setText(item.subtitle);
        subtitle.setTextColor(ThemeManager.getTextSecondary());
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(6);
        card.addView(subtitle, subLp);

        TextView tail = new TextView(requireContext());
        tail.setText(item.nav ? "进入" : "执行");
        tail.setTextColor(ThemeManager.getAccent());
        tail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        tail.setGravity(Gravity.END);
        LinearLayout.LayoutParams tailLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tailLp.topMargin = dp(10);
        card.addView(tail, tailLp);

        card.setOnClickListener(v -> handleItem(item));
        return card;
    }

    private void handleItem(MenuItem item) {
        if (item == null) return;
        if (item.nav) {
            switch (item.id) {
                case NAV_ROOT: currentLevel = "root"; break;
                case NAV_WORKSPACE: currentLevel = "workspace"; break;
                case NAV_TEMPLATES: currentLevel = "templates"; break;
                case NAV_AI: currentLevel = "ai"; break;
                case NAV_IMPORT: currentLevel = "import"; break;
                case NAV_SYSTEM: currentLevel = "system"; break;
                default: currentLevel = "root"; break;
            }
            dismiss();
            MoreMenuDialog dialog = MoreMenuDialog.newInstance(listener);
            dialog.currentLevel = currentLevel;
            dialog.show(requireActivity().getSupportFragmentManager(), "more_menu");
            return;
        }
        dismiss();
        if (listener != null) listener.onMenuItemSelected(item.id);
    }
}
