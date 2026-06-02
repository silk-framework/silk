import React from "react";
import "@testing-library/jest-dom";
import { act, render, waitFor } from "@testing-library/react";
import type { IProjectTask } from "@ducks/shared/typings";
import { mockReactI18next, testTranslate } from "../../../../test/jestTestUtils";
import type { RuleEditorEvaluationContextProps } from "../../../shared/RuleEditor/contexts/RuleEditorEvaluationContext";
import type { IRuleOperatorNode } from "../../../shared/RuleEditor/RuleEditor.typings";
import type { IRuleBlockPort, IRuleBlockTaskParameters } from "../ruleBlock.types";

const createRuleBlockEvaluationHarness = () => {
    jest.resetModules();
    jest.doMock("react", () => React);

    const mockRequestRuleBlockEvaluation = jest.fn();
    const mockRegisterError = jest.fn();
    const mockLinkRuleNodeEvaluation = jest.fn(({ ruleOperatorId }) => <div data-testid={`evaluation-${ruleOperatorId}`} />);

    mockReactI18next(testTranslate);
    jest.doMock("../../../../hooks/useErrorHandler", () => ({
        __esModule: true,
        default: () => ({
            registerError: mockRegisterError,
        }),
    }));
    jest.doMock("../../shared/rules/rule.utils", () => ({
        __esModule: true,
        default: {
            convertToRuleOperatorNodeMap: jest.fn((ruleOperatorNodes) => [
                new Map(ruleOperatorNodes.map((node) => [node.nodeId, node])),
                [ruleOperatorNodes[0]],
            ]),
            convertRuleOperatorNodeToValueInput: jest.fn(() => ({
                type: "transformInput",
                id: "lowerCaseNode",
                function: "lowerCase",
                inputs: [
                    {
                        type: "inputPortInput",
                        id: "inputPortNode",
                        portId: "inputPortA",
                    },
                ],
                parameters: {},
            })),
            ruleLayout: jest.fn(() => ({
                nodePositions: {},
            })),
        },
    }));
    jest.doMock("../ruleBlock.requests", () => ({
        requestRuleBlockEvaluation: (...args) => mockRequestRuleBlockEvaluation(...args),
    }));
    jest.doMock("../../linking/evaluation/LinkRuleNodeEvaluation", () => ({
        LinkRuleNodeEvaluation: (props) => mockLinkRuleNodeEvaluation(props),
    }));

    const { default: RuleBlockEvaluation } = require("../RuleBlockEvaluation") as typeof import("../RuleBlockEvaluation");
    const { RuleEditorEvaluationContext } = require("../../../shared/RuleEditor/contexts/RuleEditorEvaluationContext") as
        typeof import("../../../shared/RuleEditor/contexts/RuleEditorEvaluationContext");
    return {
        RuleBlockEvaluation,
        RuleEditorEvaluationContext,
        mockLinkRuleNodeEvaluation,
        mockRequestRuleBlockEvaluation,
        mockRegisterError,
    };
};

const createPort = (overrides: Partial<IRuleBlockPort> = {}): IRuleBlockPort => ({
    id: "inputPortA",
    label: "Input A",
    description: "Description",
    displayOrder: 1,
    deprecated: false,
    ...overrides,
});

const createRuleBlockTask = (ports: IRuleBlockPort[]): IProjectTask<IRuleBlockTaskParameters> => ({
    metadata: {
        label: "Rule block task",
    },
    taskType: "RuleBlock",
    id: "ruleBlockTask",
    project: "project1",
    data: {
        type: "RuleBlock",
        parameters: {
            ruleBlockModel: {
                ports,
                inputExamples: [],
                layout: { nodePositions: {} },
                uiAnnotations: { stickyNotes: [] },
            },
        },
    },
} as IProjectTask<IRuleBlockTaskParameters>);

const createInputPortNode = (): IRuleOperatorNode => ({
    nodeId: "inputPortNode",
    pluginType: "InputPortOperator",
    pluginId: "inputPort",
    label: "Input port",
    parameters: {
        portId: "inputPortA",
    },
    inputs: [],
    portSpecification: {
        type: "count",
        minInputPorts: 0,
        maxInputPorts: 0,
    },
    inputsCanBeSwitched: false,
});

const createTransformNode = (): IRuleOperatorNode => ({
    nodeId: "lowerCaseNode",
    pluginType: "TransformOperator",
    pluginId: "lowerCase",
    label: "Lower case",
    parameters: {},
    inputs: ["inputPortNode"],
    portSpecification: {
        type: "count",
        minInputPorts: 1,
    },
    inputsCanBeSwitched: false,
});

describe("RuleBlockEvaluation", () => {
    it("should evaluate the current rule block model with the current example values", async () => {
        const harness = createRuleBlockEvaluationHarness();
        const ports = [createPort()];
        const onOpenExampleValuesDialog = jest.fn();
        const inputExamples = [
            {
                id: "example-1",
                inputs: {
                    inputPortA: ["Example value"],
                },
            },
        ];
        const originalTask = createRuleBlockTask(ports);
        const capturedContexts: RuleEditorEvaluationContextProps[] = [];

        const ContextProbe = () => {
            const evaluationContext = React.useContext(harness.RuleEditorEvaluationContext);
            React.useEffect(() => {
                capturedContexts.push(evaluationContext);
            }, [evaluationContext]);
            return null;
        };

        harness.mockRequestRuleBlockEvaluation.mockResolvedValue({
            data: [
                {
                    operatorId: "lowerCaseNode",
                    values: ["port 1 value"],
                    error: null,
                    children: [
                        {
                            operatorId: "inputPortNode",
                            values: ["Port 1 value"],
                            error: null,
                            children: [],
                        },
                    ],
                },
            ],
        });

        render(
            <harness.RuleBlockEvaluation
                projectId="project1"
                ruleBlockTaskId="task1"
                numberOfEntitiesToShow={5}
                getPorts={() => ports}
                getInputExamples={() => inputExamples}
                onOpenExampleValuesDialog={onOpenExampleValuesDialog}
            >
                {(<ContextProbe /> as unknown) as React.ReactElement}
            </harness.RuleBlockEvaluation>,
        );

        await waitFor(() => expect(capturedContexts.length).toBeGreaterThan(0));
        const evaluationContext = capturedContexts[capturedContexts.length - 1];

        await act(async () => {
            await evaluationContext.startEvaluation([createTransformNode(), createInputPortNode()], originalTask, false);
        });

        expect(harness.mockRequestRuleBlockEvaluation).toHaveBeenCalledWith("project1", "task1", {
            ports,
            inputExamples,
            operatorTree: {
                type: "transformInput",
                id: "lowerCaseNode",
                function: "lowerCase",
                inputs: [
                    {
                        type: "inputPortInput",
                        id: "inputPortNode",
                        portId: "inputPortA",
                    },
                ],
                parameters: {},
            },
            layout: {
                nodePositions: {},
            },
            uiAnnotations: {
                stickyNotes: [],
            },
        });
        expect(harness.mockRegisterError).not.toHaveBeenCalled();
    });

    it("should provide evaluation config menu items for example values and the no-input-examples message", async () => {
        const harness = createRuleBlockEvaluationHarness();
        const ports = [createPort()];
        const onOpenExampleValuesDialog = jest.fn();
        const capturedContexts: RuleEditorEvaluationContextProps[] = [];

        const ContextProbe = () => {
            const evaluationContext = React.useContext(harness.RuleEditorEvaluationContext);
            React.useEffect(() => {
                capturedContexts.push(evaluationContext);
            }, [evaluationContext]);
            return evaluationContext.createRuleEditorEvaluationComponent("lowerCaseNode");
        };

        render(
            <harness.RuleBlockEvaluation
                projectId="project1"
                ruleBlockTaskId="task1"
                numberOfEntitiesToShow={5}
                getPorts={() => ports}
                getInputExamples={() => []}
                onOpenExampleValuesDialog={onOpenExampleValuesDialog}
            >
                {(<ContextProbe /> as unknown) as React.ReactElement}
            </harness.RuleBlockEvaluation>,
        );

        await waitFor(() => expect(capturedContexts.length).toBeGreaterThan(0));

        const evaluationContext = capturedContexts[capturedContexts.length - 1];
        expect(evaluationContext.evaluationConfigMenu).toMatchObject({
            tooltip: "Show more options",
            menuItems: [
                expect.objectContaining({
                    tooltip: "taskViews.ruleBlock.exampleValues",
                    icon: "item-settings",
                }),
            ],
        });

        expect(harness.mockLinkRuleNodeEvaluation).toHaveBeenCalledWith(
            expect.objectContaining({
                ruleOperatorId: "lowerCaseNode",
                noResultMsg:
                    "No input examples exist yet. Example values can be added via the evaluation menu or input port node menu.",
            }),
        );

        evaluationContext.evaluationConfigMenu?.menuItems[0].action();
        expect(onOpenExampleValuesDialog).toHaveBeenCalledTimes(1);
    });
});
