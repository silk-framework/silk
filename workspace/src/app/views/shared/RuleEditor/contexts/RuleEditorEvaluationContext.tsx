import React from "react";
import { IRuleOperatorNode, RuleValidationError } from "../RuleEditor.typings";
import { IEvaluatedReferenceLinksScore } from "../../../taskViews/linking/linking.types";
import { NodeContentExtension } from "@eccenca/gui-elements/src/extensions/react-flow";
import { IntentBlueprint as Intent } from "@eccenca/gui-elements/src/common/Intent";

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

    /** When this is set the rule validation during the rule evaluation has failed. */
    ruleValidationError: RuleValidationError | undefined;

    /** Clears the last rule validation error. */
    clearRuleValidationError: () => any;

    /** A notification from the evaluation that will be shown in the notification menu of the rule editor. */
    notifications?: RuleEditorEvaluationNotification[];

    /** Called by the rule editor to give the function to trigger an evaluation to the evaluation component. */
    fetchTriggerEvaluationFunction: (triggerFunction: () => any) => any;

    /** Sets the root node for the evaluation. This is reset by setting it to undefined. */
    setEvaluationRootNode: (nodeId: string | undefined) => void;

    /** Returns the evaluation root node ID. */
    evaluationRootNode: () => string | undefined;

    /** Checks if the sub-tree at the given node type can be evaluated. */
    canBeEvaluated: (nodeType: string | undefined) => boolean;

    ruleType?: "transform" | "linking";
}

export interface RuleEditorEvaluationNotification {
    message: string;
    intent: Intent;
    onDiscard?: () => any;
}

const NOP = () => {};

/** Context of rule editor evaluation component. */
export const RuleEditorEvaluationContext = React.createContext<RuleEditorEvaluationContextProps>({
    supportsEvaluation: false,
    supportsQuickEvaluation: false,
    startEvaluation: NOP,
    createRuleEditorEvaluationComponent: (nodeId) => (
        <NodeContentExtension isExpanded={true}>{`${nodeId}`}</NodeContentExtension>
    ),
    evaluationResultsShown: false,
    evaluationRunning: false,
    toggleEvaluationResults: NOP,
    ruleValidationError: undefined,
    clearRuleValidationError: NOP,
    fetchTriggerEvaluationFunction: NOP,
    setEvaluationRootNode: NOP,
    evaluationRootNode: () => undefined,
    canBeEvaluated: () => false,
});

export interface RuleEditorEvaluationCallbackContextProps {
    /** Allows a sub-component, e.g. an input component of a rule operator, to disable the evaluation error modal. */
    enableErrorModal: (enabled: boolean) => void;
}

export const RuleEditorEvaluationCallbackContext = React.createContext<RuleEditorEvaluationCallbackContextProps>({
    enableErrorModal: NOP
})
