package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.Color;
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
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.DialogFragment;

/**
 * Fully custom "更多" menu dialog — dark-themed, no system popup.
 */
public class MoreMenuDialog extends DialogFragment {

    public interface OnMenuItemSelectedListener {
        void onMenuItemSelected(int id);
    }

    private static OnMenuItemSelectedListener listener;

    public static MoreMenuDialog newInstance(OnMenuItemSelectedListener l) {
        listener = l;
        return new MoreMenuDialog();
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                requireContext().getResources().getDisplayMetrics());
    }

    // Menu data: {id, emoji+label, isSection}
    private static final Object[][] ITEMS = {
        // id=0 → section header, id>0 → real item
        {0, "🎨  主题", true},
        {R.id.action_change_theme, "切换主题", false},

        {0, "📊  工作台", true},
        {R.id.action_dashboard, "工作台", false},
        {R.id.action_projects_hub, "项目中心", false},
        {R.id.action_inbox_clarify, "Inbox 澄清", false},
        {R.id.action_weekly_review, "Weekly Review", false},
        {R.id.action_memory_review, "间隔复习队列", false},
        {R.id.action_focus_session, "深度工作 Session", false},
        {R.id.action_graph_insights, "图谱结构智能", false},

        {0, "🧠  科学模板", true},
        {R.id.action_generate_woop, "生成 WOOP", false},
        {R.id.action_generate_if_then, "生成 If-Then", false},
        {R.id.action_generate_daily_review, "生成每日复盘", false},
        {R.id.action_generate_weekly_review, "生成每周复盘", false},
        {R.id.action_generate_aar, "生成 AAR 复盘", false},
        {R.id.action_generate_decision_tree, "生成科学决策", false},
        {R.id.action_generate_premortem, "生成 Premortem", false},
        {R.id.action_generate_evidence_review, "生成证据评估", false},
        {R.id.action_generate_retrieval_practice, "生成检索练习", false},
        {R.id.action_generate_concept_deepening, "生成概念深化", false},
        {R.id.action_generate_transfer_practice, "生成应用迁移", false},
        {R.id.action_generate_wrap, "生成 WRAP 决策护栏", false},
        {R.id.action_generate_bayes, "生成贝叶斯更新", false},
        {R.id.action_generate_dsrp, "生成 DSRP 结构分析", false},
        {R.id.action_generate_reference_forecast, "生成参考类预测", false},

        {0, "🤖  AI 功能", true},
        {R.id.action_scientific_enhance, "智能补强当前节点", false},
        {R.id.action_scientific_autopilot, "全量科学推进", false},
        {R.id.action_ai_gap_check, "AI 检查缺口", false},
        {R.id.action_ai_execution_patch, "AI 补全执行链", false},
        {R.id.action_ai_learning_patch, "AI 补全学习链", false},
        {R.id.action_ai_assistant, "AI 助手", false},

        {0, "⚙️  系统", true},
        {R.id.action_decision_matrix, "MCDA 决策评分", false},
        {R.id.action_execution_log, "执行回填", false},
        {R.id.action_decision_follow, "决策落地", false},
        {R.id.action_import_knowledge, "知识导入", false},
        {R.id.action_help, "帮助", false},
        {R.id.action_clear_all, "⚠️  清除全部", false},
    };

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int bg = ThemeManager.getDialogBg();
        int textPrimary = ThemeManager.getTextPrimary();
        int textSecondary = ThemeManager.getTextSecondary();
        int accent = ThemeManager.getAccent();
        int stroke = ThemeManager.getStroke();

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setBackgroundColor(bg);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        root.setPadding(0, dp(8), 0, dp(24));
        scrollView.addView(root);

        for (Object[] row : ITEMS) {
            int id = (int) row[0];
            String label = (String) row[1];
            boolean isSection = (boolean) row[2];

            if (isSection) {
                // Section header
                TextView tv = new TextView(requireContext());
                tv.setText(label);
                tv.setTextColor(accent);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
                tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
                tv.setLetterSpacing(0.12f);
                tv.setAllCaps(true);
                tv.setPadding(dp(24), dp(18), dp(24), dp(6));
                root.addView(tv);

                // Divider
                View div = new View(requireContext());
                div.setBackgroundColor(stroke);
                LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
                dlp.setMarginStart(dp(24));
                dlp.setMarginEnd(dp(24));
                div.setLayoutParams(dlp);
                root.addView(div);
            } else {
                // Menu item
                boolean isDanger = label.contains("清除");
                TextView tv = new TextView(requireContext());
                tv.setText(label);
                tv.setTextColor(isDanger ? ThemeManager.getDanger() : textPrimary);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
                tv.setPadding(dp(32), dp(14), dp(24), dp(14));
                tv.setGravity(Gravity.CENTER_VERTICAL);
                tv.setBackground(getRippleDrawable(bg));

                final int itemId = id;
                tv.setOnClickListener(v -> {
                    dismiss();
                    if (listener != null) listener.onMenuItemSelected(itemId);
                });
                root.addView(tv);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(
                new ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Theme_AppCompat_Dialog))
                .setTitle("更多功能")
                .setView(scrollView)
                .create();

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                android.graphics.drawable.GradientDrawable winBg = new android.graphics.drawable.GradientDrawable();
                winBg.setColor(ThemeManager.getDialogBg());
                winBg.setCornerRadius(56f);
                winBg.setStroke(2, ThemeManager.getStroke());
                dialog.getWindow().setBackgroundDrawable(winBg);
                TextView titleView = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
                if (titleView != null) {
                    titleView.setTextColor(textPrimary);
                    titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
                    titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);
                }
            }
        });

        return dialog;
    }

    private android.graphics.drawable.Drawable getRippleDrawable(int baseColor) {
        // Simple colored state list for press feedback
        android.graphics.drawable.GradientDrawable pressed = new android.graphics.drawable.GradientDrawable();
        pressed.setColor(ThemeManager.getChipBg());

        android.graphics.drawable.GradientDrawable normal = new android.graphics.drawable.GradientDrawable();
        normal.setColor(baseColor);

        android.graphics.drawable.StateListDrawable sl = new android.graphics.drawable.StateListDrawable();
        sl.addState(new int[]{android.R.attr.state_pressed}, pressed);
        sl.addState(new int[]{}, normal);
        return sl;
    }
}
