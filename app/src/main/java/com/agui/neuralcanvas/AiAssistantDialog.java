package com.agui.neuralcanvas;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
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
        int p = dp(20);
        root.setPadding(p, p, p, p);
        scrollView.addView(root);

        TextView tip = new TextView(requireContext());
        tip.setText("AI将读取当前全部节点、内容、连接关系、箭头方向，并可返回可执行编辑命令。");
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
        switchLp.topMargin = dp(14);
        root.addView(enableSwitch, switchLp);

        EditText baseUrlInput = new EditText(requireContext());
        baseUrlInput.setHint("Base URL，例如 https://api.xxx.com/v1");
        baseUrlInput.setText(config.getBaseUrl());
        styleInput(baseUrlInput);
        LinearLayout.LayoutParams baseLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        baseLp.topMargin = dp(14);
        root.addView(baseUrlInput, baseLp);

        EditText apiKeyInput = new EditText(requireContext());
        apiKeyInput.setHint("API Key");
        apiKeyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        apiKeyInput.setText(config.getApiKey());
        styleInput(apiKeyInput);
        LinearLayout.LayoutParams keyLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        keyLp.topMargin = dp(14);
        root.addView(apiKeyInput, keyLp);

        EditText modelInput = new EditText(requireContext());
        modelInput.setHint("模型名，例如 deepseek-chat");
        modelInput.setText(config.getModel());
        styleInput(modelInput);
        LinearLayout.LayoutParams modelLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        modelLp.topMargin = dp(14);
        root.addView(modelInput, modelLp);

        EditText promptInput = new EditText(requireContext());
        promptInput.setHint("输入你的问题或要求，例如：总结当前图谱，并补充3个任务节点");
        promptInput.setMinLines(6);
        promptInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        styleInput(promptInput);
        LinearLayout.LayoutParams promptLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        promptLp.topMargin = dp(16);
        root.addView(promptInput, promptLp);

        TextView resultView = new TextView(requireContext());
        resultView.setText("AI回复会显示在这里");
        resultView.setTextColor(Color.parseColor("#0F172A"));
        resultView.setTextSize(14);
        resultView.setBackgroundColor(Color.parseColor("#EEF4FF"));
        resultView.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams resultLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        resultLp.topMargin = dp(16);
        root.addView(resultView, resultLp);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("AI助手")
                .setView(scrollView)
                .setNegativeButton("关闭", null)
                .setNeutralButton("保存配置", (d, which) -> {
                    AiConfig newConfig = new AiConfig();
                    newConfig.setEnabled(enableSwitch.isChecked());
                    newConfig.setBaseUrl(baseUrlInput.getText().toString().trim());
                    newConfig.setApiKey(apiKeyInput.getText().toString().trim());
                    newConfig.setModel(modelInput.getText().toString().trim());
                    dataManager.saveAiConfig(newConfig);
                    Toast.makeText(requireContext(), "AI配置已保存", Toast.LENGTH_SHORT).show();
                })
                .setPositiveButton("发送", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            AiConfig newConfig = new AiConfig();
            newConfig.setEnabled(enableSwitch.isChecked());
            newConfig.setBaseUrl(baseUrlInput.getText().toString().trim());
            newConfig.setApiKey(apiKeyInput.getText().toString().trim());
            newConfig.setModel(modelInput.getText().toString().trim());
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

            resultView.setText("正在请求AI...");
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

            AiRepository repository = new AiRepository();
            repository.askGraph(
                    newConfig,
                    activity.getMindMapView().getNodes(),
                    activity.getMindMapView().getConnections(),
                    prompt,
                    new AiRepository.AiCallback() {
                        @Override
                        public void onSuccess(AiResponse response) {
                            if (activity == null) return;
                            activity.runOnUiThread(() -> {
                                resultView.setText(response.getAnswer().isEmpty() ? "AI未返回文字说明" : response.getAnswer());
                                new AiGraphExecutor(activity.getMindMapView()).execute(response.getCommands());
                                activity.onGraphMutatedByAi();
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);

                                if (response.getCommands() != null && !response.getCommands().isEmpty()) {
                                    Toast.makeText(requireContext(), "AI已执行 " + response.getCommands().size() + " 条图谱操作", Toast.LENGTH_SHORT).show();
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
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
            );
        }));

        return dialog;
    }

    private void styleInput(EditText editText) {
        editText.setTextColor(Color.parseColor("#0F172A"));
        editText.setHintTextColor(Color.parseColor("#94A3B8"));
        editText.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#60A5FA")));
    }
}
