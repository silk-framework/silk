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
} from "../../shared/RuleEditor/RuleEditor.typings";
import { IPathInput, ITransformOperator, IValueInput } from "../shared/rules/rule.typings";
import { extractOperatorNodeFromValueInput } from "../shared/rules/rule.utils";

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
    const saveLinkageRule = async (ruleOperatorNodes: IRuleOperatorNode[]) => {
        try {
            const [operatorNodeMap, rootNodes] = convertToRuleOperatorNodeMap(ruleOperatorNodes);
            if (rootNodes.length > 1) {
                throw Error(`More than one root node found! Root nodes: ${rootNodes.map((n) => n.label).join(", ")}`);
            }
            await requestUpdateProjectTask(projectId, linkingTaskId, {
                data: {
                    parameters: {
                        rule:
                            rootNodes.length === 1
                                ? convertRuleOperatorNodeToSimilarityOperator(rootNodes[0], operatorNodeMap)
                                : undefined,
                    },
                },
            });
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

    const convertRuleOperatorNodeToSimilarityOperator = (
        ruleOperatorNode: IRuleOperatorNode | undefined,
        ruleOperatorNodes: Map<string, IRuleOperatorNode>
    ): ISimilarityOperator | undefined => {
        if (ruleOperatorNode) {
            if (ruleOperatorNode.pluginType === "ComparisonOperator") {
                if (ruleOperatorNode.inputs.length !== 2 || !ruleOperatorNode.inputs.every((input) => input != null)) {
                    throw Error(
                        `Comparison operator '${ruleOperatorNode.label}' must have 2 inputs, but is missing at least 1!`
                    );
                }
                const comparison: IComparisonOperator = {
                    metric: ruleOperatorNode.pluginId,
                    sourceInput: convertRuleOperatorNodeToValueInput(
                        fetchRuleOperatorNode(ruleOperatorNode.inputs[0]!!, ruleOperatorNodes, ruleOperatorNode),
                        ruleOperatorNodes
                    ),
                    targetInput: convertRuleOperatorNodeToValueInput(
                        fetchRuleOperatorNode(ruleOperatorNode.inputs[1]!!, ruleOperatorNodes, ruleOperatorNode),
                        ruleOperatorNodes
                    ),
                    id: ruleOperatorNode.nodeId,
                    indexing: false, // TODO: What to set it to?
                    parameters: {}, // Object.fromEntries(Object.entries(ruleOperator.parameters) TODO: Did I get this right?
                    //.map(([parameterKey, parameterValue]) => [parameterKey, parameterValue ?? ""])),
                    type: "Comparison",
                    threshold: parseFloat(ruleOperatorNode.parameters["threshold"]!!),
                    weight: parseInt(ruleOperatorNode.parameters["weight"]!!),
                };
                return comparison;
            } else {
                const aggregation: IAggregationOperator = {
                    id: ruleOperatorNode.nodeId,
                    aggregator: ruleOperatorNode.pluginId,
                    inputs: ruleOperatorNode.inputs
                        .filter((i) => i != null)
                        .map(
                            (i) =>
                                convertRuleOperatorNodeToSimilarityOperator(
                                    fetchRuleOperatorNode(i!!, ruleOperatorNodes, ruleOperatorNode),
                                    ruleOperatorNodes
                                )!!
                        ),
                    parameters: {}, // TODO: add parameters
                    type: "Aggregation",
                    weight: parseInt(ruleOperatorNode.parameters["weight"]!!),
                };
                return aggregation;
            }
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

/**
 * Convert to backend model:
 */

/** Converts the editor rule operator nodes to a map from ID to node and also returns all root nodes, i.e. nodes without parent. */
export const convertToRuleOperatorNodeMap = (
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

/** Converts a rule operator node to a value input. */
export const convertRuleOperatorNodeToValueInput = (
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
            parameters: {}, // TODO: How to get parameters
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

/**
 * Convert to editor model:
 */

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
        const pluginId = isComparison
            ? (operator as IComparisonOperator).metric
            : (operator as IAggregationOperator).aggregator;
        result.push({
            nodeId: operator.id,
            label: pluginId, // TODO: Adapt label
            pluginType: isComparison ? "ComparisonOperator" : "AggregationOperator",
            pluginId,
            inputs: inputs,
            parameters: Object.fromEntries(
                Object.entries(operator.parameters).map(([parameterId, parameterValue]) => {
                    return [parameterId, parameterValue.defaultValue];
                })
            ),
            portSpecification: {
                minInputPorts: isComparison ? 2 : 1,
                maxInputPorts: isComparison ? 2 : undefined,
            },
            tags: [operator.type, pluginId], // TODO: What tags?
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
