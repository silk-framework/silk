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
import modelUtils from "../RuleEditorModel.utils";

let modelContext: RuleEditorModelContextProps | undefined;
const currentContext = () => modelContext as RuleEditorModelContextProps;
const nodeById = (nodeId: string) => {
    const node = currentContext().elements.find((elem) => modelUtils.isNode(elem) && elem.id === nodeId);
    expect(node).toBeTruthy();
    return modelUtils.asNode(node)!!;
};

describe("Rule editor model", () => {
    let ruleOperatorNodes: IRuleOperatorNode[] = [];
    const ruleEditorModel = async (initialRuleNodes: IRuleOperatorNode[] = [], operatorList: IRuleOperator[] = []) => {
        modelContext = undefined;
        const ruleModel = withMount(
            testWrapper(
                <RuleEditorContext.Provider
                    value={{
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
    }
    const nodeDefaultPosition = { x: 0, y: 0 };
    const node = ({ nodeId, inputs = [], pluginId = "testPlugin" }: NodeProps): IRuleOperatorNode => {
        return {
            inputs: inputs,
            label: nodeId,
            nodeId: nodeId,
            parameters: {
                "param A": "Value A",
                "param B": "Value B",
            },
            pluginId: pluginId,
            pluginType: "TestPlugin",
            portSpecification: {
                minInputPorts: 1,
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
            type: "string",
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
        });
    });

    it("should add new nodes", async () => {
        await ruleEditorModel(
            [node({ nodeId: "pluginA" }), node({ nodeId: "node B", inputs: ["pluginA"] })],
            [operator("pluginA")]
        );
        await waitFor(async () => {
            expect(currentContext().elements).toHaveLength(3);
        });
        const position = { x: 5, y: 10 };
        act(() => {
            currentContext().executeModelEditOperation.addNode(operator("pluginA"), position);
            currentContext().executeModelEditOperation.addNode(operator("pluginA"), position);
        });
        // 4 nodes, 1 edge
        expect(currentContext().elements).toHaveLength(5);
        expect(currentContext().canUndo).toBe(true);
        expect(currentContext().canRedo).toBe(false);
        currentContext().saveRule();
        expect(ruleOperatorNodes.map((node) => node.nodeId)).toStrictEqual([
            "pluginA",
            "node B",
            "pluginA_2",
            "pluginA_3",
        ]);
        expect(modelUtils.asNode(currentContext().elements.find((n) => n.id === "pluginA_2"))!!.position).toStrictEqual(
            position
        );
        expect(ruleOperatorNodes[2].position).toStrictEqual(position);
    });

    it("should delete nodes", async () => {
        await ruleEditorModel(
            [
                node({ nodeId: "nodeA" }),
                node({ nodeId: "nodeB", inputs: ["nodeA"] }),
                node({ nodeId: "nodeC", inputs: ["nodeA", "nodeB"] }),
            ],
            [operator("pluginA")]
        );
        await waitFor(async () => {
            // 3 nodes, 3 edges
            expect(currentContext().elements).toHaveLength(6);
        });
        act(() => {
            currentContext().executeModelEditOperation.deleteNode("nodeA");
        });
        // 1 node and 2 edges removed
        expect(currentContext().elements).toHaveLength(3);
        currentContext().saveRule();
        expect(ruleOperatorNodes.map((node) => node.nodeId)).toStrictEqual(["nodeB", "nodeC"]);
    });

    it("should move a node", async () => {
        await ruleEditorModel([], [operator("pluginA")]);
        const startPosition = { x: 2, y: 4 };
        act(() => {
            currentContext().executeModelEditOperation.addNode(operator("pluginA"), startPosition);
        });
        const nodeId = currentContext().elements[0].id;
        const newPosition = { x: 100, y: 102 };
        act(() => {
            currentContext().executeModelEditOperation.moveNode(nodeId, newPosition);
        });
        expect(nodeById(nodeId).position).toStrictEqual(newPosition);
        currentContext().saveRule();
        expect(ruleOperatorNodes[0].position).toStrictEqual(newPosition);
    });

    it("should change node parameters", async () => {
        await ruleEditorModel([node({ nodeId: "nodeA" })], [operator("pluginA")]);
        currentContext().saveRule();
        expect(ruleOperatorNodes[0].parameters).toStrictEqual({
            "param A": "Value A",
            "param B": "Value B",
        });
        act(() => {
            currentContext().executeModelEditOperation.changeNodeParameter("nodeA", "param A", "new Value A");
        });
        currentContext().saveRule();
        expect(ruleOperatorNodes[0].parameters).toStrictEqual({
            "param A": "new Value A",
            "param B": "Value B",
        });
    });

    it("should delete multiple nodes", async () => {
        await ruleEditorModel(
            [
                node({ nodeId: "nodeA" }),
                node({ nodeId: "nodeB", inputs: ["nodeA"] }),
                node({ nodeId: "nodeC", inputs: ["nodeA", "nodeB"] }),
            ],
            [operator("pluginA")]
        );
        act(() => {
            currentContext().executeModelEditOperation.deleteNodes(["nodeA", "nodeC"]);
        });
        expect(currentContext().elements).toHaveLength(1);
    });

    it("should copy and paste multiple nodes", async () => {
        await ruleEditorModel([
            node({ nodeId: "nodeA" }),
            node({ nodeId: "nodeB", inputs: ["nodeA"] }),
            node({ nodeId: "nodeC", inputs: ["nodeA", "nodeB"] }),
        ]);
        act(() => {
            currentContext().executeModelEditOperation.copyAndPasteNodes(["nodeB", "nodeC"], { x: 10, y: 10 });
        });
        // 2 nodes and 1 edge added
        expect(currentContext().elements).toHaveLength(9);
        currentContext().saveRule();
        expect(new Set(ruleOperatorNodes.map((op) => op.nodeId)).size).toBe(ruleOperatorNodes.length);
    });

    it("should add an edge", async () => {
        await ruleEditorModel([node({ nodeId: "nodeA" }), node({ nodeId: "nodeB" })]);
        act(() => {
            currentContext().executeModelEditOperation.addEdge("nodeA", "nodeB", "0");
        });
        expect(currentContext().elements).toHaveLength(3);
    });

    it("should delete an edge", async () => {
        await ruleEditorModel([
            node({ nodeId: "nodeA" }),
            node({ nodeId: "nodeB", inputs: ["nodeA"] }),
            node({ nodeId: "nodeC", inputs: ["nodeA", "nodeB"] }),
        ]);
        const edge = currentContext().elements.find(
            (elem) => modelUtils.isEdge(elem) && modelUtils.asEdge(elem)!!.target === "nodeB"
        );
        const before = currentContext().elements.length;
        act(() => {
            currentContext().executeModelEditOperation.deleteEdge(edge!!.id);
        });
        expect(currentContext().elements).toHaveLength(before - 1);
    });
});

/** Makes the rule model context available to the test. */
const RuleEditorModelTestComponent = () => {
    const context = React.useContext(RuleEditorModelContext);
    modelContext = context;

    return <div>Just a test</div>;
};
