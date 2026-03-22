package com.agui.neuralcanvas;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
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

import java.util.ArrayList;

public class KnowledgeImportDialog extends DialogFragment {

    private static final int REQ_PICK_DOCS = 3101;
    private static final int REQ_PICK_IMAGES = 3102;

    private final ArrayList<Uri> selectedUris = new ArrayList<>();
    private TextView selectedFilesView;
    private EditText textInput;
    private EditText extraRuleInput;
    private TextView resultView;

    public static KnowledgeImportDialog newInstance() {
        return new KnowledgeImportDialog();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                requireContext().getResources().getDisplayMetrics());
    }

    private View card(String title, String subtitle, View... body) {
        LinearLayout box = MonetDialogStyler.card(requireContext(), title, subtitle);
        for (View child : body) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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

        LinearLayout root = MonetDialogStyler.buildRoot(requireContext());
        scrollView.addView(root);

        TextView header = new TextView(requireContext());
        header.setText("知识导入中心");
        root.addView(header);

        TextView desc = new TextView(requireContext());
        desc.setText("文本、图片 OCR、PDF、DOCX、实时语音统一入口。后台处理默认生成待确认改动。");
        LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descLp.topMargin = dp(6);
        root.addView(desc, descLp);
        MonetDialogStyler.styleHeader(header, desc);

        textInput = new EditText(requireContext());
        textInput.setHint("粘贴文本、课堂笔记、项目需求、论文摘要，或把语音结果插进来……");
        textInput.setMinLines(8);
        textInput.setTextColor(ThemeManager.getTextPrimary());
        textInput.setHintTextColor(ThemeManager.getTextSecondary());
        textInput.setBackgroundTintList(ColorStateList.valueOf(ThemeManager.getAccent()));

        extraRuleInput = new EditText(requireContext());
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
        quickRow.addView(b1); quickRow.addView(b2); quickRow.addView(b3); quickRow.addView(b4);
        View.OnClickListener fillRule = v -> extraRuleInput.setText(((Button) v).getText().toString());
        b1.setOnClickListener(fillRule); b2.setOnClickListener(fillRule); b3.setOnClickListener(fillRule); b4.setOnClickListener(fillRule);

        resultView = new TextView(requireContext());
        resultView.setText("整理结果会显示在这里");
        resultView.setTextColor(ThemeManager.getTextPrimary());
        resultView.setTextSize(14);
        resultView.setPadding(dp(12), dp(12), dp(12), dp(12));
        resultView.setBackground(MonetDialogStyler.cardBg());

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = dp(14);
        root.addView(card("文本导入", "前台整理会立刻给你预览；后台处理会生成待确认改动", textInput, extraRuleInput, quickRow, resultView), cardLp);

        Button docBtn = smallButton("选择文档");
        docBtn.setOnClickListener(v -> pickDocuments());

        Button imageBtn = smallButton("选择图片 / OCR");
        imageBtn.setOnClickListener(v -> pickImages());

        Button voiceBtn = smallButton("实时语音");
        voiceBtn.setOnClickListener(v ->
                SherpaVoiceInputDialog.newInstance(text -> appendTranscript(text))
                        .show(activity.getSupportFragmentManager(), "sherpa_voice_input_dialog"));

        Button clearBtn = smallButton("清空已选");
        clearBtn.setOnClickListener(v -> {
            selectedUris.clear();
            refreshSelectedSummary();
        });

        selectedFilesView = new TextView(requireContext());
        selectedFilesView.setTextColor(ThemeManager.getTextSecondary());
        selectedFilesView.setTextSize(13);
        selectedFilesView.setText("当前未选择文件");
        refreshSelectedSummary();

        TextView ext = MonetDialogStyler.body(requireContext(),
                "支持：TXT / MD / PDF / DOCX / 图片 OCR / 中文实时语音。Sherpa-ONNX 走本地离线识别。");
        root.addView(card("文档 / OCR / 语音", "现在可以直接口述成文字，再并入知识导入", docBtn, imageBtn, voiceBtn, clearBtn, selectedFilesView, ext), cardLp);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(scrollView)
                .setNegativeButton("关闭", null)
                .setNeutralButton("后台处理", null)
                .setPositiveButton("前台整理", null)
                .create();

        dialog.setOnShowListener(d -> {
            MonetDialogStyler.apply(dialog, requireContext());
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button neutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

            positive.setOnClickListener(v -> {
                AiConfig config = dataManager.loadAiConfig();
                if (!config.isEnabled()) {
                    Toast.makeText(requireContext(), "请先在 AI 助手中完成 API 配置", Toast.LENGTH_SHORT).show();
                    return;
                }

                String rawText = safe(textInput.getText().toString());
                if (rawText.isEmpty() && selectedUris.isEmpty()) {
                    textInput.setError("请输入文本或选择文件/图片");
                    return;
                }

                resultView.setText("正在整理中…");
                positive.setEnabled(false);

                new Thread(() -> {
                    try {
                        DocumentImportPipeline.ImportResult[] imported = new DocumentImportPipeline.ImportResult[selectedUris.size()];
                        for (int i = 0; i < selectedUris.size(); i++) {
                            imported[i] = DocumentImportPipeline.importUri(requireContext().getApplicationContext(), selectedUris.get(i));
                        }

                        String mergedText = DocumentImportPipeline.mergeForAi(rawText, imported);
                        String extraRule = safe(extraRuleInput.getText().toString());
                        String prompt = "请将下面导入内容整理为思维导图/知识网络，节点标题简洁，建立高价值有方向连接，尽量不要重排旧节点。"
                                + (extraRule.isEmpty() ? "" : ("额外要求：" + extraRule + "。"))
                                + "\n\n导入内容：\n" + mergedText;

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
                    } catch (Exception e) {
                        if (activity == null) return;
                        activity.runOnUiThread(() -> {
                            resultView.setText("导入失败：" + safe(e.getMessage()));
                            positive.setEnabled(true);
                        });
                    }
                }).start();
            });

            neutral.setOnClickListener(v -> {
                AiConfig config = dataManager.loadAiConfig();
                if (!config.isEnabled()) {
                    Toast.makeText(requireContext(), "请先在 AI 助手中完成 API 配置", Toast.LENGTH_SHORT).show();
                    return;
                }
                String rawText = safe(textInput.getText().toString());
                if (rawText.isEmpty() && selectedUris.isEmpty()) {
                    textInput.setError("请输入文本或选择文件/图片");
                    return;
                }
                String[] uriStrings = new String[selectedUris.size()];
                for (int i = 0; i < selectedUris.size(); i++) uriStrings[i] = selectedUris.get(i).toString();
                KnowledgeImportJobManager.enqueue(requireContext(), rawText, safe(extraRuleInput.getText().toString()), uriStrings);
                Toast.makeText(requireContext(), "已转入后台处理。处理完成后会给你更稳定的待确认改动。", Toast.LENGTH_LONG).show();
                dismiss();
            });
        });
        return dialog;
    }

    private void appendTranscript(String text) {
        String incoming = safe(text);
        if (incoming.isEmpty() || textInput == null) return;
        String old = safe(textInput.getText() == null ? "" : textInput.getText().toString());
        String merged = old.isEmpty() ? incoming : (old + "\n" + incoming);
        textInput.setText(merged);
        textInput.setSelection(merged.length());
    }

    private void pickDocuments() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                "text/plain",
                "text/*",
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/msword"
        });
        startActivityForResult(intent, REQ_PICK_DOCS);
    }

    private void pickImages() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQ_PICK_IMAGES);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != android.app.Activity.RESULT_OK || data == null) return;

        final int takeFlags = data.getFlags()
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                addUriWithPermission(uri, takeFlags);
            }
        } else if (data.getData() != null) {
            addUriWithPermission(data.getData(), takeFlags);
        }
        refreshSelectedSummary();
    }

    private void addUriWithPermission(Uri uri, int takeFlags) {
        if (uri == null) return;
        try {
            requireContext().getContentResolver().takePersistableUriPermission(uri, takeFlags);
        } catch (Exception ignored) {}
        if (!selectedUris.contains(uri)) selectedUris.add(uri);
    }

    private void refreshSelectedSummary() {
        if (selectedFilesView == null) return;
        if (selectedUris.isEmpty()) {
            selectedFilesView.setText("当前未选择文件");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("已选择 ").append(selectedUris.size()).append(" 个文件：");
        int limit = Math.min(5, selectedUris.size());
        for (int i = 0; i < limit; i++) {
            sb.append("\n- ").append(selectedUris.get(i).getLastPathSegment());
        }
        if (selectedUris.size() > limit) {
            sb.append("\n…其余 ").append(selectedUris.size() - limit).append(" 个");
        }
        selectedFilesView.setText(sb.toString());
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
