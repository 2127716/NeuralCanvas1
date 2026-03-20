package com.agui.neuralcanvas;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class KnowledgeImportDialog extends DialogFragment {

    public static KnowledgeImportDialog newInstance() {
        return new KnowledgeImportDialog();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                requireContext().getResources().getDisplayMetrics());
    }

    private View card(String title, String subtitle, View... body) {
        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(ThemeManager.getChipBg());
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), ThemeManager.getChipStroke());
        box.setBackground(bg);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(title);
        tvTitle.setTextColor(ThemeManager.getTextPrimary());
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(tvTitle.getTypeface(), android.graphics.Typeface.BOLD);
        box.addView(tvTitle);

        TextView tvSub = new TextView(requireContext());
        tvSub.setText(subtitle);
        tvSub.setTextColor(ThemeManager.getTextSecondary());
        tvSub.setTextSize(13);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(6);
        box.addView(tvSub, subLp);

        for (View child : body) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(12);
            box.addView(child, lp);
        }
        return box;
    }

    private Button smallButton(String text) {
        Button button = new Button(requireContext());
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(ThemeManager.getTextPrimary());
        button.setBackgroundTintList(ColorStateList.valueOf(ThemeManager.getSpinnerBg()));
        return button;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (!(getActivity() instanceof MainActivity)) return super.onCreateDialog(savedInstanceState);
        MainActivity activity = (MainActivity) getActivity();
        SimpleDataManager dataManager = activity.getDataManager();

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(ThemeManager.getDialogBg());

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        scrollView.addView(root);

        TextView header = new TextView(requireContext());
        header.setText("知识导入中心");
        header.setTextColor(ThemeManager.getTextPrimary());
        header.setTextSize(20);
        header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(header);

        TextView desc = new TextView(requireContext());
        desc.setText("把原来的单一文本导入，升级成统一入口。文本导入可直接使用；文档、OCR、语音入口已留好位置，方便你后续接 GitHub 开源能力。 ");
        desc.setTextColor(ThemeManager.getTextSecondary());
        desc.setTextSize(13);
        LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descLp.topMargin = dp(6);
        root.addView(desc, descLp);

        EditText textInput = new EditText(requireContext());
        textInput.setHint("粘贴文本、课堂笔记、项目需求、论文摘要……");
        textInput.setMinLines(8);
        textInput.setTextColor(ThemeManager.getTextPrimary());
        textInput.setHintTextColor(ThemeManager.getTextSecondary());
        textInput.setBackgroundTintList(ColorStateList.valueOf(ThemeManager.getAccent()));

        EditText extraRuleInput = new EditText(requireContext());
        extraRuleInput.setHint("额外要求：例如按章节、按因果、只提重点、不要乱排旧节点");
        extraRuleInput.setTextColor(ThemeManager.getTextPrimary());
        extraRuleInput.setHintTextColor(ThemeManager.getTextSecondary());
        extraRuleInput.setBackgroundTintList(ColorStateList.valueOf(ThemeManager.getAccent()));

        LinearLayout quickRow = new LinearLayout(requireContext());
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        Button b1 = smallButton("按因果");
        Button b2 = smallButton("按章节");
        Button b3 = smallButton("只提重点");
        Button b4 = smallButton("任务拆解");
        Button b5 = smallButton("保守加线");
        quickRow.addView(b1); quickRow.addView(b2); quickRow.addView(b3); quickRow.addView(b4); quickRow.addView(b5);
        View.OnClickListener fillRule = v -> extraRuleInput.setText(((Button) v).getText().toString());
        b1.setOnClickListener(fillRule); b2.setOnClickListener(fillRule); b3.setOnClickListener(fillRule); b4.setOnClickListener(fillRule); b5.setOnClickListener(fillRule);

        TextView resultView = new TextView(requireContext());
        resultView.setText("整理结果会显示在这里");
        resultView.setTextColor(ThemeManager.getTextPrimary());
        resultView.setTextSize(14);
        resultView.setPadding(dp(12), dp(12), dp(12), dp(12));
        resultView.setBackgroundColor(Color.parseColor("#162033"));

        root.addView(card("文本导入", "这是现在就能直接用的主入口", textInput, extraRuleInput, quickRow, resultView), descLp);

        TextView ext = new TextView(requireContext());
        ext.setText("建议接入的开源能力（未在本补丁里直接落依赖，原因是当前环境无法联网替你校验最新版兼容性）：\n• 语音转文字：Android SpeechRecognizer / Whisper.cpp Android 封装\n• OCR：ML Kit Text Recognition 或 Tesseract Android\n• 文档导入：SAF 文档选择器 + PDF/Docx 解析层\n• PDF 文本提取：PdfRenderer + 外部解析器\n这些入口已经在这个对话框里给你分类留位。 ");
        ext.setTextColor(ThemeManager.getTextSecondary());
        ext.setTextSize(13);

        Button docBtn = smallButton("文档导入入口");
        docBtn.setOnClickListener(v -> Toast.makeText(requireContext(), "这里建议接入系统文档选择器 + PDF/DOCX 解析层", Toast.LENGTH_SHORT).show());
        Button ocrBtn = smallButton("OCR 入口");
        ocrBtn.setOnClickListener(v -> Toast.makeText(requireContext(), "这里建议接入 OCR 模块，拍照或选图后抽文字", Toast.LENGTH_SHORT).show());
        Button voiceBtn = smallButton("语音转文字入口");
        voiceBtn.setOnClickListener(v -> Toast.makeText(requireContext(), "这里建议接入 SpeechRecognizer 或离线语音识别模块", Toast.LENGTH_SHORT).show());
        root.addView(card("扩展导入", "先把入口分清，后续接能力时不会再把菜单做乱", docBtn, ocrBtn, voiceBtn, ext), descLp);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(scrollView)
                .setNegativeButton("关闭", null)
                .setPositiveButton("开始整理", null)
                .create();

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                bg.setColor(ThemeManager.getDialogBg());
                bg.setCornerRadius(dp(26));
                bg.setStroke(dp(1), ThemeManager.getStroke());
                dialog.getWindow().setBackgroundDrawable(bg);
            }
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                AiConfig config = dataManager.loadAiConfig();
                if (!config.isEnabled()) {
                    Toast.makeText(requireContext(), "请先在 AI 助手中完成 API 配置", Toast.LENGTH_SHORT).show();
                    return;
                }
                String rawText = textInput.getText().toString().trim();
                if (rawText.isEmpty()) {
                    textInput.setError("请输入要导入的文本");
                    return;
                }
                String extraRule = extraRuleInput.getText().toString().trim();
                String prompt = "请将下面文本整理为思维导图/知识网络，节点标题简洁，建立高价值有方向连接，尽量不要重排旧节点。"
                        + (extraRule.isEmpty() ? "" : ("额外要求：" + extraRule + "。"))
                        + "\n\n原文：\n" + rawText;

                resultView.setText("正在整理中…");
                positive.setEnabled(false);
                AiRepository repository = new AiRepository();
                AiRepository.PreparedRequest prepared = repository.prepareRelevantRequest(
                        activity.getMindMapView().getNodesInternal(),
                        activity.getMindMapView().getConnectionsInternal(),
                        prompt,
                        true
                );
                repository.askGraph(config, prepared.snapshot, prompt, false, new AiRepository.AiCallback() {
                    @Override
                    public void onSuccess(AiResponse response) {
                        if (activity == null) return;
                        activity.runOnUiThread(() -> {
                            resultView.setText(response.getAnswer().isEmpty() ? "知识整理完成" : response.getAnswer());
                            positive.setEnabled(true);
                            if (response.getCommands() != null && !response.getCommands().isEmpty()) {
                                AiCommandPreviewDialog.newInstance(response).show(activity.getSupportFragmentManager(), "ai_command_preview");
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        if (activity == null) return;
                        activity.runOnUiThread(() -> {
                            resultView.setText(message);
                            positive.setEnabled(true);
                        });
                    }
                });
            });
        });
        return dialog;
    }
}
