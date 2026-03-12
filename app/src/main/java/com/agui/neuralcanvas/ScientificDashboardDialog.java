package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

public class ScientificDashboardDialog extends DialogFragment {

    public static ScientificDashboardDialog newInstance() {
        return new ScientificDashboardDialog();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                requireContext().getResources().getDisplayMetrics()
        );
    }

    private android.graphics.drawable.GradientDrawable createRoundedDrawable(String color) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Color.parseColor(color));
        gd.setCornerRadius(dp(16));
        gd.setStroke(dp(1), Color.parseColor("#24324A"));
        return gd;
    }

    private TextView buildTitle(String text, int sizeSp, boolean bold, String color) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(Color.parseColor(color));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        if (bold) {
            tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        }
        return tv;
    }

    private View buildSpacer(int heightDp) {
        View view = new View(requireContext());
        view.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(heightDp)
        ));
        return view;
    }

    private LinearLayout buildCard(String title, String subtitle, String bgColor) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        lp.leftMargin = dp(4);
        lp.rightMargin = dp(4);
        card.setLayoutParams(lp);
        card.setBackground(createRoundedDrawable(bgColor));

        TextView t1 = buildTitle(title, 15, true, "#F8FAFC");
        TextView t2 = buildTitle(subtitle, 12, false, "#D6E3F5");

        card.addView(t1);
        card.addView(buildSpacer(6));
        card.addView(t2);

        return card;
    }

    private void refreshSelf() {
        dismiss();
        new ScientificDashboardDialog().show(requireActivity().getSupportFragmentManager(), "scientific_dashboard");
    }

    private TextView buildNodeRow(final Node node, final MainActivity activity) {
        TextView tv = new TextView(requireContext());
        String title = DashboardSectionBuilder.safe(node.getTitle());
        if (title.isEmpty()) title = "未命名节点";
        String type = node.getType() == null ? "" : node.getType().label;

        String line = "• " + title + "  [" + type + "]";
        String extra = DashboardSectionBuilder.buildNodeExtra(node);
        if (!TextUtils.isEmpty(extra)) {
            line += "\n  " + extra;
        }

        tv.setText(line);
        tv.setTextColor(Color.parseColor("#E2E8F0"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        tv.setBackground(createRoundedDrawable("#111827"));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(8);
        tv.setLayoutParams(lp);

        tv.setOnClickListener(v -> {
            activity.getMindMapView().focusNodeById(node.getId());
            activity.getMindMapView().selectNodeById(node.getId());
            dismiss();
        });

        tv.setOnLongClickListener(v -> {
            FragmentActivity fa = getActivity();
            if (fa == null) return true;

            Runnable refresh = this::refreshSelf;

            if (DashboardSectionBuilder.isInboxNode(node)) {
                DashboardActionDialogHelper.showInboxClarifyDialog(fa, node, refresh);
                return true;
            }

            if (DashboardSectionBuilder.isDecisionNode(node)) {
                DashboardActionDialogHelper.askGenerateDecisionStarter(fa, node, refresh);
                return true;
            }

            if (DashboardSectionBuilder.isLearningNode(node)) {
                DashboardActionDialogHelper.askGenerateLearningStarter(fa, node, refresh);
                return true;
            }

            if (DashboardSectionBuilder.isKrNode(node)) {
                DashboardActionDialogHelper.showKrQuickUpdateDialog(fa, node, refresh);
                return true;
            }

            if (DashboardSectionBuilder.isExecutionNode(node) || DashboardSectionBuilder.isReviewNode(node)) {
                DashboardActionDialogHelper.showMarkDoneDialog(fa, node, refresh);
                return true;
            }

            return true;
        });

        return tv;
    }

    private void addSection(LinearLayout root, String title, java.util.List<Node> nodes, MainActivity activity) {
        TextView sectionTitle = buildTitle(title, 16, true, "#F8FAFC");
        root.addView(sectionTitle);

        if (nodes.isEmpty()) {
            TextView empty = buildTitle("当前没有内容", 13, false, "#94A3B8");
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.topMargin = dp(8);
            empty.setLayoutParams(lp);
            root.addView(empty);
            root.addView(buildSpacer(18));
            return;
        }

        int limit = Math.min(nodes.size(), 8);
        for (int i = 0; i < limit; i++) {
            root.addView(buildNodeRow(nodes.get(i), activity));
        }

        if (nodes.size() > limit) {
            TextView more = buildTitle("还有 " + (nodes.size() - limit) + " 个", 12, false, "#94A3B8");
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.topMargin = dp(8);
            more.setLayoutParams(lp);
            root.addView(more);
        }

        root.addView(buildSpacer(18));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null || activity.getMindMapView() == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        DashboardSectionBuilder.DashboardData data =
                DashboardSectionBuilder.build(activity.getMindMapView().getNodesInternal());

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.parseColor("#0B1020"));
        scrollView.addView(root);

        TextView title = buildTitle("科学工作台", 20, true, "#F8FAFC");
        root.addView(title);

        TextView subtitle = buildTitle("已拆分为 section builder / action engine / dialog helper", 13, false, "#94A3B8");
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subLp.topMargin = dp(6);
        subtitle.setLayoutParams(subLp);
        root.addView(subtitle);

        root.addView(buildSpacer(16));

        HorizontalScrollView hsv = new HorizontalScrollView(requireContext());
        hsv.setHorizontalScrollBarEnabled(false);

        LinearLayout cardRow = new LinearLayout(requireContext());
        cardRow.setOrientation(LinearLayout.HORIZONTAL);
        cardRow.addView(buildCard("Inbox", String.valueOf(data.inboxNodes.size()), "#1E293B"));
        cardRow.addView(buildCard("今日执行", String.valueOf(data.todayNodes.size()), "#0F766E"));
        cardRow.addView(buildCard("待复盘", String.valueOf(data.reviewNodes.size()), "#92400E"));
        cardRow.addView(buildCard("风险/受阻", String.valueOf(data.riskNodes.size()), "#7F1D1D"));
        cardRow.addView(buildCard("KR", String.valueOf(data.krNodes.size()), "#1D4ED8"));
        hsv.addView(cardRow);
        root.addView(hsv);

        root.addView(buildSpacer(18));

        addSection(root, "Inbox 待澄清", data.inboxNodes, activity);
        addSection(root, "今日最值得推进（已排序）", data.todayNodes, activity);
        addSection(root, "待复盘 / 待复习", data.reviewNodes, activity);
        addSection(root, "高风险 / 受阻节点", data.riskNodes, activity);
        addSection(root, "关键结果（KR）", data.krNodes, activity);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(scrollView)
                .setPositiveButton("关闭", null)
                .create();

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                dialog.getWindow().setGravity(Gravity.CENTER);
            }
        });

        return dialog;
    }
}
