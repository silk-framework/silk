import React from "react";
import { requestTaskData } from "@ducks/shared/requests";
import useErrorHandler from "../../../hooks/useErrorHandler";
import {
    IAggregationOperator,
    IComparisonOperator,
    ILinkingTaskParameters,
    ISimilarityOperator,
} from "./linking.types";
import { useTranslation } from "react-i18next";
import { IViewActions } from "../../plugins/PluginRegistry";
import { RuleEditor } from "../../shared/RuleEditor/RuleEditor";
import { requestRuleOperatorPluginDetails } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
import { IProjectTask } from "@ducks/shared/typings";
import { requestUpdateProjectTask } from "@ducks/workspace/requests";
import {
    IParameterSpecification,
    IPortSpecification,
    IRuleOperator,
    IRuleOperatorNode,
    PathInputOperator,
} from "../../shared/RuleEditor/RuleEditor.typings";
import { IPathInput, ITransformOperator, IValueInput } from "../rule.typings";

export interface LinkingRuleEditorProps {
    /** Project ID the task is in. */
    projectId: string;
    /** The task the rules are being edited of. */
    linkingTaskId: string;
    /** Generic actions and callbacks on views. */
    viewActions?: IViewActions;
}

/** Editor for creating and changing linking rule operator trees. */
export const LinkingRuleEditor = ({ projectId, linkingTaskId, viewActions }: LinkingRuleEditorProps) => {
    // The linking task parameters
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    /** Fetches the parameters of the linking task */
    const fetchTaskData = async (projectId: string, taskId: string) => {
        try {
            const taskData = (await requestTaskData<ILinkingTaskParameters>(projectId, taskId)).data;
            return taskData as IProjectTask<ILinkingTaskParameters>;
        } catch (err) {
            registerError(
                "LinkingRuleEditor_fetchLinkingTask",
                t("taskViews.linkRulesEditor.errors.fetchTaskData.msg"),
                err
            );
        }
    };
    /** Fetches the list of operators that can be used in a linking task. */
    const fetchLinkingRuleOperatorDetails = async () => {
        try {
            const response = (await requestRuleOperatorPluginDetails(false)).data;
            return Object.values(response);
        } catch (err) {
            registerError(
                "LinkingRuleEditor_fetchLinkingRuleOperatorDetails",
                t("taskViews.linkRulesEditor.errors.fetchLinkingRuleOperatorDetails.msg"),
                err
            );
        }
    };

    /** Save the rule. */
    const saveLinkageRule = async (ruleTree) => {
        try {
            // TODO: Convert rule tree to payload for PATCH request, only update the rule part, i.e. data.parameters.rule.
            await requestUpdateProjectTask(projectId, linkingTaskId, {});
            return true;
        } catch (err) {
            registerError(
                "LinkingRuleEditor_saveLinkageRule",
                t("taskViews.linkRulesEditor.errors.saveLinkageRule.msg"),
                err
            );
            return false;
        }
    };

    /** Converts the linking task rule to the internal representation. */
    const convertToRuleOperatorNodes = (linkingTask: IProjectTask<ILinkingTaskParameters>): IRuleOperatorNode[] => {
        const operatorNodes: IRuleOperatorNode[] = [];
        extractSimilarityOperatorNode(linkingTask.data.parameters.rule.operator, operatorNodes);
        return operatorNodes;
    };

    return (
        <RuleEditor<IProjectTask<ILinkingTaskParameters>, IPluginDetails>
            projectId={projectId}
            taskId={linkingTaskId}
            fetchRuleData={fetchTaskData}
            fetchRuleOperators={fetchLinkingRuleOperatorDetails}
            saveRule={saveLinkageRule}
            convertRuleOperator={convertRuleOperator}
            viewActions={viewActions}
            convertToRuleOperatorNodes={convertToRuleOperatorNodes}
        />
    );
};

/** Convert rule operators. */
export const convertRuleOperator = (op: IPluginDetails): IRuleOperator => {
    const required = (parameterId: string) => op.required.includes(parameterId);
    return {
        pluginType: op.pluginType ?? "unknown",
        pluginId: op.pluginId,
        label: op.pluginId, // TODO: What label?
        description: op.description,
        categories: op.categories,
        icon: "artefact-task", // TODO: Which icons?
        parameterSpecification: Object.fromEntries(
            Object.entries(op.properties).map(([parameterId, parameterSpec]) => {
                const spec: IParameterSpecification = {
                    label: parameterSpec.title,
                    description: parameterSpec.description,
                    advanced: parameterSpec.advanced,
                    required: required(parameterId),
                    type: "string", // TODO: Convert types from parameterSpec.parameterType, see InputMapper component
                    defaultValue: parameterSpec.value,
                };
                return [parameterId, spec];
            })
        ),
        portSpecification: portSpecification(op),
    };
};

const comparatorInputs = (comparison: IComparisonOperator): IValueInput[] => [
    comparison.sourceInput,
    comparison.targetInput,
];

const aggregatorInputs = (aggregator: IAggregationOperator): ISimilarityOperator[] => aggregator.inputs;

/**
 * Extracts and adds a single operator node to the array, recursively executes on its children.
 * @return the ID of the operator node
 */

const extractSimilarityOperatorNode = (
    operator: ISimilarityOperator | undefined,
    result: IRuleOperatorNode[]
): string | undefined => {
    if (operator) {
        const isComparison = operator.type === "Comparison";
        const inputs = isComparison
            ? comparatorInputs(operator as IComparisonOperator).map((input, idx) =>
                  extractOperatorNodeFromValueInput(input, result, idx > 0)
              )
            : aggregatorInputs(operator as IAggregationOperator).map((input) =>
                  extractSimilarityOperatorNode(input, result)
              );
        result.push({
            nodeId: operator.id,
            label: isComparison ? "Comparison" : "Aggregation", // TODO: Adapt label
            pluginType: isComparison ? "ComparisonOperator" : "AggregationOperator",
            pluginId: isComparison
                ? (operator as IComparisonOperator).metric
                : (operator as IAggregationOperator).aggregator,
            inputs: inputs,
            parameters: Object.fromEntries(
                Object.entries(operator.parameters).map(([parameterId, parameterValue]) => {
                    return [parameterId, parameterValue.defaultValue];
                })
            ),
            portSpecification: {
                minInputPorts: 1,
            },
        });
        return operator.id;
    }
};

export const portSpecification = (op: IPluginDetails): IPortSpecification => {
    switch (op.pluginType) {
        case "ComparisonOperator":
            return { minInputPorts: 2, maxInputPorts: 2 };
        default:
            return { minInputPorts: 1 };
    }
};

const extractOperatorNodeFromPathInput = (
    pathInput: IPathInput,
    result: IRuleOperatorNode[],
    isTarget: boolean | undefined
): string => {
    result.push({
        nodeId: pathInput.id,
        label: `${isTarget ? "Target path" : "Source path"} ${pathInput.path}`, // TODO: Label?
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
