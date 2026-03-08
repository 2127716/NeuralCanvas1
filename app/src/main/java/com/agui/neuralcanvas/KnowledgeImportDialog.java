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
        int p = dp(20);
        root.setPadding(p, p, p, p);
        scrollView.addView(root);

        TextView tip = new TextView(requireContext());
        tip.setText("把一段知识文段交给AI，自动整理为节点与连接关系。建议输入有结构的段落。");
        tip.setTextColor(Color.parseColor("#475569"));
        tip.setTextSize(14);
        root.addView(tip);

        EditText textInput = new EditText(requireContext());
        textInput.setHint("粘贴文本，例如课程知识、读书笔记、项目需求、论文摘要等");
        textInput.setMinLines(10);
        textInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        textInput.setTextColor(Color.parseColor("#0F172A"));
        textInput.setHintTextColor(Color.parseColor("#94A3B8"));
        textInput.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#60A5FA")));
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        inputLp.topMargin = dp(16);
        root.addView(textInput, inputLp);

        EditText extraRuleInput = new EditText(requireContext());
        extraRuleInput.setHint("可选：补充要求，例如“按因果关系建图”“按章节建图”“只提炼重点”");
        extraRuleInput.setText("");
        extraRuleInput.setTextColor(Color.parseColor("#0F172A"));
        extraRuleInput.setHintTextColor(Color.parseColor("#94A3B8"));
        extraRuleInput.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#60A5FA")));
        LinearLayout.LayoutParams ruleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        ruleLp.topMargin = dp(16);
        root.addView(extraRuleInput, ruleLp);

        TextView resultView = new TextView(requireContext());
        resultView.setText("导入结果会显示在这里");
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
                .setTitle("知识导入")
                .setView(scrollView)
                .setNegativeButton("关闭", null)
                .setPositiveButton("开始整理", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
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
                    "5. 直接给出可执行commands\n" +
                    (extraRule.isEmpty() ? "" : ("6. 额外要求：" + extraRule + "\n")) +
                    "\n原文如下：\n" + rawText;

            resultView.setText("正在让AI整理知识...");
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

            AiRepository repository = new AiRepository();
            repository.askGraph(
                    config,
                    activity.getMindMapView().getNodes(),
                    activity.getMindMapView().getConnections(),
                    prompt,
                    new AiRepository.AiCallback() {
                        @Override
                        public void onSuccess(AiResponse response) {
                            if (activity == null) return;
                            activity.runOnUiThread(() -> {
                                resultView.setText(response.getAnswer().isEmpty() ? "知识整理完成" : response.getAnswer());
                                new AiGraphExecutor(activity.getMindMapView()).execute(response.getCommands());
                                activity.onGraphMutatedByAi();
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);

                                if (response.getCommands() != null && !response.getCommands().isEmpty()) {
                                    Toast.makeText(requireContext(), "知识已导入图谱", Toast.LENGTH_SHORT).show();
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
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
            );
        }));

        return dialog;
    }
}
