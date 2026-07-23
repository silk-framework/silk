import type { IRuleOperatorNode } from "../views/shared/RuleEditor/RuleEditor.typings";
import type { RuleEditorUiContextProps } from "../views/shared/RuleEditor/contexts/RuleEditorUiContext";
import type { RuleEditorEvaluationContextProps } from "../views/shared/RuleEditor/contexts/RuleEditorEvaluationContext";
import type { RuleEditorContextProps } from "../views/shared/RuleEditor/contexts/RuleEditorContext";
import type { RuleEditorModelContextProps } from "../views/shared/RuleEditor/contexts/RuleEditorModelContext";
import { ruleEditorUiContextDefaultValue } from "../views/shared/RuleEditor/contexts/RuleEditorUiContext";
import { ruleEditorEvaluationContextDefaultValue } from "../views/shared/RuleEditor/contexts/RuleEditorEvaluationContext";
import { ruleEditorContextDefaultValue } from "../views/shared/RuleEditor/contexts/RuleEditorContext";
import {
    ruleEditorModelActionsDefaultValue,
    ruleEditorModelContextDefaultValue,
} from "../views/shared/RuleEditor/contexts/RuleEditorModelContext";

const noop = () => {};

export const createRuleEditorUiContextValue = (
    overrides: Partial<RuleEditorUiContextProps> = {},
): RuleEditorUiContextProps => ({
    ...ruleEditorUiContextDefaultValue,
    ...overrides,
});

export const createRuleEditorEvaluationContextValue = (
    overrides: Partial<RuleEditorEvaluationContextProps> = {},
): RuleEditorEvaluationContextProps => ({
    ...ruleEditorEvaluationContextDefaultValue,
    ...overrides,
});

export const createRuleEditorContextValue = (
    overrides: Partial<RuleEditorContextProps> = {},
): RuleEditorContextProps => ({
    ...ruleEditorContextDefaultValue,
    saveRule: () => ({ success: true }),
    convertRuleOperatorToRuleNode: () => {
        throw new Error("Not implemented in test.");
    },
    ...overrides,
});

export const createRuleEditorModelContextValue = (
    overrides: Partial<RuleEditorModelContextProps> = {},
): RuleEditorModelContextProps => ({
    ...ruleEditorModelContextDefaultValue,
    setReactFlowInstance:
        ruleEditorModelContextDefaultValue.setReactFlowInstance ??
        (noop as RuleEditorModelContextProps["setReactFlowInstance"]),
    executeModelEditOperation: {
        ...ruleEditorModelActionsDefaultValue,
        ...overrides.executeModelEditOperation,
    },
    ...overrides,
});

export const createSingleNodeRuleEditorModelContextValue = (
    currentNode: IRuleOperatorNode,
    overrides: Partial<RuleEditorModelContextProps> = {},
): RuleEditorModelContextProps =>
    createRuleEditorModelContextValue({
        ruleOperatorNodes: () => [currentNode],
        ...overrides,
    });
