import utils from "../LinkingRuleEditor.utils"
import {
    IPortSpecification,
    IRuleOperatorNode,
    RuleOperatorNodeParameters,
    RuleOperatorPluginType
} from "../../../shared/RuleEditor/RuleEditor.typings";
import {PluginType} from "@ducks/shared/typings";
import {IComparisonOperator} from "../linking.types";

describe("Linking rule editor utils", () => {
    it("should convert rule operator nodes with symmetric comparison to the corresponding linking rule tree", () => {
        [
            ["sourcePathInput", "targetPathInput", "false"],
            ["targetPathInput", "sourcePathInput", "true"],
            ["targetPathInput", "constant", "true"],
            ["sourcePathInput", "constant", "false"],
            ["constant", "targetPathInput", "false"],
            ["constant", "sourcePathInput", "true"],
            ["constant", "constant", "false"]
        ].forEach(([sourcePlugin, targetPlugin, reverseParameterValue]) => {
            const type = (pluginId: string) => pluginId.endsWith("PathInput") ? "PathInputOperator" : "TransformOperator"
            const nodes: IRuleOperatorNode[] = [
                node({nodeId: "inA", pluginId: sourcePlugin, pluginType: type(sourcePlugin)}),
                node({nodeId: "inB", pluginId: targetPlugin, pluginType: type(targetPlugin)}),
                node({
                    nodeId: "comparison", inputsCanBeSwitched: true,
                    inputs: ["inA", "inB"], pluginType: "ComparisonOperator"
                })
            ]
            const [expectedSourceInput, expectedTargetInput] = reverseParameterValue === "true" ? ["inB", "inA"] : ["inA", "inB"]
            console.log(sourcePlugin, targetPlugin, expectedSourceInput, reverseParameterValue)
            const tree = utils.constructLinkageRuleTree(nodes) as IComparisonOperator
            expect(tree.sourceInput.id).toBe(expectedSourceInput)
            expect(tree.targetInput.id).toBe(expectedTargetInput)
            expect(tree.parameters.reverse).toBe(reverseParameterValue)
        })
    })

    let count = 0

    interface NodeParams {
        nodeId?: string
        pluginType?: RuleOperatorPluginType | PluginType | "unknown"
        pluginId?: "sourcePathInput" | "targetPathInput" | string
        portSpecification?: IPortSpecification
        inputsCanBeSwitched?: boolean
        inputs?: (string | undefined)[];
        parameters?: RuleOperatorNodeParameters
    }

    const node = ({
                      nodeId,
                      pluginType = "PathInputOperator",
                      pluginId = "unknown",
                      portSpecification = {minInputPorts: 0},
                      inputsCanBeSwitched = false,
                      inputs = [],
                      parameters = {reverse: "false"}
                  }: NodeParams): IRuleOperatorNode => {
        const id = nodeId ?? `nodeID_${count++}`
        return {
            nodeId: id,
            parameters,
            label: `Label ${id}`,
            pluginType,
            pluginId,
            portSpecification,
            tags: [],
            inputsCanBeSwitched,
            inputs
        }
    }
})
