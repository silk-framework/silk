import React from "react";
import "@testing-library/jest-dom";
import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import jestTestUtils from "../../../../test/jestTestUtils";
import ruleTestHelper from "../../shared/rules/tests/ruleTestHelper";
import type { RuleBlockPort } from "../ruleBlock.types";

describe("RuleBlockEditor integration", () => {
    beforeEach(() => {
        window.localStorage.clear();
    });

    it("should show the unused warning for a normal port after deleting a deprecated port first via the real editor model", async () => {
        const harness = createRuleBlockEditorHarness();
        harness.mockRequestRuleOperatorPluginsDetails.mockResolvedValue({
            data: transformPluginDetails,
        });

        render(
            <harness.RuleBlockEditorOptionalContext.Provider value={{ ruleBlockSnapshot: createExternalSnapshot() }}>
                <harness.RuleBlockEditor projectId="project1" ruleBlockTaskId="task1" instanceId="instance1" />
            </harness.RuleBlockEditorOptionalContext.Provider>,
        );

        await waitFor(() =>
            expect(screen.getByTestId("sidebar-item-normalPort")).toHaveAttribute("data-warning", "false"),
        );

        fireEvent.click(screen.getByRole("button", { name: "remove-deprecated-node" }));

        await waitFor(() =>
            expect(screen.queryByRole("button", { name: "remove-deprecated-node" })).not.toBeInTheDocument(),
        );
        expect(screen.getByTestId("sidebar-item-normalPort")).toHaveAttribute("data-warning", "false");

        fireEvent.click(screen.getByRole("button", { name: "remove-normal-node" }));

        await waitFor(() =>
            expect(screen.queryByRole("button", { name: "remove-normal-node" })).not.toBeInTheDocument(),
        );
        await waitFor(() =>
            expect(screen.getByTestId("sidebar-item-normalPort")).toHaveAttribute("data-warning", "true"),
        );
    });

    it("should show both the validation error and the unused-port warning when saving an invalid changed rule", async () => {
        const harness = createRuleBlockEditorHarness();
        harness.mockRequestRuleOperatorPluginsDetails.mockResolvedValue({
            data: transformPluginDetails,
        });
        harness.mockRequestRelatedItems.mockResolvedValue({
            data: {
                total: 0,
            },
        });

        render(
            <harness.RuleBlockEditorOptionalContext.Provider value={{ ruleBlockSnapshot: createInvalidSaveSnapshot() }}>
                <harness.RuleBlockEditor projectId="project1" ruleBlockTaskId="task1" instanceId="instance1" />
            </harness.RuleBlockEditorOptionalContext.Provider>,
        );

        await waitFor(() => expect(screen.getByRole("button", { name: "Save" })).toBeDisabled());
        expect(screen.queryByTestId("context-overlay")).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole("button", { name: "remove-normal-node" }));

        await waitFor(() => expect(screen.getByRole("button", { name: "Save" })).toBeEnabled());

        await act(async () => {
            fireEvent.click(screen.getByRole("button", { name: "Save" }));
        });

        await waitFor(() => {
            const notifications = within(screen.getByTestId("context-overlay")).getAllByTestId("notification");
            expect(notifications).toHaveLength(2);
            expect(notifications[0]).toHaveAttribute("data-intent", "danger");
            expect(notifications[0]).toHaveTextContent("taskViews.ruleBlock.errors.invalidPorts");
            expect(notifications[1]).toHaveAttribute("data-intent", "warning");
            expect(notifications[1]).toHaveTextContent("taskViews.ruleBlock.warnings.unusedPorts");
        });
        expect(harness.mockRequestUpdateProjectTask).not.toHaveBeenCalled();
    });
});

const createGuiElementsModule = () => {
    const React = require("react");
    return {
        Button: jestTestUtils.createButtonMock(
            ({ affirmative, disruptive, tooltip, tooltipProps, loading, elevated, ...props }) => ({
                ...jestTestUtils.omitUnsupportedDomProps(props),
                loading,
                includeLoadingState: true,
            }),
        ),
        Card: jestTestUtils.createDivPassthroughMock(),
        CardActions: jestTestUtils.createDivPassthroughMock(),
        CardActionsAux: jestTestUtils.createDivPassthroughMock(),
        CardContent: jestTestUtils.createDivPassthroughMock(),
        Checkbox: jestTestUtils.createCheckboxMock(),
        ContextMenu: jestTestUtils.createContextMenuMock(),
        ContextOverlay: jestTestUtils.createContextOverlayMock(),
        Divider: jestTestUtils.createDivPassthroughMock(),
        Grid: jestTestUtils.createDivPassthroughMock(),
        GridColumn: jestTestUtils.createDivPassthroughMock(),
        GridRow: jestTestUtils.createDivPassthroughMock(),
        Icon: jestTestUtils.createIconMock(),
        IconButton: jestTestUtils.createButtonMock(
            ({ tooltipProps, intent, loading, size, tooltipAsTitle, description, minimal, ...props }) => ({
                ...props,
                text: props.text ?? props.name,
                loading,
                includeLoadingState: false,
            }),
        ),
        InteractionGate: ({ children }) => <>{children}</>,
        Markdown: ({ children }) => <>{children}</>,
        MenuItem: jestTestUtils.createMenuItemMock(),
        Notification: jestTestUtils.createNotificationMock(),
        Spacing: jestTestUtils.createChildrenOnlyMock(),
        StickyNote: () => null,
        TabTitle: ({ text, titlePrefix }) => (
            <span>
                {titlePrefix}
                {text}
            </span>
        ),
        Tabs: ({ tabs, selectedTabId, onChange }) => (
            <div>
                {tabs.map((tab) => (
                    <button
                        key={tab.id}
                        type="button"
                        data-testid={`sidebar-tab-${tab.id}`}
                        data-selected={String(tab.id === selectedTabId)}
                        onClick={() => onChange(tab.id)}
                    >
                        {tab.title}
                    </button>
                ))}
            </div>
        ),
        TitleMainsection: jestTestUtils.createChildrenOnlyMock(),
        Toolbar: jestTestUtils.createDivPassthroughMock(),
        ToolbarSection: jestTestUtils.createDivPassthroughMock(),
        StickyNoteModal: jestTestUtils.createDivPassthroughMock(),
        Switch: ({ label, checked, onClick, ...props }) => (
            <button
                type="button"
                onClick={onClick}
                aria-pressed={checked}
                {...jestTestUtils.omitUnsupportedDomProps(props)}
            >
                {label}
            </button>
        ),
        ReactFlowHotkeyContext: React.createContext({ hotKeysDisabled: false }),
        highlighterUtils: {
            extractSearchWords: (text: string) =>
                text
                    .split(/\s+/)
                    .map((word) => word.trim().toLowerCase())
                    .filter(Boolean),
        },
    };
};

type RenderedSidebarItem = {
    id: string;
    label: string;
    hasWarning: boolean;
};

const flattenPreConfiguredSidebarItems = (preConfiguredOperators: any[] | undefined): RenderedSidebarItem[] =>
    (preConfiguredOperators ?? []).flatMap((config) =>
        config.originalOperators.map((item) => {
            const operator = config.toPreConfiguredRuleOperator(item);
            return {
                id: config.itemId(item),
                label: operator.label,
                hasWarning: !!operator.statusIndicator,
            };
        }),
    );

const createRuleBlockEditorHarness = () => {
    jest.resetModules();
    jest.doMock("react", () => React);
    jest.doMock("react-i18next", () => {
        const translationResult = Object.assign([jestTestUtils.testTranslate], {
            t: jestTestUtils.testTranslate,
            i18n: { language: "en" },
        });
        return {
            useTranslation: () => translationResult,
            withTranslation: () => (Component) => Component,
            initReactI18next: {
                type: "3rdParty",
                init: () => undefined,
            },
            Trans: ({ children }) => children,
        };
    });
    jest.doMock("i18next", () => {
        const i18n = {
            t: jestTestUtils.testTranslate,
            use() {
                return this;
            },
            init() {
                return this;
            },
        };
        return {
            __esModule: true,
            default: i18n,
        };
    });

    const mockRegisterError = jest.fn();
    const mockRequestTaskData = jest.fn();
    const mockRequestRelatedItems = jest.fn();
    const mockRequestUpdateProjectTask = jest.fn();
    const mockRequestRuleOperatorPluginsDetails = jest.fn();

    jest.doMock("../../../../../../../libs/gui-elements", createGuiElementsModule);
    jest.doMock("react-redux", () => {
        const actual = jest.requireActual("react-redux");
        return {
            ...actual,
            useSelector: () => "en",
        };
    });
    jest.doMock("react-router", () => ({
        Prompt: () => null,
    }));
    jest.doMock("../../../shared/Loading", () => ({
        __esModule: true,
        default: () => <div data-testid="loading" />,
    }));
    jest.doMock("../../../../hooks/useErrorHandler", () => ({
        __esModule: true,
        default: () => ({
            registerError: mockRegisterError,
        }),
    }));
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
    jest.doMock("../../../shared/RuleEditor/view/evaluation/EvaluationActivityControl", () => ({
        __esModule: true,
        EvaluationActivityControl: () => null,
    }));
    jest.doMock("../../../shared/RuleEditor/view/components/RuleEditorBaseModal", () => ({
        __esModule: true,
        RuleEditorBaseModal: ({ children }) => <div>{children}</div>,
    }));
    jest.doMock("../../../shared/HotKeyHandler/HotKeyHandler", () => ({
        __esModule: true,
        default: jest.fn(),
    }));
    jest.doMock("../../../shared/ApplicationNotifications/NotificationsMenu", () => ({
        useNotificationsQueue: () => ({
            messages: [],
            notifications: null,
        }),
    }));
    jest.doMock("../../../shared/RuleEditor/view/sidebar/SidebarSearchField", () => ({
        __esModule: true,
        SidebarSearchField: () => <div data-testid="sidebar-search-field" />,
    }));
    jest.doMock("../../../shared/RuleEditor/view/sidebar/RuleOperatorList", () => ({
        __esModule: true,
        RuleOperatorList: ({ preConfiguredOperators }) => {
            const items = flattenPreConfiguredSidebarItems(preConfiguredOperators);
            return (
                <div data-testid="rule-operator-list">
                    {items.map((item) => (
                        <div
                            key={item.id}
                            data-testid={`sidebar-item-${item.id}`}
                            data-warning={String(item.hasWarning)}
                        >
                            {item.label}
                        </div>
                    ))}
                </div>
            );
        },
    }));
    jest.doMock("../../../shared/RuleEditor/view/RuleEditorCanvas", () => ({
        __esModule: true,
        RuleEditorCanvas: () => {
            const React = require("react");
            const { RuleEditorModelContext } = require("../../../shared/RuleEditor/contexts/RuleEditorModelContext");
            const modelContext = React.useContext(RuleEditorModelContext);
            React.useEffect(() => {
                modelContext.setReactFlowInstance({
                    fitView: jest.fn(),
                    zoomTo: jest.fn(),
                    toObject: () => ({ elements: [] }),
                    project: (position) => position,
                });
            }, []);
            const currentNodeIds = modelContext.ruleOperatorNodes().map((node) => node.nodeId);
            return (
                <div data-testid="rule-editor-canvas">
                    {currentNodeIds.includes("deprecatedNode") ? (
                        <button
                            type="button"
                            onClick={() => {
                                modelContext.executeModelEditOperation.startChangeTransaction();
                                modelContext.executeModelEditOperation.deleteNodes(["deprecatedNode"]);
                            }}
                        >
                            remove-deprecated-node
                        </button>
                    ) : null}
                    {currentNodeIds.includes("normalNode") ? (
                        <button
                            type="button"
                            onClick={() => {
                                modelContext.executeModelEditOperation.startChangeTransaction();
                                modelContext.executeModelEditOperation.deleteNodes(["normalNode"]);
                            }}
                        >
                            remove-normal-node
                        </button>
                    ) : null}
                </div>
            );
        },
    }));
    jest.doMock("../RuleBlockEvaluation", () => ({
        __esModule: true,
        default: ({ children }) => <>{children}</>,
    }));
    jest.doMock("../InputPortDialog", () => ({
        __esModule: true,
        default: () => null,
    }));
    jest.doMock("../ExampleValuesDialog", () => ({
        __esModule: true,
        default: () => null,
    }));

    const { RuleBlockEditor, RuleBlockEditorOptionalContext } =
        require("../RuleBlockEditor") as typeof import("../RuleBlockEditor");

    return {
        RuleBlockEditor,
        RuleBlockEditorOptionalContext,
        mockRegisterError,
        mockRequestTaskData,
        mockRequestRelatedItems,
        mockRequestUpdateProjectTask,
        mockRequestRuleOperatorPluginsDetails,
    };
};

const createPersistedPort = (overrides: Partial<RuleBlockPort> = {}): RuleBlockPort =>
    ruleTestHelper.createRuleBlockPort({
        description: "Input description",
        ...overrides,
    });

const createExternalSnapshot = () =>
    ruleTestHelper.createRuleBlockInspectionSnapshot({
        ports: [
            createPersistedPort({
                id: "deprecatedPort",
                label: "Deprecated input",
                displayOrder: 1,
                deprecated: true,
            }),
            createPersistedPort({
                id: "normalPort",
                label: "Normal input",
                displayOrder: 2,
                deprecated: false,
            }),
        ],
        operatorTree: {
            type: "transformInput",
            id: "concatNode",
            function: "concat",
            parameters: {
                glue: " ",
            },
            inputs: [
                {
                    type: "inputPortInput",
                    id: "deprecatedNode",
                    portId: "deprecatedPort",
                },
                {
                    type: "inputPortInput",
                    id: "normalNode",
                    portId: "normalPort",
                },
            ],
        },
        layout: {
            nodePositions: {
                concatNode: { x: 200, y: 40, width: null, height: null },
                deprecatedNode: { x: 20, y: 120, width: null, height: null },
                normalNode: { x: 220, y: 120, width: null, height: null },
            },
        },
    });

const createInvalidSaveSnapshot = () =>
    ruleTestHelper.createRuleBlockInspectionSnapshot({
        ports: [
            createPersistedPort({
                id: "normalPort",
                label: "Normal input",
                displayOrder: 1,
                deprecated: false,
            }),
            createPersistedPort({
                id: "unusedPort",
                label: "Unused input",
                displayOrder: 1,
                deprecated: false,
            }),
        ],
        operatorTree: {
            type: "transformInput",
            id: "concatNode",
            function: "concat",
            parameters: {
                glue: " ",
            },
            inputs: [
                {
                    type: "inputPortInput",
                    id: "normalNode",
                    portId: "normalPort",
                },
            ],
        },
        layout: {
            nodePositions: {
                concatNode: { x: 200, y: 40, width: null, height: null },
                normalNode: { x: 220, y: 120, width: null, height: null },
            },
        },
    });

const transformPluginDetails = {
    concat: {
        title: "Concat",
        description: "Concatenates values",
        taskType: "Transform",
        type: "object" as const,
        categories: ["Transform"],
        properties: {
            glue: {
                title: "Glue",
                description: "Glue string",
                parameterType: "string",
                value: " ",
            },
        },
        required: [],
        pluginId: "concat",
        pluginType: "TransformOperator" as const,
    },
};
