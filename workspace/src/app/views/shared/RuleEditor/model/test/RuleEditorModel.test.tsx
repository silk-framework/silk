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
import ruleEditorModelUtils from "../RuleEditorModel.utils";

let modelContext: RuleEditorModelContextProps | undefined;
const currentContext = () => modelContext as RuleEditorModelContextProps;

describe("Rule editor model", () => {
    let ruleOperatorNodes: IRuleOperatorNode[] = [];
    const ruleEditorModel = async (initialRuleNodes: IRuleOperatorNode[] = [], operatorList: IRuleOperator[] = []) => {
        modelContext = undefined;
        const ruleModel = withMount(
            testWrapper(
                <RuleEditorContext.Provider
                    value={{
                        editedItem: {},
                        operatorList: [],
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
        expect(
            ruleEditorModelUtils.asNode(currentContext().elements.find((n) => n.id === "pluginA_2"))!!.position
        ).toStrictEqual(position);
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
});

/** Makes the rule model context available to the test. */
const RuleEditorModelTestComponent = () => {
    const context = React.useContext(RuleEditorModelContext);
    modelContext = context;

    return <div>Just a test</div>;
};
