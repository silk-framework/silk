import React from "react";
import { IconButton } from "@eccenca/gui-elements";
import { RuleEditorEvaluationContext } from "../../contexts/RuleEditorEvaluationContext";
import { RuleEditorModelContext } from "../../contexts/RuleEditorModelContext";
import { internalRuleBlockEvaluationActionState } from "./internalRuleBlockEvaluationAction.utils";

interface InternalRuleBlockEvaluationButtonProps {
    nodeId: string;
    t: (translationKey: string, defaultValue?: string) => string;
}

/** Action button that opens the internal evaluation of a reusable rule block usage. */
export const InternalRuleBlockEvaluationButton = ({ nodeId, t }: InternalRuleBlockEvaluationButtonProps) => {
    const ruleEvaluationContext = React.useContext(RuleEditorEvaluationContext);
    const modelContext = React.useContext(RuleEditorModelContext);
    const currentRuleNode = modelContext.ruleOperatorNodes().find((node) => node.nodeId === nodeId);
    const actionState = internalRuleBlockEvaluationActionState(
        currentRuleNode,
        ruleEvaluationContext.evaluationResultsShown,
        ruleEvaluationContext.canEvaluateRuleBlock,
    );

    if (!actionState.visible || !currentRuleNode) {
        return null;
    }

    return (
        <IconButton
            data-test-id="rule-node-open-internal-rule-block-evaluation-icon-btn"
            name="item-viewdetails"
            disabled={!actionState.enabled}
            text={t("RuleEditor.node.action.openInternalRuleBlockEvaluation.tooltip", "Show internal evaluation")}
            onClick={() =>
                ruleEvaluationContext.openInternalRuleBlockEvaluation?.(
                    nodeId,
                    currentRuleNode.pluginId,
                    currentRuleNode.label,
                )
            }
        />
    );
};
