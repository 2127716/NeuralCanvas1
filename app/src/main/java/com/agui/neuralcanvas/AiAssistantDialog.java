package com.agui.neuralcanvas;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
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

    private static final String ARG_PRESET_PROMPT = "preset_prompt";

    public static AiAssistantDialog newInstance() {
        return newInstance("");
    }

    public static AiAssistantDialog newInstance(String presetPrompt) {
        AiAssistantDialog dialog = new AiAssistantDialog();
        Bundle args = new Bundle();
        args.putString(ARG_PRESET_PROMPT, presetPrompt == null ? "" : presetPrompt);
        dialog.setArguments(args);
        return dialog;
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
        tip.setText("AI可读取节点标题、内容、类型、形状、连接方向、连线文字、粗细和颜色。默认优先只读相关子图，不会无脑读完整张图。");
        tip.setTextColor(Color.parseColor("#475569"));
        tip.setTextSize(14);
        root.addView(tip);

        Switch enableSwitch = new Switch(requireContext());
        enableSwitch.setText("启用AI");
        enableSwitch.setChecked(config.isEnabled());
        addWithTopMargin(root, enableSwitch, 12);

        EditText baseUrlInput = new EditText(requireContext());
        baseUrlInput.setHint("Base URL，例如 https://api.xxx.com/v1");
        baseUrlInput.setText(config.getBaseUrl());
        styleInput(baseUrlInput);
        addWithTopMargin(root, baseUrlInput, 12);

        EditText apiKeyInput = new EditText(requireContext());
        apiKeyInput.setHint("API Key");
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

        RadioButton smartScope = new RadioButton(requireContext());
        smartScope.setText("智能相关子图");
        smartScope.setTextColor(Color.parseColor("#0F172A"));

        RadioButton fullScope = new RadioButton(requireContext());
        fullScope.setText("整张图");
        fullScope.setTextColor(Color.parseColor("#0F172A"));

        RadioButton selectedScope = new RadioButton(requireContext());
        selectedScope.setText("仅选中节点");
        selectedScope.setTextColor(Color.parseColor("#0F172A"));

        scopeGroup.addView(smartScope);
        scopeGroup.addView(fullScope);
        scopeGroup.addView(selectedScope);
        smartScope.setChecked(true);
        addWithTopMargin(root, scopeGroup, 8);

        Switch backgroundSwitch = new Switch(requireContext());
        backgroundSwitch.setText("后台回答（关闭弹窗继续等待，返回后再提示）");
        backgroundSwitch.setChecked(false);
        addWithTopMargin(root, backgroundSwitch, 12);

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
        addQuickButton(quickRow, "查问题", "找出当前图谱中的逻辑冲突、断裂点和重复节点");
        addQuickButton(quickRow, "删杂线", "识别无关紧要或冗余的连线，并给出保守删除建议，不要乱删");
        addQuickButton(quickRow, "科学缺口", "请作为科学方法教练，检查当前图谱在目标、执行、证据、复盘、学习上的关键缺口，并优先输出保守 commands 进行补强");
        addQuickButton(quickRow, "执行补强", "请把当前焦点节点补成可执行闭环：最小下一步、If-Then、阻碍、预防动作、复盘锚点、保守估时。优先输出 commands");
        addQuickButton(quickRow, "学习补强", "请把当前学习节点补成高质量学习链：检索练习、反例/边界、迁移应用、自测问题。优先输出 commands");
        addQuickButton(quickRow, "决策红队", "请切换到红队视角，攻击当前节点及其相邻结构，补少量高价值反证、风险、替代方案节点，并用明确关系连接。优先输出 commands");

        addWithTopMargin(root, quickScroll, 8);

        EditText promptInput = new EditText(requireContext());
        promptInput.setHint("输入你的问题或要求，例如：请补充3个任务节点，并把它们与目标节点连接起来");
        String presetPrompt = getArguments() == null ? "" : String.valueOf(getArguments().getString(ARG_PRESET_PROMPT, ""));
        if (!presetPrompt.trim().isEmpty()) {
            promptInput.setText(presetPrompt.trim());
            promptInput.setSelection(promptInput.getText().length());
        }
        promptInput.setMinLines(7);
        promptInput.setTextColor(Color.parseColor("#0F172A"));
        promptInput.setHintTextColor(Color.parseColor("#94A3B8"));
        promptInput.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#60A5FA")));
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

                new AiRepository().testConnection(newConfig, new AiRepository.SimpleCallback() {
                    @Override
                    public void onSuccess(String message) {
                        activity.runOnUiThread(() -> {
                            resultView.setText(message);
                            setButtonsEnabled(positive, neutral, testButton, true);
                            Toast.makeText(activity, "连接成功", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        activity.runOnUiThread(() -> {
                            resultView.setText(message);
                            setButtonsEnabled(positive, neutral, testButton, true);
                            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(activity, "请先完整填写并启用AI配置", Toast.LENGTH_SHORT).show();
                    return;
                }

                AiRepository repository = new AiRepository();
                AiRepository.PreparedRequest prepared;

                if (selectedScope.isChecked()) {
                    AiGraphSnapshot selected = activity.getMindMapView().getSelectedGraphSnapshot();
                    int nodeCount = selected == null || selected.nodes == null ? 0 : selected.nodes.size();
                    if (nodeCount == 0) {
                        Toast.makeText(activity, "当前没有选中节点，无法使用局部模式", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    prepared = new AiRepository.PreparedRequest();
                    prepared.snapshot = selected;
                    prepared.finalPrompt = "【当前仅基于选中节点子图工作】\n" + prompt;
                    prepared.layoutAllowed = containsLayoutIntent(prompt);
                } else if (fullScope.isChecked()) {
                    prepared = repository.prepareRelevantRequest(
                            activity.getMindMapView().getNodesInternal(),
                            activity.getMindMapView().getConnectionsInternal(),
                            prompt,
                            true
                    );
                } else {
                    prepared = repository.prepareRelevantRequest(
                            activity.getMindMapView().getNodesInternal(),
                            activity.getMindMapView().getConnectionsInternal(),
                            prompt,
                            false
                    );
                }

                resultView.setText("正在请求AI...");
                setButtonsEnabled(positive, neutral, testButton, false);

                boolean background = backgroundSwitch.isChecked();
                if (background) {
                    Toast.makeText(activity, "已转为后台回答，完成后会提示你", Toast.LENGTH_SHORT).show();
                    dismissAllowingStateLoss();
                }

                repository.askGraph(
                        newConfig,
                        prepared.snapshot,
                        prepared.finalPrompt,
                        prepared.layoutAllowed,
                        new AiRepository.AiCallback() {
                            @Override
                            public void onSuccess(AiResponse response) {
                                activity.runOnUiThread(() -> {
                                    if (!background && isAdded()) {
                                        resultView.setText(response.getAnswer().isEmpty() ? "AI未返回文字说明" : response.getAnswer());
                                        setButtonsEnabled(positive, neutral, testButton, true);
                                    }

                                    if (response.getCommands() != null && !response.getCommands().isEmpty()) {
                                        AiCommandPreviewDialog.newInstance(response)
                                                .show(activity.getSupportFragmentManager(), "ai_command_preview");
                                        Toast.makeText(activity, "AI已生成命令，请确认后执行", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(activity, "AI已回复，但未修改图谱", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onError(String message) {
                                activity.runOnUiThread(() -> {
                                    if (!background && isAdded()) {
                                        resultView.setText(message);
                                        setButtonsEnabled(positive, neutral, testButton, true);
                                    }
                                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
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

    private boolean containsLayoutIntent(String text) {
        String s = text == null ? "" : text.trim();
        return s.contains("布局")
                || s.contains("重排")
                || s.contains("整理")
                || s.contains("排列")
                || s.contains("排版")
                || s.contains("重新排列")
                || s.contains("自动布局");
    }
}
