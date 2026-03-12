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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    private android.graphics.drawable.GradientDrawable createRoundedDrawable(String color) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Color.parseColor(color));
        gd.setCornerRadius(dp(16));
        gd.setStroke(dp(1), Color.parseColor("#24324A"));
        return gd;
    }

    private String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        return value.trim();
    }

    private boolean containsDateHint(String text, String today) {
        if (text == null) return false;
        String s = text.trim();
        return !s.isEmpty() && s.contains(today);
    }

    private boolean isExecutionNode(Node node) {
        return node != null && node.isExecutionNode();
    }

    private boolean isReviewNode(Node node) {
        return node != null &&
                (node.getType() == Node.NodeType.REVIEW ||
                        node.getStatus() == Node.NodeStatus.REVIEW ||
                        !safe(node.getReviewAt(), "").isEmpty());
    }

    private boolean isInboxNode(Node node) {
        return node != null && node.getType() == Node.NodeType.INBOX;
    }

    private boolean isRiskOrBlocked(Node node) {
        return node != null &&
                (node.getType() == Node.NodeType.RISK
                        || node.getType() == Node.NodeType.OBSTACLE
                        || node.getStatus() == Node.NodeStatus.BLOCKED);
    }

    private boolean isKrNode(Node node) {
        return node != null && node.getType() == Node.NodeType.KEY_RESULT;
    }

    private boolean isDecisionNode(Node node) {
        return node != null && node.getType() == Node.NodeType.DECISION;
    }

    private boolean isLearningNode(Node node) {
        if (node == null) return false;
        return node.getType() == Node.NodeType.CONCEPT
                || node.getType() == Node.NodeType.NOTE
                || node.getType() == Node.NodeType.QUESTION
                || node.getType() == Node.NodeType.SOURCE
                || node.getType() == Node.NodeType.INSIGHT
                || node.getType() == Node.NodeType.EVIDENCE;
    }

    private int statusRank(Node.NodeStatus status) {
        if (status == null) return 99;
        switch (status) {
            case ACTIVE: return 0;
            case PLANNED: return 1;
            case WAITING: return 2;
            case BLOCKED: return 3;
            case REVIEW: return 4;
            case SOMEDAY: return 5;
            case DONE: return 6;
            default: return 99;
        }
    }

    private String formatPercent(float current, float target) {
        if (target <= 0f) return "";
        float percent = (current / target) * 100f;
        if (percent < 0f) percent = 0f;
        return String.format(Locale.getDefault(), "%.0f%%", percent);
    }

    private String buildNodeExtra(Node node) {
        List<String> parts = new ArrayList<>();

        if (!safe(node.getDueAt(), "").isEmpty()) {
            parts.add("截止: " + node.getDueAt());
        }
        if (!safe(node.getReviewAt(), "").isEmpty()) {
            parts.add("复盘/复习: " + node.getReviewAt());
        }
        if (!safe(node.getTriggerCondition(), "").isEmpty()) {
            parts.add("触发: " + node.getTriggerCondition());
        }
        if (node.getPriority() > 0) {
            parts.add("优先级: " + node.getPriority());
        }
        if (node.getEffortEstimate() > 0f) {
            parts.add("预计耗时: " + node.getEffortEstimate() + "h");
        }
        if (node.getKrTarget() > 0f) {
            parts.add("KR: " + node.getKrCurrent() + " / " + node.getKrTarget() + " (" +
                    formatPercent(node.getKrCurrent(), node.getKrTarget()) + ")");
        }
        if (node.getType() == Node.NodeType.EVIDENCE) {
            parts.add("证据强度: " + node.getEvidenceStrength());
        }

        if (parts.isEmpty()) {
            String content = safe(node.getContent(), "");
            if (!content.isEmpty()) {
                if (content.length() > 40) content = content.substring(0, 40) + "…";
                parts.add(content);
            }
        }

        return TextUtils.join(" ｜ ", parts);
    }

    private void markDone(Node node, MainActivity activity) {
        if (node == null || activity == null) return;
        node.setStatus(Node.NodeStatus.DONE);
        activity.onNodeUpdated(node);
    }

    private void addKrValue(Node node, float delta, MainActivity activity) {
        if (node == null || activity == null) return;
        node.setKrCurrent(node.getKrCurrent() + delta);
        activity.onNodeUpdated(node);
    }

    private void generateProjectStarterNodes(Node projectNode, MainActivity activity) {
        if (projectNode == null || activity == null || activity.getMindMapView() == null) return;

        MindMapView view = activity.getMindMapView();

        float baseX = projectNode.getX();
        float baseY = projectNode.getY();
        String projectId = projectNode.getId();
        String title = safe(projectNode.getTitle(), "项目");

        Node goalNode = new Node("Goal｜" + title, "这个项目真正要实现的结果是什么？",
                baseX - 320f, baseY + 220f, Node.NodeType.GOAL);
        goalNode.setShape(Node.NodeShape.OVAL);
        goalNode.setStatus(Node.NodeStatus.ACTIVE);
        goalNode.setProjectId(projectId);
        goalNode.setTagsFromString("Project,Goal,目标");

        Node krNode = new Node("KR｜" + title, "写一个可量化关键结果",
                baseX, baseY + 220f, Node.NodeType.KEY_RESULT);
        krNode.setShape(Node.NodeShape.HEXAGON);
        krNode.setStatus(Node.NodeStatus.ACTIVE);
        krNode.setProjectId(projectId);
        krNode.setKrTarget(1f);
        krNode.setKrCurrent(0f);
        krNode.setTagsFromString("Project,KR,关键结果");

        Node actionNode = new Node("First Action｜" + title, "这个项目最小的下一步是什么？",
                baseX + 320f, baseY + 220f, Node.NodeType.ACTION);
        actionNode.setShape(Node.NodeShape.RECT);
        actionNode.setStatus(Node.NodeStatus.PLANNED);
        actionNode.setProjectId(projectId);
        actionNode.setTriggerCondition("如果我要开始推进这个项目，那么先做这个最小动作");
        actionNode.setTagsFromString("Project,FirstAction,执行");

        Node reviewNode = new Node("Weekly Review｜" + title, "本周这个项目推进了什么？卡在哪？下周怎么调？",
                baseX, baseY + 460f, Node.NodeType.REVIEW);
        reviewNode.setShape(Node.NodeShape.OVAL);
        reviewNode.setStatus(Node.NodeStatus.REVIEW);
        reviewNode.setProjectId(projectId);
        reviewNode.setTagsFromString("Project,WeeklyReview,复盘");

        view.addNode(goalNode);
        view.addNode(krNode);
        view.addNode(actionNode);
        view.addNode(reviewNode);

        view.addConnection(new Connection(projectNode.getId(), goalNode.getId(), Connection.ConnectionType.LEADS_TO, "项目目标"));
        view.addConnection(new Connection(projectNode.getId(), krNode.getId(), Connection.ConnectionType.LEADS_TO, "关键结果"));
        view.addConnection(new Connection(projectNode.getId(), actionNode.getId(), Connection.ConnectionType.LEADS_TO, "第一步"));
        view.addConnection(new Connection(projectNode.getId(), reviewNode.getId(), Connection.ConnectionType.LEADS_TO, "每周复盘"));
        view.addConnection(new Connection(goalNode.getId(), krNode.getId(), Connection.ConnectionType.SUPPORTS, "目标量化"));
        view.addConnection(new Connection(actionNode.getId(), goalNode.getId(), Connection.ConnectionType.SUPPORTS, "行动推进目标"));
        view.addConnection(new Connection(reviewNode.getId(), actionNode.getId(), Connection.ConnectionType.TRIGGERS, "复盘指导下一步"));

        activity.onGraphMutatedByAi();
    }

    private void generateDecisionStarterNodes(Node decisionNode, MainActivity activity) {
        if (decisionNode == null || activity == null || activity.getMindMapView() == null) return;

        MindMapView view = activity.getMindMapView();

        float baseX = decisionNode.getX();
        float baseY = decisionNode.getY();
        String ownerId = safe(decisionNode.getProjectId(), "");
        if (ownerId.isEmpty()) ownerId = decisionNode.getId();

        Node optionA = new Node("Option A", "方案A的核心做法、成本、收益是什么？",
                baseX - 420f, baseY + 60f, Node.NodeType.OPTION);
        optionA.setShape(Node.NodeShape.RECT);
        optionA.setStatus(Node.NodeStatus.PLANNED);
        optionA.setProjectId(ownerId);
        optionA.setTagsFromString("Decision,Option,A");

        Node optionB = new Node("Option B", "方案B的核心做法、成本、收益是什么？",
                baseX, baseY + 60f, Node.NodeType.OPTION);
        optionB.setShape(Node.NodeShape.RECT);
        optionB.setStatus(Node.NodeStatus.PLANNED);
        optionB.setProjectId(ownerId);
        optionB.setTagsFromString("Decision,Option,B");

        Node optionC = new Node("Option C", "方案C的核心做法、成本、收益是什么？",
                baseX + 420f, baseY + 60f, Node.NodeType.OPTION);
        optionC.setShape(Node.NodeShape.RECT);
        optionC.setStatus(Node.NodeStatus.PLANNED);
        optionC.setProjectId(ownerId);
        optionC.setTagsFromString("Decision,Option,C");

        Node criterionNode = new Node("Criterion", "写 3~6 个准则：时间、成本、长期收益、可逆性、风险等",
                baseX - 260f, baseY + 320f, Node.NodeType.CRITERION);
        criterionNode.setShape(Node.NodeShape.HEXAGON);
        criterionNode.setStatus(Node.NodeStatus.ACTIVE);
        criterionNode.setProjectId(ownerId);
        criterionNode.setTagsFromString("Decision,Criterion,准则");

        Node riskNode = new Node("Risk", "每个方案最可能失败在哪？最坏情况是什么？",
                baseX + 260f, baseY + 320f, Node.NodeType.RISK);
        riskNode.setShape(Node.NodeShape.DIAMOND);
        riskNode.setStatus(Node.NodeStatus.ACTIVE);
        riskNode.setProjectId(ownerId);
        riskNode.setTagsFromString("Decision,Risk,风险");

        Node evidenceNode = new Node("Evidence", "有哪些事实、数据、经验在支持或反驳当前判断？",
                baseX - 260f, baseY + 540f, Node.NodeType.EVIDENCE);
        evidenceNode.setShape(Node.NodeShape.RECT);
        evidenceNode.setStatus(Node.NodeStatus.ACTIVE);
        evidenceNode.setProjectId(ownerId);
        evidenceNode.setEvidenceStrength(0.5f);
        evidenceNode.setTagsFromString("Decision,Evidence,证据");

        Node nextActionNode = new Node("Next Action", "下一步是直接选，还是先做一个小验证？",
                baseX + 260f, baseY + 540f, Node.NodeType.ACTION);
        nextActionNode.setShape(Node.NodeShape.RECT);
        nextActionNode.setStatus(Node.NodeStatus.PLANNED);
        nextActionNode.setProjectId(ownerId);
        nextActionNode.setTriggerCondition("如果完成方案比较，那么先执行这个最小验证动作");
        nextActionNode.setTagsFromString("Decision,NextAction,执行");

        view.addNode(optionA);
        view.addNode(optionB);
        view.addNode(optionC);
        view.addNode(criterionNode);
        view.addNode(riskNode);
        view.addNode(evidenceNode);
        view.addNode(nextActionNode);

        view.addConnection(new Connection(decisionNode.getId(), optionA.getId(), Connection.ConnectionType.LEADS_TO, "候选"));
        view.addConnection(new Connection(decisionNode.getId(), optionB.getId(), Connection.ConnectionType.LEADS_TO, "候选"));
        view.addConnection(new Connection(decisionNode.getId(), optionC.getId(), Connection.ConnectionType.LEADS_TO, "候选"));

        view.addConnection(new Connection(criterionNode.getId(), optionA.getId(), Connection.ConnectionType.SUPPORTS, "按准则评估"));
        view.addConnection(new Connection(criterionNode.getId(), optionB.getId(), Connection.ConnectionType.SUPPORTS, "按准则评估"));
        view.addConnection(new Connection(criterionNode.getId(), optionC.getId(), Connection.ConnectionType.SUPPORTS, "按准则评估"));

        view.addConnection(new Connection(riskNode.getId(), optionA.getId(), Connection.ConnectionType.BLOCKS, "风险审查"));
        view.addConnection(new Connection(riskNode.getId(), optionB.getId(), Connection.ConnectionType.BLOCKS, "风险审查"));
        view.addConnection(new Connection(riskNode.getId(), optionC.getId(), Connection.ConnectionType.BLOCKS, "风险审查"));

        view.addConnection(new Connection(evidenceNode.getId(), decisionNode.getId(), Connection.ConnectionType.EVIDENCE_FOR, "证据"));
        view.addConnection(new Connection(optionA.getId(), nextActionNode.getId(), Connection.ConnectionType.LEADS_TO, "候选"));
        view.addConnection(new Connection(optionB.getId(), nextActionNode.getId(), Connection.ConnectionType.LEADS_TO, "候选"));
        view.addConnection(new Connection(optionC.getId(), nextActionNode.getId(), Connection.ConnectionType.LEADS_TO, "候选"));

        activity.onGraphMutatedByAi();
    }

    private void generateLearningStarterNodes(Node learningNode, MainActivity activity) {
        if (learningNode == null || activity == null || activity.getMindMapView() == null) return;

        MindMapView view = activity.getMindMapView();

        float baseX = learningNode.getX();
        float baseY = learningNode.getY();
        String ownerId = safe(learningNode.getProjectId(), "");
        if (ownerId.isEmpty()) ownerId = learningNode.getId();
        String title = safe(learningNode.getTitle(), "知识点");

        Node retrievalNode = new Node("Retrieval｜" + title,
                "不用看原文，试着回答：它是什么？核心机制/步骤是什么？",
                baseX - 360f, baseY + 180f, Node.NodeType.QUESTION);
        retrievalNode.setShape(Node.NodeShape.OVAL);
        retrievalNode.setStatus(Node.NodeStatus.ACTIVE);
        retrievalNode.setProjectId(ownerId);
        retrievalNode.setReviewAt("尽快第一次回忆");
        retrievalNode.setTagsFromString("Learning,Retrieval,检索练习");

        Node deepeningNode = new Node("Deepening｜" + title,
                "用你自己的话重述定义，举例，并找一个反例。",
                baseX, baseY + 180f, Node.NodeType.CONCEPT);
        deepeningNode.setShape(Node.NodeShape.HEXAGON);
        deepeningNode.setStatus(Node.NodeStatus.ACTIVE);
        deepeningNode.setProjectId(ownerId);
        deepeningNode.setTagsFromString("Learning,Deepening,概念深化");

        Node transferNode = new Node("Transfer｜" + title,
                "把它放到一个新场景中，设计一个小应用任务验证迁移能力。",
                baseX + 360f, baseY + 180f, Node.NodeType.EXPERIMENT);
        transferNode.setShape(Node.NodeShape.RECT);
        transferNode.setStatus(Node.NodeStatus.PLANNED);
        transferNode.setProjectId(ownerId);
        transferNode.setTriggerCondition("如果我要确认自己真的会了，那么先做这个迁移验证");
        transferNode.setTagsFromString("Learning,Transfer,迁移练习");

        view.addNode(retrievalNode);
        view.addNode(deepeningNode);
        view.addNode(transferNode);

        view.addConnection(new Connection(learningNode.getId(), retrievalNode.getId(), Connection.ConnectionType.LEADS_TO, "主动回忆"));
        view.addConnection(new Connection(learningNode.getId(), deepeningNode.getId(), Connection.ConnectionType.LEADS_TO, "概念深化"));
        view.addConnection(new Connection(learningNode.getId(), transferNode.getId(), Connection.ConnectionType.LEADS_TO, "迁移验证"));
        view.addConnection(new Connection(retrievalNode.getId(), deepeningNode.getId(), Connection.ConnectionType.TRIGGERS, "回忆暴露薄弱点"));
        view.addConnection(new Connection(deepeningNode.getId(), transferNode.getId(), Connection.ConnectionType.TRIGGERS, "理解后迁移"));

        activity.onGraphMutatedByAi();
    }

    private void askGenerateProjectStarter(final Node projectNode, final MainActivity activity) {
        if (projectNode == null || activity == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("项目创建向导")
                .setMessage("已转成 PROJECT。要不要顺手生成 Goal、KR、First Action、Weekly Review？")
                .setNegativeButton("先不用", null)
                .setPositiveButton("立即生成", (d, w) -> generateProjectStarterNodes(projectNode, activity))
                .show();
    }

    private void askGenerateDecisionStarter(final Node decisionNode, final MainActivity activity) {
        if (decisionNode == null || activity == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("决策创建向导")
                .setMessage("要不要立刻生成 Option A/B/C、Criterion、Risk、Evidence、Next Action？")
                .setNegativeButton("先不用", null)
                .setPositiveButton("立即生成", (d, w) -> generateDecisionStarterNodes(decisionNode, activity))
                .show();
    }

    private void askGenerateLearningStarter(final Node learningNode, final MainActivity activity) {
        if (learningNode == null || activity == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("学习创建向导")
                .setMessage("要不要立刻生成 Retrieval Practice、Concept Deepening、Transfer Practice？")
                .setNegativeButton("先不用", null)
                .setPositiveButton("立即生成", (d, w) -> generateLearningStarterNodes(learningNode, activity))
                .show();
    }

    private void convertInboxNode(Node node, Node.NodeType targetType, MainActivity activity) {
        if (node == null || activity == null) return;

        node.setType(targetType);

        switch (targetType) {
            case TASK:
            case ACTION:
                node.setStatus(Node.NodeStatus.PLANNED);
                if (safe(node.getTriggerCondition(), "").isEmpty()) {
                    node.setTriggerCondition("如果开始处理这个事项，那么先做一个最小动作");
                }
                break;

            case GOAL:
                node.setStatus(Node.NodeStatus.ACTIVE);
                break;

            case PROJECT:
                node.setStatus(Node.NodeStatus.ACTIVE);
                node.setProjectId(node.getId());
                break;

            case NOTE:
                node.setStatus(Node.NodeStatus.ACTIVE);
                break;

            case DECISION:
                node.setStatus(Node.NodeStatus.ACTIVE);
                break;

            default:
                break;
        }

        activity.onNodeUpdated(node);

        if (targetType == Node.NodeType.PROJECT) {
            askGenerateProjectStarter(node, activity);
        } else if (targetType == Node.NodeType.DECISION) {
            askGenerateDecisionStarter(node, activity);
        }
    }

    private void showInboxClarifyDialog(final Node node, final MainActivity activity) {
        if (node == null || activity == null) return;

        String[] items = {
                "转成 TASK（下一步任务）",
                "转成 GOAL（目标）",
                "转成 NOTE（笔记）",
                "转成 PROJECT（项目）",
                "转成 DECISION（决策）"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Inbox 快速澄清")
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            convertInboxNode(node, Node.NodeType.TASK, activity);
                            break;
                        case 1:
                            convertInboxNode(node, Node.NodeType.GOAL, activity);
                            break;
                        case 2:
                            convertInboxNode(node, Node.NodeType.NOTE, activity);
                            break;
                        case 3:
                            convertInboxNode(node, Node.NodeType.PROJECT, activity);
                            break;
                        case 4:
                            convertInboxNode(node, Node.NodeType.DECISION, activity);
                            break;
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private TextView buildNodeRow(final Node node, String extra, final MainActivity activity) {
        TextView tv = new TextView(requireContext());
        String title = safe(node.getTitle(), "未命名节点");
        String type = node.getType() == null ? "" : node.getType().label;
        String line = "• " + title + "  [" + type + "]";
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
            if (activity != null && activity.getMindMapView() != null) {
                activity.getMindMapView().focusNodeById(node.getId());
                activity.getMindMapView().selectNodeById(node.getId());
                dismiss();
            }
        });

        tv.setOnLongClickListener(v -> {
            if (activity == null) return true;

            if (isInboxNode(node)) {
                showInboxClarifyDialog(node, activity);
                return true;
            }

            if (isDecisionNode(node)) {
                askGenerateDecisionStarter(node, activity);
                return true;
            }

            if (isLearningNode(node)) {
                askGenerateLearningStarter(node, activity);
                return true;
            }

            if (isKrNode(node)) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("快速更新 KR")
                        .setMessage("当前值：" + node.getKrCurrent() + " / " + node.getKrTarget())
                        .setNegativeButton("取消", null)
                        .setNeutralButton("+1", (d, w) -> addKrValue(node, 1f, activity))
                        .setPositiveButton("+5", (d, w) -> addKrValue(node, 5f, activity))
                        .show();
                return true;
            }

            if (isExecutionNode(node) || isReviewNode(node)) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("快速操作")
                        .setMessage("把这个节点标记为已完成？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("标记 DONE", (d, w) -> markDone(node, activity))
                        .show();
                return true;
            }

            return true;
        });

        return tv;
    }

    private void addSection(LinearLayout root, String title, List<Node> nodes, MainActivity activity) {
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
            Node node = nodes.get(i);
            root.addView(buildNodeRow(node, buildNodeExtra(node), activity));
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

    private void sortTodayNodes(List<Node> list, final String today) {
        Collections.sort(list, new Comparator<Node>() {
            @Override
            public int compare(Node a, Node b) {
                boolean aDueToday = containsDateHint(a.getDueAt(), today);
                boolean bDueToday = containsDateHint(b.getDueAt(), today);
                if (aDueToday != bDueToday) return aDueToday ? -1 : 1;

                int p = Integer.compare(b.getPriority(), a.getPriority());
                if (p != 0) return p;

                int sr = Integer.compare(statusRank(a.getStatus()), statusRank(b.getStatus()));
                if (sr != 0) return sr;

                boolean aHasTrigger = !safe(a.getTriggerCondition(), "").isEmpty();
                boolean bHasTrigger = !safe(b.getTriggerCondition(), "").isEmpty();
                if (aHasTrigger != bHasTrigger) return aHasTrigger ? -1 : 1;

                float ae = a.getEffortEstimate() <= 0f ? 9999f : a.getEffortEstimate();
                float be = b.getEffortEstimate() <= 0f ? 9999f : b.getEffortEstimate();
                int effortCompare = Float.compare(ae, be);
                if (effortCompare != 0) return effortCompare;

                return safe(a.getTitle(), "").compareToIgnoreCase(safe(b.getTitle(), ""));
            }
        });
    }

    private void sortKrNodes(List<Node> list) {
        Collections.sort(list, new Comparator<Node>() {
            @Override
            public int compare(Node a, Node b) {
                float ap = a.getKrTarget() > 0f ? a.getKrCurrent() / a.getKrTarget() : -1f;
                float bp = b.getKrTarget() > 0f ? b.getKrCurrent() / b.getKrTarget() : -1f;
                return Float.compare(bp, ap);
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null || activity.getMindMapView() == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        Map<String, Node> allNodes = activity.getMindMapView().getNodesInternal();
        List<Node> inboxNodes = new ArrayList<>();
        List<Node> todayNodes = new ArrayList<>();
        List<Node> reviewNodes = new ArrayList<>();
        List<Node> riskNodes = new ArrayList<>();
        List<Node> krNodes = new ArrayList<>();

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        for (Node node : allNodes.values()) {
            if (node == null) continue;

            if (isInboxNode(node)) {
                inboxNodes.add(node);
            }

            if (isExecutionNode(node)) {
                boolean important =
                        node.getPriority() >= 4
                                || containsDateHint(node.getDueAt(), today)
                                || !safe(node.getTriggerCondition(), "").isEmpty()
                                || node.getStatus() == Node.NodeStatus.ACTIVE
                                || node.getStatus() == Node.NodeStatus.PLANNED;
                if (important && node.getStatus() != Node.NodeStatus.DONE) {
                    todayNodes.add(node);
                }
            }

            if ((isReviewNode(node) || containsDateHint(node.getReviewAt(), today))
                    && node.getStatus() != Node.NodeStatus.DONE) {
                reviewNodes.add(node);
            }

            if (isRiskOrBlocked(node) && node.getStatus() != Node.NodeStatus.DONE) {
                riskNodes.add(node);
            }

            if (isKrNode(node)) {
                krNodes.add(node);
            }
        }

        sortTodayNodes(todayNodes, today);
        sortKrNodes(krNodes);

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.parseColor("#0B1020"));
        scrollView.addView(root);

        TextView title = buildTitle("科学工作台", 20, true, "#F8FAFC");
        root.addView(title);

        TextView subtitle = buildTitle("单击聚焦；长按可澄清、建项目/决策/学习骨架、快速完成或推进 KR", 13, false, "#94A3B8");
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
        cardRow.addView(buildCard("Inbox", String.valueOf(inboxNodes.size()), "#1E293B"));
        cardRow.addView(buildCard("今日执行", String.valueOf(todayNodes.size()), "#0F766E"));
        cardRow.addView(buildCard("待复盘", String.valueOf(reviewNodes.size()), "#92400E"));
        cardRow.addView(buildCard("风险/受阻", String.valueOf(riskNodes.size()), "#7F1D1D"));
        cardRow.addView(buildCard("KR", String.valueOf(krNodes.size()), "#1D4ED8"));

        hsv.addView(cardRow);
        root.addView(hsv);

        root.addView(buildSpacer(18));

        addSection(root, "Inbox 待澄清", inboxNodes, activity);
        addSection(root, "今日最值得推进（已排序）", todayNodes, activity);
        addSection(root, "待复盘 / 待复习", reviewNodes, activity);
        addSection(root, "高风险 / 受阻节点", riskNodes, activity);
        addSection(root, "关键结果（KR）", krNodes, activity);

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
