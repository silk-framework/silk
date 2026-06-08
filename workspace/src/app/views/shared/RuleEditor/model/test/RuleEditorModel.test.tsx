import React from "react";
import { RuleEditorModel } from "../RuleEditorModel";
import { RenderResultApi, renderWrapper } from "../../../../../../../test/integration/TestHelper";
import { RuleEditorModelContext, RuleEditorModelContextProps } from "../../contexts/RuleEditorModelContext";
import {
    Elements,
    FitViewParams,
    FlowElement,
    FlowExportObject,
    FlowTransform,
    ReactFlowProvider,
} from "react-flow-renderer";
import { act, cleanup, render, screen, waitFor } from "@testing-library/react";
import { RuleEditorContext, RuleEditorContextProps } from "../../contexts/RuleEditorContext";
import {
    IParameterSpecification,
    IPortSpecification,
    IRuleOperator,
    IRuleOperatorNode,
    RuleEditorValidationNode,
    RuleOperatorNodeParameters,
    RuleSaveResult,
} from "../../RuleEditor.typings";
import { XYPosition } from "react-flow-renderer/dist/types";
import utils from "../../RuleEditor.utils";
import { DEFAULT_NODE_HEIGHT, DEFAULT_NODE_WIDTH, ruleEditorModelUtilsFactory } from "../RuleEditorModel.utils";
import { PreparedClipboardPaste, RuleClipboardTask, RuleEditorNode } from "../RuleEditorModel.typings";
import { rangeArray } from "../../../../../utils/basicUtils";
import { LINKING_NODE_TYPES } from "@eccenca/gui-elements/src/cmem/react-flow/configuration/typing";
import { nodeDefaultUtils, StickyNote } from "@eccenca/gui-elements";
import type { RuleBlockPort } from "../../../../taskViews/ruleBlock/ruleBlock.types";
import ruleBlockPasteUtils from "../../../../taskViews/ruleBlock/ruleBlockPaste.utils";
import ruleTestHelper from "../../../../taskViews/shared/rules/tests/ruleTestHelper";

let modelContext: RuleEditorModelContextProps | undefined;
const currentContext = () => modelContext as RuleEditorModelContextProps;
const execute = () => currentContext().executeModelEditOperation;
const modelUtils = ruleEditorModelUtilsFactory();
const isDebugLoggingEnabled = () => process.env.DEBUG === "true";
const nodeById = (nodeId: string) => {
    const node = currentContext().elements.find((elem) => modelUtils.isNode(elem) && elem.id === nodeId);
    expect(node).toBeTruthy();
    return modelUtils.asNode(node)!!;
};

describe("Rule editor model", () => {
    // Get a deep copy of the current operator nodes sorted by node ID
    const currentOperatorNodes = (): IRuleOperatorNode[] => {
        return JSON.parse(
            JSON.stringify(
                currentContext()
                    .ruleOperatorNodes()
                    .sort((n1, n2) => (n1.nodeId < n2.nodeId ? -1 : 1)),
            ),
        );
    };
    // Fetch the current react-flow nodes
    const currentReactFlowNodes = (): RuleEditorNode[] => {
        return modelUtils.elementNodes(currentContext().elements).sort((n1, n2) => (n1.id < n2.id ? -1 : 1));
    };

    let savedRuleOperatorNodes: IRuleOperatorNode[] = [];

    const ruleOperatorList: IRuleOperator[] = [
        {
            pluginType: "unknown",
            pluginId: "testPlugin",
            label: "Test plugin",
            tags: [],
            parameterSpecification: {},
            portSpecification: {
                type: "count",
                minInputPorts: 0,
            },
            inputsCanBeSwitched: false,
        },
    ];

    interface RuleEditorModelOptions {
        initialRuleNodes?: IRuleOperatorNode[];
        operatorList?: IRuleOperator[];
        operatorSpec?: Map<string, Map<string, IParameterSpecification>>;
        validateConnection?: (
            fromRuleOperatorNode: RuleEditorValidationNode,
            toRuleOperatorNode: RuleEditorValidationNode,
            targetPortIdx: number,
        ) => boolean;
        stickyNotes?: StickyNote[];
        contextOverrides?: Partial<RuleEditorContextProps>;
        instanceId?: string;
    }

    const ruleEditorModel = async ({
        initialRuleNodes = [],
        operatorList = ruleOperatorList,
        operatorSpec = new Map(),
        validateConnection = () => true,
        stickyNotes = [],
        contextOverrides = {},
        instanceId = "id",
    }: RuleEditorModelOptions = {}) => {
        // Remove previously mounted components (needed if called multiple times in the same test)
        cleanup();
        modelContext = undefined;
        const Provider: React.FC<{ children: React.JSX.Element }> = ReactFlowProvider;
        const ruleModel = await act(() => {
            return renderWrapper(
                <RuleEditorContext.Provider
                    value={{
                        projectId: "testProject",
                        editedItem: {},
                        operatorList: operatorList,
                        editedItemLoading: false,
                        operatorListLoading: false,
                        initialRuleOperatorNodes: initialRuleNodes,
                        stickyNotes,
                        saveRule: (ruleOperatorNodes): RuleSaveResult => {
                            savedRuleOperatorNodes = ruleOperatorNodes;
                            return { success: true };
                        },
                        convertRuleOperatorToRuleNode: utils.defaults.convertRuleOperatorToRuleNode,
                        operatorSpec,
                        validateConnection,
                        instanceId,
                        datasetCharacteristics: new Map(),
                        partialAutoCompletion: () => async () => undefined,
                        saveInitiallyEnabled: false,
                        ...contextOverrides,
                    }}
                >
                    <Provider>
                        <RuleEditorModel>
                            <RuleEditorModelTestComponent />
                        </RuleEditorModel>
                    </Provider>
                </RuleEditorContext.Provider>,
            );
        });
        await waitFor(() => {
            expect(modelContext).toBeTruthy();
        });
        await act(async () => {
            modelContext!!.setReactFlowInstance({
                fitView(fitViewOptions: FitViewParams | undefined, duration: number | undefined): void {},
                getElements(): Elements {
                    return [];
                },
                project(position: XYPosition): XYPosition {
                    return position;
                },
                setTransform(transform: FlowTransform): void {},
                toObject(): FlowExportObject<any> {
                    return undefined as any;
                },
                zoomIn(): void {},
                zoomOut(): void {},
                zoomTo(zoomLevel: number): void {},
            });
            await Promise.resolve();
        });
        await act(async () => {
            // Allow RuleEditorModel's delayed fit-view/init completion timeout to settle before tests continue.
            await new Promise((resolve) => setTimeout(resolve, 1));
        });
        return ruleModel;
    };

    afterEach(() => {
        modelContext = undefined;
    });

    interface NodeProps {
        nodeId: string;
        inputs?: (string | undefined)[];
        pluginId?: string;
        pluginType?: IRuleOperator["pluginType"];
        portSpecification?: IPortSpecification;
        position?: XYPosition;
        parameters?: RuleOperatorNodeParameters;
    }
    const nodeDefaultPosition = { x: 0, y: 0 };
    const defaultParameters = {
        "param A": "Value A",
        "param B": "Value B",
    };

    const node = ({
        nodeId,
        inputs = [],
        pluginId = "testPlugin",
        pluginType = "unknown",
        portSpecification = { type: "count", minInputPorts: 1 },
        position = nodeDefaultPosition,
        parameters = defaultParameters,
    }: NodeProps): IRuleOperatorNode => {
        return ruleTestHelper.createRuleOperatorNode({
            nodeId,
            parameters,
            label: nodeId,
            inputs,
            pluginId,
            pluginType,
            portSpecification,
            position,
            tags: undefined,
        });
    };

    const parameterSpecification = (paramId: string): IParameterSpecification => {
        return {
            advanced: false,
            defaultValue: "",
            label: paramId,
            required: false,
            type: "textField",
            orderIdx: 1,
        };
    };

    const operator = (
        pluginId: string,
        minInputPorts: number = 1,
        pluginType: IRuleOperator["pluginType"] = "unknown",
    ): IRuleOperator => {
        return {
            label: pluginId,
            parameterSpecification: {
                "param A": parameterSpecification("param A"),
                "param B": parameterSpecification("param B"),
            },
            pluginId: pluginId,
            pluginType,
            portSpecification: {
                type: "count",
                minInputPorts: minInputPorts,
            },
            tags: [],
            inputsCanBeSwitched: false,
        };
    };

    const createClipboardStore = () => {
        const values = new Map<string, string>();
        return {
            clipboardData: {
                setData: jest.fn((type: string, value: string) => {
                    values.set(type, value);
                }),
                getData: jest.fn((type: string) => values.get(type) ?? ""),
            },
            readTask: (): RuleClipboardTask => JSON.parse(values.get("text/plain") ?? "{}").task,
        };
    };

    const dispatchClipboardEvent = async (
        type: "copy" | "paste",
        clipboardData: { setData?: (type: string, value: string) => void; getData?: (type: string) => string },
    ) => {
        const eventTarget = document.createElement("div");
        document.body.appendChild(eventTarget);
        const event = new Event(type, { bubbles: true }) as Event & {
            clipboardData: typeof clipboardData;
            preventDefault: jest.Mock;
        };
        Object.defineProperty(event, "clipboardData", {
            value: clipboardData,
        });
        event.preventDefault = jest.fn();
        await act(async () => {
            eventTarget.dispatchEvent(event);
        });
        eventTarget.remove();
        return event;
    };

    const ruleBlockPort = (
        id: string,
        label: string,
        displayOrder: number,
        description: string = "",
        deprecated: boolean = false,
    ): RuleBlockPort => ({
        id,
        label,
        description,
        displayOrder,
        deprecated,
    });

    const ruleBlockInputPortNodeMetaData = (port: RuleBlockPort) => ({
        label: port.label,
        description: port.description,
        tags: [String(port.displayOrder)],
    });

    const copySelectedNodesToClipboard = async ({
        initialRuleNodes,
        operatorList,
        selectedNodeIds,
        instanceId,
        editedItemId,
        extendClipboardCopy,
    }: {
        initialRuleNodes: IRuleOperatorNode[];
        operatorList: IRuleOperator[];
        selectedNodeIds: string[];
        instanceId: string;
        editedItemId?: string;
        extendClipboardCopy?: (task: RuleClipboardTask, nodeIds: string[]) => unknown;
    }) => {
        const clipboardStore = createClipboardStore();
        await ruleEditorModel({
            initialRuleNodes,
            operatorList,
            instanceId,
            contextOverrides: {
                editedItemId,
                extendClipboardCopy,
            },
        });

        act(() => {
            currentContext().updateSelectedElements(
                currentContext().elements.filter(
                    (element) => modelUtils.isNode(element) && selectedNodeIds.includes(element.id),
                ),
            );
        });

        const copyEvent = await dispatchClipboardEvent("copy", clipboardStore.clipboardData);
        expect(copyEvent.preventDefault).toHaveBeenCalled();
        return clipboardStore;
    };

    const mountRuleBlockClipboardDestination = async ({
        clipboardStore,
        currentTaskId,
        existingPorts = [],
        instanceId,
        operatorList = [
            operator("inputPort", 0, "InputPortOperator"),
            operator("concat", 1, "TransformOperator"),
        ],
    }: {
        clipboardStore: ReturnType<typeof createClipboardStore>;
        currentTaskId: string;
        existingPorts?: RuleBlockPort[];
        instanceId: string;
        operatorList?: IRuleOperator[];
    }) => {
        let externalPorts = [...existingPorts];
        let generatedPortCounter = 0;

        await ruleEditorModel({
            operatorList,
            operatorSpec: new Map(),
            validateConnection: () => true,
            instanceId,
            contextOverrides: {
                prepareClipboardPaste: (task: RuleClipboardTask): PreparedClipboardPaste => {
                    const preparedPaste = ruleBlockPasteUtils.prepareRuleBlockClipboardPaste(task, {
                        currentProjectId: "testProject",
                        currentTaskId,
                        existingPorts: externalPorts,
                        createInputPortId: () => `generatedPort${++generatedPortCounter}`,
                        inputPortNodeMetaData: ruleBlockInputPortNodeMetaData,
                    });
                    if (preparedPaste.createdPorts.length === 0) {
                        return {
                            taskData: preparedPaste.taskData,
                        };
                    }
                    const previousPorts = [...externalPorts];
                    const nextPorts = [...externalPorts, ...preparedPaste.createdPorts];
                    return {
                        taskData: preparedPaste.taskData,
                        externalChange: {
                            do: () => {
                                externalPorts = nextPorts;
                            },
                            undo: () => {
                                externalPorts = previousPorts;
                            },
                        },
                    };
                },
            },
        });

        const pasteEvent = await dispatchClipboardEvent("paste", clipboardStore.clipboardData);
        return {
            pasteEvent,
            currentExternalPorts: () => [...externalPorts],
        };
    };

    const checkAfterUndo = (moreToUndo: boolean = false) => {
        expect(currentContext().canRedo).toBe(true);
        expect(currentContext().canUndo).toBe(moreToUndo);
    };

    const checkAfterChange = () => {
        expect(currentContext().canRedo).toBe(false);
        expect(currentContext().canUndo).toBe(true);
    };

    const stickyNoteNodeBootstrap = async (stickyNote = "note") => {
        const noteNode: StickyNote = {
            id: modelUtils.freshNodeId("sticky"),
            content: stickyNote,
            position: { x: 50, y: 120, width: DEFAULT_NODE_WIDTH, height: DEFAULT_NODE_HEIGHT },
            color: "#000000",
        };
        await ruleEditorModel({ stickyNotes: [noteNode] });
    };

    const allStickyNodes = () =>
        currentContext().elements.filter(
            (elem) => elem.type === LINKING_NODE_TYPES.stickynote && modelUtils.isNode(elem),
        );

    /** Test UNDO and REDO behavior. The last check is always the current state. Each check before tests the states
     *  going back in time with every UNDO. Same checks are repeated going forwards with REDO.
     **/
    const checkUndoAndRedo = (...checks: (() => any)[]) => {
        if (checks.length === 0) {
            return;
        }
        // Check current state
        checks[checks.length - 1]();
        // Check UNDO and REDO twice in a row
        for (let i = 0; i < 2; i++) {
            // UNDO until we are at the first check state
            for (let i = checks.length - 2; i >= 0; i--) {
                act(() => {
                    currentContext().undo();
                });
                checks[i]();
                checkAfterUndo(i > 0);
            }

            // REDO
            for (let i = 1; i < checks.length; i++) {
                act(() => {
                    currentContext().redo();
                });
                checks[i]();
            }
        }
    };

    it("should load an empty internal model", async () => {
        await ruleEditorModel();
        expect(currentContext().canUndo).toBe(false);
        expect(currentContext().canRedo).toBe(false);
        expect(currentContext().elements).toHaveLength(0);
        expect(currentContext().ruleOperatorNodes()).toHaveLength(0);
    });

    it("should load initial rule nodes into the internal model", async () => {
        await ruleEditorModel({
            initialRuleNodes: [
                node({ nodeId: "node A", portSpecification: { type: "count", minInputPorts: 0 } }),
                node({ nodeId: "node B", inputs: ["node A"] }),
            ],
            operatorList: [operator("pluginA", 0)],
        });
        // 2 nodes and 1 edge
        await waitFor(async () => {
            expect(currentContext().elements).toHaveLength(3);
            expect(currentContext().ruleOperatorNodes()).toHaveLength(2);
            expect(currentContext().ruleOperatorNodes()[1].inputs).toStrictEqual(["node A"]);
        });
    });

    it("should keep node menus in rule-only layout when the editor is not read-only", async () => {
        await ruleEditorModel({
            initialRuleNodes: [node({ nodeId: "node A", pluginId: "pluginA", portSpecification: { type: "count", minInputPorts: 0 } })],
            operatorList: [operator("pluginA", 0)],
            contextOverrides: {
                showRuleOnly: true,
            },
        });

        await waitFor(() => {
            expect(nodeById("node A").data.menuButtons).toBeTruthy();
        });
    });

    it("should hide node menus in read-only mode", async () => {
        await ruleEditorModel({
            initialRuleNodes: [node({ nodeId: "node A", pluginId: "pluginA", portSpecification: { type: "count", minInputPorts: 0 } })],
            operatorList: [operator("pluginA", 0)],
            contextOverrides: {
                readOnlyMode: true,
            },
        });

        await waitFor(() => {
            expect(nodeById("node A").data.menuButtons).toBeUndefined();
        });
    });

    it("should add new nodes and undo & redo", async () => {
        await ruleEditorModel({
            initialRuleNodes: [node({ nodeId: "pluginA" }), node({ nodeId: "node B", inputs: ["pluginA"] })],
            operatorList: [operator("pluginA")],
        });
        const checkBeforeAdd = () => {
            expect(currentContext().elements).toHaveLength(3);
            expect(
                currentContext()
                    .elements.map((e) => e.id)
                    .sort((left, right) => (left < right ? 1 : -1)),
            ).toStrictEqual(["pluginA", "node B", "1"]);
        };
        await waitFor(checkBeforeAdd);

        // Add nodes
        const position = { x: 5, y: 10 };
        act(() => {
            currentContext().executeModelEditOperation.addNode(operator("pluginA"), position);
            currentContext().executeModelEditOperation.addNode(operator("pluginA"), position);
        });
        const checkAfterAddedNodes = () => {
            checkAfterChange();
            // 4 nodes, 1 edge
            expect(currentContext().elements).toHaveLength(5);
            checkAfterChange();
            expect(
                currentContext()
                    .ruleOperatorNodes()
                    .map((node) => node.nodeId),
            ).toStrictEqual(["pluginA", "node B", "pluginA_2", "pluginA_3"]);
            expect(
                modelUtils.asNode(currentContext().elements.find((n) => n.id === "pluginA_2"))!!.position,
            ).toStrictEqual(position);
            expect(currentContext().ruleOperatorNodes()[2].position).toStrictEqual(position);
        };
        checkAfterAddedNodes();

        checkUndoAndRedo(checkBeforeAdd, checkAfterAddedNodes);
    });

    it("should change style and undo & redo", async () => {
        await stickyNoteNodeBootstrap();
        //the default style object created by Color package for color #000000 supplied
        const defaultStyle = {
            backgroundColor: "rgb(194, 194, 194)",
            borderColor: "#000000",
            color: "#000",
        };
        const checkBeforeChange = () => {
            const node = allStickyNodes()[0];
            expect(node.data.style).toEqual(defaultStyle);
            return node;
        };
        const node = await waitFor(checkBeforeChange);

        const checkAfterChange = () => {
            expect(modelUtils.nodeById(currentContext().elements, node.id)!!.data.style).not.toStrictEqual(
                defaultStyle,
            );
        };
        act(() => {
            currentContext().executeModelEditOperation.changeStickyNodeProperties(node.id, "#fee2f1");
        });
        checkAfterChange();

        checkUndoAndRedo(checkBeforeChange, checkAfterChange);
    });

    it("should change node text content and undo & redo", async () => {
        const stickyNote = "# Testing... 1 2 3...";
        await stickyNoteNodeBootstrap(stickyNote);
        const checkBeforeChange: () => Promise<FlowElement> = async () => {
            return await waitFor(() => {
                const node = allStickyNodes()[0];
                expect(node.data.businessData.stickyNote).toStrictEqual(stickyNote);
                return node;
            });
        };
        const node = await checkBeforeChange();
        const newContent = "**new Content**";
        const checkAfterChange = () => {
            expect(modelUtils.nodeById(currentContext().elements, node.id)!!.data.businessData.stickyNote).toEqual(
                newContent,
            );
        };
        act(() => {
            currentContext().executeModelEditOperation.changeStickyNodeProperties(node.id, "#000", newContent);
        });
        checkAfterChange();

        checkUndoAndRedo(checkBeforeChange, checkAfterChange);
    });

    it("should change size and undo & redo", async () => {
        await stickyNoteNodeBootstrap();
        const defaultNodeDimensions = { width: DEFAULT_NODE_WIDTH, height: DEFAULT_NODE_HEIGHT };
        const node = allStickyNodes()[0];
        const checkBeforeChange = () => {
            expect(node.data.nodeDimensions).toEqual(defaultNodeDimensions);
        };
        await waitFor(checkBeforeChange);
        const randomNewNodeDimensions = { width: DEFAULT_NODE_WIDTH + 30, height: DEFAULT_NODE_HEIGHT + 10 };
        const checkAfterChange = () => {
            expect(modelUtils.nodeById(currentContext().elements, node.id)!!.data.nodeDimensions).toEqual(
                randomNewNodeDimensions,
            );
        };
        act(() => {
            currentContext().executeModelEditOperation.changeSize(node.id, randomNewNodeDimensions);
        });
        checkAfterChange();

        checkUndoAndRedo(checkBeforeChange, checkAfterChange);
    });

    it("should delete nodes and undo & redo", async () => {
        await ruleEditorModel({
            initialRuleNodes: [
                node({ nodeId: "nodeA" }),
                node({ nodeId: "nodeB", inputs: ["nodeA"] }),
                node({ nodeId: "nodeC", inputs: ["nodeA", "nodeB"] }),
            ],
            operatorList: [operator("pluginA")],
        });
        const checkBeforeDelete = () => {
            // 3 nodes, 3 edges
            expect(currentContext().elements).toHaveLength(6);
        };
        await waitFor(checkBeforeDelete);

        // Delete node
        act(() => {
            currentContext().executeModelEditOperation.deleteNode("nodeA");
        });
        const checkAfterDelete = () => {
            checkAfterChange();
            // 1 node and 2 edges removed
            expect(currentContext().elements).toHaveLength(3);
            expect(
                currentContext()
                    .ruleOperatorNodes()
                    .map((node) => node.nodeId),
            ).toStrictEqual(["nodeB", "nodeC"]);
        };
        checkAfterDelete();

        checkUndoAndRedo(checkBeforeDelete, checkAfterDelete);
    });

    it("should move a node and undo & redo", async () => {
        await ruleEditorModel({ initialRuleNodes: [], operatorList: [operator("pluginA")] });
        const startPosition = { x: 2, y: 4 };
        act(() => {
            currentContext().executeModelEditOperation.addNode(operator("pluginA"), startPosition);
        });
        const nodeId = currentContext().elements[0].id;
        const checkBeforeMove = () => {
            expect(modelUtils.nodeById(currentContext().elements, nodeId)!!.position).toStrictEqual(startPosition);
        };
        await waitFor(checkBeforeMove);

        // Move node
        const newPosition = { x: 100, y: 102 };
        act(() => {
            currentContext().executeModelEditOperation.startChangeTransaction();
            currentContext().executeModelEditOperation.moveNode(nodeId, newPosition);
        });
        const checkAfterMove = () => {
            checkAfterChange();
            expect(nodeById(nodeId).position).toStrictEqual(newPosition);
            expect(currentContext().ruleOperatorNodes()[0].position).toStrictEqual(newPosition);
        };
        checkAfterMove();

        // UNDO
        act(() => {
            currentContext().undo();
        });
        checkAfterUndo(true);
        checkBeforeMove();

        // REDO
        act(() => {
            currentContext().redo();
        });
        checkAfterMove();
        currentContext().saveRule();
        expect(savedRuleOperatorNodes).toHaveLength(1);
        expect(savedRuleOperatorNodes[0].position).toBe(newPosition);
    });

    it("should move multiple nodes by an offset", async () => {
        await ruleEditorModel({
            initialRuleNodes: [
                node({ nodeId: "nodeA", position: { x: 1, y: 2 } }),
                node({ nodeId: "nodeB", inputs: ["nodeA"], position: { x: 2, y: 3 } }),
                node({ nodeId: "nodeC", position: { x: 3, y: 4 } }),
            ],
            operatorList: [operator("pluginA")],
        });
        const checkPositions = (data: [string, number, number][]) => {
            expect(currentOperatorNodes().map((n) => ({ pos: n.position, id: n.nodeId }))).toStrictEqual(
                data.map((d) => ({ id: `node${d[0]}`, pos: { x: d[1], y: d[2] } })),
            );
        };
        const beforeUpdateCheck = () => {
            checkPositions([
                ["A", 1, 2],
                ["B", 2, 3],
                ["C", 3, 4],
            ]);
        };
        beforeUpdateCheck();
        act(() => {
            currentContext().executeModelEditOperation.moveNodes(["nodeA", "nodeC"], { x: 5, y: 10 });
        });
        const afterUpdateCheck = () => {
            checkPositions([
                ["A", 6, 12],
                ["B", 2, 3],
                ["C", 8, 14],
            ]);
        };
        afterUpdateCheck();

        checkUndoAndRedo(beforeUpdateCheck, afterUpdateCheck);
    });

    it("should change node parameters and undo & redo", async () => {
        await ruleEditorModel({ initialRuleNodes: [node({ nodeId: "nodeA" })], operatorList: [operator("pluginA")] });
        const checkParameters = (expectedParameterValues: string[] = ["Value A", "Value B"]) => {
            expect(currentOperatorNodes()[0].parameters).toStrictEqual({
                "param A": expectedParameterValues[0],
                "param B": expectedParameterValues[1],
            });
        };
        checkParameters();

        // Change parameters
        act(() => {
            currentContext().executeModelEditOperation.changeNodeParameter("nodeA", "param A", "A");
            currentContext().executeModelEditOperation.changeNodeParameter("nodeA", "param A", "A2");
            // Changing another parameter should trigger a new transaction
            currentContext().executeModelEditOperation.changeNodeParameter("nodeA", "param B", "B");
            currentContext().executeModelEditOperation.changeNodeParameter("nodeA", "param B", "B2");
            // This should again trigger a new transaction
            currentContext().executeModelEditOperation.moveNode("nodeA", { x: 1, y: 1 });
        });
        act(() => {
            // Need to run this in separate act, since moveNode runs async
            currentContext().executeModelEditOperation.changeNodeParameter("nodeA", "param B", "B3");
            currentContext().executeModelEditOperation.changeNodeParameter("nodeA", "param A", "A3");
        });
        checkParameters(["A3", "B3"]);

        const expectedValueHistory = [
            ["Value A", "Value B"],
            ["A2", "Value B"],
            ["A2", "B2"],
            ["A2", "B3"],
            ["A3", "B3"],
        ];
        // UNDO
        for (let i = expectedValueHistory.length - 1; i > 0; i--) {
            expect(currentContext().canUndo);
            checkParameters(expectedValueHistory[i]);
            act(() => {
                currentContext().undo();
            });
            checkAfterUndo(i > 1);
            checkParameters(expectedValueHistory[i - 1]);
        }

        // REDO
        for (let i = 0; i < expectedValueHistory.length - 1; i++) {
            expect(currentContext().canRedo);
            checkParameters(expectedValueHistory[i]);
            act(() => {
                currentContext().redo();
            });
            checkParameters(expectedValueHistory[i + 1]);
        }
        checkAfterChange();
    });

    it("should execute external rule model changes and undo & redo them", async () => {
        await ruleEditorModel({ initialRuleNodes: [node({ nodeId: "nodeA" })], operatorList: [operator("pluginA")] });
        let externalState = "initial";

        const checkBeforeChange = () => {
            expect(externalState).toBe("initial");
            expect(currentContext().canUndo).toBe(false);
        };
        checkBeforeChange();

        act(() => {
            currentContext().executeModelEditOperation.startChangeTransaction();
            currentContext().executeExternalRuleModelChange({
                do: () => {
                    externalState = "updated";
                },
                undo: () => {
                    externalState = "initial";
                },
            });
        });

        const checkAfterChange = () => {
            expect(externalState).toBe("updated");
            expect(currentContext().canUndo).toBe(true);
            expect(currentContext().canRedo).toBe(false);
        };
        checkAfterChange();

        checkUndoAndRedo(checkBeforeChange, checkAfterChange);
    });

    it("should execute external rule model changes in the same transaction as canvas changes", async () => {
        await ruleEditorModel({ initialRuleNodes: [node({ nodeId: "nodeA" })], operatorList: [operator("pluginA")] });
        let externalState = "present";

        const checkBeforeDelete = () => {
            expect(currentContext().elements).toHaveLength(1);
            expect(externalState).toBe("present");
        };
        checkBeforeDelete();

        act(() => {
            currentContext().executeModelEditOperation.startChangeTransaction();
            currentContext().executeModelEditOperation.deleteNode("nodeA");
            currentContext().executeExternalRuleModelChange({
                do: () => {
                    externalState = "deleted";
                },
                undo: () => {
                    externalState = "present";
                },
            });
        });

        const checkAfterDelete = () => {
            expect(currentContext().elements).toHaveLength(0);
            expect(externalState).toBe("deleted");
            expect(currentContext().canUndo).toBe(true);
            expect(currentContext().canRedo).toBe(false);
        };
        checkAfterDelete();

        checkUndoAndRedo(checkBeforeDelete, checkAfterDelete);
    });

    it("should update node metadata without changing parameters and undo & redo", async () => {
        await ruleEditorModel({ initialRuleNodes: [node({ nodeId: "nodeA" })], operatorList: [operator("pluginA")] });

        const checkBeforeUpdate = () => {
            const canvasNode = nodeById("nodeA");
            expect(canvasNode.data.businessData.originalRuleOperatorNode.label).toBe("nodeA");
            expect(canvasNode.data.businessData.originalRuleOperatorNode.description).toBeUndefined();
            expect(canvasNode.data.businessData.originalRuleOperatorNode.tags).toBeUndefined();
            expect(currentOperatorNodes()[0].parameters).toStrictEqual(defaultParameters);
        };
        checkBeforeUpdate();

        act(() => {
            currentContext().updateRuleOperatorNodeMetaData(["nodeA"], () => ({
                label: "Updated node label",
                description: "Updated node description",
                tags: ["Rule block"],
            }));
        });

        const checkAfterUpdate = () => {
            const canvasNode = nodeById("nodeA");
            expect(canvasNode.data.businessData.originalRuleOperatorNode.label).toBe("Updated node label");
            expect(canvasNode.data.businessData.originalRuleOperatorNode.description).toBe("Updated node description");
            expect(canvasNode.data.businessData.originalRuleOperatorNode.tags).toStrictEqual(["Rule block"]);
            expect(currentOperatorNodes()[0].label).toBe("Updated node label");
            expect(currentOperatorNodes()[0].description).toBe("Updated node description");
            expect(currentOperatorNodes()[0].tags).toStrictEqual(["Rule block"]);
            expect(currentOperatorNodes()[0].parameters).toStrictEqual(defaultParameters);
            checkAfterChange();
        };
        checkAfterUpdate();

        checkUndoAndRedo(checkBeforeUpdate, checkAfterUpdate);
    });

    it("should save rule parameters correctly", async () => {
        await ruleEditorModel({
            initialRuleNodes: [
                node({
                    nodeId: "nodeA",
                    parameters: {
                        "param A": "just a string",
                        "param B": {
                            label: "with label",
                            value: "0",
                        },
                    },
                }),
            ],
            operatorList: [operator("pluginA")],
        });
        act(() => {
            // Need to run this in separate act, since moveNode runs async
            currentContext().executeModelEditOperation.changeNodeParameter("nodeA", "param A", "still a string");
        });
        currentContext().saveRule();
        expect(savedRuleOperatorNodes).toHaveLength(1);
        expect(savedRuleOperatorNodes[0].parameters).toStrictEqual({
            "param A": "still a string",
            "param B": "0",
        });
    });

    it("should keep undo history after saving and track the saved state marker", async () => {
        await ruleEditorModel({
            initialRuleNodes: [node({ nodeId: "nodeA", position: { x: 1, y: 2 } })],
            operatorList: [operator("pluginA")],
        });

        act(() => {
            currentContext().executeModelEditOperation.startChangeTransaction();
            currentContext().executeModelEditOperation.moveNode("nodeA", { x: 10, y: 20 });
        });
        expect(currentContext().unsavedChanges).toBe(true);

        await act(async () => {
            await currentContext().saveRule();
        });
        expect(currentContext().canUndo).toBe(true);
        expect(currentContext().unsavedChanges).toBe(false);
        expect(currentContext().savedStatePosition).toBe("current");

        act(() => {
            currentContext().undo();
        });
        expect(nodeById("nodeA").position).toStrictEqual({ x: 1, y: 2 });
        expect(currentContext().canRedo).toBe(true);
        expect(currentContext().unsavedChanges).toBe(true);
        expect(currentContext().savedStatePosition).toBe("before");

        act(() => {
            currentContext().redo();
        });
        expect(nodeById("nodeA").position).toStrictEqual({ x: 10, y: 20 });
        expect(currentContext().unsavedChanges).toBe(false);
        expect(currentContext().savedStatePosition).toBe("current");
    });

    it("should reset to saved state without clearing undo and redo history when the saved marker is still available", async () => {
        await ruleEditorModel({
            initialRuleNodes: [node({ nodeId: "nodeA", position: { x: 1, y: 2 } })],
            operatorList: [operator("pluginA")],
        });

        act(() => {
            currentContext().executeModelEditOperation.startChangeTransaction();
            currentContext().executeModelEditOperation.moveNode("nodeA", { x: 10, y: 20 });
        });
        await act(async () => {
            await currentContext().saveRule();
        });
        act(() => {
            currentContext().executeModelEditOperation.startChangeTransaction();
            currentContext().executeModelEditOperation.moveNode("nodeA", { x: 100, y: 200 });
        });
        expect(currentContext().savedStatePosition).toBe("after");
        expect(currentContext().resetToSavedStateClearsHistory).toBe(false);

        await act(async () => {
            currentContext().resetToSavedState();
        });
        expect(nodeById("nodeA").position).toStrictEqual({ x: 10, y: 20 });
        expect(currentContext().savedStatePosition).toBe("current");
        expect(currentContext().canUndo).toBe(true);
        expect(currentContext().canRedo).toBe(true);

        act(() => {
            currentContext().undo();
        });
        expect(nodeById("nodeA").position).toStrictEqual({ x: 1, y: 2 });
        act(() => {
            currentContext().redo();
        });
        expect(nodeById("nodeA").position).toStrictEqual({ x: 10, y: 20 });
        act(() => {
            currentContext().redo();
        });
        expect(nodeById("nodeA").position).toStrictEqual({ x: 100, y: 200 });
    });

    it("should clear undo and redo history when resetting to a saved state that is no longer in history", async () => {
        await ruleEditorModel({
            initialRuleNodes: [node({ nodeId: "nodeA", position: { x: 1, y: 2 } })],
            operatorList: [operator("pluginA")],
        });

        act(() => {
            currentContext().executeModelEditOperation.startChangeTransaction();
            currentContext().executeModelEditOperation.moveNode("nodeA", { x: 10, y: 20 });
        });
        await act(async () => {
            await currentContext().saveRule();
        });
        act(() => {
            currentContext().undo();
        });
        act(() => {
            currentContext().executeModelEditOperation.startChangeTransaction();
            currentContext().executeModelEditOperation.moveNode("nodeA", { x: 50, y: 60 });
        });
        expect(currentContext().savedStatePosition).toBe("before");
        expect(currentContext().resetToSavedStateClearsHistory).toBe(true);

        await act(async () => {
            currentContext().resetToSavedState();
        });
        expect(nodeById("nodeA").position).toStrictEqual({ x: 10, y: 20 });
        expect(currentContext().savedStatePosition).toBe("current");
        expect(currentContext().canUndo).toBe(false);
        expect(currentContext().canRedo).toBe(false);
    });

    it("should delete multiple nodes and undo & redo", async () => {
        await ruleEditorModel({
            initialRuleNodes: [
                node({ nodeId: "nodeA" }),
                node({ nodeId: "nodeB", inputs: ["nodeA"] }),
                node({ nodeId: "nodeC", inputs: ["nodeA", "nodeB"] }),
            ],
            operatorList: [operator("pluginA")],
        });
        const checkBeforeDelete = () => {
            expect(currentContext().elements).toHaveLength(6);
        };
        await waitFor(checkBeforeDelete);
        act(() => {
            currentContext().executeModelEditOperation.deleteNodes(["nodeA", "nodeC"]);
        });
        const checkAfterDelete = () => {
            checkAfterChange();
            expect(currentContext().elements).toHaveLength(1);
        };
        checkAfterDelete();

        checkUndoAndRedo(checkBeforeDelete, checkAfterDelete);
    });

    it("should copy and paste multiple nodes and undo & redo", async () => {
        await ruleEditorModel({
            initialRuleNodes: [
                node({ nodeId: "nodeA" }),
                node({ nodeId: "nodeB", inputs: ["nodeA"] }),
                node({ nodeId: "nodeC", inputs: ["nodeA", "nodeB"] }),
            ],
        });
        const checkBeforeCopyAndPaste = () => {
            expect(currentContext().elements).toHaveLength(6);
        };
        await waitFor(checkBeforeCopyAndPaste);

        // Copy and paste first time
        act(() => {
            currentContext().executeModelEditOperation.copyAndPasteNodes(["nodeB", "nodeC"], { x: 10, y: 10 });
        });
        const checkAfterCopyAndPaste = () => {
            // 2 nodes and 1 edge added
            expect(currentContext().elements).toHaveLength(9);
            expect(new Set(currentOperatorNodes().map((op) => op.nodeId)).size).toBe(
                currentContext().ruleOperatorNodes().length,
            );
        };
        checkAfterCopyAndPaste();
        // Copy and paste second time
        act(() => {
            currentContext().executeModelEditOperation.startChangeTransaction();
            currentContext().executeModelEditOperation.copyAndPasteNodes(["nodeB", "nodeC"], { x: 20, y: 20 });
        });
        const checkAfterCopyAndPaste2nd = () => {
            checkAfterChange();
            // 2 nodes and 1 edge added
            expect(currentContext().elements).toHaveLength(12);
            expect(new Set(currentOperatorNodes().map((op) => op.nodeId)).size).toBe(
                currentContext().ruleOperatorNodes().length,
            );
        };
        checkAfterCopyAndPaste2nd();
        checkUndoAndRedo(checkBeforeCopyAndPaste, checkAfterCopyAndPaste, checkAfterCopyAndPaste2nd);
    });

    it("should ignore rule-editor copy and paste shortcuts in read-only mode", async () => {
        const clipboardStore = createClipboardStore();
        await ruleEditorModel({
            initialRuleNodes: [node({ nodeId: "nodeA", pluginId: "pluginA", portSpecification: { type: "count", minInputPorts: 0 } })],
            operatorList: [operator("pluginA", 0)],
            contextOverrides: {
                readOnlyMode: true,
            },
        });

        act(() => {
            currentContext().updateSelectedElements(
                currentContext().elements.filter((element) => modelUtils.isNode(element) && element.id === "nodeA"),
            );
        });

        const copyEvent = await dispatchClipboardEvent("copy", clipboardStore.clipboardData);
        expect(copyEvent.preventDefault).not.toHaveBeenCalled();
        expect(clipboardStore.clipboardData.setData).not.toHaveBeenCalled();

        clipboardStore.clipboardData.setData!(
            "text/plain",
            JSON.stringify({
                task: {
                    data: {
                        nodes: [
                            {
                                nodeId: "copiedNode",
                                pluginId: "pluginA",
                                pluginType: "unknown",
                                position: { x: 10, y: 20 },
                                dimension: undefined,
                                parameters: defaultParameters,
                                inputHandleIds: [],
                            },
                        ],
                        edges: [],
                    },
                    metaData: {
                        project: "testProject",
                        task: "taskA",
                    },
                },
            }),
        );
        const pasteEvent = await dispatchClipboardEvent("paste", clipboardStore.clipboardData);
        expect(pasteEvent.preventDefault).not.toHaveBeenCalled();
        expect(currentContext().ruleOperatorNodes()).toHaveLength(1);
    });

    it("should convert pasted paths into rule-block input ports, deduplicate identical source paths and keep target paths separate", async () => {
        const clipboardStore = await copySelectedNodesToClipboard({
            initialRuleNodes: [
                node({
                    nodeId: "sourcePathA",
                    pluginId: "sourcePathInput",
                    pluginType: "PathInputOperator",
                    portSpecification: { type: "count", minInputPorts: 0 },
                    parameters: {
                        path: "foaf:name",
                    },
                }),
                node({
                    nodeId: "sourcePathB",
                    pluginId: "sourcePathInput",
                    pluginType: "PathInputOperator",
                    portSpecification: { type: "count", minInputPorts: 0 },
                    parameters: {
                        path: "foaf:name",
                    },
                }),
                node({
                    nodeId: "targetPath",
                    pluginId: "targetPathInput",
                    pluginType: "PathInputOperator",
                    portSpecification: { type: "count", minInputPorts: 0 },
                    parameters: {
                        path: "foaf:name",
                    },
                }),
                node({
                    nodeId: "transformNode",
                    pluginId: "concat",
                    pluginType: "TransformOperator",
                    inputs: ["sourcePathA", "sourcePathB"],
                }),
            ],
            operatorList: [
                operator("sourcePathInput", 0, "PathInputOperator"),
                operator("targetPathInput", 0, "PathInputOperator"),
                operator("concat", 1, "TransformOperator"),
            ],
            selectedNodeIds: ["sourcePathA", "sourcePathB", "targetPath", "transformNode"],
            instanceId: "path-source",
        });

        expect(clipboardStore.readTask()).toMatchObject({
            data: {
                edges: [
                    expect.objectContaining({ source: "sourcePathA", target: "transformNode", targetHandle: "0" }),
                    expect.objectContaining({ source: "sourcePathB", target: "transformNode", targetHandle: "1" }),
                ],
            },
        });

        const destination = await mountRuleBlockClipboardDestination({
            clipboardStore,
            currentTaskId: "ruleBlockTarget",
            instanceId: "path-destination",
        });

        const checkAfterPaste = () => {
            expect(destination.pasteEvent.preventDefault).not.toHaveBeenCalled();
            expect(currentContext().elements).toHaveLength(6);
            expect(currentContext().ruleOperatorNodes()).toHaveLength(4);
            expect(
                currentContext()
                    .ruleOperatorNodes()
                    .filter((operatorNode) => operatorNode.pluginType === "InputPortOperator")
                    .map((operatorNode) => operatorNode.parameters.portId),
            ).toStrictEqual(["generatedPort1", "generatedPort1", "generatedPort2"]);
            expect(destination.currentExternalPorts()).toStrictEqual([
                ruleBlockPort("generatedPort1", "foaf:name", 1),
                ruleBlockPort("generatedPort2", "foaf:name", 2),
            ]);
        };

        await waitFor(checkAfterPaste);

        act(() => {
            currentContext().undo();
        });
        expect(currentContext().elements).toHaveLength(0);
        expect(destination.currentExternalPorts()).toStrictEqual([]);

        act(() => {
            currentContext().redo();
        });
        checkAfterPaste();
    });

    it("should reconcile copied input-port nodes for external and same-rule clipboard pastes", async () => {
        const externalClipboardStore = await copySelectedNodesToClipboard({
            initialRuleNodes: [
                node({
                    nodeId: "externalInputNode",
                    pluginId: "inputPort",
                    pluginType: "InputPortOperator",
                    portSpecification: { type: "count", minInputPorts: 0 },
                    parameters: {
                        portId: "externalPort",
                    },
                }),
            ],
            operatorList: [operator("inputPort", 0, "InputPortOperator")],
            selectedNodeIds: ["externalInputNode"],
            instanceId: "external-source",
            editedItemId: "externalRuleBlock",
            extendClipboardCopy: () => ({
                inputPorts: [ruleBlockPort("externalPort", "External input", 3, "External description")],
            }),
        });

        const externalDestination = await mountRuleBlockClipboardDestination({
            clipboardStore: externalClipboardStore,
            currentTaskId: "destinationRuleBlock",
            existingPorts: [ruleBlockPort("existingPort", "Existing", 1)],
            instanceId: "external-destination",
        });

        const checkAfterExternalPaste = () => {
            expect(currentContext().ruleOperatorNodes()).toHaveLength(1);
            expect(currentContext().ruleOperatorNodes()[0].parameters.portId).toBe("generatedPort1");
            expect(externalDestination.currentExternalPorts()).toStrictEqual([
                ruleBlockPort("existingPort", "Existing", 1),
                ruleBlockPort("generatedPort1", "External input", 2, "External description"),
            ]);
        };

        await waitFor(checkAfterExternalPaste);
        act(() => {
            currentContext().undo();
        });
        expect(currentContext().ruleOperatorNodes()).toHaveLength(0);
        expect(externalDestination.currentExternalPorts()).toStrictEqual([ruleBlockPort("existingPort", "Existing", 1)]);
        act(() => {
            currentContext().redo();
        });
        checkAfterExternalPaste();

        const sameRuleClipboardStore = await copySelectedNodesToClipboard({
            initialRuleNodes: [
                node({
                    nodeId: "sameRuleInputNode",
                    pluginId: "inputPort",
                    pluginType: "InputPortOperator",
                    portSpecification: { type: "count", minInputPorts: 0 },
                    parameters: {
                        portId: "sharedPort",
                    },
                }),
            ],
            operatorList: [operator("inputPort", 0, "InputPortOperator")],
            selectedNodeIds: ["sameRuleInputNode"],
            instanceId: "same-source",
            editedItemId: "sharedRuleBlock",
            extendClipboardCopy: () => ({
                inputPorts: [ruleBlockPort("sharedPort", "Shared input", 1)],
            }),
        });

        const sameRuleDestination = await mountRuleBlockClipboardDestination({
            clipboardStore: sameRuleClipboardStore,
            currentTaskId: "sharedRuleBlock",
            existingPorts: [ruleBlockPort("sharedPort", "Shared input", 1)],
            instanceId: "same-destination",
        });

        const checkAfterSameRulePaste = () => {
            expect(currentContext().ruleOperatorNodes()).toHaveLength(1);
            expect(currentContext().ruleOperatorNodes()[0].parameters.portId).toBe("sharedPort");
            expect(sameRuleDestination.currentExternalPorts()).toStrictEqual([ruleBlockPort("sharedPort", "Shared input", 1)]);
        };

        await waitFor(checkAfterSameRulePaste);
        act(() => {
            currentContext().undo();
        });
        expect(currentContext().ruleOperatorNodes()).toHaveLength(0);
        expect(sameRuleDestination.currentExternalPorts()).toStrictEqual([ruleBlockPort("sharedPort", "Shared input", 1)]);
        act(() => {
            currentContext().redo();
        });
        checkAfterSameRulePaste();
    });

    it("should create value edges for input port operators", async () => {
        await ruleEditorModel({
            initialRuleNodes: [
                node({
                    nodeId: "inputPortNode",
                    pluginId: "inputPort",
                    pluginType: "InputPortOperator",
                    portSpecification: { type: "count", minInputPorts: 0 },
                    parameters: {
                        portId: "inputPortA",
                    },
                }),
                node({
                    nodeId: "transformNode",
                    pluginId: "concat",
                    pluginType: "TransformOperator",
                    inputs: ["inputPortNode"],
                }),
            ],
            operatorList: [operator("inputPort", 0, "InputPortOperator"), operator("concat", 1, "TransformOperator")],
        });

        expect(
            currentContext().elements.filter((elem) => modelUtils.isEdge(elem)).map((edge) => modelUtils.asEdge(edge)?.type),
        ).toStrictEqual(["value"]);
    });

    it("should reject invalid clipboard content without creating nodes or external rule-block ports", async () => {
        const invalidClipboardStore = await copySelectedNodesToClipboard({
            initialRuleNodes: [
                node({
                    nodeId: "comparisonNode",
                    pluginId: "levenshtein",
                    pluginType: "ComparisonOperator",
                    portSpecification: { type: "count", minInputPorts: 2 },
                }),
            ],
            operatorList: [operator("levenshtein", 2, "ComparisonOperator")],
            selectedNodeIds: ["comparisonNode"],
            instanceId: "invalid-source",
        });

        const invalidDestination = await mountRuleBlockClipboardDestination({
            clipboardStore: invalidClipboardStore,
            currentTaskId: "invalidDestination",
            instanceId: "invalid-destination",
        });

        await waitFor(() => {
            expect(invalidDestination.pasteEvent.preventDefault).not.toHaveBeenCalled();
            expect(currentContext().elements).toHaveLength(0);
            expect(currentContext().ruleOperatorNodes()).toHaveLength(0);
            expect(invalidDestination.currentExternalPorts()).toStrictEqual([]);
            expect(currentContext().canUndo).toBe(false);
        });
    });

    it("should add an edge and undo & redo", async () => {
        await ruleEditorModel({ initialRuleNodes: [node({ nodeId: "nodeA" }), node({ nodeId: "nodeB" })] });
        const checkBeforeAdd = () => {
            expect(currentContext().elements).toHaveLength(2);
        };
        await waitFor(checkBeforeAdd);

        // Add edge
        act(() => {
            currentContext().executeModelEditOperation.addEdge("nodeA", "nodeB", "0");
        });
        const checkAfterAdd = () => {
            checkAfterChange();
            expect(currentContext().elements).toHaveLength(3);
            const nodeB = currentOperatorNodes().find((node) => node.nodeId === "nodeB")!!;
            expect(nodeB.inputs).toHaveLength(1);
            expect(nodeB.inputs[0]).toEqual("nodeA");
        };
        checkAfterAdd();

        checkUndoAndRedo(checkBeforeAdd, checkAfterAdd);
    });

    it("should delete an edge and undo & redo", async () => {
        await ruleEditorModel({
            initialRuleNodes: [
                node({ nodeId: "nodeA" }),
                node({ nodeId: "nodeB", inputs: ["nodeA"] }),
                node({ nodeId: "nodeC", inputs: ["nodeA", "nodeB"] }),
            ],
        });
        const edge = currentContext().elements.find(
            (elem) => modelUtils.isEdge(elem) && modelUtils.asEdge(elem)!!.target === "nodeB",
        );
        const before = currentContext().elements.length;
        const checkBeforeDelete = () => {
            expect(currentContext().elements).toHaveLength(before);
        };
        await waitFor(checkBeforeDelete);

        // Delete edge
        act(() => {
            currentContext().executeModelEditOperation.deleteEdge(edge!!.id);
        });
        const checkAfterDelete = () => {
            expect(currentContext().elements).toHaveLength(before - 1);
        };
        checkAfterDelete();

        checkUndoAndRedo(checkBeforeDelete, checkAfterDelete);
    });

    it("should undo and redo complex change chains", async () => {
        const stateHistory: (IRuleOperatorNode | StickyNote)[][] = [];
        const stateHistoryLabel: string[] = [];
        const currentStickyNodes = () =>
            currentContext().elements.reduce((stickyNodes, elem) => {
                if (modelUtils.isNode(elem) && elem.type === LINKING_NODE_TYPES.stickynote) {
                    const node = modelUtils.asNode(elem)!;
                    stickyNodes.push(nodeDefaultUtils.transformNodeToStickyNode(node) as StickyNote);
                }
                return stickyNodes;
            }, [] as StickyNote[]);
        const allNodes = () => [...currentOperatorNodes(), ...currentStickyNodes()];
        const recordCurrentState = (stateLabel: string) => {
            stateHistory.push(allNodes());
            stateHistoryLabel.push(stateLabel);
        };
        const recordedTransaction = async (
            stateLabel: string,
            changeAction: () => any,
            additionalCheck: () => any | Promise<any> = () => {},
        ) => {
            await act(async () => {
                currentContext().executeModelEditOperation.startChangeTransaction();
                await changeAction();
            });
            // Check that something has changed
            await waitFor(() => {
                expect(allNodes()).not.toStrictEqual(stateHistory[stateHistory.length - 1]);
            });
            await additionalCheck();
            recordCurrentState(stateLabel);
        };
        const resultApi = new RenderResultApi(
            await ruleEditorModel({
                initialRuleNodes: [
                    node({ nodeId: "nodeA" }),
                    node({ nodeId: "nodeB", inputs: ["nodeA"] }),
                    node({ nodeId: "nodeC", inputs: ["nodeB"] }),
                ],
            }),
        );
        recordCurrentState("Initial state");
        expect(currentContext().canUndo).toBe(false);
        // Record every change and check that after undo and later redo the states match
        await recordedTransaction(
            "Add a node",
            () => {
                currentContext().executeModelEditOperation.addNode(operator("pluginA"), { x: 1, y: 2 });
            },
            async () => {
                await waitFor(() => {
                    expect(currentContext().canUndo).toBe(true);
                });
            },
        );
        await recordedTransaction("Move node", () => {
            currentContext().executeModelEditOperation.moveNode("nodeA", { x: 2, y: 3 });
        });
        await recordedTransaction("Add edge", () => {
            currentContext().executeModelEditOperation.addEdge("pluginA", "nodeA", "0");
        });
        await recordedTransaction(
            "Auto-layout",
            () => {
                currentContext().executeModelEditOperation.autoLayout();
            },
            async () => {
                // Auto-layout is async, so we need to wait for the change to take place.
                await waitFor(() => {
                    expect(allNodes()).not.toStrictEqual(stateHistory[stateHistory.length - 1]);
                });
            },
        );
        await recordedTransaction("Change node parameter", () => {
            currentContext().executeModelEditOperation.changeNodeParameter("nodeA", "param A", "new param value");
            currentContext().executeModelEditOperation.changeNodeParameter("nodeA", "param A", "new param value 2");
            currentContext().executeModelEditOperation.changeNodeParameter("nodeA", "param A", "new param value 3");
        });
        await recordedTransaction("Copy and paste nodes", () => {
            currentContext().executeModelEditOperation.copyAndPasteNodes(["nodeA", "nodeB"], { x: 10, y: 10 });
        });
        await recordedTransaction("Delete edge", () => {
            currentContext().executeModelEditOperation.deleteEdge("1");
        });
        await recordedTransaction("Delete node", () => {
            currentContext().executeModelEditOperation.deleteNode("nodeA");
        });
        await recordedTransaction("Delete nodes", () => {
            currentContext().executeModelEditOperation.deleteNodes(["nodeB", "pluginA"]);
        });

        await recordedTransaction("Add a sticky node", () => {
            currentContext().executeModelEditOperation.addStickyNode("note", { x: 1, y: 2 }, "#000");
        });

        await recordedTransaction("Change node size", () => {
            currentContext().executeModelEditOperation.changeSize("sticky", { width: 50, height: 32 });
        });

        await recordedTransaction("Change node style", () => {
            currentContext().executeModelEditOperation.changeStickyNodeProperties("sticky", "#ffee13");
        });

        await recordedTransaction("Change node text content", () => {
            currentContext().executeModelEditOperation.changeStickyNodeProperties(
                "sticky",
                "#ffee12",
                "another sticky note",
            );
        });

        expect(allNodes()).toHaveLength(4);
        // // Execute UNDO and REDO twice
        for (let i = 0; i < 2; i++) {
            for (let changeIdx = stateHistory.length - 1; changeIdx > 0; changeIdx--) {
                await waitFor(() => {
                    expect(allNodes()).toStrictEqual(stateHistory[changeIdx]);
                });
                resultApi.assert(
                    currentContext().canUndo,
                    `Undo changes failed because 'can undo' is false. Round: ${i + 1}, Change: ${changeIdx + 1}/${stateHistory.length}. Currently at '${stateHistoryLabel[changeIdx]}' trying to undo to state '${stateHistoryLabel[changeIdx - 1]}'.'`,
                );
                act(() => {
                    currentContext().undo();
                });
                await waitFor(() => {
                    expect(allNodes()).not.toStrictEqual(stateHistory[changeIdx]);
                    expect(allNodes()).toStrictEqual(stateHistory[changeIdx - 1]);
                });
            }
            if (isDebugLoggingEnabled()) {
                console.log("Test REDO");
            }
            for (let changeIdx = 1; changeIdx < stateHistory.length; changeIdx++) {
                expect(currentContext().canRedo).toBe(true);
                act(() => {
                    currentContext().redo();
                });
                if (isDebugLoggingEnabled()) {
                    console.log(
                        `Redone change: ${stateHistoryLabel[changeIdx]} (${changeIdx}/${stateHistory.length - 1})`
                    );
                }
                expect(allNodes()).toStrictEqual(stateHistory[changeIdx]);
            }
        }
    });

    const nodeHasInputs = (nodeId: string, inputs: (string | null)[]) => {
        expect(currentOperatorNodes().find((op) => op.nodeId === nodeId)?.inputs).toStrictEqual(inputs);
    };

    it("should remove an existing edge when a new edge is connected to the same port", async () => {
        await ruleEditorModel({
            initialRuleNodes: [
                node({ nodeId: "nodeA" }),
                node({ nodeId: "nodeB" }),
                node({ nodeId: "nodeC", inputs: ["nodeA"] }),
                node({ nodeId: "nodeD", inputs: ["nodeB"] }),
            ],
        });
        const beforeEditCheck = () => {
            nodeHasInputs("nodeC", ["nodeA"]);
            nodeHasInputs("nodeD", ["nodeB"]);
            expect(currentContext().elements).toHaveLength(6);
        };
        beforeEditCheck();
        act(() => {
            currentContext().executeModelEditOperation.addEdge("nodeB", "nodeC", "0");
        });
        const afterEditCheck = () => {
            nodeHasInputs("nodeC", ["nodeB"]);
            nodeHasInputs("nodeD", []);
            expect(currentContext().elements).toHaveLength(5);
        };
        afterEditCheck();
        checkUndoAndRedo(beforeEditCheck, afterEditCheck);
    });

    it("should swap edges when changing an existing edge to another handle on the same node", async () => {
        await ruleEditorModel({
            initialRuleNodes: [
                node({ nodeId: "nodeA" }),
                node({ nodeId: "nodeB" }),
                node({ nodeId: "nodeC", inputs: ["nodeA", "nodeB"] }),
            ],
        });
        const checkBeforeEdit = () => {
            nodeHasInputs("nodeC", ["nodeA", "nodeB"]);
            expect(currentContext().elements).toHaveLength(5);
        };
        await waitFor(checkBeforeEdit);
        act(() => {
            currentContext().executeModelEditOperation.deleteEdge("1");
            currentContext().executeModelEditOperation.addEdge("nodeA", "nodeC", "1", "0");
        });
        const checkAfterEdit = () => {
            nodeHasInputs("nodeC", ["nodeB", "nodeA"]);
            expect(currentContext().elements).toHaveLength(5);
        };

        checkUndoAndRedo(checkBeforeEdit, checkAfterEdit);
    });

    it("should connect to the first free handle of a node when no handle is specified", async () => {
        await ruleEditorModel({
            initialRuleNodes: [
                node({ nodeId: "nodeA" }),
                node({ nodeId: "nodeB" }),
                node({ nodeId: "nodeC" }),
                node({ nodeId: "nodeD" }),
                node({
                    nodeId: "nodeE",
                    inputs: [undefined, "nodeA"],
                    portSpecification: {
                        type: "count",
                        minInputPorts: 3,
                        maxInputPorts: 3,
                    },
                }),
            ],
        });
        const checkBeforeEdit = () => {
            nodeHasInputs("nodeE", [null, "nodeA", null]);
            expect(currentContext().elements).toHaveLength(6);
        };
        await waitFor(checkBeforeEdit);
        act(() => {
            currentContext().executeModelEditOperation.addEdge("nodeB", "nodeE", undefined);
            currentContext().executeModelEditOperation.addEdge("nodeC", "nodeE", undefined);
            currentContext().executeModelEditOperation.addEdge("nodeD", "nodeE", undefined);
        });
        const checkAfterEdit = () => {
            nodeHasInputs("nodeE", ["nodeB", "nodeA", "nodeC"]);
            expect(currentContext().elements).toHaveLength(8);
        };

        checkUndoAndRedo(checkBeforeEdit, checkAfterEdit);
    });

    it("should increase and decrease input ports for nodes with potentially unlimited input ports (and only for those)", async () => {
        const nrOfDummyNodes = 10;
        const dummyNodes = rangeArray(nrOfDummyNodes).map((idx) => node({ nodeId: "inputNode" + (idx + 1) }));
        const inputHandlesForDummyNodes = dummyNodes.map(() => 1);
        await ruleEditorModel({
            initialRuleNodes: [
                // Each node can only have one output edge, so we need a lot of dummy nodes
                ...dummyNodes,
                node({ nodeId: "nodeA" }),
                node({ nodeId: "nodeA2", portSpecification: { type: "count", minInputPorts: 0, maxInputPorts: 0 } }),
                node({ nodeId: "nodeB" }),
                node({ nodeId: "nodeC", inputs: ["inputNode1", "inputNode2"] }),
                node({
                    nodeId: "nodeD",
                    inputs: ["inputNode3", "inputNode4"],
                    portSpecification: { type: "count", minInputPorts: 2, maxInputPorts: 3 },
                }),
                node({ nodeId: "nodeE", inputs: ["inputNode5", "inputNode6"] }),
                node({ nodeId: "nodeF", inputs: ["inputNode7", "inputNode8"] }),
            ],
        });
        const checkNrOfInputs = (inputs: number[]) => {
            expect(currentReactFlowNodes().map((n) => modelUtils.inputHandles(n).length)).toStrictEqual(inputs);
        };
        const checkBeforeChange = () => {
            checkNrOfInputs([...inputHandlesForDummyNodes, 1, 0, 1, 3, 3, 3, 3]);
        };
        await waitFor(checkBeforeChange);
        act(() => {
            execute().addEdge("nodeA", "nodeB", undefined);
            execute().addEdge("nodeA2", "nodeB", undefined);
            execute().addEdge("inputNode9", "nodeC", undefined);
            // Should not change since the max. number of ports is fixed
            execute().addEdge("inputNode10", "nodeD", undefined);
            // This should not change the number of inputs, since the last connection is left unchanged.
            execute().deleteEdges(
                modelUtils
                    .findEdges({ elements: currentContext().elements, source: "inputNode5", target: "nodeE" })
                    .map((e) => e.id),
            );
            // This should reduce the number of inputs, since the last connection was removed.
            execute().deleteEdges(
                modelUtils
                    .findEdges({ elements: currentContext().elements, source: "inputNode8", target: "nodeF" })
                    .map((e) => e.id),
            );
        });
        const checkAfterChange = () => {
            checkNrOfInputs([...inputHandlesForDummyNodes, 1, 0, 3, 4, 3, 3, 2]);
        };
        checkAfterChange();
        checkUndoAndRedo(checkBeforeChange, checkAfterChange);
    });

    it("should auto-layout nodes and undo & redo", async () => {
        const initialPositions = [
            { x: 0, y: 0 },
            { x: -100, y: 100 },
            { x: 200, y: 100 },
        ];
        await ruleEditorModel({
            initialRuleNodes: [
                node({ nodeId: "nodeA", position: initialPositions[0] }),
                node({ nodeId: "nodeB", position: initialPositions[1] }),
                node({
                    nodeId: "nodeC",
                    position: initialPositions[2],
                    portSpecification: { type: "count", minInputPorts: 2, maxInputPorts: 2 },
                }),
            ],
        });
        const checkBefore = () => {
            expect(currentOperatorNodes().map((n) => n.position)).toStrictEqual(initialPositions);
        };
        await waitFor(checkBefore);
        await act(async () => {
            await currentContext().executeModelEditOperation.addEdge("nodeB", "nodeC", "2");
            await currentContext().executeModelEditOperation.addEdge("nodeA", "nodeC", "1");
        });
        await act(async () => {
            await currentContext().executeModelEditOperation.autoLayout(false);
        });
        const checkAfter = () => {
            const newPositions = currentOperatorNodes().map((n) => n.position);
            expect(newPositions).not.toStrictEqual(initialPositions);
            expect(newPositions[0]?.y!!).toBeLessThan(newPositions[1]?.y!!);
        };
        await checkAfter();
        checkUndoAndRedo(checkBefore, checkAfter);
    });

    it("adds execution buttons for rule block nodes even without editable parameters", async () => {
        await ruleEditorModel({
            initialRuleNodes: [node({ nodeId: "ruleBlockNode", pluginId: "normalizeName", pluginType: "RuleBlock" })],
            operatorSpec: new Map(),
        });

        expect(currentReactFlowNodes()[0].data.executionButtons).toBeDefined();
    });

    it("does not add execution buttons for non-rule-block nodes without editable parameters", async () => {
        await ruleEditorModel({
            initialRuleNodes: [
                node({ nodeId: "transformNode", pluginId: "normalizeName", pluginType: "TransformOperator" }),
            ],
            operatorSpec: new Map(),
        });

        expect(currentReactFlowNodes()[0].data.executionButtons).toBeUndefined();
    });

    it("adds Markdown-based tooltips to named rule block input handles", async () => {
        await ruleEditorModel({
            initialRuleNodes: [
                node({
                    nodeId: "ruleBlockNode",
                    pluginId: "normalizeName",
                    pluginType: "RuleBlock",
                    portSpecification: {
                        type: "named",
                        inputPorts: [
                            {
                                id: "first",
                                label: "First input",
                                description: "- line one\n- line two",
                            },
                        ],
                    },
                    parameters: {},
                }),
            ],
            operatorList: [
                {
                    label: "normalizeName",
                    parameterSpecification: {},
                    pluginId: "normalizeName",
                    pluginType: "RuleBlock",
                    portSpecification: {
                        type: "named",
                        inputPorts: [
                            {
                                id: "first",
                                label: "First input",
                                description: "- line one\n- line two",
                            },
                        ],
                    },
                    tags: [],
                    inputsCanBeSwitched: false,
                },
            ],
            operatorSpec: new Map(),
        });

        const handle = nodeById("ruleBlockNode").data.handles?.[0];
        expect(handle?.data?.extendedTooltip).toBeTruthy();
        render(<>{handle?.data?.extendedTooltip}</>);
        expect(screen.getByText("First input")).toBeInTheDocument();
        expect(screen.getByText("line one")).toBeInTheDocument();
        expect(screen.getByText("line two")).toBeInTheDocument();
    });
});

/** Makes the rule model context available to the test. */
const RuleEditorModelTestComponent = () => {
    const context = React.useContext(RuleEditorModelContext);
    modelContext = context;

    return <div>Just a test</div>;
};
