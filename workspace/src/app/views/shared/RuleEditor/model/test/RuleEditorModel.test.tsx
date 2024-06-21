import React from "react";
import { RuleEditorModel } from "../RuleEditorModel";
import { testWrapper, withMount } from "../../../../../../../test/integration/TestHelper";
import { RuleEditorModelContext, RuleEditorModelContextProps } from "../../contexts/RuleEditorModelContext";
import { Elements, FitViewParams, FlowExportObject, FlowTransform, ReactFlowProvider } from "react-flow-renderer";
import { act, waitFor } from "@testing-library/react";
import { RuleEditorContext } from "../../contexts/RuleEditorContext";
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
import { RuleEditorNode } from "../RuleEditorModel.typings";
import { rangeArray } from "../../../../../utils/basicUtils";
import { IStickyNote } from "views/taskViews/shared/task.typings";
import { LINKING_NODE_TYPES } from "@eccenca/gui-elements/src/cmem/react-flow/configuration/typing";
import { nodeUtils } from "@eccenca/gui-elements";

let modelContext: RuleEditorModelContextProps | undefined;
const currentContext = () => modelContext as RuleEditorModelContextProps;
const execute = () => currentContext().executeModelEditOperation;
const modelUtils = ruleEditorModelUtilsFactory();
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
                    .sort((n1, n2) => (n1.nodeId < n2.nodeId ? -1 : 1))
            )
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
                minInputPorts: 0,
            },
            inputsCanBeSwitched: false,
        },
    ];

    const ruleEditorModel = async (
        initialRuleNodes: IRuleOperatorNode[] = [],
        operatorList: IRuleOperator[] = ruleOperatorList,
        operatorSpec: Map<string, Map<string, IParameterSpecification>> = new Map(),
        validateConnection: (
            fromRuleOperatorNode: RuleEditorValidationNode,
            toRuleOperatorNode: RuleEditorValidationNode,
            targetPortIdx: number
        ) => boolean = () => true,
        stickyNotes: IStickyNote[] = []
    ) => {
        modelContext = undefined;
        const ruleModel = withMount(
            testWrapper(
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
                        instanceId: "id",
                        datasetCharacteristics: new Map(),
                        partialAutoCompletion: () => async () => undefined,
                    }}
                >
                    <ReactFlowProvider>
                        <RuleEditorModel>
                            <RuleEditorModelTestComponent />
                        </RuleEditorModel>
                    </ReactFlowProvider>
                </RuleEditorContext.Provider>
            )
        );
        await waitFor(() => {
            expect(modelContext).toBeTruthy();
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
        portSpecification = { minInputPorts: 1 },
        position = nodeDefaultPosition,
        parameters = defaultParameters,
    }: NodeProps): IRuleOperatorNode => {
        return {
            inputs,
            label: nodeId,
            nodeId,
            parameters,
            pluginId,
            pluginType: "unknown",
            portSpecification,
            position,
            inputsCanBeSwitched: false,
        };
    };

    const parameterSpecification = (paramId: string): IParameterSpecification => {
        return {
            advanced: false,
            defaultValue: "",
            label: paramId,
            required: false,
            type: "textField",
        };
    };

    const operator = (pluginId: string, minInputPorts: number = 1): IRuleOperator => {
        return {
            label: pluginId,
            parameterSpecification: {
                "param A": parameterSpecification("param A"),
                "param B": parameterSpecification("param B"),
            },
            pluginId: pluginId,
            pluginType: "unknown",
            portSpecification: {
                minInputPorts: minInputPorts,
            },
            tags: [],
            inputsCanBeSwitched: false,
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
        const noteNode = {
            id: modelUtils.freshNodeId("sticky"),
            content: stickyNote,
            position: [50, 120],
            dimension: [DEFAULT_NODE_WIDTH, DEFAULT_NODE_HEIGHT],
            color: "#000000",
        };
        await ruleEditorModel(undefined, undefined, undefined, undefined, [noteNode]);
    };

    const allStickyNodes = () =>
        currentContext().elements.filter(
            (elem) => elem.type === LINKING_NODE_TYPES.stickynote && modelUtils.isNode(elem)
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

    it("should load the internal model", async () => {
        await ruleEditorModel();
        expect(currentContext().canUndo).toBe(false);
        expect(currentContext().canRedo).toBe(false);
        expect(currentContext().elements).toHaveLength(0);
        expect(currentContext().ruleOperatorNodes()).toHaveLength(0);
        await ruleEditorModel(
            [
                node({ nodeId: "node A", portSpecification: { minInputPorts: 0 } }),
                node({ nodeId: "node B", inputs: ["node A"] }),
            ],
            [operator("pluginA", 0)]
        );
        // 2 nodes and 1 edge
        await waitFor(async () => {
            expect(currentContext().elements).toHaveLength(3);
            expect(currentContext().ruleOperatorNodes()).toHaveLength(2);
            expect(currentContext().ruleOperatorNodes()[1].inputs).toStrictEqual(["node A"]);
        });
    });

    it("should add new nodes and undo & redo", async () => {
        await ruleEditorModel(
            [node({ nodeId: "pluginA" }), node({ nodeId: "node B", inputs: ["pluginA"] })],
            [operator("pluginA")]
        );
        const checkBeforeAdd = () => {
            expect(currentContext().elements).toHaveLength(3);
            expect(
                currentContext()
                    .elements.map((e) => e.id)
                    .sort((left, right) => (left < right ? 1 : -1))
            ).toStrictEqual(["pluginA", "node B", "1"]);
        };
        checkBeforeAdd();

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
                    .map((node) => node.nodeId)
            ).toStrictEqual(["pluginA", "node B", "pluginA_2", "pluginA_3"]);
            expect(
                modelUtils.asNode(currentContext().elements.find((n) => n.id === "pluginA_2"))!!.position
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
        const node = allStickyNodes()[0];
        const checkBeforeChange = () => {
            expect(node.data.style).toEqual(defaultStyle);
        };
        checkBeforeChange();

        const checkAfterChange = () => {
            expect(modelUtils.nodeById(currentContext().elements, node.id)!!.data.style).not.toStrictEqual(
                defaultStyle
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
        const node = allStickyNodes()[0];
        const checkBeforeChange = () => {
            expect(node.data.businessData.stickyNote).toStrictEqual(stickyNote);
        };
        checkBeforeChange();
        const newContent = "**new Content**";
        const checkAfterChange = () => {
            expect(modelUtils.nodeById(currentContext().elements, node.id)!!.data.businessData.stickyNote).toEqual(
                newContent
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
        checkBeforeChange();
        const randomNewNodeDimensions = { width: DEFAULT_NODE_WIDTH + 30, height: DEFAULT_NODE_HEIGHT + 10 };
        const checkAfterChange = () => {
            expect(modelUtils.nodeById(currentContext().elements, node.id)!!.data.nodeDimensions).toEqual(
                randomNewNodeDimensions
            );
        };
        act(() => {
            currentContext().executeModelEditOperation.changeSize(node.id, randomNewNodeDimensions);
        });
        checkAfterChange();

        checkUndoAndRedo(checkBeforeChange, checkAfterChange);
    });

    it("should delete nodes and undo & redo", async () => {
        await ruleEditorModel(
            [
                node({ nodeId: "nodeA" }),
                node({ nodeId: "nodeB", inputs: ["nodeA"] }),
                node({ nodeId: "nodeC", inputs: ["nodeA", "nodeB"] }),
            ],
            [operator("pluginA")]
        );
        const checkBeforeDelete = () => {
            // 3 nodes, 3 edges
            expect(currentContext().elements).toHaveLength(6);
        };
        checkBeforeDelete();

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
                    .map((node) => node.nodeId)
            ).toStrictEqual(["nodeB", "nodeC"]);
        };
        checkAfterDelete();

        checkUndoAndRedo(checkBeforeDelete, checkAfterDelete);
    });

    it("should move a node and undo & redo", async () => {
        await ruleEditorModel([], [operator("pluginA")]);
        const startPosition = { x: 2, y: 4 };
        act(() => {
            currentContext().executeModelEditOperation.addNode(operator("pluginA"), startPosition);
        });
        const nodeId = currentContext().elements[0].id;
        const checkBeforeMove = () => {
            expect(modelUtils.nodeById(currentContext().elements, nodeId)!!.position).toStrictEqual(startPosition);
        };
        checkBeforeMove();

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

    it("should auto-layout nodes and undo & redo", async () => {
        const initialPositions = [
            { x: 0, y: 0 },
            { x: -100, y: 100 },
            { x: 200, y: 100 },
        ];
        await ruleEditorModel([
            node({ nodeId: "nodeA", position: initialPositions[0] }),
            node({ nodeId: "nodeB", position: initialPositions[1] }),
            node({
                nodeId: "nodeC",
                position: initialPositions[2],
                portSpecification: { minInputPorts: 2, maxInputPorts: 2 },
            }),
        ]);
        const checkBefore = () => {
            expect(currentOperatorNodes().map((n) => n.position)).toStrictEqual(initialPositions);
        };
        checkBefore();
        act(() => {
            currentContext().executeModelEditOperation.addEdge("nodeB", "nodeC", "2");
            currentContext().executeModelEditOperation.addEdge("nodeA", "nodeC", "1");
        });
        act(() => {
            currentContext().executeModelEditOperation.autoLayout(false);
        });
        const checkAfter = async () => {
            await waitFor(() => {
                const newPositions = currentOperatorNodes().map((n) => n.position);
                expect(newPositions).not.toStrictEqual(initialPositions);
                expect(newPositions[0]?.y!!).toBeLessThan(newPositions[1]?.y!!);
            });
        };
        await checkAfter();
        checkUndoAndRedo(checkBefore, checkAfter);
    });

    it("should move multiple nodes by an offset", async () => {
        await ruleEditorModel(
            [
                node({ nodeId: "nodeA", position: { x: 1, y: 2 } }),
                node({ nodeId: "nodeB", inputs: ["nodeA"], position: { x: 2, y: 3 } }),
                node({ nodeId: "nodeC", position: { x: 3, y: 4 } }),
            ],
            [operator("pluginA")]
        );
        const checkPositions = (data: [string, number, number][]) => {
            expect(currentOperatorNodes().map((n) => ({ pos: n.position, id: n.nodeId }))).toStrictEqual(
                data.map((d) => ({ id: `node${d[0]}`, pos: { x: d[1], y: d[2] } }))
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
        await ruleEditorModel([node({ nodeId: "nodeA" })], [operator("pluginA")]);
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

    it("should save rule parameters correctly", async () => {
        await ruleEditorModel(
            [
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
            [operator("pluginA")]
        );
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

    it("should delete multiple nodes and undo & redo", async () => {
        await ruleEditorModel(
            [
                node({ nodeId: "nodeA" }),
                node({ nodeId: "nodeB", inputs: ["nodeA"] }),
                node({ nodeId: "nodeC", inputs: ["nodeA", "nodeB"] }),
            ],
            [operator("pluginA")]
        );
        const checkBeforeDelete = () => {
            expect(currentContext().elements).toHaveLength(6);
        };
        checkBeforeDelete();
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
        await ruleEditorModel([
            node({ nodeId: "nodeA" }),
            node({ nodeId: "nodeB", inputs: ["nodeA"] }),
            node({ nodeId: "nodeC", inputs: ["nodeA", "nodeB"] }),
        ]);
        const checkBeforeCopyAndPaste = () => {
            expect(currentContext().elements).toHaveLength(6);
        };
        checkBeforeCopyAndPaste();

        // Copy and paste first time
        act(() => {
            currentContext().executeModelEditOperation.copyAndPasteNodes(["nodeB", "nodeC"], { x: 10, y: 10 });
        });
        const checkAfterCopyAndPaste = () => {
            // 2 nodes and 1 edge added
            expect(currentContext().elements).toHaveLength(9);
            expect(new Set(currentOperatorNodes().map((op) => op.nodeId)).size).toBe(
                currentContext().ruleOperatorNodes().length
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
                currentContext().ruleOperatorNodes().length
            );
        };
        checkAfterCopyAndPaste2nd();
        checkUndoAndRedo(checkBeforeCopyAndPaste, checkAfterCopyAndPaste, checkAfterCopyAndPaste2nd);
    });

    it("should add an edge and undo & redo", async () => {
        await ruleEditorModel([node({ nodeId: "nodeA" }), node({ nodeId: "nodeB" })]);
        const checkBeforeAdd = () => {
            expect(currentContext().elements).toHaveLength(2);
        };
        checkBeforeAdd();

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
        await ruleEditorModel([
            node({ nodeId: "nodeA" }),
            node({ nodeId: "nodeB", inputs: ["nodeA"] }),
            node({ nodeId: "nodeC", inputs: ["nodeA", "nodeB"] }),
        ]);
        const edge = currentContext().elements.find(
            (elem) => modelUtils.isEdge(elem) && modelUtils.asEdge(elem)!!.target === "nodeB"
        );
        const before = currentContext().elements.length;
        const checkBeforeDelete = () => {
            expect(currentContext().elements).toHaveLength(before);
        };
        checkBeforeDelete();

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
        const stateHistory: (IRuleOperatorNode | IStickyNote)[][] = [];
        const stateHistoryLabel: string[] = [];
        await stickyNoteNodeBootstrap();
        const currentStickyNodes = () =>
            currentContext().elements.reduce((stickyNodes, elem) => {
                if (modelUtils.isNode(elem) && elem.type === LINKING_NODE_TYPES.stickynote) {
                    const node = modelUtils.asNode(elem)!;
                    stickyNodes.push(nodeUtils.transformNodeToStickyNode(node) as IStickyNote);
                }
                return stickyNodes;
            }, [] as IStickyNote[]);
        const allNodes = () => [...currentOperatorNodes(), ...currentStickyNodes()];
        const recordCurrentState = (stateLabel: string) => {
            stateHistory.push(allNodes());
            stateHistoryLabel.push(stateLabel);
        };
        const recordedTransaction = async (
            stateLabel: string,
            changeAction: () => any,
            additionalCheck: () => any | Promise<any> = () => {}
        ) => {
            act(() => {
                currentContext().executeModelEditOperation.startChangeTransaction();
                changeAction();
            });
            // Check that something has changed
            expect(allNodes()).not.toStrictEqual(stateHistory[stateHistory.length - 1]);
            await additionalCheck();
            recordCurrentState(stateLabel);
        };
        await ruleEditorModel([
            node({ nodeId: "nodeA" }),
            node({ nodeId: "nodeB", inputs: ["nodeA"] }),
            node({ nodeId: "nodeC", inputs: ["nodeB"] }),
        ]);
        recordCurrentState("Initial state");
        // Record every change and check that after undo and later redo the states match
        await recordedTransaction("Add a node", () => {
            currentContext().executeModelEditOperation.addNode(operator("pluginA"), { x: 1, y: 2 });
        });
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
            }
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
                "another sticky note"
            );
        });

        expect(allNodes()).toHaveLength(4);
        // // Execute UNDO and REDO twice
        for (let i = 0; i < 2; i++) {
            console.log("Test UNDO");
            for (let changeIdx = stateHistory.length - 1; changeIdx > 0; changeIdx--) {
                expect(currentContext().canUndo).toBe(true);
                act(() => {
                    currentContext().undo();
                });
                expect(allNodes()).not.toStrictEqual(stateHistory[changeIdx]);
                expect(allNodes()).toStrictEqual(stateHistory[changeIdx - 1]);
            }
            console.log("Test REDO");
            for (let changeIdx = 1; changeIdx < stateHistory.length; changeIdx++) {
                expect(currentContext().canRedo).toBe(true);
                act(() => {
                    currentContext().redo();
                });
                console.log(`Redone change: ${stateHistoryLabel[changeIdx]} (${changeIdx}/${stateHistory.length - 1})`);
                expect(allNodes()).toStrictEqual(stateHistory[changeIdx]);
            }
        }
    });

    const nodeHasInputs = (nodeId: string, inputs: (string | null)[]) => {
        expect(currentOperatorNodes().find((op) => op.nodeId === nodeId)?.inputs).toStrictEqual(inputs);
    };

    it("should remove an existing edge when a new edge is connected to the same port", async () => {
        await ruleEditorModel([
            node({ nodeId: "nodeA" }),
            node({ nodeId: "nodeB" }),
            node({ nodeId: "nodeC", inputs: ["nodeA"] }),
            node({ nodeId: "nodeD", inputs: ["nodeB"] }),
        ]);
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
        await ruleEditorModel([
            node({ nodeId: "nodeA" }),
            node({ nodeId: "nodeB" }),
            node({ nodeId: "nodeC", inputs: ["nodeA", "nodeB"] }),
        ]);
        const checkBeforeEdit = () => {
            nodeHasInputs("nodeC", ["nodeA", "nodeB"]);
            expect(currentContext().elements).toHaveLength(5);
        };
        checkBeforeEdit();
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
        await ruleEditorModel([
            node({ nodeId: "nodeA" }),
            node({ nodeId: "nodeB" }),
            node({ nodeId: "nodeC" }),
            node({ nodeId: "nodeD" }),
            node({
                nodeId: "nodeE",
                inputs: [undefined, "nodeA"],
                portSpecification: {
                    minInputPorts: 3,
                    maxInputPorts: 3,
                },
            }),
        ]);
        const checkBeforeEdit = () => {
            nodeHasInputs("nodeE", [null, "nodeA", null]);
            expect(currentContext().elements).toHaveLength(6);
        };
        checkBeforeEdit();
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
        await ruleEditorModel([
            // Each node can only have one output edge, so we need a lot of dummy nodes
            ...dummyNodes,
            node({ nodeId: "nodeA" }),
            node({ nodeId: "nodeA2", portSpecification: { minInputPorts: 0, maxInputPorts: 0 } }),
            node({ nodeId: "nodeB" }),
            node({ nodeId: "nodeC", inputs: ["inputNode1", "inputNode2"] }),
            node({
                nodeId: "nodeD",
                inputs: ["inputNode3", "inputNode4"],
                portSpecification: { minInputPorts: 2, maxInputPorts: 3 },
            }),
            node({ nodeId: "nodeE", inputs: ["inputNode5", "inputNode6"] }),
            node({ nodeId: "nodeF", inputs: ["inputNode7", "inputNode8"] }),
        ]);
        const checkNrOfInputs = (inputs: number[]) => {
            expect(currentReactFlowNodes().map((n) => modelUtils.inputHandles(n).length)).toStrictEqual(inputs);
        };
        const checkBeforeChange = () => {
            checkNrOfInputs([...inputHandlesForDummyNodes, 1, 0, 1, 3, 3, 3, 3]);
        };
        checkBeforeChange();
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
                    .map((e) => e.id)
            );
            // This should reduce the number of inputs, since the last connection was removed.
            execute().deleteEdges(
                modelUtils
                    .findEdges({ elements: currentContext().elements, source: "inputNode8", target: "nodeF" })
                    .map((e) => e.id)
            );
        });
        const checkAfterChange = () => {
            checkNrOfInputs([...inputHandlesForDummyNodes, 1, 0, 3, 4, 3, 3, 2]);
        };
        checkAfterChange();
        checkUndoAndRedo(checkBeforeChange, checkAfterChange);
    });
});

/** Makes the rule model context available to the test. */
const RuleEditorModelTestComponent = () => {
    const context = React.useContext(RuleEditorModelContext);
    modelContext = context;

    return <div>Just a test</div>;
};
