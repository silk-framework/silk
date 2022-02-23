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
} from "../../RuleEditor.typings";
import { XYPosition } from "react-flow-renderer/dist/types";
import utils from "../../RuleEditor.utils";
import { ruleEditorModelUtilsFactory } from "../RuleEditorModel.utils";
import { RuleEditorNode } from "../RuleEditorModel.typings";
import { rangeArray } from "../../../../../utils/basicUtils";

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
    let ruleOperatorNodes: IRuleOperatorNode[] = [];
    // Get a deep copy of the current operator nodes sorted by node ID
    const currentOperatorNodes = (): IRuleOperatorNode[] => {
        currentContext().saveRule();
        return JSON.parse(JSON.stringify(ruleOperatorNodes.sort((n1, n2) => (n1.nodeId < n2.nodeId ? -1 : 1))));
    };
    // Fetch the current react-flow nodes
    const currentReactFlowNodes = (): RuleEditorNode[] => {
        return modelUtils.elementNodes(currentContext().elements).sort((n1, n2) => (n1.id < n2.id ? -1 : 1));
    };

    const ruleEditorModel = async (
        initialRuleNodes: IRuleOperatorNode[] = [],
        operatorList: IRuleOperator[] = [],
        operatorSpec: Map<string, Map<string, IParameterSpecification>> = new Map()
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
                        saveRule: (nodes: IRuleOperatorNode[]): boolean => {
                            ruleOperatorNodes = nodes;
                            return true;
                        },
                        convertRuleOperatorToRuleNode: utils.defaults.convertRuleOperatorToRuleNode,
                        operatorSpec,
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
    }
    const nodeDefaultPosition = { x: 0, y: 0 };
    const node = ({
        nodeId,
        inputs = [],
        pluginId = "testPlugin",
        portSpecification = { minInputPorts: 1 },
        position = nodeDefaultPosition,
    }: NodeProps): IRuleOperatorNode => {
        return {
            inputs,
            label: nodeId,
            nodeId,
            parameters: {
                "param A": "Value A",
                "param B": "Value B",
            },
            pluginId,
            pluginType: "TestPlugin",
            portSpecification,
            position,
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

    const operator = (pluginId: string): IRuleOperator => {
        return {
            label: pluginId,
            parameterSpecification: {
                "param A": parameterSpecification("param A"),
                "param B": parameterSpecification("param B"),
            },
            pluginId: pluginId,
            pluginType: "TestPlugin",
            portSpecification: {
                minInputPorts: 1,
            },
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

    // Test UNDO and REDO behavior
    const checkUndoAndRedo = (beforeEditCheck: () => any, afterEditCheck: () => any) => {
        afterEditCheck();
        // Check UNDO and REDO twice in a row
        for (let i = 0; i < 2; i++) {
            // UNDO
            act(() => {
                currentContext().undo();
            });
            checkAfterUndo();
            beforeEditCheck();

            // REDO
            act(() => {
                currentContext().redo();
            });
            afterEditCheck();
        }
    };

    it("should load the internal model", async () => {
        await ruleEditorModel();
        expect(currentContext().canUndo).toBe(false);
        expect(currentContext().canRedo).toBe(false);
        expect(currentContext().elements).toHaveLength(0);
        currentContext().saveRule();
        expect(ruleOperatorNodes).toHaveLength(0);
        await ruleEditorModel(
            [node({ nodeId: "node A" }), node({ nodeId: "node B", inputs: ["node A"] })],
            [operator("pluginA")]
        );
        // 2 nodes and 1 edge
        await waitFor(async () => {
            expect(currentContext().elements).toHaveLength(3);
            await currentContext().saveRule();
            expect(ruleOperatorNodes).toHaveLength(2);
            expect(ruleOperatorNodes[1].inputs).toStrictEqual(["node A"]);
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
            currentContext().saveRule();
            expect(ruleOperatorNodes.map((node) => node.nodeId)).toStrictEqual([
                "pluginA",
                "node B",
                "pluginA_2",
                "pluginA_3",
            ]);
            expect(
                modelUtils.asNode(currentContext().elements.find((n) => n.id === "pluginA_2"))!!.position
            ).toStrictEqual(position);
            expect(ruleOperatorNodes[2].position).toStrictEqual(position);
        };
        checkAfterAddedNodes();

        // UNDO
        act(() => {
            currentContext().undo();
        });
        checkAfterUndo();
        checkBeforeAdd();

        // REDO
        act(() => {
            currentContext().redo();
        });
        checkAfterAddedNodes();
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
            currentContext().saveRule();
            expect(ruleOperatorNodes.map((node) => node.nodeId)).toStrictEqual(["nodeB", "nodeC"]);
        };
        checkAfterDelete();

        // UNDO
        act(() => {
            currentContext().undo();
        });
        checkAfterUndo();
        checkBeforeDelete();

        // REDO
        act(() => {
            currentContext().redo();
        });
        checkAfterDelete();
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
            currentContext().saveRule();
            expect(ruleOperatorNodes[0].position).toStrictEqual(newPosition);
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

        // UNDO
        act(() => {
            currentContext().undo();
        });
        checkAfterUndo();
        checkBeforeDelete();

        // REDO
        act(() => {
            currentContext().redo();
        });
        checkAfterDelete();
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

        // Copy and paste
        act(() => {
            currentContext().executeModelEditOperation.copyAndPasteNodes(["nodeB", "nodeC"], { x: 10, y: 10 });
        });
        const checkAfterCopyAndPaste = () => {
            checkAfterChange();
            // 2 nodes and 1 edge added
            expect(currentContext().elements).toHaveLength(9);
            currentContext().saveRule();
            expect(new Set(ruleOperatorNodes.map((op) => op.nodeId)).size).toBe(ruleOperatorNodes.length);
        };
        checkAfterCopyAndPaste();
        checkUndoAndRedo(checkBeforeCopyAndPaste, checkAfterCopyAndPaste);
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

        // UNDO
        act(() => {
            currentContext().undo();
        });
        checkAfterUndo();
        checkBeforeAdd();

        // REDO
        act(() => {
            currentContext().redo();
        });
        checkAfterAdd();
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
        const stateHistory: IRuleOperatorNode[][] = [];
        const stateHistoryLabel: string[] = [];
        const recordCurrentState = (stateLabel: string) => {
            stateHistory.push(currentOperatorNodes());
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
            expect(currentOperatorNodes()).not.toStrictEqual(stateHistory[stateHistory.length - 1]);
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
        // FIXME: Wait for elkjs release 0.7.3 to fix an issue with undefined variables
        // await recordedTransaction("Auto-layout",
        //     () => {
        //         currentContext().executeModelEditOperation.autoLayout();
        //     },
        //     async () => { // Auto-layout is async, so we need to wait for the change to take place.
        //         await waitFor(() => {
        //             expect(currentOperatorNodes()).not.toStrictEqual(stateHistory[stateHistory.length - 1])
        //         })
        //     }
        // );
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
        expect(currentOperatorNodes()).toHaveLength(3);
        // Execute UNDO and REDO twice
        for (let i = 0; i < 2; i++) {
            console.log("Test UNDO");
            for (let changeIdx = stateHistory.length - 1; changeIdx > 0; changeIdx--) {
                expect(currentContext().canUndo).toBe(true);
                act(() => {
                    currentContext().undo();
                });
                console.log(`Undone change: ${stateHistoryLabel[changeIdx]} (${changeIdx}/${stateHistory.length - 1})`);
                expect(currentOperatorNodes()).not.toStrictEqual(stateHistory[changeIdx]);
                expect(currentOperatorNodes()).toStrictEqual(stateHistory[changeIdx - 1]);
            }
            console.log("Test REDO");
            for (let changeIdx = 1; changeIdx < stateHistory.length; changeIdx++) {
                expect(currentContext().canRedo).toBe(true);
                act(() => {
                    currentContext().redo();
                });
                console.log(`Redone change: ${stateHistoryLabel[changeIdx]} (${changeIdx}/${stateHistory.length - 1})`);
                expect(currentOperatorNodes()).toStrictEqual(stateHistory[changeIdx]);
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
