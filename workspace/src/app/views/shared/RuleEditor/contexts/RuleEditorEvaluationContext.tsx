import React from "react";
import { IRuleOperatorNode } from "../RuleEditor.typings";
import { IEvaluatedReferenceLinksScore } from "../../../taskViews/linking/linking.types";

export interface RuleEditorEvaluationContextProps {
    /** If evaluation is supported. */
    supportsEvaluation: boolean;

    /** If supported then the evaluation can be executed more often, e.g. after every change of the linking rule. */
    supportsQuickEvaluation: boolean;

    /** Start evaluation. The evaluation component that created this context is expected to handle the result presentation all by itself. */
    startEvaluation: (ruleOperatorNodes: IRuleOperatorNode[], originalTask: any) => void;

    /** Creates the evaluation component for a single operator node. */
    createRuleEditorEvaluationComponent: (ruleOperatorId: string) => JSX.Element;

    /** If the evaluation is currently running. */
    evaluationRunning: boolean;

    /** Show or hide all evaluation results. */
    toggleEvaluationResults: (show: boolean) => any;

    /** The evaluation score. */
    evaluationScore?: IEvaluatedReferenceLinksScore;
}

const NOP = () => {};

/** Context of rule editor evaluation component. */
export const RuleEditorEvaluationContext = React.createContext<RuleEditorEvaluationContextProps>({
    supportsEvaluation: false,
    supportsQuickEvaluation: false,
    startEvaluation: NOP,
    createRuleEditorEvaluationComponent: (nodeId) => <div>{`${nodeId}`}</div>,
    evaluationRunning: false,
    toggleEvaluationResults: NOP,
});
