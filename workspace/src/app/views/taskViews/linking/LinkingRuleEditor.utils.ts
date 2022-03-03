/**
 * Convert to backend model:
 */

import {
    IRuleOperator,
    IRuleOperatorNode,
    IRuleSidebarPreConfiguredOperatorsTabConfig,
} from "../../shared/RuleEditor/RuleEditor.typings";
import { IValueInput } from "../shared/rules/rule.typings";
import {
    IAggregationOperator,
    IComparisonOperator,
    ILinkingTaskParameters,
    ISimilarityOperator,
} from "./linking.types";
import { RuleOperatorFetchFnType } from "../../shared/RuleEditor/RuleEditor";
import ruleUtils from "../shared/rules/rule.utils";
import { IProjectTask, RuleOperatorType } from "@ducks/shared/typings";
import linkingRuleRequests from "./LinkingRuleEditor.requests";
import { IPreConfiguredRuleOperator } from "../../shared/RuleEditor/view/sidebar/RuleEditorOperatorSidebar.typings";

/**
 * Convert to editor model:
 */

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
    result: IRuleOperatorNode[],
    ruleOperator: RuleOperatorFetchFnType
): string | undefined => {
    if (operator) {
        const isComparison = operator.type === "Comparison";
        const inputs = isComparison
            ? comparatorInputs(operator as IComparisonOperator).map((input, idx) =>
                  ruleUtils.extractOperatorNodeFromValueInput(input, result, idx > 0, ruleOperator)
              )
            : aggregatorInputs(operator as IAggregationOperator).map((input) =>
                  extractSimilarityOperatorNode(input, result, ruleOperator)
              );
        const pluginId = isComparison
            ? (operator as IComparisonOperator).metric
            : (operator as IAggregationOperator).aggregator;
        const pluginType: RuleOperatorType = isComparison ? "ComparisonOperator" : "AggregationOperator";
        const threshold = (comparison: IComparisonOperator): string => {
            let t = comparison.threshold.toString();
            if (t.length === 1) {
                // Threshold should be recognized as a float value
                t = comparison.threshold.toFixed(1);
            }
            return t;
        };
        const additionalParameters = isComparison
            ? {
                  threshold: threshold(operator as IComparisonOperator),
                  weight: operator.weight.toString(),
              }
            : {
                  weight: operator.weight.toString(),
              };

        result.push({
            nodeId: operator.id,
            label: ruleOperator(pluginId, pluginType)?.label ?? pluginId,
            pluginType,
            pluginId,
            inputs: inputs,
            parameters: {
                ...operator.parameters,
                ...additionalParameters,
            }, // TODO CMEM-3919 Add weight and threshold
            portSpecification: {
                minInputPorts: isComparison ? 2 : 1,
                maxInputPorts: isComparison ? 2 : undefined,
            },
            tags: [operator.type],
        });
        return operator.id;
    }
};

/** Converts the linking task rule to the internal representation. */
const convertToRuleOperatorNodes = (
    linkingTask: IProjectTask<ILinkingTaskParameters>,
    ruleOperator: RuleOperatorFetchFnType
): IRuleOperatorNode[] => {
    const operatorNodes: IRuleOperatorNode[] = [];
    extractSimilarityOperatorNode(linkingTask.data.parameters.rule.operator, operatorNodes, ruleOperator);
    const nodePositions = linkingTask.data.parameters.rule.layout.nodePositions;
    operatorNodes.forEach((node) => {
        const [x, y] = nodePositions[node.nodeId] ?? [null, null];
        node.position = x !== null ? { x, y } : undefined;
    });
    return operatorNodes;
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
                sourceInput: ruleUtils.convertRuleOperatorNodeToValueInput(
                    ruleUtils.fetchRuleOperatorNode(ruleOperatorNode.inputs[0]!!, ruleOperatorNodes, ruleOperatorNode),
                    ruleOperatorNodes
                ),
                targetInput: ruleUtils.convertRuleOperatorNodeToValueInput(
                    ruleUtils.fetchRuleOperatorNode(ruleOperatorNode.inputs[1]!!, ruleOperatorNodes, ruleOperatorNode),
                    ruleOperatorNodes
                ),
                id: ruleOperatorNode.nodeId,
                indexing: false, // FIXME: Should this be part of the config in the UI? CMEM-3919
                parameters: Object.fromEntries(
                    Object.entries(ruleOperatorNode.parameters).map(([parameterKey, parameterValue]) => [
                        parameterKey,
                        parameterValue ?? "",
                    ])
                ),
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
                                ruleUtils.fetchRuleOperatorNode(i!!, ruleOperatorNodes, ruleOperatorNode),
                                ruleOperatorNodes
                            )!!
                    ),
                parameters: Object.fromEntries(
                    Object.entries(ruleOperatorNode.parameters).map(([parameterKey, parameterValue]) => [
                        parameterKey,
                        parameterValue ?? "",
                    ])
                ),
                type: "Aggregation",
                weight: parseInt(ruleOperatorNode.parameters["weight"]!!),
            };
            return aggregation;
        }
    }
};

/** Input path tab configuration. */
const inputPathTab = (
    projectId: string,
    linkingTaskId: string,
    baseOperator: IRuleOperator,
    sourceOrTarget: "source" | "target",
    errorHandler: (err) => any
): IRuleSidebarPreConfiguredOperatorsTabConfig => {
    return {
        id: `${sourceOrTarget}Paths`,
        label: sourceOrTarget === "source" ? "Source paths" : "Target paths",
        fetchOperators: async () => {
            try {
                return (await linkingRuleRequests.fetchLinkingCachedPaths(projectId, linkingTaskId, sourceOrTarget))
                    .data;
            } catch (ex) {
                errorHandler(ex);
            }
        },
        convertToOperator: (path: string): IPreConfiguredRuleOperator => {
            const { pluginId, pluginType, icon } = baseOperator;
            return {
                pluginId,
                pluginType,
                icon,
                label: path,
                categories: [sourceOrTarget === "source" ? "Source path" : "Target path"],
                parameterOverwrites: {
                    path,
                },
                tags: [],
            };
        },
        isOriginalOperator: (listItem) => typeof listItem === "string",
        itemSearchText: (listItem: string) => listItem,
        itemLabel: (listItem: string) => listItem,
    };
};

const linkingRuleUtils = {
    convertRuleOperatorNodeToSimilarityOperator,
    convertToRuleOperatorNodes,
    inputPathTab,
};

export default linkingRuleUtils;
