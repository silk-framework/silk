import React from "react";
import "@testing-library/jest-dom";
import { act, render, waitFor } from "@testing-library/react";
import type { TaskPlugin } from "@ducks/shared/typings";
import jestTestUtils from "../../../../test/jestTestUtils";
import ruleTestHelper from "../../shared/rules/tests/ruleTestHelper";
import type { RuleEditorEvaluationContextProps } from "../../../shared/RuleEditor/contexts/RuleEditorEvaluationContext";
import type { IRuleOperatorNode } from "../../../shared/RuleEditor/RuleEditor.typings";
import type {
    ComparisonConfidence,
    IEvaluatedReferenceLinksWithInspection,
    ILinkingTaskParameters,
} from "../linking.types";
import type { RuleBlockSnapshot } from "../../ruleBlock/ruleBlock.types";

const mockRegisterError = jest.fn();
const mockRegisterErrorI18N = jest.fn();
const mockEvaluateLinkingRuleAgainstReferenceEntitiesWithInspection = jest.fn();
const mockEvaluateLinkingRuleWithInspection = jest.fn();
let lastInternalEvaluationModalProps: Record<string, unknown> | undefined;
let RuleEditorEvaluationContext: typeof import("../../../shared/RuleEditor/contexts/RuleEditorEvaluationContext").RuleEditorEvaluationContext;
let LinkingRuleEvaluation: typeof import("../evaluation/LinkingRuleEvaluation").LinkingRuleEvaluation;

describe("LinkingRuleEvaluation", () => {
    beforeAll(() => {
        setupLinkingRuleEvaluationTest();
    });

    beforeEach(() => {
        lastInternalEvaluationModalProps = undefined;
        mockRegisterError.mockReset();
        mockRegisterErrorI18N.mockReset();
        mockEvaluateLinkingRuleAgainstReferenceEntitiesWithInspection.mockReset();
        mockEvaluateLinkingRuleWithInspection.mockReset();
    });

    it("should derive input examples from black-box linking evaluation results", async () => {
        let latestEvaluationContext: RuleEditorEvaluationContextProps | undefined;
        const LinkingRuleEvaluationComponent = LinkingRuleEvaluation;
        const EvaluationChild = createEvaluationChild((evaluationContext) => {
            latestEvaluationContext = evaluationContext;
        });

        mockEvaluateLinkingRuleAgainstReferenceEntitiesWithInspection.mockResolvedValue({
            data: evaluationResponse(),
        });

        render(
            <LinkingRuleEvaluationComponent projectId="project1" linkingTaskId="linkingTask" numberOfLinkToShow={5}>
                {(<EvaluationChild />) as unknown as React.ReactElement<any>}
            </LinkingRuleEvaluationComponent>,
        );

        await waitFor(() => expect(latestEvaluationContext).toBeDefined());
        const evaluationContext = latestEvaluationContext!;

        await act(async () => {
            await evaluationContext.startEvaluation([createComparisonNode()], createOriginalTask(), false);
        });

        await waitFor(() =>
            expect(latestEvaluationContext?.canEvaluateRuleBlock?.("sourceRuleBlockUsage", "normalizeName")).toBe(true),
        );
        const updatedEvaluationContext = latestEvaluationContext!;

        await act(async () => {
            updatedEvaluationContext.openInternalRuleBlockEvaluation?.(
                "sourceRuleBlockUsage",
                "normalizeName",
                "Normalize Name",
            );
        });

        await waitFor(() =>
            expect(lastInternalEvaluationModalProps).toMatchObject({
                projectId: "project1",
                ruleBlockId: "normalizeName",
                ruleBlockLabel: "Normalize Name",
                inputExamples: [
                    {
                        id: "inspection-example-1",
                        inputs: {
                            labelInput: ["Kiel"],
                        },
                    },
                ],
            }),
        );
        expect(mockEvaluateLinkingRuleWithInspection).not.toHaveBeenCalled();
        expect(mockRegisterError).not.toHaveBeenCalled();
    });

    it("should inspect rule block evaluations from the slower fallback response when no reference-link results are available", async () => {
        let latestEvaluationContext: RuleEditorEvaluationContextProps | undefined;
        const LinkingRuleEvaluationComponent = LinkingRuleEvaluation;
        const EvaluationChild = createEvaluationChild((evaluationContext) => {
            latestEvaluationContext = evaluationContext;
        });

        mockEvaluateLinkingRuleAgainstReferenceEntitiesWithInspection.mockResolvedValue({
            data: {
                positive: [],
                negative: [],
                evaluationScore: {
                    fMeasure: "0.0",
                    precision: "0.0",
                    recall: "0.0",
                    falseNegatives: 0,
                    falsePositives: 0,
                    trueNegatives: 0,
                    truePositives: 0,
                },
                ruleBlockInspection: {
                    snapshots: {},
                },
            },
        });
        mockEvaluateLinkingRuleWithInspection.mockResolvedValue({
            data: {
                links: [
                    {
                        source: "source-1",
                        target: "target-1",
                        decision: "unlabeled",
                        // This covers the slower fallback path where the fast reference-link response has no links, but the
                        // fallback evaluation still exposes the same black-box rule block usage shape for inspection.
                        ruleValues: {
                            operatorId: "compareLabels",
                            score: 1,
                            sourceValue: {
                                operatorId: "sourceRuleBlockUsage",
                                values: ["Kiel"],
                                children: [
                                    {
                                        operatorId: "sourceLabel",
                                        values: ["Kiel"],
                                        children: [],
                                    },
                                ],
                            },
                            targetValue: {
                                operatorId: "targetLabel",
                                values: ["Kiel"],
                                children: [],
                            },
                        } as ComparisonConfidence,
                    },
                ],
                ruleBlockInspection: {
                    snapshots: {
                        normalizeName: createRuleBlockSnapshot(),
                    },
                },
            },
        });

        render(
            <LinkingRuleEvaluationComponent projectId="project1" linkingTaskId="linkingTask" numberOfLinkToShow={5}>
                {(<EvaluationChild />) as unknown as React.ReactElement<any>}
            </LinkingRuleEvaluationComponent>,
        );

        await waitFor(() => expect(latestEvaluationContext).toBeDefined());
        const evaluationContext = latestEvaluationContext!;

        await act(async () => {
            await evaluationContext.startEvaluation([createComparisonNode()], createOriginalTask(), false);
        });

        expect(mockEvaluateLinkingRuleAgainstReferenceEntitiesWithInspection).toHaveBeenCalledTimes(1);
        expect(mockEvaluateLinkingRuleWithInspection).toHaveBeenCalledTimes(1);
        await waitFor(() =>
            expect(latestEvaluationContext?.canEvaluateRuleBlock?.("sourceRuleBlockUsage", "normalizeName")).toBe(true),
        );

        await act(async () => {
            latestEvaluationContext!.openInternalRuleBlockEvaluation?.(
                "sourceRuleBlockUsage",
                "normalizeName",
                "Normalize Name",
            );
        });

        await waitFor(() =>
            expect(lastInternalEvaluationModalProps).toMatchObject({
                projectId: "project1",
                ruleBlockId: "normalizeName",
                ruleBlockLabel: "Normalize Name",
                inputExamples: [
                    {
                        id: "inspection-example-1",
                        inputs: {
                            labelInput: ["Kiel"],
                        },
                    },
                ],
            }),
        );
        expect(mockRegisterError).not.toHaveBeenCalled();
    });
});

const createRuleBlockSnapshot = (): RuleBlockSnapshot =>
    ruleTestHelper.createRuleBlockInspectionSnapshot({
        ports: [
            ruleTestHelper.createRuleBlockPort({
                id: "labelInput",
                label: "Label",
            }),
        ],
        operatorTree: ruleTestHelper.createTransformInput({
            id: "normalizeInput",
            inputs: [
                ruleTestHelper.createInputPortInput({
                    id: "labelPortInput",
                    portId: "labelInput",
                }),
            ],
        }),
    });

const createComparisonNode = (): IRuleOperatorNode => ruleTestHelper.createComparisonNode();

const createOriginalTask = (): TaskPlugin<ILinkingTaskParameters> => ruleTestHelper.createLinkingTask();

const evaluationResponse = (): IEvaluatedReferenceLinksWithInspection => ({
    positive: [
        {
            source: "source-1",
            target: "target-1",
            decision: "positive",
            // The rule block stays a black-box node in the outer comparison result. Inspection derives the port value
            // from the exposed source-side binding child rather than from any inlined internal rule block tree.
            ruleValues: {
                operatorId: "compareLabels",
                score: 1,
                sourceValue: {
                    operatorId: "sourceRuleBlockUsage",
                    values: ["Kiel"],
                    children: [
                        {
                            operatorId: "sourceLabel",
                            values: ["Kiel"],
                            children: [],
                        },
                    ],
                },
                targetValue: {
                    operatorId: "targetLabel",
                    values: ["Kiel"],
                    children: [],
                },
            } as ComparisonConfidence,
        },
    ],
    negative: [],
    evaluationScore: {
        fMeasure: "1.0",
        precision: "1.0",
        recall: "1.0",
        falseNegatives: 0,
        falsePositives: 0,
        trueNegatives: 0,
        truePositives: 1,
    },
    ruleBlockInspection: {
        snapshots: {
            normalizeName: createRuleBlockSnapshot(),
        },
    },
});

const createEvaluationChild = (onContextChange: (evaluationContext: RuleEditorEvaluationContextProps) => void) => {
    return function EvaluationChild({ overlayContent }: { overlayContent?: React.ReactNode }) {
        const evaluationContext = React.useContext(RuleEditorEvaluationContext);
        React.useEffect(() => {
            onContextChange(evaluationContext);
        }, [evaluationContext]);
        return <>{overlayContent}</>;
    };
};

const setupLinkingRuleEvaluationTest = () => {
    jest.resetModules();
    jest.doMock("react", () => React);
    jestTestUtils.mockReactI18next(jestTestUtils.testTranslate);
    jest.doMock("../../../../utils/basicUtils", () => ({
        queryParameterValue: jest.fn(() => []),
    }));
    jest.doMock("../../../../hooks/useErrorHandler", () => ({
        __esModule: true,
        default: () => ({
            registerError: mockRegisterError,
            registerErrorI18N: mockRegisterErrorI18N,
        }),
    }));
    jest.doMock("../LinkingRuleEditor.requests", () => ({
        evaluateLinkingRuleAgainstReferenceEntitiesWithInspection: (...args) =>
            mockEvaluateLinkingRuleAgainstReferenceEntitiesWithInspection(...args),
        evaluateLinkingRuleWithInspection: (...args) => mockEvaluateLinkingRuleWithInspection(...args),
    }));
    jest.doMock("../LinkingRuleEditor.utils", () => ({
        __esModule: true,
        default: {
            constructLinkageRuleTree: jest.fn(() =>
                ruleTestHelper.createComparisonOperator({
                    id: "compareLabels",
                    sourceInput: ruleTestHelper.createRuleBlockInput({
                        id: "sourceRuleBlockUsage",
                        ruleBlockId: "normalizeName",
                        bindings: [
                            {
                                portId: "labelInput",
                                input: ruleTestHelper.createPathInput({
                                    id: "sourceLabel",
                                    path: "label",
                                }),
                            },
                        ],
                    }),
                    targetInput: ruleTestHelper.createPathInput({
                        id: "targetLabel",
                        path: "label",
                    }),
                }),
            ),
            optionallyLabelledParameterToValue: jest.fn((value) => value),
        },
    }));
    jest.doMock("../../shared/evaluations/PathNotInCacheModal", () => ({
        PathNotInCacheModal: () => null,
    }));
    jest.doMock("../evaluation/LinkRuleNodeEvaluation", () => ({
        LinkRuleNodeEvaluation: () => null,
    }));
    jest.doMock("../../ruleBlock/RuleBlockInternalEvaluationModal", () => ({
        RuleBlockInternalEvaluationModal: (props) => {
            lastInternalEvaluationModalProps = props;
            return <div data-testid="rule-block-internal-evaluation-modal" />;
        },
    }));

    ({ RuleEditorEvaluationContext } = require("../../../shared/RuleEditor/contexts/RuleEditorEvaluationContext"));
    ({ LinkingRuleEvaluation } = require("../evaluation/LinkingRuleEvaluation"));
};
