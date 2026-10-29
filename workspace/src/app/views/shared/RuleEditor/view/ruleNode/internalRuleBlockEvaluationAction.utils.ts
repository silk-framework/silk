import { IRuleOperatorNode } from "../../RuleEditor.typings";

export interface InternalRuleBlockEvaluationActionState {
    visible: boolean;
    enabled: boolean;
}

/** Computes the action state for opening the internal evaluation of a reusable rule block usage. */
export const internalRuleBlockEvaluationActionState = (
    currentRuleNode: IRuleOperatorNode | undefined,
    evaluationResultsShown: boolean,
    canEvaluateRuleBlock?: (nodeId: string, ruleBlockId: string) => boolean,
): InternalRuleBlockEvaluationActionState => {
    const isRuleBlockUsage = currentRuleNode?.pluginType === "RuleBlock";
    const visible = !!isRuleBlockUsage && evaluationResultsShown;
    const enabled = !!(
        visible &&
        currentRuleNode &&
        canEvaluateRuleBlock?.(currentRuleNode.nodeId, currentRuleNode.pluginId)
    );
    return {
        visible,
        enabled,
    };
};
