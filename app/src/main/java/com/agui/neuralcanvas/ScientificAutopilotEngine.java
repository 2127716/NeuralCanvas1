package com.agui.neuralcanvas;

import java.util.Map;

public final class ScientificAutopilotEngine {

    private ScientificAutopilotEngine() {}

    public static ScientificTemplateEngine.TemplateResult run(Node baseNode,
                                                              Map<String, Node> nodes,
                                                              Map<String, Connection> connections) {
        ScientificTemplateEngine.TemplateResult out = new ScientificTemplateEngine.TemplateResult();
        if (baseNode == null) return out;

        merge(out, ScientificTemplateEngine.generateAiGapCheck(baseNode, nodes));
        ScientificEnhancementEngine.EnhancementResult enhanced = ScientificEnhancementEngine.enhance(baseNode, nodes, connections);
        merge(out, enhanced.templateResult);

        if (baseNode.isLearningNode()) {
            merge(out, ScientificTemplateEngine.generateConceptDeepening(baseNode, nodes));
            merge(out, ScientificTemplateEngine.generateRetrievalPractice(baseNode, nodes));
            merge(out, ScientificTemplateEngine.generateTransferPractice(baseNode, nodes));
        }

        if (baseNode.isDecisionNode() || baseNode.getType() == Node.NodeType.GOAL || baseNode.getType() == Node.NodeType.PROJECT) {
            merge(out, ScientificTemplateEngine.generateDecisionTree(baseNode, nodes));
            merge(out, ScientificTemplateEngine.generateEvidenceReview(baseNode, nodes));
            merge(out, ScientificTemplateEngine.generatePremortem(baseNode, nodes));
            merge(out, ScientificTemplateEngine.generateWrap(baseNode, nodes));
            merge(out, ScientificTemplateEngine.generateBayesUpdate(baseNode, nodes));
        }

        if (baseNode.isExecutionNode() || baseNode.getType() == Node.NodeType.GOAL || baseNode.getType() == Node.NodeType.PROJECT) {
            merge(out, ScientificTemplateEngine.generateWoop(baseNode, nodes));
            merge(out, ScientificTemplateEngine.generateIfThen(baseNode, nodes));
            merge(out, ScientificTemplateEngine.generateReferenceClassForecast(baseNode, nodes));
            merge(out, ScientificTemplateEngine.generateDailyReview(baseNode, nodes));
        }

        if (baseNode.getType() == Node.NodeType.CONCEPT || baseNode.getType() == Node.NodeType.QUESTION || baseNode.getType() == Node.NodeType.INSIGHT) {
            merge(out, ScientificTemplateEngine.generateDsrpAnalysis(baseNode, nodes));
        }

        return out;
    }

    private static void merge(ScientificTemplateEngine.TemplateResult out, ScientificTemplateEngine.TemplateResult add) {
        if (out == null || add == null) return;
        out.createdNodes.addAll(add.createdNodes);
        out.createdConnections.addAll(add.createdConnections);
    }
}
