import React from "react";
import "@testing-library/jest-dom";
import { act, render, waitFor } from "@testing-library/react";
import type { TaskPlugin } from "@ducks/shared/typings";
import ruleBlockInternalEvaluationTestHelper from "../../shared/rules/tests/ruleBlockInternalEvaluationTestHelper";
import ruleTestHelper from "../../shared/rules/tests/ruleTestHelper";
import type { RuleEditorEvaluationContextProps } from "../../../shared/RuleEditor/contexts/RuleEditorEvaluationContext";
import type { IRuleOperatorNode } from "../../../shared/RuleEditor/RuleEditor.typings";
import type {
    ComparisonConfidence,
    IEvaluatedReferenceLinksWithInspection,
    ILinkingRule,
    ILinkingTaskParameters,
} from "../linking.types";
import type { RuleBlockSnapshot } from "../../ruleBlock/ruleBlock.types";

const createLinkingInspectionHarness = () => {
    const baseHarness = ruleBlockInternalEvaluationTestHelper.createBaseInternalEvaluationHarness();
    const mockEvaluateLinkingRuleAgainstReferenceEntitiesWithInspection = jest.fn();
    const mockEvaluateLinkingRuleWithInspection = jest.fn();
    jest.doMock("../../../../utils/basicUtils", () => ({
        queryParameterValue: jest.fn(() => []),
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

    const { LinkingRuleEvaluation } =
        require("../evaluation/LinkingRuleEvaluation") as typeof import("../evaluation/LinkingRuleEvaluation");

    return {
        ...baseHarness,
        LinkingRuleEvaluation,
        mockEvaluateLinkingRuleAgainstReferenceEntitiesWithInspection,
        mockEvaluateLinkingRuleWithInspection,
    };
};

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

const createOriginalLinkingRule = (): ILinkingRule => ruleTestHelper.createLinkingRule();

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

describe("LinkingRuleEvaluation", () => {
    it("should derive input examples from black-box linking evaluation results", async () => {
        const harness = createLinkingInspectionHarness();
        let latestEvaluationContext: RuleEditorEvaluationContextProps | undefined;

        const ContextProbe = () => {
            const evaluationContext = React.useContext(harness.RuleEditorEvaluationContext);
            React.useEffect(() => {
                latestEvaluationContext = evaluationContext;
            }, [evaluationContext]);
            return null;
        };

        harness.mockEvaluateLinkingRuleAgainstReferenceEntitiesWithInspection.mockResolvedValue({
            data: evaluationResponse(),
        });

        render(
            <harness.LinkingRuleEvaluation projectId="project1" linkingTaskId="linkingTask" numberOfLinkToShow={5}>
                {(<ContextProbe />) as unknown as React.ReactElement}
            </harness.LinkingRuleEvaluation>,
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
            expect(harness.getLastInternalEvaluationModalProps()).toMatchObject({
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
        expect(harness.mockEvaluateLinkingRuleWithInspection).not.toHaveBeenCalled();
        expect(harness.mockRegisterError).not.toHaveBeenCalled();
    });

    it("should inspect rule block evaluations from the slower fallback response when no reference-link results are available", async () => {
        const harness = createLinkingInspectionHarness();
        let latestEvaluationContext: RuleEditorEvaluationContextProps | undefined;

        const ContextProbe = () => {
            const evaluationContext = React.useContext(harness.RuleEditorEvaluationContext);
            React.useEffect(() => {
                latestEvaluationContext = evaluationContext;
            }, [evaluationContext]);
            return null;
        };

        harness.mockEvaluateLinkingRuleAgainstReferenceEntitiesWithInspection.mockResolvedValue({
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
        harness.mockEvaluateLinkingRuleWithInspection.mockResolvedValue({
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
            <harness.LinkingRuleEvaluation projectId="project1" linkingTaskId="linkingTask" numberOfLinkToShow={5}>
                {(<ContextProbe />) as unknown as React.ReactElement}
            </harness.LinkingRuleEvaluation>,
        );

        await waitFor(() => expect(latestEvaluationContext).toBeDefined());
        const evaluationContext = latestEvaluationContext!;

        await act(async () => {
            await evaluationContext.startEvaluation([createComparisonNode()], createOriginalTask(), false);
        });

        expect(harness.mockEvaluateLinkingRuleAgainstReferenceEntitiesWithInspection).toHaveBeenCalledTimes(1);
        expect(harness.mockEvaluateLinkingRuleWithInspection).toHaveBeenCalledTimes(1);
        await waitFor(() =>
            expect(latestEvaluationContext?.canEvaluateRuleBlock?.("sourceRuleBlockUsage", "normalizeName")).toBe(
                true,
            ),
        );

        await act(async () => {
            latestEvaluationContext!.openInternalRuleBlockEvaluation?.(
                "sourceRuleBlockUsage",
                "normalizeName",
                "Normalize Name",
            );
        });

        await waitFor(() =>
            expect(harness.getLastInternalEvaluationModalProps()).toMatchObject({
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
        expect(harness.mockRegisterError).not.toHaveBeenCalled();
    });
});
