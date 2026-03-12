package com.agui.neuralcanvas;

public class MainMenuActionHandler {

    public static boolean handle(MainActivity activity, int id) {
        if (activity == null) return false;

        if (id == R.id.action_add_node) {
            activity.showAddNodeDialog();
            return true;
        }

        if (id == R.id.action_search) {
            activity.showSearchDialog();
            return true;
        }

        if (id == R.id.action_dashboard) {
            ScientificDashboardDialog.newInstance()
                    .show(activity.getSupportFragmentManager(), "scientific_dashboard");
            return true;
        }

        if (id == R.id.action_projects_hub) {
            ProjectsHubDialog.newInstance()
                    .show(activity.getSupportFragmentManager(), "projects_hub");
            return true;
        }

        if (id == R.id.action_generate_woop) {
            activity.generateScientificTemplate(ScientificTemplateEngine.TemplateType.WOOP);
            return true;
        }

        if (id == R.id.action_generate_if_then) {
            activity.generateScientificTemplate(ScientificTemplateEngine.TemplateType.IF_THEN);
            return true;
        }

        if (id == R.id.action_generate_daily_review) {
            activity.generateScientificTemplate(ScientificTemplateEngine.TemplateType.DAILY_REVIEW);
            return true;
        }

        if (id == R.id.action_generate_weekly_review) {
            activity.generateScientificTemplate(ScientificTemplateEngine.TemplateType.WEEKLY_REVIEW);
            return true;
        }

        if (id == R.id.action_generate_aar) {
            activity.generateScientificTemplate(ScientificTemplateEngine.TemplateType.AAR);
            return true;
        }

        if (id == R.id.action_generate_decision_tree) {
            activity.generateScientificTemplate(ScientificTemplateEngine.TemplateType.DECISION_TREE);
            return true;
        }

        if (id == R.id.action_generate_premortem) {
            activity.generateScientificTemplate(ScientificTemplateEngine.TemplateType.PREMORTEM);
            return true;
        }

        if (id == R.id.action_generate_evidence_review) {
            activity.generateScientificTemplate(ScientificTemplateEngine.TemplateType.EVIDENCE_REVIEW);
            return true;
        }

        if (id == R.id.action_generate_retrieval_practice) {
            activity.generateScientificTemplate(ScientificTemplateEngine.TemplateType.RETRIEVAL_PRACTICE);
            return true;
        }

        if (id == R.id.action_generate_concept_deepening) {
            activity.generateScientificTemplate(ScientificTemplateEngine.TemplateType.CONCEPT_DEEPENING);
            return true;
        }

        if (id == R.id.action_generate_transfer_practice) {
            activity.generateScientificTemplate(ScientificTemplateEngine.TemplateType.TRANSFER_PRACTICE);
            return true;
        }

        if (id == R.id.action_ai_gap_check) {
            activity.runAiGapCheck();
            return true;
        }

        if (id == R.id.action_ai_execution_patch) {
            activity.runAiExecutionPatch();
            return true;
        }

        if (id == R.id.action_ai_learning_patch) {
            activity.runAiLearningPatch();
            return true;
        }

        if (id == R.id.action_ai_assistant) {
            activity.showAiAssistantDialog();
            return true;
        }

        if (id == R.id.action_import_knowledge) {
            activity.showKnowledgeImportDialog();
            return true;
        }

        if (id == R.id.action_clear_all) {
            activity.confirmClearAll();
            return true;
        }

        if (id == R.id.action_help) {
            activity.showHelpDialog();
            return true;
        }

        return false;
    }
}
