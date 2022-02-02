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
                    return undefined as any;
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
        act(() => {
            currentContext().executeModelEditOperation.addNode(operator("pluginA"), { x: 5, y: 10 });
            currentContext().executeModelEditOperation.addNode(operator("pluginA"), { x: 5, y: 10 });
        });
        await waitFor(() => {
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
        });
    });
});

/** Makes the rule model context available to the test. */
const RuleEditorModelTestComponent = () => {
    const context = React.useContext(RuleEditorModelContext);
    modelContext = context;

    return <div>Just a test</div>;
};
