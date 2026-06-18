import React from "react";
import "@testing-library/jest-dom";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { IProjectTask } from "@ducks/shared/typings";
import type { StickyNote } from "../../../../../../../libs/gui-elements";
import jestTestUtils from "../../../../test/jestTestUtils";
import ruleTestHelper from "../../shared/rules/tests/ruleTestHelper";
import type { EvaluatedTransformEntity } from "../../transform/transform.types";
import { RuleValidationError } from "../../../shared/RuleEditor/RuleEditor.typings";
import type { IRuleOperatorNode, RuleSaveResult } from "../../../shared/RuleEditor/RuleEditor.typings";
import type { RuleClipboardTask } from "../../../shared/RuleEditor/model/RuleEditorModel.typings";
import type {
    IRuleBlockInputExample,
    RuleBlockSnapshot,
    RuleBlockPort,
    IRuleBlockTaskParameters,
} from "../ruleBlock.types";
import type { IRuleSidebarPreConfiguredOperatorsTabConfig } from "../../../shared/RuleEditor/RuleEditor.typings";
import type { IPreConfiguredRuleOperator } from "../../../shared/RuleEditor/view/sidebar/RuleEditorOperatorSidebar.typings";
import type { InputPortDialogSubmitValue } from "../InputPortDialog";
import type { XYPosition } from "react-flow-renderer/dist/types";
import type { RuleEditorSidebarDropRequest } from "../../../shared/RuleEditor/RuleEditor.typings";
import { RuleEditorBaseProps } from "../../../shared/RuleEditor/RuleEditor";

type CapturedRuleEditorProps = {
    projectId: string;
    taskId: string;
    showRuleOnly?: boolean;
    readOnly?: boolean;
    fetchRuleData: (projectId: string, taskId: string) => Promise<unknown> | unknown;
    saveRule: (
        ruleOperatorNodes: IRuleOperatorNode[],
        stickyNotes: StickyNote[],
        originalTask: IProjectTask<IRuleBlockTaskParameters>,
    ) => Promise<RuleSaveResult>;
    tabs?: unknown[];
    additionalToolBarComponents?: RuleEditorBaseProps["additionalToolBarComponents"];
    captureExternalSavedState?: () => unknown;
    restoreExternalSavedState?: (savedState: unknown) => void;
    extendClipboardCopy?: (task: RuleClipboardTask, nodeIds: string[]) => unknown;
    prepareClipboardPaste?: (task: RuleClipboardTask) => Promise<unknown> | unknown;
    extraRuleNodeMenuItems?: (node: IRuleOperatorNode, closeMenu: () => void) => React.JSX.Element[] | undefined;
    handleSidebarDropRequest?: (request: RuleEditorSidebarDropRequest, position: XYPosition) => boolean;
};

type CapturedInputPortDialogProps = {
    isOpen: boolean;
    mode: "create" | "edit";
    editedPortId?: string;
    initialPort: InputPortDialogSubmitValue;
    existingPorts: RuleBlockPort[];
    persistedPorts: RuleBlockPort[];
    isRuleBlockInUse: boolean;
    onSubmit: (value: InputPortDialogSubmitValue) => void;
    onClose: () => void;
};

type CapturedExampleValuesDialogProps = {
    ports: RuleBlockPort[];
    inputExamples: IRuleBlockInputExample[];
    highlightedPortId?: string;
    selectedExampleIdsForEvaluation: string[];
    onClose: () => void;
    onSelectedExampleIdsForEvaluationChange: (selectedExampleIdsForEvaluation: string[]) => void;
    onApply: (inputExamples: IRuleBlockInputExample[]) => void;
};

type CapturedRuleBlockEvaluationProps = {
    projectId: string;
    ruleBlockTaskId: string;
    numberOfEntitiesToShow: number;
    getPorts: () => RuleBlockPort[];
    getInputExamples: () => IRuleBlockInputExample[];
    getEvaluationInputExamples: () => IRuleBlockInputExample[];
    getSelectedEvaluationExampleIds: () => string[];
    onOpenExampleValuesDialog: (highlightedPortId?: string) => void;
    children: React.ReactNode;
};

const createRuleBlockEditorGuiElementsModule = () => ({
    AlertDialog: jestTestUtils.createAlertDialogMock(),
    Button: jestTestUtils.createButtonMock(({ affirmative, disruptive, tooltip, tooltipProps, loading, ...props }) => ({
        ...props,
        loading,
        includeLoadingState: true,
    })),
    ContextOverlay: jestTestUtils.createContextOverlayMock(),
    ContextMenu: jestTestUtils.createContextMenuMock(),
    Icon: jestTestUtils.createIconMock(),
    IconButton: jestTestUtils.createButtonMock(
        ({ tooltipProps, intent, loading, size, tooltipAsTitle, description, minimal, ...props }) => ({
            ...props,
            text: props.text ?? props.name,
            loading,
            includeLoadingState: false,
        }),
    ),
    MenuItem: jestTestUtils.createMenuItemMock(),
    Notification: jestTestUtils.createNotificationMock(),
    Spacing: jestTestUtils.createChildrenOnlyMock(),
    ToolbarSection: jestTestUtils.createDivPassthroughMock(),
});

const createRuleBlockEditorHarness = () => {
    jest.resetModules();
    jest.doMock("react", () => React);

    const mockRequestTaskData = jest.fn();
    const mockRequestRelatedItems = jest.fn();
    const mockRequestUpdateProjectTask = jest.fn();
    const mockRequestRuleOperatorPluginsDetails = jest.fn();
    const mockRegisterError = jest.fn();
    let mockCapturedRuleEditorProps: CapturedRuleEditorProps | undefined;
    let mockLastInputPortDialogProps: CapturedInputPortDialogProps | undefined;
    let mockLastExampleValuesDialogProps: CapturedExampleValuesDialogProps | undefined;
    let mockLastRuleBlockEvaluationProps: CapturedRuleBlockEvaluationProps | undefined;
    const mockRuleEditorApi = {
        ruleOperatorNodes: jest.fn<IRuleOperatorNode[], []>(() => []),
        startChangeTransaction: jest.fn(),
        executeExternalRuleModelChange: jest.fn(),
        updateRuleOperatorNodeMetaData: jest.fn(),
        deleteNodes: jest.fn(),
        addNodeByPlugin: jest.fn(),
    };

    jestTestUtils.mockReactI18next(jestTestUtils.testTranslate);
    jestTestUtils.mockI18next(jestTestUtils.testTranslate);
    jest.doMock("../../../../../../../libs/gui-elements", createRuleBlockEditorGuiElementsModule);
    jest.doMock("@ducks/shared/requests", () => ({
        requestTaskData: (...args) => mockRequestTaskData(...args),
        requestRelatedItems: (...args) => mockRequestRelatedItems(...args),
    }));
    jest.doMock("@ducks/workspace/requests", () => ({
        requestUpdateProjectTask: (...args) => mockRequestUpdateProjectTask(...args),
    }));
    jest.doMock("@ducks/common/requests", () => ({
        requestRuleOperatorPluginsDetails: (...args) => mockRequestRuleOperatorPluginsDetails(...args),
    }));
    jest.doMock("../../../../hooks/useErrorHandler", () => ({
        __esModule: true,
        default: () => ({
            registerError: mockRegisterError,
        }),
    }));
    jest.doMock("../RuleBlockEvaluation", () => ({
        __esModule: true,
        default: (props) => {
            mockLastRuleBlockEvaluationProps = props;
            return <>{props.children}</>;
        },
    }));
    jest.doMock("../../../shared/RuleEditor/RuleEditor", () => {
        const React = require("react");
        return {
            __esModule: true,
            default: React.forwardRef((props, ref) => {
                mockCapturedRuleEditorProps = props;
                React.useImperativeHandle(ref, () => mockRuleEditorApi, []);
                React.useEffect(() => {
                    void props.fetchRuleData(props.projectId, props.taskId);
                }, []);
                return <div data-testid="rule-editor-mock" />;
            }),
        };
    });
    jest.doMock("../InputPortDialog", () => ({
        __esModule: true,
        default: (props) => {
            mockLastInputPortDialogProps = props;
            return (
                <div
                    data-testid="input-port-dialog"
                    data-open={String(props.isOpen)}
                    data-mode={props.mode}
                    data-edited-port-id={props.editedPortId ?? ""}
                />
            );
        },
    }));
    jest.doMock("../ExampleValuesDialog", () => ({
        __esModule: true,
        default: function MockExampleValuesDialog(props) {
            const React = require("react");
            React.useEffect(() => {
                mockLastExampleValuesDialogProps = props;
                return () => {
                    mockLastExampleValuesDialogProps = undefined;
                };
            }, [props]);
            return <div data-testid="example-values-dialog" />;
        },
    }));

    const { RuleBlockEditor, RuleBlockEditorOptionalContext } =
        require("../RuleBlockEditor") as typeof import("../RuleBlockEditor");
    const { RuleBlockEvaluationOptionalContext } =
        require("../RuleBlockEvaluationOptionalContext") as typeof import("../RuleBlockEvaluationOptionalContext");
    return {
        RuleBlockEditor,
        RuleBlockEditorOptionalContext,
        RuleBlockEvaluationOptionalContext,
        mockRequestTaskData,
        mockRequestRelatedItems,
        mockRequestUpdateProjectTask,
        mockRuleEditorApi,
        getCapturedRuleEditorProps: () => mockCapturedRuleEditorProps,
        getLastInputPortDialogProps: () => mockLastInputPortDialogProps,
        getLastExampleValuesDialogProps: () => mockLastExampleValuesDialogProps,
        getLastRuleBlockEvaluationProps: () => mockLastRuleBlockEvaluationProps,
    };
};

type RuleBlockEditorHarness = ReturnType<typeof createRuleBlockEditorHarness>;

const createPersistedPort = (overrides: Partial<RuleBlockPort> = {}): RuleBlockPort =>
    ruleTestHelper.createRuleBlockPort({
        description: "Input description",
        displayOrder: 2,
        ...overrides,
    });

const createRuleBlockTask = (
    ports: RuleBlockPort[] = [],
    inputExamples: IRuleBlockInputExample[] = [],
): IProjectTask<IRuleBlockTaskParameters> =>
    ruleTestHelper.createRuleBlockTask(ports, inputExamples) as IProjectTask<IRuleBlockTaskParameters>;

const createRuleBlockInspectionSnapshot = (ports: RuleBlockPort[] = []): RuleBlockSnapshot =>
    ruleTestHelper.createRuleBlockInspectionSnapshot({
        ports,
    });

const createInputPortNode = (overrides: Partial<IRuleOperatorNode> = {}): IRuleOperatorNode =>
    ruleTestHelper.createInputPortNode({
        nodeId: "nodeA",
        label: "Input port node",
        portSpecification: {
            type: "count",
            minInputPorts: 0,
        },
        ...overrides,
    });

const createClipboardTask = (overrides: Partial<RuleClipboardTask> = {}): RuleClipboardTask => ({
    data: {
        nodes: [],
        edges: [],
        ...(overrides.data ?? {}),
    },
    metaData: {
        project: "project1",
        task: "task1",
        ...(overrides.metaData ?? {}),
    },
    ...(overrides.editorData !== undefined ? { editorData: overrides.editorData } : {}),
});

const getInputPortsTab = (
    tabs: unknown[] | undefined,
): IRuleSidebarPreConfiguredOperatorsTabConfig<RuleBlockPort | { type: "create" }> =>
    tabs?.find((tab) => (tab as { id?: string }).id === "inputPorts") as IRuleSidebarPreConfiguredOperatorsTabConfig<
        RuleBlockPort | { type: "create" }
    >;

const fetchInputPortTabItems = async (
    tab: IRuleSidebarPreConfiguredOperatorsTabConfig<RuleBlockPort | { type: "create" }>,
) => {
    const items = await tab.fetchOperators("en");
    expect(items).toBeDefined();
    return items!;
};

const renderOperatorActions = (operator: IPreConfiguredRuleOperator) =>
    render(<>{Array.isArray(operator.actions) ? operator.actions : operator.actions ? [operator.actions] : null}</>);

const renderRuleBlockEditor = async ({
    ports = [],
    inputExamples = [],
    ruleOperatorNodes = [],
    relatedItemsTotal = 0,
    relatedItemsError = false,
    externalSnapshot,
    externalEvaluationResults,
    showRuleOnly = false,
    readOnly = false,
}: {
    ports?: RuleBlockPort[];
    inputExamples?: IRuleBlockInputExample[];
    ruleOperatorNodes?: IRuleOperatorNode[];
    relatedItemsTotal?: number;
    relatedItemsError?: boolean;
    externalSnapshot?: RuleBlockSnapshot;
    externalEvaluationResults?: EvaluatedTransformEntity[];
    showRuleOnly?: boolean;
    readOnly?: boolean;
} = {}) => {
    const harness = createRuleBlockEditorHarness();
    harness.mockRequestTaskData.mockResolvedValue({
        data: createRuleBlockTask(ports, inputExamples),
    });
    if (relatedItemsError) {
        harness.mockRequestRelatedItems.mockRejectedValue(new Error("fetch failed"));
    } else {
        harness.mockRequestRelatedItems.mockResolvedValue({
            data: {
                total: relatedItemsTotal,
            },
        });
    }
    harness.mockRuleEditorApi.ruleOperatorNodes.mockReturnValue(ruleOperatorNodes);

    const editorElement = (
        <harness.RuleBlockEditor projectId="project1" ruleBlockTaskId="task1" instanceId="instance1" />
    );
    const maybeSnapshotWrappedEditor = externalSnapshot ? (
        <harness.RuleBlockEditorOptionalContext.Provider
            value={{
                ruleBlockSnapshot: externalSnapshot,
                showRuleOnly,
                readOnly,
            }}
        >
            {editorElement}
        </harness.RuleBlockEditorOptionalContext.Provider>
    ) : (
        editorElement
    );
    render(
        externalEvaluationResults !== undefined ? (
            <harness.RuleBlockEvaluationOptionalContext.Provider value={{ externalEvaluationResults }}>
                {maybeSnapshotWrappedEditor}
            </harness.RuleBlockEvaluationOptionalContext.Provider>
        ) : (
            maybeSnapshotWrappedEditor
        ),
    );

    await waitFor(() => expect(harness.getCapturedRuleEditorProps()).toBeDefined());

    return {
        ...harness,
        getRuleEditorProps: () => {
            const capturedProps = harness.getCapturedRuleEditorProps();
            expect(capturedProps).toBeDefined();
            return capturedProps!;
        },
        getInputPortDialogProps: () => harness.getLastInputPortDialogProps(),
        getExampleValuesDialogProps: () => harness.getLastExampleValuesDialogProps(),
        getRuleBlockEvaluationProps: () => harness.getLastRuleBlockEvaluationProps(),
    };
};

const renderInputPortSidebarActions = async (harness: RuleBlockEditorHarness, itemIndex: number) => {
    await waitFor(() => expect(harness.getCapturedRuleEditorProps()?.tabs).toBeDefined());
    const tab = getInputPortsTab(harness.getCapturedRuleEditorProps()!.tabs);
    const items = await fetchInputPortTabItems(tab);
    const operator = tab.convertToOperator(items[itemIndex]);
    return {
        items,
        operator,
        ...renderOperatorActions(operator),
    };
};

const openInputPortNodeMenu = async (
    harness: RuleBlockEditorHarness,
    node: IRuleOperatorNode = createInputPortNode(),
) => {
    await waitFor(() => expect(harness.getCapturedRuleEditorProps()?.extraRuleNodeMenuItems).toBeDefined());
    const closeMenu = jest.fn();
    const menuItems = harness.getCapturedRuleEditorProps()!.extraRuleNodeMenuItems!(node, closeMenu);
    return {
        closeMenu,
        ...render(<>{menuItems}</>),
    };
};

const renderToolbarActions = async (harness: RuleBlockEditorHarness) => {
    await waitFor(() => expect(harness.getCapturedRuleEditorProps()?.additionalToolBarComponents).toBeDefined());
    return render(
        <>
            {harness.getCapturedRuleEditorProps()!.additionalToolBarComponents!("portmenu")}
            {harness.getCapturedRuleEditorProps()!.additionalToolBarComponents!("ruleblockusagestatus")}
        </>,
    );
};

describe("RuleBlockEditor", () => {
    it("should render an injected snapshot in canvas-only mode without fetching the task or evaluation state", async () => {
        const snapshotPort = createPersistedPort({
            id: "snapshotPort",
            label: "Snapshot port",
        });
        const editor = await renderRuleBlockEditor({
            externalSnapshot: createRuleBlockInspectionSnapshot([snapshotPort]),
            showRuleOnly: true,
        });

        expect(editor.mockRequestTaskData).not.toHaveBeenCalled();
        expect(editor.mockRequestRelatedItems).not.toHaveBeenCalled();
        expect(editor.getRuleBlockEvaluationProps()).toBeUndefined();
        expect(screen.queryByTestId("input-port-dialog")).not.toBeInTheDocument();
        expect(screen.queryByTestId("example-values-dialog")).not.toBeInTheDocument();
        expect(editor.getRuleEditorProps().showRuleOnly).toBe(true);
        expect(editor.getRuleEditorProps().readOnly).toBe(false);
        expect(editor.getRuleEditorProps().extraRuleNodeMenuItems).toBeDefined();
        expect(editor.getRuleEditorProps().captureExternalSavedState?.()).toStrictEqual({
            ports: [snapshotPort],
            inputExamples: [],
        });
    });

    it("should wrap an injected snapshot in the evaluation path even when external evaluation results are empty", async () => {
        const editor = await renderRuleBlockEditor({
            externalSnapshot: createRuleBlockInspectionSnapshot([createPersistedPort()]),
            externalEvaluationResults: [],
            showRuleOnly: true,
        });

        expect(editor.getRuleBlockEvaluationProps()).toMatchObject({
            projectId: "project1",
            ruleBlockTaskId: "task1",
        });
        expect(editor.mockRequestTaskData).not.toHaveBeenCalled();
        expect(editor.mockRequestRelatedItems).not.toHaveBeenCalled();
    });

    it("should return a validation error when saving malformed input-port nodes without a logical port ID", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [createPersistedPort()],
        });

        const saveResult = await editor.getRuleEditorProps().saveRule(
            [
                createInputPortNode({
                    parameters: {
                        portId: "   ",
                    },
                }),
            ],
            [],
            createRuleBlockTask([createPersistedPort()]),
        );

        expect((saveResult as RuleValidationError).isRuleValidationError).toBe(true);
        expect((saveResult as RuleValidationError).nodeErrors).toStrictEqual([
            {
                nodeId: "nodeA",
                message: jestTestUtils.testTranslate("taskViews.ruleBlock.errors.missingPortId"),
            },
        ]);
        expect(editor.mockRequestRelatedItems).toHaveBeenCalledTimes(1);
        expect(editor.mockRequestUpdateProjectTask).not.toHaveBeenCalled();
    });

    it("should show the usage status control only when the rule block is in use and allow refreshing it", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [createPersistedPort()],
            relatedItemsTotal: 2,
        });
        await renderToolbarActions(editor);

        await waitFor(() =>
            expect(screen.getByTestId("context-overlay")).toHaveTextContent("taskViews.ruleBlock.usageInUse"),
        );
        expect(screen.getByRole("button", { name: "taskViews.ruleBlock.refreshUsage" })).toHaveTextContent(
            "state-info",
        );
        expect(editor.mockRequestRelatedItems).toHaveBeenCalledTimes(1);

        fireEvent.click(screen.getByRole("button", { name: "taskViews.ruleBlock.refreshUsage" }));

        await waitFor(() => expect(editor.mockRequestRelatedItems).toHaveBeenCalledTimes(2));
    });

    it("should show a warning icon when refreshing rule block usage failed", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [createPersistedPort()],
            relatedItemsError: true,
        });
        await renderToolbarActions(editor);

        await waitFor(() =>
            expect(screen.getByTestId("context-overlay")).toHaveTextContent("taskViews.ruleBlock.usageRefreshError"),
        );
        expect(screen.getByRole("button", { name: "taskViews.ruleBlock.refreshUsage" })).toHaveTextContent(
            "state-warning",
        );
        expect(editor.mockRequestRelatedItems).toHaveBeenCalledTimes(1);
    });

    it("should not show the usage status control when the rule block is not in use", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [createPersistedPort()],
            relatedItemsTotal: 0,
        });
        await renderToolbarActions(editor);

        expect(screen.queryByRole("button", { name: "taskViews.ruleBlock.refreshUsage" })).not.toBeInTheDocument();
        expect(screen.queryByTestId("context-overlay")).not.toBeInTheDocument();
    });

    it("should open the example values dialog from the evaluation config action and apply example changes in one external transaction", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [createPersistedPort({ id: "inputPortA", displayOrder: 1 })],
        });

        await act(async () => {
            editor.getRuleBlockEvaluationProps()!.onOpenExampleValuesDialog();
        });

        await waitFor(() =>
            expect(editor.getExampleValuesDialogProps()).toMatchObject({
                ports: [createPersistedPort({ id: "inputPortA", displayOrder: 1 })],
                inputExamples: [],
                selectedExampleIdsForEvaluation: [],
            }),
        );

        await act(async () => {
            editor.getExampleValuesDialogProps()!.onApply([
                {
                    id: "example-1",
                    inputs: {
                        inputPortA: ["Example value"],
                    },
                },
            ]);
        });

        expect(editor.mockRuleEditorApi.startChangeTransaction).toHaveBeenCalledTimes(1);
        expect(editor.mockRuleEditorApi.executeExternalRuleModelChange).toHaveBeenCalledTimes(1);

        const updateChange = editor.mockRuleEditorApi.executeExternalRuleModelChange.mock.calls[0][0];
        await act(async () => {
            updateChange.do();
        });

        expect(editor.getRuleEditorProps().captureExternalSavedState!()).toStrictEqual({
            ports: [createPersistedPort({ id: "inputPortA", displayOrder: 1 })],
            inputExamples: [
                {
                    id: "example-1",
                    label: undefined,
                    inputs: {
                        inputPortA: ["Example value"],
                    },
                },
            ],
        });
        await waitFor(() => expect(editor.getExampleValuesDialogProps()).toBeUndefined());
    });

    it("should evaluate only the examples selected in the example dialog until the selection is cleared", async () => {
        const inputExamples = [
            ruleTestHelper.createRuleBlockInputExample({
                id: "example-1",
                inputs: {
                    inputPortA: ["First value"],
                },
            }),
            ruleTestHelper.createRuleBlockInputExample({
                id: "example-2",
                inputs: {
                    inputPortA: ["Second value"],
                },
            }),
        ];
        const editor = await renderRuleBlockEditor({
            ports: [createPersistedPort({ id: "inputPortA", displayOrder: 1 })],
            inputExamples,
        });

        await act(async () => {
            editor.getRuleBlockEvaluationProps()!.onOpenExampleValuesDialog();
        });

        await act(async () => {
            editor.getExampleValuesDialogProps()!.onSelectedExampleIdsForEvaluationChange(["example-2"]);
            editor.getExampleValuesDialogProps()!.onClose();
        });

        expect(editor.getRuleBlockEvaluationProps()!.getSelectedEvaluationExampleIds()).toStrictEqual(["example-2"]);
        expect(editor.getRuleBlockEvaluationProps()!.getEvaluationInputExamples()).toMatchObject([inputExamples[1]]);

        await act(async () => {
            editor.getRuleBlockEvaluationProps()!.onOpenExampleValuesDialog();
        });

        await act(async () => {
            editor.getExampleValuesDialogProps()!.onSelectedExampleIdsForEvaluationChange([]);
            editor.getExampleValuesDialogProps()!.onClose();
        });

        expect(editor.getRuleBlockEvaluationProps()!.getSelectedEvaluationExampleIds()).toStrictEqual([]);
        expect(editor.getRuleBlockEvaluationProps()!.getEvaluationInputExamples()).toMatchObject(inputExamples);
    });

    it("should disable deleting persisted input ports when the rule block is in use", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [createPersistedPort()],
            relatedItemsTotal: 1,
        });
        const { unmount } = await renderInputPortSidebarActions(editor, 1);

        expect(screen.getByRole("button", { name: "item-remove" })).toBeDisabled();
        expect(editor.getInputPortDialogProps()?.isRuleBlockInUse).toBe(true);

        unmount();
    });

    it("should open the create input port dialog from a dropped sidebar creation request and cancel without changing the model", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [createPersistedPort()],
        });

        await act(async () => {
            expect(
                editor.getRuleEditorProps().handleSidebarDropRequest?.({ type: "createInputPort" }, { x: 120, y: 180 }),
            ).toBe(true);
        });

        await waitFor(() =>
            expect(editor.getInputPortDialogProps()).toMatchObject({
                mode: "create",
            }),
        );

        await act(async () => {
            editor.getInputPortDialogProps()!.onClose();
        });

        await waitFor(() =>
            expect(editor.getInputPortDialogProps()).toMatchObject({
                isOpen: false,
            }),
        );
        expect(editor.mockRuleEditorApi.startChangeTransaction).not.toHaveBeenCalled();
        expect(editor.mockRuleEditorApi.executeExternalRuleModelChange).not.toHaveBeenCalled();
        expect(editor.mockRuleEditorApi.addNodeByPlugin).not.toHaveBeenCalled();
    });

    it("should create only the logical input port when the create dialog is opened via the sidebar action button", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [createPersistedPort({ id: "existingPort", label: "Existing input", displayOrder: 1 })],
        });
        const { getByRole, unmount } = await renderInputPortSidebarActions(editor, 0);

        await act(async () => {
            fireEvent.click(getByRole("button", { name: "item-add-artefact" }));
        });

        await waitFor(() =>
            expect(editor.getInputPortDialogProps()).toMatchObject({
                mode: "create",
            }),
        );

        await act(async () => {
            editor.getInputPortDialogProps()!.onSubmit({
                label: "Created by button",
                description: "Created from sidebar action button",
                displayOrder: 2,
                deprecated: false,
            });
        });

        expect(editor.mockRuleEditorApi.startChangeTransaction).toHaveBeenCalledTimes(1);
        expect(editor.mockRuleEditorApi.executeExternalRuleModelChange).toHaveBeenCalledTimes(1);
        expect(editor.mockRuleEditorApi.addNodeByPlugin).not.toHaveBeenCalled();

        const externalChange = editor.mockRuleEditorApi.executeExternalRuleModelChange.mock.calls[0][0];
        await act(async () => {
            externalChange.do();
        });

        const savedState = editor.getRuleEditorProps().captureExternalSavedState?.() as
            | { ports: RuleBlockPort[]; inputExamples: IRuleBlockInputExample[] }
            | undefined;
        expect(savedState).toMatchObject({
            ports: [
                createPersistedPort({ id: "existingPort", label: "Existing input", displayOrder: 1 }),
                {
                    label: "Created by button",
                    description: "Created from sidebar action button",
                    displayOrder: 2,
                    deprecated: false,
                },
            ],
            inputExamples: [],
        });
        expect(savedState?.ports[1].id).toEqual(expect.any(String));

        unmount();
    });

    it("should create the logical input port and a dropped node instance when the create dialog is confirmed after a drop request", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [createPersistedPort({ id: "existingPort", label: "Existing input", displayOrder: 1 })],
        });

        await act(async () => {
            expect(
                editor.getRuleEditorProps().handleSidebarDropRequest?.({ type: "createInputPort" }, { x: 320, y: 240 }),
            ).toBe(true);
        });
        await waitFor(() => expect(editor.getInputPortDialogProps()).toBeDefined());

        await act(async () => {
            editor.getInputPortDialogProps()!.onSubmit({
                label: "Created by drop",
                description: "Created from dragged sidebar item",
                displayOrder: 2,
                deprecated: false,
            });
        });

        expect(editor.mockRuleEditorApi.startChangeTransaction).toHaveBeenCalledTimes(1);
        expect(editor.mockRuleEditorApi.executeExternalRuleModelChange).toHaveBeenCalledTimes(1);
        expect(editor.mockRuleEditorApi.addNodeByPlugin).toHaveBeenCalledTimes(1);
        const createdPortId = editor.mockRuleEditorApi.addNodeByPlugin.mock.calls[0][3].portId;
        expect(typeof createdPortId).toBe("string");

        const externalChange = editor.mockRuleEditorApi.executeExternalRuleModelChange.mock.calls[0][0];
        await act(async () => {
            externalChange.do();
        });

        expect(editor.mockRuleEditorApi.addNodeByPlugin).toHaveBeenCalledWith(
            "InputPortOperator",
            "inputPort",
            { x: 320, y: 240 },
            { portId: createdPortId },
            {
                label: "Created by drop",
                description: "Created from dragged sidebar item",
                tags: ["taskViews.ruleBlock.inputPortsTab", "2"],
            },
            true,
        );
        expect(editor.getRuleEditorProps().captureExternalSavedState?.()).toStrictEqual({
            ports: [
                createPersistedPort({ id: "existingPort", label: "Existing input", displayOrder: 1 }),
                ruleTestHelper.createRuleBlockPort({
                    id: createdPortId,
                    label: "Created by drop",
                    description: "Created from dragged sidebar item",
                    displayOrder: 2,
                    deprecated: false,
                }),
            ],
            inputExamples: [],
        });
    });

    it("should open the edit dialog when the input-port node menu item is clicked", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [createPersistedPort()],
        });

        await waitFor(() => {
            expect(editor.getRuleEditorProps().extraRuleNodeMenuItems).toBeDefined();
            expect(editor.getInputPortDialogProps()?.editedPortId).toBeUndefined();
        });

        const { closeMenu, unmount } = await openInputPortNodeMenu(editor);

        fireEvent.click(screen.getByRole("button", { name: "Edit input port" }));

        expect(closeMenu).toHaveBeenCalled();
        await waitFor(() =>
            expect(editor.getInputPortDialogProps()).toMatchObject({
                isOpen: true,
                mode: "edit",
                editedPortId: "inputPortA",
            }),
        );

        unmount();
    });

    it("should open the example values dialog from the input-port node menu and highlight the targeted port", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [createPersistedPort()],
        });

        const { closeMenu, unmount } = await openInputPortNodeMenu(editor);

        fireEvent.click(screen.getByRole("button", { name: "Edit example values" }));

        expect(closeMenu).toHaveBeenCalled();
        await waitFor(() =>
            expect(editor.getExampleValuesDialogProps()).toMatchObject({
                highlightedPortId: "inputPortA",
            }),
        );

        unmount();
    });

    it("should create an input port via the sidebar entry and register one external transaction", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [createPersistedPort({ displayOrder: 2 })],
        });

        const { unmount } = await renderInputPortSidebarActions(editor, 0);

        fireEvent.click(screen.getByRole("button", { name: "item-add-artefact" }));

        await waitFor(() =>
            expect(editor.getInputPortDialogProps()).toMatchObject({
                isOpen: true,
                mode: "create",
                initialPort: expect.objectContaining({
                    displayOrder: 3,
                }),
            }),
        );

        await act(async () => {
            editor.getInputPortDialogProps()!.onSubmit({
                label: "Created input",
                description: "Created description",
                displayOrder: 3,
                deprecated: false,
            });
        });

        expect(editor.mockRuleEditorApi.startChangeTransaction).toHaveBeenCalledTimes(1);
        expect(editor.mockRuleEditorApi.executeExternalRuleModelChange).toHaveBeenCalledTimes(1);
        expect(editor.mockRuleEditorApi.updateRuleOperatorNodeMetaData).not.toHaveBeenCalled();
        const createChange = editor.mockRuleEditorApi.executeExternalRuleModelChange.mock.calls[0][0];
        await act(async () => {
            createChange.do();
        });
        expect(
            (editor.getRuleEditorProps().captureExternalSavedState!() as { ports: RuleBlockPort[] }).ports,
        ).toContainEqual(
            expect.objectContaining({
                label: "Created input",
                description: "Created description",
                displayOrder: 3,
                deprecated: false,
            }),
        );
        await waitFor(() =>
            expect(editor.getInputPortDialogProps()).toMatchObject({
                isOpen: false,
            }),
        );

        unmount();
    });

    it("should update an input port via the sidebar entry and sync node metadata", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [createPersistedPort()],
            ruleOperatorNodes: [createInputPortNode()],
        });
        const { unmount } = await renderInputPortSidebarActions(editor, 1);

        fireEvent.click(screen.getByRole("button", { name: "item-edit" }));

        await waitFor(() =>
            expect(editor.getInputPortDialogProps()).toMatchObject({
                isOpen: true,
                mode: "edit",
                editedPortId: "inputPortA",
            }),
        );

        await act(async () => {
            editor.getInputPortDialogProps()!.onSubmit({
                label: "Updated input",
                description: "Updated description",
                displayOrder: 4,
                deprecated: false,
            });
        });

        expect(editor.mockRuleEditorApi.startChangeTransaction).toHaveBeenCalledTimes(1);
        expect(editor.mockRuleEditorApi.executeExternalRuleModelChange).toHaveBeenCalledTimes(1);
        expect(editor.mockRuleEditorApi.updateRuleOperatorNodeMetaData).toHaveBeenCalledTimes(1);
        const updateChange = editor.mockRuleEditorApi.executeExternalRuleModelChange.mock.calls[0][0];
        await act(async () => {
            updateChange.do();
        });
        expect(
            (editor.getRuleEditorProps().captureExternalSavedState!() as { ports: RuleBlockPort[] }).ports,
        ).toContainEqual({
            id: "inputPortA",
            label: "Updated input",
            description: "Updated description",
            displayOrder: 4,
            deprecated: false,
        });
        expect(editor.mockRuleEditorApi.updateRuleOperatorNodeMetaData).toHaveBeenCalledWith(
            ["nodeA"],
            expect.any(Function),
        );

        unmount();
    });

    it("should delete a used input port only after confirmation and remove matching nodes in one transaction", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [createPersistedPort()],
            ruleOperatorNodes: [createInputPortNode()],
        });
        const { unmount } = await renderInputPortSidebarActions(editor, 1);

        fireEvent.click(screen.getByRole("button", { name: "item-remove" }));

        expect(screen.getByTestId("alert-dialog")).toBeInTheDocument();
        expect(editor.mockRuleEditorApi.startChangeTransaction).not.toHaveBeenCalled();

        fireEvent.click(screen.getByRole("button", { name: "Delete" }));

        expect(editor.mockRuleEditorApi.startChangeTransaction).toHaveBeenCalledTimes(1);
        expect(editor.mockRuleEditorApi.executeExternalRuleModelChange).toHaveBeenCalledTimes(1);
        expect(editor.mockRuleEditorApi.deleteNodes).toHaveBeenCalledWith(["nodeA"]);

        unmount();
    });

    it("should delete an unused input port immediately without opening the confirmation dialog", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [createPersistedPort()],
        });
        const { unmount } = await renderInputPortSidebarActions(editor, 1);

        fireEvent.click(screen.getByRole("button", { name: "item-remove" }));

        expect(screen.queryByTestId("alert-dialog")).not.toBeInTheDocument();
        expect(editor.mockRuleEditorApi.startChangeTransaction).toHaveBeenCalledTimes(1);
        expect(editor.mockRuleEditorApi.executeExternalRuleModelChange).toHaveBeenCalledTimes(1);
        expect(editor.mockRuleEditorApi.deleteNodes).not.toHaveBeenCalled();

        unmount();
    });

    it("should prune deleted-port example values from the saved state bridge and restore them on undo", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [
                createPersistedPort({ id: "inputPortA", displayOrder: 1 }),
                createPersistedPort({ id: "inputPortB", label: "Input B", displayOrder: 2 }),
            ],
            inputExamples: [
                {
                    id: "example-1",
                    label: "Example 1",
                    inputs: {
                        inputPortA: ["Value A"],
                        inputPortB: ["Value B"],
                    },
                },
            ],
        });
        const { unmount } = await renderInputPortSidebarActions(editor, 2);

        fireEvent.click(screen.getByRole("button", { name: "item-remove" }));

        expect(editor.mockRuleEditorApi.executeExternalRuleModelChange).toHaveBeenCalledTimes(1);
        const deleteChange = editor.mockRuleEditorApi.executeExternalRuleModelChange.mock.calls[0][0];

        await act(async () => {
            deleteChange.do();
        });

        expect(editor.getRuleEditorProps().captureExternalSavedState!()).toStrictEqual({
            ports: [createPersistedPort({ id: "inputPortA", displayOrder: 1 })],
            inputExamples: [
                {
                    id: "example-1",
                    label: "Example 1",
                    inputs: {
                        inputPortA: ["Value A"],
                    },
                },
            ],
        });

        await act(async () => {
            deleteChange.undo();
        });

        expect(editor.getRuleEditorProps().captureExternalSavedState!()).toStrictEqual({
            ports: [
                createPersistedPort({ id: "inputPortA", displayOrder: 1 }),
                createPersistedPort({ id: "inputPortB", label: "Input B", displayOrder: 2 }),
            ],
            inputExamples: [
                {
                    id: "example-1",
                    label: "Example 1",
                    inputs: {
                        inputPortA: ["Value A"],
                        inputPortB: ["Value B"],
                    },
                },
            ],
        });

        unmount();
    });

    it("should normalize input-port order via the toolbar action and sync changed node metadata", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [
                createPersistedPort({ id: "inputPortA", displayOrder: 2 }),
                createPersistedPort({ id: "inputPortB", label: "Input B", displayOrder: 5 }),
            ],
            ruleOperatorNodes: [
                createInputPortNode({ nodeId: "nodeA", parameters: { portId: "inputPortA" } }),
                createInputPortNode({ nodeId: "nodeB", parameters: { portId: "inputPortB" } }),
            ],
        });
        await renderToolbarActions(editor);

        fireEvent.click(screen.getByRole("button", { name: "taskViews.ruleBlock.normalizePortOrder" }));

        expect(editor.mockRuleEditorApi.startChangeTransaction).toHaveBeenCalledTimes(1);
        expect(editor.mockRuleEditorApi.executeExternalRuleModelChange).toHaveBeenCalledTimes(1);
        expect(editor.mockRuleEditorApi.updateRuleOperatorNodeMetaData).toHaveBeenCalledTimes(2);
        expect(editor.mockRuleEditorApi.updateRuleOperatorNodeMetaData).toHaveBeenNthCalledWith(
            1,
            ["nodeA"],
            expect.any(Function),
        );
        expect(editor.mockRuleEditorApi.updateRuleOperatorNodeMetaData).toHaveBeenNthCalledWith(
            2,
            ["nodeB"],
            expect.any(Function),
        );
    });

    it("should disable the normalize action when the current input-port order is already normalized", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [
                createPersistedPort({ id: "inputPortA", displayOrder: 1 }),
                createPersistedPort({ id: "inputPortB", label: "Input B", displayOrder: 2 }),
            ],
        });

        await renderToolbarActions(editor);

        expect(screen.getByRole("button", { name: "taskViews.ruleBlock.normalizePortOrder" })).toBeDisabled();
    });

    it("should expose the current ports through the saved-state bridge and restore them through applyPorts", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [
                createPersistedPort({ id: "inputPortB", label: "Input B", displayOrder: 2 }),
                createPersistedPort({ id: "inputPortA", label: "Input A", displayOrder: 1 }),
            ],
        });

        await waitFor(() => {
            expect(editor.getRuleEditorProps().captureExternalSavedState).toBeDefined();
            expect(editor.getRuleEditorProps().restoreExternalSavedState).toBeDefined();
        });

        expect(editor.getRuleEditorProps().captureExternalSavedState!()).toStrictEqual({
            ports: [
                createPersistedPort({ id: "inputPortA", label: "Input A", displayOrder: 1 }),
                createPersistedPort({ id: "inputPortB", label: "Input B", displayOrder: 2 }),
            ],
            inputExamples: [],
        });

        await act(async () => {
            editor.getRuleEditorProps().restoreExternalSavedState!({
                ports: [
                    createPersistedPort({ id: "inputPortB", label: "Restored B", displayOrder: 4 }),
                    createPersistedPort({ id: "inputPortA", label: "Restored A", displayOrder: 2 }),
                ],
                inputExamples: [
                    {
                        id: "example-restored",
                        label: undefined,
                        inputs: {
                            inputPortA: ["Restored value"],
                        },
                    },
                ],
            });
        });

        await waitFor(() =>
            expect(editor.getRuleEditorProps().captureExternalSavedState!()).toStrictEqual({
                ports: [
                    createPersistedPort({ id: "inputPortA", label: "Restored A", displayOrder: 2 }),
                    createPersistedPort({ id: "inputPortB", label: "Restored B", displayOrder: 4 }),
                ],
                inputExamples: [
                    {
                        id: "example-restored",
                        label: undefined,
                        inputs: {
                            inputPortA: ["Restored value"],
                        },
                    },
                ],
            }),
        );
    });

    it("should prune restored example values that reference ports outside the restored port set", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [createPersistedPort({ id: "inputPortA", label: "Input A", displayOrder: 1 })],
        });

        await waitFor(() => expect(editor.getRuleEditorProps().restoreExternalSavedState).toBeDefined());

        await act(async () => {
            editor.getRuleEditorProps().restoreExternalSavedState!({
                ports: [createPersistedPort({ id: "inputPortA", label: "Restored A", displayOrder: 1 })],
                inputExamples: [
                    {
                        id: "example-restored",
                        label: "Restored example",
                        inputs: {
                            inputPortA: ["Kept value"],
                            removedPort: ["Removed value"],
                        },
                    },
                ],
            });
        });

        await waitFor(() =>
            expect(editor.getRuleEditorProps().captureExternalSavedState!()).toStrictEqual({
                ports: [createPersistedPort({ id: "inputPortA", label: "Restored A", displayOrder: 1 })],
                inputExamples: [
                    {
                        id: "example-restored",
                        label: "Restored example",
                        inputs: {
                            inputPortA: ["Kept value"],
                        },
                    },
                ],
            }),
        );
    });

    it("should extend clipboard copies with logical input-port definitions of the selected nodes", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [
                createPersistedPort({ id: "inputPortA", label: "Input A", displayOrder: 1 }),
                createPersistedPort({ id: "inputPortB", label: "Input B", displayOrder: 2 }),
            ],
            ruleOperatorNodes: [
                createInputPortNode({ nodeId: "nodeA", parameters: { portId: "inputPortA" } }),
                createInputPortNode({ nodeId: "nodeB", parameters: { portId: "inputPortB" } }),
            ],
        });

        await waitFor(() => expect(editor.getRuleEditorProps().extendClipboardCopy).toBeDefined());

        expect(editor.getRuleEditorProps().extendClipboardCopy!(createClipboardTask(), ["nodeB"])).toStrictEqual({
            inputPorts: [createPersistedPort({ id: "inputPortB", label: "Input B", displayOrder: 2 })],
        });
    });

    it("should create new input ports when preparing a clipboard paste from another rule block", async () => {
        const editor = await renderRuleBlockEditor({
            ports: [createPersistedPort({ id: "existingPort", label: "Existing", displayOrder: 4 })],
        });

        await waitFor(() => expect(editor.getRuleEditorProps().prepareClipboardPaste).toBeDefined());

        const preparedPaste = await editor.getRuleEditorProps().prepareClipboardPaste!(
            createClipboardTask({
                data: {
                    nodes: [
                        {
                            nodeId: "externalInputNode",
                            pluginType: "InputPortOperator",
                            pluginId: "inputPort",
                            position: { x: 0, y: 0 },
                            parameters: {
                                portId: "externalPort",
                            },
                            inputHandleIds: [],
                        },
                    ],
                    edges: [],
                },
                metaData: {
                    project: "project1",
                    task: "externalRuleBlock",
                },
                editorData: {
                    inputPorts: [
                        {
                            id: "externalPort",
                            label: "External input",
                            description: "External description",
                            displayOrder: 7,
                            deprecated: false,
                        },
                    ],
                },
            }),
        );

        expect(preparedPaste).toMatchObject({
            taskData: {
                nodes: [
                    expect.objectContaining({
                        nodeId: "externalInputNode",
                        pluginType: "InputPortOperator",
                        parameters: {
                            portId: expect.any(String),
                        },
                    }),
                ],
                edges: [],
            },
            externalChange: expect.objectContaining({
                do: expect.any(Function),
                undo: expect.any(Function),
            }),
        });
        expect(
            (preparedPaste as { taskData: RuleClipboardTask["data"] }).taskData.nodes[0].parameters?.portId,
        ).not.toBe("externalPort");

        await act(async () => {
            (preparedPaste as { externalChange: { do: () => void } }).externalChange.do();
        });

        expect(
            (editor.getRuleEditorProps().captureExternalSavedState!() as { ports: RuleBlockPort[] }).ports,
        ).toContainEqual(
            expect.objectContaining({
                label: "External input",
                description: "External description",
                displayOrder: 5,
                deprecated: false,
            }),
        );
    });
});
