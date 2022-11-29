import utils from "../LinkingRuleEditor.utils";
import {
    IPortSpecification,
    IRuleOperatorNode,
    RuleOperatorNodeParameters,
    RuleOperatorPluginType,
} from "../../../shared/RuleEditor/RuleEditor.typings";
import { PluginType } from "@ducks/shared/typings";
import { IComparisonOperator } from "../linking.types";
import { ITransformOperator } from "../../shared/rules/rule.typings";

describe("Linking rule editor utils", () => {
    it("should convert rule operator nodes with symmetric comparison to the corresponding linking rule tree", () => {
        // All direct inputs first
        const combinations = [
            ["sourcePathInput", "targetPathInput", "false"],
            ["targetPathInput", "sourcePathInput", "true"],
            ["targetPathInput", "constant", "true"],
            ["sourcePathInput", "constant", "false"],
            ["constant", "targetPathInput", "false"],
            ["constant", "sourcePathInput", "true"],
            ["constant", "constant", "false"],
        ];
        const type = (pluginId: string) => (pluginId.endsWith("PathInput") ? "PathInputOperator" : "TransformOperator");
        combinations.forEach(([sourcePlugin, targetPlugin, reverseParameterValue]) => {
            const nodes: IRuleOperatorNode[] = [
                node({ nodeId: "inA", pluginId: sourcePlugin, pluginType: type(sourcePlugin) }),
                node({ nodeId: "inB", pluginId: targetPlugin, pluginType: type(targetPlugin) }),
                node({
                    nodeId: "comparison",
                    inputsCanBeSwitched: true,
                    inputs: ["inA", "inB"],
                    pluginType: "ComparisonOperator",
                }),
            ];
            const [expectedSourceInput, expectedTargetInput] =
                reverseParameterValue === "true" ? ["inB", "inA"] : ["inA", "inB"];
            const tree = utils.constructLinkageRuleTree(nodes) as IComparisonOperator;
            expect(tree.sourceInput.id).toBe(expectedSourceInput);
            expect(tree.targetInput.id).toBe(expectedTargetInput);
            expect(tree.parameters.reverse).toBe(reverseParameterValue);
        });
        // Put transformations between the input operators
        combinations.forEach(([sourcePlugin, targetPlugin, reverseParameterValue]) => {
            const nodes: IRuleOperatorNode[] = [
                node({ nodeId: "inA", pluginId: sourcePlugin, pluginType: type(sourcePlugin) }),
                node({ nodeId: "transformA", inputs: ["inA"], pluginType: "TransformOperator" }),
                node({ nodeId: "inB", pluginId: targetPlugin, pluginType: type(targetPlugin) }),
                node({ nodeId: "transformB", inputs: ["inB"], pluginType: "TransformOperator" }),
                node({
                    nodeId: "comparison",
                    inputsCanBeSwitched: true,
                    inputs: ["transformA", "transformB"],
                    pluginType: "ComparisonOperator",
                }),
            ];
            const [expectedSourceInput, expectedTargetInput] =
                reverseParameterValue === "true" ? ["inB", "inA"] : ["inA", "inB"];
            const tree = utils.constructLinkageRuleTree(nodes) as IComparisonOperator;
            expect((tree.sourceInput as ITransformOperator).inputs[0].id).toBe(expectedSourceInput);
            expect((tree.targetInput as ITransformOperator).inputs[0].id).toBe(expectedTargetInput);
            expect(tree.parameters.reverse).toBe(reverseParameterValue);
        });
    });

    let count = 0;

    interface NodeParams {
        nodeId?: string;
        pluginType?: RuleOperatorPluginType | PluginType | "unknown";
        pluginId?: "sourcePathInput" | "targetPathInput" | string;
        portSpecification?: IPortSpecification;
        inputsCanBeSwitched?: boolean;
        inputs?: (string | undefined)[];
        parameters?: RuleOperatorNodeParameters;
    }

    const node = ({
        nodeId,
        pluginType = "PathInputOperator",
        pluginId = "unknown",
        portSpecification = { minInputPorts: 0 },
        inputsCanBeSwitched = false,
        inputs = [],
        parameters = { reverse: "false" },
    }: NodeParams): IRuleOperatorNode => {
        const id = nodeId ?? `nodeID_${count++}`;
        return {
            nodeId: id,
            parameters,
            label: `Label ${id}`,
            pluginType,
            pluginId,
            portSpecification,
            tags: [],
            inputsCanBeSwitched,
            inputs,
        };
    };
});
