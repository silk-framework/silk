import React from "react";
import jestTestUtils from "../../../../../test/jestTestUtils";

interface BaseInternalEvaluationHarness {
    RuleEditorEvaluationContext: typeof import("../../../../shared/RuleEditor/contexts/RuleEditorEvaluationContext").RuleEditorEvaluationContext;
    getLastInternalEvaluationModalProps: () => Record<string, unknown> | undefined;
    mockRegisterError: jest.Mock;
    mockRegisterErrorI18N: jest.Mock;
}

const createBaseInternalEvaluationHarness = (): BaseInternalEvaluationHarness => {
    jest.resetModules();
    jest.doMock("react", () => React);

    const mockRegisterError = jest.fn();
    const mockRegisterErrorI18N = jest.fn();
    let lastInternalEvaluationModalProps: Record<string, unknown> | undefined;

    jestTestUtils.mockReactI18next(jestTestUtils.testTranslate);
    jest.doMock(require.resolve("../../../../../hooks/useErrorHandler"), () => ({
        __esModule: true,
        default: () => ({
            registerError: mockRegisterError,
            registerErrorI18N: mockRegisterErrorI18N,
        }),
    }));
    jest.doMock(require.resolve("../../../linking/evaluation/LinkRuleNodeEvaluation"), () => ({
        LinkRuleNodeEvaluation: () => null,
    }));
    jest.doMock(require.resolve("../../../ruleBlock/RuleBlockInternalEvaluationModal"), () => ({
        RuleBlockInternalEvaluationModal: (props) => {
            lastInternalEvaluationModalProps = props;
            return <div data-testid="rule-block-internal-evaluation-modal" />;
        },
    }));

    const { RuleEditorEvaluationContext } =
        require("../../../../shared/RuleEditor/contexts/RuleEditorEvaluationContext") as typeof import("../../../../shared/RuleEditor/contexts/RuleEditorEvaluationContext");

    return {
        RuleEditorEvaluationContext,
        getLastInternalEvaluationModalProps: () => lastInternalEvaluationModalProps,
        mockRegisterError,
        mockRegisterErrorI18N,
    };
};

const ruleBlockInternalEvaluationTestHelper = {
    createBaseInternalEvaluationHarness,
};

export default ruleBlockInternalEvaluationTestHelper;
