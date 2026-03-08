package com.agui.neuralcanvas;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class AiAssistantDialog extends DialogFragment {

    public static AiAssistantDialog newInstance() {
        return new AiAssistantDialog();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                requireContext().getResources().getDisplayMetrics()
        );
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!(getActivity() instanceof MainActivity)) {
            return super.onCreateDialog(savedInstanceState);
        }

        MainActivity activity = (MainActivity) getActivity();
        SimpleDataManager dataManager = activity.getDataManager();
        AiConfig config = dataManager.loadAiConfig();

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(18);
        root.setPadding(p, p, p, p);
        scrollView.addView(root);

        TextView tip = new TextView(requireContext());
        tip.setText("AI可读取节点标题、内容、类型、形状、连接方向、连线文字、粗细和颜色，并生成可执行命令。");
        tip.setTextColor(Color.parseColor("#475569"));
        tip.setTextSize(14);
        root.addView(tip, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        Switch enableSwitch = new Switch(requireContext());
        enableSwitch.setText("启用AI");
        enableSwitch.setChecked(config.isEnabled());
        LinearLayout.LayoutParams switchLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        switchLp.topMargin = dp(12);
        root.addView(enableSwitch, switchLp);

        EditText baseUrlInput = new EditText(requireContext());
        baseUrlInput.setHint("Base URL，例如 https://api.xxx.com/v1");
        baseUrlInput.setText(config.getBaseUrl());
        styleInput(baseUrlInput);
        addWithTopMargin(root, baseUrlInput, 12);

        EditText apiKeyInput = new EditText(requireContext());
        apiKeyInput.setHint("API Key");
        apiKeyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        apiKeyInput.setText(config.getApiKey());
        styleInput(apiKeyInput);
        addWithTopMargin(root, apiKeyInput, 12);

        EditText modelInput = new EditText(requireContext());
        modelInput.setHint("模型名，例如 deepseek-chat");
        modelInput.setText(config.getModel());
        styleInput(modelInput);
        addWithTopMargin(root, modelInput, 12);

        TextView modeTitle = new TextView(requireContext());
        modeTitle.setText("工作范围");
        modeTitle.setTextColor(Color.parseColor("#0F172A"));
        modeTitle.setTextSize(15);
        addWithTopMargin(root, modeTitle, 14);

        RadioGroup scopeGroup = new RadioGroup(requireContext());
        scopeGroup.setOrientation(LinearLayout.HORIZONTAL);

        RadioButton fullGraphButton = new RadioButton(requireContext());
        fullGraphButton.setText("整张图");
        fullGraphButton.setTextColor(Color.parseColor("#0F172A"));
        fullGraphButton.setId(ViewIdGenerator.next());

        RadioButton selectedGraphButton = new RadioButton(requireContext());
        selectedGraphButton.setText("仅选中节点");
        selectedGraphButton.setTextColor(Color.parseColor("#0F172A"));
        selectedGraphButton.setId(ViewIdGenerator.next());

        scopeGroup.addView(fullGraphButton);
        scopeGroup.addView(selectedGraphButton);
        fullGraphButton.setChecked(true);
        addWithTopMargin(root, scopeGroup, 8);

        TextView quickTitle = new TextView(requireContext());
        quickTitle.setText("快捷指令");
        quickTitle.setTextColor(Color.parseColor("#0F172A"));
        quickTitle.setTextSize(15);
        addWithTopMargin(root, quickTitle, 14);

        HorizontalScrollView quickScroll = new HorizontalScrollView(requireContext());
        quickScroll.setHorizontalScrollBarEnabled(false);

        LinearLayout quickRow = new LinearLayout(requireContext());
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        quickScroll.addView(quickRow);

        addQuickButton(quickRow, "总结图谱", "总结当前图谱的核心结构、关键节点和薄弱点");
        addQuickButton(quickRow, "补任务", "补充3个高质量任务节点，并建立合理连接");
        addQuickButton(quickRow, "补关系", "补全关键节点之间缺失的关系，并给连线添加合适文字");
        addQuickButton(quickRow, "整理布局", "整理当前图谱结构，必要时自动布局");
        addQuickButton(quickRow, "查问题", "找出当前图谱中的逻辑冲突、断裂点和重复节点");

        addWithTopMargin(root, quickScroll, 8);

        EditText promptInput = new EditText(requireContext());
        promptInput.setHint("输入你的问题或要求，例如：请补充3个任务节点，并把它们与目标节点连接起来");
        promptInput.setMinLines(7);
        promptInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        styleInput(promptInput);
        addWithTopMargin(root, promptInput, 14);

        TextView resultTitle = new TextView(requireContext());
        resultTitle.setText("AI结果");
        resultTitle.setTextColor(Color.parseColor("#0F172A"));
        resultTitle.setTextSize(15);
        addWithTopMargin(root, resultTitle, 14);

        TextView resultView = new TextView(requireContext());
        resultView.setText("这里会显示连接测试结果、AI回复或错误信息");
        resultView.setTextColor(Color.parseColor("#0F172A"));
        resultView.setTextSize(14);
        resultView.setBackgroundColor(Color.parseColor("#EEF4FF"));
        resultView.setPadding(dp(14), dp(14), dp(14), dp(14));
        addWithTopMargin(root, resultView, 8);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("AI助手")
                .setView(scrollView)
                .setNegativeButton("关闭", null)
                .setNeutralButton("保存配置", (d, which) -> {
                    AiConfig newConfig = buildConfig(enableSwitch, baseUrlInput, apiKeyInput, modelInput);
                    dataManager.saveAiConfig(newConfig);
                    Toast.makeText(requireContext(), "AI配置已保存", Toast.LENGTH_SHORT).show();
                })
                .setPositiveButton("发送", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button neutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

            Button testButton = new Button(requireContext());
            testButton.setText("测试连接");
            testButton.setTextColor(Color.WHITE);
            testButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));

            ((ViewGroup) neutral.getParent()).addView(testButton, 0);

            testButton.setOnClickListener(v -> {
                AiConfig newConfig = buildConfig(enableSwitch, baseUrlInput, apiKeyInput, modelInput);
                dataManager.saveAiConfig(newConfig);

                if (!newConfig.isEnabled()) {
                    Toast.makeText(requireContext(), "请先填写完整配置并启用AI", Toast.LENGTH_SHORT).show();
                    return;
                }

                resultView.setText("正在测试连接...");
                setButtonsEnabled(positive, neutral, testButton, false);

                AiRepository repository = new AiRepository();
                repository.testConnection(newConfig, new AiRepository.SimpleCallback() {
                    @Override
                    public void onSuccess(String message) {
                        if (activity == null) return;
                        activity.runOnUiThread(() -> {
                            resultView.setText(message);
                            setButtonsEnabled(positive, neutral, testButton, true);
                            Toast.makeText(requireContext(), "连接成功", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        if (activity == null) return;
                        activity.runOnUiThread(() -> {
                            resultView.setText(message);
                            setButtonsEnabled(positive, neutral, testButton, true);
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            });

            positive.setOnClickListener(v -> {
                AiConfig newConfig = buildConfig(enableSwitch, baseUrlInput, apiKeyInput, modelInput);
                dataManager.saveAiConfig(newConfig);

                String prompt = promptInput.getText().toString().trim();
                if (prompt.isEmpty()) {
                    promptInput.setError("请输入内容");
                    return;
                }

                if (!newConfig.isEnabled()) {
                    Toast.makeText(requireContext(), "请先完整填写并启用AI配置", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean useSelectedOnly = selectedGraphButton.isChecked();
                AiGraphSnapshot snapshot = useSelectedOnly
                        ? activity.getMindMapView().getSelectedGraphSnapshot()
                        : AiGraphSnapshot.from(
                                activity.getMindMapView().getNodesInternal(),
                                activity.getMindMapView().getConnectionsInternal()
                        );

                int nodeCount = snapshot.nodes == null ? 0 : snapshot.nodes.size();
                if (useSelectedOnly && nodeCount == 0) {
                    Toast.makeText(requireContext(), "当前没有选中节点，无法使用局部模式", Toast.LENGTH_SHORT).show();
                    return;
                }

                resultView.setText("正在请求AI...");
                setButtonsEnabled(positive, neutral, testButton, false);

                String finalPrompt = useSelectedOnly
                        ? "【当前仅基于选中节点子图工作】\n" + prompt
                        : prompt;

                AiRepository repository = new AiRepository();
                repository.askGraph(
                        newConfig,
                        snapshotToNodeMap(snapshot),
                        snapshotToConnectionMap(snapshot),
                        finalPrompt,
                        new AiRepository.AiCallback() {
                            @Override
                            public void onSuccess(AiResponse response) {
                                if (activity == null) return;
                                activity.runOnUiThread(() -> {
                                    resultView.setText(response.getAnswer().isEmpty() ? "AI未返回文字说明" : response.getAnswer());
                                    setButtonsEnabled(positive, neutral, testButton, true);

                                    if (response.getCommands() != null && !response.getCommands().isEmpty()) {
                                        AiCommandPreviewDialog.newInstance(response)
                                                .show(activity.getSupportFragmentManager(), "ai_command_preview");
                                        Toast.makeText(requireContext(), "AI已生成命令，请确认后执行", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(requireContext(), "AI已回复，但未修改图谱", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onError(String message) {
                                if (activity == null) return;
                                activity.runOnUiThread(() -> {
                                    resultView.setText(message);
                                    setButtonsEnabled(positive, neutral, testButton, true);
                                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                );
            });
        });

        bindQuickButtonText(quickRow, promptInput);

        return dialog;
    }

    private void addWithTopMargin(LinearLayout root, android.view.View view, int topMarginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(topMarginDp);
        root.addView(view, lp);
    }

    private void styleInput(EditText editText) {
        editText.setTextColor(Color.parseColor("#0F172A"));
        editText.setHintTextColor(Color.parseColor("#94A3B8"));
        editText.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#60A5FA")));
    }

    private AiConfig buildConfig(Switch enableSwitch, EditText baseUrlInput, EditText apiKeyInput, EditText modelInput) {
        AiConfig config = new AiConfig();
        config.setEnabled(enableSwitch.isChecked());
        config.setBaseUrl(baseUrlInput.getText().toString().trim());
        config.setApiKey(apiKeyInput.getText().toString().trim());
        config.setModel(modelInput.getText().toString().trim());
        return config;
    }

    private void setButtonsEnabled(Button positive, Button neutral, Button testButton, boolean enabled) {
        if (positive != null) positive.setEnabled(enabled);
        if (neutral != null) neutral.setEnabled(enabled);
        if (testButton != null) testButton.setEnabled(enabled);
    }

    private void addQuickButton(LinearLayout parent, String title, String value) {
        Button button = new Button(requireContext());
        button.setText(title);
        button.setTag(value);
        button.setAllCaps(false);
        button.setTextColor(Color.parseColor("#0F172A"));
        button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E2E8F0")));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.rightMargin = dp(8);
        parent.addView(button, lp);
    }

    private void bindQuickButtonText(LinearLayout quickRow, EditText promptInput) {
        for (int i = 0; i < quickRow.getChildCount(); i++) {
            android.view.View child = quickRow.getChildAt(i);
            if (child instanceof Button) {
                child.setOnClickListener(v -> promptInput.setText(String.valueOf(v.getTag())));
            }
        }
    }

    private java.util.Map<String, Node> snapshotToNodeMap(AiGraphSnapshot snapshot) {
        java.util.Map<String, Node> result = new java.util.LinkedHashMap<>();
        if (snapshot == null || snapshot.nodes == null) return result;

        for (AiGraphSnapshot.SnapshotNode item : snapshot.nodes) {
            Node node = new Node(
                    item.title,
                    item.content,
                    item.x,
                    item.y,
                    parseType(item.type)
            );
            node.setShape(parseShape(item.shape));
            node.setWidth(item.width);
            node.setHeight(item.height);

            tryForceId(node, item.id);

            if (item.connectionIds != null) {
                node.setConnectionIds(new java.util.ArrayList<>(item.connectionIds));
            }
            result.put(item.id, node);
        }
        return result;
    }

    private java.util.Map<String, Connection> snapshotToConnectionMap(AiGraphSnapshot snapshot) {
        java.util.Map<String, Connection> result = new java.util.LinkedHashMap<>();
        if (snapshot == null || snapshot.connections == null) return result;

        for (AiGraphSnapshot.SnapshotConnection item : snapshot.connections) {
            Connection connection = new Connection(
                    item.fromNodeId,
                    item.toNodeId,
                    parseConnectionType(item.type),
                    item.label
            );
            if (item.customColor != null) {
                connection.setCustomColor(item.customColor);
            }
            if (item.strokeWidth != null) {
                connection.setStrokeWidth(item.strokeWidth);
            }

            tryForceConnectionId(connection, item.id);

            result.put(item.id, connection);
        }
        return result;
    }

    private Node.NodeType parseType(String value) {
        try {
            return Node.NodeType.valueOf(value == null ? "CONCEPT" : value.toUpperCase());
        } catch (Exception e) {
            return Node.NodeType.CONCEPT;
        }
    }

    private Node.NodeShape parseShape(String value) {
        try {
            return Node.NodeShape.valueOf(value == null ? "RECT" : value.toUpperCase());
        } catch (Exception e) {
            return Node.NodeShape.RECT;
        }
    }

    private Connection.ConnectionType parseConnectionType(String value) {
        try {
            return Connection.ConnectionType.valueOf(value == null ? "SEQUENCE" : value.toUpperCase());
        } catch (Exception e) {
            return Connection.ConnectionType.SEQUENCE;
        }
    }

    private void tryForceId(Node node, String id) {
        try {
            java.lang.reflect.Field field = Node.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(node, id);
        } catch (Exception ignored) {
        }
    }

    private void tryForceConnectionId(Connection connection, String id) {
        try {
            java.lang.reflect.Field field = Connection.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(connection, id);
        } catch (Exception ignored) {
        }
    }

    // 避免直接用资源 id，纯代码场景下给 RadioButton 一个不重复 id
    private static class ViewIdGenerator {
        private static int nextId = 100000;

        static int next() {
            return nextId++;
        }
    }
}
