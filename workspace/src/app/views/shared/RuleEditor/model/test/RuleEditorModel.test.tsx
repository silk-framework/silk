import React from "react";
import { RuleEditorModel } from "../RuleEditorModel";
import { cleanUpDOM, testWrapper, withMount } from "../../../../../../../test/integration/TestHelper";
import { RuleEditorModelContext, RuleEditorModelContextProps } from "../../contexts/RuleEditorModelContext";
import { FitViewParams, FlowExportObject, FlowTransform, OnLoadParams, ReactFlowProvider } from "react-flow-renderer";
import { waitFor } from "@testing-library/react";
import { RuleEditorContext } from "../../contexts/RuleEditorContext";
import { IParameterSpecification, IRuleOperator, IRuleOperatorNode } from "../../RuleEditor.typings";
import { RuleEditorView } from "../../RuleEditorView";
import { XYPosition } from "react-flow-renderer/dist/types";

let modelContext: RuleEditorModelContextProps | undefined;

describe("Rule editor model", () => {
    let ruleOperatorNodes: IRuleOperatorNode[] = [];
    const ruleEditorModel = (initialRuleNodes: IRuleOperatorNode[] = [], operatorList: IRuleOperator[] = []) => {
        modelContext = undefined;
        return withMount(
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
                        convertRuleOperatorToRuleNode: (
                            ruleOperator: IRuleOperator
                        ): Omit<IRuleOperatorNode, "nodeId"> => {
                            return {} as any;
                        },
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

    const fetchModelContext = async (): Promise<RuleEditorModelContextProps> => {
        return await waitFor(() => {
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
            return modelContext as RuleEditorModelContextProps;
        });
    };

    it("should load the internal model", async () => {
        ruleEditorModel();
        const model = await fetchModelContext();
        expect(model.canUndo).toBe(false);
        expect(model.canRedo).toBe(false);
        expect(model.elements).toHaveLength(0);
        model.saveRule();
        expect(ruleOperatorNodes).toHaveLength(0);
        ruleEditorModel(
            [node({ nodeId: "node A" }), node({ nodeId: "node B", inputs: ["node A"] })],
            [operator("pluginA")]
        );
        // 2 nodes and 1 edge
        await waitFor(async () => {
            const nonEmptyModel = await fetchModelContext();
            expect(nonEmptyModel.elements).toHaveLength(3);
            await nonEmptyModel.saveRule();
            expect(ruleOperatorNodes).toHaveLength(2);
        });
    });
});

/** Makes the rule model context available to the test. */
const RuleEditorModelTestComponent = () => {
    const context = React.useContext(RuleEditorModelContext);
    modelContext = context;

    return <div>Just a test</div>;
};
