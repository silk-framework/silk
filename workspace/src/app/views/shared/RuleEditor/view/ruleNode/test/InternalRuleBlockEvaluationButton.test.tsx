import React from "react";
import { fireEvent } from "@testing-library/react";
import { renderWrapper } from "../../../../../../../../test/integration/TestHelper";
import { InternalRuleBlockEvaluationButton } from "../InternalRuleBlockEvaluationButton";
import {
    RuleEditorEvaluationContext,
    RuleEditorEvaluationContextProps,
    ruleEditorEvaluationContextDefaultValue,
} from "../../../contexts/RuleEditorEvaluationContext";
import {
    RuleEditorModelContext,
    RuleEditorModelContextProps,
    ruleEditorModelContextDefaultValue,
} from "../../../contexts/RuleEditorModelContext";
import type { IRuleOperatorNode } from "../../../RuleEditor.typings";
import ruleTestHelper from "../../../../../taskViews/shared/rules/tests/ruleTestHelper";

const translation = (translationKey: string, defaultValue?: string) => defaultValue ?? translationKey;

const createRuleBlockNode = (): IRuleOperatorNode =>
    ruleTestHelper.createRuleOperatorNode({
        nodeId: "ruleBlockUsage",
        pluginType: "RuleBlock",
        pluginId: "normalizeName",
        label: "Normalize Name",
    });

const renderButton = ({
    evaluationContextOverrides = {},
    ruleNodes = [createRuleBlockNode()],
}: {
    evaluationContextOverrides?: Partial<RuleEditorEvaluationContextProps>;
    ruleNodes?: IRuleOperatorNode[];
} = {}) => {
    const evaluationContext: RuleEditorEvaluationContextProps = {
        ...ruleEditorEvaluationContextDefaultValue,
        evaluationResultsShown: true,
        canEvaluateRuleBlock: () => true,
        openInternalRuleBlockEvaluation: jest.fn(),
        ...evaluationContextOverrides,
    };
    const modelContext: RuleEditorModelContextProps = {
        ...ruleEditorModelContextDefaultValue,
        ruleOperatorNodes: () => ruleNodes,
    };

    const renderResult = renderWrapper(
        <RuleEditorEvaluationContext.Provider value={evaluationContext}>
            <RuleEditorModelContext.Provider value={modelContext}>
                <InternalRuleBlockEvaluationButton nodeId="ruleBlockUsage" t={translation} />
            </RuleEditorModelContext.Provider>
        </RuleEditorEvaluationContext.Provider>,
    );

    return {
        renderResult,
        openInternalRuleBlockEvaluation: evaluationContext.openInternalRuleBlockEvaluation as jest.Mock,
    };
};

describe("InternalRuleBlockEvaluationButton", () => {
    it("hides the button until evaluation results are shown", () => {
        renderButton({
            evaluationContextOverrides: {
                evaluationResultsShown: false,
            },
        });

        expect(
            document.querySelector('[data-test-id="rule-node-open-internal-rule-block-evaluation-icon-btn"]'),
        ).not.toBeInTheDocument();
    });

    it("shows a disabled button when evaluation is visible but the rule block cannot be evaluated", () => {
        const { renderResult } = renderButton({
            evaluationContextOverrides: {
                canEvaluateRuleBlock: () => false,
            },
        });

        expect(
            renderResult.container.querySelector('[data-test-id="rule-node-open-internal-rule-block-evaluation-icon-btn"]'),
        ).toBeDisabled();
    });

    it("opens the internal evaluation for evaluable rule block usages", () => {
        const { renderResult, openInternalRuleBlockEvaluation } = renderButton();

        fireEvent.click(
            renderResult.container.querySelector(
                '[data-test-id="rule-node-open-internal-rule-block-evaluation-icon-btn"]',
            ) as Element,
        );

        expect(openInternalRuleBlockEvaluation).toHaveBeenCalledWith(
            "ruleBlockUsage",
            "normalizeName",
            "Normalize Name",
        );
    });
});
