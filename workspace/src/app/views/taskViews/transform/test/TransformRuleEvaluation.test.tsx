import React from "react";
import "@testing-library/jest-dom";
import { act, render, waitFor } from "@testing-library/react";
import jestTestUtils from "../../../../test/jestTestUtils";
import ruleTestHelper from "../../shared/rules/tests/ruleTestHelper";
import type { RuleEditorEvaluationContextProps } from "../../../shared/RuleEditor/contexts/RuleEditorEvaluationContext";
import type { IRuleOperatorNode } from "../../../shared/RuleEditor/RuleEditor.typings";
import type { IComplexMappingRule, ITransformRuleEvaluationResponse } from "../transform.types";
import type { RuleBlockSnapshot, RuleBlockPort } from "../../ruleBlock/ruleBlock.types";

const mockRegisterError = jest.fn();
const mockRegisterErrorI18N = jest.fn();
const mockEvaluateTransformRuleWithInspection = jest.fn();
const mockRequestTaskContextInfo = jest.fn();
let lastInternalEvaluationModalProps: Record<string, unknown> | undefined;
let RuleEditorEvaluationContext: typeof import("../../../shared/RuleEditor/contexts/RuleEditorEvaluationContext").RuleEditorEvaluationContext;
let TransformRuleEvaluation: typeof import("../evaluation/TransformRuleEvaluation").default;

describe("TransformRuleEvaluation", () => {
    beforeAll(() => {
        setupTransformRuleEvaluationTest();
    });

    beforeEach(() => {
        lastInternalEvaluationModalProps = undefined;
        mockRegisterError.mockReset();
        mockRegisterErrorI18N.mockReset();
        mockEvaluateTransformRuleWithInspection.mockReset();
        mockRequestTaskContextInfo.mockReset();
    });

    it("should derive input examples from black-box rule block evaluation results", async () => {
        let latestEvaluationContext: RuleEditorEvaluationContextProps | undefined;
        const TransformRuleEvaluationComponent = TransformRuleEvaluation;
        const EvaluationChild = createEvaluationChild((evaluationContext) => {
            latestEvaluationContext = evaluationContext;
        });

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
        mockEvaluateTransformRuleWithInspection.mockResolvedValue({ data: evaluationResponse });

        render(
            <TransformRuleEvaluationComponent
                projectId="project1"
                transformTaskId="transformTask"
                containerRuleId="root"
                numberOfLinkToShow={5}
            >
                {(<EvaluationChild />) as unknown as React.ReactElement<any>}
            </TransformRuleEvaluationComponent>,
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
            expect(lastInternalEvaluationModalProps).toMatchObject({
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
        expect(mockRegisterError).not.toHaveBeenCalled();
        expect(mockRegisterErrorI18N).not.toHaveBeenCalled();
    });
});

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

const createEvaluationChild = (onContextChange: (evaluationContext: RuleEditorEvaluationContextProps) => void) => {
    return function EvaluationChild({ overlayContent }: { overlayContent?: React.ReactNode }) {
        const evaluationContext = React.useContext(RuleEditorEvaluationContext);
        React.useEffect(() => {
            onContextChange(evaluationContext);
        }, [evaluationContext]);
        return <>{overlayContent}</>;
    };
};

const setupTransformRuleEvaluationTest = () => {
    jest.resetModules();
    jest.doMock("react", () => React);
    jestTestUtils.mockReactI18next(jestTestUtils.testTranslate);
    jest.doMock("../../../../hooks/useErrorHandler", () => ({
        __esModule: true,
        default: () => ({
            registerError: mockRegisterError,
            registerErrorI18N: mockRegisterErrorI18N,
        }),
    }));
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
    jest.doMock("../../ruleBlock/RuleBlockInternalEvaluationModal", () => ({
        RuleBlockInternalEvaluationModal: (props) => {
            lastInternalEvaluationModalProps = props;
            return <div data-testid="rule-block-internal-evaluation-modal" />;
        },
    }));

    ({ RuleEditorEvaluationContext } = require("../../../shared/RuleEditor/contexts/RuleEditorEvaluationContext"));
    ({ default: TransformRuleEvaluation } = require("../evaluation/TransformRuleEvaluation"));
};
