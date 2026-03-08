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

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(18);
        root.setPadding(p, p, p, p);
        scrollView.addView(root);

        TextView tip = new TextView(requireContext());
        tip.setText("把一段知识文本交给AI，自动整理为节点、内容和有方向的连接关系。");
        tip.setTextColor(Color.parseColor("#475569"));
        tip.setTextSize(14);
        root.addView(tip);

        TextView quickTitle = new TextView(requireContext());
        quickTitle.setText("快捷规则");
        quickTitle.setTextColor(Color.parseColor("#0F172A"));
        quickTitle.setTextSize(15);
        addWithTopMargin(root, quickTitle, 12);

        HorizontalScrollView quickScroll = new HorizontalScrollView(requireContext());
        quickScroll.setHorizontalScrollBarEnabled(false);

        LinearLayout quickRow = new LinearLayout(requireContext());
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        quickScroll.addView(quickRow);

        addQuickRule(quickRow, "按因果", "按因果关系建图");
        addQuickRule(quickRow, "按章节", "按章节层级建图");
        addQuickRule(quickRow, "只提重点", "只提炼重点，不要太碎");
        addQuickRule(quickRow, "任务拆解", "按目标-任务-资源关系建图");
        addQuickRule(quickRow, "适度布局", "生成后尽量结构清晰，必要时自动布局");

        addWithTopMargin(root, quickScroll, 8);

        EditText textInput = new EditText(requireContext());
        textInput.setHint("粘贴文本，例如课程知识、读书笔记、项目需求、论文摘要等");
        textInput.setMinLines(10);
        textInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        styleInput(textInput);
        addWithTopMargin(root, textInput, 14);

        EditText extraRuleInput = new EditText(requireContext());
        extraRuleInput.setHint("可选：补充要求，例如“按因果关系建图”“按章节建图”“只提炼重点”");
        styleInput(extraRuleInput);
        addWithTopMargin(root, extraRuleInput, 14);

        TextView resultTitle = new TextView(requireContext());
        resultTitle.setText("导入结果");
        resultTitle.setTextColor(Color.parseColor("#0F172A"));
        resultTitle.setTextSize(15);
        addWithTopMargin(root, resultTitle, 14);

        TextView resultView = new TextView(requireContext());
        resultView.setText("整理结果会显示在这里");
        resultView.setTextColor(Color.parseColor("#0F172A"));
        resultView.setTextSize(14);
        resultView.setBackgroundColor(Color.parseColor("#EEF4FF"));
        resultView.setPadding(dp(14), dp(14), dp(14), dp(14));
        addWithTopMargin(root, resultView, 8);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("知识导入")
                .setView(scrollView)
                .setNegativeButton("关闭", null)
                .setPositiveButton("开始整理", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            positive.setOnClickListener(v -> {
                AiConfig config = dataManager.loadAiConfig();
                if (!config.isEnabled()) {
                    Toast.makeText(requireContext(), "请先在AI助手中完成API配置", Toast.LENGTH_SHORT).show();
                    return;
                }

                String rawText = textInput.getText().toString().trim();
                if (rawText.isEmpty()) {
                    textInput.setError("请输入知识文本");
                    return;
                }

                String extraRule = extraRuleInput.getText().toString().trim();

                String prompt =
                        "请将下面这段文本整理为思维导图/知识网络。\n" +
                        "要求：\n" +
                        "1. 提炼核心主题、关键概念、任务、问题、结论、资源\n" +
                        "2. 生成节点并建立有方向的连接关系\n" +
                        "3. 尽量避免重复节点\n" +
                        "4. 节点标题简洁，内容可适度概括\n" +
                        "5. 优先给出可执行 commands\n" +
                        "6. 如果结构会拥挤，加入自动布局\n" +
                        (extraRule.isEmpty() ? "" : ("7. 额外要求：" + extraRule + "\n")) +
                        "\n原文如下：\n" + rawText;

                resultView.setText("正在让AI整理知识...");
                positive.setEnabled(false);

                AiRepository repository = new AiRepository();
                repository.askGraph(
                        config,
                        activity.getMindMapView().getNodesInternal(),
                        activity.getMindMapView().getConnectionsInternal(),
                        prompt,
                        new AiRepository.AiCallback() {
                            @Override
                            public void onSuccess(AiResponse response) {
                                if (activity == null) return;
                                activity.runOnUiThread(() -> {
                                    resultView.setText(response.getAnswer().isEmpty() ? "知识整理完成" : response.getAnswer());
                                    positive.setEnabled(true);

                                    if (response.getCommands() != null && !response.getCommands().isEmpty()) {
                                        AiCommandPreviewDialog.newInstance(response)
                                                .show(activity.getSupportFragmentManager(), "ai_command_preview");
                                        Toast.makeText(requireContext(), "知识图谱命令已生成，请确认后执行", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(requireContext(), "AI未生成图谱操作", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onError(String message) {
                                if (activity == null) return;
                                activity.runOnUiThread(() -> {
                                    resultView.setText(message);
                                    positive.setEnabled(true);
                                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                );
            });
        });

        bindQuickRules(quickRow, extraRuleInput);

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

    private void addQuickRule(LinearLayout parent, String title, String value) {
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

    private void bindQuickRules(LinearLayout quickRow, EditText extraRuleInput) {
        for (int i = 0; i < quickRow.getChildCount(); i++) {
            android.view.View child = quickRow.getChildAt(i);
            if (child instanceof Button) {
                child.setOnClickListener(v -> extraRuleInput.setText(String.valueOf(v.getTag())));
            }
        }
    }
}
