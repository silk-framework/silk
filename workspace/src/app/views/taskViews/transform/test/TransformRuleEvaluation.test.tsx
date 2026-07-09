import React from "react";
import "@testing-library/jest-dom";
import { act, render, waitFor } from "@testing-library/react";
import ruleBlockInternalEvaluationTestHelper from "../../shared/rules/tests/ruleBlockInternalEvaluationTestHelper";
import ruleTestHelper from "../../shared/rules/tests/ruleTestHelper";
import type { RuleEditorEvaluationContextProps } from "../../../shared/RuleEditor/contexts/RuleEditorEvaluationContext";
import type { IRuleOperatorNode } from "../../../shared/RuleEditor/RuleEditor.typings";
import type { IComplexMappingRule, ITransformRuleEvaluationResponse } from "../transform.types";
import type { RuleBlockSnapshot, RuleBlockPort } from "../../ruleBlock/ruleBlock.types";

const createTransformInspectionHarness = () => {
    const baseHarness = ruleBlockInternalEvaluationTestHelper.createBaseInternalEvaluationHarness();
    const mockEvaluateTransformRuleWithInspection = jest.fn();
    const mockRequestTaskContextInfo = jest.fn();
    jest.doMock("@ducks/workspace/requests", () => ({
        requestTaskContextInfo: (...args) => mockRequestTaskContextInfo(...args),
    }));
    jest.doMock("../transform.requests", () => ({
        evaluateTransformRuleWithInspection: (...args) => mockEvaluateTransformRuleWithInspection(...args),
    }));
    jest.doMock("../../shared/rules/rule.utils", () => ({
        __esModule: true,
        default: {
            convertToRuleOperatorNodeMap: jest.fn((ruleOperatorNodes) => [
                new Map(ruleOperatorNodes.map((node) => [node.nodeId, node])),
                [ruleOperatorNodes[0]],
            ]),
            convertRuleOperatorNodeToValueInput: jest.fn(() =>
                ruleTestHelper.createRuleBlockInput({
                    id: "ruleBlockUsage",
                    ruleBlockId: "normalizeName",
                    bindings: [
                        {
                            portId: "nameInput",
                            input: ruleTestHelper.createPathInput({
                                id: "namePath",
                                path: "name",
                            }),
                        },
                    ],
                }),
            ),
            ruleLayout: jest.fn(() => ruleTestHelper.defaultLayout()),
        },
    }));
    const { default: TransformRuleEvaluation } =
        require("../evaluation/TransformRuleEvaluation") as typeof import("../evaluation/TransformRuleEvaluation");

    return {
        ...baseHarness,
        TransformRuleEvaluation,
        mockEvaluateTransformRuleWithInspection,
        mockRequestTaskContextInfo,
    };
};

const createRuleBlockPort = (): RuleBlockPort =>
    ruleTestHelper.createRuleBlockPort({
        id: "nameInput",
        label: "Name",
    });

const createRuleBlockSnapshot = (): RuleBlockSnapshot =>
    ruleTestHelper.createRuleBlockInspectionSnapshot({
        ports: [createRuleBlockPort()],
        operatorTree: ruleTestHelper.createTransformInput({
            id: "normalizeInput",
            inputs: [
                ruleTestHelper.createInputPortInput({
                    id: "namePortInput",
                    portId: "nameInput",
                }),
            ],
        }),
    });

const createRuleBlockUsageNode = (): IRuleOperatorNode =>
    ruleTestHelper.createRuleBlockUsageNode({
        inputs: ["namePath"],
    });

const createOriginalRule = (): IComplexMappingRule =>
    ruleTestHelper.createComplexMappingRule({
        operator: ruleTestHelper.createPathInput({
            id: "originalPath",
            path: "name",
        }),
    });

describe("TransformRuleEvaluation", () => {
    it("should derive input examples from black-box rule block evaluation results", async () => {
        const harness = createTransformInspectionHarness();
        let latestEvaluationContext: RuleEditorEvaluationContextProps | undefined;

        const EvaluationChild = ({ overlayContent }: { overlayContent?: React.ReactNode }) => {
            const evaluationContext = React.useContext(harness.RuleEditorEvaluationContext);
            React.useEffect(() => {
                latestEvaluationContext = evaluationContext;
            }, [evaluationContext]);
            return <>{overlayContent}</>;
        };

        // The outer transform evaluation stays black-box for the rule block usage and only exposes the bound path value
        // as the child that will later be turned into the synthetic input example for inspection.
        const evaluationResponse: ITransformRuleEvaluationResponse = {
            evaluatedValues: [
                {
                    operatorId: "ruleBlockUsage",
                    values: ["Kiel"],
                    error: null,
                    children: [
                        {
                            operatorId: "namePath",
                            values: ["Kiel"],
                            error: null,
                            children: [],
                        },
                    ],
                },
            ],
            ruleBlockInspection: {
                snapshots: {
                    normalizeName: createRuleBlockSnapshot(),
                },
            },
        };
        harness.mockEvaluateTransformRuleWithInspection.mockResolvedValue({ data: evaluationResponse });

        render(
            <harness.TransformRuleEvaluation
                projectId="project1"
                transformTaskId="transformTask"
                containerRuleId="root"
                numberOfLinkToShow={5}
            >
                {(<EvaluationChild />) as unknown as React.ReactElement}
            </harness.TransformRuleEvaluation>,
        );

        await waitFor(() => expect(latestEvaluationContext).toBeDefined());
        const evaluationContext = latestEvaluationContext!;

        await act(async () => {
            await evaluationContext.startEvaluation([createRuleBlockUsageNode()], createOriginalRule(), false);
        });

        await waitFor(() =>
            expect(latestEvaluationContext?.canEvaluateRuleBlock?.("ruleBlockUsage", "normalizeName")).toBe(true),
        );
        const updatedEvaluationContext = latestEvaluationContext!;

        await act(async () => {
            updatedEvaluationContext.openInternalRuleBlockEvaluation?.(
                "ruleBlockUsage",
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
                            nameInput: ["Kiel"],
                        },
                    },
                ],
            }),
        );
        expect(harness.mockRegisterError).not.toHaveBeenCalled();
        expect(harness.mockRegisterErrorI18N).not.toHaveBeenCalled();
    });
});
