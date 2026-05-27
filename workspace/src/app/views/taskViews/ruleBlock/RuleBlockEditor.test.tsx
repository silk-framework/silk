import React from "react";
import "@testing-library/jest-dom";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { IProjectTask } from "@ducks/shared/typings";
import type { StickyNote } from "@eccenca/gui-elements";
import {
    createRuleBlockEditorGuiElementsModule,
    mockI18next,
    mockReactI18next,
    testTranslate,
} from "../../../test/jestTestUtils";
import { RuleValidationError } from "../../shared/RuleEditor/RuleEditor.typings";
import type { IRuleOperatorNode, RuleSaveResult } from "../../shared/RuleEditor/RuleEditor.typings";
import type { IRuleBlockPort, IRuleBlockTaskParameters } from "./ruleBlock.types";
import type { IRuleSidebarPreConfiguredOperatorsTabConfig } from "../../shared/RuleEditor/RuleEditor.typings";
import type { IPreConfiguredRuleOperator } from "../../shared/RuleEditor/view/sidebar/RuleEditorOperatorSidebar.typings";
import type { InputPortDialogSubmitValue } from "./InputPortDialog";

const loadRuleBlockEditor = () => {
    jest.resetModules();
    jest.doMock("react", () => React);

    const mockRequestTaskData = jest.fn();
    const mockRequestRelatedItems = jest.fn();
    const mockRequestUpdateProjectTask = jest.fn();
    const mockRequestRuleOperatorPluginsDetails = jest.fn();
    const mockRegisterError = jest.fn();
    let mockCapturedRuleEditorProps:
        | {
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
              extraRuleNodeMenuItems?: (
                  node: IRuleOperatorNode,
                  closeMenu: () => void,
              ) => React.JSX.Element[] | undefined;
          }
        | undefined;
    let mockLastInputPortDialogProps:
        | {
              isOpen: boolean;
              mode: "create" | "edit";
              editedPortId?: string;
              initialPort: InputPortDialogSubmitValue;
              existingPorts: IRuleBlockPort[];
              onSubmit: (value: InputPortDialogSubmitValue) => void;
              onClose: () => void;
          }
        | undefined;
    const mockRuleEditorApi = {
        ruleOperatorNodes: jest.fn<IRuleOperatorNode[], []>(() => []),
        startChangeTransaction: jest.fn(),
        executeExternalRuleModelChange: jest.fn(),
        updateRuleOperatorNodeMetaData: jest.fn(),
        deleteNodes: jest.fn(),
    };

    mockReactI18next(testTranslate);
    mockI18next(testTranslate);
    jest.doMock("@eccenca/gui-elements", createRuleBlockEditorGuiElementsModule);
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
    jest.doMock("../../../hooks/useErrorHandler", () => ({
        __esModule: true,
        default: () => ({
            registerError: mockRegisterError,
        }),
    }));
    jest.doMock("../../shared/RuleEditor/RuleEditor", () => {
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
    jest.doMock("./InputPortDialog", () => ({
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

    const { RuleBlockEditor } = require("./RuleBlockEditor") as typeof import("./RuleBlockEditor");
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

const persistedPort = (overrides: Partial<IRuleBlockPort> = {}): IRuleBlockPort => ({
    id: "inputPortA",
    label: "Input A",
    description: "Input description",
    exampleValues: "- example",
    displayOrder: 2,
    deprecated: false,
    ...overrides,
});

const ruleBlockTask = (ports: IRuleBlockPort[] = []): IProjectTask<IRuleBlockTaskParameters> => ({
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

const inputPortNode = (overrides: Partial<IRuleOperatorNode> = {}): IRuleOperatorNode => ({
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

const inputPortsTab = (
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

describe("RuleBlockEditor", () => {
    it("should return a validation error when saving malformed input-port nodes without a logical port ID", async () => {
        const { RuleBlockEditor, mockRequestTaskData, mockRequestRelatedItems, mockRequestUpdateProjectTask, getCapturedRuleEditorProps } =
            loadRuleBlockEditor();
        mockRequestTaskData.mockResolvedValue({
            data: ruleBlockTask([persistedPort()]),
        });

        render(<RuleBlockEditor projectId="project1" ruleBlockTaskId="task1" instanceId="instance1" />);

        await waitFor(() => expect(getCapturedRuleEditorProps()).toBeDefined());

        const saveResult = await getCapturedRuleEditorProps()!.saveRule(
            [
                inputPortNode({
                    parameters: {
                        portId: "   ",
                    },
                }),
            ],
            [],
            ruleBlockTask([persistedPort()]),
        );

        expect((saveResult as RuleValidationError).isRuleValidationError).toBe(true);
        expect((saveResult as RuleValidationError).nodeErrors).toStrictEqual([
            {
                nodeId: "nodeA",
                message: testTranslate("taskViews.ruleBlock.errors.missingPortId"),
            },
        ]);
        expect(mockRequestRelatedItems).not.toHaveBeenCalled();
        expect(mockRequestUpdateProjectTask).not.toHaveBeenCalled();
    });

    it("should open the edit dialog when the input-port node menu item is clicked", async () => {
        const { RuleBlockEditor, mockRequestTaskData, getCapturedRuleEditorProps, getLastInputPortDialogProps } =
            loadRuleBlockEditor();
        mockRequestTaskData.mockResolvedValue({
            data: ruleBlockTask([persistedPort()]),
        });

        render(<RuleBlockEditor projectId="project1" ruleBlockTaskId="task1" instanceId="instance1" />);

        await waitFor(() => {
            expect(getCapturedRuleEditorProps()?.extraRuleNodeMenuItems).toBeDefined();
            expect(getLastInputPortDialogProps()?.editedPortId).toBeUndefined();
        });

        const closeMenu = jest.fn();
        const menuItems = getCapturedRuleEditorProps()!.extraRuleNodeMenuItems!(inputPortNode(), closeMenu);
        const { unmount } = render(<>{menuItems}</>);

        fireEvent.click(screen.getByRole("button", { name: "Edit input port" }));

        expect(closeMenu).toHaveBeenCalled();
        await waitFor(() =>
            expect(getLastInputPortDialogProps()).toMatchObject({
                isOpen: true,
                mode: "edit",
                editedPortId: "inputPortA",
            }),
        );

        unmount();
    });

    it("should create an input port via the sidebar entry and register one external transaction", async () => {
        const { RuleBlockEditor, mockRequestTaskData, mockRuleEditorApi, getCapturedRuleEditorProps, getLastInputPortDialogProps } =
            loadRuleBlockEditor();
        mockRequestTaskData.mockResolvedValue({
            data: ruleBlockTask([persistedPort({ displayOrder: 2 })]),
        });

        render(<RuleBlockEditor projectId="project1" ruleBlockTaskId="task1" instanceId="instance1" />);

        await waitFor(() => expect(getCapturedRuleEditorProps()?.tabs).toBeDefined());

        const tab = inputPortsTab(getCapturedRuleEditorProps()!.tabs);
        const items = await fetchInputPortTabItems(tab);
        const createOperator = tab.convertToOperator(items[0]);
        const { unmount } = renderOperatorActions(createOperator);

        fireEvent.click(screen.getByRole("button", { name: "item-add-artefact" }));

        await waitFor(() =>
            expect(getLastInputPortDialogProps()).toMatchObject({
                isOpen: true,
                mode: "create",
                initialPort: expect.objectContaining({
                    displayOrder: 3,
                }),
            }),
        );

        await act(async () => {
            getLastInputPortDialogProps()!.onSubmit({
                label: "Created input",
                description: "Created description",
                exampleValues: "",
                displayOrder: 3,
                deprecated: false,
            });
        });

        expect(mockRuleEditorApi.startChangeTransaction).toHaveBeenCalledTimes(1);
        expect(mockRuleEditorApi.executeExternalRuleModelChange).toHaveBeenCalledTimes(1);
        expect(mockRuleEditorApi.updateRuleOperatorNodeMetaData).not.toHaveBeenCalled();
        const createChange = mockRuleEditorApi.executeExternalRuleModelChange.mock.calls[0][0];
        await act(async () => {
            createChange.do();
        });
        expect(getCapturedRuleEditorProps()!.captureExternalSavedState!()).toContainEqual(
            expect.objectContaining({
                label: "Created input",
                description: "Created description",
                exampleValues: "",
                displayOrder: 3,
                deprecated: false,
            }),
        );
        await waitFor(() =>
            expect(getLastInputPortDialogProps()).toMatchObject({
                isOpen: false,
            }),
        );

        unmount();
    });

    it("should update an input port via the sidebar entry and sync node metadata", async () => {
        const { RuleBlockEditor, mockRequestTaskData, mockRuleEditorApi, getCapturedRuleEditorProps, getLastInputPortDialogProps } =
            loadRuleBlockEditor();
        mockRequestTaskData.mockResolvedValue({
            data: ruleBlockTask([persistedPort()]),
        });
        mockRuleEditorApi.ruleOperatorNodes.mockReturnValue([inputPortNode()]);

        render(<RuleBlockEditor projectId="project1" ruleBlockTaskId="task1" instanceId="instance1" />);

        await waitFor(() => expect(getCapturedRuleEditorProps()?.tabs).toBeDefined());

        const tab = inputPortsTab(getCapturedRuleEditorProps()!.tabs);
        const items = await fetchInputPortTabItems(tab);
        const existingOperator = tab.convertToOperator(items[1]);
        const { unmount } = renderOperatorActions(existingOperator);

        fireEvent.click(screen.getByRole("button", { name: "item-edit" }));

        await waitFor(() =>
            expect(getLastInputPortDialogProps()).toMatchObject({
                isOpen: true,
                mode: "edit",
                editedPortId: "inputPortA",
            }),
        );

        await act(async () => {
            getLastInputPortDialogProps()!.onSubmit({
                label: "Updated input",
                description: "Updated description",
                exampleValues: "- changed",
                displayOrder: 4,
                deprecated: false,
            });
        });

        expect(mockRuleEditorApi.startChangeTransaction).toHaveBeenCalledTimes(1);
        expect(mockRuleEditorApi.executeExternalRuleModelChange).toHaveBeenCalledTimes(1);
        expect(mockRuleEditorApi.updateRuleOperatorNodeMetaData).toHaveBeenCalledTimes(1);
        const updateChange = mockRuleEditorApi.executeExternalRuleModelChange.mock.calls[0][0];
        await act(async () => {
            updateChange.do();
        });
        expect(getCapturedRuleEditorProps()!.captureExternalSavedState!()).toContainEqual({
            id: "inputPortA",
            label: "Updated input",
            description: "Updated description",
            exampleValues: "- changed",
            displayOrder: 4,
            deprecated: false,
        });
        expect(mockRuleEditorApi.updateRuleOperatorNodeMetaData).toHaveBeenCalledWith(
            ["nodeA"],
            expect.any(Function),
        );

        unmount();
    });

    it("should delete a used input port only after confirmation and remove matching nodes in one transaction", async () => {
        const { RuleBlockEditor, mockRequestTaskData, mockRuleEditorApi, getCapturedRuleEditorProps } =
            loadRuleBlockEditor();
        mockRequestTaskData.mockResolvedValue({
            data: ruleBlockTask([persistedPort()]),
        });
        mockRuleEditorApi.ruleOperatorNodes.mockReturnValue([inputPortNode()]);

        render(<RuleBlockEditor projectId="project1" ruleBlockTaskId="task1" instanceId="instance1" />);

        await waitFor(() => expect(getCapturedRuleEditorProps()?.tabs).toBeDefined());

        const tab = inputPortsTab(getCapturedRuleEditorProps()!.tabs);
        const items = await fetchInputPortTabItems(tab);
        const existingOperator = tab.convertToOperator(items[1]);
        const { unmount } = renderOperatorActions(existingOperator);

        fireEvent.click(screen.getByRole("button", { name: "item-remove" }));

        expect(screen.getByTestId("alert-dialog")).toBeInTheDocument();
        expect(mockRuleEditorApi.startChangeTransaction).not.toHaveBeenCalled();

        fireEvent.click(screen.getByRole("button", { name: "Delete" }));

        expect(mockRuleEditorApi.startChangeTransaction).toHaveBeenCalledTimes(1);
        expect(mockRuleEditorApi.executeExternalRuleModelChange).toHaveBeenCalledTimes(1);
        expect(mockRuleEditorApi.deleteNodes).toHaveBeenCalledWith(["nodeA"]);

        unmount();
    });

    it("should delete an unused input port immediately without opening the confirmation dialog", async () => {
        const { RuleBlockEditor, mockRequestTaskData, mockRuleEditorApi, getCapturedRuleEditorProps } =
            loadRuleBlockEditor();
        mockRequestTaskData.mockResolvedValue({
            data: ruleBlockTask([persistedPort()]),
        });
        mockRuleEditorApi.ruleOperatorNodes.mockReturnValue([]);

        render(<RuleBlockEditor projectId="project1" ruleBlockTaskId="task1" instanceId="instance1" />);

        await waitFor(() => expect(getCapturedRuleEditorProps()?.tabs).toBeDefined());

        const tab = inputPortsTab(getCapturedRuleEditorProps()!.tabs);
        const items = await fetchInputPortTabItems(tab);
        const existingOperator = tab.convertToOperator(items[1]);
        const { unmount } = renderOperatorActions(existingOperator);

        fireEvent.click(screen.getByRole("button", { name: "item-remove" }));

        expect(screen.queryByTestId("alert-dialog")).not.toBeInTheDocument();
        expect(mockRuleEditorApi.startChangeTransaction).toHaveBeenCalledTimes(1);
        expect(mockRuleEditorApi.executeExternalRuleModelChange).toHaveBeenCalledTimes(1);
        expect(mockRuleEditorApi.deleteNodes).not.toHaveBeenCalled();

        unmount();
    });

    it("should normalize input-port order via the toolbar action and sync changed node metadata", async () => {
        const { RuleBlockEditor, mockRequestTaskData, mockRuleEditorApi, getCapturedRuleEditorProps } =
            loadRuleBlockEditor();
        mockRequestTaskData.mockResolvedValue({
            data: ruleBlockTask([
                persistedPort({ id: "inputPortA", displayOrder: 2 }),
                persistedPort({ id: "inputPortB", label: "Input B", displayOrder: 5 }),
            ]),
        });
        mockRuleEditorApi.ruleOperatorNodes.mockReturnValue([
            inputPortNode({ nodeId: "nodeA", parameters: { portId: "inputPortA" } }),
            inputPortNode({ nodeId: "nodeB", parameters: { portId: "inputPortB" } }),
        ]);

        render(<RuleBlockEditor projectId="project1" ruleBlockTaskId="task1" instanceId="instance1" />);

        await waitFor(() => expect(getCapturedRuleEditorProps()?.additionalToolBarComponents).toBeDefined());

        render(<>{getCapturedRuleEditorProps()!.additionalToolBarComponents!()}</>);

        fireEvent.click(screen.getByRole("button", { name: "taskViews.ruleBlock.normalizePortOrder" }));

        expect(mockRuleEditorApi.startChangeTransaction).toHaveBeenCalledTimes(1);
        expect(mockRuleEditorApi.executeExternalRuleModelChange).toHaveBeenCalledTimes(1);
        expect(mockRuleEditorApi.updateRuleOperatorNodeMetaData).toHaveBeenCalledTimes(2);
        expect(mockRuleEditorApi.updateRuleOperatorNodeMetaData).toHaveBeenNthCalledWith(
            1,
            ["nodeA"],
            expect.any(Function),
        );
        expect(mockRuleEditorApi.updateRuleOperatorNodeMetaData).toHaveBeenNthCalledWith(
            2,
            ["nodeB"],
            expect.any(Function),
        );
    });

    it("should disable the normalize action when the current input-port order is already normalized", async () => {
        const { RuleBlockEditor, mockRequestTaskData, getCapturedRuleEditorProps } = loadRuleBlockEditor();
        mockRequestTaskData.mockResolvedValue({
            data: ruleBlockTask([
                persistedPort({ id: "inputPortA", displayOrder: 1 }),
                persistedPort({ id: "inputPortB", label: "Input B", displayOrder: 2 }),
            ]),
        });

        render(<RuleBlockEditor projectId="project1" ruleBlockTaskId="task1" instanceId="instance1" />);

        await waitFor(() => expect(getCapturedRuleEditorProps()?.additionalToolBarComponents).toBeDefined());

        render(<>{getCapturedRuleEditorProps()!.additionalToolBarComponents!()}</>);

        expect(screen.getByRole("button", { name: "taskViews.ruleBlock.normalizePortOrder" })).toBeDisabled();
    });

    it("should expose the current ports through the saved-state bridge and restore them through applyPorts", async () => {
        const { RuleBlockEditor, mockRequestTaskData, getCapturedRuleEditorProps } = loadRuleBlockEditor();
        mockRequestTaskData.mockResolvedValue({
            data: ruleBlockTask([
                persistedPort({ id: "inputPortB", label: "Input B", displayOrder: 2 }),
                persistedPort({ id: "inputPortA", label: "Input A", displayOrder: 1 }),
            ]),
        });

        render(<RuleBlockEditor projectId="project1" ruleBlockTaskId="task1" instanceId="instance1" />);

        await waitFor(() => {
            expect(getCapturedRuleEditorProps()?.captureExternalSavedState).toBeDefined();
            expect(getCapturedRuleEditorProps()?.restoreExternalSavedState).toBeDefined();
        });

        expect(getCapturedRuleEditorProps()!.captureExternalSavedState!()).toStrictEqual([
            persistedPort({ id: "inputPortA", label: "Input A", displayOrder: 1 }),
            persistedPort({ id: "inputPortB", label: "Input B", displayOrder: 2 }),
        ]);

        await act(async () => {
            getCapturedRuleEditorProps()!.restoreExternalSavedState!([
                persistedPort({ id: "inputPortB", label: "Restored B", displayOrder: 4 }),
                persistedPort({ id: "inputPortA", label: "Restored A", displayOrder: 2 }),
            ]);
        });

        await waitFor(() =>
            expect(getCapturedRuleEditorProps()!.captureExternalSavedState!()).toStrictEqual([
                persistedPort({ id: "inputPortA", label: "Restored A", displayOrder: 2 }),
                persistedPort({ id: "inputPortB", label: "Restored B", displayOrder: 4 }),
            ]),
        );
    });
});
