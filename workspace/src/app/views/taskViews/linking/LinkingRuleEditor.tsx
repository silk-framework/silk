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
import { IParameterSpecification, IRuleOperator, IRuleOperatorNode } from "../../shared/RuleEditor/RuleEditor.typings";
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
            return (await requestRuleOperatorPluginDetails(false)).data;
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
        const comparatorInputs = (comparison: IComparisonOperator): IValueInput[] => [
            comparison.sourceInput,
            comparison.targetInput,
        ];
        const aggregatorInputs = (aggregator: IAggregationOperator): ISimilarityOperator[] => aggregator.operators;
        const operatorNodes: IRuleOperatorNode[] = [];
        // Extracts and adds a single operator node to the array, recursively executes on its children
        // returns the ID of the operator node
        const extractOperatorNode = (
            operator: ISimilarityOperator | undefined,
            result: IRuleOperatorNode[]
        ): string | undefined => {
            if (operator) {
                const inputs =
                    operator.type === "Comparison"
                        ? comparatorInputs(operator as IComparisonOperator).map((input) =>
                              extractOperatorNodeFromValueInput(input, operatorNodes)
                          )
                        : aggregatorInputs(operator as IAggregationOperator).map((input) =>
                              extractOperatorNode(input, operatorNodes)
                          );
                result.push({
                    nodeId: operator.id,
                    label: "", // TODO: Adapt label
                    pluginId:
                        operator.type === "Aggregation"
                            ? (operator as IAggregationOperator).aggregator
                            : (operator as IComparisonOperator).metric,
                    inputs: inputs,
                    parameters: Object.fromEntries(
                        Object.entries(operator.parameters).map(([parameterId, parameterValue]) => {
                            return [parameterId, parameterValue.defaultValue];
                        })
                    ),
                });
                return operator.id;
            }
        };
        extractOperatorNode(linkingTask.data.parameters.rule.operator, operatorNodes);
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
        pluginId: op.pluginId,
        label: op.title,
        description: op.description,
        categories: op.categories,
        icon: "TODO: add icon",
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
    };
};

const extractOperatorNodeFromPathInput = (pathInput: IPathInput, result: IRuleOperatorNode[]): string => {
    result.push({
        nodeId: pathInput.id,
        label: "", // TODO: Label?
        pluginId: "pathInput", // TODO: How to handle path input, replace with constant
        inputs: [],
        parameters: {
            path: pathInput.path,
        },
    });
    return pathInput.id;
};

const extractOperatorNodeFromTransformInput = (
    transformInput: ITransformOperator,
    result: IRuleOperatorNode[]
): string => {
    const inputs = transformInput.inputs.map((input) => extractOperatorNodeFromValueInput(input, result));
    result.push({
        nodeId: transformInput.id,
        inputs: inputs,
        pluginId: transformInput.function,
        label: "", // TODO: label
        parameters: Object.fromEntries(
            Object.entries(transformInput.parameters).map(([parameterId, parameterValue]) => {
                return [parameterId, parameterValue.defaultValue];
            })
        ),
    });
    return transformInput.id;
};

/** Extract operator nodes from an value input node, i.e. path input or transform operator. */
export const extractOperatorNodeFromValueInput = (
    operator: IValueInput | undefined,
    result: IRuleOperatorNode[]
): string | undefined => {
    if (operator) {
        const nodeId =
            operator.type === "pathInput"
                ? extractOperatorNodeFromPathInput(operator as IPathInput, result)
                : extractOperatorNodeFromTransformInput(operator as ITransformOperator, result);
        return nodeId;
    }
};
