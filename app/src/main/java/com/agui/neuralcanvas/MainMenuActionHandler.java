package com.agui.neuralcanvas;

public class MainMenuActionHandler {

    public static boolean handle(MainActivity activity, int id) {
        if (activity == null) return false;

        switch (id) {
            case MoreMenuDialog.ID_WORKSPACE_NEXT:
                activity.openNextStepCoach();
                return true;
            case MoreMenuDialog.ID_WORKSPACE_DASHBOARD:
                activity.openScientificDashboard();
                return true;
            case MoreMenuDialog.ID_WORKSPACE_PROJECTS:
                activity.openProjectsHubWorkflowView();
                return true;
            case MoreMenuDialog.ID_WORKSPACE_INBOX:
                activity.openInboxClarifier();
                return true;
            case MoreMenuDialog.ID_WORKSPACE_WEEKLY:
                activity.openWeeklyReview();
                return true;
            case MoreMenuDialog.ID_WORKSPACE_MEMORY:
                activity.openMemoryReview();
                return true;
            case MoreMenuDialog.ID_WORKSPACE_FOCUS:
                activity.openFocusSession();
                return true;
            case MoreMenuDialog.ID_WORKSPACE_GRAPH:
                activity.openGraphInsights();
                return true;

            case MoreMenuDialog.ID_TEMPLATE_WOOP:
                activity.generateScientificTemplate(ScientificTemplateEngine.TemplateType.WOOP);
                return true;
            case MoreMenuDialog.ID_TEMPLATE_IF_THEN:
                activity.generateScientificTemplate(ScientificTemplateEngine.TemplateType.IF_THEN);
                return true;
            case MoreMenuDialog.ID_TEMPLATE_WEEKLY_REVIEW:
                activity.generateScientificTemplate(ScientificTemplateEngine.TemplateType.WEEKLY_REVIEW);
                return true;
            case MoreMenuDialog.ID_TEMPLATE_PREMORTEM:
                activity.generateScientificTemplate(ScientificTemplateEngine.TemplateType.PREMORTEM);
                return true;
            case MoreMenuDialog.ID_TEMPLATE_WRAP:
                activity.generateScientificTemplate(ScientificTemplateEngine.TemplateType.WRAP);
                return true;
            case MoreMenuDialog.ID_TEMPLATE_BAYES:
                activity.generateScientificTemplate(ScientificTemplateEngine.TemplateType.BAYES_UPDATE);
                return true;
            case MoreMenuDialog.ID_TEMPLATE_DSRP:
                activity.generateScientificTemplate(ScientificTemplateEngine.TemplateType.DSRP_ANALYSIS);
                return true;
            case MoreMenuDialog.ID_TEMPLATE_REFERENCE:
                activity.generateScientificTemplate(ScientificTemplateEngine.TemplateType.REFERENCE_CLASS_FORECAST);
                return true;
            case MoreMenuDialog.ID_TEMPLATE_RETRIEVAL:
                activity.generateScientificTemplate(ScientificTemplateEngine.TemplateType.RETRIEVAL_PRACTICE);
                return true;
            case MoreMenuDialog.ID_TEMPLATE_TRANSFER:
                activity.generateScientificTemplate(ScientificTemplateEngine.TemplateType.TRANSFER_PRACTICE);
                return true;

            case MoreMenuDialog.ID_AI_ENHANCE:
                activity.runScientificEnhancement();
                return true;
            case MoreMenuDialog.ID_AI_AUTOPILOT:
                activity.runScientificAutopilot();
                return true;
            case MoreMenuDialog.ID_AI_GAP:
                activity.runAiGapCheck();
                return true;
            case MoreMenuDialog.ID_AI_EXECUTION:
                activity.runAiExecutionPatch();
                return true;
            case MoreMenuDialog.ID_AI_LEARNING:
                activity.runAiLearningPatch();
                return true;
            case MoreMenuDialog.ID_AI_ASSISTANT:
                activity.showAiAssistantDialog();
                return true;

            case MoreMenuDialog.ID_IMPORT_KNOWLEDGE:
                activity.showKnowledgeImportDialog();
                return true;
            case MoreMenuDialog.ID_BOX_SELECT:
                activity.getMindMapView().startBoxSelectionMode();
                return true;
            case MoreMenuDialog.ID_DELETE_SELECTED:
                int count = activity.getMindMapView().deleteSelectedNodes();
                android.widget.Toast.makeText(activity, count > 0 ? "已删除 " + count + " 个节点" : "当前没有选中节点", android.widget.Toast.LENGTH_SHORT).show();
                return true;
            case MoreMenuDialog.ID_CANCEL_BOX_SELECT:
                activity.getMindMapView().cancelBoxSelectionMode();
                return true;
            case MoreMenuDialog.ID_SYSTEM_THEME:
                activity.showThemePicker();
                return true;
            case MoreMenuDialog.ID_SYSTEM_HELP:
                activity.showHelpDialog();
                return true;
            case MoreMenuDialog.ID_SYSTEM_CLEAR:
                activity.confirmClearAll();
                return true;
            default:
                return false;
        }
    }
}
