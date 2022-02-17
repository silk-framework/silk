import React from "react";
import { RuleEditorModel } from "../RuleEditorModel";
import { testWrapper, withMount } from "../../../../../../../test/integration/TestHelper";
import { RuleEditorModelContext, RuleEditorModelContextProps } from "../../contexts/RuleEditorModelContext";
import { Elements, FitViewParams, FlowExportObject, FlowTransform, ReactFlowProvider } from "react-flow-renderer";
import { act, waitFor } from "@testing-library/react";
import { RuleEditorContext } from "../../contexts/RuleEditorContext";
import { IParameterSpecification, IRuleOperator, IRuleOperatorNode } from "../../RuleEditor.typings";
import { XYPosition } from "react-flow-renderer/dist/types";
import utils from "../../RuleEditor.utils";
import { ruleEditorModelUtilsFactory } from "../RuleEditorModel.utils";

let modelContext: RuleEditorModelContextProps | undefined;
const currentContext = () => modelContext as RuleEditorModelContextProps;
const modelUtils = ruleEditorModelUtilsFactory();
const nodeById = (nodeId: string) => {
    const node = currentContext().elements.find((elem) => modelUtils.isNode(elem) && elem.id === nodeId);
    expect(node).toBeTruthy();
    return modelUtils.asNode(node)!!;
};

describe("Rule editor model", () => {
    let ruleOperatorNodes: IRuleOperatorNode[] = [];
    const currentOperatorNodes = (): IRuleOperatorNode[] => {
        currentContext().saveRule();
        return JSON.parse(JSON.stringify(ruleOperatorNodes.sort((n1, n2) => (n1.nodeId < n2.nodeId ? 1 : -1))));
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
        inputs?: string[];
        pluginId?: string;
        minInputPorts?: number;
    }
    const nodeDefaultPosition = { x: 0, y: 0 };
    const node = ({
        nodeId,
        inputs = [],
        pluginId = "testPlugin",
        minInputPorts = 1,
    }: NodeProps): IRuleOperatorNode => {
        return {
            inputs: inputs,
            label: nodeId,
            nodeId: nodeId,
            parameters: {
                "param A": "Value A",
                "param B": "Value B",
            },
            pluginId: pluginId,
            pluginType: "unknown",
            portSpecification: {
                minInputPorts: minInputPorts,
            },
            position: nodeDefaultPosition,
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

    it("should load the internal model", async () => {
        await ruleEditorModel();
        expect(currentContext().canUndo).toBe(false);
        expect(currentContext().canRedo).toBe(false);
        expect(currentContext().elements).toHaveLength(0);
        currentContext().saveRule();
        expect(ruleOperatorNodes).toHaveLength(0);
        await ruleEditorModel(
            [node({ nodeId: "node A", minInputPorts: 0 }), node({ nodeId: "node B", inputs: ["node A"] })],
            [operator("pluginA", 0)]
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

        // UNDO
        act(() => {
            currentContext().undo();
        });
        checkAfterUndo();
        checkBeforeCopyAndPaste();

        // REDO
        act(() => {
            currentContext().redo();
        });
        checkAfterCopyAndPaste();
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
            node({ nodeId: "nodeC", inputs: ["nodeA", "nodeB"] }),
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
            currentContext().executeModelEditOperation.addEdge("nodeA", "pluginA", "0");
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
});

/** Makes the rule model context available to the test. */
const RuleEditorModelTestComponent = () => {
    const context = React.useContext(RuleEditorModelContext);
    modelContext = context;

    return <div>Just a test</div>;
};
