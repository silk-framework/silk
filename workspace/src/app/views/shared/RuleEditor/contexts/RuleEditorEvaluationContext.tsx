import React from "react";
import { IRuleOperatorNode } from "../RuleEditor.typings";

export interface RuleEditorEvaluationContextProps {
    /** If evaluation is supported. */
    supportsEvaluation: boolean;

    /** Start evaluation. The evaluation component that created this context is expected to handle the result presentation all by itself. */
    startEvaluation: (ruleOperatorNodes: IRuleOperatorNode[], originalTask: any) => void;

    /** Creates the evaluation component for a single operator node. */
    createRuleEditorEvaluationComponent: (ruleOperatorId: string) => JSX.Element;

    /** If the evaluation is currently running. */
    evaluationRunning: boolean;

    /** Show or hide all evaluation results. */
    toggleEvaluationResults: (show: boolean) => any;
}

const NOP = () => {};

/** Context of rule editor evaluation component. */
export const RuleEditorEvaluationContext = React.createContext<RuleEditorEvaluationContextProps>({
    supportsEvaluation: false,
    startEvaluation: NOP,
    createRuleEditorEvaluationComponent: (nodeId) => <div>{`${nodeId}`}</div>,
    evaluationRunning: false,
    toggleEvaluationResults: NOP,
});
