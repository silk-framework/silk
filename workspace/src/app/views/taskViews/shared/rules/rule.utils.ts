import { IPathInput, ITransformOperator, IValueInput } from "./rule.typings";
import {
    IParameterSpecification,
    IPortSpecification,
    IRuleOperator,
    IRuleOperatorNode,
} from "../../../shared/RuleEditor/RuleEditor.typings";
import { RuleOperatorFetchFnType } from "../../../shared/RuleEditor/RuleEditor";
import { IPluginDetails } from "@ducks/common/typings";

/** Extracts the operator node from a path input. */
const extractOperatorNodeFromPathInput = (
    pathInput: IPathInput,
    result: IRuleOperatorNode[],
    isTarget: boolean | undefined
): string => {
    result.push({
        nodeId: pathInput.id,
        label: isTarget ? "Target path" : "Source path",
        pluginType: "PathInputOperator",
        pluginId: isTarget ? "targetPathInput" : "sourcePathInput", // We use the plugin ID to denote if this is a source or target path input.
        inputs: [],
        parameters: {
            path: pathInput.path,
        },
        portSpecification: {
            minInputPorts: 0,
            maxInputPorts: 0,
        },
    });
    return pathInput.id;
};

/** Extract the operator node from a transform input. */
const extractOperatorNodeFromTransformInput = (
    transformInput: ITransformOperator,
    result: IRuleOperatorNode[],
    isTarget: boolean | undefined,
    ruleOperator: RuleOperatorFetchFnType
): string => {
    const inputs = transformInput.inputs.map((input) =>
        extractOperatorNodeFromValueInput(input, result, isTarget, ruleOperator)
    );
    result.push({
        nodeId: transformInput.id,
        inputs: inputs,
        pluginType: "TransformOperator",
        pluginId: transformInput.function,
        label: ruleOperator(transformInput.function, "TransformOperator")?.label ?? transformInput.function,
        parameters: transformInput.parameters,
        portSpecification: {
            minInputPorts: 1,
        },
        tags: ["Transform"],
    });
    return transformInput.id;
};

/** Extract operator nodes from an value input node, i.e. path input or transform operator.
 *
 * @param operator The value input operator.
 * @param result   The result array this operator should be added to.
 * @param isTarget Only important in the context of comparisons where we have to distinguish between source and target paths.
 */
export const extractOperatorNodeFromValueInput = (
    operator: IValueInput | undefined,
    result: IRuleOperatorNode[],
    isTarget: boolean | undefined,
    ruleOperator: (pluginId: string, pluginType?: string) => IRuleOperator | undefined
): string | undefined => {
    if (operator) {
        const nodeId =
            operator.type === "pathInput"
                ? extractOperatorNodeFromPathInput(operator as IPathInput, result, isTarget)
                : extractOperatorNodeFromTransformInput(operator as ITransformOperator, result, isTarget, ruleOperator);
        return nodeId;
    }
};

/** Input path operator used in the transform and linking operators. */
export const inputPathOperator = (pluginId: string, label: string, description: string): IRuleOperator => {
    return {
        pluginType: "PathInputOperator",
        pluginId: pluginId,
        portSpecification: {
            minInputPorts: 0,
            maxInputPorts: 0,
        },
        label: label,
        parameterSpecification: {
            path: {
                label: "Path",
                defaultValue: "",
                type: "pathInput",
                description: "The source input path as Silk path expression.",
                advanced: false,
                required: true,
            },
        },
        categories: ["Input"],
        icon: "", // TODO: CMEM-3919: Icon for path input
        description: description,
    };
};

/** Converts plugin details from the backend to rule operators. */
const convertRuleOperator = (op: IPluginDetails): IRuleOperator => {
    const required = (parameterId: string) => op.required.includes(parameterId);
    return {
        pluginType: op.pluginType ?? "unknown",
        pluginId: op.pluginId,
        label: op.title,
        description: op.description,
        categories: op.categories,
        icon: "artefact-task", // FIXME: Which icons? CMEM-3919
        parameterSpecification: Object.fromEntries(
            Object.entries(op.properties).map(([parameterId, parameterSpec]) => {
                const spec: IParameterSpecification = {
                    label: parameterSpec.title,
                    description: parameterSpec.description,
                    advanced: parameterSpec.advanced,
                    required: required(parameterId),
                    type: "textField", // FIXME: Convert types from parameterSpec.parameterType, see InputMapper component CMEM-3919
                    defaultValue: parameterSpec.value,
                };
                return [parameterId, spec];
            })
        ),
        portSpecification: portSpecification(op),
    };
};

const portSpecification = (op: IPluginDetails): IPortSpecification => {
    switch (op.pluginType) {
        case "ComparisonOperator":
            return { minInputPorts: 2, maxInputPorts: 2 };
        default:
            return { minInputPorts: 1 };
    }
};

/** Converts a rule operator node to a value input. */
const convertRuleOperatorNodeToValueInput = (
    ruleOperatorNode: IRuleOperatorNode,
    ruleOperatorNodes: Map<string, IRuleOperatorNode>
): IValueInput => {
    if (ruleOperatorNode.pluginType === "TransformOperator") {
        const transformOperator: ITransformOperator = {
            id: ruleOperatorNode.nodeId,
            function: ruleOperatorNode.pluginId,
            inputs: ruleOperatorNode.inputs
                .filter((i) => i != null)
                .map((i) =>
                    convertRuleOperatorNodeToValueInput(
                        fetchRuleOperatorNode(i!!, ruleOperatorNodes, ruleOperatorNode),
                        ruleOperatorNodes
                    )
                ),
            parameters: Object.fromEntries(
                Object.entries(ruleOperatorNode.parameters).map(([parameterKey, parameterValue]) => [
                    parameterKey,
                    parameterValue ?? "",
                ])
            ),
            type: "transformInput",
        };
        return transformOperator;
    } else if (ruleOperatorNode.pluginType === "PathInputOperator") {
        const pathInput: IPathInput = {
            id: ruleOperatorNode.nodeId,
            path: ruleOperatorNode.parameters["path"] ?? "",
            type: "pathInput",
        };
        return pathInput;
    } else {
        throw Error(
            `Tried to convert ${ruleOperatorNode.pluginType} node '${ruleOperatorNode.label}' to incompatible value input!`
        );
    }
};

/** Fetches and operator node from the available nodes. */
const fetchRuleOperatorNode = (
    nodeId: string,
    ruleOperators: Map<string, IRuleOperatorNode>,
    parentNode?: IRuleOperatorNode
): IRuleOperatorNode => {
    const ruleOperatorNode = ruleOperators.get(nodeId);
    if (ruleOperatorNode) {
        return ruleOperatorNode;
    } else {
        throw new Error(
            `Rule operator node with ID '${nodeId}' does not exist${
                parentNode ? `, but is defined as input for node '${parentNode.label}'!` : "!"
            }`
        );
    }
};

/** Converts the editor rule operator nodes to a map from ID to node and also returns all root nodes, i.e. nodes without parent. */
const convertToRuleOperatorNodeMap = (
    ruleOperatorNodes: IRuleOperatorNode[]
): [Map<string, IRuleOperatorNode>, IRuleOperatorNode[]] => {
    const hasParent: { [key: string]: boolean } = {};
    const nodeMap = new Map<string, IRuleOperatorNode>(
        ruleOperatorNodes.map((node) => {
            node.inputs.filter((i) => i != null).forEach((i) => (hasParent[i!!] = true));
            return [node.nodeId, node];
        })
    );
    const rootNodes = Object.entries(hasParent)
        .filter(([nodeId, hasParent]) => hasParent)
        .map(([nodeId]) => nodeMap.get(nodeId)!!);
    return [nodeMap, rootNodes];
};

const ruleUtils = {
    convertRuleOperator,
    convertRuleOperatorNodeToValueInput,
    fetchRuleOperatorNode,
    convertToRuleOperatorNodeMap,
};

export default ruleUtils;
