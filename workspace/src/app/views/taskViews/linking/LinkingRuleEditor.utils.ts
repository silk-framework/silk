/**
 * Convert to backend model:
 */

import {
    IRuleOperator,
    IRuleOperatorNode,
    IRuleSidebarPreConfiguredOperatorsTabConfig,
    RuleValidationError,
} from "../../shared/RuleEditor/RuleEditor.typings";
import { IValueInput, PathWithMetaData } from "../shared/rules/rule.typings";
import {
    IAggregationOperator,
    IComparisonOperator,
    ILinkingTaskParameters,
    ISimilarityOperator,
    LabelledParameterValue,
    OptionallyLabelledParameter,
} from "./linking.types";
import { RuleOperatorFetchFnType } from "../../shared/RuleEditor/RuleEditor";
import ruleUtils from "../shared/rules/rule.utils";
import { RuleOperatorType, TaskPlugin } from "@ducks/shared/typings";
import linkingRuleRequests from "./LinkingRuleEditor.requests";
import { IPreConfiguredRuleOperator } from "../../shared/RuleEditor/view/sidebar/RuleEditorOperatorSidebar.typings";
import { ruleEditorNodeParameterValue } from "../../shared/RuleEditor/model/RuleEditorModel.typings";

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
            },
            portSpecification: {
                minInputPorts: isComparison ? 2 : 1,
                maxInputPorts: isComparison ? 2 : undefined,
            },
            tags: [operator.type],
            description: ruleOperator(pluginId, pluginType)?.description,
        });
        return operator.id;
    }
};

/** Get the value of an optionally labelled parameter value. */
function optionallyLabelledParameterToValue<T>(optionallyLabelledValue: OptionallyLabelledParameter<T>): T {
    return (optionallyLabelledValue as LabelledParameterValue<T>).value
        ? (optionallyLabelledValue as LabelledParameterValue<T>).value
        : (optionallyLabelledValue as T);
}

/** Converts the linking task rule to the internal representation. */
const convertToRuleOperatorNodes = (
    linkSpec: TaskPlugin<ILinkingTaskParameters>,
    ruleOperator: RuleOperatorFetchFnType
): IRuleOperatorNode[] => {
    const rule = optionallyLabelledParameterToValue(linkSpec.parameters.rule);
    const operatorNodes: IRuleOperatorNode[] = [];
    extractSimilarityOperatorNode(rule.operator, operatorNodes, ruleOperator);
    const nodePositions = rule.layout.nodePositions;
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
                throw new RuleValidationError(
                    `Comparison operator '${ruleOperatorNode.label}' must have 2 inputs, but is missing at least 1 input!`,
                    [ruleOperatorNode]
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
                threshold: parseFloat(ruleEditorNodeParameterValue(ruleOperatorNode.parameters["threshold"])!!),
                weight: parseInt(ruleEditorNodeParameterValue(ruleOperatorNode.parameters["weight"])!!),
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
                weight: parseInt(ruleEditorNodeParameterValue(ruleOperatorNode.parameters["weight"])!!),
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
    const inputPathTabConfig: IRuleSidebarPreConfiguredOperatorsTabConfig<PathWithMetaData> = {
        id: `${sourceOrTarget}Paths`,
        icon: sourceOrTarget === "source" ? "data-sourcepath" : "data-targetpath",
        label: sourceOrTarget === "source" ? "Source paths" : "Target paths",
        fetchOperators: async (langPref: string) => {
            try {
                return (
                    await linkingRuleRequests.fetchLinkingCachedPaths(
                        projectId,
                        linkingTaskId,
                        sourceOrTarget,
                        true,
                        langPref
                    )
                ).data as PathWithMetaData[];
            } catch (ex) {
                errorHandler(ex);
            }
        },
        convertToOperator: (path: PathWithMetaData): IPreConfiguredRuleOperator => {
            const { pluginId, pluginType, icon } = baseOperator;
            return {
                pluginId,
                pluginType,
                icon,
                label: path.label ?? path.value,
                description: path.label ? path.value : undefined,
                categories: [sourceOrTarget === "source" ? "Source path" : "Target path"],
                parameterOverwrites: {
                    path: path.label ? { value: path.value, label: path.label } : path.value,
                },
                tags: [path.valueType],
            };
        },
        isOriginalOperator: (listItem) => (listItem as PathWithMetaData).valueType != null,
        itemSearchText: (listItem: PathWithMetaData) =>
            `${listItem.label ?? ""} ${listItem.value} ${listItem.valueType}`.toLowerCase(),
        itemLabel: (listItem: PathWithMetaData) => listItem.label ?? listItem.value,
        itemId: (listItem: PathWithMetaData) => listItem.value,
    };
    return inputPathTabConfig;
};

/** Constructs the rule tree of a linkage rule. This is data structure that is send to the backend. */
export const constructLinkageRuleTree = (ruleOperatorNodes: IRuleOperatorNode[]): ISimilarityOperator | undefined => {
    const [operatorNodeMap, rootNodes] = ruleUtils.convertToRuleOperatorNodeMap(ruleOperatorNodes, true);
    if (
        rootNodes.length === 1 &&
        rootNodes[0].pluginType !== "ComparisonOperator" &&
        rootNodes[0].pluginType !== "AggregationOperator"
    ) {
        throw new RuleValidationError(
            "Rule tree root must either be an aggregation or comparison!",
            rootNodes.map((node) => ({
                nodeId: node.nodeId,
                message: `Root node '${node.label}' is a '${node.pluginType}', but must be either a comparison or aggregation.`,
            }))
        );
    }

    return rootNodes.length === 1
        ? convertRuleOperatorNodeToSimilarityOperator(rootNodes[0], operatorNodeMap)
        : undefined;
};

const linkingRuleUtils = {
    convertRuleOperatorNodeToSimilarityOperator,
    convertToRuleOperatorNodes,
    inputPathTab,
    constructLinkageRuleTree,
    optionallyLabelledParameterToValue,
};

export default linkingRuleUtils;
