import { IPathInput, ITransformOperator, IValueInput } from "./rule.typings";
import { IRuleOperatorNode } from "../../../shared/RuleEditor/RuleEditor.typings";

const extractOperatorNodeFromPathInput = (
    pathInput: IPathInput,
    result: IRuleOperatorNode[],
    isTarget: boolean | undefined
): string => {
    result.push({
        nodeId: pathInput.id,
        label: `${isTarget ? "Target path:" : "Source path:"} ${pathInput.path}`, // TODO: Label?
        pluginType: "PathInputOperator",
        pluginId: isTarget ? "target" : "source", // We use the plugin ID to denote if this is a source or target path input.
        inputs: [],
        parameters: {
            path: pathInput.path,
        },
        portSpecification: {
            minInputPorts: 0,
            maxInputPorts: 0,
        },
        tags: [isTarget ? "target path" : "source path"],
    });
    return pathInput.id;
};

const extractOperatorNodeFromTransformInput = (
    transformInput: ITransformOperator,
    result: IRuleOperatorNode[],
    isTarget: boolean | undefined
): string => {
    const inputs = transformInput.inputs.map((input) => extractOperatorNodeFromValueInput(input, result, isTarget));
    result.push({
        nodeId: transformInput.id,
        inputs: inputs,
        pluginType: "TransformOperator",
        pluginId: transformInput.function,
        label: transformInput.function, // TODO: label
        parameters: Object.fromEntries(
            Object.entries(transformInput.parameters).map(([parameterId, parameterValue]) => {
                return [parameterId, parameterValue.defaultValue];
            })
        ),
        portSpecification: {
            minInputPorts: 1,
        },
        tags: ["transform", transformInput.function],
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
    isTarget: boolean | undefined
): string | undefined => {
    if (operator) {
        const nodeId =
            operator.type === "pathInput"
                ? extractOperatorNodeFromPathInput(operator as IPathInput, result, isTarget)
                : extractOperatorNodeFromTransformInput(operator as ITransformOperator, result, isTarget);
        return nodeId;
    }
};
