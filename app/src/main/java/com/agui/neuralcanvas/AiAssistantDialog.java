package com.agui.neuralcanvas;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
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
    public static AiAssistantDialog newInstance(String presetPrompt) { AiAssistantDialog dialog = new AiAssistantDialog(); Bundle args = new Bundle(); args.putString(ARG_PRESET_PROMPT, presetPrompt == null ? "" : presetPrompt); dialog.setArguments(args); return dialog; }
    private int dp(int value) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, requireContext().getResources().getDisplayMetrics()); }
    @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!(getActivity() instanceof MainActivity)) return super.onCreateDialog(savedInstanceState);
        MainActivity activity = (MainActivity) getActivity();
        SimpleDataManager dataManager = activity.getDataManager();
        AiConfig config = dataManager.loadAiConfig();
        BrainAutopilotSettings settings = dataManager.loadAutopilotSettings();
        ScrollView scrollView = new ScrollView(requireContext()); scrollView.setFillViewport(true);
        LinearLayout root = new LinearLayout(requireContext()); root.setOrientation(LinearLayout.VERTICAL); int p = dp(18); root.setPadding(p,p,p,p); scrollView.addView(root);
        addSectionTitle(root, "API 配置");
        Switch enableSwitch = new Switch(requireContext()); enableSwitch.setText("启用 AI API"); enableSwitch.setChecked(config.isEnabled()); addTop(root, enableSwitch, 8);
        EditText baseUrlInput = input("Base URL", config.getBaseUrl()); addTop(root, baseUrlInput, 10);
        EditText apiKeyInput = input("API Key", config.getApiKey()); addTop(root, apiKeyInput, 10);
        EditText modelInput = input("模型名", config.getModel()); addTop(root, modelInput, 10);
        addSectionTitle(root, "自动巡航");
        Switch autopilotSwitch = new Switch(requireContext()); autopilotSwitch.setText("启用 API 自动巡航"); autopilotSwitch.setChecked(settings.isApiAutopilotEnabled()); addTop(root, autopilotSwitch, 8);
        Switch safeApplySwitch = new Switch(requireContext()); safeApplySwitch.setText("自动执行低风险改动"); safeApplySwitch.setChecked(settings.isAutoApplyLowRiskChanges()); addTop(root, safeApplySwitch, 8);
        Switch notifySwitch = new Switch(requireContext()); notifySwitch.setText("后台提醒"); notifySwitch.setChecked(settings.isNotificationsEnabled()); addTop(root, notifySwitch, 8);
        Switch resumeSwitch = new Switch(requireContext()); resumeSwitch.setText("打开 App 时触发巡航脉冲"); resumeSwitch.setChecked(settings.isInAppPulseOnResume()); addTop(root, resumeSwitch, 8);
        EditText intervalInput = input("巡航间隔（小时）", String.valueOf(settings.getIntervalHours())); addTop(root, intervalInput, 10);
        addSectionTitle(root, "巡航范围");
        RadioGroup scopeGroup = new RadioGroup(requireContext()); scopeGroup.setOrientation(LinearLayout.HORIZONTAL); RadioButton relevant = new RadioButton(requireContext()); relevant.setText("相关子图"); RadioButton full = new RadioButton(requireContext()); full.setText("整张图"); scopeGroup.addView(relevant); scopeGroup.addView(full); if ("full".equals(settings.getAssistantScope())) full.setChecked(true); else relevant.setChecked(true); addTop(root, scopeGroup, 6);
        EditText autopilotInstruction = inputMulti("自动巡航指令", settings.getAutopilotInstruction(), 4); addTop(root, autopilotInstruction, 10);
        addSectionTitle(root, "即时对话");
        LinearLayout chipRow = new LinearLayout(requireContext()); chipRow.setOrientation(LinearLayout.HORIZONTAL); addQuickButton(chipRow, "学习补强", "请把当前相关子图补成高质量学习链：检索问题、例子、反例、迁移任务。优先输出 commands"); addQuickButton(chipRow, "执行闭环", "请把当前相关子图补成可执行闭环：最小下一步、触发条件、阻碍、复盘锚点。优先输出 commands"); addQuickButton(chipRow, "决策红队", "请攻击当前决策结构，补关键反证、风险、替代方案和证据。优先输出 commands"); addTop(root, chipRow, 8);
        EditText promptInput = inputMulti("给 AI 的要求", getArguments() == null ? "" : getArguments().getString(ARG_PRESET_PROMPT, ""), 6); addTop(root, promptInput, 10);
        TextView resultView = new TextView(requireContext()); resultView.setText("AI 回复会显示在这里。自动巡航不在这里触发，它会在后台或打开 App 时自动运行。\n\n建议：平时少手动操作，多让自动巡航先发现问题，再按引导处理。\n"); resultView.setTextColor(Color.parseColor("#334155")); resultView.setBackgroundColor(Color.parseColor("#EEF4FF")); resultView.setPadding(dp(14),dp(14),dp(14),dp(14)); addTop(root, resultView, 12);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setTitle("AI 主脑").setView(scrollView).setNegativeButton("关闭", null).setNeutralButton("保存配置", null).setPositiveButton("发送", null).create();
        dialog.setOnShowListener(d -> { Button saveBtn = dialog.getButton(AlertDialog.BUTTON_NEUTRAL); Button sendBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE); saveBtn.setOnClickListener(v -> { AiConfig newConfig = new AiConfig(); newConfig.setEnabled(enableSwitch.isChecked()); newConfig.setBaseUrl(baseUrlInput.getText().toString().trim()); newConfig.setApiKey(apiKeyInput.getText().toString().trim()); newConfig.setModel(modelInput.getText().toString().trim()); dataManager.saveAiConfig(newConfig); settings.setEnabled(true); settings.setApiAutopilotEnabled(autopilotSwitch.isChecked()); settings.setAutoApplyLowRiskChanges(safeApplySwitch.isChecked()); settings.setNotificationsEnabled(notifySwitch.isChecked()); settings.setInAppPulseOnResume(resumeSwitch.isChecked()); settings.setIntervalHours(parseIntSafe(intervalInput.getText().toString(), 8)); settings.setAssistantScope(full.isChecked() ? "full" : "relevant"); settings.setAutopilotInstruction(autopilotInstruction.getText().toString().trim()); dataManager.saveAutopilotSettings(settings); BrainAutopilotScheduler.ensureScheduled(requireContext()); BrainAutopilotScheduler.requestImmediatePulse(requireContext()); Toast.makeText(requireContext(), "AI 主脑配置已保存", Toast.LENGTH_SHORT).show(); }); sendBtn.setOnClickListener(v -> { AiConfig currentConfig = new AiConfig(); currentConfig.setEnabled(enableSwitch.isChecked()); currentConfig.setBaseUrl(baseUrlInput.getText().toString().trim()); currentConfig.setApiKey(apiKeyInput.getText().toString().trim()); currentConfig.setModel(modelInput.getText().toString().trim()); String prompt = promptInput.getText().toString().trim(); if (!currentConfig.isEnabled()) { Toast.makeText(activity, "请先完成 API 配置", Toast.LENGTH_SHORT).show(); return; } if (prompt.isEmpty()) { promptInput.setError("请输入内容"); return; } AiRepository repository = new AiRepository(); AiRepository.PreparedRequest prepared = repository.prepareRelevantRequest(activity.getMindMapView().getNodesInternal(), activity.getMindMapView().getConnectionsInternal(), prompt, full.isChecked()); resultView.setText("正在请求 AI..."); repository.askGraph(currentConfig, prepared.snapshot, prepared.finalPrompt, prepared.layoutAllowed, new AiRepository.AiCallback() { @Override public void onSuccess(AiResponse response) { activity.runOnUiThread(() -> { resultView.setText(response.getAnswer().isEmpty() ? "AI 未返回额外说明" : response.getAnswer()); if (response.getCommands() != null && !response.getCommands().isEmpty()) AiCommandPreviewDialog.newInstance(response).show(activity.getSupportFragmentManager(), "ai_command_preview"); }); } @Override public void onError(String message) { activity.runOnUiThread(() -> resultView.setText(message)); } }); }); });
        bindQuickButtons(chipRow, promptInput); return dialog; }
    private void addSectionTitle(LinearLayout root, String text) { TextView tv = new TextView(requireContext()); tv.setText(text); tv.setTextColor(Color.parseColor("#0F172A")); tv.setTextSize(16); tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD); addTop(root, tv, 14); }
    private void addTop(LinearLayout root, android.view.View view, int top) { LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); lp.topMargin = dp(top); root.addView(view, lp); }
    private EditText input(String hint, String value) { EditText et = new EditText(requireContext()); et.setHint(hint); et.setText(value == null ? "" : value); et.setTextColor(Color.parseColor("#0F172A")); et.setHintTextColor(Color.parseColor("#94A3B8")); et.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#60A5FA"))); return et; }
    private EditText inputMulti(String hint, String value, int minLines) { EditText et = input(hint, value); et.setMinLines(minLines); return et; }
    private void addQuickButton(LinearLayout parent, String title, String value) { Button button = new Button(requireContext()); button.setText(title); button.setTag(value); button.setAllCaps(false); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); lp.rightMargin = dp(6); parent.addView(button, lp); }
    private void bindQuickButtons(LinearLayout row, EditText promptInput) { for (int i=0;i<row.getChildCount();i++) row.getChildAt(i).setOnClickListener(v -> promptInput.setText(String.valueOf(v.getTag()))); }
    private int parseIntSafe(String value, int def) { try { return Integer.parseInt(value.trim()); } catch (Exception e) { return def; } }
}
