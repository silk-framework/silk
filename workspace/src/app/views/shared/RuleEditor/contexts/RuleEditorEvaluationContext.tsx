import React from "react";
import { IRuleOperatorNode } from "../RuleEditor.typings";
import { IEvaluatedReferenceLinksScore } from "../../../taskViews/linking/linking.types";

export interface RuleEditorEvaluationContextProps {
    /** If evaluation is supported. */
    supportsEvaluation: boolean;

    /** If supported then the evaluation can be executed more often, e.g. after every change of the linking rule. */
    supportsQuickEvaluation: boolean;

    /** Start evaluation. The evaluation component that created this context is expected to handle the result presentation all by itself.
     *
     * @param ruleOperatorNodes   The rule operator nodes used for the evaluation.
     * @param originalTask        The original linking task.
     * @param quickEvaluationOnly If set to true then only a quick evaluation is tried, the slower evaluation method is skipped if there were no results.
     */
    startEvaluation: (ruleOperatorNodes: IRuleOperatorNode[], originalTask: any, quickEvaluationOnly: boolean) => void;

    /** Creates the evaluation component for a single operator node. */
    createRuleEditorEvaluationComponent: (ruleOperatorId: string) => JSX.Element;

    /** If the evaluation is currently running. */
    evaluationRunning: boolean;

    /** Show or hide all evaluation results. */
    toggleEvaluationResults: (show: boolean) => any;

    /** If the evaluation results are currently shown. */
    evaluationResultsShown: boolean;

    /** The evaluation score. */
    evaluationScore?: IEvaluatedReferenceLinksScore;

    /** Link to external reference links UI. */
    referenceLinksUrl?: string;
}

const NOP = () => {};

/** Context of rule editor evaluation component. */
export const RuleEditorEvaluationContext = React.createContext<RuleEditorEvaluationContextProps>({
    supportsEvaluation: false,
    supportsQuickEvaluation: false,
    startEvaluation: NOP,
    createRuleEditorEvaluationComponent: (nodeId) => <div>{`${nodeId}`}</div>,
    evaluationResultsShown: false,
    evaluationRunning: false,
    toggleEvaluationResults: NOP,
});
