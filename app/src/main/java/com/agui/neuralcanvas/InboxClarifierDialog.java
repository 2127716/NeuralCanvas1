package com.agui.neuralcanvas;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InboxClarifierDialog {

    public interface Callback {
        void onNodeConverted(Node node);
        void onBatchFinished();
    }

    public static void show(Context context,
                            Map<String, Node> nodes,
                            Callback callback) {
        if (context == null || nodes == null) return;

        List<Node> inboxNodes = WorkflowEngine.getInboxNodes(nodes);

        if (inboxNodes.isEmpty()) {
            new AlertDialog.Builder(context)
                    .setTitle("Inbox 澄清")
                    .setMessage("当前没有待澄清的 Inbox 节点。")
                    .setPositiveButton("知道了", null)
                    .show();
            return;
        }

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(context, 16);
        root.setPadding(padding, padding, padding, padding);

        TextView header = new TextView(context);
        header.setText("待澄清数量：" + inboxNodes.size() + "\n点一项后选择要转换成的类型。");
        header.setTextSize(15f);
        root.addView(header);

        ListView listView = new ListView(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 360)
        );
        listView.setLayoutParams(lp);

        final List<String> items = new ArrayList<>();
        for (Node node : inboxNodes) {
            items.add(buildInboxLine(node));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_list_item_1,
                items
        );
        listView.setAdapter(adapter);
        root.addView(listView);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Inbox 澄清")
                .setView(root)
                .setNegativeButton("关闭", null)
                .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Node target = inboxNodes.get(position);
            showTypeChooser(context, target, callback, () -> {
                items.set(position, buildInboxLine(target));
                adapter.notifyDataSetChanged();
            });
        });

        dialog.show();
    }

    private static void showTypeChooser(Context context,
                                        Node node,
                                        Callback callback,
                                        Runnable onUpdated) {
        final Node.NodeType[] choices = new Node.NodeType[] {
                Node.NodeType.TASK,
                Node.NodeType.ACTION,
                Node.NodeType.PROJECT,
                Node.NodeType.IDEA,
                Node.NodeType.CONCEPT,
                Node.NodeType.QUESTION,
                Node.NodeType.RESOURCE,
                Node.NodeType.DECISION,
                Node.NodeType.NOTE
        };

        String[] labels = new String[choices.length];
        for (int i = 0; i < choices.length; i++) {
            labels[i] = getTypeLabel(choices[i]);
        }

        new AlertDialog.Builder(context)
                .setTitle("将节点转换为")
                .setItems(labels, (dialog, which) -> {
                    Node.NodeType targetType = choices[which];
                    node.setType(targetType);
                    WorkflowEngine.normalizeNodeForWorkflow(node);

                    if (targetType == Node.NodeType.PROJECT) {
                        node.setProjectId(node.getId());
                        node.addTags("Project", "InboxConverted");
                    } else if (targetType == Node.NodeType.TASK
                            || targetType == Node.NodeType.ACTION) {
                        node.addTags("Actionable", "InboxConverted");
                    } else if (targetType == Node.NodeType.DECISION) {
                        node.addTags("Decision", "InboxConverted");
                    } else if (targetType == Node.NodeType.CONCEPT
                            || targetType == Node.NodeType.QUESTION
                            || targetType == Node.NodeType.RESOURCE
                            || targetType == Node.NodeType.NOTE) {
                        node.addTags("Learning", "InboxConverted");
                    } else {
                        node.addTag("InboxConverted");
                    }

                    if (callback != null) {
                        callback.onNodeConverted(node);
                    }
                    if (onUpdated != null) {
                        onUpdated.run();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private static String buildInboxLine(Node node) {
        String title = node.getTitle() == null || node.getTitle().trim().isEmpty()
                ? "(无标题)"
                : node.getTitle().trim();

        return title
                + "\n类型：" + getTypeLabel(node.getType())
                + "｜状态：" + getStatusLabel(node.getStatus());
    }

    private static String getTypeLabel(Node.NodeType type) {
        if (type == null) return "未知";
        switch (type) {
            case INBOX: return "Inbox";
            case TASK: return "任务";
            case ACTION: return "动作";
            case PROJECT: return "项目";
            case IDEA: return "想法";
            case CONCEPT: return "概念";
            case QUESTION: return "问题";
            case RESOURCE: return "资源";
            case DECISION: return "决策";
            case NOTE: return "笔记";
            default: return type.name();
        }
    }

    private static String getStatusLabel(Node.NodeStatus status) {
        if (status == null) return "未设定";
        switch (status) {
            case ACTIVE: return "进行中";
            case PLANNED: return "计划中";
            case WAITING: return "等待中";
            case BLOCKED: return "受阻";
            case DONE: return "已完成";
            case REVIEW: return "待复盘";
            case SOMEDAY: return "也许以后";
            default: return status.name();
        }
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }
}
