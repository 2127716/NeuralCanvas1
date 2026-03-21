package com.agui.neuralcanvas;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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

    public static AiAssistantDialog newInstance() { return newInstance(""); }

    public static AiAssistantDialog newInstance(String presetPrompt) {
        AiAssistantDialog dialog = new AiAssistantDialog();
        Bundle args = new Bundle();
        args.putString(ARG_PRESET_PROMPT, presetPrompt == null ? "" : presetPrompt);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!(getActivity() instanceof MainActivity)) return super.onCreateDialog(savedInstanceState);
        MainActivity activity = (MainActivity) getActivity();
        SimpleDataManager dataManager = activity.getDataManager();
        AiConfig config = dataManager.loadAiConfig();
        BrainAutopilotSettings settings = dataManager.loadAutopilotSettings();

        LinearLayout shell = DialogUi.createRoot(this);
        LinearLayout content = DialogUi.createContentColumn(this);
        ScrollView scrollView = DialogUi.createScroll(this, content);
        shell.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout hero = DialogUi.createHero(
                this,
                "AI CONTROL CENTER",
                "AI 主脑",
                "统一成更现代的莫奈风卡片布局。配置、自动巡航和即时对话都放在一个更清晰的界面里。"
        );
        content.addView(hero);

        LinearLayout apiCard = DialogUi.createCard(this);
        DialogUi.addWithTopMargin(this, content, apiCard, 12);
        apiCard.addView(DialogUi.createSectionTitle(this, "API 配置"));
        DialogUi.addWithTopMargin(this, apiCard, DialogUi.createHelper(this, "先把模型连接好，再决定是否开启自动巡航。"), 8);

        Switch enableSwitch = buildSwitch("启用 AI API", config.isEnabled());
        DialogUi.addWithTopMargin(this, apiCard, enableSwitch, 12);
        EditText baseUrlInput = buildInput("Base URL", config.getBaseUrl(), false, 1);
        DialogUi.addWithTopMargin(this, apiCard, baseUrlInput, 12);
        EditText apiKeyInput = buildInput("API Key", config.getApiKey(), false, 1);
        DialogUi.addWithTopMargin(this, apiCard, apiKeyInput, 12);
        EditText modelInput = buildInput("模型名", config.getModel(), false, 1);
        DialogUi.addWithTopMargin(this, apiCard, modelInput, 12);

        LinearLayout autoCard = DialogUi.createCard(this);
        DialogUi.addWithTopMargin(this, content, autoCard, 12);
        autoCard.addView(DialogUi.createSectionTitle(this, "自动巡航"));
        DialogUi.addWithTopMargin(this, autoCard, DialogUi.createHelper(this, "低风险改动可自动执行，其他情况优先给你引导。"), 8);
        Switch autopilotSwitch = buildSwitch("启用 API 自动巡航", settings.isApiAutopilotEnabled());
        DialogUi.addWithTopMargin(this, autoCard, autopilotSwitch, 12);
        Switch safeApplySwitch = buildSwitch("自动执行低风险改动", settings.isAutoApplyLowRiskChanges());
        DialogUi.addWithTopMargin(this, autoCard, safeApplySwitch, 10);
        Switch notifySwitch = buildSwitch("后台提醒", settings.isNotificationsEnabled());
        DialogUi.addWithTopMargin(this, autoCard, notifySwitch, 10);
        Switch resumeSwitch = buildSwitch("打开 App 时触发巡航脉冲", settings.isInAppPulseOnResume());
        DialogUi.addWithTopMargin(this, autoCard, resumeSwitch, 10);
        EditText intervalInput = buildInput("巡航间隔（小时）", String.valueOf(settings.getIntervalHours()), false, 1);
        intervalInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        DialogUi.addWithTopMargin(this, autoCard, intervalInput, 12);

        TextView scopeLabel = DialogUi.createSectionTitle(this, "巡航范围");
        DialogUi.addWithTopMargin(this, autoCard, scopeLabel, 14);
        RadioGroup scopeGroup = buildScopeGroup();
        RadioButton relevant = (RadioButton) scopeGroup.getChildAt(0);
        RadioButton full = (RadioButton) scopeGroup.getChildAt(1);
        if ("full".equals(settings.getAssistantScope())) full.setChecked(true); else relevant.setChecked(true);
        DialogUi.addWithTopMargin(this, autoCard, scopeGroup, 10);

        EditText autopilotInstruction = buildInput("自动巡航指令", settings.getAutopilotInstruction(), true, 4);
        DialogUi.addWithTopMargin(this, autoCard, autopilotInstruction, 12);

        LinearLayout quickCard = DialogUi.createCard(this);
        DialogUi.addWithTopMargin(this, content, quickCard, 12);
        quickCard.addView(DialogUi.createSectionTitle(this, "即时对话"));
        DialogUi.addWithTopMargin(this, quickCard, DialogUi.createHelper(this, "点一下模板提示，直接把高质量指令塞进输入框。"), 8);

        LinearLayout chipRow = new LinearLayout(requireContext());
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        chipRow.setPadding(0, DialogUi.dp(this, 10), 0, 0);
        addQuickChip(chipRow, "学习补强", "请把当前相关子图补成高质量学习链：检索问题、例子、反例、迁移任务。优先输出 commands", true);
        addQuickChip(chipRow, "执行闭环", "请把当前相关子图补成可执行闭环：最小下一步、触发条件、阻碍、复盘锚点。优先输出 commands", false);
        addQuickChip(chipRow, "决策红队", "请攻击当前决策结构，补关键反证、风险、替代方案和证据。优先输出 commands", false);
        quickCard.addView(DialogUi.wrapChipRow(this, chipRow));

        EditText promptInput = buildInput("给 AI 的要求", getArguments() == null ? "" : getArguments().getString(ARG_PRESET_PROMPT, ""), true, 7);
        bindQuickChips(chipRow, promptInput);
        DialogUi.addWithTopMargin(this, quickCard, promptInput, 12);

        TextView resultView = new TextView(requireContext());
        resultView.setText("AI 回复会显示在这里。自动巡航不会在这里被动执行；它会按你的设置在后台或打开 App 时自动运行。\n\n建议先让自动巡航发现问题，再按引导做局部修正。");
        resultView.setTextColor(ThemeManager.getTextPrimary());
        resultView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        resultView.setLineSpacing(0f, 1.15f);
        resultView.setPadding(DialogUi.dp(this, 16), DialogUi.dp(this, 14), DialogUi.dp(this, 16), DialogUi.dp(this, 14));
        resultView.setBackground(DialogUi.createFieldBackground(this));
        DialogUi.addWithTopMargin(this, quickCard, resultView, 12);

        LinearLayout footer = new LinearLayout(requireContext());
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(0, DialogUi.dp(this, 12), 0, DialogUi.dp(this, 2));
        shell.addView(footer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView closeBtn = DialogUi.createFooterButton(this, "关闭", ThemeManager.getTextSecondary(), ThemeManager.getSurface(), false);
        TextView saveBtnView = DialogUi.createFooterButton(this, "保存配置", ThemeManager.getAccent(), ThemeManager.getAccentSoft(), true);
        TextView sendBtnView = DialogUi.createFooterButton(this, "发送", ThemeManager.getTextPrimary(), ThemeManager.getAccentSoft(), true);

        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp1.rightMargin = DialogUi.dp(this, 8);
        footer.addView(closeBtn, lp1);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp2.rightMargin = DialogUi.dp(this, 8);
        footer.addView(saveBtnView, lp2);
        footer.addView(sendBtnView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(shell)
                .create();

        closeBtn.setOnClickListener(v -> dialog.dismiss());

        saveBtnView.setOnClickListener(v -> {
            AiConfig newConfig = new AiConfig();
            newConfig.setEnabled(enableSwitch.isChecked());
            newConfig.setBaseUrl(baseUrlInput.getText().toString().trim());
            newConfig.setApiKey(apiKeyInput.getText().toString().trim());
            newConfig.setModel(modelInput.getText().toString().trim());
            dataManager.saveAiConfig(newConfig);

            settings.setEnabled(true);
            settings.setApiAutopilotEnabled(autopilotSwitch.isChecked());
            settings.setAutoApplyLowRiskChanges(safeApplySwitch.isChecked());
            settings.setNotificationsEnabled(notifySwitch.isChecked());
            settings.setInAppPulseOnResume(resumeSwitch.isChecked());
            settings.setIntervalHours(parseIntSafe(intervalInput.getText().toString(), 8));
            settings.setAssistantScope(full.isChecked() ? "full" : "relevant");
            settings.setAutopilotInstruction(autopilotInstruction.getText().toString().trim());
            dataManager.saveAutopilotSettings(settings);
            BrainAutopilotScheduler.ensureScheduled(requireContext());
            BrainAutopilotScheduler.requestImmediatePulse(requireContext());
            Toast.makeText(requireContext(), "AI 主脑配置已保存", Toast.LENGTH_SHORT).show();
        });

        sendBtnView.setOnClickListener(v -> {
            AiConfig currentConfig = new AiConfig();
            currentConfig.setEnabled(enableSwitch.isChecked());
            currentConfig.setBaseUrl(baseUrlInput.getText().toString().trim());
            currentConfig.setApiKey(apiKeyInput.getText().toString().trim());
            currentConfig.setModel(modelInput.getText().toString().trim());
            String prompt = promptInput.getText().toString().trim();
            if (!currentConfig.isEnabled()) {
                Toast.makeText(activity, "请先完成 API 配置", Toast.LENGTH_SHORT).show();
                return;
            }
            if (prompt.isEmpty()) {
                promptInput.setError("请输入内容");
                return;
            }

            AiRepository repository = new AiRepository();
            AiRepository.PreparedRequest prepared = repository.prepareRelevantRequest(
                    activity.getMindMapView().getNodesInternal(),
                    activity.getMindMapView().getConnectionsInternal(),
                    prompt,
                    full.isChecked()
            );
            resultView.setText("正在请求 AI...");
            repository.askGraph(currentConfig, prepared.snapshot, prepared.finalPrompt, prepared.layoutAllowed, new AiRepository.AiCallback() {
                @Override
                public void onSuccess(AiResponse response) {
                    activity.runOnUiThread(() -> {
                        resultView.setText(response.getAnswer().isEmpty() ? "AI 未返回额外说明" : response.getAnswer());
                        if (response.getCommands() != null && !response.getCommands().isEmpty()) {
                            AiCommandPreviewDialog.newInstance(response)
                                    .show(activity.getSupportFragmentManager(), "ai_command_preview");
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    activity.runOnUiThread(() -> resultView.setText(message));
                }
            });
        });

        dialog.setOnShowListener(d -> DialogUi.styleWindow(this, dialog));
        return dialog;
    }

    private Switch buildSwitch(String text, boolean checked) {
        Switch sw = new Switch(requireContext());
        sw.setText(text);
        sw.setChecked(checked);
        sw.setTextColor(ThemeManager.getTextPrimary());
        sw.setTrackTintList(ColorStateList.valueOf(ThemeManager.withAlpha(ThemeManager.getAccent(), 90)));
        sw.setThumbTintList(ColorStateList.valueOf(ThemeManager.getAccent()));
        return sw;
    }

    private EditText buildInput(String hint, String value, boolean multiLine, int minLines) {
        int type = multiLine ? (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE) : InputType.TYPE_CLASS_TEXT;
        return DialogUi.createInput(this, hint, value, type, minLines);
    }

    private RadioGroup buildScopeGroup() {
        RadioGroup group = new RadioGroup(requireContext());
        group.setOrientation(LinearLayout.HORIZONTAL);
        RadioButton relevant = buildRadio("相关子图");
        RadioButton full = buildRadio("整张图");
        group.addView(relevant);
        group.addView(full);
        return group;
    }

    private RadioButton buildRadio(String text) {
        RadioButton button = new RadioButton(requireContext());
        button.setText(text);
        button.setTextColor(ThemeManager.getTextPrimary());
        button.setButtonTintList(ColorStateList.valueOf(ThemeManager.getAccent()));
        return button;
    }

    private void addQuickChip(LinearLayout parent, String title, String value, boolean accent) {
        TextView chip = DialogUi.createChip(this, title, accent);
        chip.setTag(value);
        parent.addView(chip);
    }

    private void bindQuickChips(LinearLayout row, EditText promptInput) {
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            child.setOnClickListener(v -> promptInput.setText(String.valueOf(v.getTag())));
        }
    }

    private int parseIntSafe(String value, int def) {
        try { return Integer.parseInt(value.trim()); }
        catch (Exception e) { return def; }
    }
}
