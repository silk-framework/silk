import React from "react";
import "@testing-library/jest-dom";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { IProjectTask } from "@ducks/shared/typings";
import type { StickyNote } from "../../../../../../../libs/gui-elements";
import {
    createRuleBlockEditorGuiElementsModule,
    mockI18next,
    mockReactI18next,
    testTranslate,
} from "../../../../test/jestTestUtils";
import { RuleValidationError } from "../../../shared/RuleEditor/RuleEditor.typings";
import type { IRuleOperatorNode, RuleSaveResult } from "../../../shared/RuleEditor/RuleEditor.typings";
import type { IRuleBlockPort, IRuleBlockTaskParameters } from "../ruleBlock.types";
import type { IRuleSidebarPreConfiguredOperatorsTabConfig } from "../../../shared/RuleEditor/RuleEditor.typings";
import type { IPreConfiguredRuleOperator } from "../../../shared/RuleEditor/view/sidebar/RuleEditorOperatorSidebar.typings";
import type { InputPortDialogSubmitValue } from "../InputPortDialog";

type CapturedRuleEditorProps = {
    projectId: string;
    taskId: string;
    fetchRuleData: (projectId: string, taskId: string) => Promise<unknown> | unknown;
    saveRule: (
        ruleOperatorNodes: IRuleOperatorNode[],
        stickyNotes: StickyNote[],
        originalTask: IProjectTask<IRuleBlockTaskParameters>,
    ) => Promise<RuleSaveResult>;
    tabs?: unknown[];
    additionalToolBarComponents?: () => React.JSX.Element | React.JSX.Element[];
    captureExternalSavedState?: () => unknown;
    restoreExternalSavedState?: (savedState: unknown) => void;
    extraRuleNodeMenuItems?: (node: IRuleOperatorNode, closeMenu: () => void) => React.JSX.Element[] | undefined;
};

type CapturedInputPortDialogProps = {
    isOpen: boolean;
    mode: "create" | "edit";
    editedPortId?: string;
    initialPort: InputPortDialogSubmitValue;
    existingPorts: IRuleBlockPort[];
    onSubmit: (value: InputPortDialogSubmitValue) => void;
    onClose: () => void;
};

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
    const mockRuleEditorApi = {
        ruleOperatorNodes: jest.fn<IRuleOperatorNode[], []>(() => []),
        startChangeTransaction: jest.fn(),
        executeExternalRuleModelChange: jest.fn(),
        updateRuleOperatorNodeMetaData: jest.fn(),
        deleteNodes: jest.fn(),
    };

    mockReactI18next(testTranslate);
    mockI18next(testTranslate);
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

    const { RuleBlockEditor } = require("../RuleBlockEditor") as typeof import("../RuleBlockEditor");
    return {
        RuleBlockEditor,
        mockRequestTaskData,
        mockRequestRelatedItems,
        mockRequestUpdateProjectTask,
        mockRuleEditorApi,
        getCapturedRuleEditorProps: () => mockCapturedRuleEditorProps,
        getLastInputPortDialogProps: () => mockLastInputPortDialogProps,
    };
};

type RuleBlockEditorHarness = ReturnType<typeof createRuleBlockEditorHarness>;

const createPersistedPort = (overrides: Partial<IRuleBlockPort> = {}): IRuleBlockPort => ({
    id: "inputPortA",
    label: "Input A",
    description: "Input description",
    exampleValues: "- example",
    displayOrder: 2,
    deprecated: false,
    ...overrides,
});

const createRuleBlockTask = (ports: IRuleBlockPort[] = []): IProjectTask<IRuleBlockTaskParameters> => ({
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
                layout: { nodePositions: {} },
                uiAnnotations: { stickyNotes: [] },
            },
        },
    },
} as IProjectTask<IRuleBlockTaskParameters>);

const createInputPortNode = (overrides: Partial<IRuleOperatorNode> = {}): IRuleOperatorNode => ({
    nodeId: "nodeA",
    pluginType: "InputPortOperator",
    pluginId: "InputPortOperator",
    label: "Input port node",
    parameters: {
        portId: "inputPortA",
    },
    inputs: [],
    portSpecification: {
        type: "count",
        minInputPorts: 0,
    },
    inputsCanBeSwitched: false,
    ...overrides,
});

const getInputPortsTab = (
    tabs: unknown[] | undefined,
): IRuleSidebarPreConfiguredOperatorsTabConfig<IRuleBlockPort | { type: "create" }> =>
    tabs?.find((tab) => (tab as { id?: string }).id === "inputPorts") as IRuleSidebarPreConfiguredOperatorsTabConfig<
        IRuleBlockPort | { type: "create" }
    >;

const fetchInputPortTabItems = async (
    tab: IRuleSidebarPreConfiguredOperatorsTabConfig<IRuleBlockPort | { type: "create" }>,
) => {
    const items = await tab.fetchOperators("en");
    expect(items).toBeDefined();
    return items!;
};

const renderOperatorActions = (operator: IPreConfiguredRuleOperator) =>
    render(
        <>
            {Array.isArray(operator.actions)
                ? operator.actions
                : operator.actions
                  ? [operator.actions]
                  : null}
        </>,
    );

const renderRuleBlockEditor = async ({
    ports = [],
    ruleOperatorNodes = [],
}: {
    ports?: IRuleBlockPort[];
    ruleOperatorNodes?: IRuleOperatorNode[];
} = {}) => {
    const harness = createRuleBlockEditorHarness();
    harness.mockRequestTaskData.mockResolvedValue({
        data: createRuleBlockTask(ports),
    });
    harness.mockRuleEditorApi.ruleOperatorNodes.mockReturnValue(ruleOperatorNodes);

    render(<harness.RuleBlockEditor projectId="project1" ruleBlockTaskId="task1" instanceId="instance1" />);

    await waitFor(() => expect(harness.getCapturedRuleEditorProps()).toBeDefined());

    return {
        ...harness,
        getRuleEditorProps: () => {
            const capturedProps = harness.getCapturedRuleEditorProps();
            expect(capturedProps).toBeDefined();
            return capturedProps!;
        },
        getInputPortDialogProps: () => harness.getLastInputPortDialogProps(),
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
    return render(<>{harness.getCapturedRuleEditorProps()!.additionalToolBarComponents!()}</>);
};

describe("RuleBlockEditor", () => {
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
                message: testTranslate("taskViews.ruleBlock.errors.missingPortId"),
            },
        ]);
        expect(editor.mockRequestRelatedItems).not.toHaveBeenCalled();
        expect(editor.mockRequestUpdateProjectTask).not.toHaveBeenCalled();
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
                exampleValues: "",
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
        expect(editor.getRuleEditorProps().captureExternalSavedState!()).toContainEqual(
            expect.objectContaining({
                label: "Created input",
                description: "Created description",
                exampleValues: "",
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
                exampleValues: "- changed",
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
        expect(editor.getRuleEditorProps().captureExternalSavedState!()).toContainEqual({
            id: "inputPortA",
            label: "Updated input",
            description: "Updated description",
            exampleValues: "- changed",
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

        expect(editor.getRuleEditorProps().captureExternalSavedState!()).toStrictEqual([
            createPersistedPort({ id: "inputPortA", label: "Input A", displayOrder: 1 }),
            createPersistedPort({ id: "inputPortB", label: "Input B", displayOrder: 2 }),
        ]);

        await act(async () => {
            editor.getRuleEditorProps().restoreExternalSavedState!([
                createPersistedPort({ id: "inputPortB", label: "Restored B", displayOrder: 4 }),
                createPersistedPort({ id: "inputPortA", label: "Restored A", displayOrder: 2 }),
            ]);
        });

        await waitFor(() =>
            expect(editor.getRuleEditorProps().captureExternalSavedState!()).toStrictEqual([
                createPersistedPort({ id: "inputPortA", label: "Restored A", displayOrder: 2 }),
                createPersistedPort({ id: "inputPortB", label: "Restored B", displayOrder: 4 }),
            ]),
        );
    });
});
