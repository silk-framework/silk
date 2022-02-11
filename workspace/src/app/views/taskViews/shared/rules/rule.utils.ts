import { IPathInput, ITransformOperator, IValueInput } from "./rule.typings";
import { IRuleOperator, IRuleOperatorNode } from "../../../shared/RuleEditor/RuleEditor.typings";
import { RuleOperatorFetchFnType } from "../../../shared/RuleEditor/RuleEditor";

const extractOperatorNodeFromPathInput = (
    pathInput: IPathInput,
    result: IRuleOperatorNode[],
    isTarget: boolean | undefined
): string => {
    result.push({
        nodeId: pathInput.id,
        label: isTarget ? "Target path" : "Source path",
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
    });
    return pathInput.id;
};

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
