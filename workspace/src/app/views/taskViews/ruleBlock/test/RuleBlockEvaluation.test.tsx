import React from "react";
import "@testing-library/jest-dom";
import { act, render, screen, waitFor } from "@testing-library/react";
import type { IProjectTask } from "@ducks/shared/typings";
import jestTestUtils from "../../../../test/jestTestUtils";
import ruleTestHelper from "../../shared/rules/tests/ruleTestHelper";
import type { RuleEditorEvaluationContextProps } from "../../../shared/RuleEditor/contexts/RuleEditorEvaluationContext";
import type { IRuleOperatorNode } from "../../../shared/RuleEditor/RuleEditor.typings";
import type { RuleBlockPort, IRuleBlockTaskParameters } from "../ruleBlock.types";

const createRuleBlockEvaluationHarness = () => {
    jest.resetModules();
    jest.doMock("react", () => React);

    const mockRequestRuleBlockEvaluation = jest.fn();
    const mockRegisterError = jest.fn();
    const mockLinkRuleNodeEvaluation = jest.fn(
        ({ ruleOperatorId, registerForEvaluationResults, unregister, noResultMsg }) => {
            const React = require("react");
            const [evaluationValues, setEvaluationValues] = React.useState(undefined);
            React.useEffect(() => {
                registerForEvaluationResults(ruleOperatorId, (evaluationValues) => {
                    setEvaluationValues(evaluationValues);
                });
                return unregister;
            }, [registerForEvaluationResults, ruleOperatorId, unregister]);
            if (evaluationValues === undefined) {
                return <div data-testid={`evaluation-${ruleOperatorId}`} />;
            }
            const values = evaluationValues.flatMap((value) => value.value);
            return <div data-testid={`evaluation-${ruleOperatorId}`}>{values.length ? values.join("|") : noResultMsg}</div>;
        },
    );

    jestTestUtils.mockReactI18next(jestTestUtils.testTranslate);
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
            convertRuleOperatorNodeToValueInput: jest.fn(() => ruleTestHelper.createTransformInput()),
            ruleLayout: jest.fn(() => ruleTestHelper.defaultLayout()),
        },
    }));
    jest.doMock("../ruleBlock.requests", () => ({
        requestRuleBlockEvaluation: (...args) => mockRequestRuleBlockEvaluation(...args),
    }));
    jest.doMock("../../linking/evaluation/LinkRuleNodeEvaluation", () => ({
        LinkRuleNodeEvaluation: (props) => mockLinkRuleNodeEvaluation(props),
    }));

    const { default: RuleBlockEvaluation } =
        require("../RuleBlockEvaluation") as typeof import("../RuleBlockEvaluation");
    const { RuleEditorEvaluationContext } =
        require("../../../shared/RuleEditor/contexts/RuleEditorEvaluationContext") as typeof import("../../../shared/RuleEditor/contexts/RuleEditorEvaluationContext");
    const { RuleBlockEvaluationOptionalContext } =
        require("../RuleBlockEvaluationOptionalContext") as typeof import("../RuleBlockEvaluationOptionalContext");
    return {
        RuleBlockEvaluation,
        RuleBlockEvaluationOptionalContext,
        RuleEditorEvaluationContext,
        mockLinkRuleNodeEvaluation,
        mockRequestRuleBlockEvaluation,
        mockRegisterError,
    };
};

const createPort = (overrides: Partial<RuleBlockPort> = {}): RuleBlockPort =>
    ruleTestHelper.createRuleBlockPort({
        description: "Description",
        ...overrides,
    });

const createRuleBlockTask = (ports: RuleBlockPort[]): IProjectTask<IRuleBlockTaskParameters> =>
    ruleTestHelper.createRuleBlockTask(ports) as IProjectTask<IRuleBlockTaskParameters>;

const createInputPortNode = (): IRuleOperatorNode => ruleTestHelper.createInputPortNode();

const createTransformNode = (): IRuleOperatorNode => ruleTestHelper.createTransformNode();

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
        let latestEvaluationContext: RuleEditorEvaluationContextProps | undefined;

        const ContextProbe = () => {
            const evaluationContext = React.useContext(harness.RuleEditorEvaluationContext);
            React.useEffect(() => {
                latestEvaluationContext = evaluationContext;
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
                {(<ContextProbe />) as unknown as React.ReactElement}
            </harness.RuleBlockEvaluation>,
        );

        await waitFor(() => expect(latestEvaluationContext).toBeDefined());
        const evaluationContext = latestEvaluationContext!;

        await act(async () => {
            await evaluationContext.startEvaluation(
                [createTransformNode(), createInputPortNode()],
                originalTask,
                false,
            );
        });

        expect(harness.mockRequestRuleBlockEvaluation).toHaveBeenCalledWith("project1", "task1", {
            ports,
            inputExamples,
            operatorTree: ruleTestHelper.createTransformInput(),
            layout: ruleTestHelper.defaultLayout(),
            uiAnnotations: ruleTestHelper.defaultUiAnnotations(),
        });
        expect(harness.mockRegisterError).not.toHaveBeenCalled();
    });

    it("should provide evaluation config menu items for example values and the no-input-examples message", async () => {
        const harness = createRuleBlockEvaluationHarness();
        const ports = [createPort()];
        const onOpenExampleValuesDialog = jest.fn();
        let latestEvaluationContext: RuleEditorEvaluationContextProps | undefined;

        const ContextProbe = () => {
            const evaluationContext = React.useContext(harness.RuleEditorEvaluationContext);
            React.useEffect(() => {
                latestEvaluationContext = evaluationContext;
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
                {(<ContextProbe />) as unknown as React.ReactElement}
            </harness.RuleBlockEvaluation>,
        );

        await waitFor(() => expect(latestEvaluationContext).toBeDefined());

        const evaluationContext = latestEvaluationContext!;
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

    it("should show, hide, and restore externally provided evaluation values without enabling local evaluation controls", async () => {
        const harness = createRuleBlockEvaluationHarness();
        const ports = [createPort()];
        const originalTask = createRuleBlockTask(ports);
        let latestEvaluationContext: RuleEditorEvaluationContextProps | undefined;

        const ContextProbe = () => {
            const evaluationContext = React.useContext(harness.RuleEditorEvaluationContext);
            React.useEffect(() => {
                latestEvaluationContext = evaluationContext;
            }, [evaluationContext]);
            return evaluationContext.createRuleEditorEvaluationComponent("lowerCaseNode");
        };

        render(
            <harness.RuleBlockEvaluationOptionalContext.Provider
                value={{
                    externalEvaluationResults: [
                        {
                            operatorId: "lowerCaseNode",
                            values: ["kiel"],
                            error: null,
                            children: [],
                        },
                    ],
                }}
            >
                <harness.RuleBlockEvaluation
                    projectId="project1"
                    ruleBlockTaskId="task1"
                    numberOfEntitiesToShow={5}
                    getPorts={() => ports}
                    getInputExamples={() => []}
                >
                    {(<ContextProbe />) as unknown as React.ReactElement}
                </harness.RuleBlockEvaluation>
            </harness.RuleBlockEvaluationOptionalContext.Provider>,
        );

        await waitFor(() => expect(latestEvaluationContext).toBeDefined());
        await waitFor(() => expect(screen.getByTestId("evaluation-lowerCaseNode")).toHaveTextContent("kiel"));

        const evaluationContext = latestEvaluationContext!;
        expect(evaluationContext.evaluationResultsShown).toBe(true);
        expect(evaluationContext.evaluationConfigMenu).toBeUndefined();

        // External inspection results behave like a fixed evaluation snapshot: they can be shown/hidden, but they
        // must not re-enable the local rule block evaluation controls or trigger a backend reevaluation.
        await act(async () => {
            evaluationContext.toggleEvaluationResults(false);
        });
        await waitFor(() => expect(screen.getByTestId("evaluation-lowerCaseNode")).toHaveTextContent(""));

        await act(async () => {
            evaluationContext.toggleEvaluationResults(true);
        });
        await waitFor(() => expect(screen.getByTestId("evaluation-lowerCaseNode")).toHaveTextContent("kiel"));

        await act(async () => {
            await evaluationContext.startEvaluation(
                [createTransformNode(), createInputPortNode()],
                originalTask,
                false,
            );
        });

        expect(harness.mockRequestRuleBlockEvaluation).not.toHaveBeenCalled();
    });

    it("should show externally provided evaluation values even if node evaluation widgets register later", async () => {
        const harness = createRuleBlockEvaluationHarness();
        const ports = [createPort()];
        let latestEvaluationContext: RuleEditorEvaluationContextProps | undefined;
        const externalEvaluationResults = [
            {
                operatorId: "lowerCaseNode",
                values: ["kiel"],
                error: null,
                children: [],
            },
        ];

        const ContextProbe = ({ showEvaluation }: { showEvaluation: boolean }) => {
            const evaluationContext = React.useContext(harness.RuleEditorEvaluationContext);
            React.useEffect(() => {
                latestEvaluationContext = evaluationContext;
            }, [evaluationContext]);
            return showEvaluation ? evaluationContext.createRuleEditorEvaluationComponent("lowerCaseNode") : null;
        };

        const renderResult = render(
            <harness.RuleBlockEvaluationOptionalContext.Provider
                value={{
                    externalEvaluationResults,
                }}
            >
                <harness.RuleBlockEvaluation
                    projectId="project1"
                    ruleBlockTaskId="task1"
                    numberOfEntitiesToShow={5}
                    getPorts={() => ports}
                    getInputExamples={() => []}
                >
                    {(<ContextProbe showEvaluation={false} />) as unknown as React.ReactElement}
                </harness.RuleBlockEvaluation>
            </harness.RuleBlockEvaluationOptionalContext.Provider>,
        );

        await waitFor(() => expect(latestEvaluationContext?.evaluationResultsShown).toBe(true));

        renderResult.rerender(
            <harness.RuleBlockEvaluationOptionalContext.Provider
                value={{
                    externalEvaluationResults,
                }}
            >
                <harness.RuleBlockEvaluation
                    projectId="project1"
                    ruleBlockTaskId="task1"
                    numberOfEntitiesToShow={5}
                    getPorts={() => ports}
                    getInputExamples={() => []}
                >
                    {(<ContextProbe showEvaluation={true} />) as unknown as React.ReactElement}
                </harness.RuleBlockEvaluation>
            </harness.RuleBlockEvaluationOptionalContext.Provider>,
        );

        await waitFor(() => expect(screen.getByTestId("evaluation-lowerCaseNode")).toHaveTextContent("kiel"));
    });

    it("should show the no-results state after an evaluation was started but returned no values", async () => {
        const harness = createRuleBlockEvaluationHarness();
        const ports = [createPort()];
        const inputExamples = [
            {
                id: "example-1",
                inputs: {
                    inputPortA: ["Example value"],
                },
            },
        ];
        const originalTask = createRuleBlockTask(ports);
        let latestEvaluationContext: RuleEditorEvaluationContextProps | undefined;

        const ContextProbe = () => {
            const evaluationContext = React.useContext(harness.RuleEditorEvaluationContext);
            React.useEffect(() => {
                latestEvaluationContext = evaluationContext;
            }, [evaluationContext]);
            return evaluationContext.createRuleEditorEvaluationComponent("lowerCaseNode");
        };

        harness.mockRequestRuleBlockEvaluation.mockResolvedValue({
            data: [],
        });

        render(
            <harness.RuleBlockEvaluation
                projectId="project1"
                ruleBlockTaskId="task1"
                numberOfEntitiesToShow={5}
                getPorts={() => ports}
                getInputExamples={() => inputExamples}
            >
                {(<ContextProbe />) as unknown as React.ReactElement}
            </harness.RuleBlockEvaluation>,
        );

        await waitFor(() => expect(latestEvaluationContext).toBeDefined());

        await act(async () => {
            await latestEvaluationContext!.startEvaluation(
                [createTransformNode(), createInputPortNode()],
                originalTask,
                false,
            );
        });
        await act(async () => {
            latestEvaluationContext!.toggleEvaluationResults(true);
        });

        await waitFor(() =>
            expect(screen.getByTestId("evaluation-lowerCaseNode")).toHaveTextContent(
                "taskViews.ruleBlock.evaluation.noResults",
            ),
        );
    });
});
